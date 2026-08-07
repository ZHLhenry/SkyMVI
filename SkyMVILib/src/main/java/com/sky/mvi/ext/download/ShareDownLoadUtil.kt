package com.sky.mvi.ext.download

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.sky.mvi.base.BaseApplication.Companion.app
import androidx.core.content.edit

/**
 * @Class: ShareDownLoadUtil
 * @Author: Henry
 * @Date: 2025/2/23 10:39
 * @Description: SharedPreferences下载进度持久化工具，记录已下载字节数实现断点续传
 */

object ShareDownLoadUtil {

    private var path = Build.BRAND + "_" + Build.MODEL + "_" + "SkyMVI_Download_SP"
    private val sp: SharedPreferences = app.getSharedPreferences(path, Context.MODE_PRIVATE)

    fun setPath(path: String) {
        ShareDownLoadUtil.path = path
    }

    fun putBoolean(key: String, value: Boolean) {
        sp.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, defValue: Boolean): Boolean {
        return sp.getBoolean(key, defValue)
    }

    fun putString(key: String, value: String) {
        sp.edit { putString(key, value) }
    }

    fun getString(key: String, defValue: String): String? {
        return sp.getString(key, defValue)
    }

    fun putInt(key: String, value: Int) {
        sp.edit { putInt(key, value) }
    }

    fun getInt(key: String, defValue: Int): Int {
        return sp.getInt(key, defValue)
    }

    fun putLong(key: String?, value: Long) {
        sp.edit { putLong(key, value) }
    }

    fun getLong(key: String, defValue: Long): Long {
        return sp.getLong(key, defValue)
    }

    fun remove(key: String) {
        sp.edit { remove(key) }
    }

    fun clear() {
        sp.edit { clear() }
    }

}