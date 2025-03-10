package com.example.sidehustle;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class SearchActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Remove setContentView(R.layout.activity_search); - it's already called in BaseActivity
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_search;
    }
}