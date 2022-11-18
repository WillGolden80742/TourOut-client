package com.example.playhistory.controller;
import android.media.MediaPlayer;
import android.net.Uri;
import android.content.Context;


public class AudioController {
    private static MediaPlayer mediaPlayer = new MediaPlayer();

    public AudioController(Context context,String url) {
        this.mediaPlayer = MediaPlayer.create(context, Uri.parse(url));
    }

    public AudioController() {

    }

    public void play() {
        mediaPlayer.start();
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }


    public boolean isPlaying () {
        return  mediaPlayer.isPlaying();
    }

    public void stop () {
        mediaPlayer.stop();
    }

    public void reset () {
        mediaPlayer.reset();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public float getPercent () {
        return ((float) getPosition()/getDuration())*100;
    }
}
