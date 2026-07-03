package com.v7lthronyx.v7lpanel.ui.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.*
import androidx.navigation.compose.*
import com.v7lthronyx.v7lpanel.SessionHolder
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import com.v7lthronyx.v7lpanel.ui.components.VipBottomNav
import com.v7lthronyx.v7lpanel.ui.screens.home.HomeScreen
import com.v7lthronyx.v7lpanel.ui.screens.home.HomeViewModel
import com.v7lthronyx.v7lpanel.ui.screens.locations.LocationsScreen
import com.v7lthronyx.v7lpanel.ui.screens.locations.LocationsViewModel
import com.v7lthronyx.v7lpanel.ui.screens.login.ServerSelectScreen
import com.v7lthronyx.v7lpanel.ui.screens.management.ManagementLoginScreen
import com.v7lthronyx.v7lpanel.ui.screens.management.ManagementScreen
import com.v7lthronyx.v7lpanel.ui.screens.profile.ProfileScreen
import com.v7lthronyx.v7lpanel.ui.screens.qr.QRScannerScreen
import com.v7lthronyx.v7lpanel.ui.screens.settings.ConnectionSettingsScreen
import com.v7lthronyx.v7lpanel.ui.screens.settings.DiagnosticToolsScreen
import com.v7lthronyx.v7lpanel.ui.screens.settings.SettingsScreen
import com.v7lthronyx.v7lpanel.ui.screens.settings.SplitTunnelingScreen
import com.v7lthronyx.v7lpanel.ui.screens.splash.SplashScreen
import com.v7lthronyx.v7lpanel.ui.screens.subscription.SubViewModel
import com.v7lthronyx.v7lpanel.ui.theme.LocalLang
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun V7LNavGraph(
    modifier: Modifier = Modifier,
    deepLinkData: com.v7lthronyx.v7lpanel.DeepLinkData? = null
) {
    val navController = rememberNavController()
    val settingsStore: SettingsDataStore = koinInject()
    val manualConfigDao: com.v7lthronyx.v7lpanel.data.local.ManualConfigDao = koinInject()
    val lang by settingsStore.language.collectAsState(initial = "en")

    val direction = if (lang == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr

    // Handle deep link navigation
    LaunchedEffect(deepLinkData) {
        if (deepLinkData != null) {
            if (deepLinkData.role == "subscriber" && !deepLinkData.uuid.isNullOrBlank()) {
                SessionHolder.serverUrl = deepLinkData.serverUrl
                SessionHolder.role = "subscriber"
                SessionHolder.uuid = deepLinkData.uuid
                SessionHolder.tlsPinSha256 = deepLinkData.tlsPinSha256
                // Same destination as the normal subscriber flow (ServerSelect ->
                // Home) — there is no separate "subscription screen" anymore.
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.ServerSelect.route) {
                    popUpTo(0) { inclusive = true }
                }
                navController.currentBackStackEntry?.savedStateHandle
                    ?.set("scanned_url", deepLinkData.rawImportUri)
            }
        }
    }

    // Track current route for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomNav = currentRoute in Screen.bottomNavRoutes

    CompositionLocalProvider(
        LocalLang provides lang,
        LocalLayoutDirection provides direction
    ) {
        Scaffold(
            bottomBar = {
                if (showBottomNav) {
                    VipBottomNav(
                        currentRoute = currentRoute,
                        language = lang,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                // consumeWindowInsets: the Scaffold padding already covers the
                // system bars, so screens must not re-apply statusBarsPadding /
                // nested-Scaffold insets on top of it (double framing).
                modifier = modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
                // Smooth, directional slide + fade between destinations.
                enterTransition = {
                    slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it / 6 } +
                        fadeIn(animationSpec = tween(220))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180)) +
                        slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it / 12 }
                },
                popEnterTransition = {
                    slideInHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { -it / 6 } +
                        fadeIn(animationSpec = tween(220))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180)) +
                        slideOutHorizontally(animationSpec = tween(320, easing = FastOutSlowInEasing)) { it / 12 }
                }
            ) {
                // ── Splash ──
                composable(Screen.Splash.route) {
                    SplashScreen(onNavigate = { role, _, uuid ->
                        if (role == "admin" || role == "agent") {
                            navController.navigate(Screen.Management.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else if (role == "local") {
                            SessionHolder.role = "local"
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else if (!uuid.isNullOrBlank()) {
                            SessionHolder.uuid = uuid
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.ServerSelect.route) {
                                popUpTo(Screen.Splash.route) { inclusive = true }
                            }
                        }
                    })
                }

                // ── Server Select (login) ──
                composable(Screen.ServerSelect.route) { backStack ->
                    val scope = rememberCoroutineScope()
                    ServerSelectScreen(
                        onSubscriber = { url, uuid ->
                            SessionHolder.serverUrl = url
                            SessionHolder.role = "subscriber"
                            SessionHolder.uuid = uuid
                            // Persist session for cold start restore
                            scope.launch {
                                settingsStore.setSessionServerUrl(url)
                                settingsStore.setSessionUuid(uuid)
                                settingsStore.setSessionRole("subscriber")
                                settingsStore.setSessionTlsPin(SessionHolder.tlsPinSha256.orEmpty())
                            }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onManagement = { profile ->
                            SessionHolder.profileId = profile.id
                            SessionHolder.serverUrl = profile.url
                            SessionHolder.role = profile.role
                            SessionHolder.principal = profile.agentName.orEmpty()
                            SessionHolder.tlsPinSha256 = profile.tlsPinSha256
                            navController.navigate(Screen.ManagementLogin.route)
                        },
                        onLocalConfig = { label, uri ->
                            // Local "general client" mode: just a raw config, no panel.
                            SessionHolder.role = "local"
                            SessionHolder.uuid = ""
                            SessionHolder.serverUrl = ""
                            SessionHolder.principal = label
                            scope.launch {
                                manualConfigDao.insert(
                                    com.v7lthronyx.v7lpanel.data.db.entities.ManualConfig(label = label, uri = uri)
                                )
                                settingsStore.setSessionRole("local")
                                settingsStore.setSessionUuid("")
                                settingsStore.setSessionServerUrl("")
                            }
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onQRScanner = { navController.navigate(Screen.QRScanner.route) },
                        scannedUrl = backStack.savedStateHandle
                            .getStateFlow("scanned_url", "").collectAsState().value
                    )
                }

                composable(Screen.ManagementLogin.route) {
                    ManagementLoginScreen(
                        onAuthenticated = {
                            navController.navigate(Screen.Management.route) {
                                popUpTo(Screen.ServerSelect.route) { inclusive = true }
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.Management.route) {
                    ManagementScreen(onLogout = {
                        navController.navigate(Screen.ServerSelect.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    })
                }

                // ── Main tabs ──
                composable(Screen.Home.route) {
                    val homeVm: HomeViewModel = koinViewModel()
                    fun goTab(route: String) {
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    HomeScreen(
                        viewModel = homeVm,
                        onGoServers = { goTab(Screen.Protocols.route) }
                    )
                }

                composable(Screen.Protocols.route) {
                    val locVm: LocationsViewModel = koinViewModel()
                    val homeVm: HomeViewModel = koinViewModel()
                    val context = androidx.compose.ui.platform.LocalContext.current

                    var pendingConnect by remember { mutableStateOf<Pair<String, String>?>(null) }
                    val vpnPermLauncher = rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
                    ) {
                        pendingConnect?.let { (lbl, u) ->
                            homeVm.connect(context, lbl, u)
                            pendingConnect = null
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    }

                    LocationsScreen(
                        viewModel = locVm,
                        onConnect = { label, uri ->
                            val intent = android.net.VpnService.prepare(context)
                            if (intent != null) {
                                pendingConnect = label to uri
                                vpnPermLauncher.launch(intent)
                            } else {
                                homeVm.connect(context, label, uri)
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            }
                        }
                    )
                }

                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onNavigateToConnectionSettings = { navController.navigate(Screen.ConnectionSettings.route) },
                        onNavigateToSplitTunneling = { navController.navigate(Screen.SplitTunneling.route) },
                        onNavigateToDiagnostics = { navController.navigate(Screen.DiagnosticTools.route) },
                        onNavigateToAbout = { navController.navigate(Screen.About.route) }
                    )
                }

                composable(Screen.Profile.route) {
                    val uuid = SessionHolder.uuid
                    val logoutScope = rememberCoroutineScope()
                    if (!uuid.isNullOrBlank()) {
                        val vm: SubViewModel = koinViewModel(parameters = { parametersOf(uuid) })
                        val uiState by vm.uiState.collectAsState()
                        ProfileScreen(
                            userInfo = uiState.user,
                            profileMetadataMissing = uiState.profileMetadataMissing,
                            isLoading = uiState.isLoading,
                            error = uiState.error,
                            onRefresh = { vm.load() },
                            onLogout = {
                                SessionHolder.clear()
                                logoutScope.launch { settingsStore.clearSession() }
                                navController.navigate(Screen.ServerSelect.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onAdminConsole = {
                                // A separate admin/agent login — does not touch the
                                // current subscriber session. Back returns here.
                                navController.navigate(Screen.ServerSelect.route)
                            }
                        )
                    } else {
                        ProfileScreen(
                            userInfo = null,
                            // No subscriber uuid means there is no panel
                            // account to fetch (local/manual config) — not a
                            // failed request, so don't offer a dead Retry.
                            noAccount = true,
                            onRefresh = {},
                            onLogout = {
                                SessionHolder.clear()
                                logoutScope.launch { settingsStore.clearSession() }
                                navController.navigate(Screen.ServerSelect.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onAdminConsole = {
                                // Local mode still gets a path to the Spiritus
                                // panel login (admin/agent) — same as subscriber.
                                navController.navigate(Screen.ServerSelect.route)
                            }
                        )
                    }
                }

                // ── Sub-screens ──
                composable(Screen.ConnectionSettings.route) {
                    ConnectionSettingsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.SplitTunneling.route) {
                    SplitTunnelingScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.DiagnosticTools.route) {
                    DiagnosticToolsScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(Screen.QRScanner.route) {
                    QRScannerScreen(
                        onResult = { url ->
                            navController.previousBackStackEntry
                                ?.savedStateHandle?.set("scanned_url", url)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
