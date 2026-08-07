package com.sky.mvi.network

import android.content.Context
import androidx.annotation.StringRes
import com.sky.mvi.R

/**
 * @Class: Error
 * @Author: Henry
 * @Date: 2025/2/23 10:32
 * @Description: 错误码枚举，定义网络、解析、超时、SSL等错误类型
 */

enum class Error(private val code: Int, @param:StringRes private val errResId: Int) {

    /**
     * 未知错误
     */
    UNKNOWN(1000, R.string.sky_mvilib_exception_unknown),

    /**
     * 解析错误
     */
    PARSE_ERROR(1001, R.string.sky_mvilib_exception_parse_error),

    /**
     * 网络错误
     */
    NETWORK_ERROR(1002, R.string.sky_mvilib_exception_network_error),

    /**
     * 证书出错
     */
    SSL_ERROR(1004, R.string.sky_mvilib_exception_ssl_error),

    /**
     * 连接超时
     */
    TIMEOUT_ERROR(1006, R.string.sky_mvilib_exception_timeout_error);

    fun getValue(context: Context): String {
        return context.getString(errResId)
    }

    fun getKey(): Int {
        return code
    }

}