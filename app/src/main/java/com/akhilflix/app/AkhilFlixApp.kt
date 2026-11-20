package com.akhilflix.app

import android.app.Application
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Host Application. Installs a crash reporter first thing: if the app ever
 * crashes, the full stack trace is saved to filesDir/crash-report.txt (and
 * to external files dir when available), so MainActivity can show it on the
 * next launch instead of closing silently again.
 */
class AkhilFlixApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val report = buildString {
                    appendLine("AkhilFlix crash report - $stamp")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    appendLine(throwable.stackTraceToString())
                    var cause = throwable.cause
                    while (cause != null) {
                        appendLine("Caused by:")
                        appendLine(cause.stackTraceToString())
                        cause = cause.cause
                    }
                }
                File(filesDir, "crash-report.txt").writeText(report)
                try {
                    getExternalFilesDir(null)?.let { dir ->
                        File(dir, "crash-report.txt").writeText(report)
                    }
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }
}
