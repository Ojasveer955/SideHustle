package com.example.sidehustle;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sidehustle.adapter.JobAdapter;
import com.example.sidehustle.model.Job;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends BaseActivity {
    
    private EditText searchBar;
    private ImageView clearSearchIcon;
    private RecyclerView searchResultsRecyclerView;
    private LinearLayout noResultsContainer;
    private ProgressBar searchProgressBar;
    private BottomNavigationView bottomNavigationView;
    
    private FirebaseFirestore db;
    private JobAdapter jobAdapter;
    private List<Job> searchResults;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search); // Ensure this line is present to set the content view
        
        // Set toolbar title
        TextView toolbarTitle = findViewById(R.id.toolbarTitle);
        if (toolbarTitle != null) {
            toolbarTitle.setText(R.string.search);
        }
        
        // Initialize views
        searchBar = findViewById(R.id.searchBar);
        clearSearchIcon = findViewById(R.id.clearSearchIcon);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        noResultsContainer = findViewById(R.id.noResultsContainer);
        searchProgressBar = findViewById(R.id.searchProgressBar);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        
        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        
        // Setup RecyclerView
        searchResults = new ArrayList<>();
        jobAdapter = new JobAdapter(this, searchResults);
        searchResultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        searchResultsRecyclerView.setAdapter(jobAdapter);
        
        // Setup search functionality
        setupSearch();
        
        // Auto-focus on search bar and show keyboard
        searchBar.requestFocus();
    }
    
    private void setupSearch() {
        // Handle IME search action
        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = searchBar.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                }
                return true;
            }
            return false;
        });
        
        // Setup text changed listener for search bar
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Show/hide clear button based on text
                clearSearchIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                
                // If text is cleared, reset search results
                if (s.length() == 0) {
                    clearSearchResults();
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // Setup clear button
        clearSearchIcon.setOnClickListener(v -> {
            searchBar.setText("");
            clearSearchResults();
        });

        // Hide bottom navigation when search bar is focused
        searchBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                bottomNavigationView.setVisibility(View.GONE);
            } else {
                bottomNavigationView.setVisibility(View.VISIBLE);
            }
        });
    }
    
    private void performSearch(String query) {
        // Show progress indicator
        searchProgressBar.setVisibility(View.VISIBLE);
        noResultsContainer.setVisibility(View.GONE);
        searchResults.clear();
        jobAdapter.notifyDataSetChanged();
        
        // Create query to search in job titles, companies, and descriptions
        CollectionReference jobsRef = db.collection("Jobs");
        
        // Create a lowercase version of the query for case-insensitive search
        String lowercaseQuery = query.toLowerCase();
        
        jobsRef.get().addOnCompleteListener(task -> {
            searchProgressBar.setVisibility(View.GONE);
            
            if (task.isSuccessful()) {
                for (DocumentSnapshot document : task.getResult()) {
                    Job job = document.toObject(Job.class);
                    // Set ID manually as it's not automatically populated
                    job.setId(document.getId());
                    
                    // Check if job matches search query (case-insensitive)
                    String title = job.getTitle() != null ? job.getTitle().toLowerCase() : "";
                    String company = job.getCompany() != null ? job.getCompany().toLowerCase() : "";
                    String description = job.getDescription() != null ? job.getDescription().toLowerCase() : "";
                    String location = job.getLocation() != null ? job.getLocation().toLowerCase() : "";
                    
                    if (title.contains(lowercaseQuery) || 
                            company.contains(lowercaseQuery) || 
                            description.contains(lowercaseQuery) ||
                            location.contains(lowercaseQuery)) {
                        searchResults.add(job);
                    }
                }
                
                // Update UI based on search results
                if (searchResults.isEmpty()) {
                    noResultsContainer.setVisibility(View.VISIBLE);
                    searchResultsRecyclerView.setVisibility(View.GONE);
                } else {
                    noResultsContainer.setVisibility(View.GONE);
                    searchResultsRecyclerView.setVisibility(View.VISIBLE);
                    jobAdapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(SearchActivity.this, "Error searching jobs: " + 
                        task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void clearSearchResults() {
        searchResults.clear();
        jobAdapter.notifyDataSetChanged();
        noResultsContainer.setVisibility(View.GONE);
    }
    
    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_search;
    }

    @Override
    public void onBackPressed() {
        // Navigate back to the previous activity
        super.onBackPressed();
    }
}