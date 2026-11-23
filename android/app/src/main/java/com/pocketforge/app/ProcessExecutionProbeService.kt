package com.pocketforge.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/** Debug/preview feasibility spike. Fixed harmless binaries only; no caller-supplied commands. */
class ProcessExecutionProbeService : Service() {
    private val cancelled = AtomicBoolean(false)
    @Volatile private var child: Process? = null
    @Volatile private var worker: Thread? = null
    @Volatile private var interruptionRequested = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        isActive = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            cancelProbe()
            return START_NOT_STICKY
        }
        if (worker?.isAlive == true) return START_NOT_STICKY
        cancelled.set(false)
        interruptionRequested = false
        persist(STATE_RUNNING, "Starting Android ART and child-process probes…", null)
        val notification = buildNotification("Launching harmless system process")
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        worker = Thread({ runProbe() }, "PocketForge-ProcessProbe").also { it.start() }
        return START_NOT_STICKY
    }

    private fun runProbe() {
        try {
            val runtime = runArtRuntimeProbe()
            if (cancelled.get()) return
            val identity = runFixedCommand("/system/bin/id")
            if (cancelled.get()) return
            if (identity.exitCode != 0) error("System identity process failed with exit " + identity.exitCode)
            val wait = startFixedSleep()
            val exit = wait.waitFor()
            child = null
            if (interruptionRequested) {
                persist(STATE_INTERRUPTED, "Android stopped the process probe.", null)
            } else if (cancelled.get()) {
                persist(STATE_CANCELLED, runtime.output + "\\n" + identity.output, exit)
            } else if (exit == 0) {
                persist(STATE_SUCCEEDED, identity.output + "\nForeground process survived the 30-second wait.", exit)
            } else {
                persist(STATE_FAILED, identity.output + "\nSleep process exited with " + exit, exit)
            }
        } catch (error: Throwable) {
            if (interruptionRequested) persist(STATE_INTERRUPTED, "Android stopped the process probe.", null)
            else if (cancelled.get()) persist(STATE_CANCELLED, error.message ?: "Cancelled", null)
            else persist(STATE_FAILED, error.message ?: error.javaClass.simpleName, null)
        } finally {
            child?.destroy()
            child = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private data class CommandResult(val output: String, val exitCode: Int)

    private fun runArtRuntimeProbe(): CommandResult {
        val outputFile = File(cacheDir, "art-runtime-probe.txt")
        if (outputFile.exists()) outputFile.delete()
        val process = ProcessBuilder(
            "/system/bin/app_process",
            "/system/bin",
            "com.pocketforge.app.RuntimeProcessProbe"
        ).redirectErrorStream(true).redirectOutput(outputFile).also {
            it.environment()["CLASSPATH"] = applicationInfo.sourceDir
        }.start()
        child = process
        val completed = process.waitFor(10, TimeUnit.SECONDS)
        if (!completed) {
            process.destroyForcibly()
            process.waitFor(2, TimeUnit.SECONDS)
            child = null
            outputFile.delete()
            error("Android ART child runtime did not exit within 10 seconds.")
        }
        child = null
        val output = if (outputFile.isFile) {
            outputFile.inputStream().use { it.readBytes().toString(Charsets.UTF_8).take(2048) }
        } else ""
        outputFile.delete()
        check(process.exitValue() == 0) { "Android ART child runtime exited with " + process.exitValue() + ": " + output }
        check(output.contains("POCKETFORGE_ART_RUNTIME_OK") && output.contains("java.vm.name=")) {
            "Android ART child runtime returned an unexpected result: " + output
        }
        return CommandResult(output.trim(), process.exitValue())
    }

    private fun runFixedCommand(path: String): CommandResult {
        check(path == "/system/bin/id") { "Probe command is not allow-listed." }
        val process = ProcessBuilder(path).redirectErrorStream(true).start()
        child = process
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exit = process.waitFor()
        child = null
        return CommandResult(output.trim().take(2048), exit)
    }

    private fun startFixedSleep(): Process {
        val process = ProcessBuilder("/system/bin/sleep", "30").redirectErrorStream(true).start()
        child = process
        if (cancelled.get() || interruptionRequested) process.destroyForcibly()
        return process
    }

    private fun cancelProbe() {
        cancelled.set(true)
        child?.let { process ->
            process.destroy()
            if (process.isAlive) process.destroyForcibly()
        }
        persist(STATE_CANCELLED, "Cancelled by the user.", null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun persist(state: String, output: String, exitCode: Int?) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putString(KEY_STATE, state)
            .putString(KEY_OUTPUT, output.take(4096))
            .putLong(KEY_UPDATED, System.currentTimeMillis())
            .putInt(KEY_EXIT, exitCode ?: Int.MIN_VALUE)
            .commit()
        if (state == STATE_RUNNING) {
            getSystemService(NotificationManager::class.java).notify(
                NOTIFICATION_ID, buildNotification("Probe running · tap Cancel to stop")
            )
        }
    }

    private fun buildNotification(text: String): Notification {
        val cancelIntent = Intent(this, ProcessExecutionProbeService::class.java).setAction(ACTION_CANCEL)
        val cancel = PendingIntent.getService(
            this, 1, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("PocketForge process probe")
            .setContentText(text)
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Cancel", cancel).build())
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(CHANNEL_ID, "Process probe", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        if (getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_STATE, "") == STATE_RUNNING) {
            interruptionRequested = true
            cancelled.set(true)
            child?.destroyForcibly()
            persist(STATE_INTERRUPTED, "Android stopped the probe before it completed.", null)
        }
        isActive = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val PREFS = "pocketforge.process-probe.v1"
        const val KEY_STATE = "state"
        const val KEY_OUTPUT = "output"
        const val KEY_UPDATED = "updatedAt"
        const val KEY_EXIT = "exitCode"
        const val STATE_IDLE = "idle"
        const val STATE_RUNNING = "running"
        const val STATE_SUCCEEDED = "succeeded"
        const val STATE_FAILED = "failed"
        const val STATE_CANCELLED = "cancelled"
        const val STATE_INTERRUPTED = "interrupted"
        private const val ACTION_CANCEL = "com.pocketforge.app.action.CANCEL_PROCESS_PROBE"
        private const val CHANNEL_ID = "pocketforge-process-probe"
        private const val NOTIFICATION_ID = 7314
        @Volatile var isActive: Boolean = false
            private set
    }
}
