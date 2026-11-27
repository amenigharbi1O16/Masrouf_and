/*
 * ============================================================================
 * FICHIER : SystemIntents.java
 * MISSION : Centralise les Intents implicites Android sortants vers les
 *           applications externes du téléphone : composeur téléphonique
 *           (ACTION_DIAL), messagerie SMS (ACTION_SENDTO smsto:), caméra
 *           (MediaStore.ACTION_IMAGE_CAPTURE), visionneuse PDF sécurisée via
 *           FileProvider (ACTION_VIEW), partage texte (ACTION_SEND) et copie
 *           dans le presse-papiers.
 * IMPLÉMENTE : Classe utilitaire statique vérifiant la disponibilité d'une
 *              application cible via PackageManager.resolveActivity() avant
 *              démarrage pour éviter toute ActivityNotFoundException.
 * EFFET À L'ÉCRAN : Ouvre le composeur, l'app SMS, l'appareil photo, le lecteur
 *                   PDF système ou la feuille de partage native Android.
 * COMMUNICATIONS :
 *   ← SettingsFragment (appel parent/support, SMS, partage résumé)
 *   ← ChildrenManagementActivity (appel/SMS/copie email enfant)
 *   ← UserManualActivity / AddProductActivity (ouverture PDF, photo)
 *   ← FileProvider (manifeste authority ".fileprovider" + file_paths.xml)
 * ============================================================================
 */
package com.example.masrouf1;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Regroupe les Intents implicites Android (appel téléphonique, SMS, appareil photo, ouverture PDF via FileProvider,
 * partage texte et copie dans le presse-papiers) utilisés par les paramètres, formulaires produits et gestion enfants.
 */
public final class SystemIntents {
    /** Empêche l'instanciation de la classe utilitaire. */
    private SystemIntents() { }
    /** Lance le composeur téléphonique avec le numéro de support par défaut. */
    public static void dialHousehold(Activity activity) { dialHousehold(activity,"+21671000000"); }
    /** Lance le composeur téléphonique système (ACTION_DIAL) sans nécessiter de permission CALL_PHONE. */
    public static void dialHousehold(Activity activity,String phone) {
        String normalized=phone==null?"":phone.trim();
        if(normalized.isEmpty()) { Toast.makeText(activity,R.string.parent_phone_missing,Toast.LENGTH_SHORT).show(); return; }
        launch(activity,new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(normalized))));
    }
    /** Ouvre l'application SMS par défaut avec le numéro de support. */
    public static void shareReceipt(Activity activity) { shareReceipt(activity,"+21671000000"); }
    /** Ouvre l'application SMS par défaut (ACTION_SENDTO smsto:) avec un message prérempli. */
    public static void shareReceipt(Activity activity,String phone) {
        String normalized=phone==null?"":phone.trim();
        if(normalized.isEmpty()) { Toast.makeText(activity,R.string.parent_phone_missing,Toast.LENGTH_SHORT).show(); return; }
        Intent intent=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(normalized))); intent.putExtra("sms_body",activity.getString(R.string.share_receipt)); launch(activity,intent);
    }
    /** Ouvre l'appareil photo natif via MediaStore.ACTION_IMAGE_CAPTURE. */
    public static void capturePhoto(Activity activity,int requestCode) {
        Intent intent=new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if(intent.resolveActivity(activity.getPackageManager())!=null) activity.startActivityForResult(intent,requestCode);
        else Toast.makeText(activity,R.string.capture_receipt,Toast.LENGTH_SHORT).show();
    }
    /** Copie le PDF manuel_utilisation depuis res/raw vers le cache interne puis l'ouvre via FileProvider + ACTION_VIEW. */
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
    /** Alias lisible de {@link #dialHousehold(Activity, String)} utilisé par ChildrenManagementActivity. */
    public static void dial(Activity activity, String phone) {
        dialHousehold(activity, phone);
    }

    /** Prépare un SMS vers un numéro (ou sélecteur de contact si vide) avec le texte fourni. */
    public static void sendSms(Activity activity, String phone, String message) {
        String normalized = phone == null ? "" : phone.trim();
        String uriStr = normalized.isEmpty() ? "smsto:" : "smsto:" + Uri.encode(normalized);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(uriStr));
        intent.putExtra("sms_body", message == null ? "" : message);
        launch(activity, intent);
    }

    /** Copie une chaîne (ex. résumé ou email) dans le presse-papiers système. */
    public static void copyToClipboard(Activity activity, String text) {
        android.content.ClipboardManager cm = (android.content.ClipboardManager)
                activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(android.content.ClipData.newPlainText(
                    activity.getString(R.string.app_name), text == null ? "" : text));
            Toast.makeText(activity, android.R.string.copy, Toast.LENGTH_SHORT).show();
        }
    }

    /** Ouvre le sélecteur de partage système (ACTION_SEND text/plain) pour diffuser le résumé budgétaire. */
    public static void shareText(Activity activity, String subject, String body) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("text/plain");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject == null ? "" : subject);
        sendIntent.putExtra(Intent.EXTRA_TEXT, body == null ? "" : body);
        launch(activity, Intent.createChooser(sendIntent, activity.getString(R.string.share_summary_chooser)));
    }

    /** Vérifie qu'une application cible existe (resolveActivity) avant de démarrer l'Intent implicite. */
    private static void launch(Activity activity,Intent intent) {
        if(intent.resolveActivity(activity.getPackageManager())!=null) activity.startActivity(intent);
        else Toast.makeText(activity,R.string.app_name,Toast.LENGTH_SHORT).show();
    }
}
