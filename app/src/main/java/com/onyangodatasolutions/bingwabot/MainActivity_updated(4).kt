package com.onyangodatasolutions.bingwabot

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : Activity() {

    private val green = Color.rgb(0, 130, 70)
    private val darkGreen = Color.rgb(0, 90, 48)
    private val grey = Color.rgb(110, 110, 110)
    private val lightGrey = Color.rgb(242, 244, 245)
    private val red = Color.rgb(190, 45, 45)
    private val white = Color.WHITE
    private val black = Color.rgb(30, 30, 30)
    private val blue = Color.rgb(10, 120, 210)

    private val prefs by lazy {
        getSharedPreferences("bingwa_bot_data", MODE_PRIVATE)
    }

    private var currentPage = "Home"
    private var offerCategory = "All"
    private var transactionFilter: String? = null

    // Keep the drawer order aligned with the supplied reference design.
    private val menuItems = listOf(
        "Home", "Offers", "Statistics", "Site Link", "Quick Dial",
        "Auto Reply", "Relay", "Client Management", "Bulk SMS Sender",
        "MPESA Messages", "USSD Scheduler", "Profile", "Settings", "Logout"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPage("Home")
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun makeText(
        value: String,
        size: Float = 15f,
        color: Int = black,
        bold: Boolean = false
    ): TextView {
        return TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(dp(3), dp(5), dp(3), dp(5))
        }
    }

    private fun makeButton(
        label: String,
        color: Int = green,
        action: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            setTextColor(white)
            setBackgroundColor(color)
            isAllCaps = false
            setOnClickListener { action() }
        }
    }

    private fun addGap(parent: LinearLayout, height: Int = 8) {
        parent.addView(View(this), LinearLayout.LayoutParams(1, dp(height)))
    }

    private fun addLine(parent: LinearLayout) {
        val line = View(this)
        line.setBackgroundColor(Color.LTGRAY)
        parent.addView(
            line,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            )
        )
    }

    private fun setQueueMode(enabled: Boolean) {
        prefs.edit().putBoolean("queue_mode", enabled).apply()
    }

    private fun isQueueModeEnabled(): Boolean =
        prefs.getBoolean("queue_mode", false)

    private fun processQueuedMessagesIfAvailable(enabled: Boolean) {
        if (enabled) {
            val queuedCount = readList("messages").size
            toast(
                "Queue mode ON: UI state updated. No live SMS/USSD queue backend is connected yet. " +
                    "${queuedCount} saved message record(s) are ready for integration."
            )
        } else {
            toast("Queue mode OFF: incoming messages stay queued for later processing.")
        }
    }

    private fun showPage(page: String) {
        currentPage = page
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(lightGrey)
            setPadding(dp(14), dp(10), dp(14), dp(14))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        header.addView(makeButton("☰ Menu", darkGreen) {
            showNavigation()
        })

        val headingLabel = if (page == "Home") "Dashboard" else page
        val heading = makeText(headingLabel, 20f, green, true)
        heading.gravity = Gravity.CENTER
        header.addView(
            heading,
            LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            )
        )

        if (page == "Home") {
            val avatar = TextView(this).apply {
                val businessName = prefs.getString("business_name", "Onyango Data Solutions")
                    ?: "Onyango Data Solutions"
                val initials = businessName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                text = if (initials.isNotBlank()) initials.take(2) else "O"
                setTextColor(white)
                textSize = 14f
                gravity = Gravity.CENTER
                setPadding(dp(10), dp(8), dp(10), dp(8))
                val bg = GradientDrawable().apply {
                    setColor(darkGreen)
                    cornerRadius = dp(20).toFloat()
                }
                background = bg
            }
            header.addView(avatar)
            header.addView(makeButton("⚙", darkGreen) {
                showPage("Settings")
            })
        }

        root.addView(header)
        addLine(root)
        if (page != "Home") {
            root.addView(makeText(page, 22f, darkGreen, true))
        }

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(body)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
        )

        when (page) {
            "Home" -> buildHome(body)
            "Offers" -> buildOffers(body)
            "Client Management" -> buildRecords(body, "clients")
            "MPESA Messages" -> buildRecords(body, "messages")
            "Quick Dial" -> buildRecords(body, "quickdial")
            "Auto Reply" -> buildRecords(body, "autoreply")
            "Offer Mapping" -> buildRecords(body, "mappings")
            "Order History" -> buildRecords(body, "orders")
            "Statistics" -> buildStatistics(body)
            "Settings" -> buildSettings(body)
        }

        setContentView(root)
    }

    private fun showNavigation() {
        val dialog = Dialog(this)
        val screenWidth = resources.displayMetrics.widthPixels
        val drawer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        // Branded green header at the top of the drawer.
        val brandHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(22), dp(14), dp(20))
            setBackgroundColor(darkGreen)
        }
        val logo = TextView(this).apply {
            text = "B"
            textSize = 34f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(darkGreen)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.WHITE)
                setStroke(dp(3), green)
            }
        }
        brandHeader.addView(logo, LinearLayout.LayoutParams(dp(76), dp(76)))
        val brandText = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
        }
        brandText.addView(makeText("Bingwa Bot", 21f, Color.WHITE, true))
        brandText.addView(makeText("Data | Bundles | Airtime | More", 12f, Color.WHITE))
        val live = TextView(this).apply {
            text = "●  LIVE"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(5), dp(10), dp(5))
            background = GradientDrawable().apply {
                setColor(green)
                cornerRadius = dp(24).toFloat()
                setStroke(dp(1), Color.rgb(130, 235, 180))
            }
        }
        brandText.addView(live)
        brandText.addView(makeText("✎  Tap to edit your business details", 12f, Color.WHITE))
        brandHeader.addView(brandText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        drawer.addView(brandHeader)

        val scroll = ScrollView(this)
        val menuColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        scroll.addView(menuColumn)

        // Keep the primary navigation order requested for Bingwa Bot.
        // Other existing sections remain implemented and are not deleted.
        val drawerItems = listOf(
            Pair("⌂", "Home"),
            Pair("◆", "Offers"),
            Pair("♟", "Client Management"),
            Pair("▣", "MPESA Messages"),
            Pair("☎", "Quick Dial"),
            Pair("●", "Auto Reply"),
            Pair("▥", "Statistics")
        )
        drawerItems.forEachIndexed { index, item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(12), dp(8), dp(12))
                if (currentPage == item.second) {
                    background = GradientDrawable().apply {
                        setColor(Color.rgb(215, 247, 233))
                        cornerRadius = dp(16).toFloat()
                    }
                }
            }
            val glyph = makeText(item.first, 25f, green, true)
            glyph.gravity = Gravity.CENTER
            row.addView(glyph, LinearLayout.LayoutParams(dp(42), dp(34)))
            val label = makeText(item.second, 16f, black, true)
            label.setPadding(dp(10), 0, 0, 0)
            row.addView(label, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            if (item.second == "MPESA Messages") {
                val newBadge = makeText("New", 12f, Color.WHITE, true)
                newBadge.gravity = Gravity.CENTER
                newBadge.setPadding(dp(10), dp(5), dp(10), dp(5))
                newBadge.background = GradientDrawable().apply {
                    setColor(green)
                    cornerRadius = dp(20).toFloat()
                }
                row.addView(newBadge)
            } else {
                row.addView(makeText("›", 27f, darkGreen, false))
            }
            row.setOnClickListener {
                dialog.dismiss()
                when (item.second) {
                    "Site Link" -> showInfoPage("Site Link", "Add your business website or offer-page URL here.")
                    "Relay" -> showInfoPage("Relay", "Relay controls will be available here.")
                    "Bulk SMS Sender" -> showInfoPage("Bulk SMS Sender", "Bulk SMS tools will be available here.")
                    "USSD Scheduler" -> showInfoPage("USSD Scheduler", "Scheduled USSD offer controls will be available here.")
                    "Profile" -> showInfoPage("Profile", "Agent profile details are available from the dashboard profile editor.")
                    "Logout" -> AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("There is no account session to log out of yet.")
                        .setPositiveButton("OK", null)
                        .show()
                    else -> showPage(item.second)
                }
            }
            menuColumn.addView(row)
        }

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(190, 235, 215))
        }
        menuColumn.addView(divider, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1)))
        listOf(Pair("♟", "Profile"), Pair("⚙", "Settings"), Pair("⇥", "Logout")).forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(12), dp(8), dp(12))
            }
            row.addView(makeText(item.first, 25f, if (item.second == "Logout") red else green, true),
                LinearLayout.LayoutParams(dp(42), dp(34)))
            row.addView(makeText(item.second, 16f, black, true),
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(makeText(if (item.second == "Logout") "" else "›", 27f, darkGreen))
            row.setOnClickListener {
                dialog.dismiss()
                when (item.second) {
                    "Profile" -> showInfoPage("Profile", "Agent profile details are available from the dashboard profile editor.")
                    "Settings" -> showPage("Settings")
                    else -> AlertDialog.Builder(this).setTitle("Logout")
                        .setMessage("There is no account session to log out of yet.")
                        .setPositiveButton("OK", null).show()
                }
            }
            menuColumn.addView(row)
        }

        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.rgb(215, 247, 233))
                cornerRadius = dp(16).toFloat()
            }
        }
        footer.addView(makeText("⬟  Bingwa Bot", 15f, darkGreen, true))
        footer.addView(makeText("     Fast  •  Secure  •  Reliable", 12f, darkGreen))
        val appVersion = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "1.0.0"
        footer.addView(makeText("Bingwa Bot v$appVersion", 11f, darkGreen).apply {
            gravity = Gravity.CENTER_HORIZONTAL
        })
        val footerWrap = LinearLayout(this).apply {
            setPadding(dp(14), dp(8), dp(14), dp(14))
            addView(footer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        drawer.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        drawer.addView(footerWrap)
        dialog.setContentView(drawer)
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout((screenWidth * 0.80f).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.START)
            addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            attributes = attributes.apply { dimAmount = 0.38f }
        }
        dialog.show()
        dialog.window?.setLayout((screenWidth * 0.80f).toInt(), ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setGravity(Gravity.START)
    }

    private fun showInfoPage(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun greetingText(): String {
        val hour = SimpleDateFormat("H", Locale.getDefault()).format(Date()).toIntOrNull() ?: 0
        return when (hour) {
            in 0..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    private fun maskPhone(value: String): String {
        val digits = value.filter { it.isDigit() }
        return if (digits.length <= 4) {
            value.ifBlank { "No phone" }
        } else {
            "***${digits.takeLast(4)}"
        }
    }

    private fun buildHome(body: LinearLayout) {
        val queueEnabled = isQueueModeEnabled()
        val queueCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(white)
                cornerRadius = dp(14).toFloat()
            }
        }

        val queueRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        queueRow.addView(makeText("Queue Mode", 16f, black, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        val queueSwitch = Switch(this).apply {
            isChecked = queueEnabled
            setOnCheckedChangeListener { _, isChecked ->
                setQueueMode(isChecked)
                processQueuedMessagesIfAvailable(isChecked)
                showPage("Home")
            }
        }
        queueRow.addView(queueSwitch)
        queueCard.addView(queueRow)

        if (queueEnabled) {
            queueCard.addView(makeText("ON • Green", 13f, green, true))
            queueCard.addView(makeText("LIVE - Processing", 13f, green, true))
        } else {
            queueCard.addView(makeText("OFF • Green", 13f, green, true))
            queueCard.addView(makeText("App Paused - Queuing messages", 13f, green, true))
        }
        body.addView(queueCard)
        addGap(body, 10)

        val profileCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E8F5E9"))
                cornerRadius = dp(14).toFloat()
            }
        }

        val profileRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val businessName = prefs.getString("business_name", "Onyango Data Solutions") ?: "Onyango Data Solutions"
        val initials = businessName.split(" ")
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")

        val avatarView = TextView(this).apply {
            text = if (initials.isNotBlank()) initials.take(2) else "O"
            setTextColor(darkGreen)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = GradientDrawable().apply {
                setColor(white)
                cornerRadius = dp(28).toFloat()
            }
        }
        profileRow.addView(avatarView)

        val accountInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val agentName = prefs.getString("agent_name", "Joshua Onyango") ?: "Joshua Onyango"
        accountInfo.addView(makeText(agentName, 17f, darkGreen, true))
        accountInfo.addView(makeText(businessName, 14f, grey))
        profileRow.addView(
            accountInfo,
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        )

        val statusBadge = TextView(this).apply {
            text = if (queueEnabled) "LIVE" else "PAUSED"
            setTextColor(white)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            background = GradientDrawable().apply {
                setColor(green)
                cornerRadius = dp(8).toFloat()
            }
        }
        profileRow.addView(statusBadge)
        profileCard.addView(profileRow)
        profileCard.addView(makeText(greetingText(), 13f, black))
        profileCard.addView(makeButton("Edit Agent Profile", darkGreen) {
            editAgentProfile()
        })
        addGap(profileCard, 8)

        val balancePairs = listOf(
            Triple("Airtime Balance", "balance_airtime", "show_airtime"),
            Triple("Bonga Points", "balance_bonga", "show_bonga"),
            Triple("Tokens", "balance_tokens", "show_tokens"),
            Triple("Commission", "balance_commission", "show_commission")
        )

        for (index in balancePairs.indices step 2) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            for (offset in 0..1) {
                val pairIndex = index + offset
                if (pairIndex >= balancePairs.size) break
                val (label, key, visibleKey) = balancePairs[pairIndex]
                val hidden = !prefs.getBoolean(visibleKey, true)
                val rawValue = prefs.getString(key, "") ?: ""
                val displayValue = when {
                    rawValue.isBlank() -> "Not connected"
                    hidden -> "••••"
                    else -> rawValue
                }

                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = GradientDrawable().apply {
                        setColor(white)
                        cornerRadius = dp(10).toFloat()
                    }
                }

                val textBlock = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                textBlock.addView(makeText(label, 12f, grey))
                textBlock.addView(makeText(displayValue, 15f, black, true))
                item.addView(
                    textBlock,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                )

                val eye = Button(this).apply {
                    text = if (hidden) "🙈" else "👁"
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        prefs.edit().putBoolean(visibleKey, !hidden).apply()
                        showPage("Home")
                    }
                }
                item.addView(eye)

                row.addView(
                    item,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        rightMargin = dp(8)
                    }
                )
            }
            profileCard.addView(row)
            addGap(profileCard, 6)
        }
        body.addView(profileCard)
        addGap(body, 10)

        // Dashboard transaction summary cards.
        val allDashboardOrders = readList("orders")
        val doneCount = allDashboardOrders.count {
            it.optString("status", "pending").lowercase(Locale.ROOT) in listOf("done", "success", "completed")
        }
        val failedCount = allDashboardOrders.count {
            it.optString("status", "pending").lowercase(Locale.ROOT) in listOf("failed", "error")
        }
        val pendingCount = allDashboardOrders.count {
            it.optString("status", "pending").lowercase(Locale.ROOT) in listOf("pending", "processing", "")
        }
        val unmatchedCount = allDashboardOrders.count {
            it.optString("status", "").lowercase(Locale.ROOT) in
                listOf("unmatched", "unmatched offer", "unmatched_offer", "unmatched-offer")
        }

        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun addStatCard(label: String, count: Int, accent: Int) {
            val statCard = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dp(5), dp(10), dp(5), dp(10))
                background = GradientDrawable().apply {
                    setColor(white)
                    setStroke(dp(1), Color.parseColor("#C8E6C9"))
                    cornerRadius = dp(12).toFloat()
                }
            }
            statCard.addView(makeText(label, 11f, darkGreen, true).apply {
                gravity = Gravity.CENTER
            })
            statCard.addView(makeText(count.toString(), 21f, accent, true).apply {
                gravity = Gravity.CENTER
            })
            statsRow.addView(
                statCard,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(5)
                }
            )
        }

        addStatCard("Done", doneCount, green)
        addStatCard("Failed", failedCount, green)
        addStatCard("Pending", pendingCount, green)
        addStatCard("Unmatched Offers", unmatchedCount, darkGreen)
        body.addView(statsRow)
        addGap(body, 10)

        val filterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        fun filterButton(label: String, status: String, color: Int): Button {
            val selected = transactionFilter == status
            return Button(this).apply {
                text = label
                setTextColor(if (selected) white else color)
                setBackgroundColor(if (selected) color else white)
                isAllCaps = false
                setOnClickListener {
                    transactionFilter = if (transactionFilter == status) null else status
                    showPage("Home")
                }
            }
        }

        filterRow.addView(filterButton("Done", "done", Color.parseColor("#0B7D2E")), LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(6) })
        filterRow.addView(filterButton("Failed", "failed", Color.parseColor("#EF4444")), LinearLayout.LayoutParams(0, dp(42), 1f).apply { rightMargin = dp(6) })
        filterRow.addView(filterButton("Pending", "pending", Color.parseColor("#F97316")), LinearLayout.LayoutParams(0, dp(42), 1f))
        body.addView(filterRow)
        addGap(body, 8)

        val recentHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        recentHeader.addView(makeText("Recent Transactions", 16f, darkGreen, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        recentHeader.addView(Button(this).apply {
            text = "See all"
            setBackgroundColor(Color.TRANSPARENT)
            setTextColor(darkGreen)
            isAllCaps = false
            setOnClickListener { showPage("Order History") }
        })
        body.addView(recentHeader)
        addGap(body, 6)

        val allOrders = readList("orders")
        val orders = if (transactionFilter == null) {
            allOrders
        } else {
            allOrders.filter { it.optString("status", "pending").lowercase(Locale.ROOT) == transactionFilter }
        }

        if (orders.isEmpty()) {
            body.addView(makeText("No transactions yet", 14f, grey))
        } else {
            orders.forEach { order ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = GradientDrawable().apply {
                        setColor(white)
                        cornerRadius = dp(10).toFloat()
                    }
                }

                val customerName = order.optString("customerName", order.optString("name", "Unnamed customer"))
                val phone = order.optString("phone", order.optString("msisdn", ""))
                val amount = order.optString("amount", order.optString("price", "0"))
                val packageName = order.optString("package", order.optString("offer", "Offer"))
                val status = order.optString("status", "pending").lowercase(Locale.ROOT)

                val initials = customerName.split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                    .joinToString("")
                val avatarBadge = TextView(this).apply {
                    text = if (initials.isNotBlank()) initials.take(2) else "U"
                    setTextColor(white)
                    gravity = Gravity.CENTER
                    setPadding(dp(10), dp(8), dp(10), dp(8))
                    background = GradientDrawable().apply {
                        setColor(darkGreen)
                        cornerRadius = dp(18).toFloat()
                    }
                }
                card.addView(avatarBadge)

                val info = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                info.addView(makeText(customerName, 14f, black, true))
                info.addView(makeText(maskPhone(phone) + " • $packageName", 12f, grey))
                card.addView(
                    info,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(8) }
                )

                val right = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.END
                }
                right.addView(makeText("KSh $amount", 13f, black, true))
                val badgeColor = when (status) {
                    "done", "success" -> Color.parseColor("#0B7D2E")
                    "failed", "error" -> Color.parseColor("#EF4444")
                    else -> Color.parseColor("#F97316")
                }
                val badgeText = status.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                right.addView(TextView(this).apply {
                    text = badgeText
                    setTextColor(white)
                    setPadding(dp(8), dp(4), dp(8), dp(4))
                    background = GradientDrawable().apply {
                        setColor(badgeColor)
                        cornerRadius = dp(8).toFloat()
                    }
                })
                card.addView(right)
                body.addView(card)
                addGap(body, 7)
            }
        }

        body.addView(makeText(
            "Actual USSD execution and background SMS automation require additional implementation.",
            13f, grey
        ))
    }

    private fun editAgentProfile() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        val agentInput = EditText(this).apply {
            hint = "Agent name"
            setText(prefs.getString("agent_name", "Joshua Onyango"))
            isSingleLine = true
        }
        val businessInput = EditText(this).apply {
            hint = "Business name"
            setText(prefs.getString("business_name", "Onyango Data Solutions"))
            isSingleLine = true
        }
        layout.addView(makeText("Agent name", 13f, darkGreen, true))
        layout.addView(agentInput)
        layout.addView(makeText("Business name", 13f, darkGreen, true))
        layout.addView(businessInput)

        AlertDialog.Builder(this)
            .setTitle("Edit Agent Profile")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val agent = agentInput.text.toString().trim()
                val business = businessInput.text.toString().trim()
                prefs.edit()
                    .putString("agent_name", agent.ifBlank { "Joshua Onyango" })
                    .putString("business_name", business.ifBlank { "Onyango Data Solutions" })
                    .apply()
                showPage("Home")
                toast("Agent profile saved")
            }
            .show()
    }

    // OFFERS SCREEN
    private fun buildOffers(body: LinearLayout) {
        val offers = readList("offers")
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        toolbar.addView(View(this), LinearLayout.LayoutParams(0, dp(38), 1f))
        toolbar.addView(makeButton("⇧", darkGreen) { importOffers() }.apply { contentDescription = "Import offers" })
        toolbar.addView(makeButton("⇩", darkGreen) { exportOffers() }.apply { contentDescription = "Export offers" })
        toolbar.addView(makeButton("+", green) { editOffer(null) }.apply { contentDescription = "Add offer" })
        body.addView(toolbar)
        body.addView(makeText("Saved: ${offers.size}  •  Active: ${offers.count { isOfferActive(it) }}", 14f, green, true))
        addGap(body, 6)
        val categories = listOf("All", "Data", "SMS", "Minutes", "Others")
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categories.forEach { category ->
            categoryRow.addView(Button(this).apply {
                text = category; textSize = 10f; isAllCaps = false
                setTextColor(if (offerCategory == category) white else green)
                setBackgroundColor(if (offerCategory == category) green else white)
                setOnClickListener { offerCategory = category; showPage("Offers") }
            }, LinearLayout.LayoutParams(0, dp(44), 1f))
        }
        body.addView(categoryRow); addGap(body, 8)
        val filtered = offers.filter { offerCategory == "All" || it.optString("category", "Others") == offerCategory }
        if (filtered.isEmpty()) body.addView(makeText("No offers saved in this category yet.", 14f, grey))
        filtered.forEach { offer ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10))
                background = GradientDrawable().apply { setColor(white); cornerRadius = dp(12).toFloat(); setStroke(dp(1), Color.rgb(220,235,225)) }
            }
            val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val price = offer.optString("customerPrice", offer.optString("price", "0"))
            val oldPrice = offer.optString("oldPrice", "")
            details.addView(makeText(offer.optString("name", "Unnamed offer"), 16f, black, true))
            details.addView(makeText("${offer.optString("category", "Others")} • ${offer.optString("validity", "")}", 12f, grey))
            if (oldPrice.isNotBlank() && oldPrice != price) {
                details.addView(makeText("Was KSh $oldPrice  |  Now KSh $price", 14f, green, true))
                details.addView(makeText("Discount: ${discountPercent(oldPrice, price)}%", 12f, grey))
            } else details.addView(makeText("Customer price: KSh $price", 14f, green, true))
            top.addView(details, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val enabled = offer.optBoolean("enabled", false)
            top.addView(TextView(this).apply {
                text = "⏻"; textSize = 28f; gravity = Gravity.CENTER; setTextColor(if (enabled) green else grey)
                setPadding(dp(10), dp(4), dp(10), dp(4)); contentDescription = if (enabled) "Turn off offer" else "Turn on offer"
                setOnClickListener { offer.put("enabled", !enabled); upsertRecord("offers", offer); showPage("Offers"); toast(if (!enabled) "Offer switched ON" else "Offer switched OFF") }
            })
            card.addView(top)
            val cost = offer.optString("carrierCost", "").ifBlank { "Not set" }
            val commission = offer.optString("commission", "").ifBlank { "Not set" }
            card.addView(makeText("Carrier cost: ${if (cost == "Not set") cost else "KSh $cost"}  •  Commission: ${if (commission == "Not set") commission else "KSh $commission"}", 13f, black))
            card.addView(makeText("Payment: ${offer.optString("paymentMethod", "Not set")}  •  Execution SIM: ${offer.optString("executionSim", "SIM 1")}", 12f, grey))
            card.addView(makeText("Retries: ${offer.optInt("retryCount", 0)}  •  Alternative code: ${offer.optString("alternativeCode", "Not set")}", 12f, grey))
            card.addView(makeText("USSD: ${offer.optString("code").ifBlank { "Not configured" }}", 12f, grey))
            card.addView(makeText(if (offer.optBoolean("scheduleEnabled", false)) "Daily schedule: ${offer.optString("startTime")}–${offer.optString("endTime")} (Africa/Nairobi)" else "Daily schedule: disabled", 12f, grey))
            val mapStatus = offer.optString("mappingStatus", "Mapping pending")
            card.addView(makeText("${if (mapStatus == "Verified") "✓ " else ""}$mapStatus", 13f, when(mapStatus) { "Verified" -> green; "Needs update" -> red; else -> grey }, true))
            card.addView(makeText("Status: ${if (isOfferActive(offer)) "ACTIVE NOW" else if (enabled) "ON, OUTSIDE SCHEDULE" else "OFF"}", 12f, if (isOfferActive(offer)) green else grey, true))
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row1.addView(makeButton("Copy USSD", darkGreen) {
                val code = offer.optString("code")
                if (code.isBlank()) toast("Add a USSD code first") else {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("USSD", code)); toast("USSD code copied")
                }
            }, LinearLayout.LayoutParams(0, dp(44), 1f))
            row1.addView(makeButton("Auto Time", darkGreen) { editOfferSchedule(offer) }, LinearLayout.LayoutParams(0, dp(44), 1f))
            card.addView(row1)
            val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            row2.addView(makeButton("Set Up Mapping", green) { showTrainingMenu(offer) }, LinearLayout.LayoutParams(0, dp(44), 1.4f))
            row2.addView(makeButton("Edit", darkGreen) { editOffer(offer) }, LinearLayout.LayoutParams(0, dp(44), .8f))
            row2.addView(makeButton("Delete", red) {
                AlertDialog.Builder(this).setTitle("Delete offer?").setMessage("Delete ${offer.optString("name")} and its mapping?")
                    .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                        deleteRecord("offers", offer.optString("id"))
                        prefs.edit().remove(trainingKey(offer)).remove("tracer_${offer.optString("id")}").apply(); showPage("Offers")
                    }.show()
            }, LinearLayout.LayoutParams(0, dp(44), .8f))
            card.addView(row2); body.addView(card); addGap(body, 10)
        }
    }

    private fun editOfferSchedule(offer: JSONObject) {
        val enabled = EditText(this).apply { hint = "Enabled? yes/no"; setText(if (offer.optBoolean("scheduleEnabled", false)) "yes" else "no"); isSingleLine = true }
        val start = EditText(this).apply { hint = "Start time HH:mm"; setText(offer.optString("startTime", "23:00")); isSingleLine = true }
        val end = EditText(this).apply { hint = "End time HH:mm"; setText(offer.optString("endTime", "16:00")); isSingleLine = true }
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)); addView(makeText("Time zone: Africa/Nairobi", 13f, darkGreen, true)); addView(enabled); addView(start); addView(end) }
        AlertDialog.Builder(this).setTitle("Auto Time: ${offer.optString("name")}").setView(layout).setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val st = start.text.toString().trim(); val en = end.text.toString().trim()
                val active = enabled.text.toString().trim().equals("yes", true) || enabled.text.toString().trim().equals("true", true)
                if (active && (!validTime(st) || !validTime(en))) { toast("Use valid times such as 23:00 and 16:00"); return@setPositiveButton }
                offer.put("scheduleEnabled", active); offer.put("startTime", st); offer.put("endTime", en); offer.put("scheduleTimeZone", "Africa/Nairobi")
                upsertRecord("offers", offer); showPage("Offers"); toast("Schedule saved")
            }.show()
    }

    private fun exportOffers() {
        val array = JSONArray(); readList("offers").forEach { array.put(it) }; val json = array.toString(2)
        val input = EditText(this).apply { setText(json); minLines = 7; gravity = Gravity.TOP }
        AlertDialog.Builder(this).setTitle("Export offers").setMessage("Copy this JSON to back up or transfer offers.")
            .setView(ScrollView(this).apply { addView(input) }).setPositiveButton("Copy JSON") { _, _ ->
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Bingwa Bot offers", json)); toast("Offer JSON copied")
            }.setNegativeButton("Close", null).show()
    }

    private fun importOffers() {
        val input = EditText(this).apply { hint = "Paste exported offers JSON here"; minLines = 6; gravity = Gravity.TOP }
        AlertDialog.Builder(this).setTitle("Import offers").setMessage("Paste a JSON array exported from Bingwa Bot.")
            .setView(input).setNegativeButton("Cancel", null).setPositiveButton("Import") { _, _ ->
                try {
                    val array = JSONArray(input.text.toString())
                    if (array.length() == 0) { toast("No offers found"); return@setPositiveButton }
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        if (item.optString("id").isBlank()) item.put("id", "import_${System.currentTimeMillis()}_$i")
                        if (!item.has("enabled")) item.put("enabled", false)
                        upsertRecord("offers", item)
                    }
                    showPage("Offers"); toast("Offers imported")
                } catch (_: Exception) { toast("Invalid JSON. No offers imported.") }
            }.show()
    }

    private fun editOffer(existing: JSONObject?) {
        val isNew = existing == null
        val originalCode = existing?.optString("code", "") ?: ""
        val record = existing ?: JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("enabled", false); put("category", "Data")
            put("price", ""); put("customerPrice", ""); put("carrierCost", ""); put("commission", "")
            put("paymentMethod", "Till"); put("executionSim", "SIM 1"); put("ussdMode", "Simple")
            put("retryCount", 0); put("alternativeCode", ""); put("oldPrice", ""); put("validity", "")
            put("code", ""); put("repeatable", false); put("scheduleEnabled", false)
            put("startTime", "23:00"); put("endTime", "16:00"); put("mappingStatus", "Mapping pending")
        }
        val fields = listOf(
            "name" to "Offer name",
            "category" to "Category: Data, SMS, Minutes, Others",
            "price" to "Customer selling price (KSh)",
            "customerPrice" to "Customer price override (optional)",
            "carrierCost" to "Safaricom/carrier cost (KSh)",
            "commission" to "Commission (KSh; blank if zero/unknown)",
            "paymentMethod" to "Payment method: Till, PayBill or Both",
            "executionSim" to "Execution SIM: SIM 1 or SIM 2",
            "ussdMode" to "USSD mode: Simple or Advanced",
            "retryCount" to "Automatic retry count (0 or more)",
            "alternativeCode" to "Alternative offer/code (optional)",
            "oldPrice" to "Old price (optional)",
            "validity" to "Validity",
            "code" to "USSD template (BA = recipient phone number)",
            "repeatable" to "Repeatable? yes/no",
            "scheduleEnabled" to "Enable daily schedule? yes/no",
            "startTime" to "Daily start time, HH:mm",
            "endTime" to "Daily end time, HH:mm"
        )
        val inputs = LinkedHashMap<String, EditText>()
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(6), dp(16), dp(6)) }
        val scroll = ScrollView(this); scroll.addView(layout)
        fields.forEach { (key, label) ->
            layout.addView(makeText(label, 13f, darkGreen, true))
            val value = when (key) {
                "repeatable", "scheduleEnabled" -> if (record.optBoolean(key, false)) "yes" else "no"
                "customerPrice" -> record.optString(key, "")
                "price" -> record.optString("price", record.optString("customerPrice", ""))
                "retryCount" -> record.optString(key, "0")
                else -> record.optString(key, "")
            }
            val input = EditText(this).apply { isSingleLine = key != "code"; textSize = 15f; setText(value); hint = label }
            inputs[key] = input; layout.addView(input)
        }
        AlertDialog.Builder(this).setTitle(if (isNew) "Add Offer" else "Edit Offer").setView(scroll)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create().apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        for ((key, _) in fields) {
                            val value = inputs[key]?.text?.toString()?.trim().orEmpty()
                            when (key) {
                                "repeatable", "scheduleEnabled" -> record.put(key, value.equals("yes", true) || value.equals("true", true))
                                "category" -> {
                                    val normalized = value.lowercase(Locale.ROOT)
                                    if (normalized !in listOf("data", "sms", "minutes", "others", "other")) {
                                        toast("Choose Data, SMS, Minutes or Others"); return@setOnClickListener
                                    }
                                    record.put(key, if (normalized == "other") "Others" else normalized.replaceFirstChar { it.uppercase() })
                                }
                                "retryCount" -> {
                                    val retries = value.toIntOrNull()
                                    if (retries == null || retries !in 0..20) { toast("Retry count must be from 0 to 20"); return@setOnClickListener }
                                    record.put(key, retries)
                                }
                                "paymentMethod" -> {
                                    val normalized = value.lowercase(Locale.ROOT)
                                    if (normalized !in listOf("till", "paybill", "both")) { toast("Payment method must be Till, PayBill or Both"); return@setOnClickListener }
                                    record.put(key, normalized.replaceFirstChar { it.uppercase() })
                                }
                                "executionSim" -> {
                                    val normalized = value.uppercase(Locale.ROOT).replace("SIM1", "SIM 1").replace("SIM2", "SIM 2")
                                    if (normalized !in listOf("SIM 1", "SIM 2")) { toast("Execution SIM must be SIM 1 or SIM 2"); return@setOnClickListener }
                                    record.put(key, normalized)
                                }
                                "ussdMode" -> {
                                    val normalized = value.lowercase(Locale.ROOT)
                                    if (normalized !in listOf("simple", "advanced")) { toast("USSD mode must be Simple or Advanced"); return@setOnClickListener }
                                    record.put(key, normalized.replaceFirstChar { it.uppercase() })
                                }
                                else -> record.put(key, value)
                            }
                        }
                        if (record.optString("name").isBlank()) { toast("Enter an offer name"); return@setOnClickListener }
                        if (record.optString("price").isNotBlank() && record.optString("price").toDoubleOrNull() == null) { toast("Enter a valid customer price"); return@setOnClickListener }
                        if (record.optString("customerPrice").isBlank()) record.put("customerPrice", record.optString("price"))
                        else record.put("price", record.optString("customerPrice"))
                        if (record.optBoolean("scheduleEnabled", false) && (!validTime(record.optString("startTime")) || !validTime(record.optString("endTime")))) {
                            toast("Use valid times such as 23:00 and 16:00"); return@setOnClickListener
                        }
                        if (isNew) record.put("enabled", false)
                        if (existing != null && originalCode != record.optString("code")) record.put("mappingStatus", "Needs update")
                        else if (!record.has("mappingStatus")) record.put("mappingStatus", "Mapping pending")
                        upsertRecord("offers", record); dismiss(); showPage("Offers"); toast("Offer saved")
                    }
                }
                show()
            }
    }

    private fun discountPercent(oldPrice: String, newPrice: String): String {
        val old = oldPrice.toDoubleOrNull() ?: return "0"
        val new = newPrice.toDoubleOrNull() ?: return "0"
        if (old <= 0 || new >= old) return "0"
        return String.format(Locale.US, "%.1f", (old - new) / old * 100)
            .removeSuffix(".0")
    }

    private fun validTime(value: String): Boolean {
        if (!value.matches(Regex("\\d{2}:\\d{2}"))) return false
        val parts = value.split(":")
        val hour = parts[0].toIntOrNull() ?: return false
        val minute = parts[1].toIntOrNull() ?: return false
        return hour in 0..23 && minute in 0..59
    }

    private fun isOfferActive(offer: JSONObject): Boolean {
        if (!offer.optBoolean("enabled", false)) return false
        if (!offer.optBoolean("scheduleEnabled", false)) return true

        val start = offer.optString("startTime", "")
        val end = offer.optString("endTime", "")
        if (!validTime(start) || !validTime(end)) return false

        val now = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = TimeZone.getTimeZone(offer.optString("scheduleTimeZone", "Africa/Nairobi"))
        }.format(Date())
        if (start == end) return true

        return if (start < end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }

    // TRAINING CONTROLS

    private fun trainingKey(offer: JSONObject) =
        "training_steps_${offer.optString("id")}"

    private fun showTrainingMenu(offer: JSONObject) {
        val options = arrayOf(
            "Start training (manual step capture)", "Retrain mapping", "Delete mapping",
            "Review saved steps", "Training number", "Tracer menu", "Mapping status"
        )
        AlertDialog.Builder(this)
            .setTitle("Training: ${offer.optString("name")}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> enterTrainingSteps(offer, false)
                    1 -> enterTrainingSteps(offer, true)
                    2 -> AlertDialog.Builder(this)
                        .setTitle("Delete training?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Delete") { _, _ ->
                            prefs.edit().remove(trainingKey(offer)).apply()
                            toast("Training deleted")
                        }.show()
                    3 -> showSavedSteps(offer)
                    4 -> editTrainingNumber()
                    5 -> showTracerMenu(offer)
                    6 -> AlertDialog.Builder(this).setTitle("Mapping status")
                        .setMessage(offer.optString("mappingStatus", "Mapping pending") +
                            "\nSaving manually entered steps does not verify a live USSD mapping.")
                        .setPositiveButton("Close", null).show()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun enterTrainingSteps(offer: JSONObject, retrain: Boolean) {
        val previous = runCatching {
            val saved = JSONObject(
                prefs.getString(trainingKey(offer), "") ?: ""
            )
            val steps = saved.optJSONArray("steps") ?: JSONArray()
            (0 until steps.length()).joinToString("\n") {
                steps.optString(it)
            }
        }.getOrDefault("")

        val input = EditText(this).apply {
            minLines = 5
            gravity = Gravity.TOP
            hint = "Enter one USSD menu step per line"
            setText(if (retrain) "" else previous)
        }

        AlertDialog.Builder(this)
            .setTitle(if (retrain) "Retrain Offer" else "Start Training")
            .setMessage(
                "Training number: ${prefs.getString("training_number", "0704279192")}\n" +
                    "This saves entered steps. Live USSD menu capture is not implemented yet."
            )
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save Steps") { _, _ ->
                val steps = input.text.toString().lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                if (steps.isEmpty()) {
                    toast("Enter training steps first")
                } else {
                    val saved = JSONObject().apply {
                        put("steps", JSONArray(steps))
                        put("savedAt", System.currentTimeMillis())
                        put("skipFinalOption", true)
                        put("captureType", "manual")
                        put("verified", false)
                        put("trainingNumber", prefs.getString("training_number", "0704279192"))
                    }
                    offer.put("mappingStatus", "Mapping pending")
                    upsertRecord("offers", offer)
                    prefs.edit()
                        .putString(trainingKey(offer), saved.toString())
                        .apply()

                    toast("Steps saved as pending. No live USSD menu was captured or verified.")
                }
            }
            .show()
    }

    private fun showSavedSteps(offer: JSONObject) {
        val raw = prefs.getString(trainingKey(offer), "") ?: ""

        if (raw.isBlank()) {
            toast("No saved training steps")
            return
        }

        val message = runCatching {
            val saved = JSONObject(raw)
            val steps = saved.optJSONArray("steps") ?: JSONArray()

            buildString {
                for (i in 0 until steps.length()) {
                    append(i + 1)
                    append(". ")
                    append(steps.optString(i))
                    append("\n")
                }

                append("\nCapture type: ").append(saved.optString("captureType", "manual"))
                append("\nVerified: ").append(saved.optBoolean("verified", false))
                append("\nFinal menu option: must be skipped during training")
                append("\nTraining number: ")
                append(saved.optString("trainingNumber", "0704279192"))
            }
        }.getOrDefault(raw)

        AlertDialog.Builder(this)
            .setTitle("Saved training steps")
            .setMessage(message)
            .setPositiveButton("Close", null)
            .show()
    }

    private fun showTracerMenu(offer: JSONObject) {
        val options = arrayOf("View trace history", "Add observed change manually", "Tracer settings")
        AlertDialog.Builder(this).setTitle("Tracer: ${offer.optString("name")}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showTraceHistory(offer)
                    1 -> addObservedTrace(offer)
                    2 -> editTracerSettings()
                }
            }.setNegativeButton("Close", null).show()
    }

    private fun editTracerSettings() {
        val input = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("tracer_max_changes", 20).toString())
            hint = "Maximum trace changes"
        }
        AlertDialog.Builder(this).setTitle("Tracer settings")
            .setMessage("Maximum number of manually recorded observed changes to retain.")
            .setView(input).setNegativeButton("Cancel", null)
            .setPositiveButton("Save Tracer Settings") { _, _ ->
                val limit = input.text.toString().toIntOrNull()
                if (limit == null || limit !in 1..500) toast("Enter a limit from 1 to 500")
                else { prefs.edit().putInt("tracer_max_changes", limit).apply(); toast("Tracer settings saved") }
            }.show()
    }

    private fun addObservedTrace(offer: JSONObject) {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(8), dp(16), dp(8)) }
        val oldOption = EditText(this).apply { hint = "Previous option number (if known)"; isSingleLine = true }
        val newOption = EditText(this).apply { hint = "New observed option number (if known)"; isSingleLine = true }
        val oldText = EditText(this).apply { hint = "Previously observed menu text"; minLines = 2 }
        val newText = EditText(this).apply { hint = "Newly observed menu text"; minLines = 2 }
        layout.addView(makeText("Record only what you actually observed. This does not capture live USSD automatically.", 12f, grey))
        listOf(oldOption, newOption, oldText, newText).forEach { layout.addView(it) }
        AlertDialog.Builder(this).setTitle("Record menu change").setView(layout)
            .setNegativeButton("Cancel", null).setPositiveButton("Save change") { _, _ ->
                val historyKey = "tracer_${offer.optString("id")}"
                val history = try { JSONArray(prefs.getString(historyKey, "[]") ?: "[]") } catch (_: Exception) { JSONArray() }
                val item = JSONObject().apply {
                    put("oldOption", oldOption.text.toString().trim()); put("newOption", newOption.text.toString().trim())
                    put("oldText", oldText.text.toString().trim()); put("newText", newText.text.toString().trim())
                    put("recordedAt", System.currentTimeMillis()); put("verified", false)
                    put("status", "Needs review")
                }
                history.put(item)
                val max = prefs.getInt("tracer_max_changes", 20).coerceIn(1, 500)
                val trimmed = JSONArray()
                for (i in (history.length() - max).coerceAtLeast(0) until history.length()) trimmed.put(history.getJSONObject(i))
                prefs.edit().putString(historyKey, trimmed.toString()).apply()
                offer.put("mappingStatus", "Needs update"); upsertRecord("offers", offer)
                showTraceHistory(offer)
            }.show()
    }

    private fun showTraceHistory(offer: JSONObject) {
        val raw = prefs.getString("tracer_${offer.optString("id")}", "[]") ?: "[]"
        val text = try {
            val array = JSONArray(raw)
            if (array.length() == 0) "No trace changes recorded yet." else buildString {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    append(i + 1).append(". Option ").append(item.optString("oldOption", "?"))
                        .append(" → ").append(item.optString("newOption", "?"))
                    append("\nOld text: ").append(item.optString("oldText", ""))
                    append("\nNew text: ").append(item.optString("newText", ""))
                    append("\nStatus: ").append(item.optString("status", "Needs review")).append("\n\n")
                }
            }
        } catch (_: Exception) { "Trace history could not be read." }
        AlertDialog.Builder(this).setTitle("Tracer history: ${offer.optString("name")}")
            .setMessage(text).setPositiveButton("Close", null).show()
    }

    private fun editTrainingNumber() {
        val input = EditText(this).apply {
            isSingleLine = true
            hint = "0704279192"
            setText(prefs.getString("training_number", "0704279192"))
            inputType = android.text.InputType.TYPE_CLASS_PHONE
        }

        AlertDialog.Builder(this)
            .setTitle("Training number")
            .setMessage("Enter the phone number used for training.")
            .setView(input)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                val number = input.text.toString().trim()

                if (number.isBlank()) {
                    toast("Enter a phone number")
                } else {
                    prefs.edit()
                        .putString("training_number", number)
                        .apply()
                    toast("Training number saved")
                }
            }
            .show()
    }

    // GENERAL RECORDS

    private fun buildRecords(body: LinearLayout, key: String) {
        val title = when (key) {
            "clients" -> "Client Management"
            "messages" -> "M-PESA Messages"
            "quickdial" -> "Quick Dial"
            "autoreply" -> "Auto Reply"
            "mappings" -> "Offer Mapping"
            "orders" -> "Order History"
            else -> "Records"
        }

        val records = readList(key)

        body.addView(makeText("Saved records: ${records.size}", 16f, black, true))
        addGap(body)

        body.addView(makeButton("+ Add $title") {
            editRecord(key, null, title)
        })

        addGap(body, 10)

        if (records.isEmpty()) {
            body.addView(makeText("No records saved yet.", 14f, grey))
        }

        records.forEach { record ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(white)
            }

            card.addView(makeText(
                record.optString("name", "Unnamed record"),
                16f, black, true
            ))

            val details = record.optString("details", "")
            if (details.isNotBlank()) {
                card.addView(makeText(details, 14f, grey))
            }

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            actions.addView(makeButton("Edit", darkGreen) {
                editRecord(key, record, title)
            }, LinearLayout.LayoutParams(0, dp(48), 1f))

            actions.addView(makeButton("Delete", red) {
                AlertDialog.Builder(this)
                    .setTitle("Delete record?")
                    .setMessage(record.optString("name"))
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        deleteRecord(key, record.optString("id"))
                        showPage(currentPage)
                    }
                    .show()
            }, LinearLayout.LayoutParams(0, dp(48), 1f))

            card.addView(actions)
            body.addView(card)
            addGap(body, 8)
        }
    }

    private fun editRecord(
        key: String,
        existing: JSONObject?,
        title: String
    ) {
        val isNew = existing == null

        val record = existing ?: JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("name", "")
            put("details", "")
        }

        val nameInput = EditText(this).apply {
            isSingleLine = true
            hint = "Name"
            setText(record.optString("name", ""))
        }

        val detailsInput = EditText(this).apply {
            minLines = 3
            gravity = Gravity.TOP
            hint = "Details, phone number, message or notes"
            setText(record.optString("details", ""))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(8), dp(16), dp(8))
            addView(makeText("Name", 13f, darkGreen, true))
            addView(nameInput)
            addView(makeText("Details", 13f, darkGreen, true))
            addView(detailsInput)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isNew) "Add $title" else "Edit $title")
            .setView(layout)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener {
                            val name = nameInput.text.toString().trim()

                            if (name.isBlank()) {
                                toast("Enter a name")
                                return@setOnClickListener
                            }

                            record.put("name", name)
                            record.put(
                                "details",
                                detailsInput.text.toString().trim()
                            )

                            upsertRecord(key, record)
                            dismiss()
                            showPage(currentPage)
                            toast("Record saved")
                        }
                }
                show()
            }
    }

    private fun buildStatistics(body: LinearLayout) {
        val offers = readList("offers")
        val clients = readList("clients")
        val messages = readList("messages")
        val orders = readList("orders")

        body.addView(makeText("Bingwa Bot Statistics", 19f, darkGreen, true))
        addGap(body)

        body.addView(makeText("Total offers: ${offers.size}", 16f, black, true))
        body.addView(makeText(
            "Active offers now: ${offers.count { isOfferActive(it) }}",
            16f, green, true
        ))
        body.addView(makeText("Saved clients: ${clients.size}", 16f))
        body.addView(makeText("Saved M-PESA messages: ${messages.size}", 16f))
        body.addView(makeText("Saved orders: ${orders.size}", 16f))

        addGap(body, 12)

        body.addView(makeText(
            "Statistics count records saved in this app. " +
                "Automatic transaction processing is not yet connected.",
            13f, grey
        ))
    }

    private fun buildSettings(body: LinearLayout) {
        body.addView(makeText("Settings", 19f, darkGreen, true))
        addGap(body)

        body.addView(makeText(
            "Training number: " +
                prefs.getString("training_number", "0704279192"),
            16f, black, true
        ))

        body.addView(makeButton("Change Training Number") {
            editTrainingNumber()
        })

        addGap(body, 10)

        body.addView(makeButton("Clear All App Records", red) {
            AlertDialog.Builder(this)
                .setTitle("Clear all records?")
                .setMessage(
                    "This deletes saved offers, clients, messages, " +
                        "orders, mappings and training data."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    prefs.edit().clear().apply()
                    toast("App records cleared")
                    showPage("Settings")
                }
                .show()
        })

        addGap(body, 10)

        body.addView(makeText(
            "This app currently stores data locally on this device.",
            13f, grey
        ))
    }

    // LOCAL STORAGE HELPERS

    private fun readList(key: String): MutableList<JSONObject> {
        val raw = prefs.getString(key, "[]") ?: "[]"

        return try {
            val array = JSONArray(raw)
            MutableList(array.length()) { index ->
                array.getJSONObject(index)
            }
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    private fun saveList(key: String, records: List<JSONObject>) {
        val array = JSONArray()
        records.forEach { array.put(it) }

        prefs.edit()
            .putString(key, array.toString())
            .apply()
    }

    private fun upsertRecord(key: String, record: JSONObject) {
        val records = readList(key)
        val id = record.optString("id")

        val index = records.indexOfFirst {
            it.optString("id") == id
        }

        if (index >= 0) {
            records[index] = record
        } else {
            records.add(record)
        }

        saveList(key, records)
    }

    private fun deleteRecord(key: String, id: String) {
        val records = readList(key)
        records.removeAll { it.optString("id") == id }
        saveList(key, records)
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
