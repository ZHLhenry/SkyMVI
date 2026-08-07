package com.sky.mvi.network

import android.content.Context
import com.sky.mvi.R

/**
 * @Class: AppException
 * @Author: Henry
 * @Date: 2025/2/23 10:29
 * @Description: 应用异常类，封装错误码、错误消息和错误日志
 */

class AppException : Exception {
    var errorMsg: String //错误消息
    var errCode: Int = 0 //错误码
    var errorLog: String? //错误日志
    var throwable: Throwable? = null

    constructor(
        context: Context,
        errCode: Int,
        error: String?,
        errorLog: String? = "",
        throwable: Throwable? = null
    ) : super(error) {
        this.errorMsg = error ?: context.getString(R.string.sky_mvilib_exception_unknown)
        this.errCode = errCode
        this.errorLog = errorLog ?: this.errorMsg
        this.throwable = throwable
    }

    constructor(context: Context, error: Error, e: Throwable?) {
        errCode = error.getKey()
        errorMsg = error.getValue(context)
        errorLog = e?.message
        throwable = e
    }
}