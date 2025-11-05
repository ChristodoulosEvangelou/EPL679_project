package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class DailyAdapter extends RecyclerView.Adapter<DailyAdapter.VH> {

    private final List<DailiesModels.Day> items = new ArrayList<>();

    public void submit(List<DailiesModels.Day> days) {
        items.clear();
        if (days != null) items.addAll(days);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_summary, parent, false);
        return new VH(v);
    }
    /*
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailiesModels.Day day = items.get(position);
        DailiesModels.Daily d = day.data;

        // Ημερομηνία
        h.tvDate.setText(FormatUtils.prettyDate(day.calendarDate));
        // Label κύκλου
        h.tvStepsTitle.setText("👣  Steps Today");

        // --- Donut progress (το έχεις ήδη υλοποιήσει) ---
        int goalSteps = Math.max(1, d.stepsGoal);
        int pct  = Math.round(100f * Math.max(0, d.steps) / goalSteps);
        h.stepsDonut.setStrokeWidthDp(22f);
        h.stepsDonut.setTrackColor(0xFFD1D5DB);
        h.stepsDonut.setProgressColor(0xFF3B82F6);
        h.stepsDonut.setProgress(pct);

        h.tvSteps.setText(String.valueOf(d.steps));
        h.tvStepsGoal.setText("Steps goal: " + FormatUtils.sep(goalSteps));

        // --- Metric cards (ίδια) ---
        bindMetric(h.cardCalories,
                R.drawable.ic_calories, "Calories",
                d.activeKilocalories + " kcal", "#FFFF00");

        bindMetric(h.cardDistance,
                R.drawable.ic_distance, "Distance",
                FormatUtils.km(d.distanceInMeters) + " km", "#B4FFB7");

        bindMetric(h.cardAvgHr,
                R.drawable.ic_heart_rate, "Avg Heart Rate",
                d.averageHeartRateInBeatsPerMinute + " bpm", "#EEC522");

        bindMetric(h.cardActiveTime,
                R.drawable.ic_active_time, "Active Time",
                FormatUtils.humanDuration(d.activeTimeInSeconds), "#DB7070");

        // --- WATER INTAKE ---
        final int goalWater = WaterPrefs.getGoal(h.itemView.getContext()); // default 8
        int count = WaterPrefs.getCountForDate(h.itemView.getContext(), day.calendarDate);

        h.waterBar.setMax(goalWater);
        h.waterBar.setProgress(Math.min(goalWater, count));
        h.tvWaterSubtitle.setText(count + " of " + goalWater + " glasses");

        h.btnWaterPlus.setOnClickListener(v -> {
            WaterPrefs.increment(v.getContext(), day.calendarDate, goalWater);
            int newCount = WaterPrefs.getCountForDate(v.getContext(), day.calendarDate);
            h.waterBar.setProgress(Math.min(goalWater, newCount));
            h.tvWaterSubtitle.setText(newCount + " of " + goalWater + " glasses");
        });

        // προαιρετικό reset με long press στο card
        h.cardWater.setOnLongClickListener(v -> {
            WaterPrefs.reset(v.getContext(), day.calendarDate);
            h.waterBar.setProgress(0);
            h.tvWaterSubtitle.setText("0 of " + goalWater + " glasses");
            return true;
        });
    }*/
    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailiesModels.Day day = items.get(position);
        DailiesModels.Daily d = day.data;

        // Ημερομηνία + τίτλος
        h.tvDate.setText(FormatUtils.prettyDate(day.calendarDate));
        h.tvStepsTitle.setText("👣  Steps Today");

        // --- Donut progress (κρατάμε όλες τις ρυθμίσεις που είχες) ---
        //int goalSteps = Math.max(1, d.stepsGoal);
        Context ctx = h.itemView.getContext();
        int goalSteps = Math.max(1, UserPrefs.getGoalSteps(ctx));

        int pct = Math.round(100f * Math.max(0, d.steps) / goalSteps);
        h.stepsDonut.setStrokeWidthDp(22f);
        h.stepsDonut.setTrackColor(0xFFD1D5DB);   // γκρι υπόλοιπο
        h.stepsDonut.setProgressColor(0xFF3B82F6); // μπλε progress
        h.stepsDonut.setProgress(pct);

        h.tvSteps.setText(String.valueOf(d.steps));
        h.tvStepsGoal.setText("Steps goal: " + FormatUtils.sep(goalSteps));

        // --- Metric cards ---
        bindMetric(h.cardCalories,
                R.drawable.ic_calories, "Calories",
                d.activeKilocalories + " kcal", "#FFFF00");

        bindMetric(h.cardDistance,
                R.drawable.ic_distance, "Distance",
                FormatUtils.km(d.distanceInMeters) + " km", "#B4FFB7");

        bindMetric(h.cardAvgHr,
                R.drawable.ic_heart_rate, "Avg Heart Rate",
                d.averageHeartRateInBeatsPerMinute + " bpm", "#EEC522");

        bindMetric(h.cardActiveTime,
                R.drawable.ic_active_time, "Active Time",
                FormatUtils.humanDuration(d.activeTimeInSeconds), "#DB7070");

        // --- WATER INTAKE ---
        final int goalWater = WaterPrefs.getGoal(h.itemView.getContext()); // default 8
        // με βάση την ημερομηνία του Day (YYYY-MM-DD)
        String dateKey = day.calendarDate != null ? day.calendarDate
                : java.time.LocalDate.now().toString();
        int count = WaterPrefs.getCountForDate(h.itemView.getContext(), dateKey);

        h.waterBar.setMax(Math.max(1, goalWater));
        h.waterBar.setProgress(Math.min(goalWater, count));
        h.tvWaterSubtitle.setText(count + " of " + goalWater + " glasses");

        h.btnWaterPlus.setOnClickListener(v -> {
            WaterPrefs.increment(v.getContext(), dateKey, goalWater);
            int newCount = WaterPrefs.getCountForDate(v.getContext(), dateKey);
            h.waterBar.setProgress(Math.min(goalWater, newCount));
            h.tvWaterSubtitle.setText(newCount + " of " + goalWater + " glasses");
        });

        // προαιρετικό reset με long-press στο card
        h.cardWater.setOnLongClickListener(v -> {
            WaterPrefs.reset(v.getContext(), dateKey);
            h.waterBar.setProgress(0);
            h.tvWaterSubtitle.setText("0 of " + goalWater + " glasses");
            return true;
        });
    }


    private void bindMetric(View card, int iconRes, String title, String value, String bgHex) {
        ImageView iv = card.findViewById(R.id.ivIcon);
        TextView tTitle = card.findViewById(R.id.tvTitle);
        TextView tValue = card.findViewById(R.id.tvValue);
        iv.setImageResource(iconRes);
        tTitle.setText(title);
        tValue.setText(value);
        if (card instanceof MaterialCardView) {
            ((MaterialCardView) card).setCardBackgroundColor(Color.parseColor(bgHex));
        } else {
            card.setBackgroundColor(Color.parseColor(bgHex));
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate, tvStepsTitle, tvSteps, tvStepsGoal;
        final View cardCalories, cardDistance, cardAvgHr, cardActiveTime;

        // Donut custom view
        final DonutProgressView stepsDonut;

        // Water views
        final MaterialCardView cardWater;
        final TextView tvWaterSubtitle;
        final LinearProgressIndicator waterBar;
        final ImageButton btnWaterPlus;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate       = itemView.findViewById(R.id.tvDate);
            tvStepsTitle = itemView.findViewById(R.id.tvStepsTitle);
            tvSteps      = itemView.findViewById(R.id.tvSteps);
            tvStepsGoal  = itemView.findViewById(R.id.tvStepsGoal);

            // donut
            stepsDonut   = itemView.findViewById(R.id.stepsDonut);

            // metric cards
            cardCalories   = itemView.findViewById(R.id.cardCalories);
            cardDistance   = itemView.findViewById(R.id.cardDistance);
            cardAvgHr      = itemView.findViewById(R.id.cardAvgHr);
            cardActiveTime = itemView.findViewById(R.id.cardActiveTime);

            // water
            cardWater       = itemView.findViewById(R.id.cardWater);
            tvWaterSubtitle = itemView.findViewById(R.id.tvWaterSubtitle);
            waterBar        = itemView.findViewById(R.id.waterBar);
            btnWaterPlus    = itemView.findViewById(R.id.btnWaterPlus);
        }
    }
}
