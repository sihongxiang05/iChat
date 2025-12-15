package com.example.ichat.network

import android.util.Log
import com.example.ichat.chat.ChatMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.charset.Charset
import java.util.concurrent.Executors

object RawTcpClient {
    private var socket: Socket? = null
    private var out: DataOutputStream? = null
    private var inS: DataInputStream? = null
    private val charset = Charset.forName("UTF-8")
    private val io = Executors.newSingleThreadExecutor()
    private var readerThread: Thread? = null
    @Volatile private var connecting = false
    @Volatile private var closing = false
    private val _incoming = MutableSharedFlow<ChatMessage>(extraBufferCapacity = 64)
    val incoming: SharedFlow<ChatMessage> = _incoming
    private val _acks = MutableSharedFlow<Pair<String,String>>(extraBufferCapacity = 64)
    val acks: SharedFlow<Pair<String,String>> = _acks

    fun connect(userId: String, host: String, port: Int) {
        if (socket?.isConnected == true || connecting) return
        connecting = true
        Thread {
            try {
                val s = Socket()
                s.tcpNoDelay = true
                s.connect(InetSocketAddress(host, port), 3000)
                socket = s
                out = DataOutputStream(s.getOutputStream())
                inS = DataInputStream(s.getInputStream())
                sendFrame(1, userId)
                startReader()
                Log.i("TCP", "connected to $host:$port as $userId")
            } catch (e: Exception) {
                Log.e("TCP", "connect failed", e)
                cleanup()
            } finally {
                connecting = false
            }
        }.start()
    }

    fun sendChat(from: String, to: String, content: String) {
        val id = java.util.UUID.randomUUID().toString()
        val ts = System.currentTimeMillis()
        sendChat(from, to, content, id, ts)
    }

    fun sendChat(from: String, to: String, content: String, id: String, ts: Long) {
        val json = "{" +
                "\"id\":\"$id\"," +
                "\"from_user\":\"$from\"," +
                "\"to_user\":\"$to\"," +
                "\"content\":\"$content\"," +
                "\"ts\":$ts}"
        sendFrame(2, json)
    }

    private fun sendFrame(type: Int, payload: String) {
        io.execute {
            try {
                val o = out ?: return@execute
                val bytes = payload.toByteArray(charset)
                o.writeByte(type)
                o.writeInt(bytes.size)
                o.write(bytes)
                o.flush()
                Log.i("TCP", "frame type=$type len=${bytes.size}")
            } catch (e: Exception) {
                Log.e("TCP", "sendFrame failed", e)
            }
        }
    }

    private fun startReader() {
        val existing = readerThread
        if (existing != null && existing.isAlive) return
        val t = Thread {
            try {
                val ins = inS ?: return@Thread
                while (!Thread.currentThread().isInterrupted) {
                    val type = ins.readUnsignedByte()
                    val len = ins.readInt()
                    val buf = ByteArray(len)
                    ins.readFully(buf)
                    val body = String(buf, charset)
                    Log.i("TCP", "recv type=$type len=$len body=$body")
                    if (type == 2 || type == 5) {
                        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                        val from = Regex("\"from_user\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                        val to = Regex("\"to_user\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                        val content = Regex("\"content\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1) ?: body
                        val tsMatch = Regex("\"ts\"\\s*:\\s*(\\d+)").find(body)?.groupValues?.get(1)
                        val ts = tsMatch?.toLongOrNull() ?: System.currentTimeMillis()
                        _incoming.tryEmit(ChatMessage(id = id, from = from, to = to, content = content, ts = ts))
                    } else if (type == 3 || type == 4) {
                        val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                        val status = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"").find(body)?.groupValues?.get(1) ?: ""
                        _acks.tryEmit(id to status)
                    }
                }
            } catch (e: Exception) {
                if (closing || (e is java.net.SocketException && e.message?.contains("Socket closed") == true)) {
                    Log.i("TCP", "reader stopped")
                } else {
                    Log.e("TCP", "reader failed", e)
                }
            } finally {
                cleanup()
            }
        }
        readerThread = t
        t.start()
    }

    fun disconnect() {
        closing = true
        try {
            readerThread?.interrupt()
        } catch (_: Throwable) {}
        cleanup()
    }

    private fun cleanup() {
        try { inS?.close() } catch (_: Throwable) {}
        try { out?.close() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        inS = null
        out = null
        socket = null
        readerThread = null
        closing = false
    }
}
