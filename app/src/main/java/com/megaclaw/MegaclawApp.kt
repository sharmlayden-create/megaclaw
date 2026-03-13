package com.megaclaw

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class MegaclawApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupCrashHandler()
    }

    private fun setupCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val file = File(getExternalFilesDir(null), "crash_$timestamp.txt")
                PrintWriter(file).use { pw ->
                    pw.println("=== Megaclaw Crash Report ===")
                    pw.println("Time: $timestamp")
                    pw.println("Thread: ${thread.name}")
                    pw.println()
                    throwable.printStackTrace(pw)
                }
                Log.e("MegaclawCrash", "Crash log saved to: ${file.absolutePath}", throwable)
            } catch (_: Exception) {
                // ignore
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
