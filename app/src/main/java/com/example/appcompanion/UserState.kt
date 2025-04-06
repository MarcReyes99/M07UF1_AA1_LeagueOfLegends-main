package com.example.appcompanion

data class UserState(
    val isLoggedIn: Boolean = false,
    val isGuest: Boolean = false,
    val username: String? = null
)
