package com.v7lthronyx.v7lpanel.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.TrafficStats
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.FileDescriptor
import com.v7lthronyx.v7lpanel.MainActivity
import com.v7lthronyx.v7lpanel.R
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import com.v7lthronyx.v7lpanel.vpn.ConfigParser
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import java.io.File

class V7LVpnService : VpnService() {

    companion object {
        const val ACTION_START        = "com.v7lthronyx.v7lpanel.VPN_START"
        const val ACTION_STOP         = "com.v7lthronyx.v7lpanel.VPN_STOP"
        const val EXTRA_LABEL         = "vpn_label"
        const val EXTRA_OUTBOUND_JSON = "vpn_outbound_json"
        const val EXTRA_ENGINE        = "vpn_engine"
        private const val NOTIF_CHANNEL = "vpn_channel"
        private const val NOTIF_ID      = 1001
        private const val SOCKS_PORT    = 10808
        private const val HTTP_PORT     = 10809
    }

    private var tfd: ParcelFileDescriptor? = null
    private var xrayProcess: java.lang.Process? = null
    private var tun2socksProcess: java.lang.Process? = null
    private var singboxProcess: java.lang.Process? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val label        = intent.getStringExtra(EXTRA_LABEL) ?: "Spiritus"
                val outboundJson = intent.getStringExtra(EXTRA_OUTBOUND_JSON) ?: run {
                    VpnManager.updateStatus(VpnStatus.ERROR, "No config provided")
                    stopSelf(); return START_NOT_STICKY
                }
                val engine = intent.getStringExtra(EXTRA_ENGINE) ?: "xray"
                startForegroundWithType(
                    buildNotification(label, getString(R.string.vpn_notif_connecting))
                )
                startVpn(label, outboundJson, engine)
            }
            ACTION_STOP  -> {
                stopVpn()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    private fun startVpn(label: String, outboundJson: String, engine: String) {
        scope.launch {
            try {
                val dataStore = SettingsDataStore(this@V7LVpnService)
                val enableIpv6 = dataStore.enableIpv6.first()
                val customDns = dataStore.dnsServer.first()
                val chainUri = dataStore.proxyChainUri.first()
                val killSwitch = dataStore.killSwitchEnabled.first()
                val splitMode = dataStore.splitTunnelMode.first()
                val splitApps = dataStore.splitTunnelApps.first()
                val fragmentEnabled = dataStore.fragmentEnabled.first()
                val fragmentPackets = dataStore.fragmentPackets.first()
                val fragmentLength = dataStore.fragmentLength.first()
                val fragmentInterval = dataStore.fragmentInterval.first()
                val mtuSize = dataStore.mtuSize.first().let { if (it <= 0) 1500 else it }

                // Detect engine from wrapper JSON
                val actualEngine = try {
                    val parsed = JSONObject(outboundJson)
                    parsed.optString("_engine", engine)
                } catch (_: Exception) { engine }

                // 1. Build VPN interface
                val builder = Builder()
                    .setMtu(mtuSize)
                    .addAddress("172.19.0.1", 30)
                    .addRoute("0.0.0.0", 0)
                    .setSession(label)

                // Split tunneling
                when (splitMode) {
                    "allowlist" -> splitApps.forEach { pkg ->
                        try { builder.addAllowedApplication(pkg) } catch (_: Exception) {}
                    }
                    "disallowlist" -> {
                        try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}
                        splitApps.forEach { pkg ->
                            try { builder.addDisallowedApplication(pkg) } catch (_: Exception) {}
                        }
                    }
                    else -> try { builder.addDisallowedApplication(packageName) } catch (_: Exception) {}
                }

                // Kill switch: set blocking so traffic is blocked if VPN goes down
                if (killSwitch) {
                    builder.setBlocking(true)
                }

                if (customDns.isNotBlank()) {
                    try { builder.addDnsServer(customDns) } catch(e: Exception) { builder.addDnsServer("8.8.8.8") }
                } else {
                    builder.addDnsServer("8.8.8.8")
                }

                if (enableIpv6) {
                    try {
                        builder.addAddress("fdfe:dcba:9876::1", 126)
                        builder.addRoute("::", 0)
                    } catch(e: Exception) {}
                }

                val pfd = builder.establish() ?: run {
                    VpnManager.updateStatus(VpnStatus.ERROR, "VPN interface failed")
                    stopSelf(); return@launch
                }
                tfd = pfd
                clearCloExec(pfd.fileDescriptor)

                when (actualEngine) {
                    "singbox" -> startWithSingBox(label, outboundJson, pfd)
                    else -> startWithXray(label, outboundJson, pfd, chainUri, customDns, enableIpv6, fragmentEnabled, fragmentPackets, fragmentLength, fragmentInterval)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    VpnManager.updateStatus(VpnStatus.ERROR, e.message ?: "Unknown error")
                    stopSelf()
                }
            }
        }
    }

    private suspend fun startWithXray(
        label: String, outboundJson: String, pfd: ParcelFileDescriptor,
        chainUri: String, customDns: String, enableIpv6: Boolean,
        fragmentEnabled: Boolean, fragmentPackets: String, fragmentLength: String, fragmentInterval: String
    ) {
        // 2. Write Xray config
        val xrayConfig = buildXrayConfig(outboundJson, chainUri, customDns, enableIpv6, fragmentEnabled, fragmentPackets, fragmentLength, fragmentInterval)
        val configFile = File(codeCacheDir, "xray.json")
        configFile.writeText(xrayConfig)

        // 3. Resolve binaries
        val xrayBin = resolveBinary("xray", "xray") ?: run {
            VpnManager.updateStatus(VpnStatus.ERROR, "Xray binary missing")
            pfd.close(); stopSelf(); return
        }
        val tun2socksBin = resolveBinary("tun2socks", "tun2socks") ?: run {
            VpnManager.updateStatus(VpnStatus.ERROR, "tun2socks binary missing")
            pfd.close(); stopSelf(); return
        }

        // 4. Launch Xray
        val xrayLog = File(codeCacheDir, "xray.log")
        xrayLog.writeText("")
        val xray = ProcessBuilder(xrayBin, "run", "-c", configFile.absolutePath)
            .redirectErrorStream(true)
            .redirectOutput(xrayLog)
            .start()
        xrayProcess = xray

        delay(800)
        if (!xray.isAlive) {
            val log = runCatching { xrayLog.readText().trim().takeLast(400) }.getOrDefault("")
            withContext(Dispatchers.Main) {
                VpnManager.updateStatus(VpnStatus.ERROR, "Xray crashed: $log")
                stopSelf()
            }
            return
        }

        // 5. Launch tun2socks
        val t2sLog = File(codeCacheDir, "tun2socks.log")
        t2sLog.writeText("")
        Os.dup2(pfd.fileDescriptor, 0)
        val t2s = ProcessBuilder(
            tun2socksBin,
            "--device", "fd://0",
            "--proxy", "socks5://127.0.0.1:$SOCKS_PORT",
            "--loglevel", "warning"
        )
            .redirectInput(ProcessBuilder.Redirect.INHERIT)
            .redirectErrorStream(true)
            .redirectOutput(t2sLog)
            .start()
        tun2socksProcess = t2s

        try {
            val devNull = Os.open("/dev/null", android.system.OsConstants.O_RDONLY, 0)
            Os.dup2(devNull, 0)
            Os.close(devNull)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        delay(500)
        if (!t2s.isAlive) {
            val log = runCatching { t2sLog.readText().trim().takeLast(400) }.getOrDefault("")
            xray.destroy()
            withContext(Dispatchers.Main) {
                VpnManager.updateStatus(VpnStatus.ERROR, "tun2socks crashed: $log")
                stopSelf()
            }
            return
        }

        withContext(Dispatchers.Main) {
            startForegroundWithType(
                buildNotification(label, getString(R.string.vpn_notif_connected))
            )
            VpnManager.updateStatus(VpnStatus.CONNECTED)
        }

        monitorSpeed(label) { xray.isAlive && t2s.isAlive }

        // Monitor processes
        val exitedProcess = scope.async(Dispatchers.IO) {
            while (xray.isAlive && t2s.isAlive) { delay(500) }
            if (!xray.isAlive) "xray" else "tun2socks"
        }.await()

        val logFile = if (exitedProcess == "xray") xrayLog else t2sLog
        val log = runCatching { logFile.readText().trim().takeLast(400) }.getOrDefault("")
        withContext(Dispatchers.Main) {
            if (VpnManager.status.value == VpnStatus.CONNECTED) {
                VpnManager.updateStatus(VpnStatus.ERROR,
                    "$exitedProcess stopped${if (log.isNotBlank()) ": $log" else ""}")
            }
            stopSelf()
        }
    }

    private suspend fun startWithSingBox(label: String, wrapperJson: String, pfd: ParcelFileDescriptor) {
        val parsed = JSONObject(wrapperJson)
        val proxyNode = parsed.getJSONObject("outbound")
        // "endpoint" nodes (e.g. WireGuard in sing-box 1.11+) live in a
        // dedicated top-level array; "outbound" nodes go in `outbounds`.
        val isEndpoint = parsed.optString("_singbox_kind") == "endpoint"

        val config = buildSingBoxConfig(proxyNode, isEndpoint)
        val configFile = File(codeCacheDir, "singbox.json")
        configFile.writeText(config)

        val singboxBin = resolveBinary("singbox", "sing-box") ?: run {
            VpnManager.updateStatus(VpnStatus.ERROR, "sing-box binary missing")
            pfd.close(); stopSelf(); return
        }
        val tun2socksBin = resolveBinary("tun2socks", "tun2socks") ?: run {
            VpnManager.updateStatus(VpnStatus.ERROR, "tun2socks binary missing")
            pfd.close(); stopSelf(); return
        }

        // sing-box runs as SOCKS5 inbound → proxy outbound
        val sbLog = File(codeCacheDir, "singbox.log")
        sbLog.writeText("")
        val sb = ProcessBuilder(singboxBin, "run", "-c", configFile.absolutePath)
            .redirectErrorStream(true)
            .redirectOutput(sbLog)
            .start()
        singboxProcess = sb

        delay(1000)
        if (!sb.isAlive) {
            val log = runCatching { sbLog.readText().trim().takeLast(400) }.getOrDefault("")
            withContext(Dispatchers.Main) {
                VpnManager.updateStatus(VpnStatus.ERROR, "sing-box crashed: $log")
                stopSelf()
            }
            return
        }

        // Launch tun2socks pointing to sing-box SOCKS5
        val t2sLog = File(codeCacheDir, "tun2socks.log")
        t2sLog.writeText("")
        Os.dup2(pfd.fileDescriptor, 0)
        val t2s = ProcessBuilder(
            tun2socksBin,
            "--device", "fd://0",
            "--proxy", "socks5://127.0.0.1:$SOCKS_PORT",
            "--loglevel", "warning"
        )
            .redirectInput(ProcessBuilder.Redirect.INHERIT)
            .redirectErrorStream(true)
            .redirectOutput(t2sLog)
            .start()
        tun2socksProcess = t2s

        try {
            val devNull = Os.open("/dev/null", android.system.OsConstants.O_RDONLY, 0)
            Os.dup2(devNull, 0)
            Os.close(devNull)
        } catch (e: Exception) { e.printStackTrace() }

        delay(500)
        if (!t2s.isAlive) {
            val log = runCatching { t2sLog.readText().trim().takeLast(400) }.getOrDefault("")
            sb.destroy()
            withContext(Dispatchers.Main) {
                VpnManager.updateStatus(VpnStatus.ERROR, "tun2socks crashed: $log")
                stopSelf()
            }
            return
        }

        withContext(Dispatchers.Main) {
            startForegroundWithType(
                buildNotification(label, getString(R.string.vpn_notif_connected))
            )
            VpnManager.updateStatus(VpnStatus.CONNECTED)
        }

        monitorSpeed(label) { sb.isAlive && t2s.isAlive }

        val exitedProcess = scope.async(Dispatchers.IO) {
            while (sb.isAlive && t2s.isAlive) { delay(500) }
            if (!sb.isAlive) "sing-box" else "tun2socks"
        }.await()

        val logFile = if (exitedProcess == "sing-box") sbLog else t2sLog
        val log = runCatching { logFile.readText().trim().takeLast(400) }.getOrDefault("")
        withContext(Dispatchers.Main) {
            if (VpnManager.status.value == VpnStatus.CONNECTED) {
                VpnManager.updateStatus(VpnStatus.ERROR,
                    "$exitedProcess stopped${if (log.isNotBlank()) ": $log" else ""}")
            }
            stopSelf()
        }
    }

    private fun buildSingBoxConfig(proxyNode: JSONObject, isEndpoint: Boolean = false): String {
        val config = JSONObject()
        config.put("log", JSONObject().put("level", "warn"))

        // Inbounds: SOCKS5 (same port as Xray so tun2socks works identically)
        val socksIn = JSONObject()
        socksIn.put("type", "socks")
        socksIn.put("tag", "socks-in")
        socksIn.put("listen", "127.0.0.1")
        socksIn.put("listen_port", SOCKS_PORT)
        config.put("inbounds", JSONArray().put(socksIn))

        // The proxy node ("proxy" tag) is either a normal outbound or an
        // endpoint (WireGuard). Place it in the matching top-level array.
        // Only "direct" is needed as a secondary outbound; DNS hijacking and
        // rejection are handled by route *actions* (see below), not by the
        // legacy "dns"/"block" outbound types which sing-box 1.13 removed.
        val direct = JSONObject().put("type", "direct").put("tag", "direct")
        if (isEndpoint) {
            config.put("endpoints", JSONArray().put(proxyNode))
            config.put("outbounds", JSONArray().put(direct))
        } else {
            config.put("outbounds", JSONArray().put(proxyNode).put(direct))
        }

        // DNS — modern (sing-box 1.12+) server format. The legacy
        // {tag,address,detour} shape is FATAL on 1.13 without an opt-in env var.
        config.put("dns", JSONObject()
            .put("servers", JSONArray()
                .put(JSONObject().put("type", "https").put("tag", "proxy-dns")
                    .put("server", "8.8.8.8").put("detour", "proxy"))
                .put(JSONObject().put("type", "local").put("tag", "direct-dns")))
            .put("final", "proxy-dns"))

        // Route — unmatched traffic goes to "proxy" (outbound or endpoint).
        // DNS queries are captured with the hijack-dns action; private IPs go
        // direct. Both are route actions, not outbound references.
        config.put("route", JSONObject()
            .put("auto_detect_interface", true)
            .put("final", "proxy")
            // Resolve outbound server domains via local DNS (avoids a
            // chicken-and-egg loop through the proxy). Required by 1.13+.
            .put("default_domain_resolver", "direct-dns")
            .put("rules", JSONArray()
                .put(JSONObject().put("protocol", JSONArray().put("dns")).put("action", "hijack-dns"))
                .put(JSONObject().put("ip_is_private", true).put("outbound", "direct"))))

        return config.toString(2)
    }

    private fun monitorSpeed(label: String, processesAlive: () -> Boolean) {
        scope.launch(Dispatchers.IO) {
            val uid = android.os.Process.myUid()
            var lastRx = TrafficStats.getUidRxBytes(uid)
            var lastTx = TrafficStats.getUidTxBytes(uid)
            if (lastRx == TrafficStats.UNSUPPORTED.toLong()) lastRx = TrafficStats.getTotalRxBytes()
            if (lastTx == TrafficStats.UNSUPPORTED.toLong()) lastTx = TrafficStats.getTotalTxBytes()

            while (isActive && processesAlive()) {
                delay(1000)

                var currRx = TrafficStats.getUidRxBytes(uid)
                var currTx = TrafficStats.getUidTxBytes(uid)
                if (currRx == TrafficStats.UNSUPPORTED.toLong()) currRx = TrafficStats.getTotalRxBytes()
                if (currTx == TrafficStats.UNSUPPORTED.toLong()) currTx = TrafficStats.getTotalTxBytes()

                val dfRx = if (currRx > lastRx) currRx - lastRx else 0L
                val dfTx = if (currTx > lastTx) currTx - lastTx else 0L

                withContext(Dispatchers.Main) {
                    VpnManager.updateSpeed(dfRx, dfTx)
                }
                try {
                    val speedText = "↓ ${com.v7lthronyx.v7lpanel.util.TrafficFormatter.formatSpeed(dfRx)}   ↑ ${com.v7lthronyx.v7lpanel.util.TrafficFormatter.formatSpeed(dfTx)}"
                    val nm = getSystemService(android.app.NotificationManager::class.java)
                    nm.notify(NOTIF_ID, buildNotification(label, speedText))
                } catch (e: Exception) {}

                lastRx = currRx
                lastTx = currTx
            }
        }
    }

    private fun stopVpn() {
        try { 
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                tun2socksProcess?.destroyForcibly()
            } else {
                tun2socksProcess?.destroy() 
            }
        } catch (e: Exception) { e.printStackTrace() }
        tun2socksProcess = null
        
        try { 
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                xrayProcess?.destroyForcibly()
            } else {
                xrayProcess?.destroy() 
            }
        } catch (e: Exception) { e.printStackTrace() }
        xrayProcess = null
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                singboxProcess?.destroyForcibly()
            } else {
                singboxProcess?.destroy()
            }
        } catch (e: Exception) { e.printStackTrace() }
        singboxProcess = null
        try { tfd?.close() } catch (e: Exception) { e.printStackTrace() }
        tfd = null
        scope.coroutineContext.cancelChildren()
        VpnManager.updateStatus(VpnStatus.DISCONNECTED)
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (e: Exception) {
            try { stopForeground(true) } catch (e2: Exception) {}
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        try { 
            if (android.os.Build.VERSION.SDK_INT >= 26) tun2socksProcess?.destroyForcibly() else tun2socksProcess?.destroy() 
        } catch (e: Exception) {}
        try { 
            if (android.os.Build.VERSION.SDK_INT >= 26) xrayProcess?.destroyForcibly() else xrayProcess?.destroy() 
        } catch (e: Exception) {}
        try {
            if (android.os.Build.VERSION.SDK_INT >= 26) singboxProcess?.destroyForcibly() else singboxProcess?.destroy()
        } catch (e: Exception) {}
        try { tfd?.close() } catch (e: Exception) {}
        scope.cancel()
        if (VpnManager.status.value != VpnStatus.DISCONNECTED) {
            VpnManager.updateStatus(VpnStatus.DISCONNECTED)
        }
    }

    private fun clearCloExec(javaFd: FileDescriptor) {
        try {
            clearCloExecCompat(javaFd)
        } catch (_: Exception) {
        }
    }

    private fun clearCloExecCompat(javaFd: FileDescriptor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val flags = Os.fcntlInt(javaFd, OsConstants.F_GETFD, 0)
            Os.fcntlInt(javaFd, OsConstants.F_SETFD, flags and OsConstants.FD_CLOEXEC.inv())
            return
        }

        @Suppress("PrivateApi")
        val method = Os::class.java.getDeclaredMethod(
            "fcntlInt",
            FileDescriptor::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        val flags = method.invoke(null, javaFd, OsConstants.F_GETFD, 0) as Int
        method.invoke(null, javaFd, OsConstants.F_SETFD, flags and OsConstants.FD_CLOEXEC.inv())
    }

    /**
     * Resolve binary: first try jniLibs (lib{name}.so), then extract from assets.
     */
    private fun resolveBinary(assetDir: String, name: String): String? {
        // 1. Try jniLibs. Lib names are hyphenless (e.g. "sing-box" ->
        //    libsingbox.so) so every Android version extracts them reliably.
        val libName = "lib${name.replace("-", "")}.so"
        val nativeLib = File(applicationInfo.nativeLibraryDir, libName)
        if (nativeLib.isFile && nativeLib.length() > 10_000L) return nativeLib.absolutePath

        // 2. Extract from assets. Pick the best-matching ABI, but degrade
        //    gracefully: if a native binary for this ABI isn't bundled, fall
        //    back to arm64 (modern x86 emulators/devices run it via ARM
        //    binary translation).
        val abi = Build.SUPPORTED_ABIS.firstOrNull()
        val preferred = when {
            abi?.startsWith("arm64") == true   -> "arm64"
            abi?.startsWith("armeabi") == true -> "arm"
            abi?.startsWith("x86_64") == true  -> "x86_64"
            abi == "x86" -> "x86"
            else -> "arm64"
        }
        // Ordered candidates: preferred first, then sensible fallbacks.
        val candidates = (listOf(preferred) + listOf("x86_64", "arm64", "arm")).distinct()
        val suffix = candidates.firstOrNull { assetExists("$assetDir/$name-$it") } ?: "arm64"
        val assetName = "$assetDir/$name-$suffix"
        val dest = File(codeCacheDir, name)
        try {
            if (!dest.exists() || dest.length() < 100_000L) {
                assets.open(assetName).use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
            }
            makeExecutable(dest)
            if (!dest.canExecute()) return null
        } catch (e: Exception) {
            VpnManager.updateStatus(VpnStatus.ERROR, "$name extraction failed: ${e.message}")
            return null
        }
        return dest.absolutePath
    }

    /** True if an asset path exists (used to choose a bundled ABI binary). */
    private fun assetExists(path: String): Boolean = try {
        assets.open(path).use { true }
    } catch (_: Exception) { false }

    private fun makeExecutable(f: File) {
        val mode755 = OsConstants.S_IRWXU or OsConstants.S_IRGRP or OsConstants.S_IXGRP or
            OsConstants.S_IROTH or OsConstants.S_IXOTH
        try { Os.chmod(f.absolutePath, mode755) }
        catch (_: ErrnoException) {
            Runtime.getRuntime().exec(arrayOf("/system/bin/chmod", "755", f.absolutePath)).waitFor()
        }
        f.setReadable(true, true)
        f.setExecutable(true, true)
    }

    /**
     * Build Xray JSON config.
     *
     * Architecture: Xray runs as SOCKS5+HTTP inbound on localhost.
     * tun2socks bridges TUN fd → SOCKS5 proxy.
     * DNS: Xray handles DNS via DoH through proxy + direct UDP fallback.
     */
    private fun buildXrayConfig(
        outboundJson: String, chainUri: String, customDns: String, enableIpv6: Boolean,
        fragmentEnabled: Boolean = false, fragmentPackets: String = "tlshello",
        fragmentLength: String = "100-200", fragmentInterval: String = "10-20"
    ): String {
        val proxyOutbound = JSONObject(outboundJson)
        
        val outboundsArray = JSONArray()
        outboundsArray.put(proxyOutbound)

        if (chainUri.isNotBlank()) {
            val parsedChain = ConfigParser.parse(chainUri, "chain-proxy")
            if (parsedChain != null && parsedChain.xrayOutboundJson.isNotBlank()) {
                val chainOutbound = JSONObject(parsedChain.xrayOutboundJson)
                outboundsArray.put(chainOutbound)
                
                // Route main proxy through chain proxy
                val pSet = JSONObject().put("tag", "chain-proxy")
                proxyOutbound.put("proxySettings", pSet)
            }
        }

        val config = JSONObject()

        // Log
        config.put("log", JSONObject()
            .put("loglevel", "warning")
            .put("access", File(codeCacheDir, "xray-access.log").absolutePath)
            .put("error", File(codeCacheDir, "xray.log").absolutePath))

        // Inbounds: SOCKS5 + HTTP on localhost
        config.put("inbounds", JSONArray()
            .put(JSONObject()
                .put("tag", "socks-in")
                .put("protocol", "socks")
                .put("listen", "127.0.0.1")
                .put("port", SOCKS_PORT)
                .put("settings", JSONObject()
                    .put("udp", true))
                .put("sniffing", JSONObject()
                    .put("enabled", true)
                    .put("destOverride", JSONArray().put("http").put("tls"))))
            .put(JSONObject()
                .put("tag", "http-in")
                .put("protocol", "http")
                .put("listen", "127.0.0.1")
                .put("port", HTTP_PORT)))

        // Outbounds: proxy + direct + block + dns
        outboundsArray.put(JSONObject()
                .put("tag", "direct")
                .put("protocol", "freedom")
                .put("settings", JSONObject()))
            .put(JSONObject()
                .put("tag", "block")
                .put("protocol", "blackhole")
                .put("settings", JSONObject()))
            .put(JSONObject()
                .put("tag", "dns-out")
                .put("protocol", "dns"))

        config.put("outbounds", outboundsArray)

        // DNS: use TCP/DoH to bypass proxy UDP restrictions
        config.put("dns", JSONObject()
            .put("servers", JSONArray()
                .put("https://8.8.8.8/dns-query")
                .put("https://1.1.1.1/dns-query")
                .put("localhost")))

        // Routing – use explicit CIDRs instead of geoip.dat / geosite.dat
        config.put("routing", JSONObject()
            .put("domainStrategy", "AsIs")
            .put("rules", JSONArray()
                .put(JSONObject()
                    .put("type", "field")
                    .put("port", 53)
                    .put("network", "udp,tcp")
                    .put("outboundTag", "dns-out"))
                .put(JSONObject()
                    .put("type", "field")
                    .put("ip", JSONArray()
                        .put("10.0.0.0/8")
                        .put("172.16.0.0/12")
                        .put("192.168.0.0/16")
                        .put("127.0.0.0/8")
                        .put("100.64.0.0/10")
                        .put("169.254.0.0/16")
                        .put("fc00::/7"))
                    .put("outboundTag", "direct"))
                .put(JSONObject()
                    .put("type", "field")
                    .put("inboundTag", JSONArray().put("socks-in").put("http-in"))
                    .put("outboundTag", "proxy"))))

        // Fragment TCP settings
        if (fragmentEnabled) {
            val fragmentObj = JSONObject()
            fragmentObj.put("packets", fragmentPackets)
            fragmentObj.put("length", fragmentLength)
            fragmentObj.put("interval", fragmentInterval)
            val freedomOut = JSONObject()
            freedomOut.put("tag", "fragment")
            freedomOut.put("protocol", "freedom")
            freedomOut.put("settings", JSONObject().put("fragment", fragmentObj))
            outboundsArray.put(freedomOut)
            proxyOutbound.optJSONObject("streamSettings")?.put("sockopt", JSONObject().put("dialerProxy", "fragment"))
        }

        return config.toString(2)
    }

    private fun startForegroundWithType(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun buildNotification(label: String, status: String): Notification {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(NOTIF_CHANNEL, "VPN", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply {
                        description = "Spiritus connection"
                        setShowBadge(true)
                    }
            )
        }
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, V7LVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val b = Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(getString(R.string.vpn_notif_title, label))
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setContentIntent(pi)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_vpn_notification),
                    getString(R.string.vpn_notif_disconnect),
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            b.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return b.build()
    }
}
