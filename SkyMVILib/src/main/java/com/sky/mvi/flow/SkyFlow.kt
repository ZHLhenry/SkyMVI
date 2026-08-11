@file:Suppress("UNCHECKED_CAST", "unused")

package com.sky.mvi.flow

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.sky.mvi.SkyMVILib
import com.sky.mvi.SkyMVILib.UninitializedException
import com.sky.mvi.ext.util.logE
import com.sky.mvi.ext.util.logI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * @Class: SkyFlow
 * @Author: Henry
 * @Date: 2025/3/5 11:01
 * @Description: 基于 SharedFlow 的全局事件总线，支持普通事件流与粘性事件流。
 *
 * —— 在 MVI 架构中的定位 ——
 * SkyMVI 的单向数据流里：
 *   UI --(Intent)--> ViewModel --(Reducer)--> State --(recompose)--> UI
 *                          |
 *                          +--(SkyUiEffect)--> UI 一次性副作用（页面内：Toast / 导航 / 弹窗）
 *
 * [com.sky.mvi.core.SkyUiEffect] 由 ViewModel 的 Channel 驱动，只能被「当前页面」消费一次，
 * 适合页面内、与生命周期强绑定的瞬时事件。
 *
 * 而 SkyFlow 是 **跨页面 / 跨模块** 的全局事件总线：
 *   - 发布方不需要持有订阅方引用（解耦模块间依赖）；
 *   - 粘性事件让「后启动的页面」也能收到最近一次广播（如登录失效、主题切换）；
 *   - 与 SkyUiEffect 互补：本组件只负责「跨组件通知」，收到后通常再转成本页面的 SkyUiEffect。
 *
 * 典型用法（详见 sample 中的 TokenOutInterceptor / AppRoot / HomeScreen）：
 * ```
 * // 发布（fire-and-forget，不阻塞调用方、不泄漏协程）
 * SkyFlow.withStick<TokenExpiredEvent>(FlowKeys.TOKEN_EXPIRED).post(event)
 *
 * // 订阅（跟随 LifecycleOwner 自动解绑）
 * LaunchedEffect(Unit) {
 *     SkyFlow.withStick<TokenExpiredEvent>(FlowKeys.TOKEN_EXPIRED)
 *         .register(lifecycleOwner = lifecycleOwner) { event -> ... }
 * }
 * ```
 */

object SkyFlow {
    private const val TAG = "SkyFlow"

    /** key -> 普通事件流 */
    private val flowMap = ConcurrentHashMap<String, SkyFlowEvent<*>>()

    /** key -> 粘性事件流 */
    private val flowStickMap = ConcurrentHashMap<String, SkyFlowStickEvent<*>>()

    /** key -> 首次注册时的事件类型，用于防止泛型擦除导致的错误强转 */
    @PublishedApi
    internal val typeMap = ConcurrentHashMap<String, KClass<*>>()

    internal fun requireSkyFlowInit() {
        if (SkyMVILib.getConfig()?.skyFlowLibEnabled == false) {
            throw UninitializedException(
                "Please add the \"enableSkyFlowLib(true)\" attribute in the SkyMVILibConfig configuration."
            )
        }
    }

    /**
     * 获取或创建普通事件流。
     *
     * 同一 [key] 必须始终对应同一事件类型 [T]，若被复用为不同类型会抛出
     * [IllegalArgumentException]（而非把运行时 [ClassCastException] 暴露到订阅处）。
     */
    inline fun <reified T : Any> with(key: String): SkyFlowEvent<T> {
        checkType(key, T::class)
        return rawWith(key) { SkyFlowEvent<T>(key) }
    }

    /**
     * 获取或创建粘性事件流。
     * @see with
     */
    inline fun <reified T : Any> withStick(key: String): SkyFlowEvent<T> {
        checkType(key, T::class)
        return rawWithStick(key)
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> rawWith(key: String, factory: () -> SkyFlowEvent<T>): SkyFlowEvent<T> {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        return flowMap.getOrPut(key) { factory() } as SkyFlowEvent<T>
    }

    @PublishedApi
    @Suppress("UNCHECKED_CAST")
    internal fun <T : Any> rawWithStick(key: String): SkyFlowEvent<T> {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        return flowStickMap.getOrPut(key) { SkyFlowStickEvent<T>(key) } as SkyFlowEvent<T>
    }

    /**
     * 校验同一 key 的事件类型一致，避免泛型擦除导致的隐蔽错误强转。
     */
    @PublishedApi
    internal fun checkType(key: String, expect: KClass<*>) {
        val recorded = typeMap[key]
        if (recorded != null && recorded != expect) {
            throw IllegalArgumentException(
                "SkyFlow key \"$key\" already registered with type ${recorded.simpleName}, " +
                        "cannot be reused as ${expect.simpleName}."
            )
        }
        typeMap.putIfAbsent(key, expect)
    }

    /**
     * 清理当前没有任何订阅者的事件流，释放内存。
     *
     * 先判断 `subscriptionCount`，再原子移除，避免与并发注册/反注册产生竞态。
     */
    fun clearUnusedFlow() {
        SkyMVILib.requireInit()
        requireSkyFlowInit()
        evictIfNoSubscriber(flowMap)
        evictIfNoSubscriber(flowStickMap)
    }

    private fun <F : SkyFlowEvent<*>> evictIfNoSubscriber(map: ConcurrentHashMap<String, F>) {
        val iterator = map.entries.iterator()
        for ((key, flow) in iterator) {
            if (flow.subscriberCount == 0 && map[key] === flow) {
                map.remove(key)
                typeMap.remove(key)
                "SkyFlow - Evict unused flow: $key".logI(tag = TAG)
            }
        }
    }

    /**
     * 普通事件流（非粘性）。
     *
     * 生命周期管理：
     * 1. 传入 [LifecycleOwner] 时，协程在 `lifecycleScope` 中启动，页面销毁即随
     *    Lifecycle 自动取消订阅；
     * 2. 同时注册 [DefaultLifecycleObserver]，在 `onDestroy` 时主动移除无订阅者的 flow，
     *    作为兜底清理，避免协程异常退出导致 observer 滞留造成内存泄漏。
     */
    open class SkyFlowEvent<T : Any>(private val key: String) : DefaultLifecycleObserver {
        /** 是否为粘性事件流，子类 [SkyFlowStickEvent] 覆写为 true，用于决定从哪个 map 移除 */
        protected open val isSticky: Boolean get() = false

        // 在 lazy 中构建，确保子类覆写的 createFlow 在父类构造阶段不会被提前访问（避免 NPE）
        private var _events: MutableSharedFlow<T>? = null

        private fun ensureFlow(): MutableSharedFlow<T> {
            if (_events == null) _events = createFlow()
            return _events!!
        }

        /** 子类可覆写缓冲策略（如自定义 replay / extraBufferCapacity） */
        protected open fun createFlow(): MutableSharedFlow<T> = MutableSharedFlow(
            replay = 0,
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        /** 当前订阅者数量（公开只读，便于监控与测试） */
        val subscriberCount: Int get() = ensureFlow().subscriptionCount.value

        /** 对外暴露的不可变流 */
        private val events get() = ensureFlow().asSharedFlow()

        /**
         * 注册事件监听。
         *
         * @param lifecycleOwner 生命周期所有者，用于自动取消订阅；不传则必须传 [scope]
         * @param scope 协程作用域；与 [lifecycleOwner] 二选一
         * @param context 协程上下文，默认 [Dispatchers.Main.immediate]（UI 回调首选）
         * @param filter 事件过滤条件，默认不过滤
         * @param action 事件处理逻辑，运行在 [context] 指定的线程
         */
        fun register(
            lifecycleOwner: LifecycleOwner? = null,
            scope: CoroutineScope? = null,
            context: CoroutineContext = Dispatchers.Main.immediate,
            filter: (T) -> Boolean = { true },
            action: (t: T) -> Unit
        ) {
            val targetScope = lifecycleOwner?.lifecycleScope ?: scope
            requireNotNull(targetScope) { "Either lifecycleOwner or scope must be provided" }

            targetScope.launch(context) {
                events.filter(filter).collect { event ->
                    runCatching { action(event) }
                        .onFailure {
                            "SkyFlowEvent - Action error: ${it.message}".logE(tag = TAG)
                        }
                }
            }
            // 绑定生命周期：作为兜底清理，页面销毁时移除无订阅者的 flow
            lifecycleOwner?.lifecycle?.addObserver(this)
        }

        /**
         * 发送事件（非挂起，fire-and-forget）。
         *
         * 通过 `tryEmit` 立即投递，不依赖外部 scope、不阻塞调用方、不会造成协程泄漏，
         * 最适合事件总线场景。
         *
         * @return 是否成功投递（缓冲区满被丢弃时返回 false）
         */
        fun post(event: T): Boolean =
            runCatching { ensureFlow().tryEmit(event) }
                .onFailure {
                    "SkyFlowEvent - post Error: ${it.message}".logE(tag = TAG)
                }.getOrDefault(false)

        /**
         * 生命周期结束时自动清理（兜底）。
         * 协程订阅通常已随 lifecycleScope 取消，这里仅负责移除无订阅者的 flow。
         */
        override fun onDestroy(owner: LifecycleOwner) {
            super.onDestroy(owner)
            owner.lifecycle.removeObserver(this)
            if (subscriberCount == 0) {
                removeFlow(key, isSticky)
            }
        }

        /**
         * 手动销毁：当没有任何订阅者时从注册表移除。
         */
        fun destroy() {
            if (subscriberCount == 0) {
                removeFlow(key, isSticky)
            }
        }
    }

    /**
     * 粘性事件流：保留最近一个事件，后订阅者立即收到 replay。
     * 库内部实现细节，[withStick] 以 [SkyFlowEvent] 形式返回给消费者，消费者无需感知此类型。
     */
    internal class SkyFlowStickEvent<T : Any>(key: String) : SkyFlowEvent<T>(key) {
        override val isSticky: Boolean get() = true
        override fun createFlow(): MutableSharedFlow<T> = MutableSharedFlow(
            replay = 1, // 粘性事件，保留最新一个事件
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    /**
     * 从对应的注册表中移除事件流（普通事件走 [flowMap]，粘性事件走 [flowStickMap]）。
     */
    internal fun removeFlow(key: String, sticky: Boolean) {
        if (sticky) flowStickMap.remove(key) else flowMap.remove(key)
        typeMap.remove(key)
    }
}
