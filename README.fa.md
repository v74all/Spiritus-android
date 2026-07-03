# Spiritus — کلاینت اندروید

[English](README.md) | فارسی

کلاینت رسمی اندروید برای [پنل VPN اسپیریتوس](https://github.com/v74all/Spiritus). یک اپ برای هر نوع کاربر: **مشترک** که یک لینک کانفیگ رو ایمپورت می‌کنه، **نماینده** که کاربرهای خودش رو مدیریت می‌کنه، یا **ادمین** که کل پنل رو از روی گوشی اداره می‌کنه.

با Kotlin، Jetpack Compose، و موتور تانلینگ sing-box/Xray ساخته شده.

## چیکار می‌کنه

- **حالت مشترک** — یک لینک اشتراک اسپیریتوس رو پیست یا اسکن کن (`v7l://import…` یا آدرس مستقیم پنل `/sub/UUID`) و اپ تمام کانفیگ‌هایی که پنل برای اون حساب ساخته رو می‌گیره، با تشخیص خودکار پروتکل.
- **پروتکل‌ها** — VMess، VLESS (شامل Reality)، Trojan، Shadowsocks / SS2022، Hysteria2، TUIC، WireGuard — برای جدول کامل پارامترهای هر پروتکل به [FEATURES_FA.md](FEATURES_FA.md) نگاه کن.
- **دور زدن فیلترینگ** — فرگمنت‌سازی TCP، بسته‌های نویز، جعل اثر انگشت TLS، تونل تقسیم (Split tunneling)، قطع‌کننده اضطراری (Kill switch).
- **کنسول ادمین / نماینده** — با نقش ادمین یا نماینده وارد پنل می‌شه و کاربرها رو مستقیم از اپ مدیریت می‌کنه: ساخت/تمدید/غیرفعال‌سازی، افزودن ترافیک، محدودیت سرعت برای هر کاربر، ریست مصرف، ویرایش یادداشت، مدیریت زیرمجموعه‌های نماینده.
- **امنیت** — پین کردن گواهی (Certificate pinning) برای هر پروفایل سرور ذخیره‌شده، ترافیک فقط روی TLS (`usesCleartextTraffic=false`)، توکن‌ها با رمزنگاری در حالت سکون از طریق Android Keystore (AES-256-GCM، سخت‌افزاری وقتی در دسترس باشه)، بدون بکاپ از داده‌های اپ (`allowBackup=false`).
- **رابط کاربری فارسی‌محور** — پشتیبانی کامل RTL، دوزبانه در سراسر اپ.

## رابطه با پنل

این اپ یک کلاینت خالص API است — تمام منطق حساب/ترافیک/پروتکل توی [پنل اسپیریتوس](https://github.com/v74all/Spiritus) زندگی می‌کنه. اپ به هر نمونه‌ای از پنل که کاربر بهش اشاره کنه وصل می‌شه (خوداستقراری، پس هیچ آدرس بک‌اند ثابتی توی اپ نیست). برای API سمت سرور به همون ریپو مراجعه کن.

## ساخت (Build)

نیاز به JDK 17 و Android SDK (`minSdk 26`، `targetSdk 35`) داره.

```bash
./gradlew :app:assembleDebug      # بیلد دیباگ بدون امضا
./gradlew :app:testDebugUnitTest  # تست‌های واحد
```

### باینری‌های موتور VPN (native)

پوشه‌ی `app/native-bin/` باینری‌های پیش‌ساخته‌ی `sing-box`، `xray-core` و `tun2socks` رو برای `arm`، `arm64` و `x86_64` (مجموعاً حدود ۲۹۰ مگابایت) داره که مستقیم توی ریپو commit شدن، نه اینکه در زمان build دانلود بشن. **`sing-box` نسخه‌ی stock نیست** — یک پچ سورسی روش اعمال شده (`singbox/sing-box-android-vpn-fd.patch`) که یک گزینه‌ی `file_descriptor` به تنظیمات tun اضافه می‌کنه تا سرویس VPN بتونه یک فایل‌دیسکریپتور از قبل بازشده رو از API اندرویدی `VpnService` بهش بده، بدون نیاز به باز کردن دوباره‌ی `/dev/tun` (که نیاز به روت داره). اگه بخوای دوباره بسازیشون:

- `sing-box`: پچ روی `sagernet/sing-box` نسخه‌ی `v1.13.13` (کامیت `78b2e12`) اعمال شده
- `xray-core`: از `xtls/xray-core` بدون اطلاعات VCS جاسازی‌شده build شده — اگه دوباره می‌سازی، به یک ریلیز مشخص و اخیر پین کن
- `tun2socks`: `xjasonlyu/tun2socks` نزدیک نسخه‌ی `v2.6.0` (کامیت `4127937`)

### بیلد release امضاشده

امضای release کاملاً با متغیرهای محیطی کنترل می‌شه — **هیچ keystore یا رمزی هیچ‌وقت داخل این ریپو نیست** (به بخش [امنیت](#امنیت) پایین نگاه کن):

```bash
export V7L_KEYSTORE_PATH=/path/to/your.jks
export V7L_KEYSTORE_PASSWORD=...
export V7L_KEY_ALIAS=...
export V7L_KEY_PASSWORD=...
./gradlew :app:assembleRelease
```

بدون تنظیم هر چهار متغیر، `assembleRelease` سریع با خطای واضح fail می‌شه، به‌جای اینکه بی‌صدا یک artifact بدون امضا یا امضاشده با کلید دیباگ بسازه.

## ساختار پروژه

```
app/src/main/java/com/v7lthronyx/v7lpanel/
├── data/           # کلاینت API (Ktor)، دیتابیس Room، تنظیمات DataStore، توکن‌استور مبتنی بر Keystore
├── domain/         # use caseها
├── ui/screens/      # یک پکیج برای هر صفحه (home، locations، management، profile، settings، ...)
├── ui/components/   # کامپوننت‌های مشترک
├── vpn/             # پارس کانفیگ + سرویس VPN sing-box/Xray
└── util/            # SafeLog (لاگر با حذف داده‌ی حساس)، فرمت‌کننده‌ها، ابزارهای کمکی
```

## امنیت

- **هیچ‌وقت `keystore/` یا فایل `.jks`/`.keystore`/`keystore.properties` رو commit نکن** — `.gitignore` از قبل این‌ها رو مستثنی کرده، ولی قبل از هر `add -f` روی این مسیرها دوباره چک کن. هرکی به کلید release دسترسی داشته باشه می‌تونه یک آپدیت جعلی امضا کنه که برای نصب‌های موجود معتبر به‌نظر برسه.
- تمام لاگ‌ها از `util/SafeLog.kt` رد می‌شن که قبل از رسیدن به Logcat، URLها، UUIDها، پین‌های TLS و توکن‌های bearer رو حذف می‌کنه. یک تست واحد (`AppContractTest`) اگه `android.util.Log` مستقیم خارج از این فایل استفاده بشه، بیلد رو fail می‌کنه.
- برای گزارش آسیب‌پذیری‌ها همون روال [پروژه‌ی پنل](https://github.com/v74all/Spiritus/blob/main/SECURITY.md) رو دنبال کن.

## لایسنس

MIT — به [LICENSE](LICENSE) نگاه کن.
