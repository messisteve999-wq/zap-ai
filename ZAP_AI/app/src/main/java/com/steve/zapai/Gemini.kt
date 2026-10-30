package com.steve.zapai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Gemini {
    fun reply(contact: String, history: List<Msg>, hint: String? = null): String? {
        return try {
            val last = history.lastOrNull { !it.out }?.text ?: ""
            val h = JSONArray()
            history.takeLast(12).forEach { h.put((if (it.out) "Moi" else contact) + " : " + it.text) }
            val body = JSONObject().put("contact", contact).put("message", last)
                .put("history", h).put("persona", Config.PERSONA)
                .put("instruction", hint ?: "")
            val c = URL(Config.SERVER).openConnection() as HttpURLConnection
            c.requestMethod = "POST"; c.connectTimeout = 70000; c.readTimeout = 70000
            c.setRequestProperty("Content-Type", "application/json"); c.doOutput = true
            c.outputStream.use { it.write(body.toString().toByteArray()) }
            val txt = (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().readText()
            if (c.responseCode !in 200..299) return null
            val o = JSONObject(txt)
            o.optString("reply").ifEmpty { o.optString("text") }.ifEmpty { null }
        } catch (e: Exception) { null }
    }
}
