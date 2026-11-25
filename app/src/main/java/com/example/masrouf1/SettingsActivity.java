package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/** Redirects directly to the full Settings tab in MainActivity without extra clicks or submenus. */
public final class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, MainActivity.class)
                .putExtra(MainActivity.EXTRA_CENTRAL_TAB, "settings")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
