package com.sky.mvi.core.navigation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController

/**
 * @Class: SkyNavigationEffect
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI 导航副作用契约，ViewModel 通过实现此接口表达"我要跳转"
 */

/**
 * 导航副作用标记接口。
 *
 * 实现类提供 [handle] 方法，持有 [NavHostController] 完成具体路由。
 */
interface SkyNavigationEffect {
    fun handle(controller: NavHostController)
}

/**
 * 处理一个导航副作用
 */
fun SkyHandleNavigationEffects(
    navController: NavHostController,
    effect: SkyNavigationEffect
) {
    effect.handle(navController)
}

/**
 * 跳转到外部浏览器
 */
fun Context.openBrowser(url: String) {
    startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
