package com.sflightx.app

import android.app.Application
import android.content.Intent
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val crashLog = StringBuilder()

            crashLog.append("❗ Crash detected in thread: ${thread.name}\n\n")
            crashLog.append(Log.getStackTraceString(throwable)).append("\n\n")

            try {
                // Include logcat (optional)
                val process = ProcessBuilder("logcat", "-d", "-t", "100")
                    .redirectErrorStream(true)
                    .start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    crashLog.append(line).append("\n")
                }
                reader.close()
            } catch (e: Exception) {
                crashLog.append("[Failed to get logcat: ${e.message}]")
            }

            // Save crash log locally
            val fileName = "crash_${System.currentTimeMillis()}.txt"
            val file = File(filesDir, fileName)
            file.writeText(crashLog.toString())

            // Start ErrorActivity from a new thread
            Thread {
                val intent = Intent(this, ErrorActivity::class.java).apply {
                    putExtra("error", crashLog.toString())
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }.start()

            // Give ErrorActivity time to start
            Thread.sleep(1000)

            // Kill the process to prevent restart
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(1)
        }

    }
}
