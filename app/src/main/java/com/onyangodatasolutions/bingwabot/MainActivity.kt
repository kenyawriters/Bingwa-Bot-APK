
package com.onyangodatasolutions.bingwabot

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private val green = Color.rgb(0, 130, 70)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 32)
            setBackgroundColor(Color.rgb(245, 248, 246))
        }

        val title = TextView(this).apply {
            text = "BINGWA BOT"
            textSize = 30f
            setTextColor(green)
            gravity = Gravity.CENTER
        }
        layout.addView(title)

        val subtitle = TextView(this).apply {
            text = "Safaricom Offers & Customer Management"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 24)
        }
        layout.addView(subtitle)

        addSection(
            layout,
            "Dashboard",
            "Welcome to Bingwa Bot.\nManage your offers and customer orders."
        )

        addButton(layout, "Available Offers") {
            showMessage(
                "Offers",
                "Your offer catalogue will be added next."
            )
        }

        addButton(layout, "Customer Management") {
            showMessage(
                "Customers",
                "Customer records will be added in a later step."
            )
        }

        addButton(layout, "Offer Mapping & Training") {
            showMessage(
                "Offer Mapping",
                "Offer mapping tools will be added in a later step."
            )
        }

        addButton(layout, "Order History") {
            showMessage(
                "Order History",
                "Your order history will appear here."
            )
        }

        addButton(layout, "Settings") {
            showMessage(
                "Settings",
                "App settings will be added in a later step."
            )
        }

        val note = TextView(this).apply {
            text = "Version 1.0 • Bingwa Bot"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, 28, 0, 8)
        }
        layout.addView(note)

        scroll.addView(layout)
        setContentView(scroll)
    }

    private fun addSection(
        layout: LinearLayout,
        heading: String,
        description: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.WHITE)
        }

        val headingView = TextView(this).apply {
            text = heading
            textSize = 20f
            setTextColor(green)
        }
        card.addView(headingView)

        val descriptionView = TextView(this).apply {
            text = description
            textSize = 15f
            setTextColor(Color.DKGRAY)
            setPadding(0, 8, 0, 0)
        }
        card.addView(descriptionView)

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 8, 0, 16)
        layout.addView(card, params)
    }

    private fun addButton(
        layout: LinearLayout,
        label: String,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 4, 0, 8)
        layout.addView(button, params)
    }

    private fun showMessage(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
