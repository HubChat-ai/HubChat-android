# HubChat Android SDK

ویجت چت پشتیبانی هاب‌چت برای اپ‌های اندروید (Kotlin/Java)، با همان ظاهر و امکانات ویجت سایت. حداقل `minSdk 21`.

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

## نحوه‌ی کار
تنظیمات دکمه از `GET https://widget.hubchat.ir/widget/{key}/config` خوانده و دکمه‌ی شناور به‌صورت نیتیو ساخته می‌شود. خود صفحه‌ی ویجت (`/widget/{key}`)، همان صفحه‌ای که روی سایت در iframe باز می‌شود، در WebView بارگذاری می‌شود؛ پس ظاهر و امکانات پنل دقیقاً مثل سایت است. SDK پیش از بارگذاری صفحه `window.HubChatBridge.postMessage` را تعریف می‌کند و ویجت رویدادهای `ready`، `close` و `unread` را از آن می‌فرستد.
