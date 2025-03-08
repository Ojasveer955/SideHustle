package com.example.sidehustle;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            navigateToNextScreen();
        }, 3000); // Splash screen duration
    }

    private void navigateToNextScreen() {
        // Check if user is logged in
        boolean isLoggedIn = checkIfUserIsLoggedIn(); // Implement this based on your auth system
        
        // Navigate to appropriate screen
        Intent intent;
        if (isLoggedIn) {
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish(); // Close SplashActivity so user can't go back to it
    }

    private boolean checkIfUserIsLoggedIn() {
        // Example using SharedPreferences
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        return prefs.getBoolean("isLoggedIn", false);
    }
}
