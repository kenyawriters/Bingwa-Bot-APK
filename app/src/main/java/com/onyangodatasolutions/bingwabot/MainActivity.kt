
package com.onyangodatasolutions.bingwabot

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*

import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {

    private val green = Color.rgb(0, 130, 70)

    private val prefs by lazy {
        getSharedPreferences("bingwa_bot_data", Context.MODE_PRIVATE)
    }

    private lateinit var root: LinearLayout

    data class Field(val key: String, val label: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showHome()
        requestSmsPermissionsIfNeeded()
    }

    private fun requestSmsPermissionsIfNeeded() {
        val permissions = arrayOf(
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS
        )

        val missing = permissions.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            AlertDialog.Builder(this)
                .setTitle("Bingwa Bot Permissions")
                .setMessage(
                    "Allow the required SMS permissions to enable " +
                    "supported SMS features. You can continue using " +
                    "the dashboard if you do not grant them."
                )
                .setPositiveButton("Continue") { _, _ ->
                    requestPermissions(
                        missing.toTypedArray(),
                        SMS_PERMISSION_REQUEST
                    )
                }
                .setNegativeButton("Later", null)
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )

        if (requestCode == SMS_PERMISSION_REQUEST) {
            val granted = permissions.indices.count { index ->
                grantResults.getOrNull(index) ==
                    PackageManager.PERMISSION_GRANTED
            }

            Toast.makeText(
                this,
                if (granted == permissions.size) {
                    "Requested SMS permissions granted"
                } else {
                    "Some SMS permissions were not granted"
                },
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun page(title: String, subtitle: String = "") {
        val scroll = ScrollView(this)

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.rgb(245, 248, 246))
        }

        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = title
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(green)
            setPadding(0, 12, 0, 12)
        })

        if (subtitle.isNotBlank()) {
            root.addView(TextView(this).apply {
                text = subtitle
                textSize = 14f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                setPadding(0, 0, 0, 16)
            })
        }
    }

    private fun button(label: String, action: () -> Unit) {
        root.addView(Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 4, 0, 8)
        })
    }

    private fun text(value: String) {
        root.addView(TextView(this).apply {
            text = value
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.WHITE)
        }, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 4, 0, 4)
        })
    }

    private fun showHome() {
        page("BINGWA BOT", "Safaricom Offers & Customer Management")

        text("Dashboard\nManage offers, clients, mappings and orders.")

        button("Available Offers") { showRecords("offers") }
        button("Client Management") { showRecords("clients") }
        button("Offer Mapping") { showRecords("mappings") }
        button("Order History") { showRecords("orders") }
        button("Settings") { showSettings() }
        button("Check SMS Permissions") {
            showPermissionStatus()
        }

        text("Version 1.2")
    }

    private fun showPermissionStatus() {
        page("SMS Permissions", "Current permission status")

        val permissions = listOf(
            Manifest.permission.READ_SMS to "Read SMS",
            Manifest.permission.RECEIVE_SMS to "Receive SMS",
            Manifest.permission.SEND_SMS to "Send SMS"
        )

        permissions.forEach { (permission, label) ->
            val granted =
                checkSelfPermission(permission) ==
                    PackageManager.PERMISSION_GRANTED

            text("$label: ${if (granted) "Granted" else "Not granted"}")
        }

        button("Request Missing Permissions") {
            requestSmsPermissionsIfNeeded()
        }

        button("Back to Dashboard") { showHome() }
    }

    private fun titleFor(type: String) = when (type) {
        "offers" -> "Available Offers"
        "clients" -> "Client Management"
        "mappings" -> "Offer Mapping"
        else -> "Order History"
    }

    private fun singular(type: String) = when (type) {
        "offers" -> "Offer"
        "clients" -> "Client"
        "mappings" -> "Mapping"
        else -> "Order"
    }

    private fun fields(type: String): List<Field> = when (type) {
        "offers" -> listOf(
            Field("name", "Offer name"),
            Field("price", "Price in KES"),
            Field("validity", "Validity"),
            Field("code", "Offer code (optional)"),
            Field("repeatable", "Repeatable? Yes or No")
        )

        "clients" -> listOf(
            Field("name", "Customer name"),
            Field("phone", "Phone number"),
            Field("notes", "Notes (optional)")
        )

        "mappings" -> listOf(
            Field("name", "Offer name"),
            Field("instructions", "Purchase steps"),
            Field("code", "USSD/code (optional)")
        )

        else -> listOf(
            Field("client", "Customer name or phone"),
            Field("offer", "Offer purchased"),
            Field("amount", "Amount in KES"),
            Field("status", "Status: Pending, Successful or Failed"),
            Field("notes", "Notes (optional)")
        )
    }

    private fun showRecords(type: String) {
        page(titleFor(type), "Records saved on this phone")

        button("+ Add ${singular(type)}") {
            editRecord(type, null)
        }

        button("Back to Dashboard") { showHome() }

        val records = load(type)

        if (records.isEmpty()) {
            text("No records yet. Tap + Add to create one.")
            return
        }

        records.forEachIndexed { index, record ->
            val summary = when (type) {
                "offers" ->
                    "Offer: ${record["name"].orEmpty()}\n" +
                    "Price: KES ${record["price"].orEmpty()}\n" +
                    "Validity: ${record["validity"].orEmpty()}\n" +
                    "Repeatable: ${record["repeatable"].orEmpty()}\n" +
                    "Code: ${record["code"].orEmpty()}"

                "clients" ->
                    "Name: ${record["name"].orEmpty()}\n" +
                    "Phone: ${record["phone"].orEmpty()}\n" +
                    "Notes: ${record["notes"].orEmpty()}"

                "mappings" ->
                    "Offer: ${record["name"].orEmpty()}\n" +
                    "Steps: ${record["instructions"].orEmpty()}\n" +
                    "Code: ${record["code"].orEmpty()}"

                else ->
                    "Client: ${record["client"].orEmpty()}\n" +
                    "Offer: ${record["offer"].orEmpty()}\n" +
                    "Amount: KES ${record["amount"].orEmpty()}\n" +
                    "Status: ${record["status"].orEmpty()}\n" +
                    "Notes: ${record["notes"].orEmpty()}"
            }

            text(summary)

            button("Edit record ${index + 1}") {
                editRecord(type, index)
            }

            button("Delete record ${index + 1}") {
                AlertDialog.Builder(this)
                    .setTitle("Delete record?")
                    .setMessage("This cannot be undone.")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete") { _, _ ->
                        val updated = load(type)

                        if (index in updated.indices) {
                            updated.removeAt(index)
                            save(type, updated)
                            showRecords(type)
                            toast("Record deleted")
                        }
                    }
                    .show()
            }
        }
    }

    private fun editRecord(type: String, index: Int?) {
        val records = load(type)

        val old: Map<String, String> =
            if (index != null && index in records.indices) {
                records[index]
            } else {
                emptyMap()
            }

        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 0)
        }

        val inputs = mutableMapOf<String, EditText>()

        fields(type).forEach { field ->
            val input = EditText(this).apply {
                hint = field.label
                setText(old[field.key].orEmpty())

                setSingleLine(
                    field.key != "instructions" &&
                    field.key != "notes"
                )

                if (field.key == "price" || field.key == "amount") {
                    inputType =
                        android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                }

                if (field.key == "phone") {
                    inputType =
                        android.text.InputType.TYPE_CLASS_PHONE
                }
            }

            form.addView(input)
            inputs[field.key] = input
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                (if (index == null) "Add " else "Edit ") +
                    singular(type)
            )
            .setView(ScrollView(this).apply { addView(form) })
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val record = mutableMapOf<String, String>()

                    inputs.forEach { (key, input) ->
                        record[key] = input.text.toString().trim()
                    }

                    val required = when (type) {
                        "offers" -> listOf("name", "price")
                        "clients" -> listOf("name", "phone")
                        "mappings" -> listOf("name", "instructions")
                        else -> listOf("client", "offer", "amount", "status")
                    }

                    if (required.any { record[it].isNullOrBlank() }) {
                        toast("Please fill in all required fields")
                        return@setOnClickListener
                    }

                    val updated = load(type)

                    if (index == null) {
                        updated.add(record)
                    } else if (index in updated.indices) {
                        updated[index] = record
                    }

                    save(type, updated)
                    dialog.dismiss()
                    showRecords(type)
                    toast("Saved successfully")
                }
        }

        dialog.show()
    }

    private fun load(
        type: String
    ): MutableList<MutableMap<String, String>> {
        val result = mutableListOf<MutableMap<String, String>>()

        try {
            val array = JSONArray(
                prefs.getString(type, "[]") ?: "[]"
            )

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val record = mutableMapOf<String, String>()
                val keys = obj.keys()

                while (keys.hasNext()) {
                    val key = keys.next()
                    record[key] = obj.optString(key, "")
                }

                result.add(record)
            }
        } catch (_: Exception) {
            // Keep the dashboard usable if saved data is invalid.
        }

        return result
    }

    private fun save(
        type: String,
        records: List<Map<String, String>>
    ) {
        val array = JSONArray()

        records.forEach { record ->
            val obj = JSONObject()

            record.forEach { (key, value) ->
                obj.put(key, value)
            }

            array.put(obj)
        }

        prefs.edit().putString(type, array.toString()).apply()
    }

    private fun showSettings() {
        page("Settings", "Local data overview")

        text("Offers: ${load("offers").size}")
        text("Clients: ${load("clients").size}")
        text("Mappings: ${load("mappings").size}")
        text("Orders: ${load("orders").size}")

        text(
            "Records are stored on this phone. " +
            "Clearing app data or uninstalling may remove them."
        )

        button("Check SMS Permissions") {
            showPermissionStatus()
        }

        button("Back to Dashboard") { showHome() }
    }

    private fun toast(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {
        private const val SMS_PERMISSION_REQUEST = 100
    }
}
