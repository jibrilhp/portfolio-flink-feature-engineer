package com.flink.realestate.models;

import java.time.LocalDateTime;

public class PriceAlert {
    private String propertyId;
    private double changePercentage;
    private String changeReason;
    private String alertLevel;
    private LocalDateTime timestamp;

    public PriceAlert() {}
    public PriceAlert(String propertyId, double changePercentage, String changeReason, 
                     String alertLevel, LocalDateTime timestamp) {
        this.propertyId = propertyId;
        this.changePercentage = changePercentage;
        this.changeReason = changeReason;
        this.alertLevel = alertLevel;
        this.timestamp = timestamp;
    }
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public double getChangePercentage() { return changePercentage; }
    public void setChangePercentage(double changePercentage) { this.changePercentage = changePercentage; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public String getAlertLevel() { return alertLevel; }
    public void setAlertLevel(String alertLevel) { this.alertLevel = alertLevel; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    @Override
    public String toString() {
        return String.format("PriceAlert{{property='%s', change=%.2f%%, level='%s', reason='%s'}}",
                propertyId, changePercentage, alertLevel, changeReason);
    }
}
