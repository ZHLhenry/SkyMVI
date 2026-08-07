package com.sky.mvi.network

/**
 * @Class: BaseResponse
 * @Author: Henry
 * @Date: 2025/2/23 10:31
 * @Description: 网络响应基类，定义成功判断和数据获取的抽象方法
 */

abstract class BaseResponse<T> {
    //抽象方法，用户的基类继承该类时，需要重写该方法
    abstract fun isSucces(): Boolean

    abstract fun getResponseData(): T

    abstract fun getResponseCode(): Int

    abstract fun getResponseMsg(): String

}