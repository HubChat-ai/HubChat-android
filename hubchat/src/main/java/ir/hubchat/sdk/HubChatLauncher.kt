package ir.hubchat.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.net.URL
import kotlin.concurrent.thread

/**
 * Floating launcher button matching the website widget. Add it to your layout
 * (e.g. bottom|start of a FrameLayout/CoordinatorLayout) or call HubChatLauncher.attach(activity).
 */
class HubChatLauncher @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs), HubChatListener {
    private val pill = LinearLayout(context)
    private val icon = ImageView(context)
    private val label = TextView(context)
    private val badge = TextView(context)

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    init {
        clipChildren = false
        clipToPadding = false
        visibility = GONE
        pill.orientation = LinearLayout.HORIZONTAL
        pill.layoutDirection = View.LAYOUT_DIRECTION_RTL
        pill.gravity = Gravity.CENTER
        pill.minimumWidth = dp(60)
        pill.elevation = dp(8).toFloat()
        pill.contentDescription = "گفتگوی آنلاین"
        pill.setOnClickListener { HubChat.open(context) }
        icon.setImageResource(R.drawable.hubchat_ic_chat)
        pill.addView(icon, LinearLayout.LayoutParams(dp(28), dp(28)))
        label.setTypeface(label.typeface, Typeface.BOLD)
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        label.visibility = GONE
        pill.addView(label, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(10) })
        addView(pill, LayoutParams(-2, dp(60)).apply { setMargins(dp(4), dp(4), dp(4), dp(4)) })

        badge.setTextColor(Color.WHITE)
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        badge.setTypeface(badge.typeface, Typeface.BOLD)
        badge.gravity = Gravity.CENTER
        badge.minWidth = dp(22)
        badge.setPadding(dp(6), 0, dp(6), 0)
        badge.elevation = dp(9).toFloat()
        badge.background = GradientDrawable().apply { cornerRadius = dp(11).toFloat(); setColor(Color.parseColor("#ef4444")); setStroke(dp(2), Color.WHITE) }
        badge.visibility = GONE
        addView(badge, LayoutParams(-2, dp(22)))

        HubChat.whenSettings(::apply)
    }

    private fun apply(settings: HubChatSettings) {
        val light = blend(settings.color, Color.WHITE, 0.18f)
        pill.background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(light, settings.color)).apply { cornerRadius = dp(999).toFloat() }
        pill.setPadding(if (settings.launcherText != null) dp(16) else 0, 0, if (settings.launcherText != null) dp(16) else 0, 0)
        icon.setColorFilter(settings.onColor)
        settings.launcherText?.let { label.text = it; label.setTextColor(settings.onColor); label.visibility = VISIBLE }
        (badge.layoutParams as LayoutParams).gravity = Gravity.TOP or if (settings.isRight) Gravity.LEFT else Gravity.RIGHT
        settings.launcherImage?.let { loadImage(it, settings.launcherText != null) }
        visibility = VISIBLE
        scaleX = 0.6f; scaleY = 0.6f; alpha = 0f
        animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(450).start()
        onUnreadChanged(HubChat.unread)
    }

    private fun loadImage(url: String, small: Boolean) {
        thread {
            val bitmap: Bitmap? = runCatching { URL(url).openStream().use(BitmapFactory::decodeStream) }.getOrNull()
            Handler(Looper.getMainLooper()).post {
                bitmap ?: return@post
                val size = dp(if (small) 44 else 60)
                icon.clearColorFilter()
                icon.layoutParams = LinearLayout.LayoutParams(size, size)
                icon.scaleType = ImageView.ScaleType.CENTER_CROP
                icon.clipToOutline = true
                icon.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT) }
                icon.outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                icon.setImageBitmap(bitmap)
            }
        }
    }

    override fun onUnreadChanged(count: Int) {
        badge.text = (if (count > 9) "9+" else count.toString()).map { if (it.isDigit()) "۰۱۲۳۴۵۶۷۸۹"[it - '0'] else it }.joinToString("")
        badge.visibility = if (count > 0) VISIBLE else GONE
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        HubChat.addListener(this)
    }

    override fun onDetachedFromWindow() {
        HubChat.removeListener(this)
        super.onDetachedFromWindow()
    }

    private fun blend(from: Int, to: Int, ratio: Float): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * ratio).toInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * ratio).toInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * ratio).toInt(),
    )

    companion object {
        /** Adds the launcher floating above the activity content, on the side set in the panel. */
        @JvmStatic
        @JvmOverloads
        fun attach(activity: android.app.Activity, bottomOffsetDp: Int = 0): HubChatLauncher {
            val root = activity.findViewById<ViewGroup>(android.R.id.content)
            val launcher = HubChatLauncher(activity)
            val params = LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.LEFT)
            params.setMargins(launcher.dp(16), 0, launcher.dp(16), launcher.dp(16 + bottomOffsetDp))
            root.addView(launcher, params)
            HubChat.whenSettings { params.gravity = Gravity.BOTTOM or if (it.isRight) Gravity.RIGHT else Gravity.LEFT; launcher.layoutParams = params }
            return launcher
        }
    }
}
