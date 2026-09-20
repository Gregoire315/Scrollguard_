package com.gregoire.scrollguard

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Boucle de rappel : tant que l'utilisateur reste sur un site suivi
 * (Instagram / X), une notification s'affiche toutes les 5 minutes avec un
 * message motivant tiré au sort. Le cycle s'arrête dès que le service
 * d'accessibilité signale qu'on a quitté le site. Au 2e rappel, en plus de
 * la notification, l'utilisateur est renvoyé à l'écran d'accueil (Chrome
 * passe en arrière-plan).
 */
object ReminderManager {

    private const val CHANNEL_ID = "scrollguard_reminders"
    private const val NOTIFICATION_ID = 4242
    private const val INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

    private const val PREFS_NAME = "scrollguard_prefs"
    private const val KEY_MESSAGES = "custom_messages"

    private val handler = Handler(Looper.getMainLooper())
    private var isActive = false
    private var tickCount = 0

    private val defaultMessages = listOf(
        "Tu veux vraiment passer encore 5 minutes ici ? Retourne à ton projet.",
        "Lève-toi. Bois un verre d'eau. Puis retourne construire quelque chose.",
        "Instagram peut attendre. Ton projet, lui, avance maintenant.",
        "5 minutes de plus ici ne construiront pas ton entreprise. Ferme l'application.",
        "Est-ce que ce que tu fais maintenant te rapproche de ton objectif ?",
        "Pose ton téléphone. Fais une petite action concrète pour ton projet.",
        "Ton futur toi te remerciera d'avoir arrêté maintenant.",
        "Personne ne scrolle son chemin jusqu'au succès. Ferme.",
        "Dix minutes de sport valent mieux qu'une heure de feed.",
        "Ce que tu cherches ici n'y est pas. Retourne bosser."
    )

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isActive) return
            tickCount += 1
            appContext?.let { context ->
                showReminder(context)
                if (tickCount == 2) {
                    closeChromeSession()
                }
            }
            handler.postDelayed(this, INTERVAL_MS)
        }
    }

    private var appContext: Context? = null

    fun start(context: Context) {
        if (isActive) return
        isActive = true
        tickCount = 0
        appContext = context.applicationContext
        ensureChannel(appContext!!)
        // Premier rappel après 5 minutes, pas immédiatement à l'ouverture.
        handler.postDelayed(tickRunnable, INTERVAL_MS)
    }

    fun stop() {
        if (!isActive) return
        isActive = false
        tickCount = 0
        handler.removeCallbacks(tickRunnable)
    }

    /**
     * Envoie immédiatement une notification, sans passer par la détection
     * ni le délai de 5 minutes. Sert uniquement à vérifier que le canal de
     * notification et la permission fonctionnent, indépendamment du reste.
     */
    fun sendImmediateTestNotification(context: Context) {
        val appContextLocal = context.applicationContext
        ensureChannel(appContextLocal)
        showReminder(appContextLocal)
    }

    /**
     * Renvoie les messages actuellement utilisés : ceux personnalisés par
     * l'utilisateur s'il en a enregistré, sinon la liste par défaut.
     */
    fun getMessages(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_MESSAGES, null) ?: return defaultMessages
        val list = saved.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        return if (list.isEmpty()) defaultMessages else list
    }

    /**
     * Enregistre une nouvelle liste de messages personnalisés. Une liste
     * vide efface la personnalisation et revient aux messages par défaut.
     */
    fun saveMessages(context: Context, messages: List<String>) {
        val cleaned = messages.map { it.trim() }.filter { it.isNotEmpty() }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MESSAGES, cleaned.joinToString("\n")).apply()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rappels anti-scroll",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }
    }

    private fun showReminder(context: Context) {
        val message = getMessages(context).random()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Stop.")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).apply {
            // La permission POST_NOTIFICATIONS est vérifiée dans MainActivity
            // au premier lancement ; si elle n'est pas accordée, notify()
            // échoue silencieusement plutôt que de planter.
            try {
                notify(NOTIFICATION_ID, notification)
            } catch (_: SecurityException) {
                // Permission non accordée : rien à faire ici, l'utilisateur
                // sera re-sollicité depuis MainActivity.
            }
        }
    }

    /**
     * Renvoie l'utilisateur à l'écran d'accueil, ce qui fait passer Chrome
     * en arrière-plan. Le service d'accessibilité détectera ensuite ce
     * changement d'application et appellera stop() automatiquement.
     */
    private fun closeChromeSession() {
        ScrollGuardAccessibilityService.instance?.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_HOME
        )
    }
}
