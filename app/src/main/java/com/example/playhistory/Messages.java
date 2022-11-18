package com.example.playhistory;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.playhistory.controller.AudioController;
import com.example.playhistory.controller.Tempo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class Messages extends AppCompatActivity {

    public Tempo tempo = new Tempo();
    private TextView descricaoTextView;
    private static String descricao;
    FloatingActionButton pausarDescricao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);
        descricaoTextView = findViewById(R.id.descricao);
        pausarDescricao = findViewById(R.id.pausarDescricao);
        new Thread(runMidia).start();
        pausarDescricao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                new AudioController().pause();
            }
        });
    }

    public static void setDescricao(String descricao) {
        Messages.descricao = descricao;
    }

    private Runnable runMidia = () -> {
        descricaoTextView.setText(descricao);
        do {
            try {
                Thread.sleep(tempo.segundo*1);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        } while (new AudioController().isPlaying());
        finish();
    };
}