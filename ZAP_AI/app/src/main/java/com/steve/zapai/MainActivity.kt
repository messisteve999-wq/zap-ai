package com.steve.zapai

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.*

class MainActivity : Activity() {
    private lateinit var status: TextView

    private fun btn(root: LinearLayout, t: String, f: () -> Unit) {
        root.addView(Button(this).apply { text = t; setOnClickListener { f() } })
    }

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 60, 40, 40) }
        root.addView(TextView(this).apply { text = "😈 ZAP_AI"; textSize = 28f })
        status = TextView(this).apply { text = "Étapes 1 à 4 puis démarre la bulle."; setPadding(0, 20, 0, 20) }
        root.addView(status)
        btn(root, "1. Autoriser l'accès aux notifications (WhatsApp / Business)") {
            toast("Active ZAP_AI dans la liste.")
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        btn(root, "   Option grisée ? Autoriser les paramètres restreints") {
            toast("Menu ⋮ en haut à droite → « Autoriser les paramètres restreints », puis refais l'étape 1.")
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        btn(root, "2. Autoriser l'affichage par-dessus les autres applis") {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        }
        btn(root, "3. Autoriser l'accès aux contacts + notifications") {
            val p = mutableListOf(Manifest.permission.READ_CONTACTS)
            if (Build.VERSION.SDK_INT >= 33) p.add(Manifest.permission.POST_NOTIFICATIONS)
            requestPermissions(p.toTypedArray(), 1)
        }
        btn(root, "4. Tester le serveur Gemini") {
            status.text = "Test du serveur… (jusqu'à 60 s si Render dort)"
            Thread {
                val t0 = System.currentTimeMillis()
                val r = Gemini.reply("Test", listOf(Msg("t", "Test", "Coucou, comment tu vas ?", false, 0)))
                runOnUiThread {
                    status.text = if (r != null) "✅ Serveur OK (" + (System.currentTimeMillis() - t0) / 1000 + " s) :\n" + r
                    else "❌ Serveur injoignable ou route/format différent. Vérifie Config.SERVER."
                }
            }.start()
        }
        root.addView(CheckBox(this).apply {
            text = "Mode automatique pour tous les contacts"; isChecked = Store.autoAll(context)
            setOnCheckedChangeListener { _, v -> Store.setAutoAll(context, v) }
        })
        root.addView(CheckBox(this).apply {
            text = "Messagerie éphémère (efface après 24 h)"; isChecked = Store.ephemeral(context)
            setOnCheckedChangeListener { _, v -> Store.setEphemeral(context, v) }
        })
        btn(root, "▶ Démarrer la bulle flottante 😈") {
            if (!Settings.canDrawOverlays(this)) toast("Fais d'abord l'étape 2.")
            else startForegroundService(Intent(this, FloatingService::class.java))
        }
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_LONG).show()
}
