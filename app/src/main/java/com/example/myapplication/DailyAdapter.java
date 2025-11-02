package com.example.myapplication;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
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

    @Override public void onBindViewHolder(@NonNull VH h, int position) {
        DailiesModels.Day day = items.get(position);
        DailiesModels.Daily d = day.data;

        // Date
        h.tvDate.setText(FormatUtils.prettyDate(day.calendarDate));

        // Steps ring
        int goal = Math.max(1, d.stepsGoal);
        h.stepsIndicator.setMax(goal);
        h.stepsIndicator.setProgress(Math.max(0, Math.min(goal, d.steps)));
        h.tvSteps.setText(String.valueOf(d.steps));
        h.tvStepsGoal.setText("Steps goal: " + FormatUtils.sep(goal));

        // Metric cards
        bindMetric(h.cardCalories,
                android.R.drawable.ic_menu_manage, "Calories",
                d.activeKilocalories + " kcal", "#FFF8E1");

        bindMetric(h.cardDistance,
                android.R.drawable.ic_menu_mylocation, "Distance",
                FormatUtils.km(d.distanceInMeters) + " km", "#E1F5FE");

        bindMetric(h.cardAvgHr,
                android.R.drawable.ic_menu_compass, "Avg Heart Rate",
                d.averageHeartRateInBeatsPerMinute + " bpm", "#FFEBEE");

        bindMetric(h.cardActiveTime,
                android.R.drawable.ic_menu_recent_history, "Active Time",
                FormatUtils.humanDuration(d.activeTimeInSeconds), "#FFE0F7");

        // Stress
        h.tvStressValues.setText(d.averageStressLevel + " / " + d.maxStressLevel);
        h.stressBar.setMax(100);
        h.stressBar.setProgress(Math.max(0, Math.min(100, d.averageStressLevel)));
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
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate, tvSteps, tvStepsGoal, tvStressValues;
        final View cardCalories, cardDistance, cardAvgHr, cardActiveTime;
        final CircularProgressIndicator stepsIndicator;
        final LinearProgressIndicator stressBar;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSteps = itemView.findViewById(R.id.tvSteps);
            tvStepsGoal = itemView.findViewById(R.id.tvStepsGoal);
            tvStressValues = itemView.findViewById(R.id.tvStressValues);
            stepsIndicator = itemView.findViewById(R.id.stepsIndicator);
            stressBar = itemView.findViewById(R.id.stressBar);

            cardCalories = itemView.findViewById(R.id.cardCalories);
            cardDistance = itemView.findViewById(R.id.cardDistance);
            cardAvgHr = itemView.findViewById(R.id.cardAvgHr);
            cardActiveTime = itemView.findViewById(R.id.cardActiveTime);
        }
    }
}
