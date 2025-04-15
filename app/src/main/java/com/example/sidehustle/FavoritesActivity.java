package com.example.sidehustle;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NavUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FavoritesActivity extends BaseActivity {

    private static final String TAG = "FavoritesActivity";
    private static final String FAVORITES_PREF = "favorite_jobs";

    private RecyclerView favoritesRecyclerView;
    private JobAdapter jobAdapter;
    private List<Job> favoriteJobs;
    private TextView emptyStateText;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private Gson gson;

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

        // Initialize SharedPreferences and Gson
        sharedPreferences = getSharedPreferences(FAVORITES_PREF, MODE_PRIVATE);
        gson = new Gson();

        // Initialize views
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        progressBar = findViewById(R.id.progressBar);

        // Setup recycler view
        favoritesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        favoriteJobs = new ArrayList<>();
        jobAdapter = new JobAdapter(this, favoriteJobs);
        favoritesRecyclerView.setAdapter(jobAdapter);

        // Load saved jobs
        loadFavoriteJobs();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_favorites;
    }

    private void loadFavoriteJobs() {
        progressBar.setVisibility(View.VISIBLE);
        favoriteJobs.clear();
        jobAdapter.notifyDataSetChanged();

        // Get saved jobs from SharedPreferences
        String savedJobsJson = sharedPreferences.getString("savedJobs", "");
        if (savedJobsJson.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            showEmptyState(getString(R.string.no_saved_jobs));
            return;
        }

        // Convert JSON to list of maps
        Type type = new TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> savedJobsMaps = gson.fromJson(savedJobsJson, type);

        // Defensive: If parsing as list failed, try parsing as single map and wrap in list
        if (savedJobsMaps == null) {
            Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
            Map<String, Object> singleJobMap = gson.fromJson(savedJobsJson, mapType);
            savedJobsMaps = new ArrayList<>();
            if (singleJobMap != null && !singleJobMap.isEmpty()) {
                savedJobsMaps.add(singleJobMap);
            }
        }

        if (savedJobsMaps == null || savedJobsMaps.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            showEmptyState(getString(R.string.no_saved_jobs));
            return;
        }

        // Sort by timestamp (most recent first), handle scientific notation
        Collections.sort(savedJobsMaps, (job1, job2) -> {
            Long timestamp1 = getTimestamp(job1);
            Long timestamp2 = getTimestamp(job2);
            return timestamp2.compareTo(timestamp1);
        });

        // Convert to Job objects
        for (Map<String, Object> jobMap : savedJobsMaps) {
            Job job = new Job();
            job.setId((String) jobMap.get("jobId"));
            job.setTitle((String) jobMap.get("title"));
            job.setCompany((String) jobMap.get("company"));
            job.setLocation((String) jobMap.get("location"));
            job.setSalary((String) jobMap.get("salary"));
            job.setImageUrl((String) jobMap.get("imageUrl"));
            job.setDescription((String) jobMap.get("description"));
            job.setRequirements((String) jobMap.get("requirements"));

            favoriteJobs.add(job);
        }

        progressBar.setVisibility(View.GONE);

        if (favoriteJobs.isEmpty()) {
            showEmptyState(getString(R.string.no_saved_jobs));
        } else {
            jobAdapter.notifyDataSetChanged();
            showJobsList();
        }
    }

    // Helper to safely parse timestamp from map (handles scientific notation)
    private Long getTimestamp(Map<String, Object> job) {
        Object ts = job.get("timestamp");
        if (ts == null) return 0L;
        try {
            if (ts instanceof Number) return ((Number) ts).longValue();
            return Double.valueOf(ts.toString()).longValue();
        } catch (Exception e) {
            return 0L;
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Navigate to HomeActivity instead of just going back
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        // Navigate to HomeActivity instead of calling super.onBackPressed()
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}