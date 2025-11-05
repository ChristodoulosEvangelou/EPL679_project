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

public class InsightsActivity extends AppCompatActivity {

    private static final String TAG = "Insights";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310"; // δικό σου

    private BarChart barChart;
    private ChipGroup chips;
    private androidx.appcompat.widget.AppCompatTextView tvAvg, tvTotal;

    private List<DailiesModels.Day> allDays = new ArrayList<>();
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
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

        // Tabs (προς το παρόν μόνο Steps)
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleTabs);
        toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnTabSteps) {
                renderForCurrentRange();
            } else {
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
                g.check(R.id.btnTabSteps);
            }
        });

        barChart = findViewById(R.id.barChart);
        tvAvg = findViewById(R.id.tvAvg);
        tvTotal = findViewById(R.id.tvTotal);
        chips = findViewById(R.id.chipsRange);

        // Προεπιλογή: 30 μέρες
        ((Chip) findViewById(R.id.chip30d)).setChecked(true);

        setupChart();

        // Listeners στα chips
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> renderForCurrentRange());

        // Φέρε πραγματικά δεδομένα (πολλές μέρες)
        fetchMany();
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

        // Αρχικό max (θα αναπροσαρμοστεί στο render)
        int goal = UserPrefs.getGoalSteps(this);
        left.setAxisMaximum(Math.max(goal, 10000));
        left.setAxisMinimum(0f);
    }

    /** Τραβά πολλές μέρες από το API (ίδιο endpoint με DailiesActivity, χωρίς φίλτρο “σήμερα”). */
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
                        Toast.makeText(InsightsActivity.this, "No data", Toast.LENGTH_SHORT).show();
                        allDays = new ArrayList<>();
                        renderForCurrentRange();
                    } else {
                        parseMany(body);   // ΓΕΜΙΖΕΙ allDays με ΟΛΕΣ τις μέρες
                        renderForCurrentRange();
                    }
                });
            }
        });
    }

    /** Παίρνει ΟΛΕΣ τις μέρες από το JSON του /dailies (χωρίς φίλτρο “σήμερα”). */
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
                // προτίμησε το top-level calendarDate αν υπάρχει
                String cal = dayObj.optString("calendarDate", dObj.optString("calendarDate", null));
                d.calendarDate = cal;
                d.steps = dObj.optInt("steps");
                // (αν θελήσεις κι άλλα πεδία, πρόσθεσέ τα εδώ)

                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = d.calendarDate;
                day.data = d;
                // αγνόησε records χωρίς ημερομηνία
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

        // sort by date asc (ασφαλής parse)
        try {
            Collections.sort(allDays, (a, b) ->
                    LocalDate.parse(a.calendarDate, ISO).compareTo(LocalDate.parse(b.calendarDate, ISO)));
        } catch (Exception ignore) { /* αν χαλάσει parsing, αφήνουμε την τρέχουσα σειρά */ }

        // εύρος
        int days = 30; // default
        if (((Chip)findViewById(R.id.chip7d)).isChecked())    days = 7;
        else if (((Chip)findViewById(R.id.chip6m)).isChecked()) days = 180;
        else if (((Chip)findViewById(R.id.chip1y)).isChecked()) days = 365;

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

        // Entries
        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        int total = 0;
        int maxInWindow = 0;
        for (DailiesModels.Day d : window) {
            int s = d.data.steps;
            entries.add(new BarEntry(idx++, s));
            total += s;
            if (s > maxInWindow) maxInWindow = s;
        }
        if (entries.isEmpty()) {
            // βάλ’ το πολύ 14 κολώνες 0 για να μη δείχνει κενό
            int zeros = Math.min(days, 14);
            for (int i = 0; i < zeros; i++) entries.add(new BarEntry(i, 0));
        }

        // KPIs
        int avg = window.isEmpty() ? 0 : Math.round(total * 1f / window.size());
        tvAvg.setText("Average\n" + String.format(Locale.getDefault(), "%,d", avg));
        tvTotal.setText("Total\n" + String.format(Locale.getDefault(), "%,d", total));

        // y-axis = max(goal, maxInWindow) * 1.1
        int goal = UserPrefs.getGoalSteps(this);
        float yMax = (float) Math.max(goal, maxInWindow);
        YAxis left = barChart.getAxisLeft();
        left.setAxisMaximum(yMax > 0 ? yMax * 1.1f : Math.max(goal, 10000));
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
