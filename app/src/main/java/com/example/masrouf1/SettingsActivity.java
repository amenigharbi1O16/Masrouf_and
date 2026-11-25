package com.example.masrouf1;

import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.example.masrouf1.AppRole;
import com.example.masrouf1.LocaleController;
import com.masrouf.controller.SessionStore;
import com.masrouf.controller.InputValidation;
import com.example.masrouf1.OperationResult;
import com.example.masrouf1.db.MasroufDatabase;
import com.google.android.material.button.MaterialButton;
import android.widget.Toast;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsActivity extends SectionActivity {
    private static final ExecutorService BACKUP_EXECUTOR=Executors.newSingleThreadExecutor();
    private volatile String parentPhone="";
    private MasroufViewModel viewModel;
    @Override protected int titleResource() { return R.string.page_settings; }
    @Override protected View createSectionContent() {
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        TextView note = bodyText(getString(R.string.offline_note)); content.addView(note);
        TextView emailLabel = bodyText(getString(R.string.email_address));
        EditText email = new EditText(this); email.setId(View.generateViewId()); emailLabel.setLabelFor(email.getId()); email.setHint(R.string.hint_email); email.setSingleLine(true);
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        content.addView(emailLabel, new LinearLayout.LayoutParams(-1, dp(24)));
        content.addView(email, new LinearLayout.LayoutParams(-1, dp(58)));
        TextView feedback = new TextView(this); feedback.setTextColor(getColor(R.color.muted_green)); feedback.setTextSize(12f);
        content.addView(feedback, new LinearLayout.LayoutParams(-1, dp(32)));
        email.addTextChangedListener(new GmailValidationWatcher(feedback));
        long currentUser=SessionStore.userId(this);viewModel.loadUser(currentUser);
        viewModel.user().observe(this,state->{if(state!=null&&state.data!=null&&email.getText().toString().isEmpty())email.setText(state.data.email);});
        MaterialButton saveEmail=new MaterialButton(this);saveEmail.setText(R.string.save_email);saveEmail.setAllCaps(false);content.addView(saveEmail,new LinearLayout.LayoutParams(-1,dp(48)));
        saveEmail.setOnClickListener(v->{String value=InputValidation.normalizeGmail(email.getText().toString());if(!InputValidation.isStrictGmail(value)){feedback.setText(R.string.email_error);feedback.setTextColor(getColor(R.color.coral));return;}if(!email.getText().toString().equals(value))email.setText(value);saveEmail.setEnabled(false);viewModel.<OperationResult<Boolean>>request(repository->repository.updateEmail(currentUser,value),result->{saveEmail.setEnabled(true);OperationResult<Boolean> outcome=result.data;if(outcome!=null&&outcome.succeeded()){feedback.setText(R.string.email_saved);feedback.setTextColor(getColor(R.color.emerald));}else{feedback.setText(outcome!=null&&"duplicate".equals(outcome.error)?R.string.email_already_used:R.string.email_error);feedback.setTextColor(getColor(R.color.coral));}});});
        TextView language = bodyText(getString(R.string.language_choose_drawer));
        content.addView(language, new LinearLayout.LayoutParams(-1, dp(44)));
        // Language picker buttons: FR / AR / EN
        LinearLayout langRow = new LinearLayout(this);
        langRow.setOrientation(LinearLayout.HORIZONTAL);
        langRow.setPadding(0, dp(4), 0, dp(4));
        String[] langTags   = {"fr", "ar", "en"};
        String[] langLabels = {"Français", "العربية", "English"};
        for (int li = 0; li < langTags.length; li++) {
            final String langTag = langTags[li];
            MaterialButton langBtn = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            langBtn.setText(langLabels[li]);
            langBtn.setAllCaps(false);
            langBtn.setTextSize(13f);
            langBtn.setOnClickListener(vv -> LocaleController.apply(langTag));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(46), 1f);
            lp.setMargins(li == 0 ? 0 : dp(4), 0, 0, 0);
            langRow.addView(langBtn, lp);
        }
        content.addView(langRow, new LinearLayout.LayoutParams(-1, dp(54)));
        boolean child=SessionStore.role(this)==AppRole.CHILD;
        MaterialButton call=new MaterialButton(this); call.setText(child?R.string.call_parent:R.string.call_support); call.setAllCaps(false); call.setOnClickListener(v->{ if(child) SystemIntents.dialHousehold(this,parentPhone); else SystemIntents.dialHousehold(this); }); content.addView(call,new LinearLayout.LayoutParams(-1,dp(48)));
        MaterialButton sms=new MaterialButton(this); sms.setText(child?R.string.message_parent:R.string.share_receipt); sms.setAllCaps(false); sms.setOnClickListener(v->{ if(child) SystemIntents.shareReceipt(this,parentPhone); else SystemIntents.shareReceipt(this); }); content.addView(sms,new LinearLayout.LayoutParams(-1,dp(48)));
        if(child) loadParentPhone();
        MaterialButton manual=new MaterialButton(this); manual.setText(R.string.open_manual); manual.setAllCaps(false); manual.setOnClickListener(v->startActivity(new android.content.Intent(this,UserManualActivity.class))); content.addView(manual,new LinearLayout.LayoutParams(-1,dp(48)));
        // Bouton : Modifier le mot de passe
        MaterialButton changePassword = new MaterialButton(this);
        changePassword.setText(R.string.change_password_button);
        changePassword.setAllCaps(false);
        changePassword.setOnClickListener(v -> startActivity(new android.content.Intent(this, com.masrouf.controller.ChangePasswordActivity.class)));
        content.addView(changePassword, new LinearLayout.LayoutParams(-1, dp(48)));

        MaterialButton backup=new MaterialButton(this);backup.setText(R.string.create_local_backup);backup.setAllCaps(false);content.addView(backup,new LinearLayout.LayoutParams(-1,dp(48)));
        backup.setOnClickListener(v->{backup.setEnabled(false);BACKUP_EXECUTOR.execute(()->{
            boolean created=false;try{MasroufDatabase.backupDatabase(getApplicationContext());created=true;}catch(Exception failure){android.util.Log.e("MasroufBackup","Local database backup failed",failure);}
            boolean success=created;runOnUiThread(()->{if(isFinishing()||isDestroyed())return;backup.setEnabled(true);Toast.makeText(this,success?R.string.backup_created:R.string.backup_failed,Toast.LENGTH_LONG).show();});
        });});
        return content;
    }
    private void loadParentPhone() {
        long childId=SessionStore.userId(this);
        viewModel.<String>request(repository->repository.parentPhone(childId),state->{parentPhone=state.data==null?"":state.data;});
    }
    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density + .5f); }
}
