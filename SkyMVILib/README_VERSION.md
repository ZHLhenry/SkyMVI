## [v1.0.3] - 2026-10-12
- 基于 build-logic 1.2.8 重新发布（库源码与对外 API 无改动）
- Compose BOM 升级：2026.06.01 → 2026.09.00
- 依赖版本升级：androidx core-ktx 1.19.0 → 1.19.1、appcompat 1.7.1 → 1.8.0、navigation-compose 2.9.8 → 2.10.2、paging-compose 3.5.1 → 3.5.2

## [v1.0.2] - 2026-10-11
- 修复发布 POM 中 androidx.compose.* 依赖版本为字面量 "null" 的问题（基于 build-logic 1.2.7 重新发布）：消费方 Sync 不再逐仓库探测 null 版本产生大量 Failed 记录
- `View.toBitmap` 改用 `androidx.core.graphics.withSave` 替代手写 save/restore，避免异常路径下 Canvas 状态泄漏

## [v1.0.1] - 2026-08-14
- 修复未启用 XLog 时启动崩溃 `NoClassDefFoundError`：将 `SkyMVILibConfig` 中 XLog 相关字段类型改为 `Any`/`Any?` 擦除，仅在真正启用 XLog 时才强转加载 XLog 类，避免未依赖 XLog 的宿主模块初始化报错
- 删除未实现的 `enableStrictMode` 配置项及 `strictModeEnabled` 字段（仅有定义、无任何实际校验逻辑）

## [v1.0.0] - 2026-08-12
- SkyMVILib包重磅首发