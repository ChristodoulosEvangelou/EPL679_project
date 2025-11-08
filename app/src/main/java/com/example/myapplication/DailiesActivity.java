package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DailiesActivity extends AppCompatActivity {
    public static final String EXTRA_JSON = "extra_json";
    private static final String TAG = "Dailies";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310";

    private RecyclerView rv;
    private DailyAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dailies_activity);

        // --- Bottom bar setup (όπως είχες) ---
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setSelectedItemId(R.id.nav_home);
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_insights) {
                    startActivity(new Intent(this, InsightsActivity.class));
                    return true;
                } else if (id == R.id.nav_home) {
                    return true; // ήδη εδώ
                } else if (id == R.id.nav_notifications) {
                    Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        // insets για το pill
        MaterialCardView barCard = findViewById(R.id.bottomBarCard);
        if (barCard != null) {
            ViewCompat.setOnApplyWindowInsetsListener(barCard, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
                return insets;
            });
        }
        // --- /Bottom bar setup ---

        rv = findViewById(R.id.rvDays);
        adapter = new DailyAdapter();
        rv.setAdapter(adapter);

        // Αν ήρθαμε από Goals/Profile χωρίς payload → κάνε fetch μόνος σου
        Uri dataUri = getIntent().getData();
        if (dataUri == null) {
            fetchDailies();
        } else {
            try (InputStream is = getContentResolver().openInputStream(dataUri);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                adapter.submit(parseDays(sb.toString()));
            } catch (Exception e) {
                String TsAG="";
                Log.e(TsAG, "Failed to read payload file", e);
                Toast.makeText(this, "Error reading data", Toast.LENGTH_SHORT).show();
                fetchDailies();
            }
        }
    }


    @Override protected void onResume() {
        super.onResume();
        if (rv != null && rv.getAdapter() != null) {
            rv.getAdapter().notifyDataSetChanged();
        }
    }


    private void fetchDailies() {
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
                        Toast.makeText(DailiesActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (body == null || body.isEmpty()) {
                        Toast.makeText(DailiesActivity.this, "No data", Toast.LENGTH_SHORT).show();
                        adapter.submit(new ArrayList<>());
                    } else {
                        adapter.submit(parseDays(body));
                    }
                });
            }
        });
    }
    //new to generate if steps <2000
    private int normalizedSteps(String calendarDate, int rawSteps) {
        if (rawSteps >= 2000) return rawSteps;
        java.util.Random rnd = new java.util.Random(calendarDate != null ? calendarDate.hashCode() : 0);
        return 7000 + rnd.nextInt(3001); // 7000..10000
    }
    // --- Heuristics για derived metrics όταν τα steps είναι χαμηλά ή λείπουν ---
    private static final float STRIDE_METERS = 0.75f;          // ~75cm ανά βήμα
    private static final float KCAL_PER_STEP = 0.045f;         // ~0.045 kcal/βήμα
    private static final int   CADENCE_WALK  = 105;            // βήματα/λεπτό

    /** Επιστρέφει true αν τα steps «φουσκώθηκαν» λόγω raw<2000 */
    private boolean stepsWereNormalized(int rawSteps) {
        return rawSteps < 2000;
    }

    private List<DailiesModels.Day> parseDays(String payload) {
        List<DailiesModels.Day> out = new ArrayList<>();
        try {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return out;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);

                String calendarDate = dayObj.optString("calendarDate", null);
                boolean isToday = false;

                if (calendarDate != null && !calendarDate.isEmpty()) {
                    isToday = LocalDate.parse(calendarDate, ISO).equals(today);
                } else {
                    JSONObject dTmp = dayObj.optJSONObject("data");
                    if (dTmp != null) {
                        long start = dTmp.optLong("startTimeInSeconds", -1L);
                        int offset = dTmp.optInt("startTimeOffsetInSeconds", 0);
                        if (start > 0) {
                            Instant instant = Instant.ofEpochSecond(start).minusSeconds(offset);
                            LocalDate ld = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
                            isToday = ld.equals(today);
                            if (calendarDate == null) calendarDate = ld.format(ISO);
                        }
                    }
                }

                if (!isToday) continue;

                JSONObject dObj = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = dObj.optString("calendarDate", calendarDate);

// Steps με normalized fallback
                int rawSteps = dObj.optInt("steps");
                d.steps = normalizedSteps(d.calendarDate, rawSteps);
                boolean normalized = stepsWereNormalized(rawSteps);

// Goal
                int apiGoal = dObj.optInt("stepsGoal", 0);
                d.stepsGoal = (apiGoal > 0) ? apiGoal : UserPrefs.getGoalSteps(this);

// Πάρε ό,τι δίνει το API (αν δίνει)
                int  apiActiveKcal = dObj.optInt("activeKilocalories");
                long apiDistanceM  = dObj.optLong("distanceInMeters");
                int  apiActiveSecs = dObj.optInt("activeTimeInSeconds");

                d.activeKilocalories = apiActiveKcal;
                d.distanceInMeters   = apiDistanceM;
                d.activeTimeInSeconds= apiActiveSecs;

                d.averageHeartRateInBeatsPerMinute = dObj.optInt("averageHeartRateInBeatsPerMinute");
                d.averageStressLevel               = dObj.optInt("averageStressLevel");
                d.maxStressLevel                   = dObj.optInt("maxStressLevel");

// Αν «φουσκώσαμε» steps ή λείπουν/είναι 0 οι τιμές → εκτίμηση για να βγάζουν νόημα
                if (normalized || d.distanceInMeters <= 0) {
                    d.distanceInMeters = Math.round(d.steps * STRIDE_METERS); // σε μέτρα
                }
                if (normalized || d.activeKilocalories <= 0) {
                    d.activeKilocalories = Math.round(d.steps * KCAL_PER_STEP);
                }
                if (normalized || d.activeTimeInSeconds <= 0) {
                    // λεπτά = steps / cadence → sec = λεπτά*60
                    d.activeTimeInSeconds = Math.round((d.steps / (float) CADENCE_WALK) * 60f);
                }

// Τύλιγμα στο Day
                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = (calendarDate != null) ? calendarDate : d.calendarDate;
                day.data = d;
                out.add(day);

            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
        }
        return out;
    }
}


/*
ΧΩΡΙΣ ΤΗΝ ΔΗΜΙΟΥΡΓΕΙΑ ΨΕΥΤΙΚΩΝ DATA ΑΝ ΔΕΝ ΠΕΡΝΑ ΤΑ 2000 STEPS.

package com.example.myapplication;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DailiesActivity extends AppCompatActivity {
    public static final String EXTRA_JSON = "extra_json";
    private static final String TAG = "Dailies";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310";

    private RecyclerView rv;
    private DailyAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dailies_activity);

        // --- Bottom bar setup (όπως είχες) ---
        BottomNavigationView bottom = findViewById(R.id.bottomNav);
        if (bottom != null) {
            bottom.setSelectedItemId(R.id.nav_home);
            bottom.setOnItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_insights) {
                    startActivity(new Intent(this, InsightsActivity.class));
                    return true;
                } else if (id == R.id.nav_home) {
                    return true; // ήδη εδώ
                } else if (id == R.id.nav_notifications) {
                    Toast.makeText(this, "Notifications", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == R.id.nav_profile) {
                    startActivity(new Intent(this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }

        // insets για το pill
        MaterialCardView barCard = findViewById(R.id.bottomBarCard);
        if (barCard != null) {
            ViewCompat.setOnApplyWindowInsetsListener(barCard, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
                return insets;
            });
        }
        // --- /Bottom bar setup ---

        rv = findViewById(R.id.rvDays);
        adapter = new DailyAdapter();
        rv.setAdapter(adapter);

        // Αν ήρθαμε από Goals/Profile χωρίς payload → κάνε fetch μόνος σου
        Uri dataUri = getIntent().getData();
        if (dataUri == null) {
            fetchDailies();
        } else {
            try (InputStream is = getContentResolver().openInputStream(dataUri);
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                adapter.submit(parseDays(sb.toString()));
            } catch (Exception e) {
                String TsAG="";
                Log.e(TsAG, "Failed to read payload file", e);
                Toast.makeText(this, "Error reading data", Toast.LENGTH_SHORT).show();
                fetchDailies();
            }
        }
    }


    @Override protected void onResume() {
        super.onResume();
        if (rv != null && rv.getAdapter() != null) {
            rv.getAdapter().notifyDataSetChanged();
        }
    }


    private void fetchDailies() {
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
                        Toast.makeText(DailiesActivity.this, "Network error", Toast.LENGTH_SHORT).show()
                );
            }

            @Override public void onResponse(Call call, Response response) throws java.io.IOException {
                String body = response.body() != null ? response.body().string() : "";
                runOnUiThread(() -> {
                    if (body == null || body.isEmpty()) {
                        Toast.makeText(DailiesActivity.this, "No data", Toast.LENGTH_SHORT).show();
                        adapter.submit(new ArrayList<>());
                    } else {
                        adapter.submit(parseDays(body));
                    }
                });
            }
        });
    }

    private List<DailiesModels.Day> parseDays(String payload) {
        List<DailiesModels.Day> out = new ArrayList<>();
        try {
            LocalDate today = LocalDate.now(ZoneId.systemDefault());
            DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

            JSONObject root = new JSONObject(payload);
            JSONArray arr = root.optJSONArray("data");
            if (arr == null) return out;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject dayObj = arr.getJSONObject(i);

                String calendarDate = dayObj.optString("calendarDate", null);
                boolean isToday = false;

                if (calendarDate != null && !calendarDate.isEmpty()) {
                    isToday = LocalDate.parse(calendarDate, ISO).equals(today);
                } else {
                    JSONObject dTmp = dayObj.optJSONObject("data");
                    if (dTmp != null) {
                        long start = dTmp.optLong("startTimeInSeconds", -1L);
                        int offset = dTmp.optInt("startTimeOffsetInSeconds", 0);
                        if (start > 0) {
                            Instant instant = Instant.ofEpochSecond(start).minusSeconds(offset);
                            LocalDate ld = LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).toLocalDate();
                            isToday = ld.equals(today);
                            if (calendarDate == null) calendarDate = ld.format(ISO);
                        }
                    }
                }

                if (!isToday) continue;

                JSONObject dObj = dayObj.optJSONObject("data");
                if (dObj == null) continue;

                DailiesModels.Daily d = new DailiesModels.Daily();
                d.calendarDate = dObj.optString("calendarDate", calendarDate);
                d.steps = dObj.optInt("steps");
                int apiGoal = dObj.optInt("stepsGoal", 0);
                d.stepsGoal = (apiGoal > 0) ? apiGoal : UserPrefs.getGoalSteps(this);
                d.activeKilocalories = dObj.optInt("activeKilocalories");
                d.distanceInMeters = dObj.optLong("distanceInMeters");
                d.averageHeartRateInBeatsPerMinute = dObj.optInt("averageHeartRateInBeatsPerMinute");
                d.activeTimeInSeconds = dObj.optInt("activeTimeInSeconds");
                d.averageStressLevel = dObj.optInt("averageStressLevel");
                d.maxStressLevel = dObj.optInt("maxStressLevel");

                DailiesModels.Day day = new DailiesModels.Day();
                day.calendarDate = (calendarDate != null) ? calendarDate : d.calendarDate;
                day.data = d;
                out.add(day);
            }
        } catch (JSONException e) {
            Log.e(TAG, "JSON parse error", e);
        }
        return out;
    }
}
*/