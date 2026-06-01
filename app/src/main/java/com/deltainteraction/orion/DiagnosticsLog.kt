package com.deltainteraction.orion

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticsLog {
    private const val FILE_NAME = "orion_diagnostics.txt"
    private const val MAX_LINES = 500
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun append(context: Context, message: String) {
        val file = ensureFile(context)
        val existingLines = file.readLines().takeLast(MAX_LINES - 1)
        val cleanMessage = redact(message).replace(Regex("[\\r\\n]+"), " ").take(1_000)
        val line = "${timestampFormat.format(Date())} $cleanMessage"
        file.writeText((existingLines + line).joinToString("\n") + "\n")
    }

    @Synchronized
    fun read(context: Context): String {
        val file = ensureFile(context)
        return file.readLines().takeLast(MAX_LINES).joinToString("\n")
    }

    @Synchronized
    fun clear(context: Context) {
        ensureFile(context).writeText("")
        append(context, "Diagnostics cleared.")
    }

    @Synchronized
    fun ensureFile(context: Context): File {
        return File(context.filesDir, FILE_NAME).also {
            if (!it.exists()) {
                it.createNewFile()
            }
        }
    }

    fun sessionSummary(
        context: Context,
        language: String,
        readingMode: String,
        voiceEngine: String,
        hasApiKey: Boolean
    ): String {
        return "Service connected. version=${BuildConfig.VERSION_NAME}; " +
            "Android=${Build.VERSION.RELEASE}; device=${Build.MANUFACTURER} ${Build.MODEL}; " +
            "language=$language; readingMode=$readingMode; voiceEngine=$voiceEngine; " +
            "hasApiKey=$hasApiKey; package=${context.packageName}"
    }

    fun describe(exception: Throwable): String {
        val detail = redact(exception.message.orEmpty())
            .replace(Regex("[\\r\\n]+"), " ")
            .take(300)
        return "${exception.javaClass.simpleName}: $detail"
    }

    fun redact(message: String): String {
        return message
            .replace(Regex("AIza[0-9A-Za-z_-]+"), "[REDACTED_API_KEY]")
            .replace(
                Regex("(?i)((?:x-goog-api-key|api[_-]?key|key)\\s*[=:]\\s*)[^\\s&,\\\"]+"),
                "$1[REDACTED]"
            )
    }
}
