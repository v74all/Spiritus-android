package com.v7lthronyx.v7lpanel.data.repository

import com.v7lthronyx.v7lpanel.data.api.V7LApiClient

class SubscriptionRepository(private val api: V7LApiClient) {

    /** Single round-trip: config list + user metadata from one /sub-api call. */
    suspend fun getSubBundle(uuid: String): Result<V7LApiClient.SubBundle> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { api.getSubBundle(uuid) }
        }
}
