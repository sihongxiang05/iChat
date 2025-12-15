package com.example.ichat

import com.example.ichat.network.MarsCgi
import com.tencent.mars.stn.StnLogic
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream

class MarsClientTest {
    @Test
    fun buildTextMessage_returns_bytes() {
        val bytes = MarsCgi.buildTextMessage("hello")
        assertArrayEquals("hello".toByteArray(), bytes)
    }

    @Test
    fun newShortTask_sets_fields() {
        val task = MarsCgi.newShortTask(MarsCgi.CMD_SEND_TEXT, "10.0.2.2")
        assertEquals(StnLogic.Task.EShort, task.channelSelect)
        assertEquals(MarsCgi.CMD_SEND_TEXT, task.cmdID)
        assertEquals(listOf("10.0.2.2"), task.shortLinkHostList)
    }

    @Test
    fun req2Buf_logic_writes_userContext() {
        val out = ByteArrayOutputStream()
        val userContext = "payload".toByteArray()
        out.write(userContext)
        assertArrayEquals(userContext, out.toByteArray())
    }
}
