package com.v7lthronyx.v7lpanel.ui.screens.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v7lthronyx.v7lpanel.ui.components.*
import com.v7lthronyx.v7lpanel.ui.theme.*

data class HelpSection(
    val icon: ImageVector,
    val titleFa: String,
    val titleEn: String,
    val items: List<HelpItem>
)

data class HelpItem(
    val fa: String,
    val en: String
)

private val helpContent = listOf(
    HelpSection(
        icon = Icons.Filled.Login,
        titleFa = "ورود و احراز هویت",
        titleEn = "Login & Authentication",
        items = listOf(
            HelpItem("رمز عبور ادمین را وارد کنید تا به پنل دسترسی داشته باشید.",
                     "Enter the admin password to access the panel."),
            HelpItem("بعد از ۵ ورود ناموفق، حساب به مدت ۱۵ دقیقه قفل می‌شود.",
                     "After 5 failed attempts, account locks for 15 minutes."),
            HelpItem("تایمر قفل را در صفحه ورود مشاهده کنید.",
                     "A live countdown timer appears during lockout."),
            HelpItem("مدیران فرعی (Agent) با نام کاربری و رمز عبور وارد می‌شوند.",
                     "Agents log in with their username and password."),
            HelpItem("کاربران اشتراک (Subscriber) با UUID خود وارد می‌شوند.",
                     "Subscribers access their page using their UUID."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.Dns,
        titleFa = "افزودن سرور",
        titleEn = "Adding a Server",
        items = listOf(
            HelpItem("روی دکمه + در صفحه انتخاب سرور ضربه بزنید.",
                     "Tap the + button on the server selection screen."),
            HelpItem("آدرس سرور را وارد کنید. مثال: https://1.2.3.4:8080",
                     "Enter server URL. Example: https://1.2.3.4:8080"),
            HelpItem("می‌توانید آدرس را از کلیپ‌بورد پیست کنید (دکمه 📋 کنار URL).",
                     "Paste URL from clipboard using the 📋 button next to the URL field."),
            HelpItem("نوع نقش را انتخاب کنید: Admin / Agent / Subscriber",
                     "Select the role: Admin / Agent / Subscriber"),
            HelpItem("می‌توانید چندین سرور اضافه کرده و بین آنها سوئیچ کنید.",
                     "You can add multiple servers and switch between them."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.QrCode,
        titleFa = "اسکنر QR کد",
        titleEn = "QR Code Scanner",
        items = listOf(
            HelpItem("روی آیکون QR در صفحه انتخاب سرور ضربه بزنید.",
                     "Tap the QR icon on the server selection screen."),
            HelpItem("دوربین را روی QR کد سرور یا کانفیگ بگیرید.",
                     "Point camera at a server or config QR code."),
            HelpItem("QR کد اتوماتیک اسکن شده و آدرس پر می‌شود.",
                     "QR code is automatically scanned and URL is filled in."),
            HelpItem("دسترسی به دوربین برای اسکنر لازم است.",
                     "Camera permission is required for the scanner."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.People,
        titleFa = "مدیریت کاربران",
        titleEn = "User Management",
        items = listOf(
            HelpItem("در داشبورد، لیست تمام کاربران را مشاهده کنید.",
                     "View all users in the dashboard."),
            HelpItem("برای افزودن کاربر روی دکمه + کلیک کنید.",
                     "Tap + to add a new user."),
            HelpItem("ساخت چندگانه کاربران با پیشوند و شماره‌گذاری خودکار.",
                     "Bulk-create users with a prefix and auto-numbering."),
            HelpItem("برای حذف کاربر، کارت را به چپ بکشید.",
                     "Swipe card left to delete a user."),
            HelpItem("برای فعال/غیرفعال کردن، کارت را به راست بکشید.",
                     "Swipe card right to toggle a user on/off."),
            HelpItem("برای انتخاب چندگانه، روی کارت و نگه دارید یا از حالت Multi-Select استفاده کنید.",
                     "Long-press a card or use Multi-Select mode to select multiple users."),
            HelpItem("پس از انتخاب چندگانه، می‌توانید همه را یکجا حذف کنید.",
                     "After multi-selecting, bulk-delete all selected users."),
            HelpItem("با Pull-to-Refresh لیست را بروزرسانی کنید.",
                     "Pull down to refresh the user list."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.FilterList,
        titleFa = "جستجو و فیلتر",
        titleEn = "Search & Filter",
        items = listOf(
            HelpItem("از نوار جستجو برای فیلتر فوری کاربران استفاده کنید.",
                     "Use the search bar to instantly filter users."),
            HelpItem("با برچسب‌های فیلتر (All / Active / Online / Expired) کاربران را دسته‌بندی کنید.",
                     "Use filter chips: All / Active / Online / Inactive / Expired / Low Traffic"),
            HelpItem("تعداد کاربران در هر دسته نمایش داده می‌شود.",
                     "Each filter chip shows the count of users in that category."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.Wifi,
        titleFa = "مانیتورینگ زنده",
        titleEn = "Live Monitoring",
        items = listOf(
            HelpItem("سرعت آپلود/دانلود هر کاربر به‌صورت زنده هر ۳ ثانیه بروز می‌شود.",
                     "Each user's upload/download speed updates live every 3 seconds."),
            HelpItem("برای مشاهده ترافیک زنده به تب Monitoring بروید.",
                     "Go to the Monitoring tab for live traffic view."),
            HelpItem("لاگ Kill-Switch برای بررسی قطعی‌های اتوماتیک موجود است.",
                     "Kill-Switch log is available to review auto-disconnections."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.QrCodeScanner,
        titleFa = "کانفیگ و QR کد کاربر",
        titleEn = "User Config & QR Code",
        items = listOf(
            HelpItem("روی کارت کاربر ضربه بزنید تا جزئیات باز شود.",
                     "Tap a user card to open user details."),
            HelpItem("QR کد هر پروتکل (VMess، VLess، Trojan و ...) را مشاهده کنید.",
                     "View QR code for each protocol: VMess, VLess, Trojan, etc."),
            HelpItem("دکمه کپی لینک کانفیگ را در کلیپ‌بورد کپی می‌کند.",
                     "Copy button copies the config link to clipboard."),
            HelpItem("دکمه اشتراک‌گذاری لینک را در هر برنامه‌ای ارسال می‌کند.",
                     "Share button sends the link to any app."),
            HelpItem("صفحه Activity برای هر کاربر: سایت‌های بازدیدشده، اتصالات اخیر و تحلیل ریسک.",
                     "Activity screen per user: visited sites, recent connections, and risk analysis."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.Person,
        titleFa = "صفحه اشتراک کاربر",
        titleEn = "Subscriber Page",
        items = listOf(
            HelpItem("کاربر اشتراک با UUID خود به صفحه مخصوص خود دسترسی دارد.",
                     "Subscriber accesses their personal page via UUID."),
            HelpItem("میزان ترافیک مصرفی، تاریخ انقضا و لینک‌های کانفیگ نمایش داده می‌شود.",
                     "Shows used traffic, expiry date, and config links."),
            HelpItem("QR کد لینک اشتراک برای کپی در برنامه‌های Clash/V2Ray موجود است.",
                     "QR code for subscription URL to import in Clash/V2Ray apps."),
            HelpItem("لینک اشتراک را می‌توانید کپی یا اشتراک‌گذاری کنید.",
                     "Subscription URL can be copied or shared."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.SupportAgent,
        titleFa = "پنل نماینده (Agent)",
        titleEn = "Agent Panel",
        items = listOf(
            HelpItem("نمایندگان سهمیه ترافیک و تعداد کاربران اختصاص داده‌شده را مشاهده می‌کنند.",
                     "Agents see their traffic quota and allocated user count."),
            HelpItem("نماینده می‌تواند کاربران خود را مدیریت کند.",
                     "Agents can manage their own users."),
            HelpItem("نوار پیشرفت سهمیه در بالای صفحه نمایش داده می‌شود.",
                     "Quota progress bar is shown at the top of the screen."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.Settings,
        titleFa = "تنظیمات",
        titleEn = "Settings",
        items = listOf(
            HelpItem("فاصله بروزرسانی خودکار را تنظیم کنید.",
                     "Configure the auto-refresh interval."),
            HelpItem("تنظیمات ذخیره می‌شوند و پس از راه‌اندازی مجدد باقی می‌مانند.",
                     "Settings are persisted and survive app restarts."),
            HelpItem("پشتیبان‌گیری و بازیابی در بخش More → Backups قابل انجام است.",
                     "Backup and restore is available in More → Backups."),
        )
    ),
    HelpSection(
        icon = Icons.Filled.Security,
        titleFa = "امنیت و نشست",
        titleEn = "Security & Session",
        items = listOf(
            HelpItem("در صورت انقضای نشست، به‌صورت خودکار به صفحه ورود هدایت می‌شوید.",
                     "On session expiry, you are automatically redirected to login."),
            HelpItem("اطلاعات نشست در حافظه موقت نگه‌داری می‌شود و پس از حذف برنامه پاک می‌شود.",
                     "Session data is stored in memory and cleared on app uninstall."),
            HelpItem("برای خروج، از دکمه Logout در داشبورد استفاده کنید.",
                     "Use the Logout button in the dashboard to sign out."),
        )
    ),
)

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val lang = LocalLang.current

    V7LPanelBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = V7LColors.t1)
                }
                Text(
                    if (lang == "fa") "راهنمای برنامه" else "App Guide",
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = V7LColors.t0,
                    modifier   = Modifier.weight(1f)
                )
            }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                V7LGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = V7LColors.accentBg.copy(alpha = 0.55f),
                    borderColor = LocalAccent.current.copy(alpha = 0.26f)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Filled.Info, null, tint = LocalAccent.current, modifier = Modifier.size(20.dp))
                        Text(
                            if (lang == "fa")
                                "این راهنما روش استفاده از تمام بخش‌های Spiritus را توضیح می‌دهد."
                            else
                                "This guide explains how to use all sections of Spiritus.",
                            fontFamily = FiraCode, fontSize = 13.sp, color = LocalAccent.current
                        )
                    }
                }
            }

            items(helpContent) { section ->
                HelpSectionCard(section = section, lang = lang)
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
    }
}

@Composable
private fun HelpSectionCard(section: HelpSection, lang: String) {
    val isExpanded = remember { mutableStateOf(true) }

    V7LGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded.value = !isExpanded.value },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(section.icon, null, tint = LocalAccent.current, modifier = Modifier.size(22.dp))
                Text(
                    if (lang == "fa") section.titleFa else section.titleEn,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = V7LColors.t0,
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    if (isExpanded.value) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null, tint = V7LColors.t3, modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded.value) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = V7LColors.border)
                Spacer(Modifier.height(10.dp))
                section.items.forEachIndexed { idx, item ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "•",
                            color = LocalAccent.current,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                        Text(
                            if (lang == "fa") item.fa else item.en,
                            fontFamily = FiraCode,
                            fontSize   = 13.sp,
                            color      = V7LColors.t2,
                            textAlign  = if (lang == "fa") TextAlign.Right else TextAlign.Left,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
