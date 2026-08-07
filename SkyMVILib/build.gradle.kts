plugins {
    alias(libs.plugins.sky.android.library)
    alias(libs.plugins.sky.android.hilt)
    alias(libs.plugins.sky.android.publish)
}

android {
    namespace = "com.sky.mvi"
}

dependencies {
    // Compose BOM + 核心套件已由 sky.android.library 以 api 自动注入，使用方无需重复声明。
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    api(libs.androidx.activity.compose)

    // ---- Lifecycle ----
    api(libs.androidx.lifecycle.process)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.lifecycle.common.java8)
    api(libs.androidx.lifecycle.viewmodel.ktx)
    // Compose 侧生命周期感知：collectAsStateWithLifecycle
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)

    // ---- 协程（MVI 的 StateFlow / Channel 基础）----
    api(libs.kotlinx.coroutines.android)

    // ---- 网络 ----
    api(libs.retrofit2.retrofit)
    api(libs.persistentcookiejar)

    // ---- Hilt：hiltViewModel() 需要 hilt-navigation-compose ----
    // hilt-android 与 hilt 编译器已由 sky.android.hilt 自动注入（含版本管理）
    api(libs.androidx.hilt.navigation.compose)

    // ---- 导航（MVI Effect 驱动的页面跳转封装）----
    api(libs.androidx.navigation.compose)

    // ---- Material 图标（状态页 / 空数据 / 错误页等控件需要图标）----
    api(libs.androidx.compose.material.icons.extended)

    // ---- 日志（可选依赖，由宿主决定是否引入 XLog）----
    compileOnly(libs.xlog)
}
