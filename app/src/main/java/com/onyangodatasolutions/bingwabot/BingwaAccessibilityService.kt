
package com.onyangodatasolutions.bingwabot

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class BingwaAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i("BingwaBot", "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Foundation for future, user-authorized phone automation.
        // No buttons are clicked and no USSD requests are sent yet.
    }

    override fun onInterrupt() {
        Log.i("BingwaBot", "Accessibility service interrupted")
    }
}
