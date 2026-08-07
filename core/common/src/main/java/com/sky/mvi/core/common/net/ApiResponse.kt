package com.sky.mvi.core.common.net

import com.sky.mvi.network.BaseResponse

/**
 * 统一响应体，继承 SkyMVILib 的 [BaseResponse]。
 * 业务成功判断由 [isSucces] 实现，[errorCode] == 0 视为成功。
 */
data class ApiResponse<T>(
    val errorCode: Int,
    val errorMsg: String,
    val data: T
) : BaseResponse<T>() {

    override fun isSucces() = errorCode == 0

    override fun getResponseCode() = errorCode

    override fun getResponseData() = data

    override fun getResponseMsg() = errorMsg
}
