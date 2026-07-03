package com.v7lthronyx.v7lpanel.ui.screens.management

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.security.SecureTokenStore
import com.v7lthronyx.v7lpanel.ui.components.AuroraBackground
import com.v7lthronyx.v7lpanel.ui.theme.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun ManagementLoginScreen(onAuthenticated: () -> Unit, onBack: () -> Unit) {
    val tokenStore: SecureTokenStore = koinInject()
    val scope = rememberCoroutineScope()
    val lang = LocalLang.current
    val fa = lang == "fa"
    val accent = LocalAccent.current
    val accentLight = LocalAccentLight.current

    var username by remember { mutableStateOf(SessionHolder.principal.ifBlank { if (SessionHolder.role == "admin") "admin" else "" }) }
    var password by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var challenge by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showPassword by remember { mutableStateOf(false) }

    fun complete(token: String, principal: String) {
        SessionHolder.accessToken = token
        SessionHolder.principal = principal
        SessionHolder.getOrCreateClient().setAccessToken(token)
        tokenStore.save(token, SessionHolder.profileId, SessionHolder.role, principal)
        onAuthenticated()
    }

    fun submit() {
        scope.launch {
            loading = true; error = null
            runCatching {
                val client = SessionHolder.getOrCreateClient()
                val response = challenge?.let { client.mobile2FA(it, code) }
                    ?: client.mobileLogin(SessionHolder.role, username, password)
                if (response.totpRequired) {
                    challenge = response.challenge ?: error("Missing 2FA challenge")
                    password = ""
                } else {
                    complete(response.accessToken ?: error("Missing access token"), response.principal.ifBlank { username })
                }
            }.onFailure { error = it.message ?: if (fa) "ورود ناموفق بود" else "Login failed" }
            loading = false
        }
    }

    val roleLabel = SessionHolder.role.replaceFirstChar { it.uppercase() }
    val canSubmit = !loading && username.isNotBlank() && (challenge != null && code.length == 6 || challenge == null && password.isNotBlank())

    AuroraBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(top = 40.dp, start = 30.dp, end = 30.dp, bottom = 24.dp)
        ) {
            // ── top bar: back ──
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Dz.surf05)
                        .border(1.dp, Dz.border2, RoundedCornerShape(12.dp)).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ArrowBack, "Back", tint = Dz.t1, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(1.dp))
            }

            Column(
                Modifier.fillMaxWidth().weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── hero ──
                Box(
                    Modifier.size(92.dp).clip(RoundedCornerShape(26.dp))
                        .background(Brush.linearGradient(listOf(accent, accentLight))),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.Filled.ShieldMoon, null, tint = Color.White, modifier = Modifier.size(44.dp)) }

                Spacer(Modifier.height(22.dp))
                Row {
                    Text("$roleLabel", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Dz.tHi)
                    Text(" console", fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, color = accent)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (fa) "برای دسترسی به کنسول مدیریت وارد شوید" else "Sign in to manage users, agents & servers",
                    fontSize = 13.sp, color = Dz.t3, fontFamily = Vazirmatn
                )

                Spacer(Modifier.height(36.dp))

                // ── inputs ──
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LoginField(
                        value = username, onValueChange = { username = it },
                        placeholder = if (fa) "نام کاربری" else "Username",
                        leading = Icons.Filled.Person,
                        enabled = challenge == null
                    )
                    if (challenge == null) {
                        LoginField(
                            value = password, onValueChange = { password = it },
                            placeholder = if (fa) "گذرواژه" else "Password",
                            leading = Icons.Filled.Lock,
                            isPassword = true,
                            showPassword = showPassword,
                            onToggleShow = { showPassword = !showPassword }
                        )
                    } else {
                        LoginField(
                            value = code, onValueChange = { code = it.filter(Char::isDigit).take(6) },
                            placeholder = if (fa) "کد ۲مرحله‌ای" else "2FA code",
                            leading = Icons.Filled.Pin,
                            keyboardType = KeyboardType.Number
                        )
                    }
                }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = Dz.danger, fontSize = 12.sp, fontFamily = Vazirmatn)
                }

                Spacer(Modifier.height(18.dp))

                // ── submit ──
                Box(
                    Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(16.dp))
                        .background(
                            if (canSubmit) Brush.linearGradient(listOf(accent, accentLight))
                            else Brush.linearGradient(listOf(Dz.tFaint, Dz.tFaint))
                        )
                        .clickable(enabled = canSubmit) { submit() },
                    contentAlignment = Alignment.Center
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (challenge == null) (if (fa) "ورود" else "Sign in") else (if (fa) "تأیید" else "Verify"),
                                color = Color.White, fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.5.sp
                            )
                            Spacer(Modifier.width(9.dp))
                            Icon(Icons.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoginField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onToggleShow: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(16.dp))
            .background(Dz.surf04).border(1.dp, Dz.border2, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(leading, null, tint = Dz.t4, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = Dz.tHi, fontSize = 14.5.sp,
                fontFamily = if (isPassword) JetBrainsMono else Vazirmatn,
                letterSpacing = if (isPassword) 2.sp else 0.sp
            ),
            visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboardType),
            cursorBrush = Brush.linearGradient(listOf(Dz.tHi, Dz.tHi)),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (value.isEmpty()) Text(placeholder, color = Dz.tMute, fontSize = 14.5.sp, fontFamily = Vazirmatn)
                inner()
            }
        )
        if (isPassword && onToggleShow != null) {
            Icon(
                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null,
                tint = Dz.t4, modifier = Modifier.size(18.dp).clickable { onToggleShow() }
            )
        }
    }
}
