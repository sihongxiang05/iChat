package com.example.ichat.friend

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.example.ichat.chat.ChatMessage
import com.example.ichat.chat.ChatMessageEntity

@Composable
fun FriendScreen(vm: FriendViewModel, onLogout: () -> Unit, onOpenChat: (String) -> Unit) {
    val friends by vm.friends.collectAsState()
    val messages by vm.messages.collectAsState()
    val target = remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val message = remember { mutableStateOf("") }
    val uidState = remember { mutableStateOf("user") }

    LaunchedEffect(Unit) {
        vm.loadFriends()
        val p = ctx.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        val uid = p.getString("user_id", "user") ?: "user"
        uidState.value = uid
        vm.setUser(uid)
        com.example.ichat.network.RawTcpClient.connect(uid, "10.0.2.2", 8081)
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "当前用户：${uidState.value}")
        OutlinedTextField(
            value = target.value,
            onValueChange = { target.value = it },
            label = { Text("添加好友用户名或ID") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            val t = target.value.trim()
            if (t.isNotEmpty()) vm.addFriend(t)
            target.value = ""
        }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("添加好友") }

        LazyColumn(modifier = Modifier.padding(top = 16.dp)) {
            items(friends) { f ->
                Button(onClick = { onOpenChat(f.username) }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Text(text = if (f.displayName != null && f.displayName.isNotEmpty()) "${f.displayName} (${f.username})" else f.username)
                }
            }
        }

        

        Button(onClick = {
            val p = ctx.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
            p.edit().remove("token").remove("user_id").apply()
            com.example.ichat.network.RawTcpClient.disconnect()
            onLogout()
        }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("退出登录") }
    }
}
