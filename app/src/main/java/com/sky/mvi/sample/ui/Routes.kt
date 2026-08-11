package com.sky.mvi.sample.ui

import com.sky.mvi.core.navigation.SkyRouter

/**
 * 应用路由表：集中声明页面路径与参数占位，规避散落的字符串魔法值。
 *
 * 配合 [com.sky.mvi.core.navigation.SkyNavigationEffect] 使用：
 * ViewModel 发出的导航副作用直接引用 [SkyRouter.build] 生成目标路径，
 * 例如 `Routes.Detail.build(articleId)`。
 */
object Routes {
    val Login = SkyRouter("login")
    val Home = SkyRouter("home")
    val Detail = SkyRouter("detail/{id}")
}
