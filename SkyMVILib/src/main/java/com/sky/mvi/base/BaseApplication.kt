@file:Suppress("DEPRECATION")

package com.sky.mvi.base

import android.app.Application
import android.content.IntentFilter
import android.net.ConnectivityManager
import androidx.lifecycle.ProcessLifecycleOwner
import com.sky.mvi.ext.lifecycle.KtxAppLifeObserver
import com.sky.mvi.ext.lifecycle.KtxLifeCycleCallBack
import com.sky.mvi.network.manager.NetworkStateReceive

/**
 * @Class: BaseApplication
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: Application基类，初始化网络监听、Activity生命周期管理及App前后台监听
 */

open class BaseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initAppConfig(this)
    }

    companion object {
        lateinit var app: Application
            private set

        private var mNetworkStateReceive: NetworkStateReceive? = null
        private var watchActivityLife = true
        private var watchAppLife = true

        /**
         * 初始化App配置。
         *
         * 若宿主 Application 不便继承 [BaseApplication]，可直接在自己的
         * Application#onCreate 中调用本方法完成等价初始化。
         */
        @JvmStatic
        fun initAppConfig(application: Application) {
            install(application)
        }

        private fun install(application: Application) {
            app = application
            mNetworkStateReceive = NetworkStateReceive()
            app.registerReceiver(
                mNetworkStateReceive,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
            if (watchActivityLife) {
                application.registerActivityLifecycleCallbacks(KtxLifeCycleCallBack())
            }
            if (watchAppLife) {
                ProcessLifecycleOwner.get().lifecycle.addObserver(KtxAppLifeObserver)
            }
        }
    }
}
