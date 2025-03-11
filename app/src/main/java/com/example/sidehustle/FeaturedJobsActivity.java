package com.example.sidehustle;

import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class FeaturedJobsActivity extends BaseActivity {

    private RecyclerView featuredJobsRecyclerView;
    private JobAdapter jobAdapter;
    private List<Job> jobList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set up toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        featuredJobsRecyclerView = findViewById(R.id.featuredJobsRecyclerView);
        featuredJobsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();
        jobList = new ArrayList<>();
        jobAdapter = new JobAdapter(this, jobList);
        featuredJobsRecyclerView.setAdapter(jobAdapter);

        fetchJobsFromFirestore();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_featured_jobs;
    }

    private void fetchJobsFromFirestore() {
        CollectionReference jobsRef = db.collection("Jobs");
        
        // Remove the orderBy clause since there's no timestamp field in your Job model
        jobsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("Firestore", "Error fetching jobs", error);
                    Toast.makeText(FeaturedJobsActivity.this, "Error fetching jobs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Add logging to help debug
                Log.d("FeaturedJobsActivity", "QuerySnapshot received: " + (value != null ? value.size() : "null"));
                
                jobList.clear();
                for (QueryDocumentSnapshot document : value) {
                    try {
                        Job job = document.toObject(Job.class);
                        job.setId(document.getId());
                        jobList.add(job);
                        Log.d("FeaturedJobsActivity", "Added job: " + job.getTitle());
                    } catch (Exception e) {
                        Log.e("FeaturedJobsActivity", "Error converting document to Job", e);
                    }
                }
                
                Log.d("FeaturedJobsActivity", "Final jobList size: " + jobList.size());
                jobAdapter.notifyDataSetChanged();
            }
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