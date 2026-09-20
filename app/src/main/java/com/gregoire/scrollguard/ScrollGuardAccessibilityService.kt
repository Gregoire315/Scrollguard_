package com.gregoire.scrollguard

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Surveille Chrome et détecte si l'URL affichée correspond à un des domaines
 * suivis (Instagram, X/Twitter). Démarre/arrête la boucle de rappels en
 * conséquence via ReminderManager.
 *
 * Limite connue : l'identifiant de la barre d'adresse Chrome
 * ("com.android.chrome:id/url_bar") peut changer selon la version de Chrome
 * ou du canal (stable/beta/dev). En secours, on scanne aussi tout le texte
 * visible dans la fenêtre pour repérer les domaines suivis.
 */
class ScrollGuardAccessibilityService : AccessibilityService() {

    companion object {
        var instance: ScrollGuardAccessibilityService? = null
    }

    private val chromePackage = "com.android.chrome"

    private val trackedDomains = listOf(
        "instagram.com",
        "x.com",
        "twitter.com"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val eventPackage = event.packageName?.toString()

        if (eventPackage != chromePackage) {
            // On n'est plus dans Chrome du tout -> on coupe les rappels.
            ReminderManager.stop()
            writeDebugInfo(eventPackage ?: "(aucun)", matched = false, snippet = "")
            return
        }

        val root = rootInActiveWindow ?: return
        val currentUrl = extractCurrentUrl(root)
        val isOnTrackedSite = trackedDomains.any { domain ->
            currentUrl.contains(domain, ignoreCase = true)
        }

        writeDebugInfo(eventPackage, matched = isOnTrackedSite, snippet = currentUrl)

        if (isOnTrackedSite) {
            ReminderManager.start(applicationContext)
        } else {
            ReminderManager.stop()
        }
    }

    override fun onInterrupt() {
        ReminderManager.stop()
    }

    /**
     * Écrit l'état de la dernière détection dans les préférences partagées,
     * pour que MainActivity puisse l'afficher à titre de diagnostic.
     */
    private fun writeDebugInfo(packageName: String, matched: Boolean, snippet: String) {
        val prefs = getSharedPreferences("scrollguard_debug", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_package", packageName)
            .putBoolean("last_matched", matched)
            .putString("last_snippet", snippet.take(150))
            .putLong("last_timestamp", System.currentTimeMillis())
            .apply()
    }

    /**
     * Essaie d'abord de lire directement le champ de la barre d'adresse.
     * Si rien n'est trouvé (changement d'ID côté Chrome), scanne le texte
     * visible de la fenêtre en secours.
     */
    private fun extractCurrentUrl(root: AccessibilityNodeInfo): String {
        val urlBarNodes = root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
        val fromUrlBar = urlBarNodes.firstOrNull()?.text?.toString()
        if (!fromUrlBar.isNullOrBlank()) {
            return fromUrlBar
        }
        return collectVisibleText(root, depth = 0)
    }

    private fun collectVisibleText(node: AccessibilityNodeInfo, depth: Int, maxDepth: Int = 6): String {
        if (depth > maxDepth) return ""
        val builder = StringBuilder()
        node.text?.let { builder.append(it).append(' ') }
        node.contentDescription?.let { builder.append(it).append(' ') }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            builder.append(collectVisibleText(child, depth + 1, maxDepth))
        }
        return builder.toString()
    }
}
