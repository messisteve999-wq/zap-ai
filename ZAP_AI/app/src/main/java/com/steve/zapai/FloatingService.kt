package com.steve.zapai

import android.app.*
import android.content.*
import android.graphics.*
import android.media.RingtoneManager
import android.os.*
import android.provider.ContactsContract
import android.view.*
import android.widget.*

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var bubble: ImageView
    private lateinit var bp: WindowManager.LayoutParams
    private lateinit var panel: LinearLayout
    private lateinit var pp: WindowManager.LayoutParams
    private val ui = Handler(Looper.getMainLooper())
    private var open = false
    private var app = Config.APPS.keys.first()
    private var contact: String? = null
    private var draft = ""

    private fun dp(x: Int) = (x * resources.displayMetrics.density).toInt()
    override fun onBind(i: Intent?): IBinder? = null

    private val rx = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            try { RingtoneManager.getRingtone(c, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)).play() } catch (e: Exception) {}
            bubble.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200).withEndAction { bubble.animate().scaleX(1f).scaleY(1f).setDuration(200) }
            if (open) render()
        }
    }

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("zapai", "ZAP_AI", NotificationManager.IMPORTANCE_MIN))
        startForeground(1, Notification.Builder(this, "zapai").setSmallIcon(R.drawable.ic_launcher).setContentTitle("ZAP_AI actif").build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val f = IntentFilter(NotifService.ACTION)
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(rx, f, Context.RECEIVER_NOT_EXPORTED) else registerReceiver(rx, f)

        bubble = ImageView(this).apply { setImageBitmap(demon()) }
        bp = WindowManager.LayoutParams(dp(60), dp(60), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 0; y = dp(200) }
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        bubble.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = bp.x; oy = bp.y; moved = false }
                MotionEvent.ACTION_MOVE -> {
                    if (Math.abs(e.rawX - sx) > 12 || Math.abs(e.rawY - sy) > 12) moved = true
                    if (moved) { bp.x = ox + (e.rawX - sx).toInt(); bp.y = oy + (e.rawY - sy).toInt(); wm.updateViewLayout(bubble, bp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) toggle()
            }
            true
        }
        wm.addView(bubble, bp)

        panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(Color.parseColor("#F2121212")); setPadding(dp(8), dp(8), dp(8), dp(8)) }
        pp = WindowManager.LayoutParams(dp(320), dp(470), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT).apply {
            gravity = Gravity.TOP or Gravity.START; x = dp(8); y = dp(90); softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE }
    }

    private fun toggle() {
        if (open) { wm.removeView(panel); open = false } else { render(); wm.addView(panel, pp); open = true }
    }

    private fun demon(): Bitmap {
        val b = Bitmap.createBitmap(160, 160, Bitmap.Config.ARGB_8888); val c = Canvas(b); val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = Color.BLACK; c.drawCircle(80f, 80f, 78f, p)
        p.color = Color.parseColor("#6A0000")
        c.drawPath(Path().apply { moveTo(26f, 50f); lineTo(34f, 6f); lineTo(70f, 36f); close() }, p)
        c.drawPath(Path().apply { moveTo(134f, 50f); lineTo(126f, 6f); lineTo(90f, 36f); close() }, p)
        p.color = Color.RED
        c.drawOval(RectF(36f, 68f, 68f, 92f), p); c.drawOval(RectF(92f, 68f, 124f, 92f), p)
        p.color = Color.parseColor("#8B0000"); p.style = Paint.Style.STROKE; p.strokeWidth = 6f
        c.drawArc(RectF(50f, 100f, 110f, 140f), 20f, 140f, false, p)
        return b
    }

    private fun btn(t: String, f: () -> Unit) = Button(this).apply { text = t; textSize = 11f; isAllCaps = false; setOnClickListener { f() } }
    private fun tv(t: String, sz: Float = 13f, col: Int = Color.WHITE) = TextView(this).apply { text = t; textSize = sz; setTextColor(col); setPadding(0, dp(3), 0, dp(3)) }

    private fun saved(): Set<String>? {
        if (checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return null
        val s = HashSet<String>()
        contentResolver.query(ContactsContract.Contacts.CONTENT_URI, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use {
            while (it.moveToNext()) s.add(it.getString(0) ?: "")
        }
        return s
    }

    private fun render() {
        panel.removeAllViews()
        val c = contact
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        if (c != null) head.addView(btn("←") { contact = null; render() })
        head.addView(tv("  😈 ZAP_AI" + (if (c != null) " · " + c else ""), 16f))
        panel.addView(head)
        if (c == null) {
            val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            Config.APPS.forEach { (pkg, name) -> tabs.addView(btn(if (pkg == app) "● " + name else name) { app = pkg; render() }) }
            panel.addView(tabs)
            val sv = ScrollView(this); val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val sc = saved()
            val list = Store.contacts(this, app).filter { sc == null || sc.contains(it) }
            if (sc == null) col.addView(tv("Ouvre ZAP_AI pour autoriser l'accès aux contacts (étape 3).", 12f, Color.YELLOW))
            if (list.isEmpty()) col.addView(tv("Aucun message pour l'instant.", 13f, Color.LTGRAY))
            list.forEach { n -> col.addView(btn("💬 " + n) { contact = n; draft = ""; render() }) }
            sv.addView(col); panel.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            val sv = ScrollView(this); val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            Store.thread(this, app, c).takeLast(30).forEach {
                col.addView(tv((if (it.out) "Moi : " else "") + it.text, 13f, if (it.out) Color.parseColor("#7CFC9A") else Color.WHITE))
            }
            sv.addView(col); sv.post { sv.fullScroll(View.FOCUS_DOWN) }
            panel.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
            val edit = EditText(this).apply { setText(draft); setTextColor(Color.WHITE); hint = "Message / suggestion (modifiable)"; setHintTextColor(Color.GRAY) }
            panel.addView(edit)
            val suggest = { hint: String? ->
                edit.setText("⏳ …")
                Thread {
                    val r = Gemini.reply(c, Store.thread(this, app, c), hint)
                    ui.post { draft = r ?: "Serveur injoignable"; edit.setText(draft) }
                }.start()
            }
            val r1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            r1.addView(btn("💡 Suggestion") { suggest(null) })
            r1.addView(btn("🔄 Changer") { suggest("Propose une formulation différente de la précédente : " + edit.text) })
            r1.addView(btn("📤 Envoyer") {
                val t = edit.text.toString().trim()
                if (t.isNotEmpty()) {
                    val ok = Thread { NotifService.send(this, app, c, t) }.also { it.start() }
                    ok.join(1500); draft = ""; render()
                }
            })
            panel.addView(r1)
            val r2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            r2.addView(CheckBox(this).apply { text = "Auto"; setTextColor(Color.WHITE); isChecked = Store.auto(context, c)
                setOnCheckedChangeListener { _, v -> Store.setAuto(context, c, v) } })
            r2.addView(CheckBox(this).apply { text = "Éphémère"; setTextColor(Color.WHITE); isChecked = Store.ephemeral(context)
                setOnCheckedChangeListener { _, v -> Store.setEphemeral(context, v) } })
            r2.addView(btn("🗑") { Store.delete(this, app, c); contact = null; render() })
            panel.addView(r2)
        }
    }

    override fun onDestroy() {
        try { unregisterReceiver(rx); wm.removeView(bubble); if (open) wm.removeView(panel) } catch (e: Exception) {}
        super.onDestroy()
    }
}
