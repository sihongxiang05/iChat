package com.example.ichat.auth

import java.net.HttpURLConnection
import java.net.URL

object AuthApi {
    @Volatile var base = "http://10.0.2.2:8082"

    fun login(username: String?, email: String?, password: String): String? {
        return try {
            val u = URL("$base/api/auth/login")
            val c = (u.openConnection() as HttpURLConnection)
            c.connectTimeout = 3000
            c.readTimeout = 3000
            c.requestMethod = "POST"
            c.setRequestProperty("Content-Type", "application/json")
            c.doOutput = true
            val payload = if (username != null) "{\"username\":\"$username\",\"password\":\"$password\"}" else "{\"email\":\"$email\",\"password\":\"$password\"}"
            c.outputStream.use { it.write(payload.toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream).readBytes().toString(Charsets.UTF_8)
            c.disconnect()
            if (code == 200) Regex("\"token\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) else null
        } catch (_: Exception) {
            null
        }
    }

    fun register(username: String, email: String, password: String): String? {
        return try {
            val u = URL("$base/api/auth/register")
            val c = (u.openConnection() as HttpURLConnection)
            c.connectTimeout = 3000
            c.readTimeout = 3000
            c.requestMethod = "POST"
            c.setRequestProperty("Content-Type", "application/json")
            c.doOutput = true
            val payload = "{\"username\":\"$username\",\"email\":\"$email\",\"password\":\"$password\"}"
            c.outputStream.use { it.write(payload.toByteArray()) }
            val code = c.responseCode
            val body = (if (code in 200..299) c.inputStream else c.errorStream).readBytes().toString(Charsets.UTF_8)
            c.disconnect()
            if (code == 200) Regex("\"token\":\"([^\"]+)\"").find(body)?.groupValues?.get(1) else null
        } catch (_: Exception) {
            null
        }
    }
}
