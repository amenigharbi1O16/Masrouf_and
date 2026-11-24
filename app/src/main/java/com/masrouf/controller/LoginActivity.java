package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.UserEntity;
import com.google.android.material.card.MaterialCardView;

public final class LoginActivity extends AppCompatActivity {
    private EditText email, password;
    private MaterialCardView errorCard;
    private MasroufViewModel viewModel;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_login);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class); email=findViewById(R.id.login_email); password=findViewById(R.id.login_password);
        errorCard=findViewById(R.id.login_error_card);
        if (getIntent().hasExtra("EMAIL")) email.setText(getIntent().getStringExtra("EMAIL"));
        email.addTextChangedListener(new FormValidationTextWatcher(findViewById(R.id.login_email_feedback)));
        findViewById(R.id.login_submit).setOnClickListener(new LoginClickListener(this));
        findViewById(R.id.login_to_register).setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class).putExtra("TYPE", "personal")));
    }
    public void performLogin() {
        String address=email.getText().toString(), secret=password.getText().toString();
        if (!FormValidationTextWatcher.isGmail(address) || secret.isEmpty()) { showError(); return; }
        findViewById(R.id.login_submit).setEnabled(false);
        DevelopmentSeedManager.ensureSeeded(this,()->viewModel.<UserEntity>request(repository->repository.authenticate(address,secret),state->{
            findViewById(R.id.login_submit).setEnabled(true);
            UserEntity user=state.data;
            if (user == null) { showError(); return; }
            errorCard.setVisibility(View.GONE); SessionStore.save(this, user);
            Intent target = new Intent(this, user.onboarded ? MainActivity.class : OnboardingActivity.class);
            target.putExtra("USER_ID", user.id); startActivity(target); finish();
        }));
    }
    private void showError() { errorCard.setVisibility(View.VISIBLE); }
}
