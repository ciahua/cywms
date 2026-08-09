package com.chenyang.cywms.data.model

data class ApiResult<T>(
    val success: Boolean = false,
    val message: String? = null,
    val code: Int? = null,
    val result: T? = null
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class LoginResult(
    val token: String? = null,
    val userInfo: UserInfo? = null
)

data class UserInfo(
    val username: String? = null,
    val realname: String? = null
)
