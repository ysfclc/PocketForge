package com.pocketforge.app
import android.app.Activity
import android.os.Bundle
import android.widget.TextView
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val status = runCatching {
            System.loadLibrary("pocketforge_ffi")
            "Native core · ABI ${nativeAbiVersion()} · health ${nativeHealthCheck()}"
        }.getOrElse { "Native core unavailable · ${it.javaClass.simpleName}" }
        setContentView(TextView(this).apply {
            text = "PocketForge\nKeystone foundation\n\nNo product features included.\n$status"
            textSize = 18f
            setPadding(32, 48, 32, 32)
        })
    }
    private external fun nativeAbiVersion(): Int
    private external fun nativeHealthCheck(): Int
}
