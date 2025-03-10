package com.example.sidehustle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;

import java.util.ArrayList;
import java.util.List;
import androidx.annotation.Nullable;

public class HomeActivity extends BaseActivity {
    private RecyclerView featuredJobsRecyclerView;
    private JobAdapter jobAdapter;
    private List<Job> jobList;
    private FirebaseFirestore db;
    private CardView searchBarContainer;
    private EditText searchBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove setContentView(R.layout.activity_home); - it's already called in BaseActivity

        Toolbar toolbar = findViewById(R.id.toolbar);
        
        // Hide default title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // Get current user and set welcome message
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String displayName = currentUser != null ? currentUser.getDisplayName() : "User";
        if (displayName == null || displayName.isEmpty()) {
            displayName = "User";
        }

        // Find and set the custom title
        TextView toolbarTitle = toolbar.findViewById(R.id.toolbarTitle);
        if (toolbarTitle != null) {
            toolbarTitle.setText(getString(R.string.welcome_message, displayName));
        }

        // Initialize RecyclerView
        featuredJobsRecyclerView = findViewById(R.id.featuredJobsRecyclerView);
        featuredJobsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();
        jobList = new ArrayList<>();
        jobAdapter = new JobAdapter(this, jobList);
        featuredJobsRecyclerView.setAdapter(jobAdapter);

        // Fetch Jobs from Firestore
        fetchJobsFromFirestore();
        
        // Setup search bar click to navigate to search activity
        searchBarContainer = findViewById(R.id.searchBarContainer);
        searchBar = findViewById(R.id.searchBar);
        
        View.OnClickListener searchClickListener = v -> {
            Intent intent = new Intent(HomeActivity.this, SearchActivity.class);
            startActivity(intent);
            // No need to finish() here as we want the user to be able to come back
        };
        
        // Apply click listener to both the container and the edit text
        searchBarContainer.setOnClickListener(searchClickListener);
        searchBar.setOnClickListener(searchClickListener);
        // Disable actual editing of the search bar on home screen
        searchBar.setFocusable(false);
        searchBar.setClickable(true);

        // Initialize the temporary logout button
        Button tempLogoutButton = findViewById(R.id.tempLogoutButton);
        if (tempLogoutButton != null) {
            tempLogoutButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logout();
                }
            });
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_home;
    }

    private void fetchJobsFromFirestore() {
        CollectionReference jobsRef = db.collection("Jobs");

        jobsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, 
                    @Nullable com.google.firebase.firestore.FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("Firestore", "Error fetching jobs", error);
                    Toast.makeText(HomeActivity.this, "Error fetching jobs", Toast.LENGTH_SHORT).show();
                    return;
                }

                jobList.clear();
                for (QueryDocumentSnapshot document : value) {
                    Job job = document.toObject(Job.class);
                    // Set the document ID as the job ID
                    job.setId(document.getId());
                    jobList.add(job);
                }
                jobAdapter.notifyDataSetChanged();
            }
        });
    }

    private void logout() {
        // Clear login state
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isLoggedIn", false);
        editor.apply();

        // Navigate to LoginActivity
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
