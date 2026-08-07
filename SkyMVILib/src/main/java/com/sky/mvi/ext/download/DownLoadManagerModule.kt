package com.sky.mvi.ext.download

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * @Class: DownLoadManagerModule
 * @Author: Henry
 * @Date: 2025/2/23 10:36
 * @Description: DownLoadManager的Hilt依赖注入Module
 */

@Module
@InstallIn(SingletonComponent::class)
object DownLoadManagerModule {
    @Singleton
    @Provides
    fun provideDownLoadManager(): DownLoadManager {
        return DownLoadManager()
    }

}