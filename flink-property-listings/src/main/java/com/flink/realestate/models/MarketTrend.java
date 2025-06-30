package com.flink.realestate.models;

import java.time.LocalDateTime;

public class MarketTrend {
    private String neighborhoodId;
    private double avgImpactScore;
    private long eventCount;
    private String trendDirection;
    private LocalDateTime timestamp;

    public MarketTrend() {}
    public MarketTrend(String neighborhoodId, double avgImpactScore, long eventCount,
                      String trendDirection, LocalDateTime timestamp) {
        this.neighborhoodId = neighborhoodId;
        this.avgImpactScore = avgImpactScore;
        this.eventCount = eventCount;
        this.trendDirection = trendDirection;
        this.timestamp = timestamp;
    }
    public String getNeighborhoodId() { return neighborhoodId; }
    public void setNeighborhoodId(String neighborhoodId) { this.neighborhoodId = neighborhoodId; }
    public double getAvgImpactScore() { return avgImpactScore; }
    public void setAvgImpactScore(double avgImpactScore) { this.avgImpactScore = avgImpactScore; }
    public long getEventCount() { return eventCount; }
    public void setEventCount(long eventCount) { this.eventCount = eventCount; }
    public String getTrendDirection() { return trendDirection; }
    public void setTrendDirection(String trendDirection) { this.trendDirection = trendDirection; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    @Override
    public String toString() {
        return String.format("MarketTrend{{neighborhood='%s', trend='%s', impact=%.2f, events=%d}}",
                neighborhoodId, trendDirection, avgImpactScore, eventCount);
    }
}
