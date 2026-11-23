package com.pocketforge.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Before
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RuntimeProcessProbeInstrumentedTest {
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val prefs get() = context.getSharedPreferences(ProcessExecutionProbeService.PREFS, 0)
    private var activity: Activity? = null

    @Before
    fun launchPocketForge() {
        activity = instrumentation.startActivitySync(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @After
    fun closePocketForge() {
        activity?.finish()
        activity = null
    }

    @Test
    fun childArtRuntimeAndNativeProcessComplete() {
        grantNotificationPermission()
        context.startForegroundService(Intent(context, ProcessExecutionProbeService::class.java))

        awaitState(ProcessExecutionProbeService.STATE_SUCCEEDED, 60_000)
        val output = prefs.getString(ProcessExecutionProbeService.KEY_OUTPUT, "").orEmpty()
        assertTrue("ART success marker missing: $output", output.contains("POCKETFORGE_ART_RUNTIME_OK"))
        assertTrue("ART VM name missing: $output", output.contains("java.vm.name="))
        assertTrue("ART VM version missing: $output", output.contains("java.vm.version="))
        assertTrue("Native process identity missing: $output", output.contains("uid="))
        assertEquals(0, prefs.getInt(ProcessExecutionProbeService.KEY_EXIT, Int.MIN_VALUE))
    }

    @Test
    fun notificationActionCancelsTheChildProcess() {
        grantNotificationPermission()
        context.startForegroundService(Intent(context, ProcessExecutionProbeService::class.java))
        awaitState(ProcessExecutionProbeService.STATE_RUNNING, 5_000)
        Thread.sleep(1_000)
        context.startService(
            Intent(context, ProcessExecutionProbeService::class.java)
                .setAction("com.pocketforge.app.action.CANCEL_PROCESS_PROBE")
        )
        awaitState(ProcessExecutionProbeService.STATE_CANCELLED, 5_000)
        assertEquals(ProcessExecutionProbeService.STATE_CANCELLED,
            prefs.getString(ProcessExecutionProbeService.KEY_STATE, null))
    }

    private fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            instrumentation.uiAutomation.grantRuntimePermission(
                context.packageName, Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun awaitState(expected: String, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val actual = prefs.getString(ProcessExecutionProbeService.KEY_STATE, ProcessExecutionProbeService.STATE_IDLE)
            if (actual == expected) return
            if (actual == ProcessExecutionProbeService.STATE_FAILED || actual == ProcessExecutionProbeService.STATE_INTERRUPTED) {
                throw AssertionError("Probe ended as $actual: " + prefs.getString(ProcessExecutionProbeService.KEY_OUTPUT, ""))
            }
            Thread.sleep(200)
        }
        throw AssertionError("Timed out waiting for $expected; state=" +
            prefs.getString(ProcessExecutionProbeService.KEY_STATE, "missing") + "; output=" +
            prefs.getString(ProcessExecutionProbeService.KEY_OUTPUT, ""))
    }
}
