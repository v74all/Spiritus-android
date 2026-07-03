package com.v7lthronyx.v7lpanel.ui.components

import com.v7lthronyx.v7lpanel.ui.theme.LocalAccent
import com.v7lthronyx.v7lpanel.ui.theme.LocalAccentLight
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.navigation.Screen
import com.v7lthronyx.v7lpanel.ui.theme.V7LColors

data class BottomNavItem(
    val route: String,
    val labelEn: String,
    val labelFa: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, "Home", "خانه", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Protocols.route, "Protocols", "پروتکل‌ها", Icons.Filled.Layers, Icons.Outlined.Layers),
    BottomNavItem(Screen.Settings.route, "Settings", "تنظیمات", Icons.Filled.Settings, Icons.Outlined.Settings),
    BottomNavItem(Screen.Profile.route, "Profile", "پروفایل", Icons.Filled.Person, Icons.Outlined.Person),
)

@Composable
fun VipBottomNav(
    currentRoute: String?,
    language: String,
    onNavigate: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(V7LColors.bg1.copy(alpha = 0.92f))
            .border(1.dp, V7LColors.border.copy(alpha = 0.8f))
            // Keep the bar above the system navigation bar (edge-to-edge):
            // the background extends behind it, the items stay tappable.
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 11.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Top
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentRoute == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onNavigate(item.route)
                            }
                        }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth(0.32f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (selected) LocalAccent.current else Color.Transparent)
                    )
                    Spacer(Modifier.height(8.dp))
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.labelEn,
                        tint = if (selected) LocalAccent.current else V7LColors.t2,
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (language == "fa") item.labelFa else item.labelEn,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) LocalAccent.current else V7LColors.t2,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
