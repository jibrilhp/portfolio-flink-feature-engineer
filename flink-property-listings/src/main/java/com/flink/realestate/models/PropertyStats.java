package com.flink.realestate.models;

public class PropertyStats {
    private String neighborhoodId;
    private long count;
    private double avgPrice;
    private double minPrice;
    private double maxPrice;
    private double avgSquareFeet;

    public PropertyStats() {}
    public PropertyStats(String neighborhoodId, long count, double avgPrice, 
                        double minPrice, double maxPrice, double avgSquareFeet) {
        this.neighborhoodId = neighborhoodId;
        this.count = count;
        this.avgPrice = avgPrice;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.avgSquareFeet = avgSquareFeet;
    }
    public String getNeighborhoodId() { return neighborhoodId; }
    public void setNeighborhoodId(String neighborhoodId) { this.neighborhoodId = neighborhoodId; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public double getAvgPrice() { return avgPrice; }
    public void setAvgPrice(double avgPrice) { this.avgPrice = avgPrice; }
    public double getMinPrice() { return minPrice; }
    public void setMinPrice(double minPrice) { this.minPrice = minPrice; }
    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }
    public double getAvgSquareFeet() { return avgSquareFeet; }
    public void setAvgSquareFeet(double avgSquareFeet) { this.avgSquareFeet = avgSquareFeet; }
    @Override
    public String toString() {
        return String.format("PropertyStats{{neighborhood='%s', count=%d, avgPrice=%.2f, priceRange=[%.2f-%.2f]}}",
                neighborhoodId, count, avgPrice, minPrice, maxPrice);
    }
}
