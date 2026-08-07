package com.sky.mvi.ext.util

import android.util.Log
import com.elvishew.xlog.XLog
import com.sky.mvi.SkyMVILib

const val TAG = "SkyMVI"

var enableSkyMVILog = true

/**
 * @Class: LogExt
 * @Author: Henry
 * @Date: 2026/07/13 10:22
 * @Description: 日志扩展函数，支持XLog和Android Log双模式输出
 */

private enum class LEVEL {
    V, D, I, W, E
}

fun String.logV(tag: String = TAG) =
    log(LEVEL.V, tag, this)

fun String.logD(tag: String = TAG) =
    log(LEVEL.D, tag, this)

fun String.logI(tag: String = TAG) =
    log(LEVEL.I, tag, this)

fun String.logW(tag: String = TAG) =
    log(LEVEL.W, tag, this)

fun String.logE(tag: String = TAG) =
    log(LEVEL.E, tag, this)

private fun log(level: LEVEL, tag: String, message: String) {
    if (!enableSkyMVILog) return
    val newTag = if (tag == TAG) tag else "$TAG:$tag"
    val useXLog = SkyMVILib.getConfig()?.xLogLibEnabled == true
    when (level) {
        LEVEL.V -> if (useXLog) XLog.tag(newTag).v(message) else Log.v(newTag, message)
        LEVEL.D -> if (useXLog) XLog.tag(newTag).d(message) else Log.d(newTag, message)
        LEVEL.I -> if (useXLog) XLog.tag(newTag).i(message) else Log.i(newTag, message)
        LEVEL.W -> if (useXLog) XLog.tag(newTag).w(message) else Log.w(newTag, message)
        LEVEL.E -> if (useXLog) XLog.tag(newTag).e(message) else Log.e(newTag, message)
    }
}