package com.sky.mvi.core.model.base

/**
 * 列表分页基类，承载 wanandroid 等分页接口返回的公共字段。
 * 字段使用 var 以便 Moshi 反序列化后回填。
 */
open class BasePage {
    var curPage: Int = 0
    var offset: Int = 0
    var over: Boolean = true
    var pageCount: Int = 0
    var size: Int = 0
    var total: Int = 0
}
