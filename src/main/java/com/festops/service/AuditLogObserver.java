package com.festops.service;

import com.festops.dao.AuditDAO;
import com.festops.model.Incident;
import com.festops.model.IncidentObserver;

/**
 * {@link IncidentObserver} that persists every state transition to the audit
 * log via {@link AuditDAO}.
 */
public class AuditLogObserver implements IncidentObserver {

    private final AuditDAO auditDAO;

    public AuditLogObserver(AuditDAO auditDAO) {
        this.auditDAO = auditDAO;
    }

    @Override
    public void onStateChange(Incident incident, String fromState, String toState) {
        auditDAO.record(incident.getId(), fromState, toState);
    }
}
