package co.teina.orion

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.accessibilityservice.GestureDescription.StrokeDescription
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import java.util.Locale


class MyAccessibilityService : AccessibilityService() {

    private var TAG = "MyAccessibilityService"
    private var mLayout: FrameLayout? = null
    private var playbackLayout: FrameLayout? = null
    private var actionBarScreenButton: Button? = null
    private var playPauseButton: Button? = null
    private var repeatButton: Button? = null

    private var apiKey = BuildConfig.GEMINI_API_KEY
    private var generativeModel: GenerativeModel? = null
    private val generativeModelName = "gemini-2.5-flash"
    private val geminiTtsModelName = "gemini-3.1-flash-tts-preview"
    private var readingPromptKey = "prompt_read"
    private var voiceEngine = "android"
    private var geminiVoice = "Kore"
    private var hapticFeedbackEnabled = true
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var geminiSpeechJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var lastReadingText: String? = null
    private var lastAudioFile: File? = null
    private var pendingAndroidSynthesisId: String? = null
    private val generativeModelConfig = generationConfig {
        temperature = 0.9f
    }
    private val safetySettings: List<SafetySetting> = Arrays.asList(
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE),
    )

    private var appLanguage = "EN"
    private val appStrings = mapOf(
        "PL" to mapOf(
            "tts_voice" to "pol_POL_default",
            "prompt_read" to "Jesteś asystentem AI, który pomaga osobie niewidomej. Twoje zadanie polega na odczytaniu zawartości ekranu, ekstrakcję kluczowych informacji, i poinformowania tej osoby o tym, co dzieje się na ekranie. Po Twojej analizie, aplikacja odczyta ją na głos. Bądź zwięzły. Zacznij od: 'Ekran pokazuje...'",
            "prompt_aloud" to "Odczytaj dokładnie widoczny tekst z ekranu słowo po słowie bez parafrazowania, bez wyjaśnień, bez dodawania lub usuwania treści, zachowując oryginalną kolejność i brzmienie.",
            "read_screen" to "Odczyt\nEkranu",
            "processing" to "Chwileczkę...",
            "something_went_wrong" to "Coś poszło nie tak, spróbuj ponownie"
        ),
        "EN" to mapOf(
            "tts_voice" to "eng_GBR_default",
            "prompt_read" to "You are an AI assistant helping a blind person. Your task is to read the screen's content, extract key information, and inform the person about what is happening on the screen. After your analysis, the application will read it out loud. Be concise. Start with: 'The screen shows...'",
            "prompt_aloud" to "Read the visible text from the screen exactly as it appears word for word without summarizing, explaining, adding, or removing anything and preserve the original order.",
            "read_screen" to "Read\nScreen",
            "processing" to "One moment...",
            "something_went_wrong" to "Something went wrong, try again"
        ),
        "ES" to mapOf(
            "tts_voice" to "spa_ESP_default",
            "prompt_read" to "Eres un asistente de IA que ayuda a una persona ciega. Tu tarea es leer el contenido de la pantalla, extraer información clave e informar a la persona sobre lo que está sucediendo en la pantalla. Después de tu análisis, la aplicación lo leerá en voz alta. Sé conciso. Comienza con: 'La pantalla muestra...'",
            "prompt_aloud" to "Lee el texto visible de la pantalla exactamente palabra por palabra sin resumir, explicar, agregar ni eliminar nada y manteniendo el orden original.",
            "read_screen" to "Leer\nPantalla",
            "processing" to "Un momento...",
            "something_went_wrong" to "Algo salió mal, intenta de nuevo"
        ),
        "PT" to mapOf(
            "tts_voice" to "por_BRA_default",
            "prompt_read" to "Você é um assistente de IA ajudando uma pessoa cega. Sua tarefa é ler o conteúdo da tela, extrair informações importantes e informar a pessoa sobre o que está acontecendo na tela. Após sua análise, o aplicativo lerá em voz alta. Seja conciso. Comece com: 'A tela mostra...'",
            "prompt_aloud" to "Leia o texto visível da tela exatamente palavra por palavra sem resumir, explicar, adicionar ou remover nada mantendo a ordem original.",
            "read_screen" to "Ler\nTela",
            "processing" to "Um momento...",
            "something_went_wrong" to "Algo deu errado, tente novamente"
        ),
        "HI" to mapOf(
            "tts_voice" to "hin_IND_default",
            "prompt_read" to "आप एक AI सहायक हैं जो एक नेत्रहीन व्यक्ति की मदद कर रहे हैं। आपका कार्य स्क्रीन की सामग्री को पढ़ना, मुख्य जानकारी निकालना और व्यक्ति को बताना है कि स्क्रीन पर क्या हो रहा है। आपके विश्लेषण के बाद, एप्लिकेशन इसे जोर से पढ़ेगा। संक्षिप्त रहें। इस तरह शुरू करें: 'स्क्रीन दिखाती है...'",
            "prompt_aloud" to "स्क्रीन पर दिखाई देने वाले पाठ को शब्द दर शब्द बिना सारांश, बिना व्याख्या, बिना जोड़ या हटाए उसी क्रम में पढ़ें।",
            "read_screen" to "स्क्रीन\nपढ़ें",
            "processing" to "एक पल...",
            "something_went_wrong" to "कुछ गलत हुआ, फिर से कोशिश करें"
        ),
        "BN" to mapOf(
            "tts_voice" to "ben_IND_default",
            "prompt_read" to "আপনি একজন AI সহায়ক যিনি একজন অন্ধ ব্যক্তিকে সাহায্য করছেন। আপনার কাজ হল স্ক্রিনের বিষয়বস্তু পড়া, মূল তথ্য বের করা এবং ব্যক্তিকে জানানো যে স্ক্রিনে কী ঘটছে। আপনার বিশ্লেষণের পরে, অ্যাপ্লিকেশনটি এটি জোরে পড়বে। সংক্ষিপ্ত থাকুন। এভাবে শুরু করুন: 'স্ক্রিনটি দেখায়...'",
            "prompt_aloud" to "স্ক্রিনে দৃশ্যমান পাঠ্য শব্দে শব্দে হুবহু পড়ুন কোনো সংক্ষেপ, ব্যাখ্যা, যোগ বা বাদ না দিয়ে মূল ক্রম বজায় রেখে।",
            "read_screen" to "স্ক্রীন\nপড়ুন",
            "processing" to "একটু অপেক্ষা করুন...",
            "something_went_wrong" to "কিছু ভুল হয়েছে, আবার চেষ্টা করুন"
        ),
        "AR" to mapOf(
            "tts_voice" to "ara_EGY_default",
            "prompt_read" to "أنت مساعد ذكاء اصطناعي تساعد شخصًا كفيفًا. مهمتك هي قراءة محتوى الشاشة واستخراج المعلومات الأساسية وإبلاغ الشخص بما يحدث على الشاشة. بعد تحليلك، سيقرأ التطبيق المحتوى بصوت عالٍ. كن موجزًا. ابدأ بـ: 'تظهر الشاشة...'",
            "prompt_aloud" to "اقرأ النص الظاهر على الشاشة حرفيًا كلمة بكلمة دون تلخيص أو شرح أو إضافة أو حذف مع الحفاظ على الترتيب الأصلي.",
            "read_screen" to "قراءة\nالشاشة",
            "processing" to "لحظة من فضلك...",
            "something_went_wrong" to "حدث خطأ ما، حاول مرة أخرى"
        ),
        "SW" to mapOf(
            "tts_voice" to "swa_KEN_default",
            "prompt_read" to "Wewe ni msaidizi wa AI unayesaidia mtu asiyeona. Kazi yako ni kusoma maudhui ya skrini, kuchambua taarifa muhimu, na kumjulisha mtu kuhusu kinachoendelea kwenye skrini. Baada ya uchambuzi wako, programu itasoma kwa sauti. Fupi. Anza na: 'Skrini inaonyesha...'",
            "prompt_aloud" to "Soma maandishi yanayoonekana kwenye skrini neno kwa neno bila kufupisha, kufafanua, kuongeza au kuondoa chochote kwa mpangilio wa asili.",
            "read_screen" to "Soma\nSkrini",
            "processing" to "Subiri kidogo...",
            "something_went_wrong" to "Kuna hitilafu, jaribu tena"
        ),
        "UR" to mapOf(
            "tts_voice" to "urd_PAK_default",
            "prompt_read" to "آپ ایک AI معاون ہیں جو ایک نابینا شخص کی مدد کر رہے ہیں۔ آپ کا کام اسکرین کے مواد کو پڑھنا، اہم معلومات نکالنا اور شخص کو بتانا ہے کہ اسکرین پر کیا ہو رہا ہے۔ آپ کے تجزیے کے بعد، ایپلیکیشن اسے بلند آواز میں پڑھے گی۔ مختصر رہیں۔ اس طرح شروع کریں: 'اسکرین دکھاتی ہے...'",
            "prompt_aloud" to "اسکرین پر موجود متن کو لفظ بہ لفظ بغیر خلاصہ، وضاحت، اضافہ یا کمی کے اصل ترتیب میں پڑھیں۔",
            "read_screen" to "اسکرین\nپڑھیں",
            "processing" to "ایک لمحہ...",
            "something_went_wrong" to "کچھ غلط ہوا، دوبارہ کوشش کریں"
        ),
        "VI" to mapOf(
            "tts_voice" to "vie_VNM_default",
            "prompt_read" to "Bạn là trợ lý AI giúp đỡ người mù. Nhiệm vụ của bạn là đọc nội dung màn hình, trích xuất thông tin quan trọng và thông báo cho người đó về những gì đang xảy ra trên màn hình. Sau khi phân tích, ứng dụng sẽ đọc to nội dung. Hãy ngắn gọn. Bắt đầu bằng: 'Màn hình hiển thị...'",
            "prompt_aloud" to "Đọc văn bản hiển thị trên màn hình chính xác từng từ một mà không tóm tắt, giải thích, thêm hoặc bớt và giữ nguyên thứ tự ban đầu.",
            "read_screen" to "Đọc\nMàn hình",
            "processing" to "Chờ chút...",
            "something_went_wrong" to "Có lỗi xảy ra, thử lại"
        ),
        "ID" to mapOf(
            "tts_voice" to "ind_IDN_default",
            "prompt_read" to "Anda adalah asisten AI yang membantu orang buta. Tugas Anda adalah membaca konten layar, mengekstrak informasi penting, dan memberi tahu orang tersebut tentang apa yang terjadi di layar. Setelah analisis Anda, aplikasi akan membacanya dengan keras. Ringkas. Mulai dengan: 'Layar menampilkan...'",
            "prompt_aloud" to "Bacakan teks yang terlihat di layar kata demi kata tanpa merangkum, menjelaskan, menambah, atau mengurangi apa pun dengan urutan asli.",
            "read_screen" to "Baca\nLayar",
            "processing" to "Sebentar...",
            "something_went_wrong" to "Terjadi kesalahan, coba lagi"
        ),
        "AM" to mapOf(
            "tts_voice" to "amh_ETH_default",
            "prompt_read" to "እርስዎ ዓይነ ስውር ሰውን የሚረዳ AI ረዳት ነዎት። ስራዎ የማያ ገጹን ይዘት ማንበብ፣ ቁልፍ መረጃን ማውጣት እና ሰውየው በማያ ገጹ ላይ ስለሚከሰተው ነገር ማሳወቅ ነው። ከትንተናዎ በኋላ አፕሊኬሽኑ በጮክ ያነባል። አጭር ይሁኑ። እንደዚህ ይጀምሩ: 'ማያ ገጹ ያሳያል...'",
            "prompt_aloud" to "በማያ ገጹ ላይ የሚታየውን ጽሑፍ ቃል በቃል ሳይነገር ሳይጨመር ሳይቀነስ በመጀመሪያው ቅደም ተከተል አንብብ።",
            "read_screen" to "ማያ ገጽ\nአንብብ",
            "processing" to "ትንሽ ይጠብቁ...",
            "something_went_wrong" to "ስህተት ተፈጥሯል፣ እንደገና ይሞክሩ"
        ),
        "TL" to mapOf(
            "tts_voice" to "tgl_PHL_default",
            "prompt_read" to "Ikaw ay isang AI assistant na tumutulong sa isang bulag na tao. Ang iyong tungkulin ay basahin ang nilalaman ng screen, kunin ang mahalagang impormasyon, at ipaalam sa tao kung ano ang nangyayari sa screen. Pagkatapos ng iyong pagsusuri, babasahin ng application ito nang malakas. Maging maigsi. Magsimula sa: 'Ang screen ay nagpapakita...'",
            "prompt_aloud" to "Basahin ang tekstong nakikita sa screen nang eksakto salita sa salita nang walang buod, paliwanag, dagdag o bawas at panatilihin ang orihinal na ayos.",
            "read_screen" to "Basahin\nang Screen",
            "processing" to "Sandali lang...",
            "something_went_wrong" to "May naganap na mali, subukan ulit"
        )
    )

    // TTS Service
    lateinit var textToSpeech: TextToSpeech

    // Vibrations
    lateinit var vibratorManager: VibratorManager

    // Touch events
    var autoConfirmRecording: Boolean = false

    // BroadcastReceiver to handle the screen capture permission result and screenshot path
    private val genericBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val actionIdent = intent?.action.toString()
            val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            val data = intent?.getParcelableExtra("data", Intent::class.java)
            val path = intent?.getParcelableExtra("path", String::class.java)
            val actionResultIsOK = resultCode == Activity.RESULT_OK && data != null

            if (actionIdent == "co.teina.ACTION_FRESH_SCREENSHOT") {
                Log.i(TAG, "ACTION_FRESH_SCREENSHOT")
                actionBarScreenButton?.visibility = View.VISIBLE
                if (path != null) {
                    serviceScope.launch {
                        chatWithGemini(path)
                    }
                }
            }

            if (actionIdent == "co.teina.ACTION_SCREEN_CAPTURE") {
                if (actionResultIsOK) {
                    val serviceIntent = Intent(
                        this@MyAccessibilityService,
                        ScreenCaptureForegroundService::class.java
                    )
                    serviceIntent.putExtra("resultCode", resultCode)
                    serviceIntent.putExtra("data", data)

                    android.os.Handler(mainLooper).postDelayed({
                        startForegroundService(serviceIntent)
                        Log.d(TAG, "Foreground service started after 1s delay.")
                    }, 1000)

                } else {
                    Log.e(TAG, "Screen capture permission was not granted.")
                }
            }
        }
    }

    private fun speakToUser(outputContent: String, stopSpeakingFirst: Boolean) {
        if (textToSpeech != null) {
            Log.i(TAG, "Speaking...")
            if (stopSpeakingFirst) {
                if (textToSpeech.isSpeaking) {
                    textToSpeech.stop()
                }
                textToSpeech.speak(outputContent, TextToSpeech.QUEUE_FLUSH, null, TAG)
            } else {
                textToSpeech.speak(outputContent, TextToSpeech.QUEUE_ADD, null, TAG)
            }
        } else {
            Log.i(TAG, "TTS not started.")
        }
    }

    private fun speakGeneratedText(outputContent: String) {
        lastReadingText = outputContent
        clearPreparedReading()

        if (voiceEngine == "gemini") {
            geminiSpeechJob = serviceScope.launch {
                var audioFile: File? = null
                try {
                    audioFile = createGeminiAudioFile(generateGeminiSpeechWithRetry(outputContent))
                    withContext(Dispatchers.Main) {
                        playAudioFile(audioFile)
                    }
                } catch (e: CancellationException) {
                    audioFile?.delete()
                    throw e
                } catch (e: Exception) {
                    audioFile?.delete()
                    Log.e(TAG, "Gemini TTS failed, falling back to Android TTS.", e)
                    withContext(Dispatchers.Main) {
                        synthesizeAndroidSpeech(outputContent)
                    }
                }
            }
        } else {
            synthesizeAndroidSpeech(outputContent)
        }
    }

    private suspend fun generateGeminiSpeechWithRetry(outputContent: String): ByteArray {
        return try {
            generateGeminiSpeech(outputContent)
        } catch (e: GeminiTtsHttpException) {
            if (!e.isRetryable) {
                throw e
            }

            Log.w(TAG, "Gemini TTS failed with HTTP ${e.statusCode}; retrying once.")
            delay(1_000)
            generateGeminiSpeech(outputContent)
        } catch (e: java.io.IOException) {
            Log.w(TAG, "Gemini TTS network request failed; retrying once.", e)
            delay(1_000)
            generateGeminiSpeech(outputContent)
        }
    }

    private fun synthesizeAndroidSpeech(outputContent: String) {
        val utteranceId = "orion_reading_${System.currentTimeMillis()}"
        val audioFile = File(cacheDir, "$utteranceId.wav")
        pendingAndroidSynthesisId = utteranceId
        val result = textToSpeech.synthesizeToFile(outputContent, null, audioFile, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            pendingAndroidSynthesisId = null
            audioFile.delete()
            fallbackToDirectAndroidSpeech(outputContent)
        }
    }

    private fun fallbackToDirectAndroidSpeech(outputContent: String) {
        Log.i(TAG, "Playing reading directly through Android TTS.")
        showPlaybackControls()
        speakToUser(outputContent, true)
    }

    private fun generateGeminiSpeech(outputContent: String): ByteArray {
        val ttsPrompt =
            "Read the following text naturally and clearly at a comfortable pace. " +
                "Preserve the wording exactly:\n\n$outputContent"
        val requestBody = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", ttsPrompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put(
                        "speechConfig",
                        JSONObject().put(
                            "voiceConfig",
                            JSONObject().put(
                                "prebuiltVoiceConfig",
                                JSONObject().put("voiceName", geminiVoice)
                            )
                        )
                    )
            )

        val connection = URL(
            "https://generativelanguage.googleapis.com/v1beta/models/$geminiTtsModelName:generateContent"
        ).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-goog-api-key", apiKey ?: "")
            connection.outputStream.use {
                it.write(requestBody.toString().toByteArray(Charsets.UTF_8))
            }

            val responseBody = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw GeminiTtsHttpException(connection.responseCode, errorBody)
            }

            val encodedAudio = JSONObject(responseBody)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getJSONObject("inlineData")
                .getString("data")

            return Base64.decode(encodedAudio, Base64.DEFAULT)
        } finally {
            connection.disconnect()
        }
    }

    private class GeminiTtsHttpException(
        val statusCode: Int,
        errorBody: String?
    ) : IllegalStateException("Gemini TTS HTTP $statusCode: $errorBody") {
        val isRetryable = statusCode == 429 || statusCode in 500..599
    }

    private fun createGeminiAudioFile(pcmAudio: ByteArray): File {
        require(pcmAudio.isNotEmpty()) { "Gemini TTS returned empty audio." }

        val audioFile = File(cacheDir, "orion_gemini_${System.currentTimeMillis()}.wav")
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray())
            putInt(36 + pcmAudio.size)
            put("WAVE".toByteArray())
            put("fmt ".toByteArray())
            putInt(16)
            putShort(1)
            putShort(1)
            putInt(24_000)
            putInt(24_000 * 2)
            putShort(2)
            putShort(16)
            put("data".toByteArray())
            putInt(pcmAudio.size)
        }

        FileOutputStream(audioFile).use {
            it.write(header.array())
            it.write(pcmAudio)
        }
        return audioFile
    }

    private fun playAudioFile(audioFile: File) {
        try {
            stopPlayback()
            lastAudioFile?.takeIf { it != audioFile }?.delete()
            lastAudioFile = audioFile

            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                setOnCompletionListener {
                    it.seekTo(0)
                    updatePlayPauseButton(false)
                }
                setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    fallbackToDirectAndroidSpeech(lastReadingText ?: return@setOnErrorListener true)
                    true
                }
                prepare()
                start()
            }
            showPlaybackControls()
            updatePlayPauseButton(true)
        } catch (e: Exception) {
            Log.e(TAG, "Cached audio playback failed, falling back to Android TTS.", e)
            stopPlayback()
            audioFile.delete()
            fallbackToDirectAndroidSpeech(lastReadingText ?: return)
        }
    }

    private fun togglePlayback() {
        val player = mediaPlayer
        if (player == null) {
            lastReadingText?.let { speakGeneratedText(it) }
        } else if (player.isPlaying) {
            player.pause()
            updatePlayPauseButton(false)
        } else {
            player.start()
            updatePlayPauseButton(true)
        }
    }

    private fun repeatLastReading() {
        val player = mediaPlayer
        if (player == null) {
            lastReadingText?.let { speakGeneratedText(it) }
        } else {
            player.seekTo(0)
            player.start()
            updatePlayPauseButton(true)
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.run {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        updatePlayPauseButton(false)
    }

    private fun clearPreparedReading() {
        geminiSpeechJob?.cancel()
        geminiSpeechJob = null
        pendingAndroidSynthesisId = null
        stopPlayback()
        lastAudioFile?.delete()
        lastAudioFile = null
        cacheDir.listFiles()
            ?.filter { it.name.startsWith("orion_reading_") || it.name.startsWith("orion_gemini_") }
            ?.forEach { it.delete() }
        playbackLayout?.visibility = View.GONE
    }

    private fun showPlaybackControls() {
        playbackLayout?.visibility = View.VISIBLE
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        playPauseButton?.text = getString(
            if (isPlaying) R.string.action_pause_reading else R.string.action_play_reading
        )
    }

    suspend fun chatWithGemini(imagePath: String) {
        try {
            appStrings[appLanguage]?.get("processing")?.let { speakToUser(it, true) }

            vibrate(VibrationEffect.EFFECT_TICK)

            val prompt =
                appStrings[appLanguage]?.get(readingPromptKey)
                    ?: appStrings["EN"]?.get(readingPromptKey)
                    ?: ""

            val inputContent = content {
                image(BitmapFactory.decodeFile(imagePath))
                text(prompt)
            }

            var outputContent = ""

            generativeModel?.generateContentStream(inputContent)?.collect { response ->
                vibrate(VibrationEffect.EFFECT_TICK)
                outputContent += response.text
            }


            var imageFile = File(imagePath)
            try {
                imageFile.delete()
                Log.i(TAG, "File removed: ${imagePath}")
            } catch (fileException: Exception) {
                Log.d(TAG, fileException.toString())
            }

            Log.i(TAG, outputContent)
            vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
            withContext(Dispatchers.Main) {
                speakGeneratedText(outputContent)
            }

        } catch (e: Exception) {
            speakToUser(appStrings[appLanguage]?.get("something_went_wrong").toString(), true)
            Log.e(TAG, e.localizedMessage)
        }
    }

    override fun onServiceConnected() {
        // Vibrator
        vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

        // Set up language and settings
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val newLang = preferences.getString("pref_orion_language", "EN").toString()
        val newApiKey = preferences.getString("pref_gemini_api_key", "").toString()
        val newAutoConfirm = preferences.getBoolean("pref_orion_auto_confirm", false)
        val newTtsSpeed = preferences.getString("pref_orion_tts_speed", "1.0")!!.toFloat()
        val newReadingPromptKey = preferences.getString("pref_orion_reading_mode", "prompt_read").toString()
        voiceEngine = preferences.getString("pref_orion_voice_engine", "android").toString()
        geminiVoice = preferences.getString("pref_orion_gemini_voice", "Kore").toString()
        hapticFeedbackEnabled = preferences.getBoolean("pref_orion_haptic_feedback", true)

        if (newLang.isNotEmpty()) {
            appLanguage = newLang
        }

        if (newApiKey.isNotEmpty()) {
            apiKey = newApiKey
        }

        if (newReadingPromptKey.isNotEmpty()) {
            readingPromptKey = newReadingPromptKey
        }

        autoConfirmRecording = newAutoConfirm

        // Set up the overlay UI
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mLayout = FrameLayout(this)
        playbackLayout = FrameLayout(this).apply {
            visibility = View.GONE
        }

        // Set up WindowManager layout parameters
        val lp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.END // Align the buttons
        }
        val playbackLp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.START
        }

        // Inflate your layout
        val inflater = LayoutInflater.from(this)
        inflater.inflate(R.layout.action_bar, mLayout)
        inflater.inflate(R.layout.playback_controls, playbackLayout)

        // Add the inflated layout to the WindowManager
        wm.addView(mLayout, lp)
        wm.addView(playbackLayout, playbackLp)

        // Register the BroadcastReceiver to receive the result of screen capture permission
        registerReceiver(
            genericBroadcastReceiver,
            IntentFilter("co.teina.ACTION_SCREEN_CAPTURE"),
            RECEIVER_EXPORTED
        )
        registerReceiver(
            genericBroadcastReceiver,
            IntentFilter("co.teina.ACTION_FRESH_SCREENSHOT"),
            RECEIVER_EXPORTED
        )

        // Create LLM Client
        generativeModel = GenerativeModel(
            modelName = generativeModelName,
            apiKey = apiKey,
            generationConfig = generativeModelConfig,
            safetySettings = safetySettings
        )

        // Set-up TTS
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.forLanguageTag(appLanguage)

                val desiredVoice = appStrings[appLanguage]?.get("tts_voice").toString()
                val voices: Set<Voice> = textToSpeech.getVoices()
                val voiceList: List<Voice> = ArrayList(voices)

                for (voice in voiceList) {
                    if (voice.locale.toString().equals(desiredVoice)) {
                        textToSpeech.setVoice(voice)
                    }
                }

                textToSpeech.setSpeechRate(newTtsSpeed)

                Log.i(
                    TAG,
                    "TextToSpeech Initialization Success: ${appLanguage}, ${desiredVoice}, ${newTtsSpeed}"
                )
            } else {
                Log.d(TAG, "TextToSpeech Initialization Failed")
            }
        }
        textToSpeech.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    if (utteranceId != pendingAndroidSynthesisId) {
                        File(cacheDir, "$utteranceId.wav").delete()
                        return
                    }

                    val audioFile = File(cacheDir, "$utteranceId.wav")
                    pendingAndroidSynthesisId = null
                    serviceScope.launch(Dispatchers.Main) {
                        if (audioFile.exists()) {
                            playAudioFile(audioFile)
                        } else {
                            fallbackToDirectAndroidSpeech(lastReadingText ?: return@launch)
                        }
                    }
                }

                override fun onError(utteranceId: String?) {
                    if (utteranceId != pendingAndroidSynthesisId) {
                        File(cacheDir, "$utteranceId.wav").delete()
                        return
                    }

                    pendingAndroidSynthesisId = null
                    File(cacheDir, "$utteranceId.wav").delete()
                    serviceScope.launch(Dispatchers.Main) {
                        fallbackToDirectAndroidSpeech(lastReadingText ?: return@launch)
                    }
                }
            }
        )

        // Bind onClickListener to the button (once layout is inflated)
        actionBarScreenButton = mLayout?.findViewById(R.id.action_bar_button_screen)
        playPauseButton = playbackLayout?.findViewById(R.id.playback_button_play_pause)
        repeatButton = playbackLayout?.findViewById(R.id.playback_button_repeat)

        // Create gradient drawable programmatically
        val gradient = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,  // Set the gradient direction
            intArrayOf(Color.parseColor("#7550e1"), Color.parseColor("#748ded"))  // Colors
        )
        gradient.cornerRadii = floatArrayOf(75f, 75f, 0f, 0f, 0f, 0f, 75f, 75f)
        actionBarScreenButton?.setTextColor(Color.parseColor("#FFFFFF"))

        actionBarScreenButton?.apply {
            background = gradient
            alpha = 0.9f  // Opacity level between 0.0 and 1.0
            text = appStrings[appLanguage]?.get("read_screen")
            textSize = 20f
        }
        listOf(
            playPauseButton to floatArrayOf(0f, 0f, 75f, 75f, 0f, 0f, 0f, 0f),
            repeatButton to floatArrayOf(0f, 0f, 0f, 0f, 75f, 75f, 0f, 0f)
        ).forEach { (button, cornerRadii) ->
            val playbackGradient = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.parseColor("#7550e1"), Color.parseColor("#748ded"))
            )
            playbackGradient.cornerRadii = cornerRadii
            button?.apply {
                background = playbackGradient
                alpha = 0.9f
                setTextColor(Color.parseColor("#FFFFFF"))
            }
        }

        actionBarScreenButton?.setOnClickListener {
            if (textToSpeech.isSpeaking || geminiSpeechJob?.isActive == true ||
                pendingAndroidSynthesisId != null || mediaPlayer?.isPlaying == true
            ) {
                textToSpeech.stop()
                geminiSpeechJob?.cancel()
                pendingAndroidSynthesisId = null
                stopPlayback()
                vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
            } else {
                actionBarScreenButton?.visibility = View.GONE
                vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
                requestScreenCapture() // Re-trigger screen capture permission if needed
            }
        }
        playPauseButton?.setOnClickListener {
            togglePlayback()
            vibrate(VibrationEffect.EFFECT_TICK)
        }
        repeatButton?.setOnClickListener {
            repeatLastReading()
            vibrate(VibrationEffect.EFFECT_HEAVY_CLICK)
        }

        vibrate(VibrationEffect.EFFECT_DOUBLE_CLICK)
    }

    private fun vibrate(effectId: Int) {
        if (hapticFeedbackEnabled) {
            vibratorManager.defaultVibrator.vibrate(VibrationEffect.createPredefined(effectId))
        }
    }

    // Request screen capture permission
    private fun requestScreenCapture() {
        val captureIntent = Intent(this, ScreenCaptureActivity::class.java)
        captureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(captureIntent)
        Log.d(TAG, "Requesting screen capture permission...")
    }

    private fun touchTo(x: Float, y: Float) {
        val swipePath: Path = Path()
        swipePath.moveTo(x, y)
        swipePath.lineTo(x, y)
        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(StrokeDescription(swipePath, 0, 50))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    private fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    private fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        var source = event!!.source
        if (event != null
            && source != null
            && event!!.eventType === AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) {
            if (source.getPackageName().equals("com.android.systemui")) {

                val confirm =
                    getRootInActiveWindow()
                        .findAccessibilityNodeInfosByText("Orion?")

                if (confirm.size !== 0 && autoConfirmRecording) {
                    val x = (getScreenWidth() - 200).toFloat()
                    val y = (getScreenHeight() - 120).toFloat()

                    Log.i(TAG, "X:${x}, Y:${y}")
                    touchTo(x, y)
                }

            }
        }
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(genericBroadcastReceiver) // Unregister the receiver when the service is destroyed
        Log.d(TAG, "MyAccessibilityService destroyed.")
        if (textToSpeech != null) {
            textToSpeech.shutdown()
        }
        geminiSpeechJob?.cancel()
        stopPlayback()
        lastAudioFile?.delete()
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        mLayout?.let { runCatching { wm.removeView(it) } }
        playbackLayout?.let { runCatching { wm.removeView(it) } }
        cacheDir.listFiles()
            ?.filter { it.name.startsWith("orion_reading_") || it.name.startsWith("orion_gemini_") }
            ?.forEach { it.delete() }
        serviceScope.cancel()

    }

}

