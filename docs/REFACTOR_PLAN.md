# fcitx5-android 四页重构实施计划

本文件对应《要求.md》阶段 1 的交付物：功能盘点、四页分类方案、页面复用/新建/合并清单，以及各阶段的实施与回滚方式。

## 1. 现有功能入口盘点

当前主界面只有一页（`MainFragment`，基于 `PreferenceScreen`），用两个 `PreferenceCategory` 分组：

### Fcitx 组（输入法引擎侧配置）

| 入口 | 路由 | 实现 |
| --- | --- | --- |
| 全局选项 / Global options | `SettingsRoute.GlobalConfig` | `GlobalConfigFragment`（Fcitx RawConfig 动态渲染） |
| 输入法 / Input methods | `SettingsRoute.InputMethodList` | `InputMethodListFragment` |
| 附加组件 / Addons | `SettingsRoute.AddonList` | `AddonListFragment` |

### Android 组（客户端侧配置）

| 入口 | 路由 | 实现 |
| --- | --- | --- |
| 主题 / Theme | `SettingsRoute.Theme` | `ThemeFragment`（含实时预览 `KeyboardPreviewUi` + TabLayout + ViewPager2） |
| 图标主题 / Icon theme | 独立 Activity | `IconThemeListActivity` |
| 虚拟键盘 / Virtual keyboard | `SettingsRoute.VirtualKeyboard` | `KeyboardSettingsFragment` → `KeyboardGroupFragment`（layout / behavior / feedback / toolbar 四组） |
| 候选窗口 / Candidates window | `SettingsRoute.CandidatesWindow` | `CandidatesSettingsFragment` |
| 剪贴板 / Clipboard | `SettingsRoute.Clipboard` | `ClipboardSettingsFragment`（另有 `ClipboardSyncSettingsActivity`） |
| 表情与符号 / Emoji and symbols | `SettingsRoute.Symbol` | `SymbolSettingsFragment` |
| 插件 / Plugins | `SettingsRoute.Plugin` | `PluginFragment` |
| 高级 / Advanced | `SettingsRoute.Advanced` | `AdvancedSettingsFragment` |

### 工具栏菜单内的二级入口

| 入口 | 路由 / 实现 |
| --- | --- |
| 常见问题 FAQ | 外链 `Const.faqUrl` |
| 开发者 / Developer | `SettingsRoute.Developer` → `DeveloperFragment`（含 `LogActivity`） |
| 关于 / About | `SettingsRoute.About` → `AboutFragment`（含 `LicensesFragment`） |

### 功能栏（Kawaii Bar）与状态区自定义（已有能力）

- `ButtonsCustomizerActivity` 已支持 Kawaii Bar / Status Area 按钮的增删、排序、显隐、自定义图标/文字/标签、宏动作、导入导出与恢复默认。
- 配置模型：`ButtonsConfig`（`ButtonsLayoutConfig` / `ConfigurableButton`），存储于用户配置文件。
- 动作清单：`ButtonAction.allActions`、`kawaiiBarActions`、`statusAreaActions`、`allConfigurableActions`。

> 结论：功能栏自定义在现有架构中**已存在**，本任务的阶段 6 主要是把它收拢到新 UI 的功能页入口，并补齐实时预览与恢复默认的显式入口，而不是从零实现。

### 输入法键盘界面

- `KeyboardWindow` + `TextKeyboard` / `NumberKeyboard` / `BaseKeyboard`，按键定义集中在 `KeyDefPreset.kt`。
- 按键外观已参数化：`ThemePrefs` 中的 `keyRadius`、`keyHorizontalMargin`、`keyVerticalMargin`、`keyBorder`、`keyBorderStroke`、`keyRippleEffect`、`specialKeyOvalShape`。
- 操作按键使用 `R.drawable.ic_baseline_*` 系列图标，并通过 `iconSlot` 支持 `IconThemeManager` 覆盖。

## 2. 四页分类方案

底栏统一为四页：**主页 / 界面 / 功能 / 设置**。

### 主页（Home）

面向"当前状态与快捷入口"，不放深层配置项：

- 输入法启用状态、当前默认输入法、默认中文方案（Rime 方案名）
- 快捷入口：启用输入法、选择输入法、部署 Rime、功能栏自定义
- 最近使用 / 常用设置直达卡片

### 界面（Appearance）

所有"看得见的外观与布局"设置，全部支持实时预览：

- 主题（配色）与图标主题
- 键盘圆角 `keyRadius`
- 键盘横向间距 `keyHorizontalMargin` / `keyHorizontalMarginLandscape`
- 键盘纵向间距 `keyVerticalMargin` / `keyVerticalMarginLandscape`
- 按键边框 `keyBorder` / `keyBorderStroke`、水波纹 `keyRippleEffect`、特殊键胶囊/椭圆 `specialKeyOvalShape`
- 键盘高度百分比、候选窗口样式入口
- 功能栏样式（按钮集合、顺序、显隐）

### 功能（Features）

面向"输入能力与引擎侧配置"：

- 输入法列表 / 附加组件（Fcitx 引擎）
- Rime 方案管理（方案列表、启用/禁用、默认方案、简单参数、导入/导出、恢复默认）
- 表情与符号、剪贴板、快捷短语
- 插件管理
- 功能栏自定义（按钮增删、排序、显隐、恢复默认）

### 设置（Settings）

面向"客户端行为、诊断与关于"：

- 键盘行为（震动、按键行为、语言切换、空格长按等）
- 高级选项
- 剪贴板同步、通知权限
- 开发者选项、日志
- 关于、许可证、开源致谢（含 rime-wanxiang 与 KernelSU Style UI Kit 来源）

## 3. 页面复用 / 新建 / 合并清单

### 复用（不改实现，只换入口）

`GlobalConfigFragment`、`InputMethodListFragment`、`AddonListFragment`、`ThemeFragment`、`ThemeSettingsFragment`、`CandidatesSettingsFragment`、`ClipboardSettingsFragment`、`SymbolSettingsFragment`、`PluginFragment`、`AdvancedSettingsFragment`、`DeveloperFragment`、`AboutFragment`、`LicensesFragment`、`KeyboardSettingsFragment`、`KeyboardGroupFragment`、`ButtonsCustomizerActivity`、`IconThemeListActivity`。

### 新建

| 新增 | 说明 |
| --- | --- |
| `MainPagerActivity`（在原 `MainActivity` 上扩展） | 承载四页 + 底栏 |
| `ui/home/HomePageFragment` | 主页 |
| `ui/appearance/AppearancePageFragment` | 界面页（含实时预览） |
| `ui/features/FeaturesPageFragment` | 功能页 |
| `ui/settings/SettingsPageFragment` | 设置页 |
| `ui/design/*` | KernelSU 风格设计系统组件（卡片、列表项、开关、按钮、底栏） |
| `ui/rime/RimeSchemaFragment` 等 | Rime 方案图形化管理 |
| `KeyboardStylePreset` | Gboard 风格按键预设与一键切换 |

### 合并 / 下线

| 原入口 | 处理 |
| --- | --- |
| `MainFragment`（单页 PreferenceScreen） | 由四页导航取代，保留类作为兼容入口，内部转发到新主页 |
| `SettingsRoute.Index` | 保留为导航图根，但不再渲染旧的两组列表 |
| "主题" 与 "图标主题" | 都归入 **界面** 页 |
| "虚拟键盘" 下的 layout/toolbar 组 | 布局相关归 **界面**，行为/反馈归 **设置**，按钮自定义归 **功能** |
| 开发者/关于/许可证 | 归入 **设置** 页底部 |

## 4. 兼容性与降级策略（Android 8–17）

| API | 能力 | 降级策略 |
| --- | --- | --- |
| API 31+ (Android 12) | `RenderEffect` 模糊 | 低版本改用半透明遮罩 + 静态渐变，视觉风格保持一致 |
| API 33+ (Android 13) | `RuntimeShader` 折射 | 低版本跳过折射，保留圆角与描边 |
| API 26+ (Android 8) | 基础阴影 / 圆角裁剪 | 使用 `ViewOutlineProvider` + `GradientDrawable` |
| 全版本 | 液态玻璃卡片 | 统一走 `LiquidGlassRenderer`，内部按 API 分派 |

`minSdk` 保持 23（项目当前值），实际重点保障 API 26–37；API 23–25 走最低档纯色卡片。

## 5. 分阶段实施与回滚

| 阶段 | 内容 | 回滚方式 |
| --- | --- | --- |
| 1 | 文档与分类（本文件） | 删除本文件 |
| 2 | 设计系统 + 四页底栏 | 还原 `MainActivity` / `activity_main.xml`，删除 `ui/design`、四页 Fragment |
| 3 | 页面迁移 + 界面页实时预览 | 还原四个 Page Fragment 的 preference 组装代码 |
| 4 | Gboard 风格按键 | 还原 `KeyDefPreset.kt` 与按键外观常量 |
| 5 | 包名 `org.fcitx5.xuexgao` | 还原 `app/build.gradle.kts` 与 manifest 常量 |
| 6 | Rime + rime-wanxiang 默认方案 | 还原默认方案注册与部署逻辑 |
| 7 | Rime 方案图形化配置 | 删除 `ui/rime/*` |
| 8 | 功能栏自定义收拢 | 还原功能页入口 |
| 9 | CI 模拟器 UI 测试 | 删除新增 workflow |

每个阶段独立提交，提交信息带 `stage-N` 前缀，便于 `git revert` 单个阶段。

## 6. 不确定项与假设

1. **KernelSU Style UI Kit 采用 Compose**：本项目主界面基于 View 体系（Preference + Splitties），整体迁移到 Compose 属于重大架构变更。按《要求.md》"遇到不确定项时优先最小改动"的约束，本实现**沿用 View 体系**复刻其设计语言（圆角卡片、分组列表、浮动底栏、液态玻璃降级），不引入 Compose 依赖。
2. **键鼠布局**：`TextKeyboard.getDefaultLayout` 的行列结构保持不变，只替换操作键的视觉与 `keyRadius` 等参数。
3. **词库体积**：rime-wanxiang 完整词库较大，默认只内置基础方案与轻型词库，其余通过图形化配置按需下载。
