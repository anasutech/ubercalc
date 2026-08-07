package com.rideprofit.calc.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.rideprofit.calc.FareParser
import com.rideprofit.calc.Prefs
import com.rideprofit.calc.RideCalculator

private const val UBER_DRIVER_PACKAGE = "com.ubercab.driver"

class RideAccessibilityService : AccessibilityService() {

    private var lastSignature: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        val pkg = e.packageName?.toString()

        if (pkg != UBER_DRIVER_PACKAGE) {
            OverlayService.hide(this)
            lastSignature = null
            return
        }

        val root = rootInActiveWindow ?: return
        val allText = mutableListOf<String>()
        collectText(root, allText)

        val signature = allText.joinToString("|")
        if (signature == lastSignature) return 

        val parsed = FareParser.parse(allText) ?: return
        lastSignature = signature

        val settings = Prefs.load(this)
        val result = RideCalculator.calculate(parsed.fare, parsed.miles, parsed.minutes, settings)
        val verdict = RideCalculator.verdictLevel(result, settings)

        // إظهار النتيجة فوراً وبدون أي تأخير
        OverlayService.updateResult(this, result, verdict)
    }

    private fun collectText(node: AccessibilityNodeInfo, out: MutableList<String>, depth: Int = 0) {
        if (depth > 40) return

        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { out.add(it) }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collectText(child, out, depth + 1)
            child.recycle()
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
    }
}