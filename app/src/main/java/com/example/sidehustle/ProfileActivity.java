package com.example.sidehustle;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager2.widget.ViewPager2;
import android.widget.TextView;

public class ProfileActivity extends BaseActivity {
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TextView userName;
    private ShapeableImageView profilePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove setContentView(R.layout.activity_profile); - it's already called in BaseActivity

        // Initialize views
        userName = findViewById(R.id.user_name);
        profilePicture = findViewById(R.id.profile_picture);
        tabLayout = findViewById(R.id.profile_tabs);
        viewPager = findViewById(R.id.viewpager);

        // Set user data
        setupUserData();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }

    private void setupUserData() {
        // TODO: Implement user data setup
    }
}