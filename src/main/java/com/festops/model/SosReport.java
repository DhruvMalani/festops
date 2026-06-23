package com.festops.model;

/**
 * A raw SOS report as it arrives on the intake queue, before triage.
 */
public record SosReport(String id, String text, double lat, double lng) {
}
