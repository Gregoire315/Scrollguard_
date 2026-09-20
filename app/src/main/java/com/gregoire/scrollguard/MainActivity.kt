package com.gregoire.scrollguard

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var diagnosticText: TextView
    private lateinit var messagesContainer: LinearLayout
    private lateinit var intervalInput: EditText
    private lateinit var closeAfterInput: EditText

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val screenPadding = dp(20)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(screenPadding, dp(32), screenPadding, dp(32))
        }

        root.addView(sectionTitle("ScrollGuard", big = true))
        root.addView(bodyText(
            "Rappels pendant qu'Instagram ou X sont ouverts dans Chrome, avec retour " +
                "automatique à l'accueil après un certain nombre de rappels."
        ).apply { setPadding(0, dp(4), 0, dp(20)) })

        // --- Carte : état / configuration ---
        val setupCard = card()
        val setupContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        setupContent.addView(sectionTitle("Configuration"))
        statusText = bodyText("").apply { setPadding(0, dp(8), 0, dp(12)) }
        setupContent.addView(statusText)
        setupContent.addView(primaryButton("1. Activer le service d'accessibilité") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        setupContent.addView(spacer(8))
        setupContent.addView(primaryButton("2. Autoriser les notifications") {
            requestNotificationPermissionIfNeeded()
        })
        setupContent.addView(spacer(8))
        setupContent.addView(outlinedButton("Tester : envoyer une notification") {
            sendTestNotification()
        })
        setupCard.addView(setupContent)
        root.addView(setupCard)
        root.addView(spacer(20))

        // --- Carte : réglages de temps ---
        val timingCard = card()
        val timingContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        timingContent.addView(sectionTitle("Réglages de temps"))

        timingContent.addView(fieldLabel("Délai entre chaque rappel (minutes)"))
        intervalInput = numberField(ReminderManager.getIntervalMinutes(this).toString())
        timingContent.addView(intervalInput)
        timingContent.addView(spacer(12))

        timingContent.addView(fieldLabel("Retour à l'accueil après le Ne rappel (0 = jamais)"))
        closeAfterInput = numberField(ReminderManager.getCloseAfterTicks(this).toString())
        timingContent.addView(closeAfterInput)
        timingContent.addView(spacer(16))

        timingContent.addView(primaryButton("Enregistrer les réglages de temps") {
            saveTimingSettings()
        })
        timingCard.addView(timingContent)
        root.addView(timingCard)
        root.addView(spacer(20))

        // --- Carte : messages ---
        val messagesCard = card()
        val messagesContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        messagesContent.addView(sectionTitle("Mes messages"))
        messagesContent.addView(bodyText(
            "Un message est tiré au sort dans cette liste à chaque rappel."
        ).apply { setPadding(0, dp(4), 0, dp(12)) })

        messagesContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        ReminderManager.getMessages(this).forEach { addMessageRow(it) }
        messagesContent.addView(messagesContainer)
        messagesContent.addView(spacer(8))
        messagesContent.addView(outlinedButton("+ Ajouter un message") {
            addMessageRow("")
        })
        messagesContent.addView(spacer(12))
        messagesContent.addView(primaryButton("Enregistrer mes messages") {
            saveCustomMessages()
        })
        messagesCard.addView(messagesContent)
        root.addView(messagesCard)
        root.addView(spacer(20))

        // --- Carte : diagnostic ---
        val diagnosticCard = card()
        val diagnosticContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        diagnosticContent.addView(sectionTitle("Diagnostic"))
        diagnosticText = bodyText("").apply { setPadding(0, dp(8), 0, 0) }
        diagnosticContent.addView(diagnosticText)
        diagnosticCard.addView(diagnosticContent)
        root.addView(diagnosticCard)
        root.addView(spacer(20))

        root.addView(bodyText(
            "Note : si ScrollGuard n'apparaît pas dans la liste des services " +
                "d'accessibilité, va dans Paramètres > Applications > ScrollGuard > " +
                "menu (⋮) > \"Autoriser les paramètres restreints\", puis réessaie."
        ))

        setContentView(ScrollView(this).apply {
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.colorBackground))
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateDiagnostic()
    }

    // ---------- Actions ----------

    private fun saveTimingSettings() {
        val minutes = intervalInput.text.toString().toIntOrNull() ?: ReminderManager.getIntervalMinutes(this)
        val ticks = closeAfterInput.text.toString().toIntOrNull() ?: ReminderManager.getCloseAfterTicks(this)
        ReminderManager.saveIntervalMinutes(this, minutes)
        ReminderManager.saveCloseAfterTicks(this, ticks)
        intervalInput.setText(ReminderManager.getIntervalMinutes(this).toString())
        closeAfterInput.setText(ReminderManager.getCloseAfterTicks(this).toString())
        Toast.makeText(this, "Réglages de temps enregistrés.", Toast.LENGTH_SHORT).show()
    }

    private fun addMessageRow(initialText: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }

        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            setText(initialText)
            hint = "Message motivant"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val deleteButton = TextView(this).apply {
            text = "✕"
            setTextColor(Color.parseColor("#B3261E"))
            setPadding(dp(12), dp(8), dp(4), dp(8))
            setOnClickListener { messagesContainer.removeView(row) }
        }

        row.addView(input)
        row.addView(deleteButton)
        messagesContainer.addView(row)
    }

    private fun saveCustomMessages() {
        val texts = mutableListOf<String>()
        for (i in 0 until messagesContainer.childCount) {
            val row = messagesContainer.getChildAt(i) as? LinearLayout ?: continue
            val input = row.getChildAt(0) as? EditText ?: continue
            texts.add(input.text.toString())
        }
        ReminderManager.saveMessages(this, texts)

        // Recharge la liste pour refléter ce qui est réellement utilisé
        // (par ex. retour aux messages par défaut si tout était vide).
        messagesContainer.removeAllViews()
        ReminderManager.getMessages(this).forEach { addMessageRow(it) }

        Toast.makeText(this, "Messages enregistrés.", Toast.LENGTH_SHORT).show()
    }

    private fun sendTestNotification() {
        if (!areNotificationsEnabled()) {
            requestNotificationPermissionIfNeeded()
            return
        }
        ReminderManager.sendImmediateTestNotification(applicationContext)
    }

    // ---------- État ----------

    private fun updateStatus() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val notificationsEnabled = areNotificationsEnabled()
        statusText.text = "Service d'accessibilité : ${if (accessibilityEnabled) "activé ✅" else "désactivé ❌"}\n" +
            "Notifications : ${if (notificationsEnabled) "autorisées ✅" else "refusées ❌"}"
    }

    private fun updateDiagnostic() {
        val prefs = getSharedPreferences("scrollguard_debug", Context.MODE_PRIVATE)
        val timestamp = prefs.getLong("last_timestamp", 0L)

        if (timestamp == 0L) {
            diagnosticText.text = "Aucune détection enregistrée pour l'instant.\n" +
                "Ouvre Instagram ou X dans Chrome, attends 10-15 secondes, puis reviens ici."
            return
        }

        val pkg = prefs.getString("last_package", "?")
        val matched = prefs.getBoolean("last_matched", false)
        val snippet = prefs.getString("last_snippet", "")
        val relativeTime = DateUtils.getRelativeTimeSpanString(timestamp)

        diagnosticText.text = "Il y a $relativeTime :\n" +
            "App détectée : $pkg\n" +
            "Site suivi reconnu : ${if (matched) "oui ✅" else "non ❌"}\n" +
            "Texte lu (extrait) : ${snippet?.take(150)}"
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        return enabledServices.any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    private fun areNotificationsEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !areNotificationsEnabled()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ---------- Petits constructeurs de vues (UI) ----------

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun card(): LinearLayout {
        // On simule une carte Material simple (fond blanc, coins arrondis, légère
        // ombre) sans dépendre d'un layout XML séparé, pour rester dans un seul
        // fichier facile à modifier.
        val background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(ContextCompat.getColor(this@MainActivity, R.color.colorSurface))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = background
            elevation = dp(2).toFloat()
        }
    }

    private fun sectionTitle(text: String, big: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorPrimaryDark))
        textSize = if (big) 26f else 17f
        setTypeface(typeface, Typeface.BOLD)
    }

    private fun bodyText(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorTextSecondary))
        textSize = 14f
    }

    private fun fieldLabel(text: String): TextView = TextView(this).apply {
        this.text = text
        setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorTextSecondary))
        textSize = 13f
        setPadding(0, 0, 0, dp(4))
    }

    private fun numberField(initialValue: String): EditText = EditText(this).apply {
        inputType = InputType.TYPE_CLASS_NUMBER
        setText(initialValue)
    }

    private fun primaryButton(label: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun outlinedButton(label: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            setOnClickListener { onClick() }
        }

    private fun spacer(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp))
    }
}
