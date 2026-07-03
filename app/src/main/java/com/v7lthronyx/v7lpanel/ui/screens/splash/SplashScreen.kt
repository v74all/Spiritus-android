package com.v7lthronyx.v7lpanel.ui.screens.splash

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.data.local.ServerProfileDao
import com.v7lthronyx.v7lpanel.data.security.SecureTokenStore
import com.v7lthronyx.v7lpanel.ui.components.AsciiTypewriter
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import com.v7lthronyx.v7lpanel.ui.theme.Inter
import com.v7lthronyx.v7lpanel.ui.theme.JetBrainsMono
import com.v7lthronyx.v7lpanel.util.SafeLog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.koinInject

@Composable
fun SplashScreen(
    onNavigate: (role: String?, serverUrl: String?, uuid: String?) -> Unit
) {
    val settingsStore: SettingsDataStore = koinInject()
    val tokenStore: SecureTokenStore = koinInject()
    val profileDao: ServerProfileDao = koinInject()
    var logoVisible by remember { mutableStateOf(false) }
    var loadingVisible by remember { mutableStateOf(false) }

    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    // Scale animation for logo
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(800),
        label = "logoAlpha"
    )

    LaunchedEffect(Unit) {
        logoVisible = true
        settingsStore.purgeLegacyPasswords()

        // Restore session from DataStore
        val savedUrl  = settingsStore.sessionServerUrl.firstOrNull() ?: ""
        val savedUuid = settingsStore.sessionUuid.firstOrNull() ?: ""
        val savedPin = settingsStore.sessionTlsPin.firstOrNull().orEmpty()
        val savedRole = settingsStore.sessionRole.firstOrNull().orEmpty()
        var restoredRole: String? = null

        tokenStore.load()?.let { stored ->
            val profile = profileDao.getById(stored.profileId)
            if (profile != null && profile.role == stored.role) {
                SessionHolder.profileId = profile.id
                SessionHolder.serverUrl = profile.url
                SessionHolder.role = profile.role
                SessionHolder.principal = stored.principal
                SessionHolder.tlsPinSha256 = profile.tlsPinSha256
                SessionHolder.accessToken = stored.token
                val valid = runCatching {
                    SessionHolder.getOrCreateClient().validateMobileSession()
                }.getOrNull()
                if (valid?.role == profile.role) restoredRole = profile.role
                else {
                    tokenStore.clear()
                    SessionHolder.clear()
                }
            } else tokenStore.clear()
        }
        SafeLog.d("Splash", "Restored session urlPresent=${savedUrl.isNotBlank()} uuidPresent=${savedUuid.isNotBlank()}")

        if (restoredRole == null && savedUrl.isNotBlank() && savedUuid.isNotBlank()) {
            SessionHolder.serverUrl = savedUrl
            SessionHolder.role = "subscriber"
            SessionHolder.uuid = savedUuid
            SessionHolder.tlsPinSha256 = savedPin.ifBlank { null }
            SafeLog.d("Splash", "SessionHolder populated, navigating to Home")
        } else {
            SafeLog.d("Splash", "No saved subscriber session, navigating to ServerSelect")
        }

        delay(1200)
        loadingVisible = true
        delay(300)

        if (restoredRole != null) {
            onNavigate(restoredRole, SessionHolder.serverUrl, null)
        } else if (savedRole == "local") {
            SessionHolder.role = "local"
            onNavigate("local", null, null)
        } else if (savedUuid.isNotBlank()) {
            onNavigate(null, savedUrl, savedUuid)
        } else {
            onNavigate(null, null, null)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        V7LColors.accentDark.copy(alpha = 0.15f * gradientOffset),
                        V7LColors.bg1.copy(alpha = 0.9f),
                        V7LColors.bg0
                    ),
                    radius = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .scale(logoScale)
                .alpha(logoAlpha)
        ) {
            if (logoVisible) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(com.v7lthronyx.v7lpanel.R.drawable.logo_spiritus_lockup),
                    contentDescription = "Spiritus",
                    modifier = Modifier.width(260.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = "v2.0  •  Secure & Private",
                fontFamily = Inter,
                fontSize = 12.sp,
                color = V7LColors.t3,
                letterSpacing = 1.5.sp
            )

            AnimatedVisibility(
                visible = loadingVisible,
                enter = fadeIn(tween(400)),
                exit = fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp)
                ) {
                    AsciiTypewriter(
                        text = "Initializing...",
                        durationMs = 600,
                        fontSize = 13
                    )
                }
            }
        }
    }
}
