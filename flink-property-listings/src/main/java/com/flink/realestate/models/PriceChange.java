package com.flink.realestate.models;

import java.time.LocalDateTime;

public class PriceChange {
    private String propertyId;
    private double oldPrice;
    private double newPrice;
    private double changeAmount;
    private double changePercentage;
    private String changeReason;
    private LocalDateTime timestamp;

    public PriceChange() {}
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public double getOldPrice() { return oldPrice; }
    public void setOldPrice(double oldPrice) { this.oldPrice = oldPrice; }
    public double getNewPrice() { return newPrice; }
    public void setNewPrice(double newPrice) { this.newPrice = newPrice; }
    public double getChangeAmount() { return changeAmount; }
    public void setChangeAmount(double changeAmount) { this.changeAmount = changeAmount; }
    public double getChangePercentage() { return changePercentage; }
    public void setChangePercentage(double changePercentage) { this.changePercentage = changePercentage; }
    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String changeReason) { this.changeReason = changeReason; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    @Override
    public String toString() {
        return String.format("PriceChange{{property='%s', change=%.2f%%, reason='%s'}}",
                propertyId, changePercentage, changeReason);
    }
}
