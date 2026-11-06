# 重构交付总结

对应《要求.md》第四部分"交付要求"。分阶段提交记录见 `git log --oneline`（提交信息带 `stage-N` 前缀，可逐阶段 `git revert`）。

---

## 1. 四页功能分类

底栏统一为 **主页 / 界面 / 功能 / 设置**。原单页设置（`MainFragment`）已删除，入口按下表重新归位。

### 主页（Home）

只放"当前状态 + 今天就要用的动作"：

| 区块 | 内容 |
| --- | --- |
| 状态 | 输入法是否启用；当前是否默认输入法；启用输入法（跳系统设置）；默认中文方案（`wanxiang · 自然码`） |
| 快捷操作 | 切换输入法（系统选择器）；输入法列表；虚拟键盘；安装向导 |
| 关于 | 版本号；运行时包名（用于核对 `org.fcitx5.xuexgao`） |

### 界面（Appearance）

| 区块 | 内容 |
| --- | --- |
| 实时预览 | 键盘预览（复用 `KeyboardPreviewUi`），改动立即重绘 |
| 配色 | 主题色（8 个预设色板）；主题（原 `ThemeFragment`）；图标主题 |
| 布局 | 卡片圆角；页面边距；卡片样式（浮起/扁平/描边） |
| 键盘按键 | 键盘风格（经典/Gboard）；按键圆角；横向间距；纵向间距；按键边框；水波纹；特殊键胶囊外形 |
| 底栏 | 功能栏样式（仅图标/仅文字/图标+文字）；浮动底栏；液态玻璃效果 |
| —— | 恢复默认外观 |

### 功能（Features）

| 区块 | 内容 |
| --- | --- |
| 输入 | 输入法列表；附加组件；全局选项；**Rime 方案** |
| 文本工具 | 快捷短语；拼音自定义短语；码表输入法；表情与符号 |
| 效率 | 剪贴板；剪贴板同步；插件 |
| 功能栏 | 自定义功能栏按钮（增删/排序/显隐/恢复默认 + 实时预览） |

### 设置（Settings）

| 区块 | 内容 |
| --- | --- |
| 设置 | 虚拟键盘；候选窗口；剪贴板 |
| 应用 | 剪贴板同步；高级 |
| 关于 | 常见问题；开发者；关于；许可证 |

---

## 2. UI 重构说明

### 设计系统

新增 `app/src/main/java/org/fcitx/fcitx5/android/ui/design/`，参照 KernelSU-Style-UI-Kit 的设计语言（大圆角分组卡片、单一强调色、宽松留白、浮动底栏）复刻为 View 组件：

| 文件 | 作用 |
| --- | --- |
| `KsuTokens.kt` | 颜色/圆角/间距令牌，全部由偏好驱动；浅色与深色两套调色板 |
| `KsuViews.kt` | 卡片、分组标题、列表项、开关、滑杆、单选行、色板、按钮、滚动页面 |
| `GlassSurface.kt` | 液态玻璃表面 + 背景采样与模糊 |
| `KsuBottomBar.kt` | 四标签浮动底栏 |

> **为什么不引入 Compose**：参考项目是 Compose 实现，但本工程主界面建立在 Preference + Views 之上，
> 整体迁移属于重大架构变更。按《要求.md》"优先最小改动"的约束，这里复刻设计语言而不引入第二套渲染栈。

### Android 8–17 兼容与降级

| 能力 | 实现 | 降级 |
| --- | --- | --- |
| 液态玻璃模糊 | `BitmapBlurUtil`（API ≤30 RenderScript，>30 软件模糊）+ 背景采样 | API < 26、内存吃紧或用户关闭效果时，仅保留半透明底色 + 描边，**圆角、配色、布局完全一致** |
| 圆角/阴影 | `GradientDrawable` + `ViewOutlineProvider` | 全版本可用 |
| 涟漪反馈 | `RippleDrawable` | API 21+ |
| 门槛 | `minSdk` 保持 23；重点保障 API 26–37 | API 23–25 走纯色卡片 |

### 导航结构

- `MainActivity` 用 `ViewPager2` 承载四页，底部浮动栏同步选中态，工具栏标题随页签变化。
- 二级设置页仍走原有 `NavHostFragment` 导航图，作为**覆盖层**显示：`MainActivity.openRoute(route)` 显示覆盖层并入栈，返回时先出栈、再回到主页签、最后由系统处理。
- 返回键改为注册 `OnBackPressedCallback`（不再重写 `onBackPressed()`），并保留"子 Fragment 优先"的原有语义（例如保存中拦截返回）。
- 首页签为标题时隐藏工具栏返回箭头，进入二级页自动显示。

---

## 3. Gboard 风格按键改动说明

键盘的**行列布局、按键顺序、输入/候选/删除/组合逻辑一律未改**，只统一操作按键的视觉参数。新增 `KeyboardStylePreset`（`ui/design/`）：

| 维度 | 经典（原默认） | Gboard（新默认） |
| --- | --- | --- |
| 按键圆角 | 4 dp | 9 dp |
| 横向间距 | 3 dp | 4 dp |
| 纵向间距 | 7 dp | 6 dp |
| 边框 | 关 | 关 |
| 点击反馈 | 无涟漪 | 涟漪 |
| 回车 / `?123` | 圆角矩形 | 胶囊（pill） |

- 首次运行写入一次（`keyboard_style_applied` 标志），之后用户的滑杆调整不会被覆盖。
- 界面页提供 `键盘风格：经典 / Gboard` 一键切换，切换即写入并触发 `ThemeManager.notifyAppearanceChanged()`，
  **真实键盘与预览同时刷新，无需重启**。
- 操作键的视觉层级沿用主题既有语义：回车=`Accent`（强调色底）、退格/逗号/句号/`?123`=`Alternative`（次级底）、语言键=`AltForeground`（弱化前景）、空格=`Special`（独立底色），因此 Gboard 的"功能键与字母键区分"在任意主题下都成立。
- 图标统一使用 24 dp Material 线性图标，并保留 `iconSlot` 机制，图标主题仍可整体替换。

---

## 4. 包名修改清单

见 [PACKAGE_RENAME.md](PACKAGE_RENAME.md)（逐项检查表：Gradle / Manifest / 权限 / FileProvider authority /
深链接 / 测试配置 / 插件互通 / JNI 与 namespace 的边界）。

要点：**applicationId = `org.fcitx5.xuexgao`**（mainline 变体为 `.mainline`）；
**namespace 保持 `org.fcitx.fcitx5.android`**，因为它同时是 JNI 符号前缀与上游插件协议前缀。

---

## 5. Rime 插件与词库

见 [RIME_INTEGRATION.md](RIME_INTEGRATION.md)。

要点：
- `rime-wanxiang` 以 submodule 固定在 `plugin/rime/src/main/cpp/rime-wanxiang`，随 rime 插件一起部署。
- `default.yaml` 的 `schema_list` 首位改为 `wanxiang`，`luna_pinyin*` 兜底。
- **默认双拼为"自然码"**，写入 `wanxiang.custom.yaml` 的 `wanxiang_algebra:/base/自然码`。
- 首次进入输入法时自动生成 `default.custom.yaml` / `wanxiang.custom.yaml`；词典编译由 librime 首次 full check 完成，也可在界面手动"重新部署"。

---

## 6. 图形化配置与功能栏自定义

### Rime 方案（功能 → Rime 方案）

默认方案切换、方案启用/禁用、双拼方案（9 种）、候选数量、6 组模糊音、重新部署、导出/导入、恢复默认。
所有改动即时写入 YAML；默认方案不可被关闭（避免 Rime 回落到别的方案）。

### 功能栏（功能 → 自定义功能栏按钮）

- 原有能力（增删、拖拽排序、显隐、自定义图标/文字/标签、宏、导入导出）全部保留。
- **新增实时预览条**：编辑区上方按当前顺序渲染图标胶囊，增删/排序/显隐立刻反映（通过 `AdapterDataObserver` 驱动）。
- **新增"恢复默认功能栏"**：一键回到 `ButtonsLayoutConfig.default()`。
- 保存后持久化到按钮布局 JSON，重启生效（编辑期即预览）。

---

## 7. GitHub Actions 测试结果

每次 push 触发三条流水线：

| Workflow | 内容 | 结果 |
| --- | --- | --- |
| `compile-check.yml` | 仅 Kotlin/KSP：fx debug、mainline debug、单元测试编译 | ✅ 通过 |
| `ci.yml`（Commit CI） | 完整 NDK 构建：standard / plugins / mainline 三个矩阵，签名 APK，校验 apksigner | ✅ 通过 |
| `emulator-ui-test.yml` | 构建 APK → API 30 与 API 34 模拟器上跑仪器化测试 | ✅ 通过（**22 tests / OK**，`INSTRUMENTATION_CODE: -1`） |

最近一次全绿提交：`cf910c4b`（三条流水线均 success）。

模拟器实测还发现并修复了一个**真实崩溃**：`KeyboardStylePreset.applyDefaultOnce()` 在
`ThemeManager.init()` 之前触发主题变更，`fireChange()` 访问未初始化的 `lateinit` 主题属性，
导致全新安装启动即 `UninitializedPropertyAccessException`。修复方式：预设改到
`ThemeManager.init()` 之后应用，并让 `fireChange()` 在初始化前安全返回。

仪器化测试覆盖（`app/src/androidTest/.../ui/`）：

| 测试类 | 覆盖验收项 |
| --- | --- |
| `PackageIdentityTest` | applicationId = `org.fcitx5.xuexgao`；namespace 未变；IME 服务已声明；FileProvider authority 跟随 applicationId |
| `MainShellNavigationTest` | 底栏四个页签存在；切换页签驱动 ViewPager |
| `AppearanceSettingsTest` | 主题色/圆角/间距持久化；恢复默认；Gboard 预设写入按键参数 |
| `RimeDefaultsTest` | 默认方案 `wanxiang`；默认双拼"自然码"；生成的 YAML 内容正确；重置生效 |
| `FunctionBarConfigTest` | 默认布局存在；顺序/显隐可持久化；恢复默认幂等 |

> 运行结论（含失败原因，若有）见聊天回复中的"CI 结果"一节。

---

## 8. 已知问题与后续建议

1. **未引入 Compose**：设计语言是复刻而非移植。若后续希望完全对齐 KernelSU-Style-UI-Kit，需要为设置壳单独引入 Compose 并改造四页宿主。
2. **词库体积**：万象拼音完整词典约 54 MB，首次部署编译耗时较长（中低端机型可能数十秒）。可考虑接入 `rime-wanxiang-data` 的按需下载，或提供 `.lite` 精简词库。
3. **namespace 未随 applicationId 改名**：这是刻意取舍（见 PACKAGE_RENAME.md）。若坚持改 namespace，需要同步迁移 428 个源文件、JNI 函数名与插件协议，风险高。
4. **F-Droid 元数据未改**：`app/org.fcitx.fcitx5.android.yml` 描述的是上游 F-Droid 构建（依赖 Jenkins 产物），本 fork 不发布 F-Droid，故保持原样以免误导。
5. **CZ 键盘高度等未纳入界面页**：为控制改动面，界面页只覆盖要求列举的项（主题色/圆角/间距/布局/功能栏样式）；键盘高度、候选窗口等仍在设置页。
6. **本地无法编译**：开发容器为 aarch64 而 Android SDK 工具链是 x86_64，因此全部验证在 GitHub Actions 上完成（这也是本轮 CI 配置较重的原因）。

---

## 8.5 重做说明（第二轮，按反馈）

第一轮把 KernelSU 风格"用 View 复刻"了一遍，方向错了。第二轮按反馈重做：

| 反馈 | 处理 |
| --- | --- |
| 要模板同款风格，不是模仿 | 引入模板使用的同一个库 **miuix 0.9.4**（Apache-2.0，Maven Central），四页与设置组件都是 miuix 原生组件 |
| 二级菜单还是原样 | 二级页转为 Compose：Rime 方案页已完成；其余按批次迁移，迁移期间旧的 Fragment 页暂时由覆盖层承载，功能不断档 |
| 键盘实时预览用不了 | 界面页顶部用 `AndroidView` 承载真实的 `KeyboardPreviewUi`，由外观偏好变更驱动重建（`key(revision)`） |
| Rime 要直接集成，不要装插件 | `app/src/main/cpp/rime/CMakeLists.txt` 把 fcitx5-rime 引擎与万象词库编译进主 APK，addon 作为应用自带 native 库被 fcitx5 加载 |
| Gboard 只要默认打开原版功能 | 撤掉自创的 Geometry 预设，仅把现成开关的默认值改为：水波纹=开、回车/?123 胶囊=开、边框=关（本来就是）、空格显示当前语言（本来就是） |

构建层面的连带调整：
- `compileSdk` 36 → **37**（miuix 的 AAR 元数据要求）
- `minSdk` 23 → **24**（miuix-preference 要求 24）
- `miuix-blur` 声明 minSdk 33，用 `tools:overrideLibrary` 放行，运行时以 `isRenderEffectSupported()`
  判定：API 31+ 才有模糊/折射，低版本自动降级为半透明表面，**整体风格与布局不变**
- CI 安装 `platforms;android-37.0`（android-37 以次版本号发布）

## 9. 验收标准对照

| 验收标准 | 状态 | 证据 |
| --- | --- | --- |
| 应用可正常构建、安装、启动 | ✅ | Commit CI 三矩阵全绿；模拟器上安装并启动 MainActivity |
| 底栏四页可正常切换 | ✅ | `MainShellNavigationTest`（模拟器实测） |
| 界面页修改可实时预览并持久化 | ✅ | `AppearanceSettingsTest` + `ThemeManager.notifyAppearanceChanged()` 驱动真实键盘与预览 |
| 键盘按键为 Gboard 风格，核心输入正常 | ✅ | `KeyboardStylePreset` 默认写入；键盘布局/输入逻辑未改（`TextKeyboard` 仅未受影响） |
| applicationId 为 `org.fcitx5.xuexgao` | ✅ | `PackageIdentityTest`（release 身份精确匹配，debug 追加 `.debug` 为原项目约定） |
| 默认中文 26 键与双拼可用，rime-wanxiang 词库生效 | ✅ / ⚠️ | 26 键为默认布局；Rime 默认方案 `wanxiang`、默认双拼自然码、词库随插件打包。前提是安装了 Rime 插件 APK（见 RIME_INTEGRATION.md 第七节） |
| 其它方案可图形化配置，功能栏可自定义并保存 | ✅ | `RimeSchemaFragment`；`FunctionBarConfigTest` + 自定义器实时预览/恢复默认 |
| GitHub Actions 模拟器 UI 测试通过 | ✅ | API 30 与 34：`OK (22 tests)`，`INSTRUMENTATION_CODE: -1` |

---

## 10. 第三阶段进行中的交接说明（Compose + miuix）

已完成并验证（提交 `25aaddd2` 起）：

- 四页 Compose 外壳（miuix `Scaffold` + `FloatingNavigationBar` + `PageScaffold` 大标题+模糊栏）
- 界面页键盘实时预览（`AndroidView` + `key(revision)` 重建真实键盘）
- 二级页批次 A：候选窗口 / 表情与符号 / 剪贴板 / 高级 / 开发者 / 关于，以及 Rime 方案页
- 通用渲染器 `ManagedPrefsScreen`：任何 `ManagedPreferenceProvider` 都能直接渲染成 miuix 行
- 模拟器截图流水线（`ScreenshotTest` + `run-as tar` 拉图 + artifact），仓库内留存 5 张样张于
  `docs/screenshots/`

已完成 Compose 化并**删除了旧 Fragment 与导航图条目**的二级页（12 个）：

候选窗口、表情与符号、剪贴板、高级、开发者、关于、插件列表、附加组件列表、附加组件配置、
输入法列表、输入法配置、全局选项。

其余已 Compose 化但旧 Fragment 仍在（因为共享组件或还没迁移完）：键盘主题、Rime 方案。

已完成 Compose 化的二级页汇总：

候选窗口、表情与符号、剪贴板、高级、开发者、关于、Rime 方案、全局选项、输入法配置、
附加组件配置、键盘主题、插件列表、附加组件列表。

### 第四阶段（批次 C-3 与收尾）已完成

提交见 `git log --oneline` 的 `stage-19` … `stage-26`，逐条对应第 11 节清单：

- **二级页补齐**：快捷短语列表/编辑、标点编辑器、码表输入法、拼音词典、拼音自定义短语、
  通用多选、附加组件目录档案、许可证、键盘设置（虚拟键盘 + 五个分组）、图标主题列表。
- **通用列表改为内联对话框**：`ConfigList` / `ConfigEnumList` 写回的是父页面持有的 `RawConfig` 树，
  而入栈的子页面会让父页面离开组合、这棵树随之失效，因此它在配置页内以对话框存在。
  `SettingsRoute.ListConfig` 及其 Parcelable 参数一并删除。
- **真正的 Compose 返回栈**：`FcitxApp` 内部维护路由栈，二级页可以再入栈子页面并逐级返回；
  入栈时按栈深 `key(...)` 重建组合，避免两个同类型页面共享 `remember` 状态。
- **路由入口统一**：深链接、键盘功能栏按钮、其它 Activity 通过 `ComposeRouteHost` 把路由交给 Compose 宿主。
  这同时修掉了一个真实缺陷：stage-18 删掉 Fragment 后，`launchMainToRoute(SettingsRoute.Clipboard)` 之类
  的入口仍然走 `navController`，指向已不存在的目的地。
- **旧实现全部删除**：所有设置 Fragment、`SettingsRoute.createGraph` / `Index`、
  `MainActivity` 的导航宿主覆盖层，以及只被它们使用的 View 组件
  （`PreferenceScreenFactory`、`FcitxPreferenceFragment`、`ManagedPreferenceFragment`、
  `BaseDynamicListUi` 动态列表栈、`ui/main/modified/`、主题列表适配器、`createUi` 工厂等）。
  `SettingsRoute` 现在只是数据载体，`isComposeRoute` 已删除（所有路由都是 Compose）。
- **底栏液态玻璃样式**：选中项显示文字标签。miuix 的 `FloatingNavigationBar` 做不到（
  `FloatingNavigationBarItem` 只画图标，label 仅作为 content description；它的字节码里也没有任何模糊逻辑，
  所以替换它不会丢掉磨砂效果）。悬浮态改为「圆角 + 半透明表面 + 细描边」的容器里放常规 `NavigationBar`，
  并用 `LocalNavigationBarDisplayMode = IconWithSelectedLabel` 只给选中项显示标签；停靠态不变。
- **截图流水线**：`ScreenshotTest` 的步行路线扩到 14 步（四个主页 + 11 个二级页）。
  被底栏遮住的行点击会失败（触摸注入被拒绝，语义回退也未必找得到 OnClick），
  这一步失败会中断后续采集 —— 由于该任务本就是 best-effort（不拦门禁），
  CI 上通常能拿到前 6 张左右；把每个步骤各自 `runCatching` + 失败后回退到上一页是后续可做的改进。

---

## 11. 剩余工作

第 11 节清单（批次 C-3 + 收尾四项）已全部落地，提交见 `git log --oneline` 的 `stage-19` … `stage-26`。

仍可继续打磨的地方（都不影响功能）：

1. **真正的背景模糊**：底栏目前是半透明表面 + 描边，没有采样页面内容做模糊。
   要做的话需要在 `FcitxApp` 层持有一个 `LayerBackdrop` 并传给各页面的 `PageScaffold`，
   让页面内容画进同一张 backdrop，再在底栏上 `textureBlur(backdrop, ...)`。
   低版本（API < 31）仍走现有的「纯色 + 描边」降级即可。
2. **截图步行路线容错**：每个 `clickRow` + `capture` 各自独立 `runCatching`，失败后按返回键回到上一页继续，
   这样一张截图取不到不会丢掉后面的页面（当前行为见上一节）。
3. **历史遗留的无引用类**：如 `UpdateMirrorListActivity`（不属于设置壳重构范围），未在本轮处理。

关于「删除不再使用的布局」：`res/layout/activity_*.xml` 目前都还被引用着 ——
`ClipboardEditActivity` / `LogActivity` / `SetupActivity` 通过 **ViewBinding** 使用它们
（`binding.root`），普通文本搜索看不到这一层引用；`res/xml/preferences_local.xml` 仍被剪贴板同步设置页使用，
其中的 `DialogSeekBarPreference` 因此保留。故本轮没有删除任何布局文件。
