package com.sky.mvi.core.navigation

/**
 * @Class: Router
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 原生页面路由器，用于跨 Module / 原生 Activity 跳转
 */

/**
 * 路由定义。集中声明页面路径与参数占位，规避散落的字符串魔法值，
 * 并支持 [build] 将参数填入占位符生成真实目标路径。
 *
 * ```
 * object Routes {
 *     val Login = SkyRouter("login")
 *     val Home = SkyRouter("home")
 *     val Detail = object : SkyRouter("detail/{id}") {
 *         fun create(id: Int) = build(id)
 *     }
 * }
 * ```
 *
 * @param pattern 路由模板，可含 `{placeholder}`，如 `"detail/{id}"`
 */
data class SkyRouter(val pattern: String) {

    /**
     * 按顺序将 [args] 依次替换模板中的 `{placeholder}`。
     *
     * 例：`SkyRouter("detail/{id}").build(123)` → `"detail/123"`
     */
    fun build(vararg args: Any): String {
        var result = pattern
        for (arg in args) {
            result = result.replaceFirst(Regex("""\{[^}]+\}"""), arg.toString())
        }
        return result
    }
}
