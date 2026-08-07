package com.sky.mvi.sample.ui

import androidx.compose.runtime.Composable
import com.sky.mvi.base.activity.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * 主 Activity：仅负责承载 Compose 内容，业务逻辑全部下沉到 MVI ViewModel。
 */
@AndroidEntryPoint
class MainActivity : BaseComposeActivity() {
    @Composable
    override fun Content() {
        AppRoot()
    }
}
