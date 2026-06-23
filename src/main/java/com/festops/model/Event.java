package com.festops.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * A scheduled festival event (loaded from {@code data/events.csv}).
 */
public class Event {

    private final String id;
    private final String name;
    private final String venueName;
    private final double latitude;
    private final double longitude;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final int capacity;
    private final List<String> tags;

    public Event(String id, String name, String venueName,
                 double latitude, double longitude,
                 LocalDateTime startTime, LocalDateTime endTime,
                 int capacity, List<String> tags) {
        this.id = id;
        this.name = name;
        this.venueName = venueName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getVenueName() {
        return venueName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public List<String> getTags() {
        return tags;
    }

    @Override
    public String toString() {
        return "Event{id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", venueName='" + venueName + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", capacity=" + capacity +
                ", tags=" + tags +
                '}';
    }
}
