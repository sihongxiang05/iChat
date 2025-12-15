package com.example.ichat.auth

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(app: Application) : AndroidViewModel(app) {
    private val _token = MutableStateFlow<String?>(null)
    val token: StateFlow<String?> = _token
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val prefs = app.getSharedPreferences("auth", Context.MODE_PRIVATE)

    fun login(username: String?, email: String?, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null
            val t = AuthApi.login(username, email, password)
            if (t != null) {
                val uid = username ?: email ?: "user"
                prefs.edit().putString("token", t).putString("user_id", uid).apply()
                _token.value = t
            } else {
                _token.value = null
                _error.value = "登录失败，无法连接或凭据无效"
            }
            _loading.value = false
        }
    }

    fun register(username: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loading.value = true
            _error.value = null
            val t = AuthApi.register(username, email, password)
            if (t != null) {
                val uid = username
                prefs.edit().putString("token", t).putString("user_id", uid).apply()
                _token.value = t
            } else {
                _token.value = null
                _error.value = "注册失败，无法连接或信息无效"
            }
            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.edit().remove("token").remove("user_id").apply()
            _error.value = null
            _loading.value = false
            _token.value = null
        }
    }
}
