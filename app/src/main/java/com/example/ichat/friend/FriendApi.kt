package com.example.ichat.friend

import java.net.HttpURLConnection
import java.net.URL

object FriendApi {
    private const val base = "http://10.0.2.2:8082"

    fun listFriends(token: String): List<Friend> {
        val u = URL("$base/api/friends")
        val c = (u.openConnection() as HttpURLConnection)
        c.requestMethod = "GET"
        c.setRequestProperty("Authorization", "Bearer $token")
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream).readBytes().toString(Charsets.UTF_8)
        c.disconnect()
        return if (code == 200) parseFriends(body) else emptyList()
    }

    fun addFriend(token: String, target: String): Boolean {
        val u = URL("$base/api/friends/add")
        val c = (u.openConnection() as HttpURLConnection)
        c.requestMethod = "POST"
        c.setRequestProperty("Content-Type", "application/json")
        c.setRequestProperty("Authorization", "Bearer $token")
        c.doOutput = true
        val payload = "{\"target\":\"$target\"}"
        c.outputStream.use { it.write(payload.toByteArray()) }
        val code = c.responseCode
        val body = (if (code in 200..299) c.inputStream else c.errorStream).readBytes().toString(Charsets.UTF_8)
        c.disconnect()
        return code == 200
    }

    private fun parseFriends(json: String): List<Friend> {
        val items = mutableListOf<Friend>()
        val objRegex = Regex("\\{[^}]*\\}")
        objRegex.findAll(json).forEach { m ->
            val s = m.value
            val id = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"").find(s)?.groupValues?.get(1) ?: ""
            val username = Regex("\"username\"\\s*:\\s*\"([^\"]+)\"").find(s)?.groupValues?.get(1)
                ?: Regex("\"name\"\\s*:\\s*\"([^\"]+)\"").find(s)?.groupValues?.get(1)
                ?: ""
            val displayName = Regex("\"displayName\"\\s*:\\s*\"([^\"]*)\"").find(s)?.groupValues?.get(1)
            if (username.isNotEmpty()) items.add(Friend(id = id, username = username, displayName = displayName))
        }
        return items
    }
}
