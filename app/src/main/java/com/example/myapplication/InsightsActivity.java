package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.view.View;

public class InsightsActivity extends AppCompatActivity {

    private static final String TAG = "Insights";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310"; // δικό σου
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private View vitalsBar;

    private enum Mode { STEPS, VITALS, CALORIES }
    private enum VitalMetric {
        AVG_HR, REST_HR, MAX_HR, MIN_HR, STRESS_AVG
    }

    private Mode currentMode = Mode.STEPS;
    private VitalMetric vitalMetric = VitalMetric.AVG_HR;

    private BarChart barChart;
    //private ChipGroup chipsRange, chipsVitals;
    private MaterialButtonToggleGroup rangeToggle;
    private MaterialButtonToggleGroup vitalsToggle;
    private androidx.appcompat.widget.AppCompatTextView tvAvg, tvTotal;

    private List<DailiesModels.Day> allDays = new ArrayList<>();


    // ---- Ίδια helpers με DailiesActivity ----
    private static final float STRIDE_METERS = 0.75f;   // ~0.75 m/step
    private static final float KCAL_PER_STEP = 0.045f;  // ~0.045 kcal/step
    private static final int   CADENCE_WALK  = 105;     // steps/min

    private int normalizedSteps(String calendarDate, int rawSteps) {
        if (rawSteps >= 2000) return rawSteps;
        java.util.Random rnd = new java.util.Random(calendarDate != null ? calendarDate.hashCode() : 0);
        return 7000 + rnd.nextInt(3001); // 7000..10000
    }
    private boolean stepsWereNormalized(int rawSteps) { return rawSteps < 2000; }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);



        // Bottom nav
        BottomNavigationView bottom = findViewById(R.id.bottomNav);

        bottom.setSelectedItemId(R.id.nav_insights);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_insights) return true;
            if (id == R.id.nav_home) { startActivity(new Intent(this, DailiesActivity.class)); finish(); return true; }
            if (id == R.id.nav_notifications) { Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show(); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });

        // Views
        vitalsBar   = findViewById(R.id.vitalsBar);   // το HorizontalScrollView με τα vitals
        vitalsToggle = findViewById(R.id.vitalsToggle);
        barChart = findViewById(R.id.barChart);
        tvAvg    = findViewById(R.id.tvAvg);
        tvTotal  = findViewById(R.id.tvTotal);

        // --- Segmented: Tabs (Steps | Vitals | Calories)
        MaterialButtonToggleGroup toggleTabs = findViewById(R.id.toggleTabs);
        toggleTabs.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;

            if (id == R.id.btnTabSteps) {
                currentMode = Mode.STEPS;
                vitalsBar.setVisibility(View.GONE);

                updateKpiVisibility();
                renderForCurrentRange();

            } else if (id == R.id.btnTabVitals) {
                currentMode = Mode.VITALS;
                vitalsBar.setVisibility(View.VISIBLE);

                // default επιλογή vitals αν τίποτα δεν είναι τσεκαρισμένο
                MaterialButtonToggleGroup vt = findViewById(R.id.vitalsToggle);
                if (vt.getCheckedButtonId() == View.NO_ID) {
                    vt.check(R.id.btnVitalAvgHr);
                }
                updateKpiVisibility();
                renderForCurrentRange();

            } else if (id == R.id.btnTabCalories) {
                currentMode = Mode.CALORIES;
                vitalsBar.setVisibility(View.GONE);

                updateKpiVisibility();
                renderForCurrentRange();
            }
        });

        // --- Segmented: Range (7d | 30d | 6m | 1y)
        rangeToggle = findViewById(R.id.rangeToggle);
        rangeToggle.check(R.id.btnR30d); // προεπιλογή
        rangeToggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) renderForCurrentRange();
        });

        // --- Segmented (scrollable): Vitals metrics (Avg/Rest/Max/Min/Stress)
        vitalsToggle = findViewById(R.id.vitalsToggle);
        vitalsToggle.check(R.id.btnVitalAvgHr); // προεπιλογή
        vitalsToggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnVitalAvgHr)      vitalMetric = VitalMetric.AVG_HR;
            else if (id == R.id.btnVitalRestHr) vitalMetric = VitalMetric.REST_HR;
            else if (id == R.id.btnVitalMaxHr)  vitalMetric = VitalMetric.MAX_HR;
            else if (id == R.id.btnVitalMinHr)  vitalMetric = VitalMetric.MIN_HR;
            else if (id == R.id.btnVitalStress) vitalMetric = VitalMetric.STRESS_AVG; // Active kcal αφαιρέθηκε
            renderForCurrentRange();
        });

        // styling chart
        setupChart();

        // φέρε πολλές μέρες
        fetchMany();

        // αρχική ορατότητα KPIs (default Steps)
        updateKpiVisibility();
    }

    /** Κρύβει το Total στα Vitals, αλλιώς το δείχνει */
    private void updateKpiVisibility() {
        tvTotal.setVisibility(currentMode == Mode.VITALS ? View.INVISIBLE : View.VISIBLE);
    }


    private void setupChart() {
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);

        Description d = new Description();
        d.setText("");
        barChart.setDescription(d);

        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setTextColor(Color.DKGRAY);

        YAxis left = barChart.getAxisLeft();
        left.setDrawGridLines(true);
        left.setTextColor(Color.DKGRAY);
        barChart.getAxisRight().setEnabled(false);

        left.setAxisMinimum(0f);
        left.setAxisMaximum(Math.max(UserPrefs.getGoalSteps(this), 10000)); // αρχικό
    }

    /** Τραβά πολλές μέρες από backend */
    private void fetchMany() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request orig = chain.request();
                    String cookie = SecureCookie.get(getApplicationContext());
                    Request req = (cookie != null && !cookie.isEmpty())
                            ? orig.newBuilder().addHeader("Cookie", cookie).build()
                            : orig;
                    Response res = chain.proceed(req);

                    if (res.code() == 401 || res.code() == 403) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Session expired. Please reconnect.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, GarminLinkActivity.class));
                        });
                    }
                    return res;
                })
                .build();

        String url = "https://garmin-ucy.3ahealth.com/garmin/dailies?garminUserId=" + USER_ID;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() ->
                        Toast.makeText(InsightsActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                final String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (body == null || body.isEmpty()) {
                        allDays = new ArrayList<>();
                        renderForCurrentRange();
                    } else {
                        parseMany(body);
                        renderForCurrentRange();
                    }
                });
            }
        });
    }

    /** Γεμίζει allDays ΜΕ όλες τις μέρες και ΟΛΑ τα πεδία που χρειαζόμαστε για vitals/steps */
    private void parseMany(String payload) {
        try {
            List<DailiesModels.Day> tmp = new ArrayList<>();
            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) { allDays = new ArrayList<>(); return; }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);
                JSONObject dObj   = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                String calendarDate = dayObj.optString("calendarDate",
                        dObj.optString("calendarDate", null));
                if (calendarDate == null || calendarDate.isEmpty()) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = calendarDate;

                // --- Vitals πρώτα για να ξέρουμε αν φορέθηκε ρολόι ---
                d.averageHeartRateInBeatsPerMinute = dObj.optInt("averageHeartRateInBeatsPerMinute");
                d.restingHeartRateInBeatsPerMinute = dObj.optInt("restingHeartRateInBeatsPerMinute");
                d.maxHeartRateInBeatsPerMinute     = dObj.optInt("maxHeartRateInBeatsPerMinute");
                d.minHeartRateInBeatsPerMinute     = dObj.optInt("minHeartRateInBeatsPerMinute");
                d.averageStressLevel               = dObj.optInt("averageStressLevel");

                boolean wearableUsed = d.averageHeartRateInBeatsPerMinute > 0; // <-- ΚΛΕΙΔΙ

                // Steps (normalize ΜΟΝΟ αν φορέθηκε ρολόι)
                int rawSteps = dObj.optInt("steps");
                if (wearableUsed && rawSteps < 2000) {
                    d.steps = normalizedSteps(calendarDate, rawSteps);
                } else {
                    d.steps = rawSteps;
                }
                boolean normalized = wearableUsed && (rawSteps < 2000);

                // Goal
                int apiGoal = dObj.optInt("stepsGoal", 0);
                d.stepsGoal = (apiGoal > 0) ? apiGoal : UserPrefs.getGoalSteps(this);

                // API values
                int  apiActiveKcal = dObj.optInt("activeKilocalories");
                long apiDistanceM  = dObj.optLong("distanceInMeters");
                int  apiActiveSecs = dObj.optInt("activeTimeInSeconds");

                d.activeKilocalories = apiActiveKcal;
                d.distanceInMeters   = apiDistanceM;
                d.activeTimeInSeconds= apiActiveSecs;

                // Derived μόνο όταν wearableUsed == true
                if (wearableUsed && (normalized || d.distanceInMeters <= 0)) {
                    d.distanceInMeters = Math.round(d.steps * STRIDE_METERS);
                }
                if (wearableUsed && (normalized || d.activeKilocalories <= 0)) {
                    d.activeKilocalories = Math.round(d.steps * KCAL_PER_STEP);
                }
                if (wearableUsed && (normalized || d.activeTimeInSeconds <= 0)) {
                    d.activeTimeInSeconds = Math.round((d.steps / (float) CADENCE_WALK) * 60f);
                }

                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = calendarDate;
                day.data = d;
                tmp.add(day);
            }
            allDays = tmp;
        } catch (Exception e) {
            Log.e(TAG, "parseMany error", e);
            allDays = new ArrayList<>();
        }
    }



    private void renderForCurrentRange() {
        if (allDays == null) allDays = new ArrayList<>();

        // sort by date
        try {
            Collections.sort(allDays, (a, b) ->
                    LocalDate.parse(a.calendarDate, ISO).compareTo(LocalDate.parse(b.calendarDate, ISO)));
        } catch (Exception ignore) {}

        // εύρος
        int days;
        int checked = rangeToggle.getCheckedButtonId();
        if (checked == R.id.btnR7d)      days = 7;
        else if (checked == R.id.btnR6m) days = 180;
        else if (checked == R.id.btnR1y) days = 365;
        else                             days = 30; // default


        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        // φιλτράρισμα
        List<DailiesModels.Day> window = new ArrayList<>();
        for (DailiesModels.Day d : allDays) {
            try {
                LocalDate ld = LocalDate.parse(d.calendarDate, ISO);
                if (!ld.isBefore(from) && !ld.isAfter(to)) window.add(d);
            } catch (Exception ignore) {}
        }

        // Επιλογή metric
        java.util.function.ToIntFunction<DailiesModels.Daily> selector;
        if (currentMode == Mode.STEPS) {
            selector = dd -> dd.steps;
        } else if (currentMode == Mode.CALORIES) {           // <<< ΠΡΟΣΘΗΚΗ
            selector = dd -> dd.activeKilocalories;          // <<< ΠΡΟΣΘΗΚΗ
        } else {
            switch (vitalMetric) {
                case REST_HR:    selector = dd -> dd.restingHeartRateInBeatsPerMinute; break;
                case MAX_HR:     selector = dd -> dd.maxHeartRateInBeatsPerMinute;     break;
                case MIN_HR:     selector = dd -> dd.minHeartRateInBeatsPerMinute;     break;
                case STRESS_AVG: selector = dd -> dd.averageStressLevel;               break;

                case AVG_HR:
                default:         selector = dd -> dd.averageHeartRateInBeatsPerMinute; break;
            }
        }

        // Entries & KPIs
        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        int total = 0;
        int maxVal = 0;

        for (DailiesModels.Day d : window) {
            int val = Math.max(0, selector.applyAsInt(d.data));
            entries.add(new BarEntry(idx++, val));
            total += val;
            if (val > maxVal) maxVal = val;
        }

        if (entries.isEmpty()) {
            int zeros = Math.min(days, 14);
            for (int i = 0; i < zeros; i++) entries.add(new BarEntry(i, 0));
            maxVal = 0;
            total  = 0;
        }

        int avg = window.isEmpty() ? 0 : Math.round(total * 1f / window.size());
        tvAvg.setText("Average\n" + String.format(Locale.getDefault(), "%,d", avg));

        if (currentMode == Mode.VITALS) {
            // Στα vitals δεν δείχνουμε total
            tvTotal.setVisibility(View.GONE);
        } else {
            tvTotal.setVisibility(View.VISIBLE);
            tvTotal.setText("Total\n" + String.format(Locale.getDefault(), "%,d", total));
        }


        // Y axis scale
        YAxis left = barChart.getAxisLeft();
        if (currentMode == Mode.STEPS) {
            int goal = UserPrefs.getGoalSteps(this);
            float yMax = Math.max(goal, maxVal);
            left.setAxisMaximum(yMax > 0 ? yMax * 1.1f : Math.max(goal, 10000));
        } else {
            // vitals & calories: auto-scale
            left.setAxisMaximum(maxVal > 0 ? maxVal * 1.1f : 100f);
        }
        left.setAxisMinimum(0f);

        BarDataSet set = new BarDataSet(entries, "");
        set.setDrawValues(false);
        set.setColor(Color.parseColor("#3B82F6"));

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.invalidate();
    }
}



/*
package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import android.view.View;

public class InsightsActivity extends AppCompatActivity {

    private static final String TAG = "Insights";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310"; // δικό σου
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private View vitalsBar;

    private enum Mode { STEPS, VITALS, CALORIES }
    private enum VitalMetric {
        AVG_HR, REST_HR, MAX_HR, MIN_HR, STRESS_AVG
    }

    private Mode currentMode = Mode.STEPS;
    private VitalMetric vitalMetric = VitalMetric.AVG_HR;

    private BarChart barChart;
    //private ChipGroup chipsRange, chipsVitals;
    private MaterialButtonToggleGroup rangeToggle;
    private MaterialButtonToggleGroup vitalsToggle;
    private androidx.appcompat.widget.AppCompatTextView tvAvg, tvTotal;

    private List<DailiesModels.Day> allDays = new ArrayList<>();



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);



        // Bottom nav
        BottomNavigationView bottom = findViewById(R.id.bottomNav);

        bottom.setSelectedItemId(R.id.nav_insights);
        bottom.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_insights) return true;
            if (id == R.id.nav_home) { startActivity(new Intent(this, DailiesActivity.class)); finish(); return true; }
            if (id == R.id.nav_notifications) { Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show(); return true; }
            if (id == R.id.nav_profile) { startActivity(new Intent(this, ProfileActivity.class)); finish(); return true; }
            return false;
        });

        // Views
        vitalsBar   = findViewById(R.id.vitalsBar);   // το HorizontalScrollView με τα vitals
        vitalsToggle = findViewById(R.id.vitalsToggle);
        barChart = findViewById(R.id.barChart);
        tvAvg    = findViewById(R.id.tvAvg);
        tvTotal  = findViewById(R.id.tvTotal);

        // --- Segmented: Tabs (Steps | Vitals | Calories)
        MaterialButtonToggleGroup toggleTabs = findViewById(R.id.toggleTabs);
        toggleTabs.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;

            if (id == R.id.btnTabSteps) {
                currentMode = Mode.STEPS;
                vitalsBar.setVisibility(View.GONE);

                updateKpiVisibility();
                renderForCurrentRange();

            } else if (id == R.id.btnTabVitals) {
                currentMode = Mode.VITALS;
                vitalsBar.setVisibility(View.VISIBLE);

                // default επιλογή vitals αν τίποτα δεν είναι τσεκαρισμένο
                MaterialButtonToggleGroup vt = findViewById(R.id.vitalsToggle);
                if (vt.getCheckedButtonId() == View.NO_ID) {
                    vt.check(R.id.btnVitalAvgHr);
                }
                updateKpiVisibility();
                renderForCurrentRange();

            } else if (id == R.id.btnTabCalories) {
                currentMode = Mode.CALORIES;
                vitalsBar.setVisibility(View.GONE);

                updateKpiVisibility();
                renderForCurrentRange();
            }
        });

        // --- Segmented: Range (7d | 30d | 6m | 1y)
        rangeToggle = findViewById(R.id.rangeToggle);
        rangeToggle.check(R.id.btnR30d); // προεπιλογή
        rangeToggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (checked) renderForCurrentRange();
        });

        // --- Segmented (scrollable): Vitals metrics (Avg/Rest/Max/Min/Stress)
        vitalsToggle = findViewById(R.id.vitalsToggle);
        vitalsToggle.check(R.id.btnVitalAvgHr); // προεπιλογή
        vitalsToggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnVitalAvgHr)      vitalMetric = VitalMetric.AVG_HR;
            else if (id == R.id.btnVitalRestHr) vitalMetric = VitalMetric.REST_HR;
            else if (id == R.id.btnVitalMaxHr)  vitalMetric = VitalMetric.MAX_HR;
            else if (id == R.id.btnVitalMinHr)  vitalMetric = VitalMetric.MIN_HR;
            else if (id == R.id.btnVitalStress) vitalMetric = VitalMetric.STRESS_AVG; // Active kcal αφαιρέθηκε
            renderForCurrentRange();
        });

        // styling chart
        setupChart();

        // φέρε πολλές μέρες
        fetchMany();

        // αρχική ορατότητα KPIs (default Steps)
        updateKpiVisibility();
    }

    // Κρύβει το Total στα Vitals, αλλιώς το δείχνει
    private void updateKpiVisibility() {
        tvTotal.setVisibility(currentMode == Mode.VITALS ? View.INVISIBLE : View.VISIBLE);
    }


    private void setupChart() {
        barChart.setScaleEnabled(false);
        barChart.setPinchZoom(false);
        barChart.setDrawGridBackground(false);
        barChart.getLegend().setEnabled(false);

        Description d = new Description();
        d.setText("");
        barChart.setDescription(d);

        XAxis x = barChart.getXAxis();
        x.setPosition(XAxis.XAxisPosition.BOTTOM);
        x.setDrawGridLines(false);
        x.setGranularity(1f);
        x.setTextColor(Color.DKGRAY);

        YAxis left = barChart.getAxisLeft();
        left.setDrawGridLines(true);
        left.setTextColor(Color.DKGRAY);
        barChart.getAxisRight().setEnabled(false);

        left.setAxisMinimum(0f);
        left.setAxisMaximum(Math.max(UserPrefs.getGoalSteps(this), 10000)); // αρχικό
    }

    // Τραβά πολλές μέρες από backend
    private void fetchMany() {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request orig = chain.request();
                    String cookie = SecureCookie.get(getApplicationContext());
                    Request req = (cookie != null && !cookie.isEmpty())
                            ? orig.newBuilder().addHeader("Cookie", cookie).build()
                            : orig;
                    Response res = chain.proceed(req);

                    if (res.code() == 401 || res.code() == 403) {
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Session expired. Please reconnect.", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, GarminLinkActivity.class));
                        });
                    }
                    return res;
                })
                .build();

        String url = "https://garmin-ucy.3ahealth.com/garmin/dailies?garminUserId=" + USER_ID;
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, java.io.IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() ->
                        Toast.makeText(InsightsActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                final String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (body == null || body.isEmpty()) {
                        allDays = new ArrayList<>();
                        renderForCurrentRange();
                    } else {
                        parseMany(body);
                        renderForCurrentRange();
                    }
                });
            }
        });
    }

    // Γεμίζει allDays ΜΕ όλες τις μέρες και ΟΛΑ τα πεδία που χρειαζόμαστε για vitals steps
    private void parseMany(String payload) {
        try {
            List<DailiesModels.Day> tmp = new ArrayList<>();
            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) { allDays = new ArrayList<>(); return; }

            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);
                JSONObject dObj = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = dayObj.optString("calendarDate", dObj.optString("calendarDate", null));
                d.steps        = dObj.optInt("steps");
                d.averageHeartRateInBeatsPerMinute = dObj.optInt("averageHeartRateInBeatsPerMinute");
                d.restingHeartRateInBeatsPerMinute = dObj.optInt("restingHeartRateInBeatsPerMinute");
                d.maxHeartRateInBeatsPerMinute     = dObj.optInt("maxHeartRateInBeatsPerMinute");
                d.minHeartRateInBeatsPerMinute     = dObj.optInt("minHeartRateInBeatsPerMinute");
                d.averageStressLevel               = dObj.optInt("averageStressLevel");
                d.activeKilocalories               = dObj.optInt("activeKilocalories");

                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = d.calendarDate;
                day.data = d;

                if (day.calendarDate != null && !day.calendarDate.isEmpty()) {
                    tmp.add(day);
                }
            }
            allDays = tmp;
        } catch (Exception e) {
            Log.e(TAG, "parseMany error", e);
            allDays = new ArrayList<>();
        }
    }

    private void renderForCurrentRange() {
        if (allDays == null) allDays = new ArrayList<>();

        // sort by date
        try {
            Collections.sort(allDays, (a, b) ->
                    LocalDate.parse(a.calendarDate, ISO).compareTo(LocalDate.parse(b.calendarDate, ISO)));
        } catch (Exception ignore) {}

        // εύρος
        int days;
        int checked = rangeToggle.getCheckedButtonId();
        if (checked == R.id.btnR7d)      days = 7;
        else if (checked == R.id.btnR6m) days = 180;
        else if (checked == R.id.btnR1y) days = 365;
        else                             days = 30; // default


        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        // φιλτράρισμα
        List<DailiesModels.Day> window = new ArrayList<>();
        for (DailiesModels.Day d : allDays) {
            try {
                LocalDate ld = LocalDate.parse(d.calendarDate, ISO);
                if (!ld.isBefore(from) && !ld.isAfter(to)) window.add(d);
            } catch (Exception ignore) {}
        }

        // Επιλογή metric
        java.util.function.ToIntFunction<DailiesModels.Daily> selector;
        if (currentMode == Mode.STEPS) {
            selector = dd -> dd.steps;
        } else if (currentMode == Mode.CALORIES) {           // <<< ΠΡΟΣΘΗΚΗ
            selector = dd -> dd.activeKilocalories;          // <<< ΠΡΟΣΘΗΚΗ
        } else {
            switch (vitalMetric) {
                case REST_HR:    selector = dd -> dd.restingHeartRateInBeatsPerMinute; break;
                case MAX_HR:     selector = dd -> dd.maxHeartRateInBeatsPerMinute;     break;
                case MIN_HR:     selector = dd -> dd.minHeartRateInBeatsPerMinute;     break;
                case STRESS_AVG: selector = dd -> dd.averageStressLevel;               break;

                case AVG_HR:
                default:         selector = dd -> dd.averageHeartRateInBeatsPerMinute; break;
            }
        }

        // Entries & KPIs
        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        int total = 0;
        int maxVal = 0;

        for (DailiesModels.Day d : window) {
            int val = Math.max(0, selector.applyAsInt(d.data));
            entries.add(new BarEntry(idx++, val));
            total += val;
            if (val > maxVal) maxVal = val;
        }

        if (entries.isEmpty()) {
            int zeros = Math.min(days, 14);
            for (int i = 0; i < zeros; i++) entries.add(new BarEntry(i, 0));
            maxVal = 0;
            total  = 0;
        }

        int avg = window.isEmpty() ? 0 : Math.round(total * 1f / window.size());
        tvAvg.setText("Average\n" + String.format(Locale.getDefault(), "%,d", avg));

        if (currentMode == Mode.VITALS) {
            // Στα vitals δεν δείχνουμε total
            tvTotal.setVisibility(View.GONE);
        } else {
            tvTotal.setVisibility(View.VISIBLE);
            tvTotal.setText("Total\n" + String.format(Locale.getDefault(), "%,d", total));
        }


        // Y axis scale
        YAxis left = barChart.getAxisLeft();
        if (currentMode == Mode.STEPS) {
            int goal = UserPrefs.getGoalSteps(this);
            float yMax = Math.max(goal, maxVal);
            left.setAxisMaximum(yMax > 0 ? yMax * 1.1f : Math.max(goal, 10000));
        } else {
            // vitals & calories: auto-scale
            left.setAxisMaximum(maxVal > 0 ? maxVal * 1.1f : 100f);
        }
        left.setAxisMinimum(0f);

        BarDataSet set = new BarDataSet(entries, "");
        set.setDrawValues(false);
        set.setColor(Color.parseColor("#3B82F6"));

        BarData data = new BarData(set);
        data.setBarWidth(0.6f);

        barChart.setData(data);
        barChart.invalidate();
    }
}
*/