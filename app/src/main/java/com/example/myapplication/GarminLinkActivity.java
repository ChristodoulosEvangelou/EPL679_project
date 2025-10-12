package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GarminLinkActivity extends AppCompatActivity {

    private static final String USER_ID = "3cdf364a-da5b-453f-b0e7-6983f2f1e310";
    private static final String DOMAIN_URL = "https://garmin-ucy.3ahealth.com";

    // πιθανά ονόματα που μας έδωσαν/είδαμε στα logs
    private static final String COOKIE_NAME_A = "[garmin-ucy.3ahealth.com]garmin-ucy.3ahealth.com";
    private static final String COOKIE_NAME_B = "garmin-ucy-3ahealth";

    private final String link = DOMAIN_URL + "/garmin/login?userId=" + USER_ID;

    @SuppressLint("SetJavaScriptEnabled")
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WebView wv = new WebView(this);
        setContentView(wv);
        wv.getSettings().setJavaScriptEnabled(true);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            cm.setAcceptThirdPartyCookies(wv, true);
        }

        wv.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                String all = cm.getCookie(DOMAIN_URL);
                String best = extractPreferredCookie(all);
                if (best != null) {
                    SecureCookie.store(GarminLinkActivity.this, best); // "name=value"
                    Toast.makeText(GarminLinkActivity.this, "Connected 👍", Toast.LENGTH_SHORT).show();
                    finish();
                }
                // αν δεν βρει ακόμα, θα ξανακληθεί στο επόμενο navigation του WebView
            }
        });

        wv.loadUrl(link);
    }

    /** Προσπαθεί να επιστρέψει "name=value" για τα γνωστά ονόματα, αλλιώς τον πρώτο λογικό cookie pair. */
    private String extractPreferredCookie(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        String[] parts = raw.split(";");
        String firstValid = null;

        for (String p : parts) {
            String t = p.trim();
            // αγνόησε τυπικά attributes
            if (t.equalsIgnoreCase("Secure") || t.equalsIgnoreCase("HttpOnly") || t.startsWith("Path=")
                    || t.startsWith("Expires=") || t.startsWith("SameSite")) continue;

            if (t.startsWith(COOKIE_NAME_A + "=") || t.startsWith(COOKIE_NAME_B + "=")) {
                return t; // βρέθηκε preferred
            }
            // κράτα έναν πρώτο υποψήφιο "name=value" για fallback
            if (firstValid == null && t.contains("=") && !t.startsWith("=")) {
                firstValid = t;
            }
        }
        return firstValid; // μπορεί να είναι και άλλο cookie, αλλά αρκεί αν ο proxy το δέχεται
    }
}
