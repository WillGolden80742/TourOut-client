package com.example.playhistory.Model;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;

public class ConnectionFactory {
    private final String url;

    public ConnectionFactory(String url) {
        this.url = url;
    }

    public static byte[] inputStreamToByte(InputStream is) {
        try {
            ByteArrayOutputStream bytestream = new ByteArrayOutputStream();
            int ch;
            while ((ch = is.read()) != -1) {
                bytestream.write(ch);
            }
            byte[] imgdata = bytestream.toByteArray();
            bytestream.close();
            return imgdata;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getContent(String urlToRead) {
        StringBuilder result = new StringBuilder();
        try {
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()))) {
                for (String line; (line = reader.readLine()) != null; ) {
                    result.append(line);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public byte[] getContentBytes() {
        return getContentBytes(this.url);
    }

    public byte[] getContentBytes(String urlToRead) {
        URL contentUrl = null;//w w  w  .  jav  a  2s  .c om
        InputStream inStream = null;
        try {
            contentUrl = new URL(urlToRead);
            URLConnection connection = contentUrl.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) connection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inStream = httpConnection.getInputStream();
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] data = inputStreamToByte(inStream);
        return data;
    }

    public String getContent() {
        return getContent(this.url);
    }


    public JSONObject jsonSearch(String jsonQuery) {
        String content = getContent();
        return jsonSearch(content, jsonQuery);
    }

    public JSONObject jsonSearch(String content, String jsonQuery) {
        String json = content;
        String[] query = jsonQuery.split(":");
        JSONObject jsonResult = null;
        try {
            jsonResult = new JSONObject(json);
            int contQuery = 0;
            try {
                while (contQuery < query.length) {
                    jsonResult = new JSONObject(jsonResult.toString()).getJSONObject(query[contQuery]);
                    contQuery++;
                }
            } catch (JSONException ex) {
                if (ex.toString().contains("not found")) {
                    jsonResult = null;
                } else {
                    jsonResult = (JSONObject) new JSONObject(jsonResult.toString()).get(query[contQuery]);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonResult;
    }


    public JSONObject jsonSearchByCache(String fileName, String jsonQuery) {
        File file = new File(new Cache().getCache(fileName));
        if (file.exists()) {
            return jsonSearchByCache(file, jsonQuery);
        } else {
            return jsonSearch(jsonQuery);
        }
    }

    public JSONObject jsonSearchByCache(File file, String jsonQuery) {
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                text.append(line);
            }
            br.close();
            return jsonSearch(String.valueOf(text), jsonQuery);
        } catch (IOException e) {
            return jsonSearch(jsonQuery);
        }
    }


}



