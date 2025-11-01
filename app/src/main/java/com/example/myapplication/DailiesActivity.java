package com.example.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DailiesActivity extends AppCompatActivity {
    public static final String EXTRA_JSON = "extra_json";

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // we reuse a "fragment_" layout as an Activity content view—totally fine
        setContentView(R.layout.fragment_dailies_activity);

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
            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return out;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);
                String calendarDate = dayObj.optString("calendarDate");

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
                day.calendarDate = calendarDate;
                day.data = d;
                out.add(day);
            }
        } catch (JSONException e) {
            Log.e("DailiesActivity", "JSON parse error", e);
        }
        return out;
    }
}
