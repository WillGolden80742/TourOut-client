package com.example.tourOut.Model

import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

class Cache : ConnectionFactory {
    private var fileName: String? = null
    private val downloadMidia = Runnable { writeToFile(fileName, contentBytes) }
    private var file: File? = null
    private var url: String? = null

    constructor(url: String?) : super(url) {
        this.url = url
    }

    constructor() : super(null)

    private fun fileHashed(fileName: String) {
        val fileArr = fileName.split("[.]").toTypedArray()
        val lasPosition = fileArr.size - 1
        this.fileName = getHashMd5(fileName) + "." + fileArr[lasPosition]
    }

    fun setCache(fileName: String) {
        fileHashed(fileName)
        file = File(localDeArmazenamento + this.fileName)
        if (!file!!.exists()) {
            Thread(downloadMidia).start()
        }
    }

    fun getCache(fileName: String): String {
        fileHashed(fileName)
        file = File(localDeArmazenamento + this.fileName)
        return if (file!!.exists() && file!!.length() > 1024) {
            localDeArmazenamento + this.fileName
        } else {
            if (file!!.length() < 1024) {
                file!!.delete()
            }
            "NOT_FOUND"
        }
    }

    private fun writeToFile(fileName: String?, content: ByteArray?) {
        val path = File(localDeArmazenamento)
        val newDir = File(path.toString())
        try {
            if (!newDir.exists()) {
                newDir.mkdirs()
            }
            val writer = FileOutputStream(File(path, fileName))
            writer.write(content)
            writer.close()
        } catch (e: IOException) {
        } catch (e: NullPointerException) {
        }
    }

    companion object {
        val localDeArmazenamento =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                .toString() + "/TourOut/Cache/"

        private fun getHashMd5(value: String): String {
            val md: MessageDigest
            md = try {
                MessageDigest.getInstance("MD5")
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException(e)
            }
            val hash = BigInteger(1, md.digest(value.toByteArray()))
            return hash.toString(16)
        }
    }
}