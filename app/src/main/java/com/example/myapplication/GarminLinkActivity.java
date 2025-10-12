package com.example.myapplication;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class GarminLinkActivity extends AppCompatActivity {

    private final String link =
            "https://garmin-ucy.3ahealth.com/garmin/login?userId=3cdf364a-da5b-453f-b0e7-6983f2f1e310";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.contains("success") || url.contains("complete")) {
                    String cookieStr = cm.getCookie("https://garmin-ucy.3ahealth.com");
                    if (cookieStr != null && !cookieStr.isEmpty()) {
                        SecureCookie.store(GarminLinkActivity.this, cookieStr);
                        Toast.makeText(GarminLinkActivity.this, "Registration OK", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }
            }
        });

        wv.loadUrl(link);
    }
}
