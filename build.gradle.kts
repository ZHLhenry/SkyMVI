// skyBuild 配置
extra["skyBuild.appName"] = "SkyMVI"
extra["skyBuild.applicationId"] = "com.sky.mvi.sample"
extra["skyBuild.versionCode"] = 100
extra["skyBuild.versionName"] = "1.0.0"
extra["skyBuild.compileSdk"] = 37
extra["skyBuild.minSdk"] = 24
extra["skyBuild.targetSdk"] = 35
extra["skyBuild.enableViewBinding"] = false
extra["skyBuild.enableDataBinding"] = false
// 开启后由 build-logic 自动 apply org.jetbrains.kotlin.plugin.compose 到所有子模块，
extra["skyBuild.enableBuildConfig"] = true
// 并自动注入 Compose BOM + 核心依赖；core:model / core:common 等内部模块仅注入 runtime。
extra["skyBuild.enableCompose"] = true
// 默认版本由 SkyBuildLogic 内置，此处覆盖为项目当前使用的 BOM 版本。
extra["skyBuild.composeBomVersion"] = "2026.06.01"
