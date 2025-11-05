package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;

public class GoalsActivity extends AppCompatActivity {

    private EditText etSteps, etCal, etDist, etWater;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goals);

        // mini tabs
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleTabs);
        toggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && checkedId == R.id.btnTabProfile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            }
        });

        // fields
        etSteps = findViewById(R.id.etStepsGoal);
        etCal   = findViewById(R.id.etCalGoal);
        etDist  = findViewById(R.id.etDistGoal);
        etWater = findViewById(R.id.etWaterGoal);

        // only-int filter
        InputFilter digitsOnly = new InputFilter() {
            public CharSequence filter(CharSequence src, int start, int end,
                                       Spanned dst, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isDigit(src.charAt(i))) return "";
                }
                return null;
            }
        };
        etSteps.setFilters(new InputFilter[]{digitsOnly});
        etCal.setFilters(new InputFilter[]{digitsOnly});
        etDist.setFilters(new InputFilter[]{digitsOnly});
        etWater.setFilters(new InputFilter[]{digitsOnly});

        // prefill from prefs
        etSteps.setText(String.valueOf(UserPrefs.getGoalSteps(this)));
        etCal.setText(String.valueOf(UserPrefs.getGoalCalories(this)));
        etDist.setText(String.valueOf(UserPrefs.getGoalDistanceKm(this)));
        etWater.setText(String.valueOf(UserPrefs.getGoalWater(this)));

        // attach +/- handlers (min, max, step)
        attachStepper(findViewById(R.id.btnStepsMinus), findViewById(R.id.btnStepsPlus), etSteps, 1000, 50000, 500);
        attachStepper(findViewById(R.id.btnCalMinus),   findViewById(R.id.btnCalPlus),   etCal,   100,  10000, 50);
        attachStepper(findViewById(R.id.btnDistMinus),  findViewById(R.id.btnDistPlus),  etDist,  1,    1000, 1);
        attachStepper(findViewById(R.id.btnWaterMinus), findViewById(R.id.btnWaterPlus), etWater, 1,    30,   1);

        // save
        findViewById(R.id.btnSaveGoals).setOnClickListener(v -> {
            int steps = parseInt(etSteps, UserPrefs.getGoalSteps(this));
            int cal   = parseInt(etCal,   UserPrefs.getGoalCalories(this));
            int dist  = parseInt(etDist,  UserPrefs.getGoalDistanceKm(this));
            int water = parseInt(etWater, UserPrefs.getGoalWater(this));

            UserPrefs.setGoalSteps(this, steps);
            UserPrefs.setGoalCalories(this, cal);
            UserPrefs.setGoalDistanceKm(this, dist);
            UserPrefs.setGoalWater(this, water);

            //11.4.2025 add
            // κρατάμε και σε WaterPrefs για συνέπεια (αν το χρησιμοποιείς αλλού)
            WaterPrefs.setGoal(this, water);

            Toast.makeText(this, "Goals saved", Toast.LENGTH_SHORT).show();
        });

        // bottom bar
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        bottom.setSelectedItemId(R.id.nav_profile); // ίδιο tab group
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_insights) {
                startActivity(new Intent(this, InsightsActivity.class));
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

    private void attachStepper(ImageButton minus, ImageButton plus, EditText et,
                               int min, int max, int step) {
        minus.setOnClickListener(v -> {
            int val = parseInt(et, min);
            val = Math.max(min, val - step);
            et.setText(String.valueOf(val));
        });
        plus.setOnClickListener(v -> {
            int val = parseInt(et, min);
            val = Math.min(max, val + step);
            et.setText(String.valueOf(val));
        });
    }

    private int parseInt(EditText et, int def) {
        try { return Integer.parseInt(et.getText().toString().trim()); }
        catch (Exception e) { return def; }
    }
}
