package com.fiahub.autosmartotp.tcb.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Bundle
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.fiahub.autosmartotp.tcb.MainActivity
import com.fiahub.autosmartotp.tcb.logd
import com.fiahub.autosmartotp.tcb.service.api.ApiService
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext


var autoClickServiceTcb: AutoClickServiceTcb? = null

class AutoClickServiceTcb : AccessibilityService(), CoroutineScope {

    override fun onInterrupt() {
        // NO-OP
    }

    private var isStarted = false
    private var unlockOtpPass = ""
    var lastHomeScreenTime: Long = System.currentTimeMillis()
    var isWaitingConfirmMemo: Boolean = false

    private var currentScreen = SCREEN_UNKNOW

    private val nodeInfos = mutableListOf<AccessibilityNodeInfo>()

    companion object {
        private const val DELAY_TIME_FOR_RENDER_SCREEN = 700L
        const val SCREEN_HOME = "SCREEN_HOME"
        const val SCREEN_UNLOCK_PASS = "SCREEN_UNLOCK_PASS"
        const val SCREEN_CONFIRM_LOGIN = "SCREEN_CONFIRM_LOGIN"
        const val SCREEN_CONFIRM_TRANSFER = "SCREEN_CONFIRM_TRANSFER"
        const val SCREEN_UNKNOW = "SCREEN_UNKNOW"
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        Log.d("namtest", event.toString())
        handleScreenChanged(event)

        nodeInfos.clear()
        getNodeInfo(rootInActiveWindow)
    }

    val isCurrentHomeScreen
        get() = currentScreen == SCREEN_HOME

    private fun getNodeInfo(node: AccessibilityNodeInfo?) {

        if (node == null) {
            return
        }

        try {
            nodeInfos.add(node)

            if (node.childCount > 0) {
                for (i in 0 until node.childCount) {
                    node.getChild(i)?.let {
                        getNodeInfo(it)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isHomeScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByText("Đăng nhập")
            .isNullOrEmpty() ||
                !rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/explorerWidget")
                    .isNullOrEmpty()
    }

    private fun isInputUnlockPassScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByText("Nhập mã mở khoá để xác thực")
            .isNullOrEmpty()
    }

    private fun isConfirmLoginScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByText("Cho phép đăng nhập")
            .isNullOrEmpty()
    }

    private fun isConfirmTransferScreen(): Boolean {
        return !rootInActiveWindow?.findAccessibilityNodeInfosByText("Đến tài khoản")
            .isNullOrEmpty() && !rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnAccept")
            .isNullOrEmpty()
    }

    fun startGetAuto(unlockOtp: String) {
        isStarted = true
        unlockOtpPass = unlockOtp
        handleScreenChanged(null)
    }

    fun stopGetOtp() {
        isStarted = false
    }

    private fun handleScreenChanged(event: AccessibilityEvent?) {

        if (!isStarted) {
            return
        }

        when {
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event?.text.contains(
                "Yêu cầu xác thực từ Techcombank Online Banking") -> {

                GlobalScope.launch(Dispatchers.Main) {
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    val path = Path()
                    path.moveTo(500f, 250f)

                    val gestureDescription = GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(path, 1, 10))
                        .build()
                    dispatchGesture(gestureDescription, null, null)
                }
            }

            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event?.text.contains(
                "Xác thực đăng nhập trên thiết bị khác") -> {

                launch(Dispatchers.Main) {
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    val path = Path()
                    path.moveTo(500f, 250f)

                    val gestureDescription = GestureDescription.Builder()
                        .addStroke(GestureDescription.StrokeDescription(path, 1, 10))
                        .build()
                    dispatchGesture(gestureDescription, null, null)
                }
            }

            isHomeScreen() -> {
                if (currentScreen == SCREEN_HOME)
                    return

                currentScreen = SCREEN_HOME

                lastHomeScreenTime = System.currentTimeMillis()
                isWaitingConfirmMemo = false
            }

            isInputUnlockPassScreen() -> {
                if (currentScreen == SCREEN_UNLOCK_PASS)
                    return

                currentScreen = SCREEN_UNLOCK_PASS

                launch(Dispatchers.Main) {
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/bbInputDigitViewEditText")
                        ?.firstOrNull()?.let {
                            val arguments = Bundle()
                            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                unlockOtpPass)
                            it.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                        }
                }
            }

            isConfirmLoginScreen() -> {

                if (currentScreen == SCREEN_CONFIRM_LOGIN)
                    return

                currentScreen = SCREEN_CONFIRM_LOGIN

                launch(Dispatchers.Main) {
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnAccept")
                        ?.firstOrNull()?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                }
            }

            isConfirmTransferScreen() -> {

                if (currentScreen == SCREEN_CONFIRM_TRANSFER)
                    return

                currentScreen = SCREEN_CONFIRM_TRANSFER

                //isWaitingConfirmMemo = true

                launch(Dispatchers.Main) {
                    delay(DELAY_TIME_FOR_RENDER_SCREEN)

                    withContext(Dispatchers.IO) {

                        val transaction = ApiService.apiService.getPendingTransaction().await()

                        if (rootInActiveWindow?.findAccessibilityNodeInfosByViewId(
                                "vn.com.techcombank.bb.app:id/tvDescription")
                                ?.lastOrNull()?.text != transaction.memo) {

                            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnDecline")
                                ?.firstOrNull()
                                ?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                        } else {
                            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnAccept")
                                ?.firstOrNull()
                                ?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                        }

                        //prevent isConfirmTransferScreen call again after above action, intend to call api getPendingTransaction two times
                        delay(DELAY_TIME_FOR_RENDER_SCREEN)
                        //isWaitingConfirmMemo = false
                    }
                }
            }

            else -> {
                currentScreen = SCREEN_UNKNOW
            }
        }

        //handle edge case
        launch(Dispatchers.Main) {
            delay(DELAY_TIME_FOR_RENDER_SCREEN)
            //dismiss error dialog
            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnPositive")
                ?.firstOrNull()?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)

            //dismiss error expire login screen
            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/btnConfirm")
                ?.firstOrNull()?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)

            //others screen
            rootInActiveWindow?.findAccessibilityNodeInfosByViewId("vn.com.techcombank.bb.app:id/doneBtn")
                ?.firstOrNull()?.performAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
        }
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        "onServiceConnected".logd()
        autoClickServiceTcb = this
        startActivity(Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onUnbind(intent: Intent?): Boolean {
        "AutoClickServiceTcb onUnbind".logd()
        autoClickServiceTcb = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        "AutoClickServiceTcb onDestroy".logd()
        autoClickServiceTcb = null
        super.onDestroy()
    }

    override val coroutineContext: CoroutineContext
        get() = SupervisorJob() + Dispatchers.Main
}