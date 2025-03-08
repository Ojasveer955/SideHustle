package com.example.sidehustle.model;

public class Job {
    private String title;
    private String company;
    private String location;
    private String salary;
    private String imageUrl; // Changed to String for Firestore image URLs

    // Empty constructor required for Firestore
    public Job() {}

    public Job(String title, String company, String location, String salary, String imageUrl) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.imageUrl = imageUrl;
    }

    // Getters and setters
    public String getTitle() { return title; }    
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getSalary() { return salary; }
    public String getImageUrl() { return imageUrl; }
}