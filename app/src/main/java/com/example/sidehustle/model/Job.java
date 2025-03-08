package com.example.sidehustle.model;

public class Job {
    private String title;
    private String company;
    private String location;
    private String salary;
    private int imageResourceId;

    public Job(String title, String company, String location, String salary, int imageResourceId) {
        this.title = title;
        this.company = company;
        this.location = location;
        this.salary = salary;
        this.imageResourceId = imageResourceId;
    }

    // Getters
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getLocation() { return location; }
    public String getSalary() { return salary; }
    public int getImageResourceId() { return imageResourceId; }
}