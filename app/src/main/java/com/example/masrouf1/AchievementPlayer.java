package com.example.masrouf1;

import android.content.Context;
import android.media.MediaPlayer;

/** Short local feedback sound for budget milestones and reward events. */
public final class AchievementPlayer {
    private AchievementPlayer(){}
    public static void play(Context context){
        MediaPlayer player=MediaPlayer.create(context.getApplicationContext(),R.raw.achievement);
        if(player==null)return;
        player.setOnCompletionListener(MediaPlayer::release);
        player.setOnErrorListener((media,what,extra)->{media.release();return true;});
        try{player.start();}catch(RuntimeException error){player.release();}
    }
}
