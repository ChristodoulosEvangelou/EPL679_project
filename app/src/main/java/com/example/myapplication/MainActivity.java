package com.example.myapplication;
// MainActivity.java
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

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
        btnConnect.setOnClickListener(v -> {
            // άνοιξε το registration flow (WebView)
            startActivity(new Intent(this, GarminLinkActivity.class));
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
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "HTTP " + response.code() + " body=" + body);

                // Αν θες να ενημερώσεις UI:
                runOnUiThread(() -> {
                    // πχ. TextView tv = findViewById(R.id.tvResult);
                    // tv.setText(body);
                });
            }
        });
    }
}
