package com.example.myapplication;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
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

public class InsightsActivity extends AppCompatActivity {

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

        // Tabs (μόνο Steps ενεργό προς το παρόν)
        MaterialButtonToggleGroup toggle = findViewById(R.id.toggleTabs);
        toggle.addOnButtonCheckedListener((g, id, checked) -> {
            if (!checked) return;
            if (id == R.id.btnTabSteps) {
                // ok
                renderForCurrentRange();
            } else {
                Toast.makeText(this, "Coming soon", Toast.LENGTH_SHORT).show();
                g.check(R.id.btnTabSteps); // μείνε στα Steps
            }
        });

        barChart = findViewById(R.id.barChart);
        tvAvg = findViewById(R.id.tvAvg);
        tvTotal = findViewById(R.id.tvTotal);
        chips = findViewById(R.id.chipsRange);

        // default επιλεγμένο: 30 days
        ((Chip) findViewById(R.id.chip30d)).setChecked(true);

        // chart styling
        setupChart();

        // φέρε δεδομένα πολλών ημερών
        // Αν ήδη έχεις ένα JSON μεγάλο από το API, κάλεσε parseMany(payload).
        // Για αρχή, θα ξαναχρησιμοποιήσουμε τα data που έχεις ήδη στο cache ή
        // μπορείς να τα περάσεις από το Main/Dailies. Εδώ δείχνω stub που
        // διαβάζει από ένα singleton/last payload αν έχεις. Αλλιώς βάλε δικό σου fetch.
        fetchOrReuseData();

        // listeners στα chips
        chips.setOnCheckedStateChangeListener((group, checkedIds) -> renderForCurrentRange());

        // αρχικό render
        renderForCurrentRange();
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

        // y-axis max = goal steps
        int goal = UserPrefs.getGoalSteps(this);
        left.setAxisMaximum(goal);
        left.setAxisMinimum(0f);
    }

    private void fetchOrReuseData() {
        // Αν ήδη η Main/Dailies αποθηκεύει το τελευταίο payload κάπου (π.χ. SharedPreferences/file),
        // το διάβασε εδώ. Για demo: θα προσπαθήσω να ξαναχρησιμοποιήσω από DailiesActivity
        // εφόσον είχε φέρει πολλές ημέρες. Διαφορετικά, άφησε την allDays κενή.
        // TIP: Ιδανικά κάνε ξεχωριστό API call που γυρνάει N ημέρες και κάλεσε parseMany(json).
    }

    /** Μπορείς να καλέσεις αυτό με το JSON του /dailies που επιστρέφει πολλές μέρες */
    private void parseMany(String payload) {
        try {
            List<DailiesModels.Day> tmp = new ArrayList<>();
            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);
                JSONObject dObj = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = dayObj.optString("calendarDate", dObj.optString("calendarDate", null));
                d.steps = dObj.optInt("steps");
                //tmp.add(new DailiesModels.Day(d.calendarDate, d));
                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = d.calendarDate;  // ή ό,τι ημερομηνία κρατάς
                day.data = d;
                tmp.add(day);
            }
            allDays = tmp;
        } catch (Exception ignore) {}
    }

    private void renderForCurrentRange() {
        if (allDays == null) allDays = new ArrayList<>();
        // sort by date asc
        Collections.sort(allDays, (a, b) ->
                LocalDate.parse(a.calendarDate, ISO).compareTo(LocalDate.parse(b.calendarDate, ISO)));

        // εύρος
        int days = 30; // default
        if (findViewById(R.id.chip7d).isPressed() || ((Chip)findViewById(R.id.chip7d)).isChecked()) days = 7;
        if (((Chip)findViewById(R.id.chip30d)).isChecked()) days = 30;
        if (((Chip)findViewById(R.id.chip6m)).isChecked()) days = 180;
        if (((Chip)findViewById(R.id.chip1y)).isChecked()) days = 365;

        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days - 1);

        // φιλτράρισμα
        List<DailiesModels.Day> window = new ArrayList<>();
        for (DailiesModels.Day d : allDays) {
            LocalDate ld = LocalDate.parse(d.calendarDate, ISO);
            if (!ld.isBefore(from) && !ld.isAfter(to)) window.add(d);
        }

        // entries (αν δεν έχεις δεδομένα, βάλε zeros για να φαίνεται ο άξονας)
        List<BarEntry> entries = new ArrayList<>();
        int idx = 0;
        int total = 0;
        for (DailiesModels.Day d : window) {
            entries.add(new BarEntry(idx++, d.data.steps));
            total += d.data.steps;
        }
        if (entries.isEmpty()) {
            for (int i = 0; i < Math.min(days, 14); i++) entries.add(new BarEntry(i, 0));
        }

        // KPIs
        int avg = window.isEmpty() ? 0 : Math.round(total * 1f / window.size());
        tvAvg.setText("Average\n" + FormatUtils.sep(avg));
        tvTotal.setText("Total\n" + FormatUtils.sep(total));

        // y-axis = goal
        int goal = UserPrefs.getGoalSteps(this);
        YAxis left = barChart.getAxisLeft();
        left.setAxisMaximum(Math.max(goal, Math.max(avg, (window.isEmpty() ? 0 : Collections.max(window, (a,b)->a.data.steps-b.data.steps).data.steps))) * 1.1f);
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
