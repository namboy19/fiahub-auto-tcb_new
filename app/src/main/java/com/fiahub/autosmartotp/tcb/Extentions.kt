package com.fiahub.autosmartotp.tcb

import android.content.Context
import android.util.Log


/**
 * Created on 2018/9/28.
 * By nesto
 */
private const val TAG = "AutoClickServiceTcb"

fun Any.logd(tag: String = TAG) {
    if (!BuildConfig.DEBUG) return
    if (this is String) {
        Log.d(tag, this)
    } else {
        Log.d(tag, this.toString())
    }
}


fun Context.dp2px(dpValue: Float): Int {
    val scale = resources.displayMetrics.density
    return (dpValue * scale + 0.5f).toInt()
}

typealias Action = () -> Unit