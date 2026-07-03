package com.v7lthronyx.v7lpanel

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.v7lthronyx.v7lpanel.ui.navigation.V7LNavGraph
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors
import com.v7lthronyx.v7lpanel.ui.theme.V7LTheme
import com.v7lthronyx.v7lpanel.ui.screens.login.parseServerUrl

class MainActivity : ComponentActivity() {

    /** Must be registered in onCreate after super — not as a field initializer (lifecycle still INITIALIZED). */
    private lateinit var notifPermissionLauncher: ActivityResultLauncher<String>
    
    private val deepLinkData = mutableStateOf<DeepLinkData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notifPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* optional: track grant */ }
        enableEdgeToEdge()
        
        // Handle deep link from intent
        handleDeepLink(intent)
        
        // Defer: asking during onCreate can race with window focus on some OEMs
        window.decorView.post {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        setContent {
            val settings = remember { SettingsDataStore(applicationContext) }
            val accentHex by settings.accentColor.collectAsState(initial = "#34E5A4")
            V7LTheme(accentHex = accentHex) {
                V7LNavGraph(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(V7LColors.bg0),
                    deepLinkData = deepLinkData.value
                )
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }
    
    private fun handleDeepLink(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        val parsed = parseServerUrl(data.toString()) ?: return
        deepLinkData.value = DeepLinkData(
            serverUrl = parsed.baseUrl,
            role = parsed.role,
            uuid = parsed.uuid,
            agentName = parsed.agentName,
            tlsPinSha256 = parsed.tlsPinSha256,
            rawImportUri = data.toString()
        )
    }
}

data class DeepLinkData(
    val serverUrl: String,
    val role: String,
    val uuid: String?,
    val agentName: String?,
    val tlsPinSha256: String?,
    val rawImportUri: String
)
