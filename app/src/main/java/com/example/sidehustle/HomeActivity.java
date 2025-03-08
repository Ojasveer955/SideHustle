package com.example.sidehustle;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;
import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                // Handle home action
                return true;
            } else if (itemId == R.id.nav_search) {
                // Handle search action
                return true;
            } else if (itemId == R.id.nav_favorites) {
                // Handle favorites action
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Handle profile action
                return true;
            } else {
                return false;
            }
        });

        RecyclerView featuredJobsRecyclerView = findViewById(R.id.featuredJobsRecyclerView);
        featuredJobsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Create sample data
        List<Job> featuredJobs = new ArrayList<>();
        featuredJobs.add(new Job("Android Developer", "Google", "Mountain View, CA", "$120k - $150k/year", R.drawable.ic_google));
        featuredJobs.add(new Job("iOS Developer", "Apple", "Cupertino, CA", "$130k - $160k/year", R.drawable.ic_apple));
        featuredJobs.add(new Job("Web Developer", "Facebook", "Menlo Park, CA", "$110k - $140k/year", R.drawable.ic_facebook));
        featuredJobs.add(new Job("UX Designer", "Amazon", "Seattle, WA", "$100k - $130k/year", R.drawable.ic_amazon));

        // Set adapter for the RecyclerView
        JobAdapter jobAdapter = new JobAdapter(featuredJobs);
        featuredJobsRecyclerView.setAdapter(jobAdapter);

        // Add this in your Activity's onCreate method
        Log.d("ResourceCheck", "IC_HOME resource exists: " + 
            (getResources().getDrawable(R.drawable.ic_home) != null));
    }
}
