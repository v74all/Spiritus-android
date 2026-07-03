package com.v7lthronyx.v7lpanel.data.api

import com.v7lthronyx.v7lpanel.data.api.models.*
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.BuildConfig
import com.v7lthronyx.v7lpanel.data.security.TlsPin
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.api.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.security.cert.CertificateException
import javax.net.ssl.*

class V7LApiClient(
    serverUrlRaw: String,
    private val certificatePinSha256: String? = null,
    initialAccessToken: String? = null
) {
    /** Base URL without trailing slash — avoids double slashes in Ktor paths. */
    val serverUrl: String = serverUrlRaw.trim().trimEnd('/')

    @Volatile private var accessToken: String? = initialAccessToken

    private val systemTrustManager: X509TrustManager = TrustManagerFactory
        .getInstance(TrustManagerFactory.getDefaultAlgorithm())
        .apply { init(null as KeyStore?) }
        .trustManagers.filterIsInstance<X509TrustManager>().single()

    private val pinningTrustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = systemTrustManager.acceptedIssuers
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) =
            systemTrustManager.checkClientTrusted(chain, authType)

        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
            val pin = certificatePinSha256?.trim().orEmpty()
            if (pin.isEmpty()) {
                systemTrustManager.checkServerTrusted(chain, authType)
                return
            }
            if (chain.isEmpty()) throw CertificateException("Server sent no certificate")
            chain[0].checkValidity()
            if (TlsPin.normalize(pin) == null || !TlsPin.matches(chain[0], pin)) {
                throw CertificateException("TLS certificate pin mismatch")
            }
        }
    }

    private fun buildSslContext(): SSLContext {
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(pinningTrustManager), null)
        }
    }

    // Shared cookie jar so the CSRF interceptor can read the csrf_token cookie
    // the panel sets and echo it back as a header (double-submit-cookie).
    private val cookieStorage = AcceptAllCookiesStorage()

    val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(HttpCookies) {
            storage = cookieStorage
        }
        // Double-submit-cookie CSRF: the panel rejects POST/PUT/PATCH/DELETE
        // (admin/agent writes) with 403 unless the X-CSRF-Token header matches
        // the csrf_token cookie. Mirror it automatically on every unsafe call.
        install(createClientPlugin("CsrfDoubleSubmit") {
            onRequest { request, _ ->
                when (request.method) {
                    HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch, HttpMethod.Delete -> {
                        val token = cookieStorage.get(request.url.build())
                            .firstOrNull { it.name == "csrf_token" }?.value
                        if (!token.isNullOrEmpty()) {
                            request.headers.remove("X-CSRF-Token")
                            request.headers.append("X-CSRF-Token", token)
                        }
                    }
                    else -> {}
                }
            }
        })
        install(HttpTimeout) {
            requestTimeoutMillis = 15_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis  = 15_000
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
        }
        HttpResponseValidator {
            validateResponse { response ->
                // Redirect to login on 401 outside of login/sub endpoints
                val path = response.request.url.encodedPath
                if (response.status == HttpStatusCode.Unauthorized &&
                    !path.contains("/api/login") && !path.contains("/api/agent/login") &&
                    !path.startsWith("/sub-")) {
                    SessionHolder.emitSessionExpired()
                }
            }
        }
        defaultRequest {
            url(serverUrl)
            contentType(ContentType.Application.Json)
            accessToken?.takeIf { it.isNotBlank() }?.let {
                header(HttpHeaders.Authorization, "Bearer $it")
            }
        }
        engine {
            config {
                if (!certificatePinSha256.isNullOrBlank()) {
                    val sslContext = buildSslContext()
                    sslSocketFactory(sslContext.socketFactory, pinningTrustManager)
                }
            }
        }
    }

    // ── Auth ──────────────────────────────────────────────────────────────

    fun setAccessToken(token: String?) {
        accessToken = token
    }

    suspend fun mobileLogin(role: String, username: String, password: String): MobileAuthDto =
        client.post("/api/mobile/auth/login") {
            setBody(mapOf("role" to role, "username" to username, "password" to password))
        }.body()

    suspend fun mobile2FA(challenge: String, code: String): MobileAuthDto =
        client.post("/api/mobile/auth/2fa") {
            setBody(mapOf("challenge" to challenge, "totp_code" to code))
        }.body()

    suspend fun validateMobileSession(): MobileSessionDto =
        client.get("/api/mobile/session").body()

    suspend fun mobileLogout() {
        client.post("/api/mobile/auth/logout")
    }

    suspend fun login(password: String): Result<Boolean> = runCatching {
        val resp: HttpResponse = client.post("/api/login") {
            setBody(mapOf("password" to password))
        }
        when (resp.status.value) {
            200  -> true
            401  -> {
                val body = resp.body<AuthDto>()
                throw AuthException(body.error ?: "Wrong password", body.remaining)
            }
            429  -> {
                val body = resp.body<AuthDto>()
                throw RateLimitException(body.error ?: "Too many attempts. Locked out.")
            }
            else -> throw ApiException("Login failed: ${resp.status}", resp.status.value)
        }
    }

    suspend fun logout() = client.post("/api/logout")

    suspend fun agentLogin(name: String, password: String): Result<Int> = runCatching {
        val resp: HttpResponse = client.post("/api/agent/login") {
            setBody(mapOf("name" to name, "password" to password))
        }
        when (resp.status.value) {
            200  -> resp.body<AuthDto>().agentId ?: 0
            401  -> {
                val body = resp.body<AuthDto>()
                throw AuthException(body.error ?: "Wrong credentials", body.remaining)
            }
            429  -> throw RateLimitException("Too many attempts")
            else -> throw ApiException("Agent login failed", resp.status.value)
        }
    }

    suspend fun agentLogout() = client.post("/api/agent/logout")

    // ── Users ─────────────────────────────────────────────────────────────

    suspend fun getUsers(): List<UserDto> =
        client.get("/api/users").body()

    suspend fun addUser(name: String, trafficGb: Double, days: Int): UserDto =
        client.post("/api/users") {
            setBody(mapOf("name" to name, "traffic" to trafficGb, "days" to days))
        }.body()

    suspend fun bulkAddUsers(
        prefix: String, count: Int, trafficGb: Double, days: Int,
        numbered: Boolean = true, start: Int = 1, pad: Int = 3
    ): Map<String, Any> = client.post("/api/bulk-users") {
        setBody(mapOf(
            "prefix" to prefix, "count" to count, "traffic" to trafficGb,
            "days" to days, "numbered" to numbered, "start" to start, "pad" to pad
        ))
    }.body()

    suspend fun deleteUser(name: String) =
        client.delete("/api/users/${name.encodeURLPath()}")

    suspend fun toggleUser(name: String) =
        client.post("/api/users/${name.encodeURLPath()}/toggle")

    suspend fun renewUser(name: String, trafficGb: Double, days: Int) =
        client.post("/api/users/${name.encodeURLPath()}/renew") {
            setBody(mapOf("traffic" to trafficGb, "days" to days))
        }

    suspend fun getUserActivity(name: String): ActivityDto =
        client.get("/api/users/${name.encodeURLPath()}/activity").body()

    suspend fun addTraffic(name: String, gb: Double) =
        client.post("/api/users/${name.encodeURLPath()}/add-traffic") {
            setBody(mapOf("gb" to gb))
        }

    suspend fun resetTraffic(name: String) =
        client.post("/api/users/${name.encodeURLPath()}/reset-traffic")

    suspend fun updateNote(name: String, note: String) =
        client.post("/api/users/${name.encodeURLPath()}/update-note") {
            setBody(mapOf("note" to note))
        }

    suspend fun setSpeedLimit(name: String, downKbps: Int, upKbps: Int) =
        client.post("/api/users/${name.encodeURLPath()}/speed-limit") {
            setBody(mapOf("speed_limit_down" to downKbps, "speed_limit_up" to upKbps))
        }

    suspend fun bulkDelete(names: List<String>) =
        client.post("/api/bulk-delete") { setBody(mapOf("names" to names)) }

    suspend fun bulkExportZip(names: List<String>): ByteArray =
        client.post("/api/bulk-export-zip") { setBody(mapOf("names" to names)) }.body()

    // ── Groups ────────────────────────────────────────────────────────────

    suspend fun getGroups(): List<GroupDto> =
        client.get("/api/groups").body()

    suspend fun getGroupUsers(groupId: String): List<UserDto> =
        client.get("/api/groups/${groupId.encodeURLPath()}/users").body()

    // ── Monitoring ───────────────────────────────────────────────────────

    suspend fun getLive(): Map<String, TrafficEntry> =
        client.get("/api/live").body<LiveDataDto>()

    suspend fun sync() = client.post("/api/sync")

    suspend fun getServerInfo(): ServerInfoDto {
        return try {
            val response = client.get("/api/server-info")
            if (response.status.isSuccess()) response.body() else ServerInfoDto()
        } catch (_: Exception) {
            ServerInfoDto()
        }
    }

    // ── Settings ─────────────────────────────────────────────────────────

    suspend fun getSettings(): Map<String, Any> =
        client.get("/api/settings").body()

    suspend fun updateSettings(data: Map<String, Any>) =
        client.post("/api/settings") { setBody(data) }

    suspend fun regenerateReality() = client.post("/api/settings/regenerate-reality")

    suspend fun generateSS2022Key(): String {
        val resp = client.post("/api/settings/generate-ss2022-key")
            .body<Map<String, String>>()
        return resp["ss2022_server_key"] ?: ""
    }

    suspend fun changePassword(current: String, newPassword: String): Map<String, String> =
        client.post("/api/change-password") {
            setBody(mapOf("current" to current, "new" to newPassword))
        }.body()

    // ── Agents ───────────────────────────────────────────────────────────

    suspend fun getAgents(): List<AgentDto> =
        client.get("/api/agents").body()

    suspend fun addAgent(name: String, password: String, quotaGb: Double) =
        client.post("/api/agents") {
            setBody(mapOf("name" to name, "password" to password, "traffic_quota_gb" to quotaGb))
        }

    suspend fun deleteAgent(id: Int) = client.delete("/api/agents/$id")

    suspend fun editAgent(id: Int, data: Map<String, Any>) =
        client.post("/api/agents/$id/edit") { setBody(data) }

    suspend fun resetAgentPassword(id: Int, newPassword: String) =
        client.post("/api/agents/$id/reset-password") {
            setBody(mapOf("password" to newPassword))
        }

    // ── Backup ───────────────────────────────────────────────────────────

    suspend fun createBackup() = client.post("/api/backup/create")

    suspend fun getBackups(): List<BackupDto> =
        client.get("/api/backup/list").body<Map<String, List<BackupDto>>>()["backups"] ?: emptyList()

    // ── Agent Panel ──────────────────────────────────────────────────────

    suspend fun getAgentMe(): AgentDto =
        client.get("/api/agent/me").body()

    suspend fun updateAgentBrand(brandName: String) =
        client.post("/api/agent/brand") {
            setBody(mapOf("brand_name" to brandName))
        }

    suspend fun getAgentUsers(): List<UserDto> =
        client.get("/api/agent/users").body()

    suspend fun addAgentUser(name: String, trafficGb: Double, days: Int): UserDto =
        client.post("/api/agent/users") {
            setBody(mapOf("name" to name, "traffic" to trafficGb, "days" to days))
        }.body()

    suspend fun bulkAddAgentUsers(
        prefix: String, count: Int, trafficGb: Double, days: Int,
        numbered: Boolean = true, start: Int = 1, pad: Int = 3
    ) = client.post("/api/agent/bulk-users") {
        setBody(mapOf(
            "prefix" to prefix, "count" to count, "traffic" to trafficGb,
            "days" to days, "numbered" to numbered, "start" to start, "pad" to pad
        ))
    }

    suspend fun deleteAgentUser(name: String) =
        client.delete("/api/agent/users/${name.encodeURLPath()}")

    suspend fun toggleAgentUser(name: String) =
        client.post("/api/agent/users/${name.encodeURLPath()}/toggle")

    suspend fun renewAgentUser(name: String, trafficGb: Double, days: Int) =
        client.post("/api/agent/users/${name.encodeURLPath()}/renew") {
            setBody(mapOf("traffic" to trafficGb, "days" to days))
        }

    // Note: the panel only exposes add-traffic for agent-owned users — no
    // agent-scoped speed-limit / reset-traffic / update-note endpoints exist,
    // so those three stay admin-only in the UI.
    suspend fun addAgentTraffic(name: String, gb: Double) =
        client.post("/api/agent/users/${name.encodeURLPath()}/add-traffic") {
            setBody(mapOf("gb" to gb))
        }

    suspend fun getAgentLive(): Map<String, TrafficEntry> =
        client.get("/api/agent/live").body()

    suspend fun getAgentServerInfo(): ServerInfoDto =
        client.get("/api/agent/server-info").body()

    suspend fun agentSync() = client.post("/api/agent/sync")

    suspend fun getAgentGroups(): List<GroupDto> =
        client.get("/api/agent/groups").body<Map<String, List<GroupDto>>>()["groups"] ?: emptyList()

    suspend fun agentBulkDelete(names: List<String>) =
        client.post("/api/agent/bulk-delete") { setBody(mapOf("names" to names)) }

    suspend fun agentBulkExportZip(names: List<String>): ByteArray =
        client.post("/api/agent/bulk-export-zip") { setBody(mapOf("names" to names)) }.body()

    // ── Subscription (public) ────────────────────────────────────────────

    /**
     * Raw subscription payload plus the metadata the panel ships in headers.
     *
     * The machine-readable endpoint is /sub-api/UUID — it returns the Base64
     * config list AND the standard Subscription-Userinfo / Content-Disposition
     * headers. /sub/UUID only renders an HTML page (no headers), so it must
     * never be parsed programmatically.
     */
    data class SubRawResponse(
        val body: String,
        val subscriptionUserinfo: String? = null,
        val contentDisposition: String? = null,
        val supportUrl: String? = null,
        val profileTitle: String? = null
    )

    suspend fun getSubRawWithHeaders(uuid: String): SubRawResponse {
        val response = client.get("/sub-api/$uuid") {
            accept(ContentType.Text.Plain)
        }
        if (!response.status.isSuccess()) {
            throw ApiException("Subscription not found (${response.status})", response.status.value)
        }
        return SubRawResponse(
            body = response.bodyAsText(),
            subscriptionUserinfo = response.headers["subscription-userinfo"],
            contentDisposition = response.headers["content-disposition"],
            supportUrl = response.headers["support-url"],
            profileTitle = response.headers["profile-title"]
        )
    }

    /** Config list + user metadata derived from a single /sub-api round-trip. */
    data class SubBundle(val user: UserDto, val rawBody: String)

    /**
     * Single-call subscription fetch. Hits /sub-api ONCE and derives:
     *   • the raw config payload (Base64 list) from the body,
     *   • traffic + expiry from the Subscription-Userinfo header,
     *   • the profile name from the Content-Disposition filename.
     * Replaces the previous 2-3 round-trips (/sub-info + /sub-api + /sub-api).
     */
    suspend fun getSubBundle(uuid: String): SubBundle {
        val raw = getSubRawWithHeaders(uuid)
        val titleFromCd = profileTitleFromContentDisposition(raw.contentDisposition)
        val headerUser = raw.subscriptionUserinfo?.let { headerValue ->
            val title = raw.profileTitle?.takeIf { it.isNotBlank() } ?: titleFromCd
            runCatching { parseSubscriptionUserinfo(headerValue, uuid, title) }.getOrNull()
        }
        val user = (headerUser ?: UserDto(uuid = uuid)).copy(
            uuid = uuid,
            name = (headerUser?.name).orEmpty().ifBlank {
                titleFromCd?.trim().orEmpty().ifBlank { "Subscriber" }
            },
            supportUrl = raw.supportUrl.orEmpty().ifBlank { headerUser?.supportUrl.orEmpty() }
        )
        return SubBundle(user, raw.body)
    }

    private fun profileTitleFromContentDisposition(contentDisposition: String?): String? {
        if (contentDisposition.isNullOrBlank()) return null
        val m = Regex("filename\\*=(?:UTF-8''|)([^\";]+)", RegexOption.IGNORE_CASE).find(contentDisposition)
            ?: Regex("filename=\"([^\"]+)\"").find(contentDisposition)
            ?: return null
        return m.groupValues.getOrNull(1)?.trim()?.trim('"')?.takeIf { it.isNotBlank() }
    }

    private fun parseSubscriptionUserinfo(header: String, uuid: String, title: String?): UserDto {
        // Format: upload=123; download=456; total=789; expire=1234567890 (spaces after ; optional)
        val parts = header.split(";").associate { part ->
            val kv = part.trim().split("=", limit = 2)
            if (kv.size == 2) kv[0].trim().lowercase() to kv[1].trim() else "" to ""
        }
        val upload = parts["upload"]?.toLongOrNull() ?: 0L
        val download = parts["download"]?.toLongOrNull() ?: 0L
        val total = parts["total"]?.toLongOrNull() ?: 0L
        var expire = parts["expire"]?.toLongOrNull() ?: 0L
        if (expire > 10_000_000_000L) expire /= 1000L

        val usedBytes = upload + download
        val usedGb = usedBytes / (1024.0 * 1024.0 * 1024.0)
        val totalGb = total / (1024.0 * 1024.0 * 1024.0)
        val daysLeft = if (expire > 0L) {
            val now = System.currentTimeMillis() / 1000
            ((expire - now) / 86400).toInt().coerceAtLeast(0)
        } else 0
        val expireStr = if (expire > 0L) {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(expire * 1000))
        } else ""

        return UserDto(
            name = title?.trim()?.takeIf { it.isNotBlank() } ?: "Subscriber",
            uuid = uuid,
            uploadBytes = upload,
            downloadBytes = download,
            trafficLimitGb = totalGb,
            trafficUsedGb = usedGb,
            trafficPercent = if (totalGb > 0.0) (usedGb / totalGb * 100.0).coerceIn(0.0, 100.0) else 0.0,
            trafficLimitBytes = total,
            trafficUsedBytes = usedBytes,
            expireAt = expireStr,
            daysLeft = daysLeft,
            active = daysLeft > 0 || expire == 0L
        )
    }

    fun close() = client.close()
}
