package com.example.ichat

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ichat.mars.MarsRuntime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarsRuntimeInstrumentedTest {
    @Test
    fun tryLoadAndInit_sets_available_flag() {
        val ctx: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val ok = MarsRuntime.tryLoadAndInit(ctx)
        if (ok) assertTrue(MarsRuntime.available) else assertFalse(MarsRuntime.available)
    }
}
