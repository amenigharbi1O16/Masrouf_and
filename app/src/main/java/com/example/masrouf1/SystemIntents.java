package com.example.masrouf1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/** Shared implicit actions used by account settings and capture forms. */
public final class SystemIntents {
    private SystemIntents() { }
    public static void dialHousehold(Activity activity) { dialHousehold(activity,"+21671000000"); }
    public static void dialHousehold(Activity activity,String phone) {
        String normalized=phone==null?"":phone.trim();
        if(normalized.isEmpty()) { Toast.makeText(activity,R.string.parent_phone_missing,Toast.LENGTH_SHORT).show(); return; }
        launch(activity,new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(normalized))));
    }
    public static void shareReceipt(Activity activity) { shareReceipt(activity,"+21671000000"); }
    public static void shareReceipt(Activity activity,String phone) {
        String normalized=phone==null?"":phone.trim();
        if(normalized.isEmpty()) { Toast.makeText(activity,R.string.parent_phone_missing,Toast.LENGTH_SHORT).show(); return; }
        Intent intent=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(normalized))); intent.putExtra("sms_body",activity.getString(R.string.share_receipt)); launch(activity,intent);
    }
    public static void capturePhoto(Activity activity,int requestCode) {
        Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(activity.getPackageManager())!=null) activity.startActivityForResult(intent,requestCode);
        else Toast.makeText(activity,R.string.capture_receipt,Toast.LENGTH_SHORT).show();
    }
    public static void openManual(Activity activity) {
        try {
            File folder=new File(activity.getCacheDir(),"manual"); if(!folder.exists()&&!folder.mkdirs()) throw new IllegalStateException("manual cache");
            File target=new File(folder,"manuel_utilisation.pdf");
            if(!target.exists()||target.length()==0) {
                try(InputStream in=activity.getResources().openRawResource(R.raw.manuel_utilisation); FileOutputStream out=new FileOutputStream(target)) {
                    byte[] buffer=new byte[8192]; int read; while((read=in.read(buffer))!=-1) out.write(buffer,0,read);
                }
            }
            Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",target);
            Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/pdf"); view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            Intent chooser=Intent.createChooser(view,activity.getString(R.string.open_manual)); launch(activity,chooser);
        } catch(Exception ignored) { Toast.makeText(activity,R.string.open_manual,Toast.LENGTH_SHORT).show(); }
    }
    private static void launch(Activity activity,Intent intent) {
        if(intent.resolveActivity(activity.getPackageManager())!=null) activity.startActivity(intent);
        else Toast.makeText(activity,R.string.app_name,Toast.LENGTH_SHORT).show();
    }
}
