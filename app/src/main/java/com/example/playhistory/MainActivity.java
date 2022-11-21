package com.example.playhistory;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.playhistory.Controller.Audio;
import com.example.playhistory.Controller.Monumento;
import com.example.playhistory.Controller.Tempo;
import com.example.playhistory.Model.Cache;
import com.example.playhistory.Model.ConnectionFactory;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements LocationListener {

    static boolean initCalc = false;
    // SERVIDOR
    private static final String host = "https://desmatamenos.website/";
    private static SeekBar seekMusic;
    private static Audio audio;
    private static Thread progress;
    private static boolean isPlaying = false;
    // LISTA DE MONUMENTOS
    private static List<Monumento> monumentosObjectList = new ArrayList<>();
    // LOCALIZAÇÃO
    private static TextView coordenada;
    // PLAYER DE AUDIO
    FloatingActionButton buttonAudio;
    SearchView urlInput;
    String currentUrl = "";
    private Cache cache;
    private final Map<Integer, Boolean> visitado = new HashMap<>();
    private ListView monumentosLista;
    private Monumento currentMonumento;
    private LocationManager locationManager;
    // CALCULO DO MONUMENTO MAIS PROXIMO
    private Monumento monumentoMaisProximo;
    private int monumentoMaisProximoIndex = 0;
    private double menorDistancia, currentLat, currentLong;
    private EditText distaciaMinima;
    private final Tempo tempo = new Tempo();
    @SuppressLint("NewApi")
    private final Runnable updateProgress = () -> {
        do {
            try {
                float percent = audio.getPercent();
                seekMusic.setProgress((int) percent, true);
                Thread.sleep(tempo.segundo * 1);
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
                    urlInput.setQuery("Erro", false);
                }
            }
        }
    };
    private final Runnable monumentoMenorDistancia = () -> {
        while (true) {
            if (monumentosObjectList.size() != 1) {
                menorDistancia = 1000000;
                int index = 0;
                int quantidadeVisitados = 0;
                for (Monumento m : monumentosObjectList) {
                    if (!visitado.get(m.getIdMonumento())) {
                        calculaDistancia(index, m, currentLat, currentLong);
                        quantidadeVisitados++;
                    }
                    index++;
                }
                if (quantidadeVisitados != 0) {
                    coordenada.setText(monumentoMaisProximo.getNome() + "\n à " + (int) (menorDistancia * 1000) + " metros");
                    int distaciaMinimaInt;
                    try {
                        distaciaMinimaInt = Integer.parseInt(String.valueOf(distaciaMinima.getText()));
                    } catch (Exception ex) {
                        distaciaMinimaInt = 0;
                    }
                    if (((int) (menorDistancia * 1000)) <= distaciaMinimaInt && !visitado.get(monumentoMaisProximo.getIdMonumento()) && audio.isPlaying()) {
                        visitado.put(monumentoMaisProximo.getIdMonumento(), true);
                        monumentosObjectList.set(monumentoMaisProximoIndex, monumentoMaisProximo);
                        reproduzirAudioDescricao(String.valueOf(monumentoMaisProximo.getIdMonumento()));
                        setCurrentMonumento(monumentoMaisProximo);
                        setMessage(currentMonumento);
                    }
                } else {
                    coordenada.setText("Todo monumentos foram visitado!");
                }
            } else {
                coordenada.setText("Sem dados suficientes para cálculo");
            }
            try {
                new Thread().sleep(tempo.segundo * 10);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    };
    private String monumentos;
    private final Runnable baixarAudioDescricaoDeMonumentos = () -> {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isNotDownloaded = !(new Audio().isDownloaded());
        if (mWifi.isConnected() && isNotDownloaded) {
            MediaPlayer baixando = MediaPlayer.create(this, R.raw.baixando_dados);
            baixando.start();
            for (Monumento m : monumentosObjectList) {
                String url = host + "audioDescricao.php?idDocumento=" + m.getIdMonumento();
                audio = new Audio(this, url);
            }
            while (audio.isDownloading() || baixando.isPlaying()) {
                try {
                    Thread.sleep(tempo.segundo / 4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            MediaPlayer baixado = MediaPlayer.create(this, R.raw.dados_baixados);
            baixado.start();
            audio.setDownloaded();
        }
    };
    private final Runnable reiniciarVisitados = () -> {
        while (true) {
            setVisitados(false);
            try {
                new Thread().sleep(tempo.hora * 2);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    public void init() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        inserirMonumentos();
        new Thread(reiniciarVisitados).start();
        getPermissions();
        buttonAudio = findViewById(R.id.playAudio);
        seekMusic = findViewById(R.id.seekAudio);
        urlInput = findViewById(R.id.urlInput);
        coordenada = findViewById(R.id.coordenada);
        distaciaMinima = findViewById(R.id.metroNumber);
        setListener();
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
                    urlInput.setQuery("", false);
                    urlInput.clearFocus();
                    inserirMonumentos();
                    setVisitados(false);
                } else {
                    setCurrentMonumento(monumentosObjectList.get(itemPosition));
                    reproduzirAudioDescricao(String.valueOf(idDMonumento));
                    setMessage(currentMonumento);
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

    private void setMessage(Monumento m) {
        Intent intent = new Intent(this, Messages.class);
        startActivity(intent);
        Messages.setDescricao(m.getNome(), m.getDescricao());
    }

    public void reproduzirAudioDescricao(String idDocumento) {
        if (!idDocumento.equals("0")) {
            try {
                audio.reset();
            } catch (Exception ex) {
            }
            resetPlayer();
            currentUrl = host + "audioDescricao.php?idDocumento=" + idDocumento;
            audio = new Audio(this, this.currentUrl);
            new Thread(playAudio).start();
        } else {
            coordenada.setText("Nenhum arquivo de voz encontrado");
        }
    }

    public void resetPlayer() {
        isPlaying = false;
        buttonAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    private String[] listaDeMonumentos(String query) {

        String url = host + "monumentos.php?nome=" + query;
        String fileName = "listaDeMonumentos.json";
        ConnectionFactory connection = new ConnectionFactory(url);

        JSONObject jsonArr;
        if (query.equals("")) {
            cache = new Cache(url);
            if (cache.getCache(fileName) == "NOT_FOUND") {
                cache.setCache(fileName);
            }
            jsonArr = connection.jsonSearchByCache(fileName, "Monumentos");
        } else {
            jsonArr = connection.jsonSearch("Monumentos");
        }

        monumentosObjectList = new ArrayList<>();
        monumentos = "";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                AtomicInteger cont = new AtomicInteger();
                jsonArr.keys().forEachRemaining(k -> {
                    monumentos += k + ",";
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
                    monumentos = "Nenhum Monumento encontrado";
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

    private void setNullResult() {
        Monumento m = new Monumento();
        m.setNome("");
        m.setIdMonumento(0);
        monumentosObjectList.add(m);
    }

    public void inserirMonumentos() {
        inserirMonumentos(listaDeMonumentos(""));
    }

    public void inserirMonumentos(String[] monumentos) {
        monumentosLista = (ListView) findViewById(R.id.monumentosLista);
        String[] dados = monumentos;
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dados);
        monumentosLista.setAdapter(adapter);
    }

    @SuppressLint("NewApi")
    public void getPermissions() {
        boolean noLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean noStorage = !(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (noLocation) {
            audio = new Audio(this, R.raw.permissoes);
            audio.play();
            ActivityResultLauncher<String[]> permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION, false);
                        Boolean writeStorageGranted = result.getOrDefault(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                        Boolean readStorageGranted = result.getOrDefault(
                                Manifest.permission.READ_EXTERNAL_STORAGE, false);
                        Boolean location = fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted;
                        Boolean storage = writeStorageGranted != null && readStorageGranted;
                        if (location && storage) {
                            setLocationManager();
                            audio.stop();
                            new Thread(baixarAudioDescricaoDeMonumentos).start();
                        } else if (location) {
                            coordenada.setText("Autorize a Geocalização");
                        }
                    }
            );

            permissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });

        } else {
            setLocationManager();
        }
        if (!noStorage) {
            new Thread(baixarAudioDescricaoDeMonumentos).start();
        }
    }

    @SuppressLint("MissingPermission")
    public void setLocationManager() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    private void calculaDistancia(int index, Monumento m, double lat2, double lng2) {
        double earthRadius = 6372.795477598;//kilometers
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
            monumentoMaisProximo = m;
            monumentoMaisProximoIndex = index;
        }
    }

    private void setVisitados(boolean b) {
        int i = 0;
        for (Monumento m : monumentosObjectList) {
            visitado.put(m.getIdMonumento(), b);
            monumentosObjectList.set(i, m);
            i++;
        }
    }

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