package com.masrouf.controller;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.masrouf1.R;
import com.example.masrouf1.AppRole;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.OperationResult;

public final class RegisterActivity extends AppCompatActivity {
    private EditText name, email, password, confirm;
    private TextView error;
    private MasroufViewModel viewModel;
    private String accountType;
    private final int[] ruleIds={R.id.rule_length,R.id.rule_uppercase,R.id.rule_lowercase,R.id.rule_number,R.id.rule_symbol};
    private final int[] ruleTextIds=InputValidation.passwordRuleStringIds();
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_register);viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        name=findViewById(R.id.register_name); email=findViewById(R.id.register_email); password=findViewById(R.id.register_password); confirm=findViewById(R.id.register_confirm); error=findViewById(R.id.register_error);
        accountType=getIntent().getStringExtra("TYPE"); if(accountType==null) accountType="personal";
        ((TextView)findViewById(R.id.register_type)).setText("family".equals(accountType)?R.string.family_account:R.string.personal_account);
        email.addTextChangedListener(new FormValidationTextWatcher(findViewById(R.id.register_email_feedback)));
        password.addTextChangedListener(new PasswordRulesWatcher());
        findViewById(R.id.register_submit).setOnClickListener(v -> register());
    }
    private void register() {
        String fullName=name.getText().toString().trim(), address=email.getText().toString(), secret=password.getText().toString();
        if(fullName.isEmpty()) { error.setText(R.string.full_name); return; }
        if(!FormValidationTextWatcher.isGmail(address)) { error.setText(R.string.email_error); return; }
        if(!InputValidation.isValidPassword(secret)) { error.setText(InputValidation.passwordError(this,secret)); return; }
        if(!secret.equals(confirm.getText().toString())) { error.setText(R.string.passwords_differ); return; }
        findViewById(R.id.register_submit).setEnabled(false);
        String role="family".equals(accountType)?AppRole.PARENT.value():AppRole.USER.value();
        viewModel.<OperationResult<Long>>request(repository->repository.register(fullName,address,secret,role,accountType),state->{
            findViewById(R.id.register_submit).setEnabled(true);
            OperationResult<Long> outcome=state.data;
            if(outcome==null||!outcome.succeeded()||outcome.value==null) { String failure=outcome==null?"save":outcome.error;if("duplicate".equals(failure))error.setText(R.string.account_exists);else if("password".equals(failure))error.setText(InputValidation.passwordError(this,secret));else error.setText(R.string.email_error); return; }
            Toast.makeText(this,R.string.account_created,Toast.LENGTH_SHORT).show();
            startActivity(new android.content.Intent(this,LoginActivity.class).putExtra("EMAIL",address)); finish();
        });
    }
    private final class PasswordRulesWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) { }
        @Override public void onTextChanged(CharSequence s,int st,int before,int count) {
            boolean[] valid=InputValidation.passwordRules(s==null?"":s.toString());
            for(int i=0;i<ruleIds.length;i++) { TextView row=findViewById(ruleIds[i]); row.setText((valid[i]?"✓  ":"○  ")+getString(ruleTextIds[i])); row.setTextColor(getColor(valid[i]?com.example.masrouf1.R.color.emerald:com.example.masrouf1.R.color.muted_green)); }
        }
        @Override public void afterTextChanged(Editable s) { }
    }
}
