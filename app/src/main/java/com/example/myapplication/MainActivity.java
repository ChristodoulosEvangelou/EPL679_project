package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DailiesCall";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310";

    private TextView tvResult;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvResult = findViewById(R.id.tvResult);

        Button btnConnect = findViewById(R.id.btnConnectGarmin);
        btnConnect.setOnClickListener(v ->
                startActivity(new Intent(this, GarminLinkActivity.class))
        );

        Button btnToday = findViewById(R.id.btnFetchToday);
        btnToday.setOnClickListener(v -> fetchDailiesForDate(today()));

        Button btnLast7 = findViewById(R.id.btnFetchLast7);
        btnLast7.setOnClickListener(v -> fetchDailiesFromTo(daysAgo(7), today()));
    }

    @Override protected void onResume() {
        super.onResume();
        String cookie = SecureCookie.get(this);
        if (cookie == null || cookie.isEmpty()) {
            tvResult.setText("No cookie yet. Connect Garmin first.");
        } else {
            tvResult.setText("Ready. Press a button to fetch.");
        }
    }

    /* ---------- Helpers για ημερομηνίες ---------- */
    private String today() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new java.util.Date());
    }
    private String daysAgo(int d) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, -d);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    /* ---------- URLs ---------- */
    private String urlDailiesForDate(String yyyyMmDd) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("garmin-ucy.3ahealth.com")
                .addPathSegments("garmin/dailies")
                .addQueryParameter("garminUserId", USER_ID)
                .addQueryParameter("date", yyyyMmDd)
                .build().toString();
    }
    private String urlDailiesFromTo(String from, String to) {
        return new HttpUrl.Builder()
                .scheme("https")
                .host("garmin-ucy.3ahealth.com")
                .addPathSegments("garmin/dailies")
                .addQueryParameter("garminUserId", USER_ID)
                .addQueryParameter("from", from)
                .addQueryParameter("to", to)
                .build().toString();
    }

    /* ---------- Fetch methods ---------- */
    private void fetchDailiesForDate(String date) { doGet(urlDailiesForDate(date)); }
    private void fetchDailiesFromTo(String from, String to) { doGet(urlDailiesFromTo(from, to)); }

    private void doGet(String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request orig = chain.request();
                    String cookie = SecureCookie.get(this); // "name=value"
                    Log.d("COOKIE", cookie != null ? ("len=" + cookie.length() + " preview=" + cookie.substring(0, Math.min(40, cookie.length()))) : "null");

                    Request.Builder b = orig.newBuilder()
                            .addHeader("Accept", "application/json");
                    if (cookie != null && !cookie.isEmpty()) {
                        b.addHeader("Cookie", cookie);
                    }

                    Response res = chain.proceed(b.build());
                    if (res.code() == 401 || res.code() == 403) {
                        runOnUiThread(() -> startActivity(new Intent(this, GarminLinkActivity.class)));
                    }
                    return res;
                })
                .build();

        Log.d(TAG, "URL: " + url);
        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() -> tvResult.setText("Network error: " + e.getMessage()));
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Headers h = response.headers();
                Log.d(TAG, "HTTP " + response.code() + "\n" + h + "\n" + body);
                runOnUiThread(() -> tvResult.setText("HTTP " + response.code() + "\n" + body));
            }
        });
    }
}
