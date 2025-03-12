package com.example.sidehustle;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {
    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Check if the activity wants to handle its own content view
        if (!skipSetContentView()) {
            setContentView(getLayoutResourceId());
        }
        setupBottomNavigation();
    }

    // Add this method to allow activities to skip the automatic setContentView
    protected boolean skipSetContentView() {
        return false;
    }

    protected abstract int getLayoutResourceId();

    private void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView == null) {
            return; // Skip if bottomNavigationView is not in this layout
        }
        
        // Set the correct item as selected first
        if (this instanceof HomeActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (this instanceof SearchActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_search);
        } else if (this instanceof FavoritesActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_favorites);
        } else if (this instanceof ProfileActivity) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }

        // Then set up the listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home && !isCurrentActivity(HomeActivity.class)) {
                startActivity(new Intent(this, HomeActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_search && !isCurrentActivity(SearchActivity.class)) {
                startActivity(new Intent(this, SearchActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_favorites && !isCurrentActivity(FavoritesActivity.class)) {
                startActivity(new Intent(this, FavoritesActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_profile && !isCurrentActivity(ProfileActivity.class)) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
                return true;
            }
            return true; // Return true to indicate the item should be selected
        });
    }

    private <T> boolean isCurrentActivity(Class<T> activityClass) {
        return getClass().equals(activityClass);
    }
}