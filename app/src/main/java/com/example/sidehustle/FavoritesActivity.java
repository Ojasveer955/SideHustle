package com.example.sidehustle;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends BaseActivity {
    
    private static final String TAG = "FavoritesActivity";
    private RecyclerView favoritesRecyclerView;
    private JobAdapter jobAdapter;
    private List<Job> favoriteJobs;
    private FirebaseFirestore db;
    private FirebaseUser currentUser;
    private TextView emptyStateText;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set up toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        
        // Set toolbar title
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.favorites);
        }
        
        // Initialize views
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);
        
        // Setup recycler view
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteJobs = new ArrayList<>();
        jobAdapter = new JobAdapter(this, favoriteJobs);
        favoritesRecyclerView.setAdapter(jobAdapter);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        // Load saved jobs
        loadFavoriteJobs();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_favorites;
    }
    
    private void loadFavoriteJobs() {
        if (currentUser == null) {
            showEmptyState("Please sign in to see your favorite jobs");
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        favoriteJobs.clear();
        jobAdapter.notifyDataSetChanged();
        
        String userId = currentUser.getUid();
        db.collection("users")
            .document(userId)
            .collection("favoriteJobs")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnCompleteListener(task -> {
                progressBar.setVisibility(View.GONE);
                
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        // Create a Job object directly from the saved data
                        Job job = new Job();
                        job.setId(document.getString("jobId"));
                        job.setTitle(document.getString("title"));
                        job.setCompany(document.getString("company"));
                        job.setLocation(document.getString("location"));
                        job.setSalary(document.getString("salary"));
                        job.setImageUrl(document.getString("imageUrl"));
                        job.setDescription(document.getString("description"));
                        job.setRequirements(document.getString("requirements"));
                        
                        favoriteJobs.add(job);
                    }
                    
                    jobAdapter.notifyDataSetChanged();
                    
                    if (favoriteJobs.isEmpty()) {
                        showEmptyState("No favorite jobs found");
                    } else {
                        showJobsList();
                    }
                } else {
                    showEmptyState("Error loading favorite jobs");
                    Log.d(TAG, "Error getting documents: ", task.getException());
                }
            });
    }
    
    private void showEmptyState(String message) {
        favoritesRecyclerView.setVisibility(View.GONE);
        emptyStateText.setText(message);
        emptyStateText.setVisibility(View.VISIBLE);
    }
    
    private void showJobsList() {
        emptyStateText.setVisibility(View.GONE);
        favoritesRecyclerView.setVisibility(View.VISIBLE);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload favorites when returning to this screen
        // This ensures the list updates if a job was un-favorited from detail view
        loadFavoriteJobs();
    }
}