package com.example.tourOut

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.tourOut.Controller.Audio
import com.example.tourOut.Controller.Tempo
import com.example.tourOut.Model.ConnectionFactory.Companion.isConnected
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Descricao : AppCompatActivity() {
    var tempo = Tempo()
    var pausarDescricao: FloatingActionButton? = null
    private var descricaoTextView: TextView? = null
    private val runMidia = Runnable {
        descricaoTextView!!.text = descricao
        do {
            try {
                Thread.sleep((tempo.segundo * 1).toLong())
            } catch (e: InterruptedException) {
                System.exit(0)
            }
        } while (Audio().isPlaying)
        if (isConnected(this)) {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_descricao)
        descricaoTextView = findViewById(R.id.descricao)
        pausarDescricao = findViewById(R.id.pausarDescricao)
        Thread(runMidia).start()
        pausarDescricao!!.setOnClickListener {
            close()
        }
    }

    override fun onBackPressed() {
        close()
    }

    fun close() {
        finish()
        Audio().pause()
    }

    companion object {
        private var descricao: String? = null

        @JvmStatic
        fun setDescricao(nome: String, descricao: String) {
            Companion.descricao = """
                   ${nome.uppercase()}
                   
                   $descricao
                   """.trimIndent()
        }
    }
}