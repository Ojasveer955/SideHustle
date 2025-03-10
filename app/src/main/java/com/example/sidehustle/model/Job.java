package com.example.sidehustle.model;

public class Job {
    private String id;
    private String title;
    private String company;
    private String location;
    private String salary;
    private String imageUrl;
    private String description;
    private String requirements;

    // Empty constructor required for Firestore
    public Job() {}

    public Job(String id, String title, String company, String location, String salary, String imageUrl, String description, String requirements) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.imageUrl = imageUrl;
        this.description = description;
        this.requirements = requirements;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getSalary() { return salary; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; } 
    public String getRequirements() { return requirements; }
}