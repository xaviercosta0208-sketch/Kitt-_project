package com.kitt.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var status: TextView
    private lateinit var transcript: TextView
    private lateinit var talkButton: Button
    private lateinit var tts: TextToSpeech
    private var speechRecognizer: SpeechRecognizer? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy { getSharedPreferences("kitt_settings", MODE_PRIVATE) }
    private var ttsReady = false
    private var listening = false

    private val red = Color.rgb(255, 42, 48)
    private val black = Color.rgb(5, 6, 8)
    private val panel = Color.rgb(18, 21, 25)
    private val white = Color.rgb(235, 239, 242)
    private val muted = Color.rgb(155, 164, 174)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tts = TextToSpeech(this, this)
        buildUi()
        showStatus("SISTEMAS EN ESPERA")
        transcript.text = "KITT: Sistemas en línea. Estoy listo para ayudarte."
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
        }
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(18))
            setBackgroundColor(black)
        }

        val top = TextView(this).apply {
            text = "K I T T"
            textSize = 30f
            typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
            setTextColor(red)
            gravity = Gravity.CENTER
            letterSpacing = 0.18f
        }
        root.addView(top, matchWrap())

        val subtitle = TextView(this).apply {
            text = "ASISTENTE PERSONAL • PROTOTIPO 0.1"
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
            setPadding(0, dp(4), 0, dp(18))
        }
        root.addView(subtitle, matchWrap())

        val scanner = TextView(this).apply {
            text = "━━━━━━━━━━━━━━━━━━━━━━━━"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(red)
            setShadowLayer(18f, 0f, 0f, red)
        }
        root.addView(scanner, matchWrap())

        status = TextView(this).apply {
            textSize = 12f
            setTextColor(red)
            gravity = Gravity.CENTER
            letterSpacing = 0.1f
            setPadding(0, dp(16), 0, dp(12))
        }
        root.addView(status, matchWrap())

        val transcriptBox = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(panel)
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        transcript = TextView(this).apply {
            textSize = 16f
            setTextColor(white)
            setLineSpacing(dp(5).toFloat(), 1.0f)
        }
        transcriptBox.addView(transcript)
        root.addView(transcriptBox, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        talkButton = Button(this).apply {
            text = "🎙  HABLAR CON KITT"
            textSize = 16f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(red))
            setOnClickListener { startListening() }
        }
        val btnParams = matchWrap()
        btnParams.topMargin = dp(18)
        root.addView(talkButton, btnParams)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val settings = Button(this).apply {
            text = "Configurar IA"
            isAllCaps = false
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel))
            setOnClickListener { showSettings() }
        }
        val test = Button(this).apply {
            text = "Probar voz"
            isAllCaps = false
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(panel))
            setOnClickListener { speak("A su servicio. Todos los sistemas funcionan dentro de los límites del prototipo.") }
        }
        row.addView(settings, LinearLayout.LayoutParams(0, dp(52), 1f))
        row.addView(test, LinearLayout.LayoutParams(0, dp(52), 1f))
        root.addView(row, matchWrap())

        val footer = TextView(this).apply {
            text = "Usá los comandos de voz solo cuando sea seguro. No manipules el teléfono mientras conducís."
            textSize = 10f
            setTextColor(muted)
            gravity = Gravity.CENTER
            setPadding(dp(6), dp(10), dp(6), 0)
        }
        root.addView(footer, matchWrap())
        setContentView(root)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 41)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            showStatus("RECONOCIMIENTO DE VOZ NO DISPONIBLE")
            speak("No encuentro un servicio de reconocimiento de voz en este teléfono.")
            return
        }
        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { listening = true; showStatus("ESCUCHANDO"); talkButton.text = "ESCUCHANDO…" }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { listening = false; talkButton.text = "🎙  HABLAR CON KITT" }
                override fun onError(error: Int) {
                    listening = false
                    talkButton.text = "🎙  HABLAR CON KITT"
                    showStatus("NO SE DETECTÓ VOZ • INTENTÁ DE NUEVO")
                }
                override fun onResults(results: Bundle?) {
                    listening = false
                    talkButton.text = "🎙  HABLAR CON KITT"
                    val phrase = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!phrase.isNullOrBlank()) handlePhrase(phrase)
                    else showStatus("NO ENTENDÍ EL COMANDO")
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-AR")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Hablá con KITT")
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            showStatus("ERROR AL INICIAR EL MICRÓFONO")
        }
    }

    private fun handlePhrase(phrase: String) {
        transcript.text = "VOS: $phrase\n\nProcesando..."
        showStatus("ANALIZANDO COMANDO")
        val local = localReply(phrase)
        if (local != null) {
            transcript.text = "VOS: $phrase\n\nKITT: $local"
            showStatus("MODO LOCAL")
            speak(local)
            return
        }
        val key = prefs.getString("api_key", "").orEmpty().trim()
        if (key.isBlank()) {
            val reply = "No encuentro una clave de inteligencia artificial configurada. Puedo responder comandos básicos en modo local. Para activar la conversación inteligente, abrí Configurar IA."
            transcript.text = "VOS: $phrase\n\nKITT: $reply"
            showStatus("MODO LOCAL • IA SIN CONFIGURAR")
            speak(reply)
            return
        }
        askOnline(phrase, key)
    }

    private fun localReply(raw: String): String? {
        val p = raw.lowercase(Locale("es", "AR"))
        return when {
            "cómo te llamas" in p || "quién sos" in p || "quien sos" in p ->
                "Soy KITT, tu asistente personal. Estoy a tu servicio."
            "hora" in p -> "No tengo un reloj hablado integrado todavía. Podés consultar la hora en el teléfono."
            "cómo estás" in p || "como estas" in p ->
                "Todos mis sistemas están operativos. Gracias por preguntar."
            "gracias" in p -> "Un placer ayudarte. Para eso estoy."
            "ayuda" in p || "qué podés hacer" in p || "que podes hacer" in p ->
                "Por ahora puedo conversar cuando está configurada la inteligencia en línea, y responder algunos comandos básicos sin internet."
            "modo local" in p || "sin internet" in p ->
                "Modo local activo. Las respuestas son limitadas hasta que vuelva la conexión."
            "silencio" in p || "callate" in p || "callate" in p ->
                if (ttsReady) { tts.stop(); "De acuerdo. Quedo en silencio." } else "De acuerdo."
            else -> null
        }
    }

    private fun askOnline(phrase: String, key: String) {
        executor.execute {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 15000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = JSONObject().apply {
                    put("model", "gpt-4o-mini")
                    put("temperature", 0.7)
                    put("max_tokens", 220)
                    put("messages", JSONArray().put(JSONObject().apply {
                        put("role", "system")
                        put("content", "Sos KITT, un asistente ficticio inspirado en el Auto Fantástico. Hablás español rioplatense, con tono masculino grave, sereno, inteligente y levemente irónico. Respondé breve, claro y útil. No afirmes controlar el vehículo ni sistemas que no estén conectados. Si el usuario conduce, priorizá seguridad y respuestas cortas.")
                    }).put(JSONObject().apply {
                        put("role", "user")
                        put("content", phrase)
                    }))
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val text = stream.bufferedReader().use { it.readText() }
                if (code !in 200..299) throw Exception("La API respondió HTTP $code")
                val answer = JSONObject(text).getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content").trim()
                runOnUiThread {
                    transcript.text = "VOS: $phrase\n\nKITT: $answer"
                    showStatus("INTELIGENCIA EN LÍNEA")
                    speak(answer)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    val answer = "No pude conectar con la inteligencia en línea. Revisá la conexión y la clave de API. En modo local sigo disponible."
                    transcript.text = "VOS: $phrase\n\nKITT: $answer\n\nDetalle: ${e.message ?: "error de conexión"}"
                    showStatus("SIN CONEXIÓN CON IA")
                    speak(answer)
                }
            } finally {
                connection?.disconnect()
            }
        }
    }

    private fun showSettings() {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val info = TextView(this).apply {
            text = "Pegá tu clave de API de OpenAI para habilitar conversaciones inteligentes. La clave se guarda solo en este teléfono, en las preferencias privadas de la app. El uso de la API puede generar cargos según tu cuenta."
            setTextColor(white)
            textSize = 14f
        }
        val input = EditText(this).apply {
            hint = "sk-… (clave API)"
            setTextColor(white)
            setHintTextColor(muted)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("api_key", ""))
        }
        box.addView(info, matchWrap())
        box.addView(input, matchWrap())
        android.app.AlertDialog.Builder(this)
            .setTitle("Configurar inteligencia")
            .setView(box)
            .setPositiveButton("Guardar") { _, _ ->
                prefs.edit().putString("api_key", input.text.toString().trim()).apply()
                Toast.makeText(this, "Configuración guardada en el teléfono", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Borrar clave") { _, _ ->
                prefs.edit().remove("api_key").apply()
                Toast.makeText(this, "Clave borrada", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun speak(text: String) {
        if (!ttsReady) {
            showStatus("VOZ DEL SISTEMA CARGANDO")
            return
        }
        tts.stop()
        tts.setPitch(0.82f)
        tts.setSpeechRate(0.88f)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kitt_${System.currentTimeMillis()}")
    }

    override fun onInit(statusCode: Int) {
        ttsReady = statusCode == TextToSpeech.SUCCESS
        if (ttsReady) {
            tts.language = Locale("es", "AR")
            tts.setPitch(0.82f)
            tts.setSpeechRate(0.88f)
        }
    }

    private fun showStatus(text: String) { status.text = "◆  $text  ◆" }
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            showStatus("MICRÓFONO AUTORIZADO")
        } else if (requestCode == 41) {
            showStatus("PERMISO DE MICRÓFONO NECESARIO")
        }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        executor.shutdownNow()
        super.onDestroy()
    }
}
