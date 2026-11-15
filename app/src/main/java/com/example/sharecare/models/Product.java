package com.example.sharecare.models;

public class Product {

    private String id;
    private String name;
    private String category;
    private String address;
    private String description;
    private String status;
    private String ownerId;
    private String ownerName;
    private String imageUrl;
    private int imageRes;
    private double distanceKm = -1;
    private boolean placeholder = false;

    public Product() {
    }

    public Product(
            String id,
            String name,
            String category,
            String address,
            String description,
            String status,
            String ownerId,
            String ownerName,
            double distanceKm
    ) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.address = address;
        this.description = description;
        this.status = status;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.distanceKm = distanceKm;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getImageRes() {
        return imageRes;
    }

    public void setImageRes(int imageRes) {
        this.imageRes = imageRes;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public boolean isAvailable() {
        return "AVAILABLE".equals(status);
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
    }
}
