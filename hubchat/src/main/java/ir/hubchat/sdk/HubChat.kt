package ir.hubchat.sdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/** Launcher settings configured in the HubChat panel. */
data class HubChatSettings(
    val color: Int,
    val isRight: Boolean,
    val launcherText: String?,
    val launcherImage: String?,
    val title: String?,
    val subtitle: String?,
) {
    /** Same contrast rule as widget.js. */
    val onColor: Int
        get() {
            fun channel(v: Int): Double = (v / 255.0).let { if (it <= 0.03928) it / 12.92 else Math.pow((it + 0.055) / 1.055, 2.4) }
            val l = 0.2126 * channel(Color.red(color)) + 0.7152 * channel(Color.green(color)) + 0.0722 * channel(Color.blue(color))
            return if (l > 0.45) Color.parseColor("#0b1220") else Color.WHITE
        }
}

interface HubChatListener {
    fun onUnreadChanged(count: Int) {}
    fun onEvent(type: String, payload: JSONObject) {}
}

/**
 * Entry point. Call HubChat.init(context, "hc_...") in Application.onCreate,
 * then add a HubChatLauncher to your layout or call HubChat.open(context).
 */
@SuppressLint("StaticFieldLeak")
object HubChat {
    var baseUrl: String = "https://widget.hubchat.ir"
    lateinit var widgetKey: String
        private set
    var settings: HubChatSettings? = null
        private set
    var unread: Int = 0
        private set
    var isOpen: Boolean = false
        internal set

    private val listeners = mutableSetOf<HubChatListener>()
    private val main = Handler(Looper.getMainLooper())
    private val settingsCallbacks = mutableListOf<(HubChatSettings) -> Unit>()

    /** Kept alive across opens so the conversation and unread events keep running. */
    internal var webView: WebView? = null
        private set

    val widgetUrl: String get() = "${baseUrl.trimEnd('/')}/widget/$widgetKey"

    @JvmStatic
    @JvmOverloads
    fun init(context: Context, key: String, baseUrl: String = this.baseUrl) {
        widgetKey = key
        this.baseUrl = baseUrl
        thread { fetchSettings() }
        main.postDelayed({ ensureWebView(context.applicationContext) }, 2500)
    }

    @JvmStatic
    fun open(context: Context) {
        ensureWebView(context.applicationContext)
        context.startActivity(Intent(context, HubChatActivity::class.java).apply {
            if (context !is android.app.Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun addListener(listener: HubChatListener) = listeners.add(listener)
    fun removeListener(listener: HubChatListener) = listeners.remove(listener)

    internal fun whenSettings(callback: (HubChatSettings) -> Unit) {
        settings?.let(callback) ?: settingsCallbacks.add(callback)
    }

    private var screen: JSONObject? = null

    /**
     * Tells operators which app screen the user is on (shown next to the chat and in
     * the panel's visit stats). Call it from each Activity/Fragment or your navigation listener.
     */
    @JvmStatic
    @JvmOverloads
    fun setScreen(name: String, title: String? = null) {
        val path = name.trim().trimStart('/')
        if (path.isEmpty()) return
        screen = JSONObject().put("source", "hubchat-loader").put("type", "page")
            .put("url", "app://screen/" + android.net.Uri.encode(path, "/"))
            .put("title", title ?: name)
        main.post { sendScreen() }
    }

    internal fun sendScreen() {
        val payload = screen ?: return
        webView?.evaluateJavascript("window.postMessage($payload,'*');", null)
    }

    internal fun command(type: String) {
        webView?.evaluateJavascript("window.postMessage({source:'hubchat-loader',type:'$type'},'*');", null)
    }

    internal fun opened() {
        isOpen = true
        setUnread(0)
        command("open")
    }

    internal fun closed() {
        isOpen = false
        command("closed")
    }

    internal var closeRequest: (() -> Unit)? = null

    private fun setUnread(count: Int) {
        unread = count
        listeners.forEach { it.onUnreadChanged(count) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun ensureWebView(context: Context) {
        if (webView != null) return
        webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            setBackgroundColor(Color.parseColor("#f4f6fa"))
            addJavascriptInterface(Bridge(), "HubChatBridge")
            loadUrl(widgetUrl)
        }
    }

    private fun fetchSettings() {
        try {
            val connection = URL("$widgetUrl/config").openConnection() as HttpURLConnection
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body).getJSONObject("settings")
            fun text(key: String) = json.optString(key, "").takeIf { it.isNotBlank() && it != "null" }
            val parsed = HubChatSettings(
                color = runCatching { Color.parseColor(json.optString("color", "#2563eb")) }.getOrDefault(Color.parseColor("#2563eb")),
                isRight = json.optString("position") == "right",
                launcherText = text("launcher_text"),
                launcherImage = text("launcher_image"),
                title = text("title"),
                subtitle = text("subtitle"),
            )
            main.post {
                settings = parsed
                settingsCallbacks.forEach { it(parsed) }
                settingsCallbacks.clear()
            }
        } catch (_: Exception) {
        }
    }

    private class Bridge {
        @JavascriptInterface
        fun postMessage(raw: String) {
            val data = runCatching { JSONObject(raw) }.getOrNull() ?: return
            if (data.optString("source") != "hubchat") return
            main.post {
                when (val type = data.optString("type")) {
                    "close" -> closeRequest?.invoke()
                    "ready" -> {
                        sendScreen()
                        if (isOpen) command("open")
                    }
                    "unread" -> if (!isOpen) setUnread(data.optInt("count", unread + 1))
                    else -> Unit
                }
                listeners.forEach { it.onEvent(data.optString("type"), data) }
            }
        }
    }
}
