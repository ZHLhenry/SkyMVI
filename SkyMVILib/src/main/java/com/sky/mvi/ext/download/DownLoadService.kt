package com.sky.mvi.ext.download

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * @Class: DownLoadService
 * @Author: Henry
 * @Date: 2025/2/23 10:37
 * @Description: Retrofit下载服务接口，支持Range断点下载
 */

interface DownLoadService {
    @Streaming
    @GET
    suspend fun downloadFile(
        @Header("RANGE") start: String,
        @Url url: String
    ): Response<ResponseBody>
}