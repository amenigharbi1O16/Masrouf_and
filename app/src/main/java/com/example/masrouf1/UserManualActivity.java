package com.example.masrouf1;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Copies the bundled guide to app cache and dispatches it through FileProvider. */
public final class UserManualActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);SystemIntents.openManual(this);finish();}
}
