package com.example.playhistory.controller;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class AudioController {
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    private ConnectionFactory connection;
    private final String padraoNomeArquivo = "audio_descricao_";
    private final String localDeArmazenamento = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/TourOut/Audio_Descricao/";
    private String fileName;
    private File file;

    public AudioController(Context context, String url) {
        connection = new ConnectionFactory(url);
        fileName = padraoNomeArquivo+url.split("=")[1]+".mp3";
        file = new File(localDeArmazenamento+fileName);
        if (!file.exists() || file.length() <= 1024) {
            this.mediaPlayer = MediaPlayer.create(context, Uri.parse(url));
            new Thread(downloadMidia).start();
        } else {
            this.mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file));
        }
    }


    public AudioController() {

    }

    private Runnable downloadMidia = () -> {
        writeToFile(fileName,connection.getContentBytes());
    };

    public void writeToFile(String fileName, byte[] content){
        File path = new File(localDeArmazenamento);
        File newDir = new File(String.valueOf(path));
        try {
            if (!newDir.exists()) {
                newDir.mkdirs();
            }
            FileOutputStream writer = new FileOutputStream(new File(path, fileName));
            writer.write(content);
            writer.close();
        } catch (IOException e) {
        }
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
