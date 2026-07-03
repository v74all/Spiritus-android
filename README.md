# Spiritus — Android Client

Official Android client for the [Spiritus VPN panel](https://github.com/v74all/Spiritus). One app for every kind of user: a **subscriber** importing a config link, an **agent** managing their own customers, or an **admin** running the whole panel — all from the phone.

Built with Kotlin, Jetpack Compose, and sing-box/Xray as the tunneling engine.

## What it does

- **Subscriber mode** — paste or scan a Spiritus subscription link (`v7l://import…` or a plain `/sub/UUID` panel URL) and the app pulls every config the panel generated for that account, auto-detecting the protocol.
- **Protocols** — VMess, VLESS (incl. Reality), Trojan, Shadowsocks / SS2022, Hysteria2, TUIC, WireGuard — see [FEATURES_FA.md](FEATURES_FA.md) for the full per-protocol parameter matrix (Persian).
- **Anti-censorship** — TCP fragmentation, noise packets, TLS fingerprint spoofing, split tunneling, kill switch.
- **Admin / Agent console** — logs into the panel as an admin or agent and manages users directly from the app: create/renew/disable, add traffic, set per-user speed limits, reset usage, edit notes, manage sub-agents.
- **Security** — certificate pinning per saved server profile, TLS-only traffic (`usesCleartextTraffic=false`), tokens encrypted at rest via the Android Keystore (AES-256-GCM, hardware-backed when available), no backup of app data (`allowBackup=false`).
- **Farsi-first UI** — full RTL support, bilingual throughout.

## Relationship to the panel

This app is a pure API client — all account/traffic/protocol logic lives in the [Spiritus panel](https://github.com/v74all/Spiritus). The app talks to whatever panel instance the user points it at (self-hosted, so no fixed backend URL is baked in). See that repo for the server-side API.

## Building

Requires JDK 17 and the Android SDK (`minSdk 26`, `targetSdk 35`).

```bash
./gradlew :app:assembleDebug      # unsigned debug build
./gradlew :app:testDebugUnitTest  # unit tests
```

### Native VPN engine binaries

`app/native-bin/` ships prebuilt `sing-box`, `xray-core`, and `tun2socks` binaries for `arm`, `arm64`, and `x86_64` (~290 MB total), committed directly to this repo rather than fetched at build time. **`sing-box` is not a stock upstream build** — it carries a source patch (`singbox/sing-box-android-vpn-fd.patch`) that adds a `file_descriptor` tun option so the VPN service can hand it an already-open fd from Android's `VpnService` API without re-opening `/dev/tun` (which requires root). If you ever rebuild these:

- `sing-box`: patch applied on top of `sagernet/sing-box` `v1.13.13` (commit `78b2e12`)
- `xray-core`: built from `xtls/xray-core` without embedded VCS info — pin to a specific recent release if rebuilding
- `tun2socks`: `xjasonlyu/tun2socks` around `v2.6.0` (commit `4127937`)

### Signed release build

Release signing is driven entirely by environment variables — **no keystore or password ever lives in this repo** (see [Security](#security) below):

```bash
export V7L_KEYSTORE_PATH=/path/to/your.jks
export V7L_KEYSTORE_PASSWORD=...
export V7L_KEY_ALIAS=...
export V7L_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

Without all four variables set, `assembleRelease` fails fast with a clear error instead of silently producing an unsigned or debug-signed artifact.

## Project structure

```
app/src/main/java/com/v7lthronyx/v7lpanel/
├── data/           # API client (Ktor), Room DB, DataStore settings, Keystore-backed token store
├── domain/         # use cases
├── ui/screens/      # one package per screen (home, locations, management, profile, settings, ...)
├── ui/components/   # shared composables
├── vpn/             # config parsing + sing-box/Xray VPN service
└── util/            # SafeLog (redacting logger), formatters, helpers
```

## Security

- **Never commit `keystore/` or any `.jks`/`.keystore`/`keystore.properties` file** — `.gitignore` already excludes them, but double-check before force-adding anything under that path. Anyone with the release key can sign an update that looks legitimate to existing installs.
- All logging goes through `util/SafeLog.kt`, which redacts URLs, UUIDs, TLS pins, and bearer tokens before anything reaches Logcat. A unit test (`AppContractTest`) fails the build if `android.util.Log` is used directly anywhere outside that file.
- Report vulnerabilities the same way as the [panel project](https://github.com/v74all/Spiritus/blob/main/SECURITY.md).

## License

MIT — see [LICENSE](LICENSE).
