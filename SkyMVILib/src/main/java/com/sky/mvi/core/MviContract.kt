package com.sky.mvi.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * @Class: MviContract
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI 架构契约三件套，定义单向数据流中 State / Intent / Effect 的类型约束
 *
 * 单向数据流：
 *
 *   UI --(Intent)--> ViewModel --(Reducer)--> State --(recompose)--> UI
 *                          |
 *                          +--(Effect)--> UI 一次性事件（Toast / 导航 / 弹窗）
 */

/**
 * 页面状态。
 *
 * 约定：
 * 1. 必须使用 `data class` 实现，保证结构化相等，避免无意义重组；
 * 2. 必须是不可变的（所有属性 `val`），集合请使用只读类型；
 * 3. 一个页面只应有一个 State，用属性组合表达所有 UI 状态。
 *
 * 标注 [Stable] 是为了告知 Compose 编译器：该类型的相等性判断可靠，
 * 从而在状态未变化时跳过重组。
 *
 * ```
 * data class HomeState(
 *     val isLoading: Boolean = false,
 *     val list: List<Article> = emptyList(),
 *     val errorMsg: String? = null
 * ) : SkyUiState
 * ```
 */
@Stable
interface SkyUiState

/**
 * 用户意图 / 页面事件，是 UI 层向 ViewModel 传递的唯一入口。
 *
 * 约定：使用 `sealed interface` 收敛一个页面的全部意图。
 *
 * ```
 * sealed interface HomeIntent : SkyUiIntent {
 *     data object Refresh : HomeIntent
 *     data class ItemClick(val id: Int) : HomeIntent
 * }
 * ```
 */
@Immutable
interface SkyUiIntent

/**
 * 一次性副作用，用于「消费后即失效」的事件，如 Toast、导航、震动。
 *
 * 与 State 的区别：State 描述「页面此刻是什么样」，可重复渲染；
 * Effect 描述「刚刚发生了什么」，只能被消费一次，不会因旋转屏幕重复触发。
 *
 * ```
 * sealed interface HomeEffect : SkyUiEffect {
 *     data class ShowToast(val msg: String) : HomeEffect
 *     data object NavigateToLogin : HomeEffect
 * }
 * ```
 */
@Immutable
interface SkyUiEffect
