/*
 * ============================================================================
 * FICHIER : AchievementPlayer.java
 * MISSION : Jouer un effet sonore court de réussite lors d'une récompense ou clôture budgétaire positive.
 * IMPLÉMENTE : Utilitaire statique non instanciable encapsulant android.media.MediaPlayer.
 * EFFET À L'ÉCRAN : Aucun widget dessiné ; produit le son res/raw/achievement.mp3 sur le haut-parleur du téléphone.
 * COMMUNICATIONS : Appelé par CongratulationsActivity / RewardsActivity lors d'un succès utilisateur.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.media.MediaPlayer;

/**
 * Lecteur audio local léger basé sur android.media.MediaPlayer.
 */
public final class AchievementPlayer {
    // Constructeur privé : cette classe utilitaire ne contient que la méthode statique play().
    private AchievementPlayer(){}

    /**
     * Lance la lecture ponctuelle du son de réussite res/raw/achievement.mp3 puis libère la ressource audio.
     * @param context contexte Android (converti en ApplicationContext pour éviter toute fuite d'Activity).
     */
    public static void play(Context context){
        // Initialise MediaPlayer avec le contexte applicatif et le fichier audio embarqué.
        MediaPlayer player=MediaPlayer.create(context.getApplicationContext(),R.raw.achievement);
        // Si l'appareil ou l'environnement de test ne peut pas créer le lecteur audio, on abandonne sans crash.
        if(player==null)return;
        // Libère automatiquement les ressources natives MediaPlayer dès la fin normale de la lecture.
        player.setOnCompletionListener(MediaPlayer::release);
        // Libère également le lecteur en cas d'erreur matérielle ou codec et signale l'erreur comme traitée.
        player.setOnErrorListener((media,what,extra)->{media.release();return true;});
        // Démarre la lecture audio en protégeant l'appel contre une éventuelle IllegalStateException.
        try{player.start();}catch(RuntimeException error){player.release();}
    }
}
