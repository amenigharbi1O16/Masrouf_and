package com.masrouf.controller;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Gravity;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.ChildSpendingSummary;
import com.google.android.material.button.MaterialButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class VoiceTransactionDialogFragment extends DialogFragment {
    private static final int REQUEST_AUDIO=811;
    private MasroufViewModel viewModel;
    private SpeechRecognizer recognizer;
    private EditText phrase,label,amount;
    private Spinner locale;
    private TextView status,typePreview;
    private MaterialButton mic;
    private android.animation.ObjectAnimator pulse;
    private VoiceParser.Result parsed;

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        viewModel=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
        LinearLayout form=new LinearLayout(requireContext());form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(22),dp(8),dp(22),dp(8));
        status=new TextView(requireContext());status.setText(R.string.voice_transaction_hint);form.addView(status);
        TextView localeLabel=new TextView(requireContext());localeLabel.setText(R.string.voice_language_label);localeLabel.setTextColor(requireContext().getColor(R.color.muted_green));localeLabel.setTextSize(12);form.addView(localeLabel);
        locale=new Spinner(requireContext());locale.setContentDescription(getString(R.string.voice_language_label));locale.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"ar-TN","fr-FR"}));form.addView(locale,new LinearLayout.LayoutParams(-1,dp(48)));
        mic=new MaterialButton(requireContext());mic.setText(R.string.start);mic.setIconResource(R.drawable.ic_mic_mint);form.addView(mic,new LinearLayout.LayoutParams(-1,dp(52)));
        phrase=field(R.string.voice_text_preview,R.string.hint_voice_text,form);label=field(R.string.transaction_name,R.string.hint_transaction_name,form);amount=field(R.string.amount_dinars,R.string.hint_amount,form);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        typePreview=new TextView(requireContext());typePreview.setTextColor(requireContext().getColor(R.color.muted_green));typePreview.setPadding(0,dp(8),0,dp(4));form.addView(typePreview);
        AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle(R.string.voice_transaction).setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(ignored->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->saveDraft());mic.setOnClickListener(v->requestOrStart());});return dialog;
    }
    private EditText field(int labelResource,int hintResource,LinearLayout parent){EditText input=new EditText(requireContext());input.setId(View.generateViewId());input.setSingleLine(true);input.setHint(hintResource);TextView labelView=new TextView(requireContext());labelView.setText(labelResource);labelView.setLabelFor(input.getId());labelView.setTextColor(requireContext().getColor(R.color.muted_green));labelView.setTextSize(12);parent.addView(labelView);parent.addView(input,new LinearLayout.LayoutParams(-1,dp(50)));return input;}
    private void requestOrStart(){if(ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_AUDIO);return;}startRecognition();}
    @Override public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] results){super.onRequestPermissionsResult(requestCode,permissions,results);if(requestCode==REQUEST_AUDIO&&results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)startRecognition();else if(requestCode==REQUEST_AUDIO)status.setText(R.string.voice_permission_denied);}
    private void startRecognition(){
        try{
            if(recognizer!=null){recognizer.destroy();recognizer=null;}
            if(Build.VERSION.SDK_INT>=33&&SpeechRecognizer.isOnDeviceRecognitionAvailable(requireContext()))recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(requireContext());
            else recognizer=SpeechRecognizer.createSpeechRecognizer(requireContext());
            recognizer.setRecognitionListener(new RecognitionListener(){
                public void onReadyForSpeech(Bundle params){status.setText(R.string.voice_listening);startPulse();}
                public void onBeginningOfSpeech(){} public void onRmsChanged(float rms){} public void onBufferReceived(byte[] b){} public void onEndOfSpeech(){stopPulse();status.setText(R.string.voice_processing);}
                public void onError(int error){stopPulse();status.setText(R.string.voice_recognition_error);}
                public void onResults(Bundle results){stopPulse();ArrayList<String> matches=results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(matches!=null&&!matches.isEmpty())preview(matches.get(0));}
                public void onPartialResults(Bundle partial){ArrayList<String> matches=partial.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);if(matches!=null&&!matches.isEmpty())phrase.setText(matches.get(0));}
                public void onEvent(int type,Bundle params){}
            });
            Intent intent=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,locale.getSelectedItem().toString());intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true);intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,true);intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,3);recognizer.startListening(intent);
        }catch(RuntimeException error){stopPulse();status.setText(R.string.voice_unavailable);}
    }
    private void preview(String text){parsed=VoiceParser.parse(text);phrase.setText(text);label.setText(localizedLabel(parsed));amount.setText(parsed.amountMilli<=0?"":BigDecimal.valueOf(parsed.amountMilli,3).stripTrailingZeros().toPlainString());typePreview.setText(getString(R.string.voice_parsed_type,getString(typeResource(parsed.type)),VoiceParser.toArabicScript(text)));status.setText(parsed.ambiguous?R.string.voice_ambiguous_review:R.string.voice_preview_editable);}
    private String localizedLabel(VoiceParser.Result value){switch(value.type){case "child_allowance":return getString(R.string.voice_action_allowance);case "donation":return getString(R.string.voice_action_donation);case "purchase":return value.productKey.isEmpty()?getString(R.string.voice_action_purchase):getString(productResource(value.productKey));default:return phrase.getText().length()==0?getString(R.string.voice_action_expense):value.label.equals("Dépense")?phrase.getText().toString().trim():getString(R.string.voice_action_expense);}}
    private int typeResource(String type){switch(type){case "purchase":return R.string.voice_action_purchase;case "donation":return R.string.voice_action_donation;case "child_allowance":return R.string.voice_action_allowance;default:return R.string.voice_action_expense;}}
    private int productResource(String product){switch(product){case "bread":return R.string.voice_product_bread;case "milk":return R.string.voice_product_milk;case "meat":return R.string.voice_product_meat;case "chicken":return R.string.voice_product_chicken;case "oil":return R.string.voice_product_oil;case "sugar":return R.string.voice_product_sugar;case "coffee":return R.string.voice_product_coffee;case "vegetables":return R.string.voice_product_vegetables;case "fruit":return R.string.voice_product_fruit;default:return R.string.voice_action_purchase;}}
    private void startPulse(){if(pulse!=null)pulse.cancel();pulse=android.animation.ObjectAnimator.ofFloat(mic,View.SCALE_X,1f,1.12f,1f);pulse.setDuration(650);pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);pulse.setInterpolator(new LinearInterpolator());pulse.start();mic.animate().scaleY(1.12f).setDuration(320).withEndAction(()->mic.animate().scaleY(1f).setDuration(320).start()).start();}
    private void stopPulse(){if(pulse!=null)pulse.cancel();if(mic!=null){mic.setScaleX(1f);mic.setScaleY(1f);}}
    private void saveDraft(){String title=label.getText().toString().trim();long amountMilli;try{amountMilli=MoneyInput.toMilli(amount.getText().toString());if(amountMilli<=0)throw new NumberFormatException();}catch(RuntimeException ex){amount.setError(getString(R.string.money_invalid));return;}
        VoiceParser.Result intent=parsed==null?VoiceParser.parse(phrase.getText().toString()):parsed;
        Runnable continueWithForm=()->{
            if("child_allowance".equals(intent.type)){chooseChildAndGive(title,amountMilli,intent.childName);return;}
            Class<?> destination="purchase".equals(intent.type)?AddProductActivity.class:AddExpenseActivity.class;
            Intent next=new Intent(requireContext(),destination).putExtra("VOICE_LABEL",title).putExtra("VOICE_AMOUNT",BigDecimal.valueOf(amountMilli,3).stripTrailingZeros().toPlainString()).putExtra("VOICE_KIND",intent.type);startActivity(next);dismiss();
        };
        if(intent.ambiguous)new AlertDialog.Builder(requireContext()).setTitle(R.string.voice_ambiguous_title).setMessage(R.string.voice_ambiguous_review).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.voice_continue_editing,(dialog,which)->continueWithForm.run()).show();else continueWithForm.run();
    }
    private void chooseChildAndGive(String note,long amountMilli,String recognizedName){long parentId=SessionStore.userId(requireContext());viewModel.<List<ChildSpendingSummary>>request(repository->repository.familyChildren(parentId),state->{if(!isAdded())return;List<ChildSpendingSummary> children=state.data;if(children==null||children.isEmpty()){Toast.makeText(requireContext(),R.string.children_empty,Toast.LENGTH_SHORT).show();return;}List<ChildSpendingSummary> matches=new ArrayList<>();String target=VoiceParser.normalizeName(recognizedName);if(!target.isEmpty())for(ChildSpendingSummary child:children)if(target.equals(VoiceParser.normalizeName(child.nickname)))matches.add(child);
        if(matches.size()==1){ChildSpendingSummary chosen=matches.get(0);new AlertDialog.Builder(requireContext()).setTitle(R.string.choose_allowance_child).setMessage(getString(R.string.voice_allowance_confirmation,chosen.nickname,BigDecimal.valueOf(amountMilli,3).stripTrailingZeros().toPlainString())).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.voice_confirm_allocation,(d,which)->giveMoney(parentId,chosen,amountMilli,note)).show();return;}
        String[] labels=new String[children.size()];for(int i=0;i<children.size();i++)labels[i]=children.get(i).nickname;new AlertDialog.Builder(requireContext()).setTitle(R.string.choose_allowance_child).setItems(labels,(d,which)->new AlertDialog.Builder(requireContext()).setMessage(getString(R.string.voice_allowance_confirmation,children.get(which).nickname,BigDecimal.valueOf(amountMilli,3).stripTrailingZeros().toPlainString())).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.voice_confirm_allocation,(confirmDialog,confirmWhich)->giveMoney(parentId,children.get(which),amountMilli,note)).show()).setNegativeButton(android.R.string.cancel,null).show();});}
    private void giveMoney(long parentId,ChildSpendingSummary child,long amount,String note){long occurredAt=System.currentTimeMillis();viewModel.<Integer>request(repository->repository.giveMoney(parentId,child.childProfileId,amount,note,occurredAt),state->{if(!isAdded())return;Integer result=state.data;if(result!=null&&result==0){Toast.makeText(requireContext(),R.string.family_gift_saved,Toast.LENGTH_SHORT).show();dismiss();}else Toast.makeText(requireContext(),R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();});}
    @Override public void onDestroy(){stopPulse();if(recognizer!=null){recognizer.destroy();recognizer=null;}super.onDestroy();}
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
