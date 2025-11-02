package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.button.MaterialButton;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirst, etLast, etHeight, etWeight;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Mini tabs
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleTabs);
        MaterialButton btnProfile = findViewById(R.id.btnTabProfile);
        MaterialButton btnGoals   = findViewById(R.id.btnTabGoals);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && checkedId == R.id.btnTabGoals) {
                startActivity(new Intent(this, GoalsActivity.class));
                finish();
            }
        });

        // Inputs
        etFirst  = findViewById(R.id.etFirst);
        etLast   = findViewById(R.id.etLast);
        etHeight = findViewById(R.id.etHeight);
        etWeight = findViewById(R.id.etWeight);

        // Prefill από UserPrefs
        etFirst.setText(UserPrefs.getFirstName(this));
        etLast.setText(UserPrefs.getLastName(this));
        etHeight.setText(String.valueOf(UserPrefs.getHeightCm(this)));
        etWeight.setText(String.valueOf(UserPrefs.getWeightKg(this)));

        // Save
        findViewById(R.id.btnSaveProfile).setOnClickListener(v -> {
            String first = etFirst.getText().toString().trim();
            String last  = etLast.getText().toString().trim();
            int height   = parseIntSafe(etHeight.getText().toString().trim(), 170);
            int weight   = parseIntSafe(etWeight.getText().toString().trim(), 70);

            UserPrefs.setFirstName(this, first);
            UserPrefs.setLastName(this, last);
            UserPrefs.setHeightCm(this, height);
            UserPrefs.setWeightKg(this, weight);

            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });

        // Bottom bar
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.nav_profile);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_insights) {
                Toast.makeText(this, "Insights", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_home) {
                startActivity(new Intent(this, DailiesActivity.class)); finish(); return true;
            } else if (id == R.id.nav_notifications) {
                Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
