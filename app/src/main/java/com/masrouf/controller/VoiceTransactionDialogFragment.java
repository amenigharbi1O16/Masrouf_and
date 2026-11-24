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

import com.example.masrouf1.BudgetNotificationManager;
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
        locale=new Spinner(requireContext());locale.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_spinner_dropdown_item,new String[]{"ar-TN","fr-FR"}));form.addView(locale,new LinearLayout.LayoutParams(-1,dp(48)));
        mic=new MaterialButton(requireContext());mic.setText(R.string.start);mic.setIconResource(R.drawable.ic_mic_mint);form.addView(mic,new LinearLayout.LayoutParams(-1,dp(52)));
        phrase=field(R.string.voice_text_preview,form);label=field(R.string.transaction_name,form);amount=field(R.string.amount_dinars,form);amount.setInputType(android.text.InputType.TYPE_CLASS_NUMBER|android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        typePreview=new TextView(requireContext());typePreview.setTextColor(requireContext().getColor(R.color.muted_green));typePreview.setPadding(0,dp(8),0,dp(4));form.addView(typePreview);
        AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle(R.string.voice_transaction).setView(form).setNegativeButton(android.R.string.cancel,null).setPositiveButton(R.string.save,null).create();
        dialog.setOnShowListener(ignored->{dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->saveDraft());mic.setOnClickListener(v->requestOrStart());});return dialog;
    }
    private EditText field(int hint,LinearLayout parent){EditText input=new EditText(requireContext());input.setSingleLine(true);input.setHint(hint);parent.addView(input,new LinearLayout.LayoutParams(-1,dp(50)));return input;}
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
    private void preview(String text){parsed=VoiceParser.parse(text);phrase.setText(text);label.setText(parsed.label);amount.setText(BigDecimal.valueOf(parsed.amountMilli,3).stripTrailingZeros().toPlainString());typePreview.setText(getString(R.string.voice_parsed_type,parsed.type,VoiceParser.toArabicScript(text)));status.setText(R.string.voice_preview_editable);}
    private void startPulse(){if(pulse!=null)pulse.cancel();pulse=android.animation.ObjectAnimator.ofFloat(mic,View.SCALE_X,1f,1.12f,1f);pulse.setDuration(650);pulse.setRepeatCount(android.animation.ValueAnimator.INFINITE);pulse.setInterpolator(new LinearInterpolator());pulse.start();mic.animate().scaleY(1.12f).setDuration(320).withEndAction(()->mic.animate().scaleY(1f).setDuration(320).start()).start();}
    private void stopPulse(){if(pulse!=null)pulse.cancel();if(mic!=null){mic.setScaleX(1f);mic.setScaleY(1f);}}
    private void saveDraft(){String title=label.getText().toString().trim();long amountMilli;try{amountMilli=MoneyInput.toMilli(amount.getText().toString());if(amountMilli<=0)throw new NumberFormatException();}catch(RuntimeException ex){amount.setError(getString(R.string.money_invalid));return;}
        VoiceParser.Result intent=parsed==null?VoiceParser.parse(phrase.getText().toString()):parsed;
        if("child_allowance".equals(intent.type)){chooseChildAndGive(title,amountMilli);return;}
        Intent next=new Intent(requireContext(),AddExpenseActivity.class).putExtra("VOICE_LABEL",title).putExtra("VOICE_AMOUNT",BigDecimal.valueOf(amountMilli,3).stripTrailingZeros().toPlainString()).putExtra("VOICE_KIND",intent.type);startActivity(next);dismiss();
    }
    private void chooseChildAndGive(String note,long amountMilli){long parentId=SessionStore.userId(requireContext());viewModel.<List<ChildSpendingSummary>>request(repository->repository.familyChildren(parentId),state->{if(!isAdded())return;List<ChildSpendingSummary> children=state.data;if(children==null||children.isEmpty()){Toast.makeText(requireContext(),R.string.children_empty,Toast.LENGTH_SHORT).show();return;}String[] labels=new String[children.size()];for(int i=0;i<children.size();i++)labels[i]=children.get(i).nickname;new AlertDialog.Builder(requireContext()).setTitle(R.string.choose_allowance_child).setItems(labels,(d,which)->giveMoney(parentId,children.get(which),amountMilli,note)).setNegativeButton(android.R.string.cancel,null).show();});}
    private void giveMoney(long parentId,ChildSpendingSummary child,long amount,String note){long occurredAt=System.currentTimeMillis();viewModel.<Integer>request(repository->repository.giveMoney(parentId,child.childProfileId,amount,note,occurredAt),state->{if(!isAdded())return;Integer result=state.data;if(result!=null&&result==0){BudgetNotificationManager.allowanceReceived(requireContext(),(int)(child.childUserId&0x7fffffff),child.childUserId,amount);Toast.makeText(requireContext(),R.string.family_gift_saved,Toast.LENGTH_SHORT).show();dismiss();}else Toast.makeText(requireContext(),R.string.transaction_saved_error,Toast.LENGTH_SHORT).show();});}
    @Override public void onDestroy(){stopPulse();if(recognizer!=null){recognizer.destroy();recognizer=null;}super.onDestroy();}
    private int dp(int value){return(int)(value*getResources().getDisplayMetrics().density+.5f);}
}
