# HubChat Android SDK

ویجت چت پشتیبانی هاب‌چت برای اپ‌های اندروید (Kotlin/Java)، با همان ظاهر و امکانات ویجت سایت. حداقل `minSdk 21`.

## امکانات
- دکمه‌ی شناور با رنگ، آیکن، متن و جایگاهی که در پنل هاب‌چت تنظیم کرده‌اید
- پنل گفتگو با همان ظاهر و امکانات ویجت سایت: ارسال فایل و عکس، پایگاه دانش، ساعات کاری و نظرسنجی رضایت
- شمارنده‌ی پیام‌های خوانده‌نشده روی دکمه، حتی وقتی پنل بسته است
- تغییر ظاهر از پنل، بدون نیاز به انتشار نسخه‌ی جدید اپ
- پیام‌های کاربران در صندوق ورودی هاب‌چت، کنار تلگرام، بله، روبیکا و بقیه‌ی کانال‌ها

## کلید کانال
کلید را از پنل هاب‌چت بگیرید: «تنظیمات > کانال‌ها > افزودن کانال > اپلیکیشن موبایل». کلید با `hc_` شروع می‌شود و در صفحه‌ی همان کانال، کنار نمونه‌کد نصب، نمایش داده می‌شود.

## نصب
از JitPack:

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

// app/build.gradle.kts
implementation("com.github.HubChat-ai:HubChat-android:1.0.1")
```

## استفاده
```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        HubChat.init(this, "hc_xxxxxxxxxxxxxxxxxxxxxxxx")
    }
}

// دکمه‌ی شناور روی صفحه:
HubChatLauncher.attach(this)            // در onCreate اکتیویتی
// یا در XML: <ir.hubchat.sdk.HubChatLauncher android:layout_gravity="bottom|start" ... />

// باز کردن از دکمه‌ی خودتان:
HubChat.open(context)

// تعداد خوانده‌نشده:
HubChat.addListener(object : HubChatListener { override fun onUnreadChanged(count: Int) { } })
```

## پشتیبانی
- مستندات: https://hubchat.ir/docs/connect-channel/mobile_sdk
- گزارش مشکل: بخش Issues همین مخزن
- وب‌سایت: https://hubchat.ir
