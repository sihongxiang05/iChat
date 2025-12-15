package com.example.ichat.mars

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.tencent.mars.BaseEvent
import com.tencent.mars.Mars

object MarsRuntime {
    @Volatile
    var available: Boolean = false
        private set

    fun tryLoadAndInit(context: Context): Boolean {
        return try {
            Mars.loadDefaultMarsLibrary()
            Mars.init(context, Handler(Looper.getMainLooper()))
            Mars.onCreate(true)
            available = true
            true
        } catch (t: Throwable) {
            Log.e("MarsRuntime", "init failed", t)
            available = false
            false
        }
    }

    fun onForeground(foreground: Boolean) {
        if (!available) return
        try {
            BaseEvent.onForeground(foreground)
        } catch (t: Throwable) {
            Log.e("MarsRuntime", "onForeground failed", t)
        }
    }
}
