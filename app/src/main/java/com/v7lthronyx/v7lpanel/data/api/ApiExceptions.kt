package com.v7lthronyx.v7lpanel.data.api

/** Thrown when login credentials are rejected (HTTP 401). */
class AuthException(
    message: String,
    val remaining: Int? = null
) : Exception(message)

/** Thrown when client is rate-limited / locked out (HTTP 429). */
class RateLimitException(message: String) : Exception(message)

/** General API error with HTTP status info. */
class ApiException(message: String, val code: Int = 0) : Exception(message)
