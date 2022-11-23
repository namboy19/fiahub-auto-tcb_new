package com.fiahub.autosmartotp.tcb

import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import com.fiahub.autosmartotp.tcb.service.FloatingClickService
import com.fiahub.autosmartotp.tcb.service.autoClickServiceTcb
import kotlinx.android.synthetic.main.activity_main.*


private const val PERMISSION_CODE = 110

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || Settings.canDrawOverlays(this)) {

                FloatingClickService.start(this, edtPin.text.toString(), "")
                onBackPressed()
            } else {
                askPermission()
                Toast.makeText(this,
                    "You need System Alert Window Permission to do this",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAccess(): Boolean {
        val string = getString(R.string.accessibility_service_id)
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val list =
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (id in list) {
            if (string == id.id) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        val hasPermission = checkAccess()
        "has access? $hasPermission".logd()
        if (!hasPermission) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && !Settings.canDrawOverlays(this)) {
            askPermission()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askPermission() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName"))
        startActivityForResult(intent, PERMISSION_CODE)
    }

    override fun onDestroy() {

        stopService(Intent(this@MainActivity, FloatingClickService::class.java))

        autoClickServiceTcb?.let {
            "stop auto click service".logd()
            it.stopSelf()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) return it.disableSelf()
            autoClickServiceTcb = null
        }
        super.onDestroy()
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }
}
