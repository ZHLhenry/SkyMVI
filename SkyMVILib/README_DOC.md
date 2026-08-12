# SkyMVILib 使用文档

SkyMVILib 是一套基于 **MVI（Model-View-Intent）+ Jetpack Compose + Kotlin Coroutines** 的 Android 业务框架，封装了单向数据流、网络请求、文件下载、缓存、Paging 等通用能力，帮助业务以一致的方式组织页面逻辑。

> 版本：v1.0.0（首发） · 适用 `targetSdk` 34+

---

## 目录

1. [快速接入](#1-快速接入)
2. [MVI 核心契约](#2-mvi-核心契约)
3. [ViewModel 基类](#3-viewmodel-基类)
4. [Compose 页面集成](#4-compose-页面集成)
5. [副作用（Effect）处理](#5-副作用effect处理)
6. [网络请求扩展](#6-网络请求扩展)
7. [文件下载](#7-文件下载)
8. [HTTP 缓存](#8-http-缓存)
9. [Paging3 接入](#9-paging3-接入)
10. [通用工具扩展](#10-通用工具扩展)
11. [常见问题](#11-常见问题)

---

## 1. 快速接入

### 1.1 依赖

在业务模块 `build.gradle.kts` 中依赖本库（具体坐标以发布仓库为准）：

```kotlin
dependencies {
    implementation(project(":SkyMVILib"))
    // 或在 maven 发布后：
    // implementation("com.sky.mvi:skymvilib:1.0.0")
}
```

### 1.2 Application 初始化

库的入口为 `SkyMVILib.init(config)`，必须在 `Application#onCreate` **最前面** 完成；网络监听与前后台监听由 `BaseApplication` 自动注册，建议业务 `Application` 直接继承它：

```kotlin
class App : BaseApplication() {
    override fun onCreate() {
        super.onCreate()                       // 内部会注册网络/前后台监听
        SkyMVILib.init(                         // 必须最先调用
            SkyMVILibConfig.Builder(this)
                .enableXLog(true)               // 是否启用 XLog 日志
                .enableOkHttpLogLib(true)       // 是否启用 OkHttp 日志拦截
                .enableSkyFlow(true)            // 是否启用 SkyFlow 事件总线
                .enableStrictMode(BuildConfig.DEBUG) // Debug 校验 MVI 用法
                .build()
        )
        // 其它初始化...
    }
}
```

> 若宿主 `Application` 不便继承 `BaseApplication`，可在自己的 `Application#onCreate` 中手动调用 `BaseApplication.initAppConfig(this)` 完成等价初始化。

> 注意：`ViewModel` 在首次访问时会调用 `SkyMVILib.requireInit()`，若未初始化会抛 `SkyMVILib.UninitializedException`。

---

## 2. MVI 核心契约

位于 `com.sky.mvi.core.MviContract`，定义单向数据流的三件套约束：

### 2.1 SkyUiState

```kotlin
@Stable
interface SkyUiState
```

页面状态。**约定**：

- 用 `data class` 实现，保证结构化相等，避免无意义重组；
- 所有属性 `val`（不可变），集合使用只读类型。

```kotlin
data class HomeState(
    val isLoading: Boolean = false,
    val list: List<Article> = emptyList(),
    val errorMsg: String? = null
) : SkyUiState
```

单向数据流示意：

```
UI --(Intent)--> ViewModel --(Reducer)--> State --(recompose)--> UI
                       |
                       +--(Effect)--> UI 一次性事件（Toast / 导航 / 弹窗）
```

### 2.2 SkyUiIntent

```kotlin
@Immutable
interface SkyUiIntent
```

用户意图，是 UI 向 ViewModel 传值的**唯一入口**。约定使用 `sealed interface` 收敛一个页面的全部意图：

```kotlin
sealed interface HomeIntent : SkyUiIntent {
    data object Refresh : HomeIntent
    data class ItemClick(val id: Int) : HomeIntent
}
```

### 2.3 SkyUiEffect

```kotlin
@Immutable
interface SkyUiEffect
```

一次性副作用，用于「消费后即失效」的事件（Toast / 导航 / 弹窗）。与 State 的区别：State 描述「此刻是什么样」可重复渲染；Effect 描述「刚刚发生了什么」，只能被消费一次，旋转屏幕不会重复触发。

```kotlin
sealed interface HomeEffect : SkyUiEffect {
    data class ShowToast(val msg: String) : HomeEffect
    data object NavigateToLogin : HomeEffect
}
```

---

## 3. ViewModel 基类

### 3.1 定义

`SkyBaseMviViewModel<S, I, E>` 继承自 `androidx.lifecycle.ViewModel`，泛型依次为 State / Intent / Effect。

```kotlin
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository
) : SkyBaseMviViewModel<HomeState, HomeIntent, HomeEffect>() {

    override fun initialState() = HomeState()           // 必须实现：初始状态

    override fun handleIntent(intent: HomeIntent) {     // 必须实现：意图分发
        when (intent) {
            is HomeIntent.Refresh -> loadData()
            is HomeIntent.ItemClick -> sendEffect(HomeEffect.NavigateDetail(intent.id))
        }
    }
}
```

### 3.2 公开 API

| 成员 | 类型 | 说明 |
| --- | --- | --- |
| `uiState` | `StateFlow<S>` | 页面状态流，UI 通过 `collectAsStateWithLifecycle()` 订阅 |
| `currentState` | `S` | 同步读取当前状态（ViewModel 内部使用） |
| `initialState()` | `protected abstract fun` | 提供页面初始状态 |
| `setState(reducer)` | `protected fun S.() -> S` | 状态归约，基于当前状态生成新状态 |
| `handleIntent(intent)` | `protected abstract fun` | 处理来自 UI 的意图 |
| `sendIntent(intent)` | `fun @MainThread` | UI 发送意图的**唯一入口**（Channel 串行消费，保证顺序） |
| `invoke(intent)` | `operator fun @MainThread` | `sendIntent` 的操作符别名：`viewModel(HomeIntent.Refresh)` |
| `effect` | `Flow<E>` | 副作用流，UI 通过 `SkyCollectEffect` 消费 |
| `sendEffect(effect)` | `protected fun` | 发送一次性副作用（非挂起） |
| `postEffect(effect)` | `protected suspend fun` | 发送一次性副作用（挂起，缓冲满时挂起等待） |
| `singleFlight(key, block)` | `protected fun` | 防重入执行，同一 key 的任务未结束前重复调用直接忽略（防下拉刷新连点） |
| `stateInViewModel(initialValue)` | `protected fun Flow<T>.stateInViewModel(initialValue: T): StateFlow<T>` | 将 Flow 转为在 `viewModelScope` 内共享的 `StateFlow`，订阅停止 5 秒后自动断开上游 |

> `setState` 内部使用 CAS 循环而非直接赋值，多协程并发 `copy` 时不会互相覆盖。

---

## 4. Compose 页面集成

### 4.1 SkyMviScreen（推荐）

位于 `com.sky.mvi.core.compose`，一次性完成「订阅状态 + 消费副作用 + 分发意图」三件事绑定：

```kotlin
@Composable
fun HomeRoute(viewModel: HomeViewModel = hiltViewModel()) {
    SkyMviScreen(
        viewModel = viewModel,
        onEffect = { effect ->
            when (effect) {
                is HomeEffect.ShowToast -> toast(effect.msg)
                is HomeEffect.NavigateDetail -> navController.navigate("detail/${effect.id}")
            }
        }
    ) { state, onIntent ->
        HomeContent(state = state, onIntent = onIntent)
    }
}
```

| 参数 | 说明 |
| --- | --- |
| `viewModel` | MVI ViewModel |
| `lifecycleState` | 消费副作用的最低生命周期状态，默认 `Lifecycle.State.STARTED` |
| `onEffect` | 一次性副作用处理（普通 lambda，内部自动转 suspend 收集协程） |
| `content` | 页面内容，接收 `(state, onIntent)` |

> `content` 只依赖不可变的 `S` 与 `(I) -> Unit`，不持有 ViewModel，因此可直接被 `@Preview` 复用。

### 4.2 其它可组合辅助

| 函数 | 说明 |
| --- | --- |
| `viewModel.collectSkyState()` | 生命周期感知地订阅状态，等价于 `uiState.collectAsStateWithLifecycle()` |
| `SkyCollectEffect(flow, lifecycleState, collector)` | 在指定生命周期内安全消费一次性事件（页面进后台自动暂停，期间事件由 Channel 缓存不丢） |
| `SkyLaunchedIntent(viewModel, intent)` | 页面首次进入时发送一次初始化 Intent（如首屏加载） |
| `rememberSkyIntentDispatcher(viewModel)` | 返回 `(I) -> Unit` 分发器，向下传递让子 Composable 与 ViewModel 解耦 |

```kotlin
SkyLaunchedIntent(viewModel, HomeIntent.Refresh)

SkyCollectEffect(viewModel.effect) { effect ->
    when (effect) {
        is HomeEffect.ShowToast -> snackbarHostState.showSnackbar(effect.msg)
    }
}
```

---

## 5. 副作用（Effect）处理

### 5.1 通用处理器

`rememberSkyMviEffectHandler` 自动分发两类标准副作用（导航、Toast），其余交给自定义兜底：

```kotlin
val navController = rememberNavController()
val onEffect = rememberSkyMviEffectHandler(navController) { effect ->
    // 处理自定义 Effect
}
SkyMviScreen(viewModel = vm, onEffect = onEffect) { state, onIntent -> /* ... */ }
```

> 设计要点：`SkyUiEffect` 由 Channel 驱动，**只能被一个订阅者消费**。`SkyMviScreen` 内部已启动唯一的 `SkyCollectEffect`，因此通用处理器只在 `onEffect` 中处理，**不会再启动新收集协程**，避免与 `SkyMviScreen` 竞争消费。

### 5.2 Toast 副作用

`SkyToastEffect`（位于 `com.sky.mvi.widget.toast`）：

```kotlin
interface SkyToastEffect : SkyUiEffect {
    val message: String
    val duration: Int get() = Toast.LENGTH_SHORT
}

fun SkyHandleToastEffects(context: Context, effect: SkyToastEffect)
```

ViewModel 的 Effect 实现 `SkyToastEffect` 后，会被 `SkyHandleToastEffects` 自动弹出 Toast。

### 5.3 导航副作用

`SkyNavigationEffect`（位于 `com.sky.mvi.core.navigation`）：

```kotlin
interface SkyNavigationEffect {
    fun handle(controller: NavHostController)
}

fun SkyHandleNavigationEffects(navController: NavHostController, effect: SkyNavigationEffect)
```

另提供扩展 `Context.openBrowser(url)` 跳转到外部浏览器。

---

## 6. 网络请求扩展

位于 `com.sky.mvi.ext`，通过 `SkyBaseMviViewModel` 的扩展函数封装请求、状态解析与异常处理。
**所有网络 block 内部已切到 `Dispatchers.IO`，不会阻塞主线程。**

### 6.1 回调式（推荐在 `handleIntent` 内直接 `setState`）

```kotlin
apiRequest(
    context = getApplication(),
    block = { api.getArticleList(page) },          // suspend 请求体
    success = { setState { copy(list = it.datas) } },
    error = { setState { copy(errorMsg = it.errorMsg) } }
)
```

| 函数 | 说明 |
| --- | --- |
| `apiRequest(context, block, success, error?, onStart?, onComplete?)` | 校验业务状态码，失败走 `error` 回调 |
| `apiRequestNoCheck(context, block, success, error?, onStart?, onComplete?)` | 不校验业务码，直接返回原始数据 |

- `onStart`：请求开始，常用于置 loading 态；
- `onComplete`：无论成败都执行（即使协程被取消也会执行，避免 loading 卡死）。

### 6.2 StateFlow 式（适合把结果直接喂给 `uiState`）

```kotlin
val resultState = MutableStateFlow<ResultState<List<Article>>>(ResultState.onAppLoading())
apiFlow(context, resultState) { api.getArticleList(page) }
```

| 函数 | 说明 |
| --- | --- |
| `apiFlow(context, resultState, isShowLoading?, loadingMessage?, block)` | 结果写入 `MutableStateFlow<ResultState<T>>` |
| `apiFlowNoCheck(context, resultState, isShowLoading?, loadingMessage?, block)` | 同上，不校验业务码 |

### 6.3 ResultState 与异常

```kotlin
sealed interface ResultState<out T> {
    data class Loading(val message: String = "") : ResultState<Nothing>
    data class Success<out T>(val data: T) : ResultState<T>
    data class Error(val exception: AppException) : ResultState<Nothing>
    data object Idle : ResultState<Nothing>
}
```

常用扩展（写入 `MutableStateFlow<ResultState<T>>`）：

| 函数 | 说明 |
| --- | --- |
| `parseResult(context, BaseResponse)` | 校验业务码，成功脱壳，失败转 `AppException` |
| `parseResult(T)` | 直接写入成功结果 |
| `parseException(context, Throwable)` | 异常转 `ResultState.Error` |

> 历史拼写 `paresResult` / `paresException` 作为 `@Deprecated` 别名保留，请改用 `parseResult` / `parseException`。

### 6.4 底层 API

- `executeResponse(context, response, success)`：解析 `BaseResponse` 的私有扩展，成功回调数据，失败抛 `AppException`。
- `launchIO(block)`：在 `viewModelScope` 中切到 IO 启动协程。

---

## 7. 文件下载

位于 `com.sky.mvi.ext.download`，核心类 `DownLoadManager`（已 `@Inject @Singleton`，由 Hilt 直接注入）：

```kotlin
@Inject lateinit var downLoadManager: DownLoadManager

downLoadManager.download(
    path = "/sdcard/download/",
    url = "https://example.com/apk/app.apk",
    fileName = "app.apk",
    onProgress = { current, total -> /* 更新进度 */ },
    onSuccess = { file -> /* 完成 */ },
    onError = { e -> /* 失败 */ }
)
```

> 下载进度通过 `DownLoadProgressListener` 回调，下载结果用 `DownloadResultState` 表达（Loading / Success / Error）。

---

## 8. HTTP 缓存

`CacheInterceptor`（位于 `com.sky.mvi.network.interceptor`）按网络状态设置缓存头：

- **离线**：仅返回已缓存内容（容忍 `day` 天过期数据），即 `only-if-cached, max-stale`；
- **在线**：返回最新数据并缓存 1 小时（`max-age=3600`）。

构造器：`CacheInterceptor(var day: Int = 7)`（`day` 控制离线可容忍的过期天数）。

> 缓存生效依赖 OkHttp 已配置 `Cache` 目录（见 `BaseNetworkApi`），否则拦截器设置的头不会实际落盘。

---

## 9. Paging3 接入

`BasePagingSource`（位于 `com.sky/mvi/paging`）封装了 `PagingSource<Int, T>` 的通用模板，业务只需提供「根据页码取一页数据」的挂起块：

```kotlin
class ArticlePagingSource(
    private val repo: HomeRepository
) : BasePagingSource<Article>() {

    override suspend fun loadData(page: Int, pageSize: Int): List<Article> {
        return repo.getArticleList(page, pageSize).datas
    }
}
```

对应 `BasePagingSource.load()` 会处理 `prevKey` / `nextKey` 的拼接，并区分「空页」与「异常」。

---

## 10. 通用工具扩展

### 10.1 View 扩展（`com.sky.mvi.ext.view`）

| 函数 | 说明 |
| --- | --- |
| `View.clickNoRepeat(interval, action)` | 防重复点击（默认 0.5s），基于 View 自身 tag 记录时间，避免多 View 冲突 |
| `View.visible() / invisible() / gone()` | 可见性快捷设置 |
| `View.visibleOrGone(show)` / `visibleOrInvisible(show)` | 按布尔值切换可见性 |
| `Bitmap.toBitmap(scale)` | Bitmap 缩放工具 |

### 10.2 工具扩展（`com.sky.mvi.ext.util`）

| 函数 | 说明 |
| --- | --- |
| `Context.checkAccessibilityServiceEnabled(name)` | 检测指定无障碍服务是否已开启 |
| `Context.copyToClipboard(text, label?)` | 复制到剪贴板 |
| `dp2px / px2dp` | 尺寸单位换算 |
| `setOnclick(vararg views, onClick)` | 批量设置点击 |
| `setDebouncedClickListener(vararg views, interval, onClick)` | 批量设置防重复点击 |

### 10.3 前后台监听（`com.sky.mvi.ext.lifecycle`）

```kotlin
AppLifeObserver.isForegroundState   // StateFlow<Boolean>，应用是否在前台
AppLifeObserver.isForeground / isBackground
```

> 已通过 `DefaultLifecycleObserver` 实现，`BaseApplication` 在 `install` 时自动注册。

### 10.4 网络状态（`com.sky.mvi.network.manager`）

```kotlin
NetworkStateManager.instance.updateNetworkState(isAvailable)  // 网络状态变更
NetworkStateManager.instance.isNetworkState()                 // 当前是否有网
```

> v1.0.0 起网络监听基于 `ConnectivityManager.NetworkCallback`（替代已废弃的 `CONNECTIVITY_ACTION` 广播），由 `NetworkStateReceive` 在 `BaseApplication` 中注册。

---

## 11. 常见问题

**Q：调用 ViewModel 报 `SkyMVILib.UninitializedException`？**
A：未先调用 `SkyMVILib.init(config)`。请确保在 `Application#onCreate` 最前面初始化。

**Q：Effect 偶发不触发？**
A：`SkyUiEffect` 由 Channel 驱动，只能被一个订阅者消费。请保证页面内**只通过一处**（`SkyMviScreen` 的 `onEffect`，或通过 `SkyCollectEffect`、`SkyHandleMviEffects`）收集，避免多处竞争消费导致事件被其中一个吞掉。

**Q：旋转屏幕后 Toast/导航重复弹出？**
A：把一次性事件放在 `SkyUiEffect`（而非 `SkyUiState`）中，并通过 `SkyCollectEffect` 消费即可避免重复。

**Q：下拉刷新连点导致重复请求？**
A：在 `handleIntent` 的请求逻辑外包一层 `singleFlight("key") { ... }`，同一 key 的任务未结束前重复调用会被忽略。
