package com.example.sidehustle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

// Change this to extend BaseActivity instead of FragmentActivity
public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView userName;
    private ShapeableImageView profilePicture;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected int getLayoutResourceId() {
        // Return the layout resource ID that includes the bottom navigation
        return R.layout.activity_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // This calls setContentView with our layout
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();
        
        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // Hide default title
        }
        
        // Initialize views
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        userName = findViewById(R.id.user_name);
        profilePicture = findViewById(R.id.profile_picture);
        
        // Setup ViewPager and TabLayout
        setupViewPager();
        setupTabLayout();
        
        // Load user data
        loadUserData();
    }

    private void setupViewPager() {
        ProfilePagerAdapter adapter = new ProfilePagerAdapter(this);
        viewPager.setAdapter(adapter);
    }
    
    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.personal_details);
                    break;
                case 1:
                    tab.setText(R.string.skills);
                    break;
                case 2:
                    tab.setText(R.string.work_experience);
                    break;
                case 3:
                    tab.setText(R.string.settings);
                    break;
            }
        }).attach();
    }

    private void loadUserData() {
        if (currentUser != null) {
            // Default user info from Firebase Auth
            if (currentUser.getDisplayName() != null) {
                userName.setText(currentUser.getDisplayName());
            }
            
            if (currentUser.getPhotoUrl() != null) {
                Glide.with(this)
                    .load(currentUser.getPhotoUrl())
                    .placeholder(R.drawable.profile_placeholder)
                    .into(profilePicture);
            }
            
            // Get additional info from Firestore
            db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(this::updateUserInterface)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    // Create user document if it doesn't exist yet
                    createNewUserDocument();
                });
        }
    }
    
    private void updateUserInterface(DocumentSnapshot document) {
        if (document.exists()) {
            Map<String, Object> personal = (Map<String, Object>) document.get("Personal");
            if (personal != null) {
                String name = (String) personal.get("name");
                if (name != null && !name.isEmpty()) {
                    userName.setText(name);
                }
            }
        } else {
            // Document doesn't exist yet
            createNewUserDocument();
        }
    }
    
    private void createNewUserDocument() {
        if (currentUser == null) return;
        
        // Create basic user data
        Map<String, Object> userData = new java.util.HashMap<>();
        
        // Personal details
        Map<String, Object> personal = new java.util.HashMap<>();
        personal.put("name", currentUser.getDisplayName() != null ? 
                currentUser.getDisplayName() : "");
        personal.put("email", currentUser.getEmail() != null ? 
                currentUser.getEmail() : "");
        personal.put("phone", "");
        
        userData.put("Personal", personal);
        userData.put("Skills", new java.util.ArrayList<String>());
        
        // Add empty experiences array instead of a map
        userData.put("Experiences", new java.util.ArrayList<>());
        
        // Save to Firestore
        db.collection("Users")
            .document(currentUser.getUid())
            .set(userData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "User document created successfully");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating user document", e);
                Toast.makeText(this, "Error setting up profile", Toast.LENGTH_SHORT).show();
            });
    }
    
    // Add this method to your ProfileActivity class
    public void updateHeaderName(String name) {
        if (userName != null) {
            userName.setText(name);
        }
    }
    
    // Add this method to get the current header name
    public String getHeaderName() {
        if (userName != null) {
            return userName.getText().toString();
        }
        return "";
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // For Up button in ActionBar, also go to HomeActivity
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Always go to HomeActivity when back is pressed
        Intent homeIntent = new Intent(this, HomeActivity.class);
        // Clear the back stack so the user can't navigate back to this activity
        homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(homeIntent);
        finish();
    }
    
    // ViewPager Adapter
    private class ProfilePagerAdapter extends FragmentStateAdapter {
        
        public ProfilePagerAdapter(@NonNull BaseActivity fragmentActivity) {
            super(fragmentActivity);
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new PersonalDetailsFragment();
                case 1:
                    return new SkillsFragment();
                case 2:
                    return new ExperienceFragment();
                case 3:
                    return new SettingsFragment();
                default:
                    return new PersonalDetailsFragment();
            }
        }
        
        @Override
        public int getItemCount() {
            return 4; // Number of tabs
        }
    }
}