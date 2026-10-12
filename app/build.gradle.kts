plugins {
    alias(libs.plugins.sky.android.application)
    alias(libs.plugins.sky.android.hilt)
    alias(libs.plugins.sky.android.application.flavors)
}

android {
    namespace = "com.sky.mvi.sample"
    flavorDimensions += "contentType"
    productFlavors {
        create("dev") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skymvi_debug"
        }
        create("uat") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skymvi"
        }
        create("prod") {
            dimension = "contentType"
            manifestPlaceholders["app_icon"] = "@mipmap/ic_skymvi"
        }
    }
}

dependencies {
    // ---- 本地模块 ----
    implementation(project(":core:common"))
    implementation(project(":core:model"))
}
