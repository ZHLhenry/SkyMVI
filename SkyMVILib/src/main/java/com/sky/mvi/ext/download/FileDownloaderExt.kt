package com.sky.mvi.ext.download

import android.content.Context
import com.sky.mvi.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * @Class: FileDownloaderExt
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: 下载扩展函数，将下载回调桥接为 Flow / StateFlow 形态，便于 MVI 消费
 */

/**
 * 将下载回调写入 [MutableStateFlow]，适合在 ViewModel 中把下载进度合并进 SkyUiState
 *
 * @param context 用于读取默认错误文案
 * @param downloadState 承载下载状态的 StateFlow
 */
fun downLoadExt(
    context: Context,
    downloadState: MutableStateFlow<DownloadResultState>
): OnDownLoadListener {
    val appContext = context.applicationContext
    return object : OnDownLoadListener {
        override fun onDownLoadPrepare(key: String) {
            downloadState.value = DownloadResultState.onPending()
        }

        override fun onDownLoadError(key: String, throwable: Throwable) {
            downloadState.value = DownloadResultState.onError(
                throwable.message ?: appContext.getString(R.string.sky_mvilib_download_error)
            )
        }

        override fun onDownLoadSuccess(key: String, path: String, size: Long) {
            downloadState.value = DownloadResultState.onSuccess(path, size)
        }

        override fun onDownLoadPause(key: String) {
            downloadState.value = DownloadResultState.onPause()
        }

        override fun onUpdate(key: String, progress: Int, read: Long, count: Long, done: Boolean) {
            downloadState.value = DownloadResultState.onProgress(read, count, progress)
        }
    }
}

/**
 * 以冷流形式发起下载：收集即开始、协程取消即中止下载，是 MVI 中推荐的下载用法
 *
 * 用法示例：
 * ```
 * downloadFlow(context, "apk", url, dir, "app.apk")
 *     .onEach { state -> setState { copy(downloadState = state) } }
 *     .launchIn(viewModelScope)
 * ```
 *
 * @param context 上下文
 * @param key 下载任务唯一标识
 * @param url 下载地址
 * @param savePath 保存目录
 * @param saveName 保存文件名
 * @param reDownload 文件已存在时是否重新下载
 * @param whetherHttps 是否开启忽略 https 证书模式
 */
fun DownLoadManager.downloadFlow(
    context: Context,
    key: String,
    url: String,
    savePath: String,
    saveName: String,
    reDownload: Boolean = false,
    whetherHttps: Boolean = false
): Flow<DownloadResultState> = callbackFlow {
    val appContext = context.applicationContext

    // 标记下载是否已自然结束（成功/失败）。
    // DownLoadManager.cancel() 会删除已下载文件，因此只有在「非自然结束」
    // （即下游取消订阅）时才允许调用，否则会误删下载成功的文件
    var finished = false

    val listener = object : OnDownLoadListener {
        override fun onDownLoadPrepare(key: String) {
            trySend(DownloadResultState.onPending())
        }

        override fun onDownLoadError(key: String, throwable: Throwable) {
            finished = true
            trySend(
                DownloadResultState.onError(
                    throwable.message ?: appContext.getString(R.string.sky_mvilib_download_error)
                )
            )
            close()
        }

        override fun onDownLoadSuccess(key: String, path: String, size: Long) {
            finished = true
            trySend(DownloadResultState.onSuccess(path, size))
            close()
        }

        override fun onDownLoadPause(key: String) {
            trySend(DownloadResultState.onPause())
        }

        override fun onUpdate(key: String, progress: Int, read: Long, count: Long, done: Boolean) {
            trySend(DownloadResultState.onProgress(read, count, progress))
        }
    }

    // downLoad 为挂起函数且会阻塞至下载结束，必须另起协程，否则 awaitClose 无法执行
    val job = launch {
        downLoad(
            tag = key,
            url = url,
            savePath = savePath,
            saveName = saveName,
            reDownload = reDownload,
            whetherHttps = whetherHttps,
            loadListener = listener
        )
    }

    awaitClose {
        job.cancel()
        if (!finished) cancel(key)
    }
}
