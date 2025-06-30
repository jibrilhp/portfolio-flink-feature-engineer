package com.flink.realestate.models;

import java.time.LocalDateTime;

public class MarketEvent {
    private String eventType;
    private String neighborhoodId;
    private String neighborhoodName;
    private double impactScore;
    private String description;
    private LocalDateTime timestamp;

    public MarketEvent() {}
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getNeighborhoodId() { return neighborhoodId; }
    public void setNeighborhoodId(String neighborhoodId) { this.neighborhoodId = neighborhoodId; }
    public String getNeighborhoodName() { return neighborhoodName; }
    public void setNeighborhoodName(String neighborhoodName) { this.neighborhoodName = neighborhoodName; }
    public double getImpactScore() { return impactScore; }
    public void setImpactScore(double impactScore) { this.impactScore = impactScore; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
