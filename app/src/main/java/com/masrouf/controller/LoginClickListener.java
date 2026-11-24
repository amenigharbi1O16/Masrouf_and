package com.masrouf.controller;

import android.view.View;

public final class LoginClickListener implements View.OnClickListener {
    private final LoginActivity activity;
    public LoginClickListener(LoginActivity activity) { this.activity = activity; }
    @Override public void onClick(View view) { activity.performLogin(); }
}
