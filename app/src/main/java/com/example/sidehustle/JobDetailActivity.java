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
import android.util.Log;
import android.view.View;

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
    private boolean isSavingInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_detail);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        
        // Check if authentication token needs refreshing
        if (currentUser != null) {
            currentUser.getIdToken(true)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Token refreshed successfully
                        Log.d(TAG, "Auth token refreshed");
                    } else {
                        Log.e(TAG, "Error refreshing auth token", task.getException());
                    }
                });
        }
        
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
            Toast.makeText(this, "Application feature coming soon!", Toast.LENGTH_SHORT).show();
        });
        
        saveButton.setOnClickListener(v -> {
            // Verify user is authenticated
            if (currentUser == null) {
                Toast.makeText(this, "Please sign in to save jobs", Toast.LENGTH_SHORT).show();
                // Optional: redirect to login
                // Intent intent = new Intent(this, LoginActivity.class);
                // startActivity(intent);
                return;
            }
            
            if (jobId == null) {
                Toast.makeText(this, "Error: Unable to identify job", Toast.LENGTH_SHORT).show();
                return;
            }
            
            toggleFavoriteStatus(saveButton);
        });
        
        // Check if job is already saved to set correct button text
        if (currentUser != null && jobId != null) {
            checkIfJobIsSaved(saveButton);
        } else {
            // Ensure button shows correct state even when not logged in
            saveButton.setText(R.string.save_job);
        }
    }

    private void checkIfJobIsSaved(Button saveButton) {
        String userId = currentUser.getUid();
        
        db.collection("users")
            .document(userId)
            .collection("favoriteJobs")
            .document(jobId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    saveButton.setText(R.string.remove_from_favorites);
                } else {
                    saveButton.setText(R.string.save_job);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking if job is saved", e);
            });
    }

    private void toggleFavoriteStatus(Button saveButton) {
        // Prevent duplicate operations
        if (isSavingInProgress) {
            return;
        }
        
        isSavingInProgress = true;
        String userId = currentUser.getUid();
        
        db.collection("users")
            .document(userId)
            .collection("favoriteJobs")
            .document(jobId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    // Job is already saved, so remove it
                    removeJobFromFavorites(saveButton);
                } else {
                    // Job is not saved, so add it
                    saveJobToFavorites(saveButton);
                }
            })
            .addOnFailureListener(e -> {
                isSavingInProgress = false; // Reset flag on failure
                Toast.makeText(JobDetailActivity.this, 
                        "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void saveJobToFavorites(Button saveButton) {
        String userId = currentUser.getUid();
        
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
        
        // Show progress if operation takes long
        Toast.makeText(JobDetailActivity.this, "Saving job...", Toast.LENGTH_SHORT).show();
        
        // Ensure the users collection and document exist first
        db.collection("users").document(userId).get()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Error checking user document", task.getException());
                    Toast.makeText(JobDetailActivity.this, 
                        "Error saving job: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Create user document if it doesn't exist
                if (!task.getResult().exists()) {
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", userId);
                    userData.put("email", currentUser.getEmail());
                    userData.put("createdAt", System.currentTimeMillis());
                    
                    db.collection("users").document(userId).set(userData)
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Error creating user document", e);
                            Toast.makeText(JobDetailActivity.this,
                                "Error creating user profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
                
                // Now save the favorite job
                db.collection("users")
                    .document(userId)
                    .collection("favoriteJobs")
                    .document(jobId)
                    .set(favoriteJob)
                    .addOnSuccessListener(aVoid -> {
                        isSavingInProgress = false; // Reset flag on success
                        Toast.makeText(JobDetailActivity.this, 
                                "Job saved to favorites", Toast.LENGTH_SHORT).show();
                        saveButton.setText(R.string.remove_from_favorites);
                    })
                    .addOnFailureListener(e -> {
                        isSavingInProgress = false; // Reset flag on failure
                        Log.e(TAG, "Error saving job to favorites", e);
                        Toast.makeText(JobDetailActivity.this, 
                                "Failed to save job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            });
    }

    private void removeJobFromFavorites(Button saveButton) {
        String userId = currentUser.getUid();
        
        db.collection("users")
            .document(userId)
            .collection("favoriteJobs")
            .document(jobId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                isSavingInProgress = false; // Reset flag on success
                Toast.makeText(JobDetailActivity.this, 
                        "Job removed from favorites", Toast.LENGTH_SHORT).show();
                saveButton.setText(R.string.save_job);
            })
            .addOnFailureListener(e -> {
                isSavingInProgress = false; // Reset flag on failure
                Toast.makeText(JobDetailActivity.this, 
                        "Failed to remove job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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