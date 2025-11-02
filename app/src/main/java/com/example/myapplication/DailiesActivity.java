package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DailiesActivity extends AppCompatActivity {
    public static final String EXTRA_JSON = "extra_json";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dailies_activity);

        // --- Bottom bar setup ---
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setSelectedItemId(R.id.nav_home);
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();

                if (id == R.id.nav_insights) {
                    Toast.makeText(this, "Insights", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_home) {
                    return true; // ήδη εδώ
                } else if (id == R.id.nav_notifications) {
                    Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_profile) {
                    Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });
        }

        // Προσαρμογή για gesture navigation ώστε το pill να μη πέφτει κάτω από το system bar
        MaterialCardView barCard = findViewById(R.id.bottomBarCard);
        if (barCard != null) {
            ViewCompat.setOnApplyWindowInsetsListener(barCard, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
                return insets;
            });
        }
        // --- /Bottom bar setup ---

        RecyclerView rv = findViewById(R.id.rvDays);
        DailyAdapter adapter = new DailyAdapter();
        rv.setAdapter(adapter);

        String payload = getIntent().getStringExtra(EXTRA_JSON);
        if (payload == null || payload.trim().isEmpty()) {
            Toast.makeText(this, "No data", Toast.LENGTH_SHORT).show();
            adapter.submit(new ArrayList<>());
            return;
        }
        adapter.submit(parseDays(payload));
    }

    private List<DailiesModels.Day> parseDays(String payload) {
        List<DailiesModels.Day> out = new ArrayList<>();
        try {
            // υπολόγισε "σήμερα" στη ζώνη του κινητού
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return out;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);

                String calendarDate = dayObj.optString("calendarDate", null);
                boolean isToday = false;

                if (calendarDate != null && !calendarDate.isEmpty()) {
                    // Σύγκριση με το πεδίο calendarDate (μορφή YYYY-MM-DD)
                    isToday = LocalDate.parse(calendarDate, ISO).equals(today);
                } else {
                    // Fallback: από startTimeInSeconds + startTimeOffsetInSeconds
                    JSONObject dTmp = dayObj.optJSONObject("data");
                    if (dTmp != null) {
                        long start = dTmp.optLong("startTimeInSeconds", -1L);
                        int offset = dTmp.optInt("startTimeOffsetInSeconds", 0);
                        if (start > 0) {
                            Instant instant = Instant.ofEpochSecond(start).minusSeconds(offset);
                            LocalDate ld = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
                            isToday = ld.equals(today);
                            if (calendarDate == null) {
                                calendarDate = ld.format(ISO);
                            }
                        }
                    }
                }

                if (!isToday) continue;

                JSONObject dObj = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = dObj.optString("calendarDate", calendarDate);
                d.steps = dObj.optInt("steps");
                d.stepsGoal = dObj.optInt("stepsGoal", 10000);
                d.activeKilocalories = dObj.optInt("activeKilocalories");
                d.distanceInMeters = dObj.optLong("distanceInMeters");
                d.averageHeartRateInBeatsPerMinute = dObj.optInt("averageHeartRateInBeatsPerMinute");
                d.activeTimeInSeconds = dObj.optInt("activeTimeInSeconds");
                d.averageStressLevel = dObj.optInt("averageStressLevel");
                d.maxStressLevel = dObj.optInt("maxStressLevel");

                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = (calendarDate != null) ? calendarDate : d.calendarDate;
                day.data = d;

                out.add(day);
            }
        } catch (JSONException e) {
            Log.e("DailiesActivity", "JSON parse error", e);
        }
        return out;
    }
}
