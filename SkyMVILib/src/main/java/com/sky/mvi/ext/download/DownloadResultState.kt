package com.sky.mvi.ext.download

/**
 * @Class: DownloadResultState
 * @Author: Henry
 * @Date: 2025/2/23 10:37
 * @Description: 下载状态密封类，封装等待、进度、成功、暂停、错误状态
 */

sealed class DownloadResultState {
    companion object {

        fun onPending(): DownloadResultState = Pending

        fun onProgress(soFarBytes: Long, totalBytes: Long, progress: Int): DownloadResultState =
            Progress(soFarBytes, totalBytes, progress)

        fun onSuccess(filePath: String, totalBytes: Long): DownloadResultState =
            Success(filePath, totalBytes)

        fun onPause(): DownloadResultState = Pause

        fun onError(errorMsg: String): DownloadResultState = Error(errorMsg)
    }

    object Pending : DownloadResultState()
    data class Progress(val soFarBytes: Long, val totalBytes: Long, val progress: Int) :
        DownloadResultState()

    data class Success(val filePath: String, val totalBytes: Long) : DownloadResultState()
    object Pause : DownloadResultState()
    data class Error(val errorMsg: String) : DownloadResultState()
}