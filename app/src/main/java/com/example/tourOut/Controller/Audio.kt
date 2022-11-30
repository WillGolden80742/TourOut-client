package com.example.tourOut.Controller

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import com.example.tourOut.Model.ConnectionFactory
import com.example.tourOut.Model.ConnectionFactory.Companion.isConnected
import java.io.*

class Audio {
    private val padraoNomeArquivo = "audio_descricao_"
    private val localDeArmazenamento =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            .toString() + "/TourOut/Audio_Descricao/"
    private val configFile = "config.txt"
    private val configFilePath = localDeArmazenamento + configFile
    private var connection: ConnectionFactory? = null
    private var fileName: String? = null
    private val downloadMidia = Runnable { writeToFile(fileName, connection!!.contentBytes) }
    private var file: File? = null

    constructor(context: Context?, url: String) {
        connection = ConnectionFactory(url)
        val nome = url.split("=").toTypedArray()
        fileName = padraoNomeArquivo + nome[nome.size - 1] + ".mp3"
        file = File(localDeArmazenamento + fileName)
        val hashNoFile = !file!!.exists() || file!!.length() < 1024
        if (hashNoFile && isConnected(context!!)) {
            if (file!!.length() < 1024) {
                file!!.delete()
            }
            downloading[fileName] = true
            mediaPlayer = MediaPlayer.create(context, Uri.parse(url))
            Thread(downloadMidia).start()
        } else if (!hashNoFile) {
            downloading[fileName] = false
            mediaPlayer = MediaPlayer.create(context, Uri.fromFile(file))
        }
    }

    constructor(context: Context?, id: Int) {
        mediaPlayer = MediaPlayer.create(context, id)
    }

    constructor()

    val isDownloading: Boolean
        get() {
            for ((_, value) in downloading) {
                if (value) {
                    return true
                }
            }
            return false
        }

    fun setDownloaded() {
        writeToFile(configFile, "downloaded:true")
    }

    val isDownloaded: Boolean
        get() {
            var line = ""
            var downloaded = false
            val file = File(configFilePath)
            if (file.exists()) {
                return try {
                    val br = BufferedReader(FileReader(file))
                    while (br.readLine().also { line = it } != null) {
                        val hashDownloaded = line.contains("downloaded") && line.split(":")
                            .toTypedArray()[1] == "true"
                        if (hashDownloaded) {
                            downloaded = true
                        }
                    }
                    br.close()
                    downloaded
                } catch (e: FileNotFoundException) {
                    println("FileNotFoundException : $e")
                    false
                } catch (e: IOException) {
                    println("IOException ex : $e")
                    false
                }
            } else {
                downloaded = false
            }
            return downloaded
        }

    fun writeToFile(fileName: String?, content: String) {
        val path = File(localDeArmazenamento)
        val newDir = File(path.toString())
        try {
            if (!newDir.exists()) {
                newDir.mkdirs()
            }
            val writer = FileOutputStream(File(path, fileName))
            writer.write(content.toByteArray())
            writer.close()
            downloading[fileName] = false
        } catch (e: IOException) {
        }
    }

    fun writeToFile(fileName: String?, content: ByteArray?) {
        val path = File(localDeArmazenamento)
        val newDir = File(path.toString())
        try {
            if (!newDir.exists()) {
                newDir.mkdirs()
            }
            val writer = FileOutputStream(File(path, fileName))
            writer.write(content)
            writer.close()
            downloading[fileName] = false
        } catch (e: IOException) {
        }
    }

    fun play() {
        mediaPlayer.start()
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
        }
    }

    val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    fun stop() {
        mediaPlayer.stop()
    }

    fun reset() {
        mediaPlayer.reset()
    }

    val duration: Int
        get() = mediaPlayer.duration
    val position: Int
        get() = mediaPlayer.currentPosition
    var percent: Float
        get() = position.toFloat() / duration * 100
        set(percent) {
            var percent = percent
            if (percent > 100) {
                percent = percent % 100
            } else if (percent < 0) {
                percent = 0f
            }
            mediaPlayer.seekTo((duration / 100 * percent).toInt())
        }

    companion object {
        private val downloading = HashMap<String?, Boolean>()
        private var mediaPlayer = MediaPlayer()
    }
}