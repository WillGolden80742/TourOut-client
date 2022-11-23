package com.example.playhistory;

import static com.example.playhistory.R.*;
import static java.lang.Thread.*;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
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
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends AppCompatActivity implements LocationListener {

    //SERVIDOR
    private static final String host = "https://desmatamenos.website/";

    //TEMPO
    private final Tempo tempo = new Tempo();

    //AUDIO START
    private static Audio audio = new Audio();
    private static boolean isPlaying = false;
    private String currentUrl = "";
    //AUDIO END

    //MONUMENTOS START
    private String monumentos;
    private static List<Monumento> monumentosObjectList = new ArrayList<>();
    private static Monumento currentMonumento;
    //MONUMENTOS END

    //CALCULO PROXIMIDADE START
    private Monumento monumentoMaisProximo;
    private int monumentoMaisProximoIndex = 0;
    private double menorDistancia, currentLat, currentLong;
    private static boolean initCalc = false;
    private final Map<Integer, Boolean> visitado = new HashMap<>();
    //CALCULO PROXIMIDADE END

    //INTERFACE START
    //  PESQUISA
    private SearchView urlInput;
    //  PLAYER DE AUDIO
    private FloatingActionButton buttonAudio;
    //  MESSAGEM COORDENADA
    private TextView coordenada;
    //  DISTANCIA MINIMA
    private EditText distaciaMinima;
    //  SEEK AUDIO
    private SeekBar seekMusic;
    //  SEEK DISTANCIA
    private SeekBar seekdistancia;
    //  LISTA
    private ListView monumentosLista;
    //INTERFACE END

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        init();
    }

    public void init() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        inserirMonumentos();
        new Thread(reiniciarVisitados).start();
        getPermissions();
        buttonAudio = findViewById(id.playAudio);
        seekMusic = findViewById(id.seekAudio);
        urlInput = findViewById(id.urlInput);
        coordenada = findViewById(id.coordenada);
        distaciaMinima = findViewById(id.metros);
        seekdistancia = findViewById(id.seekDistancia);
        setListener();
    }

    private void speech () {

        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        if (intent.resolveActivity(getPackageManager()) != null) {
            speechActivityResultLauncher.launch(intent);
        } else {
            Toast.makeText(this, getString(string.seu_dispositivo_nao_suporta_entrada_de_fala), Toast.LENGTH_SHORT).show();
        }
    }

    private void disableKeyboard () {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    ActivityResultLauncher<Intent> speechActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        ArrayList<String> textos = data != null ? data
                                .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) : null;
                        StringBuilder textosConcatenados = new StringBuilder();
                        assert textos != null;
                        for (String t:textos) {
                            textosConcatenados.append(t);
                        }
                        urlInput.setQuery(textosConcatenados.toString(),true);
                    }
                }
            });

    public void setListener() {
        buttonAudio.setOnClickListener(view -> new Thread(playAudio).start());

        monumentosLista.setOnItemClickListener((listView, itemView, itemPosition, itemId) -> {
            int idMonumento = monumentosObjectList.get(itemPosition).getIdMonumento();
            if (idMonumento == 0) {
                urlInput.setQuery("", false);
                urlInput.clearFocus();
                inserirMonumentos();
                setVisitados(false);
            } else {
                setCurrentMonumento(monumentosObjectList.get(itemPosition));
                reproduzirAudioDescricao(String.valueOf(idMonumento));
                setMessage(currentMonumento);
            }
        });

        urlInput.setOnSearchClickListener(view -> {
            disableKeyboard();
            speech();
        });


        urlInput.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                disableKeyboard();
                if (!TextUtils.isEmpty(newText)) {
                    callSearch(newText);
                }
                return true;
            }

            public void callSearch(String query) {
                inserirMonumentos(listaDeMonumentos(query));
                urlInput.setQuery("", false);
                urlInput.clearFocus();
                urlInput.setIconified(true);
            }

        });

        seekMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                this.progress=i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(!currentUrl.equals("")) {
                    audio.setPercent(progress);
                } else {
                    seekMusic.setProgress(0);
                }
            }
        });

        seekdistancia.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                this.progress = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int metros = 0;
                switch (progress) {
                    case 0:
                        metros=15;
                        break;
                    case 1:
                        metros=25;
                        break;
                    case 2:
                        metros=50;
                        break;
                    case 3:
                        metros=100;
                        break;
                    case 4:
                        metros=250;
                        break;
                    case 5:
                        metros=500;
                        break;
                    case 6:
                        metros=750;
                        break;
                    case 7:
                        metros=1000;
                        break;
                    case 8:
                        metros=2500;
                        break;
                    case 9:
                        metros=5000;
                        break;
                    case 10:
                        metros=7500;
                        break;
                    case 11:
                        metros=10000;
                        break;
                    case 12:
                        metros=25000;
                        break;
                }
                distaciaMinima.setText(String.valueOf(metros));
            }
        });

    }

    public void setCurrentMonumento(Monumento currentMonumento) {
        MainActivity.currentMonumento = currentMonumento;
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
            } catch (Exception ignored) {
            }
            resetPlayer();
            currentUrl = host + "audioDescricao.php?idDocumento=" + idDocumento;
            audio = new Audio(this, this.currentUrl);
            new Thread(playAudio).start();
        } else {
            coordenada.setText(string.nenhum_arquivo_encontrando);
        }
    }

    public void resetPlayer() {
        isPlaying = false;
        buttonAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    private String[] listaDeMonumentos(String query) {

        String url = host + "monumentos.php?nome=" + query;
        String fileName = url+".json";
        ConnectionFactory connection = new ConnectionFactory(url);

        JSONObject jsonArr;
        //CACHE
        Cache cache = new Cache(url);
        if (cache.getCache(fileName).equals("NOT_FOUND")) {
            cache.setCache(fileName);
            jsonArr = connection.jsonSearch("Monumentos");
        } else {
            jsonArr = connection.jsonSearchByCache(fileName, "Monumentos");
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
                    monumentos = getString(string.nenhum_monumento_encontrando);
                } else if (!query.equals("")) {
                    Monumento m = new Monumento();
                    m.setIdMonumento(0);
                    String msg = getString(string.espaco)+getString(string.limpar_pesquisa);
                    m.setNome(msg);
                    monumentos+=msg+",";
                    monumentosObjectList.add(m);
                }
            }
        } catch (NullPointerException ex) {
            setNullResult();
            monumentos = getString(string.verifique_sua_conexao);
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
        monumentosLista = findViewById(id.monumentosLista);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, monumentos);
        monumentosLista.setAdapter(adapter);
    }

    @SuppressLint("NewApi")
    public void getPermissions() {
        boolean noLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean noStorage = !(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (noLocation) {
            audio = new Audio(this, raw.permissoes);
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
                        Boolean storage = writeStorageGranted != null && Boolean.TRUE.equals(readStorageGranted);
                        if (location && storage) {
                            setLocationManager();
                            audio.stop();
                            new Thread(baixarAudioDescricaoDeMonumentos).start();
                        } else if (location) {
                            coordenada.setText(string.autorize_localizacao);
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
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
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
        double distancia = earthRadius * c;
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

    //RUNNABLE START
    private final Runnable baixarAudioDescricaoDeMonumentos = () -> {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        boolean isNotDownloaded = !(new Audio().isDownloaded());
        if (mWifi.isConnected() && isNotDownloaded) {
            MediaPlayer baixando = MediaPlayer.create(this, raw.baixando_dados);
            baixando.start();
            for (Monumento m : monumentosObjectList) {
                String url = host + "audioDescricao.php?idDocumento=" + m.getIdMonumento();
                audio = new Audio(this, url);
            }
            while (audio.isDownloading() || baixando.isPlaying()) {
                try {
                    sleep(tempo.segundo / 4);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            MediaPlayer baixado = MediaPlayer.create(this, raw.dados_baixados);
            baixado.start();
            audio.setDownloaded();
        }
    };
    private final Runnable reiniciarVisitados = () -> {
        while (true) {
            setVisitados(false);
            try {
                sleep(tempo.hora * 2);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    };
    @SuppressLint("NewApi")
    private final Runnable updateProgress = () -> {
        do {
            try {
                float percent = audio.getPercent();
                seekMusic.setProgress((int) percent, true);
                sleep(tempo.segundo);
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
                    Thread progress = new Thread(updateProgress);
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
                    if (m.getIdMonumento()!=0) {
                        if (Boolean.FALSE.equals(visitado.get(m.getIdMonumento()))) {
                            calculaDistancia(index, m, currentLat, currentLong);
                            quantidadeVisitados++;
                        }
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
                    if (((int) (menorDistancia * 1000)) <= distaciaMinimaInt && Boolean.FALSE.equals(visitado.get(monumentoMaisProximo.getIdMonumento())) && !audio.isPlaying()) {
                        visitado.put(monumentoMaisProximo.getIdMonumento(), true);
                        monumentosObjectList.set(monumentoMaisProximoIndex, monumentoMaisProximo);
                        reproduzirAudioDescricao(String.valueOf(monumentoMaisProximo.getIdMonumento()));
                        setCurrentMonumento(monumentoMaisProximo);
                        setMessage(currentMonumento);
                        coordenada.setText(string.localizando);
                    }
                } else {
                    coordenada.setText(string.todos_visitados);
                }
            } else {
                coordenada.setText(string.sem_dados);
            }
            try {
                sleep(tempo.segundo * 10);
            } catch (InterruptedException e) {
                System.exit(0);
            }
        }
    };
    //RUNNABLE END
}