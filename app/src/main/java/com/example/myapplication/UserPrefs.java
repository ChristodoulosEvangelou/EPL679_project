package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class UserPrefs {
    private static final String FILE = "user_prefs";
    private static final String K_FIRST = "first_name";
    private static final String K_LAST  = "last_name";
    private static final String K_HEIGHT_CM = "height_cm";
    private static final String K_WEIGHT_KG = "weight_kg";

    private static final String K_GOAL_STEPS   = "goal_steps";
    private static final String K_GOAL_CAL     = "goal_calories";
    private static final String K_GOAL_DIST_KM = "goal_distance_km";
    private static final String K_GOAL_WATER   = "goal_water_glasses";

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static void setFirstName(Context c, String v) { sp(c).edit().putString(K_FIRST, v).apply(); }
    public static String getFirstName(Context c) { return sp(c).getString(K_FIRST, ""); }

    public static void setLastName(Context c, String v) { sp(c).edit().putString(K_LAST, v).apply(); }
    public static String getLastName(Context c) { return sp(c).getString(K_LAST, ""); }

    public static void setHeightCm(Context c, int v) { sp(c).edit().putInt(K_HEIGHT_CM, v).apply(); }
    public static int getHeightCm(Context c) { return sp(c).getInt(K_HEIGHT_CM, 170); }

    public static void setWeightKg(Context c, int v) { sp(c).edit().putInt(K_WEIGHT_KG, v).apply(); }
    public static int getWeightKg(Context c) { return sp(c).getInt(K_WEIGHT_KG, 70); }

    public static void setGoalSteps(Context c, int v){ sp(c).edit().putInt(K_GOAL_STEPS, v).apply(); }
    public static int getGoalSteps(Context c){ return sp(c).getInt(K_GOAL_STEPS, 10000); }

    public static void setGoalCalories(Context c, int v){ sp(c).edit().putInt(K_GOAL_CAL, v).apply(); }
    public static int getGoalCalories(Context c){ return sp(c).getInt(K_GOAL_CAL, 750); }

    public static void setGoalDistanceKm(Context c, int v){ sp(c).edit().putInt(K_GOAL_DIST_KM, v).apply(); }
    public static int getGoalDistanceKm(Context c){ return sp(c).getInt(K_GOAL_DIST_KM, 8); }

    public static void setGoalWater(Context c, int v){ sp(c).edit().putInt(K_GOAL_WATER, v).apply(); }
    public static int getGoalWater(Context c){ return sp(c).getInt(K_GOAL_WATER, 8); }
}

