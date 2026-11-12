package ir.hubchat.sdk

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.content.Intent
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Full-screen chat panel hosting the shared widget WebView. */
class HubChatActivity : AppCompatActivity() {
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var container: FrameLayout

    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        fileCallback?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data))
        fileCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        container = FrameLayout(this).apply { fitsSystemWindows = true }
        setContentView(container)

        val webView = HubChat.webView ?: return finish()
        (webView.parent as? ViewGroup)?.removeView(webView)
        container.addView(webView, FrameLayout.LayoutParams(-1, -1))

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url
                // Links to other sites/apps (Telegram, phone, mail...) open outside the panel.
                if (url.host != Uri.parse(HubChat.widgetUrl).host) {
                    runCatching { startActivity(Intent(Intent.ACTION_VIEW, url)) }
                    return true
                }
                return false
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView, callback: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return runCatching { picker.launch(params.createIntent()); true }.getOrElse { fileCallback = null; false }
            }
        }

        HubChat.closeRequest = { finish() }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finish()
        })
        HubChat.opened()
    }

    override fun onDestroy() {
        HubChat.closed()
        HubChat.closeRequest = null
        HubChat.webView?.let { (it.parent as? ViewGroup)?.removeView(it) }
        super.onDestroy()
    }
}
