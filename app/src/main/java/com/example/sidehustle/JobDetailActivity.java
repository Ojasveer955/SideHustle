package com.example.sidehustle;

import android.content.Intent;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class JobDetailActivity extends AppCompatActivity {

    private static final String TAG = "JobDetailActivity";
    
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
    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        
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
            // TODO: Implement job application logic
            Toast.makeText(this, "Application feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        saveButton.setOnClickListener(v -> {
            if (currentUser != null && jobId != null) {
                saveJobToFavorites();
            } else {
                Toast.makeText(this, "You must be logged in to save jobs", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void saveJobToFavorites() {
        String userId = currentUser.getUid();
        
        // Create a map for the favorite job entry
        Map<String, Object> favoriteJob = new HashMap<>();
        favoriteJob.put("jobId", jobId);
        favoriteJob.put("timestamp", System.currentTimeMillis());
        
        // Add to Firestore
        db.collection("users")
            .document(userId)
            .collection("favoriteJobs")
            .document(jobId)
            .set(favoriteJob)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(JobDetailActivity.this, 
                        "Job saved to favorites", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(JobDetailActivity.this, 
                        "Failed to save job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
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