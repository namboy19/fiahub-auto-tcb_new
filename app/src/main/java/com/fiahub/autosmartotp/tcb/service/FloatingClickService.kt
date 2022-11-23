package com.fiahub.autosmartotp.tcb.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.fiahub.autosmartotp.tcb.R
import com.fiahub.autosmartotp.tcb.TouchAndDragListener
import com.fiahub.autosmartotp.tcb.dp2px
import com.fiahub.autosmartotp.tcb.logd
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


class FloatingClickService : Service(), CoroutineScope {

    private lateinit var manager: WindowManager
    private lateinit var view: RelativeLayout
    private lateinit var params: WindowManager.LayoutParams
    private var xForRecord = 0
    private var yForRecord = 0
    private var startDragDistance: Int = 0

    ///-------------------------------------------------------------
    private var isOn = false
    private var unlockOtpPass: String = ""

    companion object {
        val RESTART_INTERVAL = 20000L

        const val PARAM_UNLOCK_OTP_PIN = "PARAM_UNLOCK_OTP_PIN"
        const val PARAM_PASSWORD = "PARAM_PASSWORD"

        fun start(context: Context, pin: String, pass: String) {
            context.startService(Intent(context, FloatingClickService::class.java).apply {
                putExtra(PARAM_UNLOCK_OTP_PIN, pin)
                putExtra(PARAM_PASSWORD, pass)
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        unlockOtpPass = intent?.getStringExtra(PARAM_UNLOCK_OTP_PIN) ?: ""
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        startDragDistance = dp2px(10f)
        view = LayoutInflater.from(this).inflate(R.layout.widget, null) as RelativeLayout

        //setting the layout parameters
        val overlayParam =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayParam,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT)

        //adding an touchlistener to make drag movement of the floating widget
        view.setOnTouchListener(TouchAndDragListener(params, startDragDistance,
            {
                isOn = !isOn

                if (isOn) {
                    openBankApp()
                    startAuto()
                } else {
                    stopAuto()
                }
                view.findViewById<TextView>(R.id.button).text = if (isOn) "ON" else "OFF"
            },
            { manager.updateViewLayout(view, params) }))

        //getting windows services and adding the floating view to it
        manager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        manager.addView(view, params)
    }


    private var restartAutoJob: Job? = null

    private fun startAuto() {
        autoClickServiceTcb?.startGetAuto(unlockOtpPass)
        restartAutoJob?.cancel()
        restartAutoJob = GlobalScope.launch {

            while (true) {
                delay(RESTART_INTERVAL)
                if (autoClickServiceTcb?.isCurrentHomeScreen == false &&
                    System.currentTimeMillis() - (autoClickServiceTcb?.lastHomeScreenTime ?: 0) > RESTART_INTERVAL){
                    openBankApp()
                }
            }
        }
    }

    private fun stopAuto() {
        autoClickServiceTcb?.stopGetOtp()
        restartAutoJob?.cancel()
    }

    private fun openBankApp() {
        val packageName = getString(R.string.bank_package_id)

        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent?.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "App not installed", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        manager.removeView(view)
        stopAuto()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        "FloatingClickService onConfigurationChanged".logd()
        val x = params.x
        val y = params.y
        params.x = xForRecord
        params.y = yForRecord
        xForRecord = x
        yForRecord = y
        manager.updateViewLayout(view, params)
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main

}