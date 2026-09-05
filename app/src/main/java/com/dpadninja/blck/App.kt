package com.dpadninja.blck

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

object AppContext {
    private var ref: WeakReference<Context>? = null

    fun init(app: Application) {
        ref = WeakReference(app.applicationContext)
    }

}

fun ensureServiceRunning(context: Context) {
    val intent = Intent(context, BlackoutService::class.java)
    runCatching { ContextCompat.startForegroundService(context, intent) }
        .onFailure { e ->
            Log.w("blck", "startForegroundService failed: $e")
            runCatching { context.startService(intent) }
        }
}

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AppContext.init(this)
        AppSettings.init(this)
        ensureServiceRunning(this)
    }
}
