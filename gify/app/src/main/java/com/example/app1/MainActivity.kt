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
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {

    private lateinit var root: LinearLayout

    /** All dangerous runtime permissions that match the manifest */
    private val runtimePermissions: Array<String>
        get() {
            val list = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACTIVITY_RECOGNITION
            )

            // Notifications (API 33+)
            if (Build.VERSION.SDK_INT >= 33) {
                list += Manifest.permission.POST_NOTIFICATIONS
            }

            // Storage / Media
            when {
                Build.VERSION.SDK_INT >= 33 -> {
                    list += Manifest.permission.READ_MEDIA_IMAGES
                    list += Manifest.permission.READ_MEDIA_VIDEO
                    list += Manifest.permission.READ_MEDIA_AUDIO
                }
                Build.VERSION.SDK_INT >= 30 -> {
                    list += Manifest.permission.READ_EXTERNAL_STORAGE
                }
                else -> {
                    list += Manifest.permission.READ_EXTERNAL_STORAGE
                    if (Build.VERSION.SDK_INT <= 28) {
                        list += Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }
                }
            }

            // Bluetooth (API 31+)
            if (Build.VERSION.SDK_INT >= 31) {
                list += Manifest.permission.BLUETOOTH_CONNECT
                list += Manifest.permission.BLUETOOTH_SCAN
            }

            return list.toTypedArray()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = LinearLayout(this)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun allRuntimeGranted(): Boolean =
        runtimePermissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun canDrawOverlays(): Boolean =
        if (Build.VERSION.SDK_INT >= 23) Settings.canDrawOverlays(this) else true

    private fun hasManageExternalStorage(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager() else true

    private fun render() {
        root.removeAllViews()
        if (allRuntimeGranted() && canDrawOverlays() && hasManageExternalStorage()) {
            showDeviceInfo()
        } else {
            showPermissionGate()
        }
    }

    private fun showPermissionGate() {
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setPadding(48, 48, 48, 48)

        val msg = TextView(this).apply {
            text = "Permissions required\n\n" +
                    "Grant the runtime permissions, then use the buttons below " +
                    "for the special system settings (overlay, all-files, battery)."
            textSize = 15f
            gravity = Gravity.CENTER
        }
        root.addView(msg)
        root.addView(TextView(this).apply { text = "\n" })

        // Runtime permissions
        val grantBtn = Button(this).apply {
            text = "Grant Runtime Permissions"
            setOnClickListener { requestPermissions(runtimePermissions, 100) }
        }
        root.addView(grantBtn)

        // Overlay
        if (!canDrawOverlays()) {
            root.addView(Button(this).apply {
                text = "Allow Draw Over Other Apps"
                setOnClickListener {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                    )
                }
            })
        }

        // All-files access (API 30+)
        if (Build.VERSION.SDK_INT >= 30 && !hasManageExternalStorage()) {
            root.addView(Button(this).apply {
                text = "Allow All Files Access"
                setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            })
        }

        // Battery optimization
        if (!isIgnoringBatteryOptimizations()) {
            root.addView(Button(this).apply {
                text = "Ignore Battery Optimizations"
                setOnClickListener {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
            })
        }

        // Fallback
        root.addView(Button(this).apply {
            text = "Open App Settings"
            setOnClickListener {
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }

    private fun granted(p: String) =
        checkSelfPermission(p) == PackageManager.PERMISSION_GRANTED

    @Suppress("DEPRECATION")
    private fun showDeviceInfo() {
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.TOP

        val sv = ScrollView(this)
        val tv = TextView(this).apply {
            setPadding(40, 40, 40, 40)
            textSize = 15f
            setTextIsSelectable(true)
        }
        sv.addView(tv)
        root.addView(sv)

        val sb = StringBuilder()
        sb.append("DEVICE\n")
        sb.append("Brand: ${Build.BRAND}\nModel: ${Build.MODEL}\nManufacturer: ${Build.MANUFACTURER}\n")
        sb.append("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        sb.append("Security patch: ${Build.VERSION.SECURITY_PATCH}\n")
        sb.append("CPU ABI: ${Build.SUPPORTED_ABIS.joinToString()}\n\n")

        val bat = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bat?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bat?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        sb.append("BATTERY\n${if (level >= 0) level * 100 / scale else -1}%\n\n")

        val st = StatFs(Environment.getDataDirectory().path)
        val free = st.availableBlocksLong * st.blockSizeLong / (1024 * 1024 * 1024.0)
        val total = st.blockCountLong * st.blockSizeLong / (1024 * 1024 * 1024.0)
        sb.append("STORAGE\nFree: %.1f GB of %.1f GB\n".format(free, total))
        if (Build.VERSION.SDK_INT <= 32) {
            if (granted(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val n = Environment.getExternalStorageDirectory().listFiles()?.size ?: 0
                sb.append("Items in shared storage root: $n\n")
            } else sb.append("File access: not granted\n")
        } else if (Build.VERSION.SDK_INT >= 30 && hasManageExternalStorage()) {
            sb.append("All-files access: granted\n")
        }
        sb.append("\n")

        try {
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val info = wm.connectionInfo
            val ip = info.ipAddress
            val ipStr = "( {ip and 0xff}. ){ip shr 8 and 0xff}.( {ip shr 16 and 0xff}. ){ip shr 24 and 0xff}"
            sb.append("WIFI\nEnabled: ${wm.isWifiEnabled}\nIP: $ipStr\n")
            sb.append("Link speed: ${info.linkSpeed} Mbps\nSignal: ${info.rssi} dBm\n\n")
        } catch (e: Exception) {
            sb.append("WIFI\nUnavailable\n\n")
        }

        val boot = getSharedPreferences("info", Context.MODE_PRIVATE).getLong("boot", 0L)
        sb.append("LAST BOOT SEEN BY APP\n")
        sb.append(
            if (boot == 0L) "None yet (reboot the phone once)"
            else DateFormat.getDateTimeInstance().format(Date(boot))
        )
        tv.text = sb.toString()
    }
}
