package com.example.sidehustle;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Gravity; // Added import
import android.view.LayoutInflater; // Added import
import android.view.View; // Added import
import android.view.ViewGroup; // Added import
import android.widget.ImageView; // Added import
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable; // Added import
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions; // Added import
import com.bumptech.glide.signature.ObjectKey;   // Added import
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.util.Map;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView userName;
    private ShapeableImageView profilePicture; // Assuming this is the main profile picture in the header
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        // Initialize views from the layout
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        userName = findViewById(R.id.user_name); // ID for the TextView showing the user's name
        profilePicture = findViewById(R.id.profile_picture); // ID for the ShapeableImageView showing the profile picture

        // Setup ViewPager and TabLayout
        setupViewPager();
        setupTabLayout();

        // Load user data initially
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
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                userName.setText(currentUser.getDisplayName());
            } else {
                 userName.setText(getString(R.string.not_available)); // Or some default
            }

            // Load profile picture (prioritize local, then Google, then placeholder)
            updateProfilePictureFromLocal(); // Try loading local first

            // Get additional info from Firestore
            db.collection("Users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(this::updateUserInterface)
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data from Firestore", e);
                    // Attempt to create a new document if loading fails (might indicate it doesn't exist)
                    createNewUserDocument();
                });
        } else {
            Log.w(TAG, "currentUser is null in loadUserData");
            // Handle case where user is not logged in (e.g., redirect to login)
            userName.setText(getString(R.string.not_available));
            updateProfilePictureToPlaceholder();
        }
    }

    private void updateUserInterface(DocumentSnapshot document) {
        if (document.exists()) {
            Log.d(TAG, "Firestore document found for user.");
            Map<String, Object> personal = (Map<String, Object>) document.get("Personal");
            if (personal != null) {
                String name = (String) personal.get("name");
                // Update name from Firestore if it's valid and different from current Auth name
                if (name != null && !name.isEmpty() && !name.equals(userName.getText().toString())) {
                    Log.d(TAG, "Updating username from Firestore: " + name);
                    userName.setText(name);
                } else if (userName.getText().toString().equals(getString(R.string.not_available)) && name != null && !name.isEmpty()) {
                    // Update if current name is "N/A" and Firestore has a valid name
                    userName.setText(name);
                }
            } else {
                 Log.w(TAG, "Firestore document exists but 'Personal' map is null.");
            }
        } else {
            Log.w(TAG, "Firestore document does not exist for user: " + currentUser.getUid());
            // Document doesn't exist, try creating it
            createNewUserDocument();
        }
    }

    private void createNewUserDocument() {
        if (currentUser == null) {
            Log.e(TAG, "Cannot create document, currentUser is null");
            return;
        }
        Log.d(TAG, "Attempting to create new user document in Firestore.");

        Map<String, Object> userData = new java.util.HashMap<>();
        Map<String, Object> personal = new java.util.HashMap<>();
        String displayName = currentUser.getDisplayName();
        String email = currentUser.getEmail();

        personal.put("name", (displayName != null && !displayName.isEmpty()) ? displayName : "");
        personal.put("email", (email != null && !email.isEmpty()) ? email : "");
        personal.put("phone", ""); // Default empty phone

        userData.put("Personal", personal);
        // Initialize other sections if needed
        // userData.put("Skills", new java.util.ArrayList<String>());
        // userData.put("Experiences", new java.util.ArrayList<>());

        db.collection("Users")
            .document(currentUser.getUid())
            .set(userData) // Use set() to create or overwrite
            .addOnSuccessListener(aVoid -> Log.d(TAG, "User document created/updated successfully"))
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating/updating user document", e);
                Toast.makeText(this, "Error setting up profile data", Toast.LENGTH_SHORT).show();
            });
    }

    // Method called by fragments to update the header name
    public void updateHeaderName(String name) {
        if (userName != null && name != null) {
            Log.d(TAG, "Updating header name to: " + name);
            userName.setText(name);
        } else {
             Log.w(TAG, "Cannot update header name, TextView is null or name is null");
        }
    }

    // Method for fragments to get the current header name
    public String getHeaderName() {
        if (userName != null) {
            return userName.getText().toString();
        }
        return ""; // Return empty string if TextView is null
    }

    // Method called by fragments to update the header profile picture from local file
    public void updateProfilePictureFromLocal() {
        Log.d(TAG, "updateProfilePictureFromLocal() called.");
        File file = new File(getFilesDir(), "profile_pic.jpg");
        // Use the ID of the ImageView in this activity's layout
        // *** Ensure R.id.profile_picture is the correct ID for the header ImageView ***
        ImageView headerImageView = findViewById(R.id.profile_picture);

        if (headerImageView == null) {
             Log.e(TAG, "headerImageView (profile_picture) is null! Check the ID in activity_profile.xml.");
             return;
        }

        if (file.exists() && file.length() > 0) {
            Log.d(TAG, "Local file found. Loading into header ImageView.");
            RequestOptions options = new RequestOptions()
                .signature(new ObjectKey(file.lastModified())) // Bust Glide cache
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop(); // Apply circle crop if needed

            Glide.with(this)
                 .load(Uri.fromFile(file))
                 .apply(options)
                 .into(headerImageView);
            Log.d(TAG, "Glide load initiated for header from local file.");
        } else {
            Log.w(TAG, "Local file not found or empty in updateProfilePictureFromLocal. Checking Google photo.");
            // If local file doesn't exist, try loading Google photo or placeholder
            updateProfilePictureFromGoogle(currentUser != null ? currentUser.getPhotoUrl() : null);
        }
    }

    // Method called by fragments to update the header profile picture from Google URL
    public void updateProfilePictureFromGoogle(Uri googlePhotoUrl) {
        Log.d(TAG, "updateProfilePictureFromGoogle called.");
        ImageView headerImageView = findViewById(R.id.profile_picture); // Use the correct ID
        if (headerImageView == null) {
             Log.e(TAG, "headerImageView (profile_picture) is null!");
             return;
        }

        if (googlePhotoUrl != null) {
             Log.d(TAG, "Loading Google photo into header ImageView: " + googlePhotoUrl);
             RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop(); // Apply circle crop if needed

             Glide.with(this)
                .load(googlePhotoUrl)
                .apply(options)
                .into(headerImageView);
        } else {
             Log.d(TAG, "Google photo URL is null. Loading placeholder.");
             updateProfilePictureToPlaceholder(); // Fallback to placeholder
        }
    }

    // Method called by fragments to set the header profile picture to placeholder
    public void updateProfilePictureToPlaceholder() {
        Log.d(TAG, "updateProfilePictureToPlaceholder called.");
        ImageView headerImageView = findViewById(R.id.profile_picture); // Use the correct ID
        if (headerImageView != null) {
            Log.d(TAG, "Setting header ImageView to placeholder.");
            // Use Glide to load placeholder for consistency
            RequestOptions options = new RequestOptions()
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_placeholder)
                .circleCrop(); // Apply circle crop if needed
            Glide.with(this)
                 .load(R.drawable.profile_placeholder)
                 .apply(options)
                 .into(headerImageView);
            // headerImageView.setImageResource(R.drawable.profile_placeholder); // Alternative direct way
        } else {
             Log.e(TAG, "headerImageView (profile_picture) is null!");
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle Toolbar back button press
        if (item.getItemId() == android.R.id.home) {
            // Navigate back like the system back button when on the first tab
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Check if the ViewPager is currently showing the first fragment (PersonalDetailsFragment at position 0)
        if (viewPager != null && viewPager.getCurrentItem() == 0) {
            // If it's the first tab, finish the activity and go back to HomeActivity
            Log.d(TAG, "Back pressed on first tab, navigating to HomeActivity.");
            Intent homeIntent = new Intent(this, HomeActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); // Go back to existing HomeActivity
            startActivity(homeIntent);
            finish(); // Finish ProfileActivity
        } else if (viewPager != null) {
            // If it's not the first tab, navigate the ViewPager back to the first tab
            Log.d(TAG, "Back pressed on non-first tab, navigating ViewPager to first tab.");
            viewPager.setCurrentItem(0);
        }
         else {
            // Fallback: If viewPager is null or something unexpected happens, perform default back press
            Log.w(TAG, "Back pressed but ViewPager is null, performing default back action.");
            super.onBackPressed();
        }
    }

    // Adapter for the ViewPager2
    private class ProfilePagerAdapter extends FragmentStateAdapter {

        // Define the number of tabs/fragments
        private static final int NUM_TABS = 4;

        public ProfilePagerAdapter(@NonNull ProfileActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            Log.d(TAG, "Creating fragment for position: " + position);
            switch (position) {
                case 0:
                    return new PersonalDetailsFragment();
                case 1:
                    // TODO: Replace with your actual Skills Fragment class
                    // return new SkillsFragment();
                    Log.w(TAG, "SkillsFragment not implemented yet, returning placeholder.");
                    return new PlaceholderFragment(); // Use a placeholder for now
                case 2:
                    // TODO: Replace with your actual Experience Fragment class
                    // return new ExperienceFragment();
                    Log.w(TAG, "ExperienceFragment not implemented yet, returning placeholder.");
                    return new PlaceholderFragment(); // Use a placeholder for now
                case 3:
                    // TODO: Replace with your actual Settings Fragment class
                    // return new SettingsFragment();
                    Log.w(TAG, "SettingsFragment not implemented yet, returning placeholder.");
                    return new PlaceholderFragment(); // Use a placeholder for now
                default:
                    Log.e(TAG, "Invalid position requested in ProfilePagerAdapter: " + position);
                    return new PersonalDetailsFragment(); // Fallback to default
            }
        }

        @Override
        public int getItemCount() {
            return NUM_TABS;
        }
    }

    // Simple placeholder fragment for unimplemented tabs
    public static class PlaceholderFragment extends Fragment {
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            TextView textView = new TextView(getContext());
            textView.setText("Coming Soon");
            textView.setGravity(Gravity.CENTER);
            return textView;
        }
    }
}