package com.festops.triage;

import com.festops.model.IncidentType;

import java.util.regex.Pattern;

/**
 * Classifies SOS text by matching keyword regexes. Patterns are checked in
 * priority order (fire and medical before security and logistics) so a report
 * mentioning several hazards is routed to the most urgent one. Falls back to
 * {@link IncidentType#LOGISTICS} when nothing matches.
 */
public class RuleBasedTriageStrategy implements TriageStrategy {

    private static final Pattern FIRE = Pattern.compile(
            "\\b(fire|smoke|flame|flames|burning|gas\\s*leak|explosion|blaze)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern MEDICAL = Pattern.compile(
            "\\b(hurt|injured|bleeding|blood|unconscious|medic|doctor|ambulance|"
                    + "faint|fainted|collapse|collapsed|heart|breathing|seizure|overdose)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SECURITY = Pattern.compile(
            "\\b(fight|fighting|weapon|theft|stolen|assault|stampede|suspicious|"
                    + "gun|knife|attack|riot|brawl)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern LOGISTICS = Pattern.compile(
            "\\b(power|water|barricade|stage|supply|equipment|broken|maintenance|"
                    + "outage|blocked|generator|fence)\\b",
            Pattern.CASE_INSENSITIVE);

    @Override
    public IncidentType classify(String sosText) {
        if (sosText == null || sosText.isBlank()) {
            return IncidentType.LOGISTICS;
        }
        if (FIRE.matcher(sosText).find()) {
            return IncidentType.FIRE;
        }
        if (MEDICAL.matcher(sosText).find()) {
            return IncidentType.MEDICAL;
        }
        if (SECURITY.matcher(sosText).find()) {
            return IncidentType.SECURITY;
        }
        if (LOGISTICS.matcher(sosText).find()) {
            return IncidentType.LOGISTICS;
        }
        return IncidentType.LOGISTICS;
    }
}
