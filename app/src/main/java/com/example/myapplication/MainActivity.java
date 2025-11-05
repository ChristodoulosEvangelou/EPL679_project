package com.example.myapplication;
// MainActivity.java
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "DailiesCall";
    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310"; // βάλ' το δικό σας

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = findViewById(R.id.btnConnectGarmin);
//        btnConnect.setOnClickListener(v -> {
//            // άνοιξε το registration flow (WebView)
//            startActivity(new Intent(this, GarminLinkActivity.class));
//        });
        // MainActivity.java (στο onClick του Connect)
        btnConnect.setOnClickListener(v -> {
            String cookie = SecureCookie.get(getApplicationContext());
            if (cookie != null && !cookie.isEmpty()) {
                Toast.makeText(this, "Ήδη συνδεδεμένο. Θα χρησιμοποιήσω το υπάρχον session.", Toast.LENGTH_SHORT).show();
                // Optional: fetchDailies();
            } else {
                // Go to link/registration flow (no payload here)
                startActivity(new Intent(this, GarminLinkActivity.class));
            }
        });

        Button btnFetch = findViewById(R.id.btnFetchDailies); // βάλε δεύτερο κουμπί στο XML αν θέλεις
        if (btnFetch != null) {
            btnFetch.setOnClickListener(v -> fetchDailies());
        }
    }

    @Override protected void onResume() {
        super.onResume();
        // Αν έχεις ήδη κάνει registration και υπάρχει cookie, μπορείς να κάνεις auto-fetch
        // fetchDailies();
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
                        // cookie έληξε → ξανακάνε registration
                        runOnUiThread(() -> {
                            startActivity(new Intent(this, GarminLinkActivity.class));
                        });
                    }
                    return res;
                })
                .build();

        String url = "https://garmin-ucy.3ahealth.com/garmin/dailies?garminUserId=" + USER_ID;

        Request request = new Request.Builder().url(url).get().build();

        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Network error", e);
                runOnUiThread(() -> {
                    // Προβάλλε το error στην οθόνη εμφάνισης για συνέπεια
                    Intent it = new Intent(MainActivity.this, DailiesActivity.class);
                    it.putExtra(DailiesActivity.EXTRA_JSON,
                            "{ \"error\": \"Network error\", \"message\": \"" + e.getMessage() + "\" }");
                    startActivity(it);
                });
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                final boolean ok = response.isSuccessful();
                final String body = response.body() != null ? response.body().string() : "";

                runOnUiThread(() -> {
                    if (!ok) {
                        Toast.makeText(MainActivity.this, "Request failed: " + response.code(), Toast.LENGTH_SHORT).show();
                        // ΜΗΝ ανοίγεις activity με ολόκληρο το body όταν αποτυγχάνει
                        return;
                    }

                    try {
                        File f = File.createTempFile("garmin_dailies_", ".json", getCacheDir());
                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            fos.write((body == null || body.isEmpty() ? "{ \"data\": [] }" : body).getBytes(StandardCharsets.UTF_8));
                        }

                        Uri uri = FileProvider.getUriForFile(
                                MainActivity.this,
                                getPackageName() + ".provider",
                                f
                        );

                        Intent it = new Intent(MainActivity.this, DailiesActivity.class)
                                .setData(uri)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(it);
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to pass payload via file", e);
                        Toast.makeText(MainActivity.this, "Internal error saving response", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        });
    }
}
