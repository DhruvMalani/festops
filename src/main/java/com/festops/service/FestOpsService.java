package com.festops.service;

import com.festops.dao.AuditDAO;
import com.festops.dao.ResponderCsvLoader;
import com.festops.dispatch.Dispatcher;
import com.festops.exception.IncidentNotFoundException;
import com.festops.exception.NoResponderAvailableException;
import com.festops.exception.ResponderNotFoundException;
import com.festops.factory.IncidentFactory;
import com.festops.model.Incident;
import com.festops.model.IncidentType;
import com.festops.model.Responder;
import com.festops.triage.AgenticTriageStrategy;
import com.festops.triage.RuleBasedTriageStrategy;
import com.festops.triage.TriageStrategy;
import com.festops.util.HaversineUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds the shared FestOps runtime state and runs the SOS intake pipeline as a
 * producer-consumer system:
 *
 * <ul>
 *   <li>The REST controller is the <b>producer</b>: it assigns an incident id
 *       and drops the report on a {@link BlockingQueue}, then returns
 *       immediately — API latency is decoupled from processing.</li>
 *   <li>A single <b>consumer</b> thread on an {@link ExecutorService} pulls from
 *       the queue, triages, dispatches and stores each incident.</li>
 *   <li>A {@link ScheduledExecutorService} runs every 30s as an <b>SLA monitor</b>,
 *       auto-escalating any incident stuck in REPORTED beyond its
 *       {@code slaSeconds()}.</li>
 * </ul>
 *
 * All shared state ({@code incidents}, the {@link Dispatcher}'s internal maps,
 * the intake queue) is held in concurrent collections.
 */
@Service
public class FestOpsService {

    private static final Logger log = LoggerFactory.getLogger(FestOpsService.class);
    private static final long SLA_CHECK_INTERVAL_SECONDS = 30;

    // --- shared state -------------------------------------------------------
    private final Map<String, Incident> incidents = new ConcurrentHashMap<>();
    private final Dispatcher dispatcher = new Dispatcher();
    private TriageStrategy triageStrategy;
    private final AuditDAO auditDAO = new AuditDAO();
    private final AuditLogObserver auditObserver = new AuditLogObserver(auditDAO);
    private final AtomicInteger sequence = new AtomicInteger(0);

    @Value("${festops.responders.csv:data/responders.csv}")
    private String responderCsvPath;

    /** "rule" = regex triage; "agentic" = Anthropic API triage (rule-based fallback). */
    @Value("${festops.triage.mode:rule}")
    private String triageMode;

    // --- producer-consumer plumbing ----------------------------------------
    private final BlockingQueue<QueuedSos> intakeQueue = new LinkedBlockingQueue<>();
    private final ExecutorService consumerExecutor =
            Executors.newSingleThreadExecutor(r -> new Thread(r, "sos-consumer"));
    private final ScheduledExecutorService slaScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "sla-monitor"));
    private volatile boolean running = true;

    /** Internal queue payload — the incident id is assigned up front so the API can return it. */
    private record QueuedSos(String incidentId, String reporterId, String description,
                             double latitude, double longitude) {
    }

    @PostConstruct
    void start() {
        loadResponders();
        triageStrategy = "agentic".equalsIgnoreCase(triageMode)
                ? new AgenticTriageStrategy()
                : new RuleBasedTriageStrategy();
        log.info("Triage strategy: {} (mode='{}')",
                triageStrategy.getClass().getSimpleName(), triageMode);
        consumerExecutor.submit(this::consumeLoop);
        slaScheduler.scheduleAtFixedRate(this::checkSlaBreaches,
                SLA_CHECK_INTERVAL_SECONDS, SLA_CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        log.info("FestOps started: SOS consumer + SLA monitor (every {}s) running",
                SLA_CHECK_INTERVAL_SECONDS);
    }

    @PreDestroy
    void stop() {
        running = false;
        consumerExecutor.shutdownNow();
        slaScheduler.shutdownNow();
        log.info("FestOps stopped");
    }

    /** Load the responder pool from the CSV file so dispatch has something to choose from. */
    private void loadResponders() {
        List<Responder> loaded = ResponderCsvLoader.load(responderCsvPath);
        loaded.forEach(dispatcher::register);
        log.info("Loaded {} responders from {}", loaded.size(), responderCsvPath);
    }

    // --- producer side (called by the REST controller) ---------------------

    /**
     * Enqueue an SOS report for async processing.
     *
     * @return the incident id assigned to the report (the incident itself is
     *         created a moment later by the consumer).
     */
    public String enqueue(String reporterId, String description, double latitude, double longitude) {
        String id = "INC-" + sequence.incrementAndGet();
        intakeQueue.add(new QueuedSos(id, reporterId, description, latitude, longitude));
        log.info("Queued {} (queue depth now {})", id, intakeQueue.size());
        return id;
    }

    public int queueDepth() {
        return intakeQueue.size();
    }

    // --- consumer side ------------------------------------------------------

    private void consumeLoop() {
        log.info("SOS consumer thread started");
        while (running) {
            QueuedSos item;
            try {
                item = intakeQueue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            try {
                process(item);
            } catch (RuntimeException e) {
                log.error("Failed to process {}", item.incidentId(), e);
            }
        }
        log.info("SOS consumer thread exiting");
    }

    private void process(QueuedSos item) {
        IncidentType type = triageStrategy.classify(item.description());
        Incident incident = IncidentFactory.create(
                type, item.incidentId(), item.description(), item.latitude(), item.longitude());
        incident.addObserver(auditObserver);

        Responder assigned = null;
        try {
            assigned = dispatcher.dispatch(incident);
        } catch (NoResponderAvailableException e) {
            log.warn("{} — incident stored unassigned", e.getMessage());
        }
        incidents.put(incident.getId(), incident);

        log.info("Processed {} -> {} (severity={}, responder={})",
                incident.getId(), type, incident.getSeverity(),
                assigned != null ? assigned.getName() : "none");
    }

    // --- SLA monitor --------------------------------------------------------

    /**
     * Auto-escalate any incident that has been in REPORTED longer than its SLA.
     * Package-private so it can be exercised directly in tests.
     */
    void checkSlaBreaches() {
        Instant now = Instant.now();
        for (Incident incident : incidents.values()) {
            if (!"REPORTED".equals(incident.getStateName())) {
                continue;
            }
            long ageSeconds = Duration.between(incident.getReportedAt(), now).getSeconds();
            if (ageSeconds > incident.slaSeconds()) {
                try {
                    incident.escalate();
                    log.warn("SLA breach: {} stuck in REPORTED for {}s (SLA {}s) -> auto-escalated",
                            incident.getId(), ageSeconds, incident.slaSeconds());
                } catch (RuntimeException e) {
                    log.error("Failed to auto-escalate {}", incident.getId(), e);
                }
            }
        }
    }

    // --- queries / transitions ---------------------------------------------

    public Collection<Incident> listIncidents() {
        return incidents.values();
    }

    public Incident getIncident(String id) {
        Incident incident = incidents.get(id);
        if (incident == null) {
            throw new IncidentNotFoundException(id);
        }
        return incident;
    }

    public List<String> auditTrail(String id) {
        return auditDAO.findByIncident(id);
    }

    // --- responder operations ----------------------------------------------

    public Collection<Responder> listResponders() {
        return dispatcher.getResponders().values();
    }

    public Responder getResponder(String id) {
        Responder responder = dispatcher.getResponders().get(id);
        if (responder == null) {
            throw new ResponderNotFoundException(id);
        }
        return responder;
    }

    /**
     * Responders ranked nearest-first from the given point, optionally filtered
     * by skill (case-insensitive; blank/null means no filter).
     */
    public List<Responder> findNearby(double lat, double lng, String skill) {
        boolean filterSkill = skill != null && !skill.isBlank();
        return listResponders().stream()
                .filter(r -> !filterSkill || r.getSkill().equalsIgnoreCase(skill.trim()))
                .sorted(Comparator.comparingDouble(
                        r -> HaversineUtil.distanceKm(lat, lng, r.getLat(), r.getLng())))
                .toList();
    }

    public Responder updateResponderAvailability(String id, boolean available) {
        Responder responder = getResponder(id);
        responder.setAvailable(available);
        log.info("Responder {} availability set to {}", id, available);
        return responder;
    }

    /**
     * Move an incident to the requested target {@code status}, one of
     * ACKNOWLEDGED / RESPONDING / RESOLVED / ESCALATED. The target state maps to
     * the lifecycle transition that reaches it; an illegal move from the current
     * state throws {@link com.festops.exception.InvalidStateTransitionException}.
     */
    public Incident transition(String id, String status) {
        Incident incident = getIncident(id);
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Missing 'status'");
        }
        switch (status.trim().toUpperCase()) {
            case "ACKNOWLEDGED" -> incident.acknowledge();
            case "RESPONDING" -> incident.respond();
            case "RESOLVED" -> incident.resolve();
            case "ESCALATED" -> incident.escalate();
            default -> throw new IllegalArgumentException(
                    "Unknown status '" + status + "'. Expected one of: "
                            + "ACKNOWLEDGED, RESPONDING, RESOLVED, ESCALATED");
        }
        return incident;
    }
}
