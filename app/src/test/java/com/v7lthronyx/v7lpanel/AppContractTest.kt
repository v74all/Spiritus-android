package com.v7lthronyx.v7lpanel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AppContractTest {
    private val projectDir: File = findAppProjectDir()
    private val mainDir: File = File(projectDir, "src/main")

    @Test
    fun userFacingLegacyBrandsDoNotReturn() {
        val banned = listOf("VIP VPN", "VIP", "V7L Panel", "Enter Panel")
        val offenders = sourceFiles()
            .flatMap { file ->
                val text = file.readText()
                banned.filter { text.contains(it) }.map { "${file.relativeTo(projectDir).path}: $it" }
            }

        assertTrue("Legacy user-facing brand strings found:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    @Test
    fun spiritusIdentityAndLegacyImportCompatibilityAreDeclared() {
        val manifest = File(mainDir, "AndroidManifest.xml").readText()
        val strings = File(mainDir, "res/values/strings.xml").readText()
        val parser = File(mainDir, "java/com/v7lthronyx/v7lpanel/ui/screens/login/ServerSelectScreen.kt").readText()

        assertTrue(strings.contains("<string name=\"app_name\">Spiritus</string>"))
        assertTrue(manifest.contains("android:scheme=\"spiritus\""))
        assertTrue(manifest.contains("android:scheme=\"v7l\""))
        assertTrue(parser.contains("setOf(\"spiritus\", \"v7l\")"))
    }

    @Test
    fun privacySensitiveDirectAndroidLogsStayOutOfMainSources() {
        val allowed = "java/com/v7lthronyx/v7lpanel/util/SafeLog.kt"
        val offenders = sourceFiles()
            .filter { it.relativeTo(mainDir).path != allowed }
            .filter { file ->
                val text = file.readText()
                text.contains("android.util.Log") || Regex("""\bLog\.[dwiev]""").containsMatchIn(text)
            }
            .map { it.relativeTo(projectDir).path }

        assertTrue("Direct Android Log usage must go through SafeLog:\n${offenders.joinToString("\n")}", offenders.isEmpty())
    }

    @Test
    fun splashStillPurgesLegacyPersistedPasswords() {
        val settings = File(mainDir, "java/com/v7lthronyx/v7lpanel/data/local/SettingsDataStore.kt").readText()
        val splash = File(mainDir, "java/com/v7lthronyx/v7lpanel/ui/screens/splash/SplashScreen.kt").readText()

        assertTrue(settings.contains("suspend fun purgeLegacyPasswords()"))
        assertTrue(settings.contains("it.remove(KEY_SAVED_ADMIN_PASS)"))
        assertTrue(settings.contains("it.remove(KEY_SAVED_AGENT_PASS)"))
        assertTrue(splash.contains("settingsStore.purgeLegacyPasswords()"))
    }

    private fun sourceFiles(): List<File> =
        mainDir.walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "xml") }
            .toList()

    private fun findAppProjectDir(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        repeat(6) {
            if (File(dir, "src/main").exists()) return dir
            if (File(dir, "app/src/main").exists()) return File(dir, "app")
            dir = dir.parentFile ?: return File("app").absoluteFile
        }
        return File("app").absoluteFile
    }
}
