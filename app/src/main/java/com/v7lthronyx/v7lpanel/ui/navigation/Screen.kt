package com.v7lthronyx.v7lpanel.ui.navigation

sealed class Screen(val route: String) {
    object Splash         : Screen("splash")
    object ServerSelect   : Screen("server_select")
    object ManagementLogin : Screen("management_login")
    object Management      : Screen("management")

    // ── Main tabs (bottom nav) ──
    object Home           : Screen("home")
    /** Protocol list tab (UI label: Protocols / پروتکل‌ها). */
    object Protocols      : Screen("protocols")
    object Settings       : Screen("settings")
    object Profile        : Screen("profile")

    // ── Sub-screens ──
    object ConnectionSettings : Screen("connection_settings")
    object SplitTunneling     : Screen("split_tunneling")
    object Help               : Screen("help")
    object QRScanner          : Screen("qr_scanner")
    object About              : Screen("about")
    object DiagnosticTools    : Screen("diagnostic_tools")

    companion object {
        val bottomNavRoutes = setOf(
            Home.route, Protocols.route, Settings.route, Profile.route
        )
    }
}
