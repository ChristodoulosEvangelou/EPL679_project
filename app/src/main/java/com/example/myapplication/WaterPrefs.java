package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class WaterPrefs {
    private static final String FILE = "water_intake_prefs";
    private static final String KEY_GOAL = "water_goal"; // κοινός στόχος (default 8)

    public static int getGoal(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getInt(KEY_GOAL, 8);
    }

    public static void setGoal(Context ctx, int goal) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putInt(KEY_GOAL, Math.max(1, goal)).apply();
    }

    /** μετρητής για συγκεκριμένη ημερομηνία "YYYY-MM-DD" */
    public static int getCountForDate(Context ctx, String date) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .getInt("count_" + date, 0);
    }

    public static void setCountForDate(Context ctx, String date, int count) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
                .edit().putInt("count_" + date, Math.max(0, count)).apply();
    }

    public static void increment(Context ctx, String date, int goal) {
        int cur = getCountForDate(ctx, date);
        if (cur < goal) setCountForDate(ctx, date, cur + 1);
    }

    public static void reset(Context ctx, String date) {
        setCountForDate(ctx, date, 0);
    }
}
