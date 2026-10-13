package com.example.app1

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            context.getSharedPreferences("info", Context.MODE_PRIVATE)
                .edit().putLong("boot", System.currentTimeMillis()).apply()
        }
    }
}
