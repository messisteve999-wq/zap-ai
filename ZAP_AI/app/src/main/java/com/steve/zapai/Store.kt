package com.steve.zapai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Msg(val app: String, val contact: String, val text: String, val out: Boolean, val ts: Long)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("zapai", 0)

    @Synchronized private fun load(c: Context): MutableList<Msg> {
        val a = JSONArray(sp(c).getString("msgs", "[]"))
        val l = mutableListOf<Msg>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            l.add(Msg(o.getString("app"), o.getString("c"), o.getString("t"), o.getBoolean("o"), o.getLong("ts")))
        }
        if (ephemeral(c)) { val lim = System.currentTimeMillis() - 86400000L; l.removeAll { it.ts < lim } }
        return l
    }

    @Synchronized private fun save(c: Context, l: List<Msg>) {
        val a = JSONArray()
        l.takeLast(3000).forEach { a.put(JSONObject().put("app", it.app).put("c", it.contact).put("t", it.text).put("o", it.out).put("ts", it.ts)) }
        sp(c).edit().putString("msgs", a.toString()).apply()
    }

    @Synchronized fun add(c: Context, m: Msg) {
        val l = load(c)
        val p = l.lastOrNull()
        if (p != null && p.contact == m.contact && p.text == m.text && p.out == m.out && m.ts - p.ts < 3000) return
        l.add(m); save(c, l)
    }
    fun thread(c: Context, app: String, contact: String) = load(c).filter { it.app == app && it.contact == contact }
    fun contacts(c: Context, app: String) = load(c).filter { it.app == app }.sortedByDescending { it.ts }.map { it.contact }.distinct()
    @Synchronized fun delete(c: Context, app: String, contact: String) = save(c, load(c).filter { !(it.app == app && it.contact == contact) })

    fun ephemeral(c: Context) = sp(c).getBoolean("eph", false)
    fun setEphemeral(c: Context, v: Boolean) = sp(c).edit().putBoolean("eph", v).apply()
    fun autoAll(c: Context) = sp(c).getBoolean("auto_all", false)
    fun setAutoAll(c: Context, v: Boolean) = sp(c).edit().putBoolean("auto_all", v).apply()
    fun auto(c: Context, contact: String) = autoAll(c) || sp(c).getBoolean("auto_" + contact, false)
    fun setAuto(c: Context, contact: String, v: Boolean) = sp(c).edit().putBoolean("auto_" + contact, v).apply()
}
