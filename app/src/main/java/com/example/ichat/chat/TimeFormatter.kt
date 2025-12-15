package com.example.ichat.chat

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTs(ts: Long): String {
    val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return df.format(Date(ts))
}
