package com.masrouf.controller;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.R;
import com.example.masrouf1.AppRole;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.databinding.ActivityRegisterBinding;

public final class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding binding;
    private MasroufViewModel viewModel;
    private String accountType;
    private final int[] ruleTextIds=InputValidation.passwordRuleStringIds();
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);binding=ActivityRegisterBinding.inflate(getLayoutInflater());setContentView(binding.getRoot());
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        accountType=getIntent().getStringExtra("TYPE"); if(accountType==null) accountType="personal";
        binding.registerType.setText("family".equals(accountType)?R.string.family_account:R.string.personal_account);
        binding.registerEmail.addTextChangedListener(new FormValidationTextWatcher(binding.registerEmailFeedback));
        binding.registerPassword.addTextChangedListener(new PasswordRulesWatcher());
        watchForSubmit(binding.registerName);watchForSubmit(binding.registerEmail);watchForSubmit(binding.registerConfirm);
        binding.registerSubmit.setEnabled(false);refreshPasswordIndicators("");refreshSubmitAvailability();
        binding.registerSubmit.setOnClickListener(v->register());
    }
    private void register() {
        String fullName=binding.registerName.getText().toString().trim();
        String address=InputValidation.normalizeGmail(binding.registerEmail.getText().toString());
        String secret=binding.registerPassword.getText().toString();
        if(fullName.isEmpty()){binding.registerError.setText(R.string.full_name);return;}
        if(!InputValidation.isStrictGmail(address)){binding.registerError.setText(R.string.email_error);return;}
        if(!InputValidation.isValidPassword(secret)){binding.registerError.setText(InputValidation.passwordError(this,secret));return;}
        if(!secret.equals(binding.registerConfirm.getText().toString())){binding.registerError.setText(R.string.passwords_differ);return;}
        if(!binding.registerEmail.getText().toString().equals(address))binding.registerEmail.setText(address);
        binding.registerSubmit.setEnabled(false);
        String role="family".equals(accountType)?AppRole.PARENT.value():AppRole.USER.value();
        viewModel.<OperationResult<Long>>request(repository->repository.register(fullName,address,secret,role,accountType),state->{
            binding.registerSubmit.setEnabled(true);
            OperationResult<Long> outcome=state.data;
            if(outcome==null||!outcome.succeeded()||outcome.value==null) { String failure=outcome==null?"save":outcome.error;if("duplicate".equals(failure))binding.registerError.setText(R.string.account_exists);else if("password".equals(failure))binding.registerError.setText(InputValidation.passwordError(this,secret));else binding.registerError.setText(R.string.email_error); return; }
            Toast.makeText(this,R.string.account_created,Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this,LoginActivity.class).putExtra("EMAIL",address)); finish();
        });
    }
    private void watchForSubmit(EditText field){field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){refreshSubmitAvailability();}public void afterTextChanged(Editable text){}});}
    private void refreshSubmitAvailability(){
        boolean ready=!binding.registerName.getText().toString().trim().isEmpty()
                &&InputValidation.isStrictGmail(binding.registerEmail.getText().toString())
                &&InputValidation.isValidPassword(binding.registerPassword.getText().toString())
                &&binding.registerPassword.getText().toString().equals(binding.registerConfirm.getText().toString());
        binding.registerSubmit.setEnabled(ready);
    }
    private void refreshPasswordIndicators(String value){
        boolean[] valid=InputValidation.passwordRules(value);
        TextView[] rows={binding.ruleLength,binding.ruleUppercase,binding.ruleLowercase,binding.ruleNumber,binding.ruleSymbol};
        for(int i=0;i<rows.length;i++){
            rows[i].setText((valid[i]?"✓  ":"○  ")+getString(ruleTextIds[i]));
            rows[i].setTextColor(getColor(valid[i]?R.color.emerald:R.color.muted_green));
        }
    }
    private final class PasswordRulesWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) { }
        @Override public void onTextChanged(CharSequence s,int st,int before,int count){refreshPasswordIndicators(s==null?"":s.toString());refreshSubmitAvailability();}
        @Override public void afterTextChanged(Editable s) { }
    }
}
