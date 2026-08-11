# SkyMVILib

基于 **Jetpack Compose** + **MVI 架构**的快速开发库（包名 `com.sky.mvi`）。
由 [`SkyMVVM`](https://github.com/...) 演进而来：用单向数据流的 **MVI** 取代原 MVVM，
将 LiveData / DataBinding / ViewBinding 体系全面迁移到 `StateFlow` / `Channel` /
`SharedFlow`，并原生支持 Compose。

本库在「MVI 内核 + Flow 化网络层」之外，还内置了 **Compose 导航封装** 与 **通用 UI 组件**，
让业务方以最少的样板代码搭建一个完整的 Compose 页面。

---

## 模块结构

```
SkyMVI/
├── SkyMVILib/        # 核心库：MVI 框架 + 网络层 + Flow/工具扩展 + Compose 基类 + 导航封装 + UI 组件
├── core/common/      # 通用能力：ApiResponse / ApiService / 拦截器 / 全局配置
├── core/model/       # 数据模型：BasePage + 业务 Bean（可由业务方自行替换）
└── app/              # 示例 Demo：登录页 + 文章列表页 + 详情页（演示 MVI 用法）
```

> `core:common` 与 `core:model` 为纯 Kotlin / 数据模块，通过 `SkyMVILib` 的本地
> `project()` 依赖（或 `useLocalSkyMVI=false` 时切换为远程 Maven 坐标 `com.sky.lib:SkyMVI`）接入。

---

## 架构总览（MVI）

单向数据流：**UI → Intent → ViewModel → Reducer → State → 重组(recompose)**。
一次性事件（跳转、Toast、弹窗）走 `SkyUiEffect`，由 `Channel` 保证「只消费一次」。

```
┌──────┐  Intent   ┌────────────────────┐  setState   ┌────────────┐
│  UI  │ ────────▶ │ SkyBaseMviViewModel │ ──────────▶ │ SkyUiState │ ──▶ 重组
└──────┘ ◀──────── │   (Reducer/DISP)   │  sendEffect └────────────┘
        Effect     └────────────────────┘
```

三件套约定（建议放在各 Feature 的 `xxxContract` 中）：

```kotlin
// 状态：用 @Stable 标记，便于 Compose 跳过无变化重组
@Stable
data class LoginState(
    val account: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
) : SkyUiState

// 意图：用 @Immutable 标记
@Immutable
sealed interface LoginIntent : SkyUiIntent {
    data object Submit : LoginIntent
    data class AccountChanged(val value: String) : LoginIntent
    data class PasswordChanged(val value: String) : LoginIntent
}

// 一次性事件：用 @Immutable 标记
@Immutable
sealed interface LoginEffect : SkyUiEffect {
    data class ShowToast(val msg: String) : LoginEffect, SkyToastEffect {
        override val message: String get() = msg
    }
    // 导航类副作用：标记 SkyNavigationEffect 后由框架自动分发
    data object NavigateHome : LoginEffect, SkyNavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.skyNavigateTo(Routes.Home.pattern, popUpToRoute = Routes.Login.pattern, inclusive = true)
    }
}
```

---

## 快速接入

### 1. 初始化（Application）

```kotlin
class App : BaseApplication() {
    override fun onCreate() {
        super.onCreate()
        SkyMVILib.requireInit(
            SkyMVILibConfig.Builder()
                .setBaseUrl("https://www.wanandroid.com/")
                .setNetWorkStateListener { connected -> /* 网络状态变化 */ }
                .enableStrictMode(true) // 严格模式：Intent/State 类型校验
                .build()
        )
    }
}
```

### 2. Activity 基类

`BaseComposeActivity` 已封装 `enableEdgeToEdge` + `setContent`，并通过
`LocalNetworkState` 向子树暴露网络状态 `StateFlow<NetState>`：

```kotlin
class MainActivity : BaseComposeActivity() {
    @Composable
    override fun Content() {
        AppRoot()   // 你的根 Composable
    }
}
```

### 3. ViewModel（MVI 核心）

```kotlin
@HiltViewModel
class LoginViewModel @Inject constructor() :
    SkyBaseMviViewModel<LoginState, LoginIntent, LoginEffect>() {

    override fun initialState() = LoginState()

    override fun handleIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.AccountChanged -> setState { copy(account = intent.value) }
            is LoginIntent.PasswordChanged -> setState { copy(password = intent.value) }
            LoginIntent.Submit -> submit()
        }
    }

    private fun submit() {
        // 网络请求见下方「网络层」章节；此处仅演示状态与副作用
        setState { copy(isLoading = true) }
        viewModelScope.launch {
            delay(800)
            setState { copy(isLoading = false) }
            sendEffect(LoginEffect.NavigateHome) // 触发导航
        }
    }
}
```

`SkyBaseMviViewModel` 内置能力：

| 方法 | 说明 |
| --- | --- |
| `initialState()` | 返回页面初始 `SkyUiState`（必须实现） |
| `handleIntent(intent)` | 处理来自 UI 的 `Intent`（必须实现） |
| `setState { ... }` | 基于 CAS 的安全更新 `SkyUiState`（传入以当前 state 为 receiver 的 lambda） |
| `sendIntent` / `invoke` | 串行消费 `Intent` 的 `Channel(UNLIMITED)`，避免并发竞态 |
| `sendEffect` / `postEffect` | 通过 `Channel(BUFFERED, SUSPEND)` 发送一次性 `SkyUiEffect` |
| `currentState` | 同步读取当前 `SkyUiState` |
| `singleFlight { }` | 合并并发请求，同一 key 仅执行一次 |
| `stateInViewModel { }` | 将 `Flow` 以 `viewModelScope` + `WhileSubscribed` 收敛为 `StateFlow` |
| `launchIO { }` | 在 `viewModelScope` + `Dispatchers.IO` 中启动协程 |

### 4. Composable 侧消费

推荐用 `SkyMviScreen` 一次性绑定「订阅状态 + 消费副作用 + 分发意图」，
并用 `rememberSkyMviEffectHandler` 把「导航 + Toast」两类副作用交给框架自动处理：

```kotlin
@Composable
fun LoginRoute(
    navController: NavHostController,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    // 统一处理「导航 + Toast」副作用，无需手写 onNavigateXxx 回调
    val onEffect = rememberSkyMviEffectHandler(navController)
    SkyMviScreen(
        viewModel = viewModel,
        onEffect = onEffect
    ) { state, onIntent ->
        LoginContent(state = state, onIntent = onIntent)
    }
}
```

辅助扩展：

- `viewModel.collectSkyState()` — `collectAsStateWithLifecycle()` 的封装，自动跟随生命周期收集。
- `SkyMviScreen(viewModel, onEffect, content)` — 一次性完成状态订阅 / 副作用消费 / 意图分发。
- `SkyCollectEffect(flow) { }` — 在生命周期内收集任意 `Flow`（含 `viewModel.effect`）。
- `SkyLaunchedIntent(viewModel, intent)` — 进入组合时发送一次初始化 `Intent`（如首屏加载）。
- `rememberSkyIntentDispatcher(viewModel)` — 返回 `(I) -> Unit`，便于向子 Composable 透传。

> **注意**：`viewModel.effect` 由 `Channel` 驱动，**只能被一个收集器消费**。
> 使用 `SkyMviScreen` 时，副作用统一走其 `onEffect` 参数（如 `rememberSkyMviEffectHandler`），
> 不要再额外调用 `SkyCollectEffect(viewModel.effect)`，否则事件会被分流而漏消费。

---

## Compose 导航封装

把「页面跳转」从屏幕上的 `onNavigateXxx` 回调里解放出来，让 ViewModel 成为跳转逻辑的唯一来源。

### 路由表（集中声明，避免魔法字符串）

```kotlin
object Routes {
    val Login = SkyRouter("login")
    val Home = SkyRouter("home")
    val Detail = SkyRouter("detail/{id}")
}
// 生成真实路径：Routes.Detail.build(123) -> "detail/123"
```

### 导航副作用

只需让 `Effect` 实现 `SkyNavigationEffect`，框架会自动调用其 `handle(controller)`：

```kotlin
sealed interface HomeEffect : SkyUiEffect {
    data class ShowToast(val msg: String) : HomeEffect, SkyToastEffect {
        override val message: String get() = msg
    }
    data class NavigateDetail(val id: Int) : HomeEffect, SkyNavigationEffect {
        override fun handle(controller: NavHostController) =
            controller.skyNavigateTo(Routes.Detail.build(id)) // 自动在 Detail 路由下跳转
    }
}
```

### 统一副作用处理器

`rememberSkyMviEffectHandler` 返回 `(SkyUiEffect) -> Unit`，专用于 `SkyMviScreen` 的 `onEffect`
（不会自行启动新的收集协程，避免与 SkyMviScreen 内部的唯一 `SkyCollectEffect` 竞争）：

```kotlin
val navController = rememberNavController()
val onEffect = rememberSkyMviEffectHandler(navController)
SkyMviScreen(viewModel = vm, onEffect = onEffect) { state, onIntent -> /* ... */ }
```

它内部按类型分派：

- `SkyNavigationEffect` → 调用其 `handle(controller)`（即 `skyNavigateTo`）
- `SkyToastEffect` → `Toast.makeText(...).show()`
- 其它 → 交回 `onUnhandled` 兜底

### 导航扩展与独立收集器

| API | 说明 |
| --- | --- |
| `navController.skyNavigateTo(route, popUpToRoute, inclusive, singleTop)` | 封装 `popUpTo` / `singleTop` 的跳转 |
| `navController.skyNavigateBack()` | 返回上一页 |
| `SkyRouter(pattern).build(vararg args)` | 参数填入占位符生成目标路径 |
| `SkyNavigationEffect` / `SkyToastEffect` | 标记接口，交给框架自动处理 |
| `SkyHandleNavigationEffects(navController, flow)` | 独立收集器（**非 SkyMviScreen 场景**使用） |
| `SkyHandleToastEffects(flow)` | 独立 Toast 收集器（**非 SkyMviScreen 场景**使用） |

> 独立收集器（`SkyHandleNavigationEffects` / `SkyHandleToastEffects`）自带 `SkyCollectEffect`，
> 仅适用于**未**使用 `SkyMviScreen` 的屏幕，请勿与 `SkyMviScreen` 的 `onEffect` 同时使用。

---

## 通用 UI 组件

位于 `com.sky.mvi.widget.*`，均为无状态、可组合的纯展示组件，配合 MVI 状态使用。

### 页面状态容器 `PageStateLayout`

根据 `PageState` 自动在「加载 / 成功 / 空 / 错误」之间切换，成功态渲染 `content`：

```kotlin
PageStateLayout(
    pageState = state.pageState,
    onRetry = { onIntent(HomeIntent.Refresh) }  // 空/错误占位上的重试按钮共用
) {
    ArticleList(items = state.datas)             // 仅成功态渲染
}
```

### 下拉刷新 + 触底加载 `RefreshListWidget`

通用列表，内置下拉刷新（`PullToRefreshBox`）与滚到倒数第 2 项自动加载更多：

```kotlin
RefreshListWidget(
    items = state.datas,
    isRefreshing = state.isRefreshing,
    isLoadingMore = state.isLoadingMore,
    hasMore = state.curPage + 1 < state.pageCount,
    onRefresh = { onIntent(HomeIntent.Refresh) },
    onLoadMore = { onIntent(HomeIntent.LoadMore) },
    key = { it.id },
    itemContent = { article -> ArticleItem(article) }
)
```

> 典型组合：首屏 `Loading` / `Error` 交给外层 `PageStateLayout`，`Success` 时再渲染
> `RefreshListWidget`，避免刷新时整页被 Loading 占位覆盖。

### 组件清单

| 组件 | 位置 | 说明 |
| --- | --- | --- |
| `PageStateLayout` | `widget.state` | 加载/成功/空/错误 四态切换容器 |
| `LoadingWidget` | `widget.state` | 居中转圈加载占位 |
| `EmptyWidget` | `widget.state` | 空数据占位（可选重试按钮） |
| `ErrorWidget` | `widget.state` | 加载失败占位（可选重试按钮） |
| `LoadingMoreWidget` | `widget.state` | 列表底部「加载更多」行 |
| `RefreshListWidget` | `widget.refresh` | 下拉刷新 + 触底加载更多列表 |
| `ConfirmDialog` | `widget.dialog` | 通用确认对话框（`visible` 控制显隐） |

所有组件均接受 `Modifier`，可直接接入主题与布局；图标来自 `material-icons-extended`
（已随 `SkyMVILib` 以 `api` 形式暴露，业务方无需重复引入）。

---

## 网络层（Flow 化）

`MviViewModelExt` 提供四类网络调用，全部基于 `suspend` + `StateFlow`/`Flow`：

| 方法 | 形态 | 适用 |
| --- | --- | --- |
| `apiRequest { }` | 回调式（`onStart`/`success`/`error`） | 简单一次性请求 |
| `apiRequestNoCheck { }` | 同上，跳过业务码校验 | 非标准返回 |
| `apiFlow { }` | 返回 `Flow<ResultState<T>>` | 需要连续映射/合并 |
| `apiFlowNoCheck { }` | 同上，跳过校验 | 同上 |

```kotlin
apiRequest(
    context = context,
    block = { api.getEntryAndExitDataApi(page) },
    success = { resp -> setState { copy(datas = resp.datas) } },
)
```

`ResultState` 为 `sealed class`（`Loading` / `Success` / `Error`），并附带
`doSuccess/doError/doLoading/parseState` 系列扩展；`BaseResponse` 由各业务 `core:model`
中的 `BasePage`/响应 Bean 继承，实现 `isSuccess()` 与 `getResponseCode()` 即可对接任意后端。

---

## 分页 / 多状态

列表页推荐直接使用 Paging 3：通过 `BasePagingSource<Value>` 抽象分页机制、在薄子类中提供
具体 API 与数据映射（见 `core:common` 的 `BasePagingSource`）。首屏的「加载 / 空 / 错 / 成功」
四态以及下拉刷新、触底加载更多，统一由 widget 层的 `SkyPageState` 与 `RefreshListWidget` 承接
（参考 `app` 模块的 `HomeScreen` 示例，它将 `LazyPagingItems` 的 `LoadState` 映射为 `SkyPageState`）。
`SkyNavigationEffect` 仍用于「点击条目 → 跳转详情」等导航副作用。

---

## 构建与版本

- Gradle 9.6.1 / AGP 9.3.1 / Kotlin 2.4.0 / JVM 17
- Compose BOM `2026.06.01`；navigation-compose `2.9.8`；hilt-navigation-compose `1.4.0`
- lifecycle `2.11.0`（要求 `compileSdk >= 37`）；coroutines `1.11.0`；coil3 `3.5.0`
- 约定插件 `com.sky.buildLogic:convention:1.2.1`（`sky.android.library` / `.hilt` / `.publish` 等）

> **关于 Compose 编译器插件**：SkyBuildLogic 的 `enableCompose` 经 `linkToRoot` 会镜像到
> 所有子模块，因此 `core:model` / `core:common` 也会应用 Compose 编译器插件。为避免
> `IncompatibleComposeRuntimeVersionException`（缺少 Compose Runtime），这两个纯模块已在
> `dependencies` 中加入 `androidx.compose.runtime`（仅运行时，不含 UI 组件），版本由 BOM 统一管理。

---

## 本地调试与发布

- `local.properties`：`useLocalSkyMVI=true` 使用本地 `:SkyMVILib` 源码依赖；
  `false` 时切换为远程 Maven 坐标 `com.sky.lib:SkyMVI:1.0.0`。
- `publish` 约定插件已接入（`SkyMVILib/build.gradle.kts`），`local.properties` 配置
  `maven.*` / 签名 `SkyMVI.jks` 后即可执行发布任务。

---

## 与原 SkyMVVM 的主要差异

| 维度 | SkyMVVM | SkyMVI |
| --- | --- | --- |
| 架构 | MVVM | MVI（State/Intent/Effect 三件套） |
| 状态容器 | LiveData / DataBinding | `StateFlow` + `Channel`（单向数据流） |
| UI | XML + ViewBinding | Jetpack Compose |
| 基类 | `BaseActivity/BaseVmActivity` | `BaseComposeActivity` + `SkyBaseMviViewModel` |
| 网络回调 | 回调 + LiveData | `suspend` + `StateFlow` / `Flow` |
| 导航 | 路由框架 / 手动跳转 | `SkyNavigationEffect` 驱动的 Effect 化跳转 |
| UI 组件 | 自定义 View | `PageStateLayout` / `RefreshListWidget` 等 Compose 组件 |
| 包名 | `com.sky.mvvm` | `com.sky.mvi` |
| 资源前缀 | `sky_mvvmlib_` | `sky_mvilib_` |
