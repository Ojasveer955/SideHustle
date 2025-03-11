package com.example.sidehustle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobDetailActivity extends AppCompatActivity {

    private static final String TAG = "JobDetailActivity";
    private static final String FAVORITES_PREF = "favorite_jobs";
    
    // Keys for intent extras
    public static final String EXTRA_JOB_ID = "job_id";
    public static final String EXTRA_JOB_TITLE = "job_title";
    public static final String EXTRA_COMPANY_NAME = "company_name";
    public static final String EXTRA_JOB_LOCATION = "job_location";
    public static final String EXTRA_JOB_SALARY = "job_salary";
    public static final String EXTRA_JOB_IMAGE = "job_image";
    public static final String EXTRA_JOB_DESCRIPTION = "job_description";
    public static final String EXTRA_JOB_REQUIREMENTS = "job_requirements";
    
    private String jobId;
    private FirebaseUser currentUser;
    private boolean isSavingInProgress = false;
    private SharedPreferences sharedPreferences;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Initialize SharedPreferences and Gson
        sharedPreferences = getSharedPreferences(FAVORITES_PREF, MODE_PRIVATE);
        gson = new Gson();
        
        // Initialize Firebase Auth (just for user info)
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // Get intent data
        Intent intent = getIntent();
        if (intent != null) {
            jobId = intent.getStringExtra(EXTRA_JOB_ID);
            String jobTitle = intent.getStringExtra(EXTRA_JOB_TITLE);
            String companyName = intent.getStringExtra(EXTRA_COMPANY_NAME);
            String jobLocation = intent.getStringExtra(EXTRA_JOB_LOCATION);
            String jobSalary = intent.getStringExtra(EXTRA_JOB_SALARY);
            String jobImageUrl = intent.getStringExtra(EXTRA_JOB_IMAGE);
            String jobDescription = intent.getStringExtra(EXTRA_JOB_DESCRIPTION);
            String jobRequirements = intent.getStringExtra(EXTRA_JOB_REQUIREMENTS);
            
            // Set up views with job details
            setupJobDetails(jobTitle, companyName, jobLocation, jobSalary, 
                           jobImageUrl, jobDescription, jobRequirements);
        }
        
        // Set up action buttons
        setupButtons();
    }
    
    private void setupJobDetails(String title, String company, String location, String salary,
                               String imageUrl, String description, String requirements) {
        // Set text values
        TextView jobTitleView = findViewById(R.id.jobTitle);
        TextView companyNameView = findViewById(R.id.companyName);
        TextView jobLocationView = findViewById(R.id.jobLocation);
        TextView jobSalaryView = findViewById(R.id.jobSalary);
        TextView jobDescriptionView = findViewById(R.id.jobDescription);
        TextView jobRequirementsView = findViewById(R.id.jobRequirements);
        
        jobTitleView.setText(title);
        companyNameView.setText(company);
        jobLocationView.setText(location);
        jobSalaryView.setText(salary);
        
        // Set description and requirements or default message
        String descriptionText = description != null && !description.isEmpty() 
                ? description 
                : "No description available for this job.";
        
        String requirementsText = requirements != null && !requirements.isEmpty() 
                ? requirements 
                : "No specific requirements listed.";
                
        jobDescriptionView.setText(descriptionText);
        jobRequirementsView.setText(requirementsText);
        
        // Load company image using Glide
        ImageView companyLogoView = findViewById(R.id.companyLogo);
        if (imageUrl != null && !imageUrl.isEmpty()) {
            String directImageUrl = convertDriveUrlToDirectUrl(imageUrl);
            
            Glide.with(this)
                .load(directImageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model,
                            Target<Drawable> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model,
                            Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        return false;
                    }
                })
                .into(companyLogoView);
        } else {
            companyLogoView.setImageResource(R.drawable.placeholder_image);
        }
    }
    
    private String convertDriveUrlToDirectUrl(String driveUrl) {
        String fileId = "";
        if (driveUrl.contains("drive.google.com/file/d/")) {
            fileId = driveUrl.split("/file/d/")[1].split("/")[0];
        } else if (driveUrl.contains("drive.google.com/open?id=")) {
            fileId = driveUrl.split("open\\?id=")[1];
        }
        
        return "https://lh3.googleusercontent.com/d/" + fileId;
    }
    
    private void setupButtons() {
        Button applyButton = findViewById(R.id.applyButton);
        Button saveButton = findViewById(R.id.saveButton);
        
        applyButton.setOnClickListener(v -> {
            Toast.makeText(this, "Application feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        saveButton.setOnClickListener(v -> {
            // Prevent duplicate operations
            if (isSavingInProgress) {
                return;
            }
            
            isSavingInProgress = true;
            
            if (jobId == null) {
                Toast.makeText(this, "Error: Unable to identify job", Toast.LENGTH_SHORT).show();
                isSavingInProgress = false;
                return;
            }
            
            toggleFavoriteStatus(saveButton);
        });
        
        // Check if job is already saved to set correct button text
        if (jobId != null) {
            checkIfJobIsSaved(saveButton);
        } else {
            saveButton.setText(R.string.save_job);
        }
    }

    private void checkIfJobIsSaved(Button saveButton) {
        if (isJobSaved(jobId)) {
            saveButton.setText(R.string.remove_from_favorites);
        } else {
            saveButton.setText(R.string.save_job);
        }
    }

    private boolean isJobSaved(String jobId) {
        // Get all saved jobs
        List<Map<String, Object>> savedJobs = getSavedJobs();
        
        // Check if job with this ID exists in saved jobs
        for (Map<String, Object> job : savedJobs) {
            if (job.get("jobId").equals(jobId)) {
                return true;
            }
        }
        
        return false;
    }
    
    private List<Map<String, Object>> getSavedJobs() {
        String savedJobsJson = sharedPreferences.getString("savedJobs", "");
        if (savedJobsJson.isEmpty()) {
            return new ArrayList<>();
        }
        
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        return gson.fromJson(savedJobsJson, type);
    }

    private void toggleFavoriteStatus(Button saveButton) {
        if (isJobSaved(jobId)) {
            // Job is already saved, so remove it
            removeJobFromFavorites(saveButton);
        } else {
            // Job is not saved, so add it
            saveJobToFavorites(saveButton);
        }
    }

    private void saveJobToFavorites(Button saveButton) {
        // Get all saved jobs
        List<Map<String, Object>> savedJobs = getSavedJobs();
        
        // Create a map with all job details
        Map<String, Object> favoriteJob = new HashMap<>();
        favoriteJob.put("jobId", jobId);
        favoriteJob.put("timestamp", System.currentTimeMillis());
        
        // Get all job details from the intent
        Intent intent = getIntent();
        favoriteJob.put("title", intent.getStringExtra(EXTRA_JOB_TITLE));
        favoriteJob.put("company", intent.getStringExtra(EXTRA_COMPANY_NAME));
        favoriteJob.put("location", intent.getStringExtra(EXTRA_JOB_LOCATION));
        favoriteJob.put("salary", intent.getStringExtra(EXTRA_JOB_SALARY));
        favoriteJob.put("imageUrl", intent.getStringExtra(EXTRA_JOB_IMAGE));
        favoriteJob.put("description", intent.getStringExtra(EXTRA_JOB_DESCRIPTION));
        favoriteJob.put("requirements", intent.getStringExtra(EXTRA_JOB_REQUIREMENTS));
        
        // Add to saved jobs
        savedJobs.add(favoriteJob);
        
        // Save the updated list
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("savedJobs", gson.toJson(savedJobs));
        editor.apply();
        
        // Update UI
        isSavingInProgress = false;
        Toast.makeText(this, "Job saved to favorites", Toast.LENGTH_SHORT).show();
        saveButton.setText(R.string.remove_from_favorites);
    }

    private void removeJobFromFavorites(Button saveButton) {
        // Get all saved jobs
        List<Map<String, Object>> savedJobs = getSavedJobs();
        
        // Remove job with matching ID
        boolean removed = false;
        for (int i = 0; i < savedJobs.size(); i++) {
            if (savedJobs.get(i).get("jobId").equals(jobId)) {
                savedJobs.remove(i);
                removed = true;
                break;
            }
        }
        
        if (removed) {
            // Save the updated list
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("savedJobs", gson.toJson(savedJobs));
            editor.apply();
            
            // Update UI
            Toast.makeText(this, "Job removed from favorites", Toast.LENGTH_SHORT).show();
            saveButton.setText(R.string.save_job);
        }
        
        isSavingInProgress = false;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}