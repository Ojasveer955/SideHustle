package com.example.sidehustle;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
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

public class HomeActivity extends AppCompatActivity {
    private RecyclerView featuredJobsRecyclerView;
    private JobAdapter jobAdapter;
    private List<Job> jobList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) return true;
            else if (itemId == R.id.nav_search) return true;
            else if (itemId == R.id.nav_favorites) return true;
            else if (itemId == R.id.nav_profile) return true;
            else return false;
        });

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
    }

    private void fetchJobsFromFirestore() {
        CollectionReference jobsRef = db.collection("Jobs");

        jobsRef.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable com.google.firebase.firestore.FirebaseFirestoreException error) {
                if (error != null) {
                    Log.e("Firestore", "Error fetching jobs", error);
                    Toast.makeText(HomeActivity.this, "Error fetching jobs", Toast.LENGTH_SHORT).show();
                    return;
                }

                jobList.clear();
                for (QueryDocumentSnapshot document : value) {
                    Job job = document.toObject(Job.class);
                    jobList.add(job);
                }
                jobAdapter.notifyDataSetChanged();
            }
        });
    }
}
