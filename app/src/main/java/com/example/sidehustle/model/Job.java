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
    
    // No-argument constructor required for Firestore
    public Job() {}

    // Constructor with all fields
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

    // Getters
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getSalary() { return salary; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public String getRequirements() { return requirements; }

    // Setters - These were missing and causing the errors
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setCompany(String company) { this.company = company; }
    public void setLocation(String location) { this.location = location; }
    public void setSalary(String salary) { this.salary = salary; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setRequirements(String requirements) { this.requirements = requirements; }
}