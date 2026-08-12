package com.sky.mvi.core

import androidx.annotation.MainThread
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sky.mvi.SkyMVILib
import com.sky.mvi.ext.util.logE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * @Class: SkyBaseMviViewModel
 * @Author: Henry
 * @Date: 2026/08/03
 * @Description: MVI 架构 ViewModel 基类，实现 Intent 分发、State 归约与 Effect 单次消费
 *
 * 三条数据通道：
 * - [uiState]：StateFlow，粘性、去重，页面重建后自动恢复；
 * - [effect]：Channel，一次性事件，不会重放，屏幕旋转不会重复触发；
 * - [sendIntent]：UI 层唯一入口，所有交互都转成 Intent 进入。
 *
 * 典型用法：
 * ```
 * @HiltViewModel
 * class HomeViewModel @Inject constructor(
 *     private val repo: HomeRepository
 * ) : SkyBaseMviViewModel<HomeState, HomeIntent, HomeEffect>() {
 *
 *     override fun initialState() = HomeState()
 *
 *     override fun handleIntent(intent: HomeIntent) {
 *         when (intent) {
 *             is HomeIntent.Refresh -> loadData()
 *             is HomeIntent.ItemClick -> sendEffect(HomeEffect.NavigateDetail(intent.id))
 *         }
 *     }
 *
 *     private fun loadData() {
 *         setState { copy(isLoading = true) }
 *         // ...
 *     }
 * }
 * ```
 */
abstract class SkyBaseMviViewModel<S : SkyUiState, I : SkyUiIntent, E : SkyUiEffect> : ViewModel() {

    private val tag: String = this::class.java.simpleName

    init {
        SkyMVILib.requireInit()
    }

    // ---------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------

    private val _uiState: MutableStateFlow<S> by lazy { MutableStateFlow(initialState()) }

    /**
     * 页面状态流。UI 层通过 `collectAsStateWithLifecycle()` 订阅。
     *
     * StateFlow 自带去重，State 为 data class 时，相同内容不会触发重组。
     */
    val uiState: StateFlow<S> by lazy { _uiState.asStateFlow() }

    /**
     * 同步读取当前状态，供 ViewModel 内部逻辑判断使用
     */
    protected val currentState: S get() = _uiState.value

    /**
     * 提供页面初始状态，子类必须实现
     */
    protected abstract fun initialState(): S

    /**
     * 状态归约：基于当前状态生成新状态。
     *
     * MutableStateFlow 的赋值本身是线程安全的，且 `update` 采用 CAS 循环，
     * 可保证并发调用下 reducer 不丢更新。
     *
     * ```
     * setState { copy(isLoading = false, list = data) }
     * ```
     */
    protected fun setState(reducer: S.() -> S) {
        // 使用 CAS 循环而非直接赋值，避免多协程并发 copy 时互相覆盖
        while (true) {
            val prev = _uiState.value
            val next = prev.reducer()
            if (prev == next) return
            if (_uiState.compareAndSet(prev, next)) return
        }
    }

    // ---------------------------------------------------------------------
    // Intent
    // ---------------------------------------------------------------------

    /**
     * Intent 通道。UNLIMITED 保证快速连续点击不会丢事件
     */
    private val intentChannel = Channel<I>(capacity = Channel.UNLIMITED)

    /**
     * 处理来自 UI 的意图，子类必须实现
     */
    protected abstract fun handleIntent(intent: I)

    init {
        // 串行消费 Intent，保证状态变更顺序与用户操作顺序一致
        intentChannel.receiveAsFlow()
            .onEach { intent ->
                runCatching { handleIntent(intent) }
                    .onFailure {
                        it.printStackTrace()
                        "handleIntent error: ${it.message}".logE(tag)
                    }
            }
            .launchIn(viewModelScope)
    }

    /**
     * UI 层发送意图的唯一入口
     */
    @MainThread
    fun sendIntent(intent: I) {
        val result = intentChannel.trySend(intent)
        if (result.isFailure) {
            "sendIntent failed: $intent".logE(tag)
        }
    }

    /**
     * [sendIntent] 的操作符别名，调用更简洁：`viewModel(HomeIntent.Refresh)`
     */
    @MainThread
    operator fun invoke(intent: I) = sendIntent(intent)

    // ---------------------------------------------------------------------
    // Effect
    // ---------------------------------------------------------------------

    /**
     * 副作用通道。
     *
     * 使用 Channel 而非 SharedFlow：Channel 保证事件「有且仅有一个订阅者消费一次」，
     * 页面重建期间发出的事件会被缓存，重新订阅后补发，不会丢失也不会重复。
     */
    private val effectChannel = Channel<E>(
        capacity = Channel.BUFFERED,
        onBufferOverflow = BufferOverflow.SUSPEND
    )

    /**
     * 副作用流，UI 层通过 `SkyCollectEffect(viewModel.effect) { ... }` 消费
     */
    val effect: Flow<E> = effectChannel.receiveAsFlow()

    /**
     * 发送一次性副作用（挂起版本，缓冲区满时挂起等待）
     */
    protected suspend fun postEffect(effect: E) {
        effectChannel.send(effect)
    }

    /**
     * 发送一次性副作用（非挂起版本，内部在 viewModelScope 中发送）
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch { effectChannel.send(effect) }
    }

    // ---------------------------------------------------------------------
    // 工具
    // ---------------------------------------------------------------------

    /**
     * 单飞锁：用于 [singleFlight]，避免重复请求
     */
    private val singleFlightMutex = Mutex()
    private val runningKeys = mutableSetOf<String>()

    /**
     * 防重入执行：同一 key 的任务未结束前，重复调用直接忽略。
     * 适合防止「下拉刷新连点」造成的重复请求。
     */
    protected fun singleFlight(key: String, block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            val acquired = singleFlightMutex.withLock {
                if (runningKeys.contains(key)) false else runningKeys.add(key)
            }
            if (!acquired) return@launch
            try {
                block()
            } finally {
                singleFlightMutex.withLock { runningKeys.remove(key) }
            }
        }
    }

    /**
     * 将任意 Flow 转换为在 viewModelScope 内共享的 StateFlow，
     * 订阅停止 5 秒后自动断开上游，配置变更（旋屏）期间不会重启
     */
    protected fun <T> Flow<T>.stateInViewModel(initialValue: T): StateFlow<T> =
        stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initialValue
        )

    override fun onCleared() {
        intentChannel.close()
        effectChannel.close()
    }
}

/**
 * 无副作用场景的简化基类，省去 Effect 泛型
 */
abstract class SkySimpleMviViewModel<S : SkyUiState, I : SkyUiIntent> :
    SkyBaseMviViewModel<S, I, SkyUiEffect>()

/**
 * 内部保留：部分场景需要以 SharedFlow 暴露 Effect（如多订阅者），
 * 使用 replay=0 的 SharedFlow 即可
 */
internal fun <T> mutableEventFlow(): MutableSharedFlow<T> = MutableSharedFlow(
    replay = 0,
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST
)
