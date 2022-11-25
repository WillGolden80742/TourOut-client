package com.example.tourOut.Model;

import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cache extends ConnectionFactory {

    public static final String localDeArmazenamento = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/TourOut/Cache/";
    private String fileName;
    private final Runnable downloadMidia = () -> {
        writeToFile(fileName, getContentBytes());
    };
    private File file;
    private String url;

    public Cache(String url) {
        super(url);
        this.url = url;

    }

    public Cache() {
        super(null);
    }

    public static String getHashMd5(String value) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        BigInteger hash = new BigInteger(1, md.digest(value.getBytes()));
        return hash.toString(16);
    }

    public void fileHashed(String fileName) {
        String[] fileArr = fileName.split("[.]");
        int lasPosition = fileArr.length - 1;
        this.fileName = getHashMd5(fileName) + "." + fileArr[lasPosition];
    }

    public void setCache(String fileName) {
        fileHashed(fileName);
        this.file = new File(localDeArmazenamento + this.fileName);
        if (!file.exists()) {
            new Thread(downloadMidia).start();
        }
    }

    public String getCache(String fileName) {
        fileHashed(fileName);
        this.file = new File(localDeArmazenamento + this.fileName);
        if (file.exists() && file.length() > 1024) {
            return localDeArmazenamento + this.fileName;
        } else {
            if (file.length() < 1024) {
                file.delete();
            }
            return "NOT_FOUND";
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
        } catch (IOException | NullPointerException e) {
        }
    }

}
