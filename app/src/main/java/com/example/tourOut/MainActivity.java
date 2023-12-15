package com.example.tourOut;
import static com.example.tourOut.R.id;
import static com.example.tourOut.R.layout;
import static com.example.tourOut.R.raw;
import static com.example.tourOut.R.string;
import static java.lang.Thread.sleep;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
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
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import com.example.tourOut.Controller.Audio;
import com.example.tourOut.Controller.Coordenadas;
import com.example.tourOut.Controller.Monumento;
import com.example.tourOut.Controller.Tempo;
import com.example.tourOut.Model.Cache;
import com.example.tourOut.Model.ConnectionFactory;
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
public class MainActivity extends AppCompatActivity {

    //SERVIDOR
    private static final String host = "https://twoleafchat.site/TourOut/";
    // AUDIO START
    private static Audio audio = new Audio();
    private static boolean isPlaying = false;
    private static List<Monumento> monumentosObjectList = new ArrayList<>();
    private static Monumento currentMonumento;
    private static boolean midiaDownloaded = true;
    //AUDIO END
    //PERMISSIONS
    private boolean location;
    private boolean storage;
    //TEMPO
    private final Tempo tempo = new Tempo();
    private final Map<Integer, Boolean> visitado = new HashMap<>();
    private final Map<Integer, Boolean> anunciado = new HashMap<>();
    //MONUMENTOS END
    private String currentUrl = "";
    //MONUMENTOS START
    private String monumentos;
    //CALCULO PROXIMIDADE START
    private Monumento monumentoMaisProximo;
    //CALCULO PROXIMIDADE END
    private int monumentoMaisProximoIndex = 0;
    private double menorDistancia;
    private Thread calculoProximidade;
    private Coordenadas coordenadas = new Coordenadas();
    private Intent intentService;
    //INTERFACE START
    //  PESQUISA
    private SearchView urlInput;
    //  PLAYER DE AUDIO
    private FloatingActionButton buttonAudio;
    //  MESSAGEM COORDENADA
    private TextView message;
    //  DISTANCIA MINIMA
    private EditText distaciaMinima;
    private TextView distanciaMedida;
    //INTERFACE END
    //  SEEK AUDIO
    private SeekBar seekMusic;
    //  SEEK DISTANCIA
    private SeekBar seekdistancia;
    //  LISTA
    private ListView monumentosLista;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(layout.activity_main);
        init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        coordenadas.setStop(true);
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
        message = findViewById(id.message);
        distaciaMinima = findViewById(id.metros);
        distanciaMedida = findViewById(id.distanciaMedida);
        seekdistancia = findViewById(id.seekDistancia);
        setListener();
    }

    public void setListener() {
        buttonAudio.setOnClickListener(view -> new Thread(playAudio).start());
        monumentosLista.setOnItemClickListener((listView, itemView, itemPosition, itemId) -> {
            Monumento monumento =  monumentosObjectList.get(itemPosition);
            int idMonumento = monumento.getIdMonumento();
            if (idMonumento == 0) {
                urlInput.setQuery("", false);
                urlInput.clearFocus();
                inserirMonumentos();
                setVisitados(false);
            } else {
                setVisitado(monumento,true);
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
                if (isConnected()) {
                    urlInput.setQuery("", false);
                    urlInput.clearFocus();
                    urlInput.setIconified(true);
                }
            }

        });

        seekMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
                if (!currentUrl.equals("")) {
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
                String metros = "0";
                switch (progress) {
                    case 0:
                        distanciaMedida.setText("m");
                        metros = "15";
                        break;
                    case 1:
                        distanciaMedida.setText("m");
                        metros = "25";
                        break;
                    case 2:
                        distanciaMedida.setText("m");
                        metros = "50";
                        break;
                    case 3:
                        distanciaMedida.setText("m");
                        metros = "100";
                        break;
                    case 4:
                        distanciaMedida.setText("m");
                        metros = "250";
                        break;
                    case 5:
                        distanciaMedida.setText("m");
                        metros = "500";
                        break;
                    case 6:
                        distanciaMedida.setText("m");
                        metros = "750";
                        break;
                    case 7:
                        distanciaMedida.setText("km");
                        metros = "1";
                        break;
                    case 8:
                        distanciaMedida.setText("km");
                        metros = "2.5";
                        break;
                    case 9:
                        distanciaMedida.setText("km");
                        metros = "5";
                        break;
                    case 10:
                        distanciaMedida.setText("km");
                        metros = "7.5";
                        break;
                    case 11:
                        distanciaMedida.setText("km");
                        metros = "10";
                        break;
                    case 12:
                        distanciaMedida.setText("km");
                        metros = "25";
                        break;
                }
                distaciaMinima.setText(metros);
            }
        });
    }

    public void setCurrentMonumento(Monumento currentMonumento) {
        MainActivity.currentMonumento = currentMonumento;
    }

    private void setMessage(Monumento m) {
        if (!isAccessibilityEnabled()){
            Intent intent = new Intent(this, Descricao.class);
            startActivity(intent);
            Descricao.setDescricao(m.getNome(), m.getDescricao());
        }
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
            message.setText(string.nenhum_arquivo_encontrando);
        }
    }

    public void resetPlayer() {
        isPlaying = false;
        buttonAudio.setImageResource(android.R.drawable.ic_media_play);
    }

    private JSONObject cache (String url,String fileName) {
        ConnectionFactory connection = new ConnectionFactory(url);
        Cache cache = new Cache(url);
        if (cache.getCache(fileName).equals("NOT_FOUND")) {
            cache.setCache(fileName);
            return connection.jsonSearch("Monumentos");
        } else {

            return connection.jsonSearchByCache(fileName, "Monumentos");
        }
    }

    private String[] listaDeMonumentos(String query) {

        String url = host + "monumentos.php?nome=" + query;
        JSONObject jsonArr = cache(url,url + ".json");

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
                    String msg = "                       "+ getString(string.limpar_pesquisa);
                    m.setNome(msg);
                    monumentos += msg + ",";
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
        for (Monumento m : monumentosObjectList) {
            setVisitado(m, b);
        }
    }

    private void setVisitado(Monumento monumento, boolean b) {
        int idMonumento = monumento.getIdMonumento();
        visitado.put(idMonumento, b);
        anunciado.put(idMonumento, b);
        monumentosObjectList.set(monumentosObjectList.indexOf(monumento), monumento);
    }

    //DEVICE START
    private boolean isConnected (String op) {
        op = op.toLowerCase(Locale.ROOT).replace("-","");
        switch (op){
            case "wifi":
                ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                return connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected();
            default:
                ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    private boolean isConnected () {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void speech() {
        if (isConnected()) {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            if (intent.resolveActivity(getPackageManager()) != null) {
                speechActivityResultLauncher.launch(intent);
            } else {
                Toast.makeText(this, getString(string.seu_dispositivo_nao_suporta_entrada_de_fala), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private ActivityResultLauncher<Intent> speechActivityResultLauncher = registerForActivityResult(
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
                        for (String t : textos) {
                            textosConcatenados.append(t);
                        }
                        urlInput.setQuery(textosConcatenados.toString(), true);
                    }
                }
            });

    private void disableKeyboard() {
        if (isConnected()) {
            View view = this.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @SuppressLint("NewApi")
    public void getPermissions() {
        boolean noLocation = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
        boolean noStorage = !(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (noLocation) {
            new Thread(permissoesMsg).start();
            ActivityResultLauncher<String[]> permissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                        Boolean fineLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_FINE_LOCATION, false);
                        Boolean coarseLocationGranted = result.getOrDefault(
                                Manifest.permission.ACCESS_COARSE_LOCATION, false);
                        Boolean writeStorageGranted = result.getOrDefault(
                                Manifest.permission.WRITE_EXTERNAL_STORAGE, false);
                        Boolean readStorageGranted = result.getOrDefault(
                                Manifest.permission.READ_EXTERNAL_STORAGE, false);
                        location = fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted;
                        storage = writeStorageGranted != null && Boolean.TRUE.equals(readStorageGranted);
                        if (location && storage) {
                            setLocationManager();
                            audio.stop();
                            new Thread(baixarAudioDescricaoDeMonumentos).start();
                        } else if (location) {
                            message.setText(string.autorize_localizacao);
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
    private boolean isAccessibilityEnabled () {
        AccessibilityManager am = (AccessibilityManager) getSystemService(ACCESSIBILITY_SERVICE);
        boolean isAccessibilityEnabled = am.isEnabled();
        boolean isExploreByTouchEnabled = am.isTouchExplorationEnabled();
        return isAccessibilityEnabled && isExploreByTouchEnabled;
    }

    @SuppressLint("MissingPermission")
    public void setLocationManager() {
        //start overlay service if not started
        try {
            intentService = new Intent(this, Coordenadas.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intentService);
            } else {
                startService(intentService);
            }
            calculoProximidade = new Thread(monumentoMenorDistancia);
            calculoProximidade.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //DEVICE END

    //RUNNABLE START
    private final Runnable permissoesMsg = () -> {
        try {
            sleep(tempo.segundo*10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (!location) {
            audio = new Audio(this, raw.permissoes);
            audio.play();
        }
    };

    private final Runnable baixarAudioDescricaoDeMonumentos = () -> {
        boolean isNotDownloaded = !(new Audio().isDownloaded());
        if (isConnected("wi-fi") && isNotDownloaded) {
            midiaDownloaded = false;
            MediaPlayer baixando = MediaPlayer.create(this, raw.baixando_dados);
            baixando.start();
            for (Monumento m : monumentosObjectList) {
                int id = m.getIdMonumento();
                String url = host + "audioDescricao.php?idDocumento=" + id;
                audio = new Audio(this, url);
                url = host + "audioDescricaoNome.php?idDocumento=" + id +"&nome=nome_"+id;
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
            midiaDownloaded = true;
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
            Location l = coordenadas.getLocation();
            try {
                boolean coordenateFounded = l.getLatitude() != 0.0 && l.getLongitude() != 0.0;
                boolean dataEnough = monumentosObjectList.size() != 1;
                if (dataEnough && coordenateFounded) {
                    menorDistancia = 1000000;
                    int index = 0;
                    int quantidadeVisitados = 0;

                    for (Monumento m : monumentosObjectList) {
                        if (m.getIdMonumento() != 0) {
                            if (Boolean.FALSE.equals(visitado.get(m.getIdMonumento()))) {
                                calculaDistancia(index, m, l.getLatitude(), l.getLongitude());
                                quantidadeVisitados++;
                            }
                        }
                        index++;
                    }

                    if (quantidadeVisitados != 0) {
                        boolean isKm = (menorDistancia * 1000) > 1000;
                        String medida = (isKm) ? "quilômetros" : "metros";
                        String tourOutMsg = monumentoMaisProximo.getNome() + "\nà " + ((isKm) ? String.format("%.4f", menorDistancia) : (int) (menorDistancia * 1000)) + " " + medida;
                        message.setText(tourOutMsg);
                        coordenadas.setMessage(tourOutMsg);
                        float distaciaMinimaFloat;
                        try {
                            float distancia = Float.parseFloat(String.valueOf(distaciaMinima.getText()));
                            distaciaMinimaFloat = (distanciaMedida.getText().equals("km")) ? distancia * 1000 : distancia;
                        } catch (Exception ex) {
                            distaciaMinimaFloat = 0;
                        }
                        if (((int) (menorDistancia * 1000)) <= distaciaMinimaFloat && Boolean.FALSE.equals(visitado.get(monumentoMaisProximo.getIdMonumento())) && !audio.isPlaying()) {
                            visitado.put(monumentoMaisProximo.getIdMonumento(), true);
                            monumentosObjectList.set(monumentoMaisProximoIndex, monumentoMaisProximo);
                            reproduzirAudioDescricao(String.valueOf(monumentoMaisProximo.getIdMonumento()));
                            setCurrentMonumento(monumentoMaisProximo);
                            setMessage(currentMonumento);
                            message.setText(string.localizando);
                        } else if (Boolean.FALSE.equals(anunciado.get(monumentoMaisProximo.getIdMonumento())) && midiaDownloaded && !audio.isPlaying()) {
                            anunciado.put(monumentoMaisProximo.getIdMonumento(), true);
                            int id = monumentoMaisProximo.getIdMonumento();
                            audio = new Audio(this, raw.localidade_mais_proxima);
                            audio.play();
                            try {
                                sleep(audio.getDuration());
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            String url = host + "audioDescricaoNome.php?idDocumento=" + id + "&nome=nome_" + id;
                            audio = new Audio(this, url);
                            audio.play();
                        }
                    } else {
                        message.setText(string.todos_visitados);
                    }
                } else if (!coordenateFounded) {
                    message.setText(string.localizando);
                } else {
                    message.setText(string.sem_dados);
                }
            } catch (NullPointerException ex) {
                Log.d("Erro", ex.getMessage());
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