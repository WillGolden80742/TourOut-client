package com.example.playhistory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.playhistory.controller.AudioController;
import com.example.playhistory.controller.Cache;
import com.example.playhistory.controller.ConnectionFactory;
import com.example.playhistory.controller.Monumento;
import com.example.playhistory.controller.Tempo;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements LocationListener {

    // SERVIDOR
    private static String host = "https://desmatamenos.website/";
    private Cache cache;

    // PLAYER DE AUDIO
    FloatingActionButton buttonAudio;
    SearchView urlInput;
    String currentUrl = "";
    private static SeekBar seekMusic;
    private static AudioController audio;
    private static Thread progress;
    private static boolean isPlaying = false;
    private boolean baixadasAudioDescricoes = false;

    // LISTA DE MONUMENTOS
    private static List<Monumento> monumentosObjectList = new ArrayList<>();
    private ListView monumentosLista;
    private Monumento currentMonumento;
    // LOCALIZAÇÃO
    private static TextView coordenada;
    private LocationManager locationManager;


    // CALCULO DO MONUMENTO MAIS PROXIMO
    private Monumento menorDistanciaMonumento = new Monumento();
    private int menorDistanciaMonumentoIndex = 0;
    private double menorDistancia, currentLat,currentLong;
    private EditText distaciaMinima;
    private Tempo tempo = new Tempo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }



    public void init() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        getPermissions ();
        buttonAudio = findViewById(R.id.playAudio);
        seekMusic = findViewById(R.id.seekAudio);
        urlInput = findViewById(R.id.urlInput);
        coordenada = findViewById(R.id.coordenada);
        distaciaMinima = findViewById(R.id.metroNumber);
        inserirMonumentos();
        setListener();
        new Thread(reiniciarVisitados).start();
    }

    public void setListener() {
        buttonAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(playAudio).start();
            }
        });

        monumentosLista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> listView, View itemView, int itemPosition, long itemId) {
                int idDMonumento = monumentosObjectList.get(itemPosition).getIdMonumento();
                if (idDMonumento == 0) {
                    urlInput.setQuery("",false);
                    urlInput.clearFocus();
                    inserirMonumentos();
                } else {
                    setCurrentMonumento(monumentosObjectList.get(itemPosition));
                    setMidia(String.valueOf(idDMonumento));
                }
            }
        });

        urlInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                callSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (TextUtils.isEmpty(newText)) {
                    callSearch(newText);
                }
                return true;
            }

            public void callSearch(String query) {
                inserirMonumentos(listaDeMonumentos(query));
            }

        });
    }

    public void setCurrentMonumento(Monumento currentMonumento) {
        this.currentMonumento = currentMonumento;
    }

    public void setMidia (String idDocumento) {
        if (!idDocumento.equals("0")) {
            try {
                audio.reset();
            } catch (Exception ex ) {}
            resetPlayer();
            currentUrl=host+"audioDescricao.php?idDocumento="+idDocumento;
            audio = new AudioController(this,this.currentUrl);
            new Thread(playAudio).start();
        } else {
            coordenada.setText("Nenhum arquivo de voz encontrado");
        }
    }

    public void resetPlayer () {
        isPlaying = false;
        buttonAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    @SuppressLint("NewApi")
    private Runnable updateProgress = () -> {
            Intent intent = new Intent(this, Messages.class);
            startActivity(intent);
            Messages.setDescricao(currentMonumento.getNome(),currentMonumento.getDescricao());
            do {
                try {
                    float percent = audio.getPercent();
                    seekMusic.setProgress((int) percent,true);
                    Thread.sleep(tempo.segundo*1);
                } catch (InterruptedException e) {
                    System.exit(0);
                }
            } while (audio.isPlaying());
            if (audio.getPercent() >= 99) {
                seekMusic.setProgress(0);
            }
            resetPlayer();
    };


    private final Runnable playAudio = () -> {
        if (isPlaying) {
            audio.pause();
            isPlaying = false;
        } else {
            if (!currentUrl.equals("")) {
                try {
                    audio.play();
                    progress = new Thread(updateProgress);
                    progress.start();
                    buttonAudio.setImageResource(android.R.drawable.ic_media_pause);
                    isPlaying = true;
                } catch (Exception ex) {
                    urlInput.setQuery("Erro",false);
                }
            }
        }
    };


    private String monumentos;
    private String[] listaDeMonumentos (String query) {

        String url = host+"monumentos.php?nome="+query;
        String fileName = "listaDeMonumentos.json";
        ConnectionFactory connection = new ConnectionFactory(url);

        JSONObject jsonArr;
        if (query.equals("")) {
            cache = new Cache(url);
            if (cache.getCache(fileName) == "NOT_FOUND") {
                cache.setCache(fileName);
            }
            jsonArr = connection.jsonSearchByCache(fileName,"Monumentos");
        } else {
            jsonArr = connection.jsonSearch("Monumentos");
        }

        monumentosObjectList = new ArrayList<>();
        monumentos = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                AtomicInteger cont = new AtomicInteger();
                jsonArr.keys().forEachRemaining(k -> {
                    monumentos+=k+",";
                    try {
                        Monumento m = new Monumento();
                        m.setIdMonumento(jsonArr.getJSONObject(k).getInt("idMonumento"));
                        m.setNome(k);
                        m.setLatitude(jsonArr.getJSONObject(k).getDouble("latitude"));
                        m.setLongitude(jsonArr.getJSONObject(k).getDouble("longitude"));
                        m.setDescricao(jsonArr.getJSONObject(k).getString("descricao"));
                        monumentosObjectList.add(m);
                        cont.getAndIncrement();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });
                if (cont.get() == 0) {
                    setNullResult();
                    monumentos="Nenhum Monumento encontrado";
                } else {
                    if (!baixadasAudioDescricoes) {
                        new Thread(baixarAudioDescricaoDeMonumentos).start();
                    }
                }
            }
        } catch (NullPointerException ex) {
            setNullResult();
            monumentos = "Verifique sua conexão com a internet";
        } catch (Exception ex) {
            setNullResult();
            monumentos = String.valueOf(ex);
        }
        return monumentos.split(",");
    }

    private final Runnable baixarAudioDescricaoDeMonumentos = () -> {
        for (Monumento m : monumentosObjectList) {
            currentUrl = host + "audioDescricao.php?idDocumento=" + m.getIdMonumento();
            audio = new AudioController(this, this.currentUrl);
        }
        baixadasAudioDescricoes=true;
        String coordenadaText = String.valueOf(coordenada.getText());
        coordenada.setText("Audiodescrições baixadas");
        try {
            Thread.sleep(tempo.segundo*2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        coordenada.setText(coordenadaText);
    };

    private void setNullResult () {
        Monumento m = new Monumento();
        m.setNome("");
        m.setIdMonumento(0);
        monumentosObjectList.add(m);
    }


    public void inserirMonumentos () {
        inserirMonumentos (listaDeMonumentos(""));
    }

    public void inserirMonumentos (String[] monumentos) {
        monumentosLista = (ListView) findViewById(R.id.monumentosLista);
        String[] dados = monumentos;
        ArrayAdapter<String>  adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dados);
        monumentosLista.setAdapter(adapter);
    }

    @SuppressLint("NewApi")
    public void getPermissions () {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED || !(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)) {
            String url=host+"permissoes.php?nome=permissoes";
            audio = new AudioController(this,url);
            audio.play();
            ActivityResultLauncher<String[]> locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION,false);
                        if (fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted) {
                            setLocationManager();
                        } else {
                            coordenada.setText("Autorize a Geocalização");
                        }
                    }
            );

            locationPermissionRequest.launch(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });

        } else {
            setLocationManager();
        }
    }

    @SuppressLint("MissingPermission")
    public void setLocationManager () {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }


    private void calculaDistancia(int index,Monumento m, double lat2, double lng2) {
        double earthRadius = 6371;//kilometers
        double dLat = Math.toRadians(lat2 - m.getLatitude());
        double dLng = Math.toRadians(lng2 - m.getLongitude());
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2)
                * Math.cos(Math.toRadians(m.getLatitude()))
                * Math.cos(Math.toRadians(lat2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double dist = earthRadius * c;
        double distancia = dist;
        if (distancia < menorDistancia) {
            menorDistancia = distancia;
            menorDistanciaMonumento = m;
            menorDistanciaMonumentoIndex = index;
        }
    }


    private final Runnable monumentoMenorDistancia = () -> {
            while (true) {
                if (monumentosObjectList.size() != 1) {
                    menorDistancia = 1000000;
                    int index=0;
                    for (Monumento m : monumentosObjectList) {
                        calculaDistancia(index,m, currentLat, currentLong);
                        index++;
                    }
                    coordenada.setText(menorDistanciaMonumento.getNome() + "\n à " + (int)(menorDistancia * 1000) + " metros");
                    int distaciaMinimaInt;
                    try {
                        distaciaMinimaInt = Integer.parseInt(String.valueOf(distaciaMinima.getText()));
                    } catch (Exception ex ) {
                        distaciaMinimaInt = 0;
                    }
                    if (((int)(menorDistancia * 1000)) <= distaciaMinimaInt && !menorDistanciaMonumento.isVisitado()) {
                        menorDistanciaMonumento.setVisitado(true);
                        monumentosObjectList.set(menorDistanciaMonumentoIndex,menorDistanciaMonumento);
                        setMidia(String.valueOf(menorDistanciaMonumento.getIdMonumento()));
                        setCurrentMonumento(menorDistanciaMonumento);
                    }
                } else {
                    coordenada.setText("Sem dados suficientes para cálculo");
                }
                try {
                    new Thread().sleep(tempo.segundo*10);
                } catch (InterruptedException e) {
                    System.exit(0);
                }
            }
    };

    private final Runnable reiniciarVisitados = () -> {
        while (true) {
            int i = 0;
            for (Monumento m : monumentosObjectList) {
                m.setVisitado(false);
                monumentosObjectList.set(i, m);
                i++;
            }
            try {
                new Thread().sleep(tempo.hora*2);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    };

    static boolean initCalc = false;
    @Override
    public void onLocationChanged(@NonNull Location location) {
        currentLat = location.getLatitude();
        currentLong = location.getLongitude();
        if (!initCalc) {
            new Thread(monumentoMenorDistancia).start();
            initCalc = true;
        }
    }
}