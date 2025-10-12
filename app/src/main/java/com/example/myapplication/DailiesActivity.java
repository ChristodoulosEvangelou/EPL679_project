package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DailiesActivity extends AppCompatActivity {

    public static final String EXTRA_JSON = "dailies_json";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_dailies_activity);

        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvJson  = findViewById(R.id.tvJson);
        Button btnClose  = findViewById(R.id.btnClose);

        String raw = getIntent().getStringExtra(EXTRA_JSON);
        if (raw == null || raw.trim().isEmpty()) {
            tvJson.setText("Δεν βρέθηκαν δεδομένα.");
        } else {
            tvJson.setText(prettyJson(raw));
        }

        btnClose.setOnClickListener(v -> finish());
    }

    private String prettyJson(String raw) {
        try {
            raw = raw.trim();
            if (raw.startsWith("{")) {
                JSONObject obj = new JSONObject(raw);
                return obj.toString(2); // indent 2
            } else if (raw.startsWith("[")) {
                JSONArray arr = new JSONArray(raw);
                return arr.toString(2);
            }
            // αν δεν είναι valid JSON, γύρνα όπως είναι
            return raw;
        } catch (JSONException e) {
            return raw;
        }
    }
}
