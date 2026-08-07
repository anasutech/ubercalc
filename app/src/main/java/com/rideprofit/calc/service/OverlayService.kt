package com.rideprofit.calc.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.rideprofit.calc.R
import com.rideprofit.calc.RideCalculator
import com.rideprofit.calc.RideResult
import java.lang.ref.WeakReference

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var params: WindowManager.LayoutParams? = null

    companion object {
        private const val CHANNEL_ID = "ride_overlay_channel"
        private const val NOTIF_ID = 1001

        private var instanceRef: WeakReference<OverlayService>? = null

        fun updateResult(context: Context, result: RideResult, verdict: RideCalculator.Verdict) {
            val running = instanceRef?.get()
            if (running == null) {
                val intent = Intent(context, OverlayService::class.java)
                intent.putExtra("net", result.net)
                intent.putExtra("verdict", verdict.name)
                context.startForegroundService(intent)
            } else {
                running.render(result, verdict)
            }
        }

        fun hide(context: Context) {
            instanceRef?.get()?.removeBubble()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instanceRef = WeakReference(this)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())

        val net = intent?.getDoubleExtra("net", 0.0) ?: 0.0
        val verdictName = intent?.getStringExtra("verdict") ?: "AMBER"
        val verdict = RideCalculator.Verdict.valueOf(verdictName)

        if (bubbleView == null) {
            showBubble()
        }
        renderRaw(net, verdict)

        return START_STICKY
    }

    private fun render(result: RideResult, verdict: RideCalculator.Verdict) {
        renderRaw(result.net, verdict)
    }

    private fun renderRaw(net: Double, verdict: RideCalculator.Verdict) {
        val view = bubbleView ?: return
        val rootLayout = view.findViewById<LinearLayout>(R.id.bubbleRootLayout)
        val tvNetProfit = view.findViewById<TextView>(R.id.tvNetProfit)

        // الألوان حسب تقييم الرحلة: أخضر، أصفر، أحمر
        val backgroundColor = when (verdict) {
            RideCalculator.Verdict.GREEN -> Color.parseColor("#2E7D32") // أخضر
            RideCalculator.Verdict.AMBER -> Color.parseColor("#F9A825") // أصفر
            RideCalculator.Verdict.RED   -> Color.parseColor("#C62828") // أحمر
        }

        val drawable = rootLayout.background
        if (drawable is GradientDrawable) {
            drawable.setColor(backgroundColor)
        } else {
            rootLayout.setBackgroundColor(backgroundColor)
        }

        tvNetProfit.text = "$${"%.2f".format(net)}"
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showBubble() {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.overlay_verdict, null)
        bubbleView = view

        // تفعيل زر الإغلاق اليدوي (X)
        val btnClose = view.findViewById<TextView>(R.id.btnCloseBubble)
        btnClose.setOnClickListener {
            removeBubble()
            stopSelf()
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = 40
        lp.y = 300
        params = lp

        windowManager.addView(view, lp)
        makeDraggable(view, lp)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeDraggable(view: View, lp: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var touchX = 0f
        var touchY = 0f

        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    touchX = event.rawX
                    touchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = initialX + (event.rawX - touchX).toInt()
                    lp.y = initialY + (event.rawY - touchY).toInt()
                    windowManager.updateViewLayout(v, lp)
                    true
                }
                else -> false
            }
        }
    }

    private fun removeBubble() {
        bubbleView?.let {
            runCatching { windowManager.removeView(it) }
            bubbleView = null
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("حاسبة ربح الرحلة شغالة")
            .setContentText("تظهر صافي الربح تلقائيًا للطلبات")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "فقاعة حساب الربح", NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        removeBubble()
        instanceRef = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}