package com.example.tourOut.Model

import android.content.Context
import android.net.ConnectivityManager
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.ProtocolException
import java.net.URL

open class ConnectionFactory(private val url: String?) {
    fun getContent(urlToRead: String?): String {
        val result = StringBuilder()
        try {
            val url = URL(urlToRead)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            BufferedReader(
                InputStreamReader(conn.inputStream)
            ).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    result.append(line)
                }
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: ProtocolException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return result.toString()
    }

    val contentBytes: ByteArray?
        get() = getContentBytes(url)

    fun getContentBytes(urlToRead: String?): ByteArray? {
        var contentUrl: URL? = null //w w  w  .  jav  a  2s  .c om
        var inStream: InputStream? = null
        try {
            contentUrl = URL(urlToRead)
            val connection = contentUrl.openConnection()
            val httpConnection =
                connection as HttpURLConnection
            val responseCode = httpConnection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                inStream = httpConnection.inputStream
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return inputStreamToByte(inStream)
    }

    val content: String
        get() = getContent(url)

    fun jsonSearch(jsonQuery: String): JSONObject? {
        val content = content
        return jsonSearch(content, jsonQuery)
    }

    fun jsonSearch(content: String, jsonQuery: String): JSONObject? {
        val json = content
        val query = jsonQuery.split(":").toTypedArray()
        var jsonResult: JSONObject? = null
        try {
            jsonResult = JSONObject(json)
            var contQuery = 0
            try {
                while (contQuery < query.size) {
                    jsonResult = JSONObject(jsonResult.toString()).getJSONObject(query[contQuery])
                    contQuery++
                }
            } catch (ex: JSONException) {
                jsonResult = if (ex.toString().contains("not found")) {
                    null
                } else {
                    JSONObject(jsonResult.toString())[query[contQuery]] as JSONObject
                }
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonResult
    }

    fun jsonSearchByCache(fileName: String, jsonQuery: String): JSONObject? {
        val file = File(Cache().getCache(fileName))
        return if (file.exists()) {
            jsonSearchByCache(file, jsonQuery)
        } else {
            jsonSearch(jsonQuery)
        }
    }

    fun jsonSearchByCache(file: File?, jsonQuery: String): JSONObject? {
        val text = StringBuilder()
        return try {
            val br = BufferedReader(FileReader(file))
            var line: String? = ""
            while (br.readLine().also { line = it } != null) {
                text.append(line)
            }
            br.close()
            jsonSearch(text.toString(), jsonQuery)
        } catch (e: IOException) {
            // return exception
            e.printStackTrace()
            jsonSearch(jsonQuery)
        }
    }

    companion object {
        fun inputStreamToByte(`is`: InputStream?): ByteArray? {
            try {
                val bytestream = ByteArrayOutputStream()
                var ch: Int
                while (`is`!!.read().also { ch = it } != -1) {
                    bytestream.write(ch)
                }
                val imgdata = bytestream.toByteArray()
                bytestream.close()
                return imgdata
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        @JvmStatic
        fun isConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }
    }
}