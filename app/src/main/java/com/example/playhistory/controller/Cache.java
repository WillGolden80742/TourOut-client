package com.example.playhistory.controller;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Cache {
    
    private ConnectionFactory connection;
    public static final String localDeArmazenamento = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)+"/TourOut/Cache5/";
    private String fileName;
    private File path;
    private String url;

    public Cache (String url) {
        this.url = url;
    }

    public Cache () {

    }

    public void fileHashed(String fileName) {
        String[] fileArr = fileName.split("[.]");
        int lasPosition = fileArr.length-1;
        this.fileName = getHashMd5(fileName)+"."+fileArr[lasPosition];
    }

    public void setCache (String fileName) {
        connection = new ConnectionFactory(url);
        fileHashed(fileName);
        this.path = new File(localDeArmazenamento+this.fileName);
        if (!path.exists()) {
            new Thread(downloadMidia).start();
        }
    }



    public String getCache (String fileName) {
        fileHashed(fileName);
        this.path = new File(localDeArmazenamento+this.fileName);
        if (path.exists()) {
            return localDeArmazenamento+this.fileName;
        } else {
            return "NOT_FOUND";
        }
    }


    private Runnable downloadMidia = () -> {
        writeToFile(fileName,connection.getHTMLBytes());
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

}
