package com.example.ichat.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(vm: LoginViewModel, onSuccess: () -> Unit) {
    val ctx = LocalContext.current
    val username = remember { mutableStateOf("") }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val token by vm.token.collectAsState()
    val loading by vm.loading.collectAsState()
    val err by vm.error.collectAsState()
    val server = remember { mutableStateOf(loadServer(ctx)) }

    LaunchedEffect(token) {
        if (token != null) onSuccess()
    }

    Column(modifier = Modifier.padding(16.dp)) {
        OutlinedTextField(value = server.value, onValueChange = { server.value = it }, label = { Text("服务器地址") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = username.value, onValueChange = { username.value = it }, label = { Text("用户名") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = email.value, onValueChange = { email.value = it }, label = { Text("邮箱") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = password.value, onValueChange = { password.value = it }, label = { Text("密码") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val u = if (username.value.isNotBlank()) username.value else null
            val e = if (email.value.isNotBlank()) email.value else null
            val base = normalizeBase(server.value)
            saveServer(ctx, base)
            com.example.ichat.auth.AuthApi.base = base
            vm.login(u, e, password.value)
        }, enabled = !loading && (username.value.isNotBlank() || email.value.isNotBlank()) && password.value.isNotBlank() && server.value.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("登录") }
        Button(onClick = {
            val base = normalizeBase(server.value)
            saveServer(ctx, base)
            com.example.ichat.auth.AuthApi.base = base
            vm.register(username.value, email.value, password.value)
        }, enabled = !loading && username.value.isNotBlank() && email.value.isNotBlank() && password.value.isNotBlank() && server.value.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("注册") }
        Button(onClick = {
            val base = normalizeBase(server.value)
            saveServer(ctx, base)
            com.example.ichat.auth.AuthApi.base = base
            probe(base)
        }, enabled = !loading && server.value.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("测试连接") }
        if (loading) {
            CircularProgressIndicator()
        }
        if (err != null) {
            Text(text = err!!)
        }
    }
}

private fun loadServer(ctx: Context): String {
    val p = ctx.getSharedPreferences("auth", Context.MODE_PRIVATE)
    return p.getString("server_base", "http://10.0.2.2:8082") ?: "http://10.0.2.2:8082"
}

private fun saveServer(ctx: Context, v: String) {
    val p = ctx.getSharedPreferences("auth", Context.MODE_PRIVATE)
    p.edit().putString("server_base", v).apply()
}

private fun normalizeBase(v: String): String {
    return if (v.startsWith("http://") || v.startsWith("https://")) v else "http://$v"
}

private fun probe(base: String) {
    Thread {
        try {
            val u = URL(base)
            val c = (u.openConnection() as HttpURLConnection)
            c.connectTimeout = 2000
            c.readTimeout = 2000
            c.requestMethod = "GET"
            val code = try { c.responseCode } catch (_: Exception) { -1 }
            c.disconnect()
            android.util.Log.i("AuthProbe", "GET $base status=$code")
        } catch (e: Exception) {
            android.util.Log.e("AuthProbe", "probe failed $base", e)
        }
    }.start()
}
