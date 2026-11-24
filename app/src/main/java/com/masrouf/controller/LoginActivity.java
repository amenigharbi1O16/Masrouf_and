package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.MainActivity;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.databinding.ActivityLoginBinding;
import com.example.masrouf1.db.MasroufEntities.UserEntity;

public final class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private MasroufViewModel viewModel;
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);binding=ActivityLoginBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        if(getIntent().hasExtra("EMAIL"))binding.loginEmail.setText(getIntent().getStringExtra("EMAIL"));
        binding.loginEmail.addTextChangedListener(new FormValidationTextWatcher(binding.loginEmailFeedback));
        binding.loginSubmit.setOnClickListener(new LoginClickListener(this));
        binding.loginToRegister.setOnClickListener(v->startActivity(new Intent(this,RegisterActivity.class).putExtra("TYPE","personal")));
    }
    public void performLogin() {
        String address=InputValidation.normalizeGmail(binding.loginEmail.getText().toString()),secret=binding.loginPassword.getText().toString();
        if(!InputValidation.isStrictGmail(address)||secret.isEmpty()){showError();return;}
        if(!binding.loginEmail.getText().toString().equals(address))binding.loginEmail.setText(address);
        binding.loginSubmit.setEnabled(false);
        DevelopmentSeedManager.ensureSeeded(this,()->viewModel.<UserEntity>request(repository->repository.authenticate(address,secret),state->{
            binding.loginSubmit.setEnabled(true);
            UserEntity user=state.data;
            if (user == null) { showError(); return; }
            binding.loginErrorCard.setVisibility(View.GONE); SessionStore.save(this, user);
            Intent target = new Intent(this, user.categoriesSelected&&user.onboarded ? MainActivity.class : OnboardingActivity.class);
            target.putExtra("USER_ID", user.id); startActivity(target); finish();
        }));
    }
    private void showError() { binding.loginErrorCard.setVisibility(View.VISIBLE); }
}
