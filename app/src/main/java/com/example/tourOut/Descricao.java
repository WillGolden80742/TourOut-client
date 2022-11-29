package com.example.tourOut;
import static com.example.tourOut.Model.ConnectionFactory.isConnected;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.tourOut.Controller.Audio;
import com.example.tourOut.Controller.Tempo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Locale;

public class Descricao extends AppCompatActivity {

    private static String descricao;
    public Tempo tempo = new Tempo();
    FloatingActionButton pausarDescricao;
    private TextView descricaoTextView;
    private final Runnable runMidia = () -> {
        descricaoTextView.setText(descricao);
        do {
            try {
                Thread.sleep(tempo.segundo * 1);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        } while (new Audio().isPlaying());
        if (isConnected(this)) {
            finish();
        }
    };

    public static void setDescricao(String nome, String descricao) {
        Descricao.descricao = nome.toUpperCase(Locale.ROOT) + "\n\n" + descricao;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_descricao);
        descricaoTextView = findViewById(R.id.descricao);
        pausarDescricao = findViewById(R.id.pausarDescricao);
        new Thread(runMidia).start();
        pausarDescricao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
                new Audio().pause();
            }
        });
    }

    @Override
    public void onBackPressed() {
        finish();
        new Audio().pause();
    }
}