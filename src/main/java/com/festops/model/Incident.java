package com.festops.model;

import com.festops.state.IncidentState;
import com.festops.state.ReportedState;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base of the incident hierarchy. Holds common report data, drives the
 * lifecycle via the State pattern, and notifies {@link IncidentObserver}s on
 * every transition.
 *
 * <p>Subclasses define their category-specific severity, required responder
 * skill and SLA via the three abstract methods.</p>
 */
public abstract class Incident {

    private final String id;
    private final String description;
    private final double lat;
    private final double lng;
    private final Severity severity;
    private final Instant reportedAt;

    private IncidentState state;
    private Responder assignedResponder;
    private final List<IncidentObserver> observers = new ArrayList<>();

    protected Incident(String id, String description, double lat, double lng) {
        this.id = id;
        this.description = description;
        this.lat = lat;
        this.lng = lng;
        this.severity = defaultSeverity();
        this.reportedAt = Instant.now();
        this.state = new ReportedState();
    }

    // --- category-specific behaviour ---------------------------------------

    public abstract IncidentType getType();

    public abstract Severity defaultSeverity();

    public abstract String requiredSkill();

    public abstract long slaSeconds();

    // --- observer registration ---------------------------------------------

    public void addObserver(IncidentObserver observer) {
        observers.add(observer);
    }

    // --- lifecycle transitions (State pattern) -----------------------------

    public synchronized void acknowledge() {
        transitionTo(state.acknowledge(this));
    }

    public synchronized void startResponding() {
        transitionTo(state.respond(this));
    }

    public synchronized void resolve() {
        transitionTo(state.resolve(this));
    }

    public synchronized void escalate() {
        transitionTo(state.escalate(this));
    }

    private void transitionTo(IncidentState next) {
        String from = state.name();
        this.state = next;
        for (IncidentObserver observer : observers) {
            observer.onStateChange(this, from, next.name());
        }
    }

    // --- accessors ----------------------------------------------------------

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public double getLat() {
        return lat;
    }

    public double getLng() {
        return lng;
    }

    public Severity getSeverity() {
        return severity;
    }

    public Instant getReportedAt() {
        return reportedAt;
    }

    public synchronized IncidentState getState() {
        return state;
    }

    public synchronized String getStateName() {
        return state.name();
    }

    public Responder getAssignedResponder() {
        return assignedResponder;
    }

    public void setAssignedResponder(Responder assignedResponder) {
        this.assignedResponder = assignedResponder;
    }
}
