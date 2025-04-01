package com.darilink.models;

import java.util.List;

public class Offer {
    private String id;
    private String title;
    private String description;
    private double area;
    private double rent;
    private int floorNumber;
    private int numBedrooms;
    private int numBathrooms;
    private String agentId;
    private List<String> images;
    private String address;
    private String city;
    private String country;
    private boolean isAvailable;
    private String propertyType; // Apartment, House, Villa, etc.
    private List<String> amenities;
    private long createdAt;
    private long updatedAt;

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public double getRent() { return rent; }
    public void setRent(double rent) { this.rent = rent; }

    public int getFloorNumber() { return floorNumber; }
    public void setFloorNumber(int floorNumber) { this.floorNumber = floorNumber; }

    public int getNumBedrooms() { return numBedrooms; }
    public void setNumBedrooms(int numBedrooms) { this.numBedrooms = numBedrooms; }

    public int getNumBathrooms() { return numBathrooms; }
    public void setNumBathrooms(int numBathrooms) { this.numBathrooms = numBathrooms; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }

    public String getPropertyType() { return propertyType; }
    public void setPropertyType(String propertyType) { this.propertyType = propertyType; }

    public List<String> getAmenities() { return amenities; }
    public void setAmenities(List<String> amenities) { this.amenities = amenities; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
