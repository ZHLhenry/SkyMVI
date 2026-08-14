## [v1.0.1] - 2026-08-14
- 修复未启用 XLog 时启动崩溃 `NoClassDefFoundError`：将 `SkyMVILibConfig` 中 XLog 相关字段类型改为 `Any`/`Any?` 擦除，仅在真正启用 XLog 时才强转加载 XLog 类，避免未依赖 XLog 的宿主模块初始化报错
- 删除未实现的 `enableStrictMode` 配置项及 `strictModeEnabled` 字段（仅有定义、无任何实际校验逻辑）

## [v1.0.0] - 2026-08-12
- SkyMVILib包重磅首发