package com.example.ichat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ichat.ui.theme.IChatTheme
import com.example.ichat.mars.MarsRuntime
import com.tencent.mars.stn.StnLogic
import com.example.ichat.network.MarsCgi
import com.example.ichat.network.RawTcpClient
import com.example.ichat.auth.LoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val loginVm = com.example.ichat.auth.LoginViewModel(application)
        val friendVm = com.example.ichat.friend.FriendViewModel(application)
        setContent {
            IChatTheme {
                val loggedIn = remember { mutableStateOf(false) }
                val chatPeer = remember { mutableStateOf<String?>(null) }
                if (!loggedIn.value) {
                    LoginScreen(vm = loginVm) { loggedIn.value = true }
                } else {
                    if (chatPeer.value == null) {
                        com.example.ichat.friend.FriendScreen(vm = friendVm, onLogout = {
                            loginVm.logout()
                            loggedIn.value = false
                            chatPeer.value = null
                        }, onOpenChat = { peer -> chatPeer.value = peer })
                    } else {
                        com.example.ichat.chat.ChatScreen(vm = friendVm, peer = chatPeer.value!!, onBack = { chatPeer.value = null })
                    }
                }
            }
        }

        // 在 Activity 创建时，我们假设用户“登录”成功，并启动 Mars
        startMars()
    }

    override fun onResume() {
        super.onResume()
        MarsRuntime.onForeground(true)
    }

    override fun onPause() {
        super.onPause()
        MarsRuntime.onForeground(false)
        com.example.ichat.network.RawTcpClient.disconnect()
    }

    private fun startMars() { }
}

@Composable
fun MarsTestScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = {
            probeConnectivity()
            sendRawTcpEcho("Hello TCP Echo")
            sendTestMessage()
        }) {
            Text("发送 Mars 测试消息")
        }
    }
}

private fun sendTestMessage() {
    if (!com.example.ichat.mars.MarsRuntime.available) {
        android.util.Log.w("Mars", "Native libraries unavailable, ignore send")
        return
    }
    // 创建一个 STN 任务
    val task = MarsCgi.newShortTask(MarsCgi.CMD_SEND_TEXT, "10.0.2.2")

    // 设置任务参数
    task.cmdID = MarsCgi.CMD_SEND_TEXT

    // 构造请求体 (通常使用 protobuf 序列化，这里用简单字符串代替)
    val requestBody = MarsCgi.buildTextMessage("Hello from Mars Client!")
    task.userContext = requestBody

    try {
        StnLogic.startTask(task)
    } catch (e: UnsatisfiedLinkError) {
        android.util.Log.e("Mars", "startTask failed", e)
    }

    RawTcpClient.sendChat("userA", "userB", "Hello via TCP Router")
}

private fun probeConnectivity() {
    Thread {
        try {
            val ip = "10.0.2.2"
            val ports = listOf(8080, 8081)
            for (p in ports) {
                try {
                    java.net.Socket().use { s ->
                        s.connect(java.net.InetSocketAddress(ip, p), 3000)
                        android.util.Log.i("NetProbe", "TCP connected to $ip:$p")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("NetProbe", "TCP connect failed to $ip:$p", e)
                }
            }
            try {
                val url = java.net.URL("http://$ip:8080/")
                (url.openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 3000
                    readTimeout = 3000
                    requestMethod = "GET"
                    val code = try { responseCode } catch (e: Exception) { -1 }
                    android.util.Log.i("NetProbe", "HTTP GET $ip:8080 status=$code")
                    disconnect()
                }
            } catch (e: Exception) {
                android.util.Log.w("NetProbe", "HTTP GET failed", e)
            }
        } catch (t: Throwable) {
            android.util.Log.e("NetProbe", "probe error", t)
        }
    }.start()
}

private fun sendRawTcpEcho(msg: String) {
    Thread {
        val ip = "10.0.2.2"
        val port = 8081
        try {
            java.net.Socket().use { s ->
                s.connect(java.net.InetSocketAddress(ip, port), 3000)
                val out = s.getOutputStream()
                val inS = s.getInputStream()
                val payload = msg.toByteArray()
                val t0 = System.nanoTime()
                out.write(payload)
                out.flush()
                val buf = ByteArray(payload.size)
                var read = 0
                while (read < buf.size) {
                    val r = inS.read(buf, read, buf.size - read)
                    if (r <= 0) break
                    read += r
                }
                val t1 = System.nanoTime()
                android.util.Log.i("TCP", "echo len=$read rtt=${(t1-t0)/1_000_000}ms body=${String(buf,0,read)}")
            }
        } catch (e: Exception) {
            android.util.Log.e("TCP", "raw echo failed", e)
        }
    }.start()
}
