import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.sky.android.library.common)
    alias(libs.plugins.sky.android.hilt)
}

val localProps = Properties().apply {
    val file = File(rootDir, "local.properties")
    if (file.exists()) load(FileInputStream(file))
}
val useLocalSkyMVI = localProps.getProperty("useLocalSkyMVI")?.toBooleanStrictOrNull() ?: false
val useLocalSkyWidgetCompose = localProps.getProperty("useLocalSkyWidgetCompose")?.toBooleanStrictOrNull() ?: false

android {
    namespace = "com.sky.mvi.core.common"
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.activity.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.hilt.navigation.compose)
    api(libs.androidx.navigation.compose)
    api(libs.androidx.core.splashscreen)

    // ---- 图片加载 ----
    api(libs.coil.compose)
    api(libs.coil.network.okhttp)

    // hilt-noop-processor(编译消除警告)
    annotationProcessor(libs.hilt.noop.processor)

    // ---- Json（Moshi）----
    api(libs.moshi)
    api(libs.moshi.converter)

    // ---- 调试面板 Chucker（debug 生效，release 走 no-op）----
    debugApi(libs.okhttp.chucker)
    releaseApi(libs.okhttp.chucker.release)

    // ---- 日志（XLog 由 SkyMVILib 以 compileOnly 暴露，此处补齐 api）----
    api(libs.xlog)

    // ---- Paging 3（配合 SkyRefreshPagingLayout 使用）----
    api(libs.androidx.paging.compose)

    if (useLocalSkyMVI) {
        api(project(":SkyMVILib"))
    } else {
        api(libs.skymvi)
    }
    if (useLocalSkyWidgetCompose) {
        api(project(":SkyWidgetComposeLib"))
    } else {
        api(libs.skyWidgetCompose)
    }
    implementation(project(":core:model"))
}
