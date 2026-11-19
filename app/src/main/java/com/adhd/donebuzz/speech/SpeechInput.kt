package com.adhd.donebuzz.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.adhd.donebuzz.i18n.SpeechProblem

/** Live dictation for the add/edit sheet, in whichever language the app is set to. */
class SpeechInput(private val context: Context) {
    var isRecording by mutableStateOf(false)
        private set
    var transcript by mutableStateOf("")
        private set
    var problem by mutableStateOf<SpeechProblem?>(null)

    private var recognizer: SpeechRecognizer? = null

    fun toggle(languageTag: String) {
        if (isRecording) stop() else start(languageTag)
    }

    private fun start(languageTag: String) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            problem = SpeechProblem.UNAVAILABLE
            return
        }
        // Prefer on-device recognition: faster, works offline, and nothing leaves the phone.
        val onDevice = android.os.Build.VERSION.SDK_INT >= 31 && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        val r = if (onDevice) SpeechRecognizer.createOnDeviceSpeechRecognizer(context) else SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle) = read(partialResults)
            override fun onResults(results: Bundle) {
                read(results)
                finish()
            }
            override fun onError(error: Int) {
                // A slow first word is normal; only report a real failure if nothing was heard at all.
                if (transcript.isEmpty()) {
                    problem = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> SpeechProblem.PERMISSION_DENIED
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> SpeechProblem.NO_MATCH
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> SpeechProblem.UNAVAILABLE
                        else -> SpeechProblem.FAILED
                    }
                }
                finish()
            }
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        problem = null
        transcript = ""
        isRecording = true
        r.startListening(intent)
    }

    private fun read(bundle: Bundle) {
        bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { transcript = it }
    }

    fun stop() {
        recognizer?.stopListening()
        isRecording = false
    }

    private fun finish() {
        isRecording = false
        recognizer?.destroy()
        recognizer = null
    }

    fun release() = finish()
}
