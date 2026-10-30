package com.steve.zapai

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import java.util.concurrent.ConcurrentHashMap

class NotifService : NotificationListenerService() {
    companion object {
        const val ACTION = "com.steve.zapai.NEW"
        val replies = ConcurrentHashMap<String, Notification.Action>()

        fun send(ctx: Context, app: String, contact: String, text: String): Boolean {
            val a = replies[app + "|" + contact] ?: return false
            val ris = a.remoteInputs ?: return false
            if (ris.isEmpty()) return false
            return try {
                val i = Intent(); val b = Bundle()
                b.putCharSequence(ris[0].resultKey, text)
                RemoteInput.addResultsToIntent(ris, i, b)
                a.actionIntent.send(ctx, 0, i)
                Store.add(ctx, Msg(app, contact, text, true, System.currentTimeMillis()))
                true
            } catch (e: Exception) { false }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val app = Config.APPS[sbn.packageName] ?: return
        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val e = n.extras
        val title = e.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = e.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return
        if (Regex("\\d+ (nouveaux )?messages?.*|\\d+ new messages?.*").matches(text)) return
        val contact = title.replace(Regex("\\s*\\(\\d+.*\\)$"), "")
        n.actions?.firstOrNull { it.remoteInputs?.isNotEmpty() == true }?.let { replies[app + "|" + contact] = it }
        Store.add(this, Msg(app, contact, text, false, System.currentTimeMillis()))
        sendBroadcast(Intent(ACTION).setPackage(packageName).putExtra("contact", contact))
        if (Store.auto(this, contact)) Thread {
            val r = Gemini.reply(contact, Store.thread(this, app, contact))
            if (r != null) send(this, app, contact, r)
        }.start()
    }
}
