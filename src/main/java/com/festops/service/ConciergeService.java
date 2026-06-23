package com.festops.service;

import com.festops.model.Event;
import com.festops.util.HaversineUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads festival events from {@code data/events.csv} on startup and answers
 * "what's happening near me, soon" queries.
 *
 * <p><b>Demo rebasing:</b> the sample CSV is dated in the past, so on load the
 * whole schedule is shifted in time so that the busiest 60-minute block of the
 * fest begins at the current moment. Venue coordinates and the relative spacing
 * between events are preserved — only the absolute times move.</p>
 */
@Service
public class ConciergeService {

    private static final Logger log = LoggerFactory.getLogger(ConciergeService.class);

    private static final double RADIUS_METERS = 500.0;
    private static final long WINDOW_MINUTES = 60;

    private static final double PROXIMITY_WEIGHT = 0.4;
    private static final double URGENCY_WEIGHT = 0.3;
    private static final double TAG_WEIGHT = 0.3;

    @Value("${festops.events.csv:data/events.csv}")
    private String eventsCsvPath;

    private final List<Event> events = new ArrayList<>();

    @PostConstruct
    void load() {
        List<Event> raw = readCsv(eventsCsvPath);
        rebaseToNow(raw);
        log.info("Loaded {} events from {} (rebased relative to now)", events.size(), eventsCsvPath);
    }

    /** Read every event row using a BufferedReader in try-with-resources. */
    private List<Event> readCsv(String path) {
        List<Event> parsed = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (first) {
                    first = false;
                    if (line.toLowerCase().startsWith("id,")) {
                        continue; // skip header
                    }
                }
                parsed.add(parse(line));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load events CSV from: " + path, e);
        }
        return parsed;
    }

    /**
     * Columns 0-7 are fixed; everything from index 8 on is a tag (the CSV's tag
     * list is unquoted and comma-separated, so a row has a variable field count).
     */
    private Event parse(String line) {
        String[] f = line.split(",");
        if (f.length < 9) {
            throw new IllegalArgumentException("Malformed event row: " + line);
        }
        List<String> tags = new ArrayList<>();
        for (int i = 8; i < f.length; i++) {
            String tag = f[i].trim();
            if (!tag.isEmpty()) {
                tags.add(tag);
            }
        }
        return new Event(
                f[0].trim(),
                f[1].trim(),
                f[2].trim(),
                Double.parseDouble(f[3].trim()),
                Double.parseDouble(f[4].trim()),
                LocalDateTime.parse(f[5].trim()),
                LocalDateTime.parse(f[6].trim()),
                Integer.parseInt(f[7].trim()),
                tags);
    }

    /** Shift all events so the busiest 60-minute block starts now. */
    private void rebaseToNow(List<Event> raw) {
        if (raw.isEmpty()) {
            return;
        }
        LocalDateTime anchor = busiestWindowStart(raw);
        Duration delta = Duration.between(anchor, LocalDateTime.now());
        for (Event e : raw) {
            events.add(new Event(
                    e.getId(), e.getName(), e.getVenueName(),
                    e.getLatitude(), e.getLongitude(),
                    e.getStartTime().plus(delta), e.getEndTime().plus(delta),
                    e.getCapacity(), e.getTags()));
        }
    }

    /** The event start time that maximizes how many events begin within the next hour. */
    private LocalDateTime busiestWindowStart(List<Event> raw) {
        LocalDateTime best = raw.get(0).getStartTime();
        int bestCount = -1;
        for (Event a : raw) {
            LocalDateTime s = a.getStartTime();
            LocalDateTime end = s.plusMinutes(WINDOW_MINUTES);
            int count = 0;
            for (Event b : raw) {
                LocalDateTime t = b.getStartTime();
                if (!t.isBefore(s) && !t.isAfter(end)) {
                    count++;
                }
            }
            if (count > bestCount || (count == bestCount && s.isBefore(best))) {
                bestCount = count;
                best = s;
            }
        }
        return best;
    }

    /**
     * Events within 500 m that start within the next 60 minutes, ranked by
     * {@code proximity*0.4 + urgency*0.3 + tagMatch*0.3}, top 5 first.
     */
    public List<RankedEvent> getNearbyEvents(double lat, double lng, List<String> userTags) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusMinutes(WINDOW_MINUTES);

        List<String> normalizedTags = userTags == null ? Collections.emptyList()
                : userTags.stream()
                        .filter(Objects::nonNull)
                        .map(t -> t.trim().toLowerCase())
                        .filter(t -> !t.isEmpty())
                        .toList();

        List<RankedEvent> ranked = new ArrayList<>();
        for (Event e : events) {
            double distanceMeters =
                    HaversineUtil.distanceKm(lat, lng, e.getLatitude(), e.getLongitude()) * 1000.0;
            if (distanceMeters > RADIUS_METERS) {
                continue;
            }

            LocalDateTime start = e.getStartTime();
            if (start.isBefore(now) || start.isAfter(windowEnd)) {
                continue;
            }
            long minutesUntilStart = Duration.between(now, start).toMinutes();

            double proximity = 1.0 - (distanceMeters / RADIUS_METERS);
            double urgency = 1.0 - (minutesUntilStart / (double) WINDOW_MINUTES);
            double tagMatch = tagMatch(normalizedTags, e.getTags());
            double score = PROXIMITY_WEIGHT * proximity
                    + URGENCY_WEIGHT * urgency
                    + TAG_WEIGHT * tagMatch;

            ranked.add(new RankedEvent(e, round(distanceMeters), minutesUntilStart, round(score)));
        }

        ranked.sort(Comparator.comparingDouble(RankedEvent::score).reversed());
        return ranked.stream().limit(5).toList();
    }

    /** Fraction of the user's tags that the event carries (case-insensitive). */
    private double tagMatch(List<String> userTags, List<String> eventTags) {
        if (userTags.isEmpty()) {
            return 0.0;
        }
        Set<String> lowered = eventTags.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
        long matches = userTags.stream().filter(lowered::contains).count();
        return (double) matches / userTags.size();
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    /** All loaded events (rebased), for diagnostics. */
    public List<Event> getAllEvents() {
        return events;
    }
}
