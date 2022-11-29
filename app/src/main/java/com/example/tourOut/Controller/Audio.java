package com.example.tourOut.Controller;
import static com.example.tourOut.Model.ConnectionFactory.isConnected;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;

import com.example.tourOut.Model.ConnectionFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class Audio {
    private static final HashMap<String, Boolean> downloading = new HashMap<>();
    private static MediaPlayer mediaPlayer = new MediaPlayer();
    private final String padraoNomeArquivo = "audio_descricao_";
    private final String localDeArmazenamento = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TourOut/Audio_Descricao/";
    private final String configFile = "config.txt";
    private final String configFilePath = localDeArmazenamento + configFile;
    private ConnectionFactory connection;
    private String fileName;
    private final Runnable downloadMidia = () -> {
        writeToFile(fileName, connection.getContentBytes());
    };
    private File file;

    public Audio(Context context, String url) {
        connection = new ConnectionFactory(url);
        String[] nome = url.split("=");
        fileName = padraoNomeArquivo + nome[nome.length - 1] + ".mp3";
        file = new File(localDeArmazenamento + fileName);
        boolean hashNoFile = !file.exists() || file.length() < 1024;
        if (hashNoFile && isConnected(context)) {
            if (file.length() < 1024) {
                file.delete();
            }
            downloading.put(fileName, true);
            mediaPlayer = MediaPlayer.create(context, Uri.parse(url));
            new Thread(downloadMidia).start();
        } else if (!hashNoFile) {
            downloading.put(fileName, false);
            mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file));
        }
    }


    public Audio(Context context, int id) {
        mediaPlayer = MediaPlayer.create(context, id);
    }

    public Audio() {
    }

    public boolean isDownloading() {
        for (Map.Entry<String, Boolean> pair : downloading.entrySet()) {
            if (pair.getValue()) {
                return true;
            }
        }
        return false;
    }

    public void setDownloaded() {
        writeToFile(configFile, "downloaded:true");
    }

    public boolean isDownloaded() {
        String line = "";
        boolean downloaded = false;
        File file = new File(configFilePath);
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                while ((line = br.readLine()) != null) {
                    boolean hashDownloaded = line.contains("downloaded") && line.split(":")[1].equals("true");
                    if (hashDownloaded) {
                        downloaded = true;
                    }
                }
                br.close();
                return downloaded;
            } catch (FileNotFoundException e) {
                System.out.println("FileNotFoundException : "+e);
                return false;
            } catch (IOException e) {
                System.out.println("IOException ex : "+e);
                return false;
            }
        } else {
            downloaded = false;
        }
        return downloaded;
    }

    public void writeToFile(String fileName, String content) {
        File path = new File(localDeArmazenamento);
        File newDir = new File(String.valueOf(path));
        try {
            if (!newDir.exists()) {
                newDir.mkdirs();
            }
            FileOutputStream writer = new FileOutputStream(new File(path, fileName));
            writer.write(content.getBytes());
            writer.close();
            downloading.put(fileName, false);
        } catch (IOException e) {
        }
    }

    public void writeToFile(String fileName, byte[] content) {
        File path = new File(localDeArmazenamento);
        File newDir = new File(String.valueOf(path));
        try {
            if (!newDir.exists()) {
                newDir.mkdirs();
            }
            FileOutputStream writer = new FileOutputStream(new File(path, fileName));
            writer.write(content);
            writer.close();
            downloading.put(fileName, false);
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


    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    public void stop() {
        mediaPlayer.stop();
    }

    public void reset() {
        mediaPlayer.reset();
    }

    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    public int getPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    public float getPercent() {
        return ((float) getPosition() / getDuration()) * 100;
    }

    public void setPercent(float percent) {
        if (percent > 100) {
            percent = percent % 100;
        } else if (percent < 0) {
            percent = 0;
        }
        mediaPlayer.seekTo((int) ((getDuration() / 100) * (percent)));
    }
}
