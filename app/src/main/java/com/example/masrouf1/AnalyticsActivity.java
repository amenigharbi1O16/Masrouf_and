package com.example.masrouf1;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Compatibility route that selects analytics in the side-drawer host. */
public final class AnalyticsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state){super.onCreate(state);startActivity(new Intent(this,MainActivity.class).putExtra(MainActivity.EXTRA_CENTRAL_TAB,"analytics").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
}
