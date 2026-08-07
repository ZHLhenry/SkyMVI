package com.sky.mvi.sample.ui

import com.sky.mvi.mvi.navigation.Router

/**
 * 应用路由表：集中声明页面路径与参数占位，规避散落的字符串魔法值。
 *
 * 配合 [com.sky.mvi.mvi.navigation.NavigationEffect] 使用：
 * ViewModel 发出的导航副作用直接引用 [Router.build] 生成目标路径，
 * 例如 `Routes.Detail.build(articleId)`。
 */
object Routes {
    val Login = Router("login")
    val Home = Router("home")
    val Detail = Router("detail/{id}")
}
