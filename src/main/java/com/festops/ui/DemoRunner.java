package com.festops.ui;

import com.festops.dao.AuditDAO;
import com.festops.dispatch.Dispatcher;
import com.festops.exception.InvalidStateTransitionException;
import com.festops.factory.IncidentFactory;
import com.festops.model.Incident;
import com.festops.model.IncidentType;
import com.festops.model.Responder;
import com.festops.model.SosReport;
import com.festops.service.AuditLogObserver;
import com.festops.triage.RuleBasedTriageStrategy;
import com.festops.triage.TriageStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * End-to-end smoke test of the FestOps domain core, runnable as a plain Java
 * program (no Spring). It pushes sample SOS reports onto a {@link BlockingQueue},
 * triages each into an incident type, builds the incident, dispatches a
 * responder, walks the lifecycle, and finally dumps the JDBC-backed audit log.
 */
public class DemoRunner {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== FestOps domain-core demo ===\n");

        // Wire up the audit observer backed by H2.
        AuditDAO auditDAO = new AuditDAO();
        AuditLogObserver auditObserver = new AuditLogObserver(auditDAO);

        // Register a pool of responders around the festival grounds (Mumbai-ish coords).
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.register(new Responder("R1", "Asha (Medic)",     "medical",     19.0760, 72.8777, true, 0));
        dispatcher.register(new Responder("R2", "Ravi (Security)",  "security",    19.0765, 72.8781, true, 1));
        dispatcher.register(new Responder("R3", "Fire Unit 1",      "fire",        19.0751, 72.8792, true, 0));
        dispatcher.register(new Responder("R4", "Maya (Medic)",     "medical",     19.0902, 72.8801, true, 2));
        dispatcher.register(new Responder("R5", "Ops Crew",         "maintenance", 19.0770, 72.8770, true, 0));

        // Three SOS reports land on the intake queue.
        BlockingQueue<SosReport> queue = new LinkedBlockingQueue<>();
        queue.put(new SosReport("SOS-1", "Someone collapsed near the main stage and is bleeding!", 19.0761, 72.8778));
        queue.put(new SosReport("SOS-2", "I can see smoke and flames behind the food court!",      19.0752, 72.8793));
        queue.put(new SosReport("SOS-3", "A fight broke out and someone is waving a knife!",       19.0766, 72.8782));

        TriageStrategy triage = new RuleBasedTriageStrategy();
        List<Incident> processed = new ArrayList<>();
        int seq = 0;

        while (!queue.isEmpty()) {
            SosReport report = queue.take();
            seq++;

            IncidentType type = triage.classify(report.text());
            Incident incident = IncidentFactory.create(
                    type, "INC-" + seq, report.text(), report.lat(), report.lng());
            incident.addObserver(auditObserver);

            System.out.println("[" + report.id() + "] \"" + report.text() + "\"");
            System.out.printf("   triaged -> %-9s | severity=%-8s | skill=%-11s | SLA=%ds%n",
                    type, incident.getSeverity(), incident.requiredSkill(), incident.slaSeconds());

            Responder chosen = dispatcher.dispatch(incident);
            if (chosen != null) {
                System.out.printf("   dispatched -> %s [%s]%n", chosen.getName(), chosen.getSkill());
            } else {
                System.out.println("   dispatched -> (no responder available)");
            }

            // Walk the happy-path lifecycle: REPORTED -> ACKNOWLEDGED -> RESPONDING -> RESOLVED.
            incident.acknowledge();
            incident.respond();
            incident.resolve();
            System.out.println("   lifecycle  -> final state = " + incident.getStateName());
            System.out.println();

            processed.add(incident);
        }

        // Demonstrate escalation and an illegal transition being rejected.
        System.out.println("=== Escalation & invalid-transition demo ===");
        Incident surge = IncidentFactory.create(
                IncidentType.SECURITY, "INC-ESC", "Crowd surge at the east gate", 19.0700, 72.8800);
        surge.addObserver(auditObserver);

        surge.acknowledge();                 // REPORTED -> ACKNOWLEDGED
        surge.escalate();                    // any -> ESCALATED
        System.out.println("   after escalate       -> " + surge.getStateName());
        surge.acknowledge();                 // ESCALATED -> ACKNOWLEDGED
        System.out.println("   after re-acknowledge -> " + surge.getStateName());

        try {
            surge.resolve();                 // illegal: ACKNOWLEDGED has no direct resolve
        } catch (InvalidStateTransitionException ex) {
            System.out.println("   rejected as expected -> " + ex.getMessage());
        }
        System.out.println();

        // Dump the audit trail straight from H2 via JDBC.
        System.out.println("=== Audit log (read from H2 via JDBC) ===");
        for (String row : auditDAO.findAll()) {
            System.out.println("   " + row);
        }
        System.out.println("   total entries: " + auditDAO.count());
    }
}
