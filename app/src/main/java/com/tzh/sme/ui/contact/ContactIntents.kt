package com.tzh.sme.ui.contact

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object ContactIntents {
    fun openTelegram(context: Context, handle: String = "Thanzawhein997") {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$handle"))
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openEmail(context: Context, email: String, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:thanzawhein29897@gmail.com")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
        }
    }
}
