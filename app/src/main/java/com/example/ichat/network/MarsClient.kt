package com.example.ichat.network

import com.tencent.mars.stn.StnLogic

object MarsCgi {
    const val CMD_SEND_TEXT = 1001

    fun buildTextMessage(text: String): ByteArray {
        return text.toByteArray()
    }

    fun newShortTask(cmdId: Int, shortHost: String): StnLogic.Task {
        return StnLogic.Task(
            StnLogic.Task.EShort,
            cmdId,
            "/mars/sendText",
            arrayListOf("10.0.2.2")
        ).apply {
            channelSelect = StnLogic.Task.EShort
            clientSequenceId = StnLogic.genTaskID()
            needRealtimeNetInfo = false
            headers["Content-Type"] = "application/octet-stream"
            needAuthed = false
            headers["Connection"] = "close"
        }
    }
}
