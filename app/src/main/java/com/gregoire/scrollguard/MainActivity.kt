package com.gregoire.scrollguard

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.format.DateUtils
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var diagnosticText: TextView

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
        }

        val title = TextView(this).apply {
            text = "ScrollGuard"
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }

        val explanation = TextView(this).apply {
            text = "Rappels toutes les 5 minutes tant qu'Instagram ou X sont ouverts dans Chrome. " +
                "Deux étapes sont nécessaires ci-dessous pour que ça fonctionne."
            setPadding(0, 0, 0, 48)
        }

        statusText = TextView(this).apply {
            setPadding(0, 0, 0, 32)
        }

        val accessibilityButton = Button(this).apply {
            text = "1. Activer le service d'accessibilité"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        val notificationButton = Button(this).apply {
            text = "2. Autoriser les notifications"
            setOnClickListener {
                requestNotificationPermissionIfNeeded()
            }
        }

        val testButton = Button(this).apply {
            text = "Tester : envoyer une notification maintenant"
            setPadding(0, 32, 0, 0)
            setOnClickListener {
                sendTestNotification()
            }
        }

        val diagnosticTitle = TextView(this).apply {
            text = "Diagnostic (dernière détection dans Chrome)"
            setPadding(0, 64, 0, 8)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        diagnosticText = TextView(this).apply {
            setTextColor(Color.DKGRAY)
        }

        val note = TextView(this).apply {
            setPadding(0, 48, 0, 0)
            setTextColor(Color.DKGRAY)
            text = "Note : Android bloque par défaut l'accès à l'accessibilité pour les " +
                "applications installées hors Play Store. Si l'interrupteur ScrollGuard " +
                "n'apparaît pas dans la liste des services d'accessibilité, va dans " +
                "Paramètres > Applications > ScrollGuard > menu (⋮) > " +
                "\"Autoriser les paramètres restreints\", puis réessaie l'étape 1."
        }

        root.addView(title)
        root.addView(explanation)
        root.addView(accessibilityButton)
        root.addView(notificationButton)
        root.addView(testButton)
        root.addView(statusText)
        root.addView(diagnosticTitle)
        root.addView(diagnosticText)
        root.addView(note)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        updateDiagnostic()
    }

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
                "Ouvre Instagram ou X dans Chrome, attends 10-15 secondes, puis " +
                "reviens sur cette page pour voir apparaître un résultat ici."
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

    private fun sendTestNotification() {
        if (!areNotificationsEnabled()) {
            requestNotificationPermissionIfNeeded()
            return
        }
        ReminderManager.sendImmediateTestNotification(applicationContext)
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
}
