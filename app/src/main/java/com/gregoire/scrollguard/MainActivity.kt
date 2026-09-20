package com.gregoire.scrollguard

import android.Manifest
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView

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
        root.addView(statusText)
        root.addView(note)

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        val notificationsEnabled = areNotificationsEnabled()
        statusText.text = "Service d'accessibilité : ${if (accessibilityEnabled) "activé ✅" else "désactivé ❌"}\n" +
            "Notifications : ${if (notificationsEnabled) "autorisées ✅" else "refusées ❌"}"
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
