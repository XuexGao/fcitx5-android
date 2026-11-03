# 包名修改清单（applicationId → `org.fcitx5.xuexgao`）

## 结论

- **安装身份（applicationId）**：`org.fcitx5.xuexgao`
  - `fx` 变体（默认）：`org.fcitx5.xuexgao`
  - `mainline` 变体（`-PincludeMainlineFlavor=true`）：`org.fcitx5.xuexgao.mainline`
- **代码命名空间（namespace）**：保持 `org.fcitx.fcitx5.android`

## 为什么 namespace 不改

`namespace` 同时承担三个角色，改动它会带来不成比例的风险：

1. 它是 Kotlin/Java 源码包名，428 个源文件、所有 `import ...R` 都要跟着改；
2. 它是 JNI 符号前缀，`app/src/main/cpp/native-lib.cpp` 里 30+ 个
   `Java_org_fcitx_fcitx5_android_core_Fcitx_*` 入口函数名与之绑定；
3. 它是与上游 fcitx5-android 插件互通的协议前缀（`<package>` 查询、
   `${packageBase}.plugin.MANIFEST` 兼容 action、`BUILTIN_ALLOWED_PLUGIN_PREFIX`）。

Android 官方支持 `applicationId != namespace`，用户看到的"包名"就是 applicationId。
因此本次只改安装身份，`namespace` 保持不变并在此记录为已知假设。

## 逐项检查结果

| 位置 | 处理 | 说明 |
| --- | --- | --- |
| `app/build.gradle.kts` `appIdBase` | ✅ 改 | `org.fcitx.fcitx5.android` → `org.fcitx5.xuexgao` |
| `app/build.gradle.kts` `appIdFxSuffix` | ✅ 改 | `.fx` → 空（fx 成为规范身份） |
| `app/build.gradle.kts` `appIdMainlineSuffix` | ✅ 新增 | `.mainline`，两个变体可共存 |
| `app/build.gradle.kts` `flavorFx` | ✅ 改 | 后缀为空时不再设置 `applicationIdSuffix` |
| `app/build.gradle.kts` fx APK 文件重命名 | ✅ 改 | 改为基于 `appIdBase` 拼接，避免写死旧包名 |
| `gradle.properties` | ✅ 新增 | `mainApplicationId=org.fcitx5.xuexgao`，供插件模块继承 |
| `AndroidManifest.xml` 权限 | ✅ 无需改 | 全部使用 `${applicationId}` 占位符 |
| `AndroidManifest.xml` FileProvider authorities | ✅ 无需改 | `.provider` / `.fileprovider` / `.share.fileprovider` / `.cropper.fileprovider` / `.androidx-startup` 全部走 `${applicationId}` |
| `AndroidManifest.xml` Query（插件/输入法/语音） | ✅ 无需改 | `${applicationId}.plugin.MANIFEST` 等占位符 |
| `AndroidManifest.xml` IPC action / 自定义权限 | ✅ 无需改 | `${applicationId}.IPC`、`${applicationId}.permission.*` |
| 深链接 / `ACTION_VIEW` 导入处理 | ✅ 无需改 | intent-filter 未绑定包名，路由经 `EXTRA_SETTINGS_ROUTE` |
| `ime_settings_activity` resValue | ✅ 保持 | 指向类名 `org.fcitx.fcitx5.android.ui.main.MainActivity`（namespace 未变） |
| `lib/plugin-base`（main） | ✅ 无需改 | `<package>` 与 AboutActivity action 均用 `${mainApplicationId}` |
| `lib/plugin-base`（debug） | ✅ 改 | 补上 legacy `org.fcitx.fcitx5.android` 查询 |
| `plugin/text-editor` manifest | ✅ 改 | `<queries>` 覆盖 `org.fcitx5.xuexgao.mainline` + legacy 变体 |
| `plugin/text-editor` `FileBrowserActivity` | ✅ 改 | 候选主应用包名列表补充 `.mainline` |
| `build-logic` `AndroidPluginAppConventionPlugin` | ✅ 无需改 | `mainApplicationId` 属性优先级最高，仍保留旧值作兜底 |
| `VoiceInputProviderManager` 提供方包名 | ✅ 改 | 补充 legacy `.fx` / `.fx.debug` 以兼容旧提供方 |
| `DataManager` / `FcitxRemoteService` 内置插件前缀 | ✅ 保持 | 插件包名仍是 `org.fcitx.fcitx5.android.plugin.*` |
| `UserDataManager.canonicalExportPackageName` | ✅ 保持 | 用于与上游导出数据互通 |
| `FcitxApplication` 重启广播 action | ✅ 无需改 | 由 `BuildConfig.APPLICATION_ID` 派生 |
| `JsonFileQrShareManager` / `IconThemeListActivity` / `ThemeShareImportManager` | ✅ 无需改 | share.fileprovider authority 由 `BuildConfig.APPLICATION_ID` 派生 |
| `app/src/androidTest` | ✅ 无需改 | 测试通过 `targetContext` 取包名，未写死 |
| `app/org.fcitx.fcitx5.android.yml`（F-Droid 元数据） | ⚠️ 未改 | 该文件描述的是上游 F-Droid 构建（依赖 Jenkins 产物），本 fork 不发布 F-Droid，保持原样以免误导 |

## 验证方式

1. `./gradlew :app:assembleDebug` 构建通过（GitHub Actions `Commit CI`）。
2. 新增仪器化测试断言 `targetContext.packageName == "org.fcitx5.xuexgao"`（见 `app/src/androidTest`）。
3. 主页"关于"卡片直接展示运行时 `packageName`，便于人工核对。
