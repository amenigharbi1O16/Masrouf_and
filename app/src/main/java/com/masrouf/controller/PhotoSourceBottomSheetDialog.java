package com.masrouf.controller;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.example.masrouf1.R;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Offers the two native Android photo sources without requesting broad storage access. */
public final class PhotoSourceBottomSheetDialog extends BottomSheetDialogFragment {
    public interface Listener { void onPhotoSelected(String path); }
    private static final int CAMERA=923, GALLERY=924;
    private String cameraPath;

    @NonNull @Override public android.app.Dialog onCreateDialog(@Nullable Bundle state) {
        android.app.Dialog dialog=super.onCreateDialog(state);
        LinearLayout choices=new LinearLayout(requireContext()); choices.setOrientation(LinearLayout.VERTICAL); choices.setPadding(dp(20),dp(12),dp(20),dp(20));
        MaterialButton camera=new MaterialButton(requireContext()); camera.setText(R.string.photo_camera); choices.addView(camera,new LinearLayout.LayoutParams(-1,dp(50)));
        MaterialButton gallery=new MaterialButton(requireContext()); gallery.setText(R.string.photo_gallery); choices.addView(gallery,new LinearLayout.LayoutParams(-1,dp(50)));
        camera.setOnClickListener(v->openCamera()); gallery.setOnClickListener(v->openGallery()); dialog.setContentView(choices);
        return dialog;
    }
    private void openCamera() {
        try {
            File folder=new File(requireContext().getCacheDir(),"photos"); if(!folder.exists()&&!folder.mkdirs()) throw new IllegalStateException();
            File target=new File(folder,"product-"+System.currentTimeMillis()+".jpg"); cameraPath=target.getAbsolutePath();
            Uri output=FileProvider.getUriForFile(requireContext(),requireContext().getPackageName()+".fileprovider",target);
            Intent intent=new Intent(MediaStore.ACTION_IMAGE_CAPTURE); intent.putExtra(MediaStore.EXTRA_OUTPUT,output);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent,CAMERA);
        } catch(Exception error) { Toast.makeText(requireContext(),R.string.photo_unavailable,Toast.LENGTH_SHORT).show(); }
    }
    private void openGallery() {
        Intent pick=new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(pick.resolveActivity(requireContext().getPackageManager())!=null) startActivityForResult(pick,GALLERY);
        else Toast.makeText(requireContext(),R.string.photo_unavailable,Toast.LENGTH_SHORT).show();
    }
    @Override public void onActivityResult(int requestCode,int resultCode,@Nullable Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if(resultCode!=Activity.RESULT_OK) return;
        String path=cameraPath;
        if(requestCode==GALLERY&&data!=null&&data.getData()!=null) path=copyGallery(data.getData());
        if(requestCode==CAMERA&&path!=null){File captured=new File(path);if(!captured.exists()||captured.length()==0)path=null;}
        if(path!=null&&getActivity() instanceof Listener)((Listener)getActivity()).onPhotoSelected(path);
        Bundle result=new Bundle(); result.putString("photoPath",path);
        getParentFragmentManager().setFragmentResult("masrouf-photo-selected",result); dismiss();
    }
    private String copyGallery(Uri source){
        try{File folder=new File(requireContext().getCacheDir(),"photos");if(!folder.exists()&&!folder.mkdirs())return null;File target=new File(folder,"gallery-"+System.currentTimeMillis()+".jpg");try(InputStream in=requireContext().getContentResolver().openInputStream(source);FileOutputStream out=new FileOutputStream(target)){if(in==null)return null;byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);}return target.getAbsolutePath();}catch(Exception ignored){return null;}
    }
    private int dp(int value) { return (int)(value*getResources().getDisplayMetrics().density+.5f); }
}
