package com.example.myapplication;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import android.util.TypedValue;
import android.graphics.Typeface;

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

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DailiesModels.Day day = items.get(position);
        DailiesModels.Daily d = day.data;

        // Ημερομηνία + τίτλος
        h.tvDate.setText(FormatUtils.prettyDate(day.calendarDate));
        h.tvStepsTitle.setText("👣  Steps Today");

        // Donut
        Context ctx = h.itemView.getContext();
        int goalSteps = Math.max(1, UserPrefs.getGoalSteps(ctx));
        int pct = Math.round(100f * Math.max(0, d.steps) / goalSteps);
        h.stepsDonut.setStrokeWidthDp(12f);
        h.stepsDonut.setTrackColor(0xFFD1D5DB);
        h.stepsDonut.setProgressColor(0xFF3B82F6);
        h.stepsDonut.setProgress(pct);

        h.tvSteps.setText(String.valueOf(d.steps));
        h.tvStepsGoal.setText("Steps goal: " + FormatUtils.sep(goalSteps));

        // Metric cards
        bindMetric(h.cardCalories,
                R.drawable.ic_calories, "Calories",
                d.activeKilocalories + " kcal", "#FFFF00");

        bindMetric(h.cardDistance,
                R.drawable.location, "Distance",
                FormatUtils.km(d.distanceInMeters) + " km", "#B4FFB7");

        bindMetric(h.cardAvgHr,
                R.drawable.ic_heart_rate, "Avg Heart Rate",
                d.averageHeartRateInBeatsPerMinute + " bpm", "#EEC522");

        bindMetric(h.cardActiveTime,
                R.drawable.clocktime, "Active Time",
                FormatUtils.humanDuration(d.activeTimeInSeconds), "#DB7070");

        // WATER INTAKE (νέο UI)
        final int goalWater = WaterPrefs.getGoal(h.itemView.getContext()); // π.χ. 10
        int count = WaterPrefs.getCountForDate(h.itemView.getContext(), day.calendarDate);

        h.tvWaterSubtitle.setText(count + " of " + goalWater + " glasses");
        renderWaterDots(h.llWaterDots, Math.min(count, goalWater), goalWater);

        h.btnWaterPlus.setOnClickListener(v -> {
            WaterPrefs.increment(v.getContext(), day.calendarDate, goalWater);
            int newCount = WaterPrefs.getCountForDate(v.getContext(), day.calendarDate);
            h.tvWaterSubtitle.setText(newCount + " of " + goalWater + " glasses");
            renderWaterDots(h.llWaterDots, Math.min(newCount, goalWater), goalWater);
        });

        h.cardWater.setOnLongClickListener(v -> {
            WaterPrefs.reset(v.getContext(), day.calendarDate);
            h.tvWaterSubtitle.setText("0 of " + goalWater + " glasses");
            renderWaterDots(h.llWaterDots, 0, goalWater);
            return true;
        });
    }

    /*private void bindMetric(View card, int iconRes, String title, String value, String bgHex) {
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
    }*/


    private void bindMetric(View card, int iconRes, String title, String value, String bgHex) {
        ImageView iv    = card.findViewById(R.id.ivIcon);
        TextView tTitle = card.findViewById(R.id.tvTitle);
        TextView tValue = card.findViewById(R.id.tvValue);

        iv.setImageResource(iconRes);

        // default icon size (dp)
        int iconDp = 28;

        // per-icon overrides με if/else if
        if (iconRes == R.drawable.ic_heart_rate) {
            iconDp = 32; // πιο μεγάλο για HR
        } else if (iconRes == R.drawable.clocktime) {
            iconDp = 28;
        } else if (iconRes == R.drawable.location) {
            iconDp = 30;
        } else if (iconRes == R.drawable.ic_calories) {
            iconDp = 34;
        }

        ViewGroup.LayoutParams lp = iv.getLayoutParams();
        lp.width  = dp(card, iconDp);
        lp.height = dp(card, iconDp);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setAdjustViewBounds(true);

        tTitle.setText(title);
        tTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 16f);

        tValue.setText(value);
        tValue.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 20f);
        tValue.setTypeface(tValue.getTypeface(), android.graphics.Typeface.BOLD);

        if (card instanceof com.google.android.material.card.MaterialCardView) {
            ((com.google.android.material.card.MaterialCardView) card)
                    .setCardBackgroundColor(Color.parseColor(bgHex));
        } else {
            card.setBackgroundColor(Color.parseColor(bgHex));
        }
    }


    private void renderWaterDots(LinearLayout row, int count, int goal) {
        row.removeAllViews();
        int n = Math.max(1, goal); // ή: n = 10; αν τις θέλεις πάντα 10

        for (int i = 0; i < n; i++) {
            View dot = new View(row.getContext());
            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(dp(row, 10), dp(row, 10));
            lp.rightMargin = dp(row, 6);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(i < count ?
                    R.drawable.water_dot_filled : R.drawable.water_dot_empty);
            row.addView(dot);
        }
    }

    private int dp(View v, int d) {
        float den = v.getResources().getDisplayMetrics().density;
        return Math.round(d * den);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDate, tvStepsTitle, tvSteps, tvStepsGoal;
        final View cardCalories, cardDistance, cardAvgHr, cardActiveTime;
        final DonutProgressView stepsDonut;

        // Water views
        final MaterialCardView cardWater;
        final TextView tvWaterSubtitle;
        final ImageView  btnWaterPlus;
        final LinearLayout llWaterDots;   // <-- ΠΡΟΣΘΗΚΗ: πεδίο + init

        VH(@NonNull View itemView) {
            super(itemView);

            tvDate       = itemView.findViewById(R.id.tvDate);
            tvStepsTitle = itemView.findViewById(R.id.tvStepsTitle);
            tvSteps      = itemView.findViewById(R.id.tvSteps);
            tvStepsGoal  = itemView.findViewById(R.id.tvStepsGoal);

            stepsDonut   = itemView.findViewById(R.id.stepsDonut);

            cardCalories   = itemView.findViewById(R.id.cardCalories);
            cardDistance   = itemView.findViewById(R.id.cardDistance);
            cardAvgHr      = itemView.findViewById(R.id.cardAvgHr);
            cardActiveTime = itemView.findViewById(R.id.cardActiveTime);

            cardWater       = itemView.findViewById(R.id.cardWater);
            tvWaterSubtitle = itemView.findViewById(R.id.tvWaterSubtitle);
            btnWaterPlus = itemView.findViewById(R.id.btnWaterPlus);
            llWaterDots     = itemView.findViewById(R.id.llWaterDots); // <-- ΧΡΕΙΑΖΕΤΑΙ
        }
    }
}
