package com.example.playhistory.controller;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class ConnectionFactory {
    private String url;

    public ConnectionFactory(String url) {
        this.url = url;
    }

    public ConnectionFactory() {
    }

    public String getHTML(String urlToRead) {
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

    public String getHTML () {
        return getHTML (this.url);
    }

    public JSONObject jsonSearch (String jsonQuery) {
        return jsonSearch (this.url,jsonQuery);
    }

    public JSONObject jsonSearch (String urlToRead, String jsonQuery) {
        String json = "";
        try {
            json = getHTML(urlToRead);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] query = jsonQuery.split(":");
        JSONObject jsonResult = null;
        try {
            jsonResult = new JSONObject(json);
            int contQuery=0;
            try {
                while (contQuery<query.length) {
                    jsonResult = new JSONObject(jsonResult.toString()).getJSONObject(query[contQuery]);
                    contQuery++;
                }
            } catch (org.json.JSONException ex) {
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


}



