package com.flink.realestate.models;

import java.time.LocalDateTime;

public class PropertyListing {
    private String propertyId;
    private String neighborhoodId;
    private String propertyTypeId;
    private double price;
    private int bedrooms;
    private double bathrooms;
    private int squareFeet;
    private int lotSizeSquareFeet;
    private int yearBuilt;
    private double latitude;
    private double longitude;
    private LocalDateTime timestamp;
    public PropertyListing() {}
    public PropertyListing(String propertyId, String neighborhoodId, String propertyTypeId,
                          double price, int bedrooms, double bathrooms, int squareFeet,
                          int lotSizeSquareFeet, int yearBuilt, double latitude, 
                          double longitude, LocalDateTime timestamp) {
        this.propertyId = propertyId;
        this.neighborhoodId = neighborhoodId;
        this.propertyTypeId = propertyTypeId;
        this.price = price;
        this.bedrooms = bedrooms;
        this.bathrooms = bathrooms;
        this.squareFeet = squareFeet;
        this.lotSizeSquareFeet = lotSizeSquareFeet;
        this.yearBuilt = yearBuilt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
    // Getters & Setters
    public String getPropertyId() { return propertyId; }
    public void setPropertyId(String propertyId) { this.propertyId = propertyId; }
    public String getNeighborhoodId() { return neighborhoodId; }
    public void setNeighborhoodId(String neighborhoodId) { this.neighborhoodId = neighborhoodId; }
    public String getPropertyTypeId() { return propertyTypeId; }
    public void setPropertyTypeId(String propertyTypeId) { this.propertyTypeId = propertyTypeId; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }
    public double getBathrooms() { return bathrooms; }
    public void setBathrooms(double bathrooms) { this.bathrooms = bathrooms; }
    public int getSquareFeet() { return squareFeet; }
    public void setSquareFeet(int squareFeet) { this.squareFeet = squareFeet; }
    public int getLotSizeSquareFeet() { return lotSizeSquareFeet; }
    public void setLotSizeSquareFeet(int lotSizeSquareFeet) { this.lotSizeSquareFeet = lotSizeSquareFeet; }
    public int getYearBuilt() { return yearBuilt; }
    public void setYearBuilt(int yearBuilt) { this.yearBuilt = yearBuilt; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    @Override
    public String toString() {
        return String.format("PropertyListing{{id='%s', neighborhood='%s', price=%.2f, bedrooms=%d, sqft=%d}}",
                propertyId, neighborhoodId, price, bedrooms, squareFeet);
    }
}
