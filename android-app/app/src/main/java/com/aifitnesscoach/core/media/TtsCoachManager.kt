package com.aifitnesscoach.core.media

import android.content.Context
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Offline Android Text-To-Speech coach with bilingual locale fallback and cooldowns.
 */
class TtsCoachManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady: Boolean = false
    private var lastSpokenMessage: String = ""
    private var lastSpokenAtMs: Long = 0L

    companion object {
        private const val COOLDOWN_SAME_MSG_MS = 4500L
        private const val COOLDOWN_ANY_MSG_MS = 1800L
    }

    override fun onInit(status: Int) {
        isReady = (status == TextToSpeech.SUCCESS)
    }

    fun speakIfReady(
        text: String,
        locale: Locale,
        enabled: Boolean,
        force: Boolean = false
    ) {
        if (!enabled || !isReady || text.isBlank()) return
        val engine = tts ?: return

        val now = SystemClock.elapsedRealtime()
        if (!force) {
            if (now - lastSpokenAtMs < COOLDOWN_ANY_MSG_MS) return
            if (text == lastSpokenMessage && (now - lastSpokenAtMs) < COOLDOWN_SAME_MSG_MS) return
        }

        val langResult = engine.isLanguageAvailable(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back silently to on-screen text feedback when offline TTS voice is missing
            return
        }

        engine.language = locale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "coach_${now}")
        lastSpokenMessage = text
        lastSpokenAtMs = now
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isReady = false
    }
}
