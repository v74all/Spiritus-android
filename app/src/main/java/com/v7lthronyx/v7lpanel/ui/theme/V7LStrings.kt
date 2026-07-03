package com.v7lthronyx.v7lpanel.ui.theme

import androidx.compose.runtime.*
import com.v7lthronyx.v7lpanel.data.local.SettingsDataStore
import kotlinx.coroutines.flow.map

/** App-wide language state: "en" or "fa" */
val LocalLang = compositionLocalOf { "en" }

fun s(en: String, fa: String): @Composable () -> String = @Composable {
    val lang = LocalLang.current
    if (lang == "fa") fa else en
}

/**
 * Central string table. Access: `S.serverConnections` (returns the string for the current language).
 */
object S {
    // ── General ──
    val appName          @Composable get() = "Spiritus"
    val version          @Composable get() = "v2.0"
    val ok               @Composable get() = if (LocalLang.current == "fa") "باشه" else "OK"
    val cancel           @Composable get() = if (LocalLang.current == "fa") "انصراف" else "Cancel"
    val save             @Composable get() = if (LocalLang.current == "fa") "ذخیره" else "Save"
    val add              @Composable get() = if (LocalLang.current == "fa") "افزودن" else "Add"
    val delete           @Composable get() = if (LocalLang.current == "fa") "حذف" else "Delete"
    val back             @Composable get() = if (LocalLang.current == "fa") "بازگشت" else "Back"
    val retry            @Composable get() = if (LocalLang.current == "fa") "تلاش مجدد" else "Retry"
    val copy             @Composable get() = if (LocalLang.current == "fa") "کپی" else "Copy"
    val share            @Composable get() = if (LocalLang.current == "fa") "اشتراک‌گذاری" else "Share"
    val loading          @Composable get() = if (LocalLang.current == "fa") "در حال بارگذاری..." else "Loading..."
    val offline          @Composable get() = if (LocalLang.current == "fa") "آفلاین" else "offline"
    val noExpiry         @Composable get() = if (LocalLang.current == "fa") "بدون انقضا" else "No expiry"
    val expired          @Composable get() = if (LocalLang.current == "fa") "منقضی شده" else "Expired"

    // ── Server Select ──
    val serverConnections   @Composable get() = if (LocalLang.current == "fa") "اتصالات سرور" else "Server Connections"
    val selectServer        @Composable get() = if (LocalLang.current == "fa") "کانفیگ، اشتراک یا پنل ادمین/نماینده اضافه کنید" else "Add a config, subscription, or admin/agent panel"
    val emptyServerHint     @Composable get() = if (LocalLang.current == "fa") "کانفیگ vless/vmess/… یا لینک پنل را Paste کنید، یا QR بزنید" else "Paste a vless/vmess/… config or a panel link, or scan a QR"
    val addServer           @Composable get() = if (LocalLang.current == "fa") "افزودن سرور" else "Add Server"
    val pasteLink           @Composable get() = if (LocalLang.current == "fa") "وارد کردن لینک یا آدرس سرور" else "Enter your server link or URL"
    val linkUrl             @Composable get() = if (LocalLang.current == "fa") "لینک / آدرس" else "Link / URL"
    val serverName          @Composable get() = if (LocalLang.current == "fa") "نام" else "Name"
    val subscriber          @Composable get() = if (LocalLang.current == "fa") "سابسکرایبر" else "Subscriber"
    val admin               @Composable get() = if (LocalLang.current == "fa") "ادمین" else "Admin"
    val agent               @Composable get() = if (LocalLang.current == "fa") "نماینده" else "Agent"
    val agentUsername       @Composable get() = if (LocalLang.current == "fa") "نام نماینده" else "Agent Username"
    val advancedSettings    @Composable get() = if (LocalLang.current == "fa") "تنظیمات بیشتر ▼" else "More settings ▼"
    val advancedSettingsUp  @Composable get() = if (LocalLang.current == "fa") "بستن تنظیمات ▲" else "Less settings ▲"

    // ── QR Scanner ──
    val scanQr              @Composable get() = if (LocalLang.current == "fa") "اسکن کد QR" else "Scan QR Code"
    val cameraPermNeeded    @Composable get() = if (LocalLang.current == "fa") "دسترسی به دوربین لازم است" else "Camera permission required"
    val grantPermission     @Composable get() = if (LocalLang.current == "fa") "مجوز دادن" else "Grant"

    // ── Login ──
    val password            @Composable get() = if (LocalLang.current == "fa") "رمز عبور" else "Password"
    val login               @Composable get() = if (LocalLang.current == "fa") "ورود" else "Login"
    val adminLogin          @Composable get() = if (LocalLang.current == "fa") "ورود ادمین" else "Admin Login"

    // ── Bottom Nav ──
    val users               @Composable get() = if (LocalLang.current == "fa") "کاربران" else "Users"
    val monitor             @Composable get() = if (LocalLang.current == "fa") "نظارت" else "Monitor"
    val settings            @Composable get() = if (LocalLang.current == "fa") "تنظیمات" else "Settings"
    val more                @Composable get() = if (LocalLang.current == "fa") "بیشتر" else "More"
    val info                @Composable get() = if (LocalLang.current == "fa") "اطلاعات" else "Info"

    // ── More Screen ──
    val agentsResellers     @Composable get() = if (LocalLang.current == "fa") "نمایندگان و فروشندگان" else "Agents & Resellers"
    val userGroups          @Composable get() = if (LocalLang.current == "fa") "گروه‌های کاربری" else "User Groups"
    val backups             @Composable get() = if (LocalLang.current == "fa") "پشتیبان‌گیری" else "Backups"
    val killSwitchLog       @Composable get() = if (LocalLang.current == "fa") "لاگ کیل سوییچ" else "Kill-Switch Log"
    val help                @Composable get() = if (LocalLang.current == "fa") "راهنما" else "Help"
    val appSettings         @Composable get() = if (LocalLang.current == "fa") "تنظیمات برنامه" else "App Settings"

    // ── Subscription ──
    val subUrl              @Composable get() = if (LocalLang.current == "fa") "لینک اشتراک" else "Subscription URL"
    val copySubUrl          @Composable get() = if (LocalLang.current == "fa") "کپی لینک اشتراک" else "Copy Sub URL"
    val expiresIn           @Composable get() = if (LocalLang.current == "fa") "باقی‌مانده:" else "Expires in"
    val days                @Composable get() = if (LocalLang.current == "fa") "روز" else "days"

    // ── Protocol Settings ──
    val saveSettings        @Composable get() = if (LocalLang.current == "fa") "ذخیره تنظیمات" else "SAVE SETTINGS"
    val security            @Composable get() = if (LocalLang.current == "fa") "امنیت" else "Security"
    val killSwitch          @Composable get() = if (LocalLang.current == "fa") "کیل سوییچ" else "Kill Switch"
    val killSwitchDesc      @Composable get() = if (LocalLang.current == "fa") "غیرفعال‌سازی خودکار اتصال مشکوک" else "Auto-disable suspicious connections"
    val changePassword      @Composable get() = if (LocalLang.current == "fa") "تغییر رمز" else "Change Password"
    val currentPassword     @Composable get() = if (LocalLang.current == "fa") "رمز فعلی" else "Current Password"
    val newPassword         @Composable get() = if (LocalLang.current == "fa") "رمز جدید" else "New Password"
    val confirmPassword     @Composable get() = if (LocalLang.current == "fa") "تکرار رمز" else "Confirm Password"
    val min8Chars           @Composable get() = if (LocalLang.current == "fa") "حداقل ۸ کاراکتر" else "Min 8 characters"
    val passwordsMismatch   @Composable get() = if (LocalLang.current == "fa") "رمزها مطابقت ندارند" else "Passwords don't match"
    val change              @Composable get() = if (LocalLang.current == "fa") "تغییر" else "Change"
    val fragment            @Composable get() = if (LocalLang.current == "fa") "فرگمنت" else "Fragment"
    val fragmentDesc        @Composable get() = if (LocalLang.current == "fa") "تکه‌تکه‌سازی TCP جهت عبور از فیلتر" else "Client-side TCP fragmentation to bypass DPI"
    val mux                 @Composable get() = if (LocalLang.current == "fa") "مالتی‌پلکس" else "MUX"
    val concurrency         @Composable get() = if (LocalLang.current == "fa") "همزمانی" else "Concurrency"

    // ── App Settings ──
    val language            @Composable get() = if (LocalLang.current == "fa") "زبان" else "Language"
    val english             @Composable get() = "English"
    val farsi               @Composable get() = "فارسی"
    val about               @Composable get() = if (LocalLang.current == "fa") "درباره" else "About"
    val appVersion          @Composable get() = if (LocalLang.current == "fa") "نسخه برنامه" else "App Version"
}
