package com.example.app1

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.StatFs
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var tv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sv = ScrollView(this)
        tv = TextView(this)
        tv.setPadding(40, 40, 40, 40)
        tv.textSize = 15f
        tv.setTextIsSelectable(true)
        sv.addView(tv)
        setContentView(sv)

        val wanted = mutableListOf(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT <= 32) wanted.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        val need = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (need.isNotEmpty()) requestPermissions(need.toTypedArray(), 1)
        refresh()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }

    private fun granted(p: String) =
        checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun refresh() {
        val sb = StringBuilder()
        sb.append("DEVICE\n")
        sb.append("Brand: \${Build.BRAND}\nModel: \${Build.MODEL}\nManufacturer: \${Build.MANUFACTURER}\n")
        sb.append("Android: \${Build.VERSION.RELEASE} (API \${Build.VERSION.SDK_INT})\n")
        sb.append("Security patch: \${Build.VERSION.SECURITY_PATCH}\n")
        sb.append("CPU ABI: \${Build.SUPPORTED_ABIS.joinToString()}\n\n")

        val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        sb.append("BATTERY\n\${if (level >= 0) level * 100 / scale else -1}%\n\n")

        val st = StatFs(Environment.getDataDirectory().path)
        val free = st.availableBlocksLong * st.blockSizeLong / (1024 * 1024 * 1024.0)
        val total = st.blockCountLong * st.blockSizeLong / (1024 * 1024 * 1024.0)
        sb.append("STORAGE\nFree: %.1f GB of %.1f GB\n".format(free, total))
        if (Build.VERSION.SDK_INT <= 32) {
            if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val n = Environment.getExternalStorageDirectory().listFiles()?.size ?: 0
                sb.append("Items in shared storage root: \$n\n")
            } else sb.append("File access: not granted\n")
        }
        sb.append("\n")

        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            val ip = info.ipAddress
            val ipStr = "\${ip and 0xff}.\${ip shr 8 and 0xff}.\${ip shr 16 and 0xff}.\${ip shr 24 and 0xff}"
            sb.append("WIFI\nEnabled: \${wm.isWifiEnabled}\nIP: \$ipStr\n")
            sb.append("Link speed: \${info.linkSpeed} Mbps\nSignal: \${info.rssi} dBm\n\n")
        } catch (e: Exception) {
            sb.append("WIFI\nUnavailable\n\n")
        }

        sb.append("SMS\n")
        if (granted(Manifest.permission.READ_SMS)) {
            try {
                val c = contentResolver.query(Uri.parse("content://sms/inbox"), arrayOf("_id"), null, null, null)
                sb.append("Messages in inbox: \${c?.use { it.count } ?: 0}\n\n")
            } catch (e: Exception) {
                sb.append("Could not read\n\n")
            }
        } else sb.append("Permission not granted\n\n")

        val boot = getSharedPreferences("info", Context.MODE_PRIVATE).getLong("boot", 0L)
        sb.append("LAST BOOT SEEN BY APP\n")
        sb.append(if (boot == 0L) "None yet (reboot the phone once)" else DateFormat.getDateTimeInstance().format(Date(boot)))
        tv.text = sb.toString()
    }
}
