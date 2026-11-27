/*
 * ============================================================================
 * FICHIER : BudgetAllocationReceiver.java
 * MISSION : Récepteur Android (BroadcastReceiver) prévu pour convertir un broadcast interne de dotation enfant en notification.
 * IMPLÉMENTE : Hérite de android.content.BroadcastReceiver (déclaré exported="false" dans AndroidManifest.xml).
 * EFFET À L'ÉCRAN : Aucun affichage direct ; génère le cas échéant une notification système via BudgetNotificationManager.
 * COMMUNICATIONS : Audit — aucune classe ne diffuse actuellement ACTION_CHILD_ALLOCATION via sendBroadcast(),
 *                  le Repository notifiant directement via NotificationEntity/BudgetNotificationManager.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * BroadcastReceiver local déclaré dans AndroidManifest.xml (exported="false") pour l'action CHILD_ALLOCATION.
 */
public final class BudgetAllocationReceiver extends BroadcastReceiver {
    // Identifiant d'action attendu dans l'Intent reçu par onReceive().
    public static final String ACTION_CHILD_ALLOCATION="com.masrouf.action.CHILD_ALLOCATION";
    // Clés des extras attendus : identifiant de l'enfant, montant en millimes et clé anti-doublon.
    public static final String EXTRA_CHILD_USER_ID="child_user_id";
    public static final String EXTRA_AMOUNT_MILLI="amount_milli";
    public static final String EXTRA_EVENT_KEY="event_key";

    /**
     * Reçoit l'Intent de dotation, vérifie la validité des paramètres et déclenche la notification de réception d'argent.
     * @param context contexte Android fourni par le système lors de la livraison du broadcast.
     * @param intent Intent contenant ACTION_CHILD_ALLOCATION et ses trois extras obligatoires.
     */
    @Override public void onReceive(Context context,Intent intent) {
        // Ignore tout Intent nul ou ne correspondant pas à l'action ACTION_CHILD_ALLOCATION.
        if(intent==null||!ACTION_CHILD_ALLOCATION.equals(intent.getAction())) return;
        // Extrait l'identifiant du compte enfant et le montant alloué en millimes (0L si absent).
        long childId=intent.getLongExtra(EXTRA_CHILD_USER_ID,0L), amount=intent.getLongExtra(EXTRA_AMOUNT_MILLI,0L);
        // Lit la clé unique d'événement évitant de poster deux fois la même alerte système.
        String eventKey=intent.getStringExtra(EXTRA_EVENT_KEY);
        // Interrompt le traitement si l'un des paramètres requis est absent ou invalide.
        if(childId<=0||amount<=0||eventKey==null||eventKey.trim().isEmpty()) return;
        // Délègue au gestionnaire de notifications avec un ID entier positif déduit du hash d'eventKey.
        BudgetNotificationManager.allowanceReceived(context,(int)(eventKey.hashCode()&0x7fffffff),childId,amount,eventKey);
    }
}
