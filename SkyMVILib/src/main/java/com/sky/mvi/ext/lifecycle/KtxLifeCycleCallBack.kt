package com.sky.mvi.ext.lifecycle
import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.sky.mvi.ext.util.logI

/**
 * @Class: KtxLifeCycleCallBack
 * @Author: Henry
 * @Date: 2025/2/23 08:59
 * @Description: Activity生命周期回调，记录日志并管理Activity栈
 */

class KtxLifeCycleCallBack : Application.ActivityLifecycleCallbacks {

    private val TAG = javaClass.simpleName

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        KtxActivityManger.pushActivity(activity)
        "onActivityCreated : ${activity.localClassName}".logI(TAG)
    }

    override fun onActivityStarted(activity: Activity) {
        "onActivityStarted : ${activity.localClassName}".logI(TAG)
    }

    override fun onActivityResumed(activity: Activity) {
        "onActivityResumed : ${activity.localClassName}".logI(TAG)
    }

    override fun onActivityPaused(activity: Activity) {
        "onActivityPaused : ${activity.localClassName}".logI(TAG)
    }

    override fun onActivityDestroyed(activity: Activity) {
        "onActivityDestroyed : ${activity.localClassName}".logI(TAG)
        KtxActivityManger.popActivity(activity)
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {

    }

    override fun onActivityStopped(activity: Activity) {
        "onActivityStopped : ${activity.localClassName}".logI(TAG)
    }

}