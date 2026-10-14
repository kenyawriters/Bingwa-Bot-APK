
package com.onyangodatasolutions.bingwabot

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val green = Color.rgb(0, 130, 70)
    private val darkGreen = Color.rgb(0, 90, 48)
    private val lightGreen = Color.rgb(225, 245, 234)
    private val grey = Color.rgb(110, 110, 110)
    private val lightGrey = Color.rgb(242, 244, 245)
    private val red = Color.rgb(190, 45, 45)
    private val white = Color.WHITE
    private val black = Color.rgb(30, 30, 30)

    private val prefs by lazy {
        getSharedPreferences("bingwa_bot_data", MODE_PRIVATE)
    }

    private var currentPage = "Home"
    private var offerCategory = "All"
    private var editingId: String? = null

    private val menuItems = listOf(
        "Home",
        "Offers",
        "Client Management",
        "MPESA Messages",
        "Quick Dial",
        "Auto Reply",
        "Statistics",
        "Offer Mapping",
        "Order History",
        "Settings"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPage("Home")
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun rootColumn(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(lightGrey)
            setPadding(dp(14), dp(10), dp(14), dp(14))
        }
    }

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
        parent.addView(View(this), LinearLayout.LayoutParams(
            1, dp(height)
        ))
    }

    private fun addLine(parent: LinearLayout) {
        val line = View(this)
        line.setBackgroundColor(Color.LTGRAY)
        parent.addView(line, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
        ))
    }

    private fun showPage(page: String) {
        currentPage = page

        val root = rootColumn()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }

        header.addView(
            makeButton("☰ Menu", darkGreen) { showNavigation() },
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val heading = makeText("BINGWA BOT", 20f, green, true)
        heading.gravity = Gravity.CENTER
        header.addView(heading, LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
        ))

        root.addView(header)
        addLine(root)
        root.addView(makeText(page, 22f, darkGreen, true))
        addGap(root, 4)

        val scroll = ScrollView(this)
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

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
            else -> body.addView(makeText("Page not found"))
        }

        setContentView(root)
    }

    private fun showNavigation() {
        AlertDialog.Builder(this)
            .setTitle("Bingwa Bot Menu")
            .setItems(menuItems.toTypedArray()) { _, which ->
                showPage(menuItems[which])
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun buildHome(body: LinearLayout) {
        val offers = readList("offers")
        val active = offers.count { isOfferActive(it) }

        body.addView(makeText(
            "Welcome to Bingwa Bot", 19f, black, true
        ))
        body.addView(makeText(
            "Manage offers, customers, messages and transactions."
        ))

        addGap(body, 12)

        body.addView(makeText("Saved offers: ${offers.size}", 17f, black, true))
        body.addView(makeText("Active offers now: $active", 17f, green, true))

        addGap(body, 12)

        listOf(
            "Offers",
            "Client Management",
            "MPESA Messages",
            "Quick Dial",
            "Auto Reply",
            "Statistics",
            "Offer Mapping",
            "Order History",
            "Settings"
        ).forEach { page ->
            body.addView(makeButton(page) { showPage(page) })
            addGap(body, 5)
        }

        body.addView(makeText(
            "Note: background SMS handling and real USSD execution " +
                "must be configured and tested separately.",
            13f, grey
        ))
    }

    // ------------------------------------------------------------
    // OFFERS
    // ------------------------------------------------------------

    private fun buildOffers(body: LinearLayout) {
        val offers = readList("offers")
        val active = offers.count { isOfferActive(it) }

        body.addView(makeText(
            "Total saved offers: ${offers.size}", 17f, black, true
        ))
        body.addView(makeText(
            "Total active offers now: $active", 17f, green, true
        ))
        addGap(body, 6)

        body.addView(makeButton("+ Add New Offer") {
            editOffer(null)
        })
        addGap(body, 8)

        val categories = listOf("All", "Data", "SMS", "Minutes", "Others")
        val categoryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        categories.forEach { category ->
            val button = Button(this).apply {
                text = category
                textSize = 11f
                isAllCaps = false
                setTextColor(if (offerCategory == category) white else green)
                setBackgroundColor(
                    if (offerCategory == category) green else white
                )
                setOnClickListener {
                    offerCategory = category
                    showPage("Offers")
                }
            }
            categoryRow.addView(button, LinearLayout.LayoutParams(
                0, dp(46), 1f
            ))
        }

        body.addView(categoryRow)
        addGap(body, 8)

        val filtered = offers.filter {
            offerCategory == "All" ||
                it.optString("category", "Others") == offerCategory
        }

        if (filtered.isEmpty()) {
            body.addView(makeText(
                "No offers in this category. Tap Add New Offer to create one."
            ))
        }

        filtered.forEach { offer ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(10), dp(8), dp(10), dp(8))
                setBackgroundColor(white)
            }

            val top = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val name = offer.optString("name", "Unnamed offer")
            val price = offer.optString("price", "0")
            val oldPrice = offer.optString("oldPrice", "")
            val enabled = offer.optBoolean("enabled", false)

            val namePrice = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            namePrice.addView(makeText(name, 16f, black, true))

            if (oldPrice.isNotBlank() && oldPrice != price) {
                namePrice.addView(makeText(
                    "Was KSh $oldPrice  →  Now KSh $price",
                    14f, green, true
                ))
                namePrice.addView(makeText(
                    "Discount: ${discountPercent(oldPrice, price)}%",
                    13f, grey
                ))
            } else {
                namePrice.addView(makeText("KSh $price", 14f, green, true))
            }

            top.addView(namePrice, LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
            ))

            // Power icon only. New offers start OFF.
            val power = TextView(this).apply {
                text = "⏻"
                textSize = 29f
                gravity = Gravity.CENTER
                setTextColor(if (enabled) green else grey)
                setPadding(dp(10), dp(4), dp(10), dp(4))
                contentDescription = if (enabled) "Turn offer off" else "Turn offer on"
                setOnClickListener {
                    offer.put("enabled", !enabled)
                    replaceRecord("offers", offer)
                    showPage("Offers")
                    toast(
                        if (!enabled) "Offer switched ON" else "Offer switched OFF"
                    )
                }
            }
            top.addView(power)

            card.addView(top)
            addGap(card, 3)

            val validity = offer.optString("validity", "")
            val code = offer.optString("code", "")
            val schedule = if (offer.optBoolean("scheduleEnabled", false)) {
                "Schedule: ${offer.optString("startTime")}–${offer.optString("endTime")} daily"
            } else {
                "Schedule: not set"
            }

            card.addView(makeText(
                "${offer.optString("category", "Others")} • $validity",
                13f, grey
            ))
            if (code.isNotBlank()) {
                card.addView(makeText("Code: $code", 13f, grey))
            }
            card.addView(makeText(schedule, 13f, grey))
            card.addView(makeText(
                if (isOfferActive(offer)) "Status: ACTIVE NOW"
                else if (enabled) "Status: ON, but outside availability hours"
                else "Status: OFF",
                13f, if (isOfferActive(offer)) green else grey, true
            ))

            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            actions.addView(makeButton("Edit", darkGreen) {
                editOffer(offer)
            }, LinearLayout.LayoutParams(0, dp(48), 1f))

            actions.addView(makeButton("Training", grey) {
                showTrainingMenu(offer)
            }, LinearLayout.LayoutParams(0, dp(48), 1f))

            actions.addView(makeButton("Delete", red) {
                AlertDialog.Builder(this)
                    .setTitle("Delete offer?")
                    .setMessage("Delete ${offer.optString("name")}?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        deleteRecord("offers", offer.optString("id"))
                        showPage("Offers")
                    }
                    .show()
            }, LinearLayout.LayoutParams(0, dp(48), 1f))

            card.addView(actions)
            body.addView(card)
            addGap(body, 9)
        }
    }

    private fun editOffer(existing: JSONObject?) {
        editingId = existing?.optString("id")
        val isNew = existing == null
        val record = existing ?: JSONObject().apply {
            put("id", System.currentTimeMillis().toString())
            put("enabled", false)
            put("category", "Data")
            put("repeatable", false)
            put("scheduleEnabled", false)
            put("startTime", "23:00")
            put("endTime", "16:00")
            put("oldPrice", "")
            put("price", "")
            put("validity", "")
            put("code", "")
            put("name", "")
        }

        val fields = listOf(
            "name" to "Offer name",
            "category" to "Category: Data, SMS, Minutes, Others",
            "price" to "Current price (KSh)",
            "oldPrice" to "Old price for discount (optional)",
            "validity" to "Validity",
            "code" to "Offer code / USSD code",
            "repeatable" to "Repeatable? Enter yes or no",
            "scheduleEnabled" to "Enable daily schedule? Enter yes or no",
            "startTime" to "Daily start time (HH:mm, e.g. 23:00)",
            "endTime" to "Daily end time (HH:mm, e.g. 16:00)"
        )

        val inputs = LinkedHashMap<String, EditText>()
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
        }
        val scroll = ScrollView(this)
        scroll.addView(layout)

        fields.forEach { (key, label) ->
            layout.addView(makeText(label, 13f, darkGreen, true))
            val input = EditText(this).apply {
                textSize = 15f
                singleLine = true
                setText(
                    when (key) {
                        "repeatable" -> if (record.optBoolean(key, false)) "yes" else "no"
                        "scheduleEnabled" -> if (record.optBoolean(key, false)) "yes" else "no"
                        else -> record.optString(key, "")
                    }
                )
                hint = label
            }
            inputs[key] = input
            layout.addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle(if (isNew) "Add Offer" else "Edit Offer")
            .setView(scroll)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                fields.forEach { (key, _) ->
                    val value = inputs[key]?.text?.toString()?.trim().orEmpty()
                    when (key) {
                        "repeatable", "scheduleEnabled" -> {
                            record.put(key, value.equals("yes", true) ||
                                value.equals("true", true))
                        }
                        "category" -> {
                            val category = value.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            }
                            record.put(
                                key,
                                if (category in listOf("Data", "Sms", "Minutes", "Others")) {
                                    if (category == "Sms") "SMS" else category
                                } else "Others"
                            )
                        }
                        else -> record.put(key, value)
                    }
                }

                if (record.optString("name").isBlank()) {
                    toast("Enter an offer name")
                    return@setPositiveButton
                }

                // Existing enabled state is retained when editing.
                // A new offer is always OFF.
                if (isNew) record.put("enabled", false)

                upsertRecord("offers", record)
                showPage("Offers")
                toast("Offer saved")
            }
            .show()
    }

    private fun discountPercent(oldPrice: String, newPrice: String): String {
        val old = oldPrice.toDoubleOrNull() ?: return "0"
        val new = newPrice.toDoubleOrNull() ?: return "0"
        if (old <= 0 || new >= old) return "0"
        return String.format(Locale.US, "%.1f", (old - new) / old * 100)
            .removeSuffix(".0")
    }

    private fun isOfferActive(offer: JSONObject): Boolean {
        if (!offer.optBoolean("enabled", false)) return false
        if (!offer.optBoolean("scheduleEnabled", false)) return true

        val start = offer.optString("startTime", "")
        val end = offer.optString("endTime", "")
        val now = SimpleDateFormat("HH:mm", Locale.US).format(Date())

        if (!validTime(start) || !validTime(end)) return false
        if (start == end) return true

        // Supports schedules that cross midnight, e.g. 23:00–16:00.
        return if (start < end) {
            now >= start && now < end
        } else {
            now >= start || now < end
        }
    }

    private fun validTime(value: String): Boolean {
        return try {
            val parts = value.split(":")
            if (parts.size != 2) return false
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            hour in 0..23 && minute in 0..59 &&
                value.matches(Regex("\\d{2}:\\d{2}"))
        } catch (_: Exception) {
            false
        }
    }

    // ------------------------------------------------------------
    // OFFER TRAINING
    // ------------------------------------------------------------

    private fun showTrainingMenu(offer: JSONObject) {
        val options = arrayOf(
            "Start training",
            "Retrain",
            "Delete training",
            "Saved steps",
            "Training number"
        )

        AlertDialog.Builder(this)
            .setTitle("Training: ${offer.optString("name")}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> enterTrainingSteps(offer, false)
                    1 -> enterTrainingSteps(offer, true)
                    2 -> {
                        AlertDialog.Builder(this)
                            .setTitle("Delete training?")
                            .setMessage("Remove saved steps for this offer?")
                            .setNegativeButton("Cancel", null)
                            .setPositiveButton("Delete") { _, _ ->
                                prefs.edit().remove(trainingKey(offer)).apply()
                                toast("Saved training deleted")
                            }
                            .show()
                    }
                    3 -> showSavedSteps(offer)
                    4 -> editTrainingNumber()
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun trainingKey(offer: JSONObject): String =
        "training_steps_${offer.optString("id")}"

    private fun enterTrainingSteps(offer: JSONObject, retrain: Boolean) {
        val oldSteps = prefs.getString(trainingKey(offer), "") ?: ""
        val input = EditText(this).apply {
            minLi
