package com.example.tourOut

import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.speech.RecognizerIntent
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android.widget.AdapterView.OnItemClickListener
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.tourOut.Controller.Audio
import com.example.tourOut.Controller.Coordenadas
import com.example.tourOut.Controller.Monumento
import com.example.tourOut.Controller.Tempo
import com.example.tourOut.Descricao.Companion.setDescricao
import com.example.tourOut.Model.Cache
import com.example.tourOut.Model.ConnectionFactory
import com.example.tourOut.R.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

@RequiresApi(api = Build.VERSION_CODES.N)
class MainActivity : AppCompatActivity() {
    //AUDIO END
    //PERMISSIONS
    private var location = false
    private var storage = false

    //TEMPO
    private val tempo = Tempo()
    private val visitado: MutableMap<Int, Boolean> = HashMap()
    private val anunciado: MutableMap<Int, Boolean> = HashMap()

    //MONUMENTOS END
    private var currentUrl = ""

    //MONUMENTOS START
    private var monumentos: String? = null

    //CALCULO PROXIMIDADE START
    private var monumentoMaisProximo: Monumento? = null

    //CALCULO PROXIMIDADE END
    private var monumentoMaisProximoIndex = 0
    private var menorDistancia = 0.0
    private var calculoProximidade: Thread? = null
    private val coordenadas = Coordenadas()
    private var intentService: Intent? = null

    //INTERFACE START
    //  PESQUISA
    private var urlInput: SearchView? = null

    //  PLAYER DE AUDIO
    private var buttonAudio: FloatingActionButton? = null

    //  MESSAGEM COORDENADA
    private var message: TextView? = null

    //  DISTANCIA MINIMA
    private var distaciaMinima: EditText? = null
    private var distanciaMedida: TextView? = null

    //INTERFACE END
    //  SEEK AUDIO
    private var seekMusic: SeekBar? = null

    //  SEEK DISTANCIA
    private var seekdistancia: SeekBar? = null

    //  LISTA
    private var monumentosLista: ListView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_main)
        init()
    }

    override fun onDestroy() {
        super.onDestroy()
        coordenadas.setStop(true)
    }

    fun init() {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        inserirMonumentos()
        Thread(reiniciarVisitados).start()
        permissions
        buttonAudio = findViewById(id.playAudio)
        seekMusic = findViewById(id.seekAudio)
        urlInput = findViewById(id.urlInput)
        message = findViewById(id.message)
        distaciaMinima = findViewById(id.metros)
        distanciaMedida = findViewById(id.distanciaMedida)
        seekdistancia = findViewById(id.seekDistancia)
        setListener()
    }

    fun setListener() {
        buttonAudio!!.setOnClickListener { view: View? -> Thread(playAudio).start() }
        monumentosLista!!.onItemClickListener =
            OnItemClickListener { listView: AdapterView<*>?, itemView: View?, itemPosition: Int, itemId: Long ->
                val idMonumento = monumentosObjectList[itemPosition]!!.idMonumento
                if (idMonumento == 0) {
                    urlInput!!.setQuery("", false)
                    urlInput!!.clearFocus()
                    inserirMonumentos()
                    setVisitados(false)
                } else {
                    setCurrentMonumento(monumentosObjectList[itemPosition])
                    reproduzirAudioDescricao(idMonumento.toString())
                    setMessage(currentMonumento)
                }
            }
        urlInput!!.setOnSearchClickListener { view: View? ->
            disableKeyboard()
            speech()
        }
        urlInput!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                disableKeyboard()
                if (!TextUtils.isEmpty(newText)) {
                    callSearch(newText)
                }
                return true
            }

            fun callSearch(query: String) {
                inserirMonumentos(listaDeMonumentos(query))
                if (isConnected) {
                    urlInput!!.setQuery("", false)
                    urlInput!!.clearFocus()
                    urlInput!!.isIconified = true
                }
            }
        })
        seekMusic!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var progress = 0
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                progress = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (currentUrl != "") {
                    audio.percent = progress.toFloat()
                } else {
                    seekMusic!!.progress = 0
                }
            }
        })
        seekdistancia!!.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            var progress = 0
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                progress = i
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                var metros = "0"
                when (progress) {
                    0 -> {
                        distanciaMedida!!.text = "m"
                        metros = "15"
                    }
                    1 -> {
                        distanciaMedida!!.text = "m"
                        metros = "25"
                    }
                    2 -> {
                        distanciaMedida!!.text = "m"
                        metros = "50"
                    }
                    3 -> {
                        distanciaMedida!!.text = "m"
                        metros = "100"
                    }
                    4 -> {
                        distanciaMedida!!.text = "m"
                        metros = "250"
                    }
                    5 -> {
                        distanciaMedida!!.text = "m"
                        metros = "500"
                    }
                    6 -> {
                        distanciaMedida!!.text = "m"
                        metros = "750"
                    }
                    7 -> {
                        distanciaMedida!!.text = "km"
                        metros = "1"
                    }
                    8 -> {
                        distanciaMedida!!.text = "km"
                        metros = "2.5"
                    }
                    9 -> {
                        distanciaMedida!!.text = "km"
                        metros = "5"
                    }
                    10 -> {
                        distanciaMedida!!.text = "km"
                        metros = "7.5"
                    }
                    11 -> {
                        distanciaMedida!!.text = "km"
                        metros = "10"
                    }
                    12 -> {
                        distanciaMedida!!.text = "km"
                        metros = "25"
                    }
                }
                distaciaMinima!!.setText(metros)
            }
        })
    }

    fun setCurrentMonumento(currentMonumento: Monumento?) {
        Companion.currentMonumento = currentMonumento
    }

    private fun setMessage(m: Monumento?) {
        if (!isAccessibilityEnabled) {
            val intent = Intent(this, Descricao::class.java)
            startActivity(intent)
            setDescricao(m!!.nome!!, m.descricao!!)
        }
    }

    fun reproduzirAudioDescricao(idDocumento: String) {
        if (idDocumento != "0") {
            try {
                audio.reset()
            } catch (ignored: Exception) {
            }
            resetPlayer()
            currentUrl = host + "audioDescricao.php?idDocumento=" + idDocumento
            audio = Audio(this, currentUrl)
            Thread(playAudio).start()
        } else {
            message!!.setText(string.nenhum_arquivo_encontrando)
        }
    }

    fun resetPlayer() {
        isPlaying = false
        buttonAudio!!.setImageResource(R.drawable.ic_media_play)
    }

    private fun cache(url: String, fileName: String): JSONObject? {
        val connection = ConnectionFactory(url)
        val cache = Cache(url)
        return if (cache.getCache(fileName) == "NOT_FOUND") {
            cache.setCache(fileName)
            connection.jsonSearch("Monumentos")
        } else {
            connection.jsonSearchByCache(fileName, "Monumentos")
        }
    }

    private fun listaDeMonumentos(query: String): Array<String?> {
        val url = host + "monumentos.php?nome=" + query
        val jsonArr = cache(url, "$url.json")
        monumentosObjectList = ArrayList()
        monumentos = ""
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val cont = AtomicInteger()
                jsonArr!!.keys().forEachRemaining { k: String ->
                    monumentos += "$k,"
                    try {
                        val m = Monumento()
                        m.idMonumento = jsonArr.getJSONObject(k).getInt("idMonumento")
                        m.nome = k
                        m.latitude = jsonArr.getJSONObject(k).getDouble("latitude")
                        m.longitude = jsonArr.getJSONObject(k).getDouble("longitude")
                        m.descricao = jsonArr.getJSONObject(k).getString("descricao")
                        monumentosObjectList.add(m)
                        cont.getAndIncrement()
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
                if (cont.get() == 0) {
                    setNullResult()
                    monumentos = getString(string.nenhum_monumento_encontrando)
                } else if (query != "") {
                    val m = Monumento()
                    m.idMonumento = 0
                    val msg = "                       " + getString(string.limpar_pesquisa)
                    m.nome = msg
                    monumentos += "$msg,"
                    monumentosObjectList.add(m)
                }
            }
        } catch (ex: NullPointerException) {
            setNullResult()
            monumentos = getString(string.verifique_sua_conexao)
        } catch (ex: Exception) {
            setNullResult()
            monumentos = ex.toString()
        }
        return monumentos!!.split(",").toTypedArray()
    }

    private fun setNullResult() {
        val m = Monumento()
        m.nome = ""
        m.idMonumento = 0
        monumentosObjectList.add(m)
    }

    @JvmOverloads
    fun inserirMonumentos(monumentos: Array<String?>? = listaDeMonumentos("")) {
        monumentosLista = findViewById(id.monumentosLista)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, monumentos!!)
        monumentosLista!!.adapter = adapter
    }

    private fun calculaDistancia(index: Int, m: Monumento?, lat2: Double, lng2: Double) {
        val earthRadius = 6372.795477598 //kilometers
        val dLat = Math.toRadians(lat2 - m!!.latitude)
        val dLng = Math.toRadians(lng2 - m.longitude)
        val sindLat = Math.sin(dLat / 2)
        val sindLng = Math.sin(dLng / 2)
        val a = Math.pow(sindLat, 2.0) + (Math.pow(sindLng, 2.0)
                * Math.cos(Math.toRadians(m.latitude))
                * Math.cos(Math.toRadians(lat2)))
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val distancia = earthRadius * c
        if (distancia < menorDistancia) {
            menorDistancia = distancia
            monumentoMaisProximo = m
            monumentoMaisProximoIndex = index
        }
    }

    private fun setVisitados(b: Boolean) {
        var i = 0
        for (m in monumentosObjectList) {
            visitado[m!!.idMonumento] = b
            anunciado[m.idMonumento] = b
            monumentosObjectList[i] = m
            i++
        }
    }

    //DEVICE START
    private fun isConnected(op: String): Boolean {
        var op = op
        op = op.lowercase().replace("-", "")
        return when (op) {
            "wifi" -> {
                val connManager =
                    getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.isConnected
            }
            else -> {
                val cm =
                    this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = cm.activeNetworkInfo
                activeNetwork != null && activeNetwork.isConnectedOrConnecting
            }
        }
    }

    private val isConnected: Boolean
        private get() {
            val cm = this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting
        }

    private fun speech() {
        if (isConnected) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            if (intent.resolveActivity(packageManager) != null) {
                speechActivityResultLauncher.launch(intent)
            } else {
                Toast.makeText(
                    this,
                    getString(string.seu_dispositivo_nao_suporta_entrada_de_fala),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val speechActivityResultLauncher = registerForActivityResult<Intent, ActivityResult>(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // There are no request codes
            val data = result.data
            val textos = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val textosConcatenados = StringBuilder()
            assert(textos != null)
            for (t in textos!!) {
                textosConcatenados.append(t)
            }
            urlInput!!.setQuery(textosConcatenados.toString(), true)
        }
    }

    private fun disableKeyboard() {
        if (isConnected) {
            val view = this.currentFocus
            if (view != null) {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(view.windowToken, 0)
            }
        }
    }

    @get:SuppressLint("NewApi")
    val permissions: Unit
        get() {
            val noLocation = ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
            val noStorage =
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
            if (noLocation) {
                Thread(permissoesMsg).start()
                val permissionRequest =
                    registerForActivityResult<Array<String>, Map<String, Boolean>>(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        val fineLocationGranted =
                            permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
                        val coarseLocationGranted =
                            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
                        val writeStorageGranted =
                            permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: false
                        val readStorageGranted =
                            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
                        location =
                            fineLocationGranted != null && fineLocationGranted || coarseLocationGranted != null && coarseLocationGranted
                        storage =
                            writeStorageGranted != null && java.lang.Boolean.TRUE == readStorageGranted
                        if (location && storage) {
                            setLocationManager()
                            audio.stop()
                            Thread(baixarAudioDescricaoDeMonumentos).start()
                        } else if (location) {
                            message!!.setText(string.autorize_localizacao)
                        }
                    }
                permissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                )
            } else {
                setLocationManager()
            }
            if (!noStorage) {
                Thread(baixarAudioDescricaoDeMonumentos).start()
            }
        }
    private val isAccessibilityEnabled: Boolean
        private get() {
            val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
            val isAccessibilityEnabled = am.isEnabled
            val isExploreByTouchEnabled = am.isTouchExplorationEnabled
            return isAccessibilityEnabled && isExploreByTouchEnabled
        }

    @SuppressLint("MissingPermission")
    fun setLocationManager() {
        //start overlay service if not started
        try {
            intentService = Intent(this, Coordenadas::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intentService)
            } else {
                startService(intentService)
            }
            calculoProximidade = Thread(monumentoMenorDistancia)
            calculoProximidade!!.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //DEVICE END
    //RUNNABLE START
    private val permissoesMsg = Runnable {
        try {
            Thread.sleep((tempo.segundo * 10).toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (!location) {
            audio = Audio(this, raw.permissoes)
            audio.play()
        }
    }
    private val baixarAudioDescricaoDeMonumentos = Runnable {
        val isNotDownloaded = !Audio().isDownloaded
        if (isConnected("wi-fi") && isNotDownloaded) {
            midiaDownloaded = false
            val baixando = MediaPlayer.create(this, raw.baixando_dados)
            baixando.start()
            for (m in monumentosObjectList) {
                val id = m!!.idMonumento
                var url = host + "audioDescricao.php?idDocumento=" + id
                audio = Audio(this, url)
                url = host + "audioDescricaoNome.php?idDocumento=" + id + "&nome=nome_" + id
                audio = Audio(this, url)
            }
            while (audio.isDownloading || baixando.isPlaying) {
                try {
                    Thread.sleep((tempo.segundo / 4).toLong())
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
            val baixado = MediaPlayer.create(this, raw.dados_baixados)
            baixado.start()
            audio.setDownloaded()
            midiaDownloaded = true
        }
    }
    private val reiniciarVisitados = Runnable {
        while (true) {
            setVisitados(false)
            try {
                Thread.sleep((tempo.hora * 2).toLong())
            } catch (e: InterruptedException) {
                System.exit(0)
            }
        }
    }

    @SuppressLint("NewApi")
    private val updateProgress = Runnable {
        do {
            try {
                val percent = audio.percent
                seekMusic!!.setProgress(percent.toInt(), true)
                Thread.sleep(tempo.segundo.toLong())
            } catch (e: InterruptedException) {
                System.exit(0)
            }
        } while (audio.isPlaying)
        if (audio.percent >= 99) {
            seekMusic!!.progress = 0
        }
        resetPlayer()
    }
    private val playAudio = Runnable {
        if (isPlaying) {
            audio.pause()
            isPlaying = false
        } else {
            if (currentUrl != "") {
                try {
                    audio.play()
                    val progress = Thread(updateProgress)
                    progress.start()
                    buttonAudio!!.setImageResource(R.drawable.ic_media_pause)
                    isPlaying = true
                } catch (ex: Exception) {
                    urlInput!!.setQuery("Erro", false)
                }
            }
        }
    }
    private val monumentoMenorDistancia = Runnable {
        while (true) {
            val l = Coordenadas.location
            try {
                val coordenateFounded = l.latitude != 0.0 && l.longitude != 0.0
                val dataEnough = monumentosObjectList.size != 1
                if (dataEnough && coordenateFounded) {
                    menorDistancia = 1000000.0
                    var index = 0
                    var quantidadeVisitados = 0
                    for (m in monumentosObjectList) {
                        if (m!!.idMonumento != 0) {
                            if (java.lang.Boolean.FALSE == visitado[m.idMonumento]) {
                                calculaDistancia(index, m, l.latitude, l.longitude)
                                quantidadeVisitados++
                            }
                        }
                        index++
                    }
                    if (quantidadeVisitados != 0) {
                        val isKm = menorDistancia * 1000 > 1000
                        val medida = if (isKm) "quilômetros" else "metros"
                        val tourOutMsg = """${monumentoMaisProximo!!.nome}
à ${if (isKm) String.format("%.4f", menorDistancia) else (menorDistancia * 1000).toInt()} $medida"""
                        message!!.text = tourOutMsg
                        coordenadas.setMessage(tourOutMsg)
                        var distaciaMinimaFloat: Float
                        distaciaMinimaFloat = try {
                            val distancia = distaciaMinima!!.text.toString().toFloat()
                            if (distanciaMedida!!.text == "km") distancia * 1000 else distancia
                        } catch (ex: Exception) {
                            0f
                        }
                        if ((menorDistancia * 1000).toInt() <= distaciaMinimaFloat && java.lang.Boolean.FALSE == visitado[monumentoMaisProximo!!.idMonumento] && !audio.isPlaying) {
                            visitado[monumentoMaisProximo!!.idMonumento] = true
                            monumentosObjectList[monumentoMaisProximoIndex] = monumentoMaisProximo
                            reproduzirAudioDescricao(monumentoMaisProximo!!.idMonumento.toString())
                            setCurrentMonumento(monumentoMaisProximo)
                            setMessage(currentMonumento)
                            message!!.setText(string.localizando)
                        } else if (java.lang.Boolean.FALSE == anunciado[monumentoMaisProximo!!.idMonumento] && midiaDownloaded && !audio.isPlaying) {
                            anunciado[monumentoMaisProximo!!.idMonumento] = true
                            val id = monumentoMaisProximo!!.idMonumento
                            audio = Audio(this, raw.localidade_mais_proxima)
                            audio.play()
                            try {
                                Thread.sleep(audio.duration.toLong())
                            } catch (e: InterruptedException) {
                                e.printStackTrace()
                            }
                            val url =
                                host + "audioDescricaoNome.php?idDocumento=" + id + "&nome=nome_" + id
                            audio = Audio(this, url)
                            audio.play()
                        }
                    } else {
                        message!!.setText(string.todos_visitados)
                    }
                } else if (!coordenateFounded) {
                    message!!.setText(string.localizando)
                } else {
                    message!!.setText(string.sem_dados)
                }
            } catch (ex: NullPointerException) {
                Log.d("Erro", ex.message!!)
            }
            try {
                Thread.sleep((tempo.segundo * 10).toLong())
            } catch (e: InterruptedException) {
                System.exit(0)
            }
        }
    } //RUNNABLE END

    companion object {
        //SERVIDOR
        private const val host = "https://desmatamenos.website/"

        // AUDIO START
        private var audio = Audio()
        private var isPlaying = false
        private var monumentosObjectList: MutableList<Monumento?> = ArrayList()
        private var currentMonumento: Monumento? = null
        private var midiaDownloaded = true
    }
}