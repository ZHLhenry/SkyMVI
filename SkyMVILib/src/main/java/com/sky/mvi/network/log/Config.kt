package com.sky.mvi.network.log
import android.util.Log
import com.elvishew.xlog.XLog
import com.sky.mvi.SkyMVILib

/**
 * @Class: Config
 * @Author: Henry
 * @Date: 2025/2/23 10:23
 * @Description: 日志配置初始化，根据XLog启用状态选择日志输出方式
 */

fun init() {
    val useXLog = SkyMVILib.getConfig()?.xLogLibEnabled == true
    LogManager.logProxy(object : LogProxy {
        override fun e(tag: String, msg: String) = log(useXLog, { XLog.tag(tag).e(msg) }, { Log.e(tag, msg) })
        override fun w(tag: String, msg: String) = log(useXLog, { XLog.tag(tag).w(msg) }, { Log.w(tag, msg) })
        override fun i(tag: String, msg: String) = log(useXLog, { XLog.tag(tag).i(msg) }, { Log.i(tag, msg) })
        override fun d(tag: String, msg: String) = log(useXLog, { XLog.tag(tag).d(msg) }, { Log.d(tag, msg) })
    })
}

private inline fun log(useXLog: Boolean, xLogAction: () -> Unit, logAction: () -> Unit) {
    if (useXLog) xLogAction() else logAction()
}
