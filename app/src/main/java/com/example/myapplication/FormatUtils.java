package com.example.myapplication;

import android.annotation.SuppressLint;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class FormatUtils {
    private static final NumberFormat NF = NumberFormat.getIntegerInstance();

    public static String sep(int n) { return NF.format(n); }

    public static String km(long meters) {
        double km = meters / 1000.0;
        return String.format(Locale.getDefault(), "%.1f", km);
    }

    public static String humanDuration(int sec) {
        if (sec <= 0) return "0 min";
        int h = sec / 3600;
        int m = (sec % 3600) / 60;
        if (h > 0) return h + "h " + m + "m";
        return m + " min";
    }

    @SuppressLint("SimpleDateFormat")
    public static String prettyDate(String yyyyMMdd) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat out = new SimpleDateFormat("EEEE, MMM d", Locale.getDefault());
            return out.format(in.parse(yyyyMMdd));
        } catch (Exception ignored) {
            return yyyyMMdd;
        }
    }
}
