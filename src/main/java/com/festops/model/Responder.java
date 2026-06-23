package com.festops.model;

/**
 * A field responder who can be dispatched to incidents. Position is held as
 * latitude/longitude so the {@code Dispatcher} can score by proximity.
 */
public class Responder {

    private String id;
    private String name;
    private String skill;
    private double lat;
    private double lng;
    private volatile boolean available;
    private int currentLoad;

    public Responder() {
    }

    public Responder(String id, String name, String skill,
                     double lat, double lng, boolean available, int currentLoad) {
        this.id = id;
        this.name = name;
        this.skill = skill;
        this.lat = lat;
        this.lng = lng;
        this.available = available;
        this.currentLoad = currentLoad;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkill() {
        return skill;
    }

    public void setSkill(String skill) {
        this.skill = skill;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public synchronized int getCurrentLoad() {
        return currentLoad;
    }

    public synchronized void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    /** Atomically increment the responder's load (used when a dispatch is assigned). */
    public synchronized void incrementLoad() {
        this.currentLoad++;
    }
}
