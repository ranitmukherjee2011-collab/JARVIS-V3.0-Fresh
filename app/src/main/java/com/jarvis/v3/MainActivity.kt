package com.jarvis.v3

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.speech.*
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

enum class JarvisState { IDLE, LISTENING, PROCESSING, SPEAKING, EXECUTING, SUCCESS, ERROR, WAKE_DETECTED }

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null

    private val permissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        permissions.launch(arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR,
            Manifest.permission.POST_NOTIFICATIONS
        ))
        setContent { JarvisScreen() }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(0.92f)
        }
    }

    private fun speak(s: String) {
        tts?.speak(s, TextToSpeech.QUEUE_FLUSH, null, "jarvis")
    }

    private fun listen(onResult: (String) -> Unit) {
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(b: Bundle?) {
                onResult(b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: "")
            }
            override fun onError(e: Int) {}
            override fun onReadyForSpeech(p: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(b: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        recognizer?.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        })
    }

    @Composable
    private fun JarvisScreen() {
        var state by remember { mutableStateOf(JarvisState.IDLE) }
        var text by remember { mutableStateOf("JARVIS V3.0 online.") }

        fun command(raw: String) {
            val c = raw.lowercase(Locale.getDefault()).trim()
            state = JarvisState.PROCESSING
            text = "You: $raw"
            when {
                c.contains("open whatsapp") -> {
                    state = JarvisState.EXECUTING
                    val i = packageManager.getLaunchIntentForPackage("com.whatsapp")
                    if (i != null) {
                        startActivity(i); text = "JARVIS: Opening WhatsApp."; speak("Opening WhatsApp."); state = JarvisState.SUCCESS
                    } else { text = "JARVIS: WhatsApp is not installed."; speak("WhatsApp is not installed."); state = JarvisState.ERROR }
                }
                c.startsWith("open ") -> {
                    val wanted = c.removePrefix("open ").trim()
                    val pm = packageManager
                    val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
                    val hit = apps.firstOrNull { it.loadLabel(pm).toString().lowercase().contains(wanted) }
                    if (hit != null) {
                        state = JarvisState.EXECUTING
                        startActivity(pm.getLaunchIntentForPackage(hit.activityInfo.packageName))
                        text = "JARVIS: Opening ${hit.loadLabel(pm)}."; speak("Opening ${hit.loadLabel(pm)}."); state = JarvisState.SUCCESS
                    } else { text = "JARVIS: App not found."; speak("I couldn't find that app."); state = JarvisState.ERROR }
                }
                c.contains("calendar") || c.contains("schedule") -> {
                    state = JarvisState.EXECUTING
                    startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI))
                    text = "JARVIS: Opening Calendar."; speak("Opening Calendar."); state = JarvisState.SUCCESS
                }
                c.contains("settings") -> {
                    startActivity(Intent(Settings.ACTION_SETTINGS)); text = "JARVIS: Opening settings."; speak("Opening settings."); state = JarvisState.SUCCESS
                }
                c.startsWith("call ") -> {
                    val number = raw.substringAfter("call ").trim()
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}")))
                    text = "JARVIS: Opening dialer."; speak("Opening the dialer."); state = JarvisState.SUCCESS
                }
                else -> {
                    text = "JARVIS: \"$raw\" — Gemini agent connection required for full reasoning."
                    speak("I heard your command. Connect the Gemini agent to enable full reasoning.")
                    state = JarvisState.SPEAKING
                }
            }
        }

        Surface(Modifier.fillMaxSize(), color = Color(0xFF05070B)) {
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("JARVIS", color = Color.White, fontSize = 30.sp)
                Text("PERSONAL AI AGENT • V3.0", color = Color(0xFF73D7FF), fontSize = 12.sp)
                HeartCore(state, Modifier.size(310.dp))
                Text(state.name.replace("_", " "), color = Color(0xFF73D7FF), fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(text, color = Color.LightGray, fontSize = 15.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = {
                    state = JarvisState.LISTENING
                    text = "JARVIS: Listening..."
                    listen { command(it) }
                }) { Text("🎙  TALK TO JARVIS") }
            }
        }
    }

    @Composable
    private fun HeartCore(state: JarvisState, modifier: Modifier) {
        val inf = rememberInfiniteTransition(label = "heart")
        val pulse by inf.animateFloat(
            0.91f, 1.08f,
            infiniteRepeatable(
                tween(if (state == JarvisState.LISTENING || state == JarvisState.WAKE_DETECTED) 400 else 1100, easing = FastOutSlowInEasing),
                RepeatMode.Reverse), label = "pulse")
        val spin by inf.animateFloat(
            0f, 360f,
            infiniteRepeatable(tween(if (state == JarvisState.PROCESSING || state == JarvisState.EXECUTING) 3000 else 10000, easing = LinearEasing)),
            label = "spin")
        Box(modifier.scale(pulse).rotate(spin), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(Color(0x221EA7FF), r * .86f)
                drawCircle(Color(0x332EA8FF), r * .66f)
                repeat(12) { i -> drawArc(Color(0xFF58B9FF), i * 30f, 13f, false, style = Stroke(5f)) }
                drawCircle(Color(0xFF0B2135), r * .43f)
                drawCircle(Color(0xFF73D7FF), r * .28f)
                drawCircle(Color.White, r * .10f)
            }
        }
    }
}
