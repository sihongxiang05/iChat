package com.example.ichat.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ichat.friend.FriendViewModel
import com.example.ichat.chat.formatTs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ChatScreen(vm: FriendViewModel, peer: String, onBack: () -> Unit) {
    val messages by vm.conversation(peer).collectAsState(initial = emptyList())
    val input = remember { mutableStateOf("") }
    val ctx = LocalContext.current
    val me = remember { mutableStateOf("user") }
    LaunchedEffect(Unit) {
        val p = ctx.getSharedPreferences("auth", android.content.Context.MODE_PRIVATE)
        me.value = p.getString("user_id", "user") ?: "user"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "与 $peer 的对话") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } }
            )
        },
        bottomBar = {
            androidx.compose.foundation.layout.Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = input.value,
                    onValueChange = { input.value = it },
                    label = { Text("消息内容") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val msg = input.value.trim()
                    if (msg.isNotEmpty()) {
                        vm.sendMessage(peer, msg)
                        input.value = ""
                    }
                }, modifier = Modifier.padding(start = 8.dp)) { Text("发送") }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp, vertical = 8.dp)) {
            items(messages) { m ->
                val isMine = m.sender == me.value
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxWidth()) {
                        val boxAlign = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                        androidx.compose.foundation.layout.Box(modifier = Modifier.align(boxAlign)) {
                            Surface(shape = RoundedCornerShape(16.dp), color = bubbleColor) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                    val state = when (m.status) {
                                        "pending" -> "待发送"
                                        "accepted" -> "已接收"
                                        "delivered" -> "已投递"
                                        "read" -> "已读"
                                        else -> m.status
                                    }
                                    Text(text = m.content, color = MaterialTheme.colorScheme.onPrimaryContainer.takeIf { isMine } ?: MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "${formatTs(m.ts)} · $state", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
