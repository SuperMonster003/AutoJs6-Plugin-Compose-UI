# AutoJs6 Compose UI 插件 Roadmap

本文是 `AutoJs6-Plugin-Compose-UI` (显示名 `Compose UI`; 以 Jetpack Compose + Material 3 为 AutoJs6 脚本提供一套声明式界面 API, 脚本侧全局对象 `compose`) 的可执行状态表.
以 2026-10-02 的宿主本地代码快照 (`AutoJs6 master@77b5a3b0c5`, `VERSION_NAME=6.8.0`, `VERSION_BUILD=5307`, 6.8.0 尚未正式发布), 平台版本插件 `1.8.3`, Compose BOM `2026.09.00` (Compose 1.12 系列, 2026-09-09 发布, 要求 compileSdk 37 与 AGP 9.1.1+) 为起点, 每个条目均可独立 Check 并落地, 后续会话按阶段逐步推进.
历史议题 _[`issue #84`](http://issues.autojs6.com/84)_ (2023-06, aiselp) 是纯提案, 曾以 "无法支持" 关闭; 本路线图以 "脚本描述界面, 预编译 Kotlin 渲染" 的形态重新立项, 不尝试让 Rhino 直接调用 `@Composable` 函数.

使用方式:

1. 每次会话开始时, 从 "阶段总览" 选取一个或多个未完成条目, 优先级按阶段顺序; 单次会话可完成多个小节, 除非单个小节已足够繁杂.
2. 条目完成后勾选 `[x]`, 并在条目后追加证据 (提交 hash / 测试类名 / 设备型号与 API / 度量值), 证据等级见附录 E.
3. 条目前缀标明主要落点: `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(文档)` 文档 / d.ts / Ace / 离线文档四个关联仓库, `(测试)`, `(发布)`.
4. 涉及宿主公开契约或脚本 API 的条目, 完成后必须同步宿主 `docs/dev/`, 宿主 `.changelog` (10 语言) 与本仓库 `.changelog`.
5. 附录 D 的 "待决事项" 在进入对应阶段前由维护者拍板, 拍板结果回填到 "固定决策".
6. 本仓库骨架 (Gradle / Manifest / 资源 / CI) 在 P0.1 按 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成, 其裁剪版即本仓库 `AGENTS.md`; 本插件没有独立启动入口 (D8), 因此 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 不适用, 图标按 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` 只生成基础 `mipmap/ic_launcher.png` 系列; 之后的工程约定以 `AGENTS.md` 为准, 本文件只记录 "改什么" 与证据.
7. 2026-10-02 的首个会话只落盘路线图; 同日 P0.1 完成 `git init` 与首批提交, 之后按 `AGENTS.md` 第 3 节的提交规则工作 (每笔提交前 `VERSION_BUILD = 提交数 + 1`).

---

## 1. 固定决策

以下决策 D1-D8 已由维护者于 2026-10-02 分两轮确认, 后续阶段不再重新讨论; D9-D31 为据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 仓库 `AutoJs6-Plugin-Compose-UI`; `rootProject.name=autojs6-plugin-compose-ui`; 显示名 `Compose UI` (英文, 不可翻译); `applicationId` / namespace / Kotlin 包 `io.github.supermonster003.autojs6.plugin.compose.ui`; 插件 ID `compose-ui`, engine `compose`, variant `default`; 脚本全局对象 `compose` (别名 `$compose`); 文档页 `api/compose.md`, 声明 `aj6-int-compose.d.ts`. 不并入 Three 系列 |
| D2 | 进程内渲染 | 插件 APK 内预编译的 Kotlin Compose 渲染器装入宿主进程, `ComposeView` 直接嵌入 `ScriptExecuteActivity` 与 floaty 窗口; 协议层 (节点 / 补丁 / 事件) 与传输无关, 进程外插件 Activity 只作为退路 (附录 E.2), 不在 1.0.0 实现 |
| D3 | API 范式: 两者都做 | 节点句柄 (`compose.Text({...})` 返回可长期持有的 `ComposeNode`, 直接改属性与增删子节点) 是底层协议对象; `compose.state` + `compose.mount(render)` 的声明式 render 层是上层糖, 两层共用同一条补丁管线, 1.0.0 同时交付 (附录 A.2 / A.3) |
| D4 | 1.0.0 承载面 | `"ui";` 模式 Activity 内容 (`compose.mount`) 与悬浮窗 (`compose.floaty`); "与旧 ui 混合" (XML 内 `<compose>` 容器, Compose 内 `AndroidView`) 与 "独立对话框 API" (`compose.dialog`) 排 1.1 (附录 F.2 / F.3) |
| D5 | 组件范围: 核心集 | 1.0.0 实现附录 A.5 的核心集 (30 项: 布局 7, 容器 / 顶栏 4, 文本 / 图标 / 图片 3, Button 家族 5 + IconButton, 选择与输入 6, 进度 2, 对话框与提示 2); 导航 / Tabs / 底部弹层 / 菜单 / 日期时间选择 / Pager / Grid / Chips 等宽集排 1.1 (附录 F.4) |
| D6 | TSX 排 1.1 | 1.0.0 只提供函数式元素工厂; TSX 工厂 (经 TypeScript Engine 插件现有 classic TSX 契约, 输出 Compose 节点而非 XML) 为 1.1 阶段 (附录 F.1), 1.0.0 的节点协议为其预留 `compose.createElement(type, props, ...children)` 接点 |
| D7 | 仅本地提交 | 与 3-Shell Terminal / 3-Setup Installer 当前策略一致: 本仓库与宿主改动均仅本地 Conventional Commits, 不推送 GitHub, 不登记官方索引, 不发 Release, 直至维护者明确恢复; 宿主 `PluginInstallWizardCatalog` 条目先落地 |
| D8 | 无独立界面 | 插件只有 Wake Activity, INFO 服务与渲染器入口, 没有启动器图标, 设置页与组件画廊 (画廊见附录 F.5); 示例脚本随插件 `assets/examples/` 提供并同步到宿主示例目录; 发行历史由插件中心展示 |
| D9 | 契约模块不依赖 Compose | 宿主新增 `plugin-api/compose-ui-api` (宿主编译并打包, 因此不得依赖任何 Compose 类): 装载面接口 (`ComposeUiRendererFactory` / `ComposeUiRenderer` / `ComposeUiHostEnvironment` / `ComposeUiEventSink`), 数据模型 (`UiNode` / `UiPatch` / `UiEvent` / `UiCommand` / `UiValue` / `ModifierOp` / `ThemeSpec`), 组件目录 (`ComponentCatalog`: 组件名, 属性类型, 插槽, 事件, 作用域限制), 常量 (`ComposeUiIds` / `ComposeUiCapabilityKeys` / `ComposeUiErrorCodes` / `ComposeUiLimits`); 插件以 `compileOnly` 消费该 AAR 的副本 (运行时类由宿主提供), 单元测试 `testImplementation` |
| D10 | 装载方式: 宿主 classloader 为父 | 宿主为插件 APK 自建 `PathClassLoader(apkPath, nativeLibraryDir, parent = 宿主 classLoader)`: 契约类型, Kotlin stdlib, kotlinx.coroutines 与宿主已有的 AndroidX (core / appcompat / activity / lifecycle / savedstate 等) 全部 parent-first 共享, 因而 `ComposeView` 能直接找到宿主 Activity 设置的 `ViewTreeLifecycleOwner` / `ViewTreeSavedStateRegistryOwner`; Compose 本体 (runtime / ui / foundation / material3 / animation / icons-core) 只在插件 APK, 由该加载器提供. 资源经 `createPackageContext(pkg, 0)` 单独取得. 不复用 `plugins.load` 的 `createPackageContext(CONTEXT_INCLUDE_CODE)` 隔离加载器 (它以 boot classloader 为父, 契约类型无法 cast, AndroidX 会重复加载). 入口类名由插件 Manifest meta-data `org.autojs.plugin.compose.RENDERER_FACTORY` 声明, 宿主经 `Class.forName(name, true, loader)` 实例化并 cast 为契约接口. 门禁: 插件中心已启用 + `PluginTrustManager.isAuthorized` + `requiresHostVersion` + 契约版本区间. P0.2 的 debug / release 插件加载已验证; 2026-10-02 维护者选择 Q1(b), 升级宿主共享 AndroidX, 正式保持 parent-first, 不启用附录 E.3 退路. 原生搜索路径使用 `<apk>!/lib/<当前进程 ABI>`, 不能仅按设备首选 ABI 选择; P1.1 起 V1 契约由宿主打包; P1.2 正式装载器与会话已使用 V1, 负版本探针实现已退役, 未使用的旧定义仅保留于未改动的锁定 AAR |
| D11 | ComposeView 上下文 | 宿主提供 `ComposeHostContext : ContextWrapper`: base 为宿主 Activity 或 floaty 服务上下文 (窗口, 系统服务, 主题属性), `getResources()` / `getAssets()` 委托插件包资源 (以宿主当前 `Configuration` 经 `createConfigurationContext` 对齐密度 / 夜间 / 语言), 使 material3 内部字符串 (`LocalContext.current.resources.getString(插件 R id)`) 与无障碍文案可解析; `getClassLoader()` 返回 D10 的插件加载器. P0.2 最小计数器证据确认保留宿主 `getTheme()` 即可, 不把插件资源主题覆盖到 Activity 上; RawWindow 在 attach 前显式设置宿主 LifecycleOwner 与 SavedStateRegistryOwner, 关闭时推进 DESTROYED 并 dispose. 扩展组件与 IME / 夜间配色仍需 P2 / P5 回归 |
| D12 | 线程模型 | 每个会话绑定一个 "脚本调度器": `"ui";` 模式下即 Android 主线程 (UI 模式脚本主线程就是 Android UI 线程, 见 3.1), 非 ui 脚本的 floaty 会话为脚本 looper 线程 (经 `ScriptAsyncDispatcher` 单跳回到脚本线程). render, state 变更, 事件回调, 节点属性写入都在脚本调度器执行; 补丁应用在主线程 (ui 模式同线程直接应用, 否则 `mainExecutor.execute`); 渲染器只经 `ComposeUiEventSink.enqueue` 投递事件, 不在组合 / 测量 / 布局 / 绘制期间同步执行 JS; `compose.post(fn)` 把任务投递到会话的脚本调度器, 供工作线程回写 state. 禁止主线程等待 JS 与 JS 同步等待主线程的双向等待 |
| D13 | 节点身份与差分 | 节点句柄持有会话内单调递增的 `nodeId`; render 模式下新树按 (父节点, `key` 或 类型 + 同类索引) 与上一棵树匹配并复用 `nodeId`, 宿主 `TreeReconciler` 产出补丁 ops (`setProps` / `insert` / `remove` / `move` / `replaceSlot`), 一次 render 合并为一个 `UiPatchBatch`; 渲染器把 `nodeId` 映射为 Compose `key(nodeId)` 以保留组合状态. 1.0.0 的 render 为整树重建 + 宿主差分, 不做脚本侧细粒度依赖裁剪; 动态列表项缺 `key` 时按索引匹配并对每个会话 warn 一次 (Q3) |
| D14 | 回调注册表 | JS 函数只留在宿主 `CallbackRegistry` (`sessionId`, `callbackId`, `generation`); 渲染节点只持 `callbackId`; 事件携带 `sessionId` / `generation` / `nodeId` / `eventType` / `callbackId` / `payload`, 会话关闭或 generation 过期的事件丢弃; 回调的增删与对应补丁同一批提交, 避免 "界面已换, 事件仍指向旧函数" |
| D15 | 输入框编辑态由原生维护 | `TextField` / `OutlinedTextField` 的文本与选区由渲染器内的 `TextFieldState` 持有; 脚本收 `onValueChange(text, editSeq)` 通知, 脚本改写文本用带 `editSeq` 的 `node.edit({ text, selection })` 命令, 渲染器拒绝 `editSeq` 落后于当前的编辑 (旧的脚本写入不覆盖更新的用户输入); 不做 "每输入一个字符等 JS 回传再显示"; 中文输入法组合, 连续删除, 粘贴, 光标移动是 P2.4 验收项 |
| D16 | Modifier 为有序操作链 | `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 生成 `ModifierOp` 序列并保留顺序 (`padding -> background` 与 `background -> padding` 是两个不同的链); 作用域限定操作 (`weight` 只在 Column / Row 子节点, `align` 只在 Box 子节点等) 由组件目录声明并在宿主校验 (`SCOPE_MISMATCH`); 常用快捷属性 (`w` / `h` / `padding` / `bg` / `weight` / `alpha`) 映射为链首的等价操作 |
| D17 | 协议上限 | 见附录 B.5, 写入 `ComposeUiLimits`; 超限返回 `LIMIT_EXCEEDED`, 整批补丁拒绝并保留上一版有效界面, 不应用到一半 |
| D18 | 错误模型 | 脚本侧抛 `ComposeError` (可 `instanceof`, 有 `code` / `message` / `nodeId` / `prop`); 代码词汇见附录 B.4; 插件缺失 / 禁用 / 未授权 / 版本过低分别为 `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_UNAUTHORIZED` / `PLUGIN_INCOMPATIBLE`, 消息以插件中心既有的本地化提示开头 (`AidlPluginHost.buildSelectionFailure` 的 5 条字符串复用); `compose.isAvailable()` 预探测不抛错 |
| D19 | 主题 | 默认 Material 3 `ColorScheme` 从宿主主题色 (`ThemeColorManager`) 与夜间模式派生, 字体随系统; `compose.theme({ seed, colors, dark, dynamicColor, typography })` 可按会话覆盖, Android 12+ `dynamicColor: true` 使用系统动态色; 主题以 `ThemeSpec` (纯数据) 跨契约传递, 由渲染器构造 `MaterialTheme` |
| D20 | 无障碍 | 每个节点可设 `testTag` / `contentDescription`; 渲染器在根 `semantics { testTagsAsResourceId = true }`; P4.2 核实宿主 `id()` / `desc()` 选择器对 Compose 语义节点的匹配规则 (无 `pkg:id/` 前缀的原样 tag) 并写入文档, 不预设与旧 View ID 的查找逻辑自动兼容 |
| D21 | 生命周期 | Activity 重建 (旋转 / 多窗口) 沿用 ui 模式现状: `ScriptExecuteActivity.onDestroy` 销毁引擎, 不承诺恢复; 会话在引擎退出, Activity destroy, 窗口 close 时释放 Composition (`ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool` + 显式 `dispose`), 回调注册表与补丁队列; `rememberSaveable` 不暴露给脚本 (1.0.0) |
| D22 | 插件 APK 形态 | 无插件自有原生代码, 不启用 ABI 拆分; 单个发布文件 `autojs6-plugin-compose-ui-v{VERSION_NAME}-{CRC32}.apk`, `getInfo()` 显式 `supportedAbis = emptyArray()`. Compose graphics-path 1.0.1 自带 `libandroidx.graphics.path.so` x arm64-v8a / armeabi-v7a / x86_64 / x86, 各约 10 KB, APK 中未压缩, 发布门禁校验 ELF 与 zip 偏移 16 KB 对齐. P0.2 已验证 D10 的 `<apk>!/lib/<进程 ABI>` 搜索路径. release 保留 R8 裁剪 / 混淆与 `isShrinkResources = true`, 保持 Kotlin ABI, 使用插件私有混淆包名, 补回 compileOnly lifecycle 的 ViewModel 构造规则; 因 R8 专用 Kotlin 方法不在宿主副本中, P0 禁用优化变换 (`-dontoptimize`), 恢复优化前必须重跑宿主内装载验证 |
| D23 | 插件中心与宿主注册 | INFO 服务 (action `org.autojs.plugin.INFO`, category `compose-ui`, 实现 `IPluginInfoProvider`), Wake Activity, meta-data `requiresHostVersion` 与 `org.autojs.plugin.compose.RENDERER_FACTORY`; 没有 Binder 能力服务 (与 ImGui 同形), 因此不进 `SERVICE_ACTION_BY_ENGINE`, 由 INFO 通用发现列出; 默认启用遵循 `PluginDefaultEnabledPolicy` 现状 (不新增例外); 宿主向导 `entry(official("compose.ui"), "Compose UI", UI)` 与 ImGui 同组 |
| D24 | 宿主退化 | 宿主只保留契约模块, 装载器, 会话核心与脚本 API; 插件缺失 / 禁用 / 未授权 / 不兼容时 `compose.mount` / `compose.floaty` 抛 D18 的对应错误, 宿主不内置任何 Compose 渲染实现, 也不回退到 XML 布局 |
| D25 | Compose 依赖与编译器 | 插件 `platform("androidx.compose:compose-bom:2026.09.00")` + `runtime` / `ui` / `foundation` / `material3` / `animation` / `material-icons-core` (extended 图标集见 Q2), `debugImplementation ui-tooling`; Compose 编译器插件 `org.jetbrains.kotlin.plugin.compose` 在根 `build.gradle.kts` 以 `version System.getProperty("gradle.kotlin.version") apply false` 声明 (与平台插件选定的 Kotlin 同版本), 不硬编码; 若 AGP 9 内置 Kotlin 不接受该编译器插件, 经构建验证后在插件仓库声明 `org.jetbrains.kotlin.android` 并在 `AGENTS.md` 注明理由 (规范 5.2 的例外条款) |
| D26 | 宿主共享依赖的版本纪律 | 2026-10-02 Q1(b) 定稿: 宿主 app / inrt 的 debug / release 共用 lifecycle 2.9.4, savedstate 1.3.2, emoji2 1.4.0, window 1.5.0, 由宿主版本目录单点声明; 插件编译依赖不能高于宿主, `locks/host-shared-deps.lock` 锁定 51 项解析版本. AndroidX / coroutines / serialization 共享项为插件 `compileOnly`, Kotlin stdlib 2.4.0 为 INFO 独立进程保留打包副本, 宿主内仍 parent-first 且 R8 必须保持 ABI. 宿主 `ComposeUiSharedClasspathTest` 与 `verifyComposeUiSharedClasspath` 守卫四个 runtime classpath, 插件 `verifySharedClasspath` 守卫编译与排除集合; 任一版本变化时同步两仓库锁与正式最低宿主版本. 依赖解析一致不等于 minified 宿主 ABI 已通过, P1 需补正式契约 / keep 规则 / 装载器并回填最低版本 |
| D27 | 图片与大对象 | `Image` 的 `src` 接受 `ImageWrapper` (`images.read` 等), 文件路径, 以及宿主 `R.drawable` 名称; 进程内直接传递 `Bitmap` 引用 (`UiValue.BitmapRef`), 不编码; 位图所有权归脚本 (`recycle` 由脚本决定), 渲染器只持弱引用并在节点移除时放手; URL 图片不在 1.0.0 |
| D28 | 兼容矩阵 | API 24 AVD x86, API 28 Sony G8441 (arm64), API 31 Sony XQ-AT72, API 33 Redmi 22120RN86C, API 35 Xiaomi 23046RP50C (HyperOS), API 37 AVD (16 KB 页); 每台设备记录宿主 build 与插件 build; HyperOS 上悬浮窗只在桌面之上活动且需 `requestFocus` (既有记录), 作为 P3.4 验收的已知条件 |
| D29 | 契约与最低宿主版本 | V1 `CONTRACT_VERSION = 1`, `MIN_SUPPORTED = 1`; P1.3 将 `REQUIRED_HOST_VERSION_CODE` 确认为 5316 (AutoJs6 6.8.0), 对应集成装载器 / 会话核心 / 插件中心注册的构建. 同步 Manifest, 公共文案, JVM / 设备断言与锁定 AAR, 只更新部署元数据, 不变更 V1 wire 语义. 原 5308 为建仓占位值, 保留于历史证据中 |
| D30 | 打包应用 (inrt) | 打包的脚本应用使用 `compose` 时需目标设备已安装并启用本插件 (与 epub / mail 等插件模块一致); P5.2 验证 inrt 构建中的装载路径与错误提示, 文档写明 |
| D31 | 示例与守卫 | 插件 `assets/examples/*.js` (计数器, 表单, 列表, 悬浮 HUD, 主题) 由 `assets/examples/index.json` 列出, P4.1 同步到宿主 `app/src/main/assets-app/sample/` 的对应分类; 示例必须能在兼容矩阵上运行, 作为 P5 回归用例 |

由 D2 / D9 / D10 派生的硬约束:

- 契约模块与宿主任何代码都不得 `import androidx.compose.*`; 宿主 `ComposeUiHostCompileGuardTest` 扫描宿主源码与契约模块拒绝该前缀.
- 插件渲染器不得持有 Rhino 对象 (`NativeObject` / `NativeArray` / `BaseFunction`) 或宿主内部类型; 进入渲染器的数据只能是契约数据模型, 转换与校验在宿主完成.
- 插件不复制宿主 `PluginInfo` 或契约伪实现; `compose-ui-api` 与 `common-plugin-api` 的 AAR 复制到本仓库 `libs/` 并以 SHA-256 锁定 (`locks/host-api-aars.lock`, 格式同 MCP Server / 3-Shell Terminal 插件).
- 已发布契约演进只追加: 数据模型加可选字段, 组件目录加组件 / 属性 / 事件, 接口以 `CONTRACT_VERSION` 协商; 不改既有字段语义.

---

## 2. 范围与非目标

范围内:

- 本仓库: 插件 APK (渲染器, 组件实现, 主题桥, Wake / INFO, 10 语言资源, README / changelog 生成, JVM 与 instrumentation 测试, CI 本地校验), 示例脚本, 契约 AAR 副本与锁文件.
- 宿主 `D:/idea-projects/AutoJs6`: `plugin-api/compose-ui-api` 契约模块; `core/plugin/compose/` 装载器, 会话, 差分, 回调注册表, 主题桥; `runtime/api/augment/compose/` 脚本 API `compose`; `ScriptRuntime` 注册; 插件中心向导条目; `docs/dev/compose-ui-plugin-protocol-v1.md`; changelog; 示例目录.
- 关联仓库: `AutoJs6-Documentation` (`api/compose.md` 与类型页, sidebar / toc / dataTypes / progress, `json/`), `AutoJs6-TypeScript-Declarations` (`aj6-int-compose.d.ts` 与 `index.d.ts` 引用), `AutoJs6-Plugin-Ace-Editor` (内置声明与 LSP 聚合再生成), `AutoJs6-Plugin-Offline-Docs` (离线文档同步).

范围内但在 1.0.0 之后交付 (2026-10-02 拍板):

- TSX / JSX 工厂 (附录 F.1, 1.1.0).
- 与旧 `ui` 混合: XML `<compose>` 容器与 Compose `AndroidView` (附录 F.2, 1.1.0).
- 独立对话框 / 底部弹层 API `compose.dialog` (附录 F.3, 1.1.0).
- 宽集组件 (附录 F.4, 1.1.0 起按需求).
- 组件画廊 Activity (附录 F.5, 未排期).

非目标 (本 Roadmap 不处理):

- 在设备上编译 Kotlin / Compose 源码; 让脚本经反射调用原始 `Composer` 或任意 `@Composable` 函数; 脚本加载任意第三方 Compose 库.
- 完整模拟 Kotlin `remember` / `LaunchedEffect` / `derivedStateOf` API; Vue / React 运行时适配 (协议稳定后可在上层自行实现).
- 跨进程把 `View` 或 Composition 传回宿主嵌入 (进程外模式只作为退路评估).
- 替换或改写旧 `ui` (XML / E4X) 的对象模型; `ui.someId` 继续只返回 `android.view.View`.
- Activity 重建后的脚本业务状态恢复 (D21).
- 对 `app/src/main/java/com/stardust/**` 兼容包的任何改动.

---

## 3. 现状诊断

以下是 2026-10-02 探查得到的事实, 是各阶段条目的直接依据. 行号以宿主快照 `77b5a3b0c5` 为准.

### 3.1 可直接复用的宿主能力

| 事实 | 锚点 |
| --- | --- |
| 进程内插件装载已有完整先例 (ImGui 插件): 宿主 `Plugins.load(packageName)` 检查 `PluginEnableStore.isEnabled` 与 `PluginTrustManager.isAuthorized`, 以 `createPackageContext(pkg, CONTEXT_INCLUDE_CODE or CONTEXT_IGNORE_SECURITY)` 取得包上下文, 读 Manifest meta-data `org.autojs.plugin.sdk.registry` 指定的类并反射调用 `loadDefault(hostContext, pluginContext, runtime, scope)`; 脚本侧 `plugins.load("包名")` 再 `require` 插件 `assets/plugin/index.js`; 插件中心 "自动挂载" 把导出对象挂为全局名 | `runtime/api/Plugins.kt:36-67, 84-91`, `core/plugin/Plugin.kt:127-143`, `runtime/api/augment/plugins/Plugins.kt:163-176`, `core/plugin/center/PluginAutoMountManager.kt:16-45`, `PluginMountStore.kt` |
| 该先例的局限: `createPackageContext` 的 classloader 以 boot 为父, 插件与宿主之间只能经 `Object` + 反射或 Rhino 动态调用 (`ImGuiPluginRegistry.loadDefault(Context, Context, Object, Object)` 返回 `ServiceConnection`), 契约类型无法共享; 这是 D10 自建加载器的原因 | `AutoJs6-Plugin-ImGui/.../ImGuiPluginRegistry.java:11-24`, `ImGuiPluginConnection.java:95-108` |
| 签名信任: `PluginTrustManager.isAuthorized(context, pkg)`, 官方签名 SHA-256 集合 `OFFICIAL_SHA_256`; 插件中心启用态 `PluginEnableStore` | `core/plugin/center/PluginTrustManager.kt:22, 42, 51` |
| ui 模式宿主: `ScriptExecuteActivity : AppCompatActivity` (因而宿主 `androidx.lifecycle` 的 `ViewTreeLifecycleOwner` / `ViewTreeSavedStateRegistryOwner` 已设在 decorView), `prepare()` 把 `activity` 放入引擎, `onDestroy` 销毁引擎, 事件 `create / resume / pause / back_pressed / key_down / activity_result / save_instance_state` 经 `emit` 进入脚本; `android.R.id.content` 上的 Insets 监听给系统栏加 padding 并返回 `CONSUMED` | `execution/ScriptExecuteActivity.kt:52-70, 121-126, 206-212, 255-279, 300-351` |
| `ui.setContentView(view)` 要求 `View`, 设置 `scriptRuntime.ui.view` 后在主线程 `activity.setContentView`; `ui.layout(xml)` 经 `DynamicLayoutInflater`; `ui.run` / `ui.post` / `ui.isUiThread`; `runtime/api/UI.kt` 的 `view` 字段只接受 `View` | `runtime/api/augment/ui/UI.kt:447-519, 523-539, 580-597`, `runtime/api/UI.kt:30, 55-60` |
| UI 模式脚本的 JavaScript 主线程就是 Android UI 线程 (文档明文), 因此 ui 模式下 render 与补丁应用同线程; 非 ui 脚本的回调回到脚本线程有成熟范例 `ScriptAsyncDispatcher.dispatchValues` (含溢出保护) 与 `ScriptPromiseAdapter` | `AutoJs6-Documentation/api/ui.md` "UI 模式" 节, `runtime/api/augment/ScriptAsyncDispatcher.kt:23, 130`, `augment/PromiseInterop.kt` |
| 悬浮窗基建: `runtime/api/Floaty.kt` 已有 `window(view: View)` / `rawWindow(view: View)` 重载 (`ViewSupplier.inflate` 直接返回既有 View), `JsResizableWindow` / `JsRawWindow` 提供 `setSize / setPosition / close / requestFocus / disableFocus / setTouchable`; 权限等待 `DisplayOverOtherAppsPermission.waitFor`; `RawWindow` 的窗口参数与 `FloatyService` | `runtime/api/Floaty.kt:43-67, 170-321`, `runtime/api/augment/floaty/Floaty.kt:103-145`, `core/floaty/RawWindow.kt:22-70` |
| 现有 classic TSX 契约: TypeScript Engine 插件把 TSX 编译为 `__autojs6Tsx(type, props, ...children)` 调用, 宿主 `TypeScriptTsxRuntime` 渲染为 XML 并绑定 click; F.1 的 Compose 工厂可在同一契约上按 `type` 路由 | `runtime/api/augment/ui/UI.kt:111, 120, 381`, `runtime/api/augment/ui/TypeScriptTsxRuntime.kt:1-120`, `AutoJs6-Plugin-TypeScript-Engine/.../CompilerHostProtocol.kt` |
| 脚本全局对象定义方式: `object X : Augmentable(), Invokable`, `AugmentableKey("name")`, `@RhinoRuntimeFunctionInterface` / `@RhinoFunctionBody` 双层, 注册于 `ScriptRuntime` (`PluginAutoMountManager.apply(this)` 在 :543 之后); epub / mail 的 `*NativeObject` / `*JsErrors` / `*Promises` 是插件型模块的现成形态 | `runtime/ScriptRuntime.kt:515-543`, `runtime/api/augment/epub/*.kt`, `runtime/api/augment/mail/*.kt` |
| 插件中心注册: INFO 通用发现 `queryDeclaredPluginServices` 的 `ServiceQuery(PluginActions.INFO)`; Binder 能力映射 `SERVICE_ACTION_BY_ENGINE` (本插件无 Binder 能力, 不进此表); 向导目录 `UI` 分组已有 ImGui 条目; 默认启用策略 `PluginDefaultEnabledPolicy` | `core/plugin/center/InstalledPluginRepository.kt:235-290`, `PluginCenterViewModel.kt:1046-1074`, `center/wizard/PluginInstallWizardCatalog.kt:71`, `PluginDefaultEnabledPolicy.kt:15` |
| 宿主 Manifest `<queries>` 已声明 `org.autojs.plugin.INFO` intent, 因此按 INFO 发现的包在 API 30+ 可见 | `app/src/main/AndroidManifest.xml:44` |
| 宿主 `isMinifyEnabled = false`, 契约接口与宿主装载器不需要额外 keep 规则 | `app/build.gradle.kts:1134` |
| 宿主运行时共享依赖版本 (D26 的初表): `core-ktx 1.15.0`, `activity-ktx 1.12.2`, `appcompat 1.7.1`, `material 1.13.0`, kotlinx-coroutines (由 Kotlin 版本间接确定); `androidx.lifecycle` / `savedstate` 为传递依赖, P0.2 用 `:app:dependencies` 导出精确版本 | `gradle/libs.versions.toml:20, 25, 28, 57` |
| 协议文档形态: `docs/dev/epub-plugin-protocol-v1.md` (状态, 决策, 服务表, 数据模型, 错误, 上限, 版本协商) | `docs/dev/epub-plugin-protocol-v1.md:1-45` |
| 示例目录: `app/src/main/assets-app/sample/` 按分类 (AutoJs6 / HTTP / Java API / JavaScript / OCR ...) 组织 | `app/src/main/assets-app/sample/` |

### 3.2 缺口 (需要新建或修改)

- 宿主没有任何 Compose 依赖 (`app/build.gradle.kts:503-504, 1089-1091` 只有注释掉的条目), 也没有以宿主 classloader 为父装载插件 APK 代码的工具类 (现有 `AndroidClassLoader.loadDex / loadJar` 面向脚本生成类, 不适用).
- 没有与引擎无关的界面协议 (节点 / 补丁 / 事件 / Modifier) 与树差分器; 旧 ui 以 View 为模型.
- `compose` 全局对象, `ComposeError`, 文档页, d.ts 全部不存在; `AugmentableKey("compose")` 当前无冲突 (已 grep).
- 插件中心对 "进程内插件" 的用户可见状态只有启用 / 授权, 没有 "宿主版本过低" 的装载前检查文案 (需复用 `AidlPluginHost.buildSelectionFailure` 的字符串或新增).
- 宿主示例目录没有 Compose 分类.

### 3.3 外部事实

- `@Composable` 函数经 Compose 编译器改写 (追加 `Composer` / changed 参数并插入重组管理代码), 不能按普通签名由 Rhino 调用; 本方案所有 Compose 调用都位于插件内正常编译的 Kotlin 代码, 脚本只提供数据 (D2).
- `AndroidComposeView` 附加到窗口时要求从视图树找到 `LifecycleOwner` 与 `SavedStateRegistryOwner`, 否则抛 "Composed into the View which doesn't propagate ViewTreeLifecycleOwner"; `WindowRecomposer` 同样从内容子视图向上查找 Lifecycle. 这两处查找用的是 `androidx.lifecycle` / `androidx.savedstate` 的 R id tag, 因此插件与宿主必须使用同一份这两个库的类 (D10), 否则需插件自持 owner (附录 E.3).
- material3 组件经 `LocalContext.current.resources` 读取自身字符串资源 (无障碍文案等), 这些 R id 属于插件包, 宿主 Activity 的 Resources 解析不到; 因此 ComposeView 的 Context 必须委托插件资源 (D11).
- Compose 的 `testTagsAsResourceId = true` 让 UiAutomator / 无障碍以 `testTag` 原样作为 `viewIdResourceName`, 不是 `pkg:id/name` 形式 (D20).
- Compose BOM `2026.09.00` 对应 Compose 1.12 系列 (2026-08 发布说明: compileSdk 37, AGP 9.1.1+); Kotlin 2.0 起 Compose 编译器插件随 Kotlin 版本发布, 由平台插件的 `gradle.kotlin.version` 决定 (D25).
- `ComposeView` 与既有 View 层级双向互操作 (`ComposeView` 嵌入 View, `AndroidView` 嵌入 Compose) 均为官方支持 API; 本路线图 1.0.0 只用前者 (D4).

---

## 4. 目标架构

### 4.1 数据流

```text
JavaScript 脚本 (ui 模式: Android 主线程; floaty 会话: 脚本 looper 线程)
  |  compose.Text({...}) / node.set / compose.state / compose.mount(render) / compose.floaty
  v
宿主 runtime/api/augment/compose  (Rhino 对象 <-> 契约数据; 参数校验; ComposeError)
  |  ComposeNode(nodeId, type, props, children, callbackIds)
  v
宿主 core/plugin/compose/ComposeSession  (CallbackRegistry, TreeReconciler, 上限校验, 脚本调度器)
  |  UiPatchBatch (setProps / insert / remove / move / replaceSlot), UiCommand, ThemeSpec
  |  -- ui 模式同线程; floaty 会话 post 到主线程 --
  v
插件 ComposeUiRenderer (D10 加载器装入宿主进程; ComposeView + mutableStateOf(rootNode))
  |  RenderNode(node): key(nodeId) { when(type) { Column -> ..., Text -> ..., TextField(TextFieldState) ... } }
  v
ComposeView  <- ScriptExecuteActivity.setContentView  |  JsRawWindow / JsResizableWindow (floaty)

事件 (click / valueChange / checkedChange / dismiss ...) 沿相反方向:
渲染器 ComposeUiEventSink.enqueue(UiEvent) -> ComposeSession 校验 generation -> 脚本调度器 -> JS 回调 -> state 变更 -> 合并调度下一次 render
```

### 4.2 插件包结构 (`app/src/main/java/io/github/supermonster003/autojs6/plugin/compose/ui/`)

```text
ComposeUiPlugin.kt                      身份常量 (与 D1 / 4.4 对齐)
ComposeUiPluginInfoService.kt           IPluginInfoProvider (org.autojs.plugin.INFO, category compose-ui)
WakeActivity.kt
renderer/
  ComposeUiRendererFactoryImpl.kt       契约 ComposeUiRendererFactory 实现 (Manifest meta-data 指向它)
  ComposeUiRendererImpl.kt              ComposeView 宿主, rootState, 补丁应用, 命令执行, dispose
  RenderNode.kt                         节点分派 (when(type)), key(nodeId)
  NodeStore.kt                          nodeId -> 节点快照; 补丁应用到不可变树
  ModifierMapper.kt                     ModifierOp 链 -> Modifier
  ThemeMapper.kt                        ThemeSpec -> MaterialTheme (ColorScheme / Typography / dynamicColor)
  ValueMapper.kt                        UiValue (颜色 / 尺寸 / 文本样式 / BitmapRef / 图标名) -> Compose 类型
  components/
    LayoutComponents.kt                 Column / Row / Box / Spacer / LazyColumn / LazyRow / Scaffold
    SurfaceComponents.kt                Surface / Card / HorizontalDivider / TopAppBar
    TextComponents.kt                   Text / Icon / Image
    ButtonComponents.kt                 Button 家族 / IconButton
    SelectionComponents.kt              Switch / Checkbox / RadioButton / Slider
    TextFieldComponents.kt              TextField / OutlinedTextField (TextFieldState, editSeq)
    ProgressComponents.kt               CircularProgressIndicator / LinearProgressIndicator
    DialogComponents.kt                 AlertDialog / Snackbar (经 Scaffold snackbarHost)
  input/
    TextFieldEditState.kt               原生编辑态 + editSeq 仲裁
  a11y/
    SemanticsSupport.kt                 testTag / contentDescription / testTagsAsResourceId
src/main/assets/examples/               index.json + *.js (D31)
src/test/                               JVM: ManifestContractTest, PluginInfo, 资源守卫, ModifierMapper / ValueMapper / NodeStore 纯逻辑
src/androidTest/                        渲染器 instrumentation (需宿主 compose-ui-api 类在测试 APK 中可用: 以 androidTestImplementation 引入 AAR)
```

### 4.3 宿主包结构

```text
plugin-api/compose-ui-api/src/main/java/org/autojs/plugin/compose/api/
  ComposeUiIds.kt / ComposeUiActions.kt / ComposeUiCapabilityKeys.kt / ComposeUiErrorCodes.kt / ComposeUiLimits.kt
  ComposeUiContract.kt                  CONTRACT_VERSION, META_RENDERER_FACTORY, INFO category
  loading/ComposeUiRendererFactory.kt, ComposeUiRenderer.kt, ComposeUiHostEnvironment.kt, ComposeUiEventSink.kt
  model/UiNode.kt, UiPatch.kt, UiPatchBatch.kt, UiEvent.kt, UiCommand.kt, UiValue.kt, ModifierOp.kt, ThemeSpec.kt
  catalog/ComponentCatalog.kt, ComponentSpec.kt, PropSpec.kt, SlotSpec.kt, EventSpec.kt, ScopeKind.kt

app/src/main/java/org/autojs/autojs/core/plugin/compose/
  ComposeUiPluginHost.kt                发现 (INFO + meta-data), 门禁, 版本协商, 加载器缓存
  ComposeUiPluginLoader.kt              D10 PathClassLoader, 资源上下文, 工厂实例化
  ComposeHostContext.kt                 D11
  ComposeSession.kt                     会话: 节点表, 回调注册表, 补丁队列, 脚本调度器, 生命周期
  TreeReconciler.kt                     D13
  CallbackRegistry.kt                   D14
  ComposeThemeBridge.kt                 D19 宿主主题 -> ThemeSpec
  ComposeUiError.kt / ComposeUiErrorMapper.kt
app/src/main/java/org/autojs/autojs/runtime/api/augment/compose/
  Compose.kt                            Augmentable: isAvailable / version / mount / floaty / state / modifier / theme / post / createElement / 元素工厂
  ComposeNodeNativeObject.kt            节点句柄 (set / get / append / insert / remove / replace / on / off / focus / scrollTo / edit)
  ComposeStateNativeObject.kt           state.value 依赖收集与变更调度
  ComposeModifierBuilder.kt             链式 Modifier
  ComposeSessionNativeObject.kt         mount 返回对象 (root / update / close / post / on)
  ComposeFloatyWindowNativeObject.kt    floaty 会话 (继承 JsRawWindow / JsResizableWindow 能力)
  ComposeJsErrors.kt                    ComposeError
app/src/main/java/org/autojs/autojs/runtime/api/compose/
  ComposeScriptArguments.kt / ComposeScriptValues.kt   Rhino 值 <-> UiValue (颜色 / 尺寸 / 图片 / 回调)
docs/dev/compose-ui-plugin-protocol-v1.md
```

### 4.4 身份派生表

| 项目 | 值 |
| --- | --- |
| 仓库与目录名 | `AutoJs6-Plugin-Compose-UI` |
| `rootProject.name` | `autojs6-plugin-compose-ui` |
| 应用标题 (不可翻译) | `Compose UI` |
| `applicationId` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.compose.ui` |
| 插件 ID / engine / variant | `compose-ui` / `compose` / `default` |
| INFO 服务 | `ComposeUiPluginInfoService`, action `org.autojs.plugin.INFO`, category `compose-ui` |
| 渲染器入口 meta-data | `org.autojs.plugin.compose.RENDERER_FACTORY` = `io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl` |
| 宿主契约包 | `org.autojs.plugin.compose.api` (`plugin-api/compose-ui-api`) |
| 契约版本 / 最低宿主 | `CONTRACT_VERSION = 1`; `REQUIRED_HOST_VERSION_CODE` 在 P1.3 回填 |
| 脚本全局对象 | `compose` (别名 `$compose`), 错误类 `ComposeError` |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 |
| Compose | BOM `2026.09.00`; 编译器插件版本 = `gradle.kotlin.version` |
| 发布文件名 | `autojs6-plugin-compose-ui-v{VERSION_NAME}-{CRC32}.apk` (单 APK, D22) |
| 向导条目 | `entry(official("compose.ui"), "Compose UI", UI)` |
| 文档 / 声明 | `api/compose.md` + 类型页; `aj6-int-compose.d.ts` |

---

## 5. 阶段总览

| 阶段 | 内容 | 目标版本 | 状态 |
| --- | --- | --- | --- |
| P0 | 仓库骨架; 进程内装载 spike (加载器, owner, 资源, 计数器闭环) | 1.0.0 | 已完成 (2026-10-02, P0.1 - P0.3, Q1(b)) |
| P1 | 宿主契约模块, 装载器, 会话核心, 注册与协议文档 | 1.0.0 | 已完成 (2026-10-03, P1.1 - P1.3) |
| P2 | 插件渲染器: 骨架, 核心集组件, 输入框, Modifier 链, 主题 | 1.0.0 | 未开始 |
| P3 | 脚本 API `compose`: 节点句柄层, state + render 层, ui 模式与悬浮窗承载, refs / 命令 / 错误 | 1.0.0 | 未开始 |
| P4 | 示例, 无障碍与选择器, 守卫测试 | 1.0.0 | 未开始 |
| P5 | 健壮性, 兼容矩阵 (含 inrt), 性能与体积 | 1.0.0 | 未开始 |
| P6 | 文档, 声明, README / changelog, 1.0.0 本地 gate | 1.0.0 | 未开始 |
| P7 | 1.1.0 候选: TSX, 与旧 ui 混合, `compose.dialog`, 宽集组件 | 1.1.0 | 未开始 |

依赖关系: P0.2 的结论决定 D10 / D11 是否调整 (P0.3); P1 依赖 P0.3; P2 与 P3 可在 P1.1 契约冻结后并行 (P2 先于 P3.3 的端到端验收); P4 - P6 依赖 P2 / P3; P7 依赖 1.0.0 gate.

---

## P0: 仓库骨架与可行性 spike

目标: 建立可构建的插件仓库, 并用最小闭环 (宿主临时钩子 + 三个组件 + 一个回调) 证明 D10 / D11 的进程内装载可行, 把 AndroidX 共享与资源委托的事实固定下来.

### P0.1 仓库骨架

- [x] (插件) 按 `AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` 生成骨架: `settings.gradle.kts` (平台插件 1.8.3 在 `includeBuild("build-logic")` 之前), 根 `build.gradle.kts` (`com.android.application` 与 `org.jetbrains.kotlin.plugin.compose` 均以平台属性声明并 `apply false`), `build-logic` (从 3-Setup Installer / OpenCC 复制后精简, 不带 native-alignment), `version.properties` (`VERSION_NAME=1.0.0`, `VERSION_BUILD=1`, compileSdk / targetSdk 37, minSdk 24, `OVERRIDDEN_*=NONE`), `.gitignore` / `sign.properties` / `app/sm003.jks` 从宿主复制并以 `git check-ignore` 验证. 证据: 提交 e7efcea; Temurin 模拟命令下平台决策 Gradle 9.5.0 / AGP 9.3.2 (auto-specified) / Kotlin 2.3.20 (nearest-lower-matched) / KSP 2.3.12 / R8 8.13.19, 版本决策块只打印一次; `git check-ignore` 确认 `sign.properties`, `app/sm003.jks`, `local.properties` 被忽略.
- [x] (插件) `app/build.gradle.kts`: `buildFeatures { compose = true; aidl = true; resValues = true }`, Compose BOM 与 D25 模块, 契约 AAR `compileOnly(files("libs/compose-ui-api.aar"))` (P1.1 产出前先用 P0.2 的 spike 契约草案 jar), `implementation(files("libs/common-plugin-api.aar"))`, release R8 + shrinkResources, 无 ABI 拆分 (理由注释), `appendDigestToReleasedFiles` 校验单 APK 并追加 CRC32. 验收: Temurin 模拟命令 `assembleDebug testDebugUnitTest` 只打印一段版本决策, 无 `checkAarMetadata` 错误; 若 AGP 内置 Kotlin 拒绝 Compose 编译器插件, 按 D25 处理并记录. 证据: 提交 e7efcea + 657f021; `assembleDebug testDebugUnitTest` 通过, 无 `checkAarMetadata` 错误, AGP 9 内置 Kotlin 接受 `org.jetbrains.kotlin.plugin.compose` 2.3.20 (未触发 D25 退路); debug APK 11,351,432 B, release APK 235,182 B (R8 + shrinkResources, 骨架尚未引用 Compose 代码); `appendDigestToReleasedFiles` 产出 `releases/autojs6-plugin-compose-ui-v1.0.0-fae2797b.apk`. 偏差: 契约 AAR `compose-ui-api.aar` 尚不存在, `compileOnly` 依赖留待 P0.2 spike 契约草案; Compose 模块暂以 `implementation` 打包, D26 共享依赖锁在 P0.2 落地后再改 `compileOnly`. 发现: Compose `ui-graphics` 传递依赖 `androidx.graphics:graphics-path` 1.0.1 打包 `libandroidx.graphics.path.so` x 4 ABI (见 D22 批注), 发布门禁改为校验原生库集合 + 未压缩 + ELF `PT_LOAD` 与 zip 偏移 16 KB 对齐 (四个库均 0x4000 对齐, 通过).
- [x] (插件) Manifest: `org.autojs.permission.PLUGIN`, `WAKE_ACTIVITY` + `WakeActivity`, `org.autojs.plugin.info.AUTHOR`, `requiresHostVersion`, `org.autojs.plugin.compose.RENDERER_FACTORY`, INFO 服务 (exported, PLUGIN 权限, category `compose-ui`); 无 launcher; `allowBackup=false` + `dataExtractionRules`. 证据: 提交 aeb0af8; `ManifestContractTest` 6 项通过; `requiresHostVersion=5308` 为临时值 (D29, P1.3 回填).
- [x] (插件) 资源: 11 个 values 目录, `strings_donottranslate.xml` (`app_name`, `plugin_id` / `plugin_engine` / `plugin_variant` 经 `resValue`), `plugin_description` 10 语言 (例: `以 Jetpack Compose 与 Material 3 渲染脚本声明的界面`, 句尾无点号, 不含 "AutoJs6"), `plugin_author`; 基础 `mipmap/ic_launcher.png` (Q6 源图到位前用 `.python/generate_launcher_icons.py` 以临时源图生成并标记待替换). 证据: 提交 9879baa; `StringResourceParityTest` 5 项通过; 图标由临时源图 `.python/icons/compose-ui-ic-launcher-light.png` / `-dark.png` (1024 x 1024, 占位几何图形) 经 `generate_launcher_icons.py` 生成并 `--check` 通过, 待 Q6 替换.
- [x] (插件) `.readme/` + `.changelog/` 10 语言 JSON 与模板, `.python/generate_markdown.py` (+ `.bat`, `--check`), 根 `README.md` 为简体中文; `LICENSE` MPL-2.0; `THIRD_PARTY_NOTICES.md` (Compose / AndroidX Apache-2.0, 宿主 AAR MPL-2.0); `libs/README.md` + `locks/host-api-aars.lock`. 证据: 提交 9879baa + 657f021; `generate_markdown.py` 生成 36 个产物且 `--check` 通过; `HostApiAarLockTest` 4 项通过 (锁 / 文件 / `THIRD_PARTY_NOTICES.md` / `libs/README.md` 摘要一致).
- [x] (插件) `AGENTS.md`: 参考规范裁剪版, 写明本仓库事实: 进程内渲染, 无原生库 / 无 ABI 拆分理由, 无独立界面, D7 推送策略, D26 共享依赖锁, 验证顺序. 证据: 提交 9879baa + 657f021 (原生库事实修正: 第 1, 2, 5.3, 7, 9, 13 节, "无原生库" 改为 "无插件自有原生代码").
- [x] (插件) JVM 守卫: `ManifestContractTest` (权限, meta-data, Wake, INFO 契约, 无 launcher), `ComposeUiPluginRuntimeInfoTest` (PluginInfo 纯数据映射, `supportedAbis` 显式空数组, 身份常量对齐 `common.json` / Gradle), `StringResourceParityTest`, `ApplicationTextPunctuationTest`, `HostApiAarLockTest`. 证据: 提交 6cc0d52 + 657f021; `:app:testDebugUnitTest` 19 项全部通过 (E1).
- [x] (插件) `.github/workflows/build.yml` (JVM 测试, debug / release 装配, lint, androidTest 装配; API 24 x86 与 API 35 x86_64 模拟器契约测试) 与 `markdown.yml`; 未推送期间只做本地语法与路径校验. 证据: 提交 6cc0d52; 工作流已落盘, 未推送 (D7), 仅本地语法与路径校验; 本地等效命令 `testDebugUnitTest assembleDebugAndroidTest lintDebug assembleRelease` 通过, lint 0 问题. 额外 (E3): `ComposeUiPluginContractTest` 5 项在 Xiaomi Pad 23046RP50C (HyperOS, API 35, arm64-v8a) 与 Sony G8441 (API 28, arm64-v8a) 上全部通过 (含 `System.loadLibrary("androidx.graphics.path")`), 测试后无残留安装. 未执行: API 24 x86 / API 35 x86_64 / 16 KB 页 AVD 的模拟器矩阵 (留给 CI 与 P0.2).
- [x] (插件) `git init`, 按 "身份与构建骨架 / 契约与激活 / 资源与文档 / 测试与 CI" 拆分初始提交, 每笔提交前 `VERSION_BUILD = 提交数 + 1`, 最终 `VERSION_BUILD == git rev-list --count HEAD` 且工作区干净. 证据: 6 笔提交 e7efcea (身份与构建骨架), aeb0af8 (契约与激活), 9879baa (资源与文档), 6cc0d52 (测试与 CI), 657f021 (graphics-path 原生库修正), 本提交 (路线图证据); 最终 `VERSION_BUILD=6 == git rev-list --count HEAD`, 工作区干净.

### P0.2 进程内装载 spike

- [x] (宿主, 临时) 在宿主开发者选项或临时测试 Activity 中放一个 "Compose spike" 入口 (不进 release 路径, P0.3 结束时删除或改为正式装载器): 发现插件包, 门禁检查, 按 D10 构造 `PathClassLoader(parent = 宿主 classLoader)`, 反射实例化工厂, `ComposeHostContext` 包装, 把渲染器 `view()` 作为 Activity 内容. 证据: 宿主提交 `21dff98f26`, 独立 worktree 分支 `spike/compose-ui-p0`, debug-only `ComposeSpikeActivity` / `ComposeSpikeLoader` / `ComposeHostContext`, 独立包名保护既有宿主; API 33 Redmi 与 API 37 / 16 KB AVD 的 debug / release 插件加载通过 (宿主 5309, 插件 6). 详见 `docs/dev/p0-spike-evidence.md`.
- [x] (插件, spike) 最小渲染器: `UiNode` 三种类型 (Column / Text / Button), `mutableStateOf(root)`, `Button.onClick -> eventSink.enqueue(callbackId)`; 宿主侧计数器: 点击后重建树并 `apply`, 文本更新. 证据: `renderer/SpikeRenderer.kt`, 草案 `api.spike` 版本 -1 (未冻结 V1); 实际无障碍点击后 `Count: 0 -> Count: 1`, 两台设备通过; `SpikeTreeTest` 5 项输入边界与快照测试通过.
- [x] (测试) 验证清单, 每项记录结论与证据 (设备, 日志, 堆栈). 证据: `docs/dev/p0-spike-evidence.md`, Redmi 22120RN86C API 33 (E3) 与 AVD_API_37.1_16K API 37 (E2), 宿主 `ComposeUiSpikeDeviceTest` 各 5 项在 debug / release 插件上通过, 独立冷进程 PSS 各 1 项通过; 24 项插件 JVM 测试通过. 下列结论只覆盖该最小 spike, 不代表 P5 全矩阵完成:
  - 加载器: Compose 类来自插件 APK, `androidx.lifecycle` / `savedstate` / `core` / `activity` / `appcompat` / kotlin stdlib / coroutines 来自宿主 (对比 `Class.getClassLoader()`); 无 `NoSuchMethodError` / `LinkageError`.
  - owner: `ComposeView` 在 `ScriptExecuteActivity` 等价 Activity 内附加成功 (无 "doesn't propagate ViewTreeLifecycleOwner"); 在 `RawWindow` (非 Activity 窗口) 内附加时的 owner 来源 (预期需宿主为 floaty 窗口提供 `LifecycleOwner` + `SavedStateRegistryOwner`, 记录方案).
  - 资源: material3 组件的无障碍字符串可解析 (开启 TalkBack 或读取语义树), 密度 / 夜间 / 语言随宿主 Configuration; 是否需要委托 `getTheme()`.
  - 版本: 导出宿主 `:app:dependencies` 中 AndroidX / coroutines 精确版本, 与 Compose BOM `2026.09.00` 的传递依赖下界比对, 形成 D26 的首版 `host-shared-deps.lock`. 结果: 51 项, 指纹 `f3042acc624d499feea9907a20257b62debaa6c523f40bfce19b084274334576`; debug 宿主需升级 lifecycle / savedstate / emoji2 / window, Q1 正式决策待确认. Kotlin 因 INFO 独立进程需随插件打包并保留 ABI, 渲染时仍 parent-first; SavedState 的 Compose 注解从宿主 runtime 排除.
  - R8: 插件 release 构建后仍可装载 (keep 工厂与契约实现), 记录 APK 体积与 dex 方法数. 结果: release 1,459,210 B / 28,213 DEX 方法引用 (插件 build 6); 保留 Kotlin ABI + `-dontoptimize` + 补回 compileOnly lifecycle 的 ViewModel 构造保留规则后通过. 三次失败堆栈与原因见证据文件; 未验证 minified 宿主 (P1 / P5).
  - 内存: 装载前后宿主 PSS 差值 (API 33 真机 + API 37 AVD). 结果: 独立进程单次观测分别 +13,102 KiB / +428 KiB, 退出读数与测量局限见证据; 尚不是性能门槛.
  - 退出: Activity destroy 后 Composition 释放, 无 `Recomposer` / `Choreographer` 回调泄漏 (LeakCanary 或 `dumpsys meminfo` 二次对比). 结果: Composition / DisposableEffect 计数与 Recomposer ShutDown 断言通过, Debug.MemoryInfo PSS 退出对比已记录; 未做 LeakCanary 或 Choreographer 堆对象审计, 不推断一般性无泄漏.
- [x] (测试) 失败路径: 插件未安装 / 未启用 / 未授权 / `requiresHostVersion` 过高 / 工厂类缺失 / 契约版本不匹配, 各自产生可区分的错误而不是崩溃. 证据: `loaderFailuresAreDistinguishable`, 两台设备通过; 未安装 / 系统停用 / 未授权通过门禁快照注入, 插件中心停用通过真实 store 切换并恢复, 版本与缺失工厂经真实校验 / 反射路径. 未改变用户安装包或授权记录.

### P0.3 spike 结论回填

维护者于 2026-10-02 明确选择 Q1(b): 升级宿主共享依赖. 本阶段把已验证版本应用于 app / inrt 的 debug / release, 并删除临时 Activity, 将 P0 草案加载器与计数器控制器迁入 instrumentation, 从宿主 APK 移除临时入口; V1 契约与正式装载器仍在 P1 实施.

- [x] (宿主 / 插件) 按 P0.2 结果定稿 D10 / D11 (或启用附录 E.3 退路并回填 Q1 拍板), 写 `docs/dev/compose-ui-spike-evidence.md` (宿主) 与本仓库 `docs/dev/p0-spike-evidence.md`; 删除宿主临时入口或转为 P1.2 的正式装载器; 更新本路线图 "固定决策" 与附录 B 草案中受影响的字段. 证据: 宿主提交 `fb784f034a`, 插件为本条所在提交 (build 8); 2026-10-02 维护者批准 Q1(b); 51 项版本用于四种宿主变体并经 JVM 快照守卫, 原临时 Activity 删除, P0 草案与探针迁至 instrumentation 并复用宿主 AboutActivity. app debug / androidTest / release 装配通过, release 原生对齐通过; 宿主 JVM 18 项, 插件 JVM 24 项, API 24 x86 AVD 与 API 33 Redmi 各 12 项设备回归通过. 详见两仓库证据文档的 P0.3 addendum. 正式 V1 契约 / 装载器仍待 P1, 不把草案计作 V1 交付.

---

## P1: 宿主契约, 装载器与注册

目标: 宿主拥有可编译, 可测试的契约与装载链路, 尚不含脚本 API; 契约在 P1.1 结束时冻结为 V1.

### P1.1 契约模块 `plugin-api/compose-ui-api`

- [x] (宿主) `settings.gradle.kts` `pluginApi` 列表加入 `compose-ui-api`; 模块 `build.gradle.kts` 为 `com.android.library`, `api(project(":plugin-api:common-plugin-api"))`, 不依赖 Compose; `app/build.gradle.kts` `implementation(project(":plugin-api:compose-ui-api"))`.
- [x] (宿主) 常量: `ComposeUiIds` (PLUGIN_ID / ENGINE / VARIANT / REQUIRED_HOST_VERSION_CODE 占位), `ComposeUiContract` (CONTRACT_VERSION = 1, META_RENDERER_FACTORY, INFO_CATEGORY), `ComposeUiCapabilityKeys` (CONTRACT_VERSION / COMPONENTS / FEATURES / COMPOSE_VERSION / SHARED_DEPS_FINGERPRINT), `ComposeUiErrorCodes` (附录 B.4), `ComposeUiLimits` (附录 B.5).
- [x] (宿主) 装载面接口与数据模型 (附录 B.1 / B.2), Kotlin 实现, 不可变 (`List` 深拷贝), 带 `Parcelable` 实现以便将来进程外复用但 1.0.0 不经 Binder 传输.
- [x] (宿主) `ComponentCatalog.V1` (附录 B.3): 核心集每个组件的属性 (名称 / 类型 / 必填 / 默认 / 作用域), 插槽 (`content` / `title` / `actions` / `label` / `leadingIcon` ...), 事件 (名称 / payload 字段); `ModifierOp` 词汇与参数类型; 快捷属性映射表.
- [x] (测试) `ComposeUiContractTest` 快照全部字面量 (常量, 错误码, 上限, 目录条目数与名称, ModifierOp 词汇); `ComponentCatalogConsistencyTest` (属性名唯一, 作用域引用的父组件存在, 事件 payload 字段类型合法); `ComposeUiHostCompileGuardTest` (宿主与契约源码无 `androidx.compose` 引用).
- [x] (插件) 契约 AAR 复制到 `libs/compose-ui-api.aar`, 更新 `locks/host-api-aars.lock`, `libs/README.md`, `THIRD_PARTY_NOTICES.md`.

证据: 宿主提交 `d9b090fd68`, 插件为本条所在提交 (build 9), `docs/dev/p1-contract-evidence.md` 与宿主 `docs/dev/compose-ui-contract-evidence.md`. V1 快照 747 条, 30 项目录 / 20 个 Modifier / 8 个快捷映射; API JVM 13 项, 宿主共享类路径 JVM 3 项, 插件 JVM 24 项通过; API 24 / 33 Parcel 设备测试各 6 项, API 33 release 插件宿主回归 6 项, 插件 debug 契约 5 项. AAR SHA-256 `e6024147dd45776e1f0bc178da66a1a337e9d084cbe3dbcf244291e20857ba21`. `api.spike` 的 -1 接口只保留给回归, 不进入 V1 冻结面, 随 P2.1 迁移删除.

### P1.2 宿主装载器与会话核心

- [x] (宿主) `ComposeUiPluginHost`: 按 INFO 发现 + `RENDERER_FACTORY` meta-data 定位插件包; 门禁顺序 已安装 -> 应用未被系统停用 -> 插件中心启用 -> 授权 -> `requiresHostVersion` -> 契约版本区间 -> 共享依赖指纹 (D26); 每种失败映射为 `ComposeUiError` 代码与本地化消息; 加载器与工厂按包 + versionCode 缓存, 插件更新 / 卸载广播使缓存失效.
- [x] (宿主) `ComposeUiPluginLoader` (D10) 与 `ComposeHostContext` (D11); floaty 窗口的 owner 提供方案按 P0.2 结论实现 (`ComposeFloatyOwners`: 随窗口 create / destroy 推进 `LifecycleRegistry` 与 `SavedStateRegistryController`).
- [x] (宿主) `ComposeSession`: 节点表 (nodeId 分配, 父子关系, 插槽), `CallbackRegistry` (D14), 补丁队列与批量提交 (一次脚本调度器 tick 合并为一个 `UiPatchBatch`), 上限校验 (D17), 脚本调度器抽象 (`ScriptUiDispatcher`: ui 模式 = 主线程直接执行; 否则经 `ScriptAsyncDispatcher`), 生命周期 (`open / attach(view) / detach / close`), 事件入口 (generation 校验 -> 调度器 -> 回调).
- [x] (宿主) `TreeReconciler` (D13): 旧树 / 新树 -> 补丁 ops; key 匹配, 同类索引回退, 移动检测, 插槽替换; 纯 JVM 可测.
- [x] (宿主) `ComposeThemeBridge` (D19): 宿主主题色 + 夜间模式 -> `ThemeSpec`; 监听宿主主题变更时更新活动会话.
- [x] (测试) JVM: `TreeReconcilerTest` (插入 / 删除 / 移动 / 重排 / 缺 key 回退 / 插槽), `ComposeSessionLimitsTest` (节点数 / 深度 / 批大小 / 字符串长度 / 回调数), `CallbackRegistryTest` (generation 过期丢弃, close 后丢弃), `ComposeUiPluginHostSelectionTest` (门禁顺序与错误映射, 用假 PackageManager), `ComposeUiSharedClasspathTest` (D26 快照).
- [x] (测试) instrumentation (需插件 debug APK 已安装): `ComposeUiLoaderTest` 装载 + 计数器往返 + dispose 无泄漏; 在 API 24 x86 AVD 与一台 arm64 真机执行.

P1.2 证据 (2026-10-02): `docs/dev/p1-session-evidence.md`; 宿主提交 `adf66b07e7`, 插件为本条所在提交 (build 10). 宿主 Compose JVM 16 项 + API JVM 13 项, 插件 JVM 25 项通过; API 24 x86 临时 AVD 与 Xiaomi Pad API 35 arm64 各 7 项 debug 宿主测试 + 2 项 minified release 往返 / INFO 测试通过, 插件自身契约测试 5 项通过. 共享指纹与冻结 V1 AAR 未变. 插件入口提前迁移 V1, 仅支持三个预览组件及已列明的属性 / Modifier, 完整渲染器仍属 P2. 当前无公开 compose 脚本入口, 不宣称 P1.3 / P3 已完成.

### P1.3 注册, 协议文档, changelog 与版本回填

- [x] (宿主) `PluginInstallWizardCatalog` 增加 `entry(official("compose.ui"), "Compose UI", UI)`; 确认插件中心对本包的 INFO 发现, 启用 / 禁用 / 授权与 `requiresHostVersion` 展示正确 (`PluginCenterComposeUiRegistrationTest`); 如 INFO 发现需要包名 `<queries>` 则补充.
- [x] (宿主) `docs/dev/compose-ui-plugin-protocol-v1.md`: 状态, 决策, 装载面, 数据模型, 组件目录, 事件, 命令, 错误, 上限, 版本协商, 线程与所有权, 与 ImGui 注册表路线的差异.
- [x] (宿主) 回填 `ComposeUiIds.REQUIRED_HOST_VERSION_CODE` 为本阶段提交后的 `VERSION_BUILD`; 插件 `requiresHostVersion` 与 `README` / 测试同步.
- [x] (宿主) `.changelog` 10 语言: feature 说明 Compose UI 开发预览已纳入插件中心, 可管理已安装插件的启用 / 授权 / 兼容性; compose 脚本 API 随 P3 实际交付后再公告, 与 hint (6.8.0 既有 "部分内置功能改由独立插件提供" 条目下补充); 生成器 `--check`.

---

P1.3 证据 (2026-10-03): `docs/dev/p13-p2-evidence.md`. 宿主基于当前 master 同步后为 6.8.0 / 5316; 25 项宿主 JVM + 13 项 API JVM, debug / release 装配与原生对齐通过; API 24 x86 与 API 35 arm64 各 9 项 V1 装载 / 受控事件测试通过. 最低版本回填为 5316, 新 AAR 仅变更部署门槛. 公告按实际交付写为开发预览, 避免在 P3 前宣称存在 compose 脚本入口; 安装向导仍需远端索引实际存在该包才展示下载项.

## P2: 插件渲染器核心

目标: 渲染器实现核心集全部组件, Modifier 链, 主题与输入框原生编辑态, 通过宿主 instrumentation 的补丁 / 事件往返.

### P2.1 渲染器骨架

- [ ] (插件) `ComposeUiRendererFactoryImpl` (契约版本, capabilities: COMPONENTS 列表, FEATURES, COMPOSE_VERSION, SHARED_DEPS_FINGERPRINT), `ComposeUiRendererImpl` (`ComposeView(ComposeHostContext)`, `setViewCompositionStrategy`, `setContent { MaterialTheme(ThemeMapper(theme)) { RenderNode(rootState.value) } }`, `apply(batch)` 在主线程更新不可变树, `execute(command)`, `setTheme`, `dispose`).
- [x] (插件) `NodeStore` 不可变树与补丁应用 (setProps / insert / remove / move / replaceSlot), 全部校验失败整批拒绝并抛契约异常 (宿主转为 `ComposeError`), 不留下半应用状态. P1.2 为正式宿主往返提前落地, 6 项 JVM 测试覆盖增删 / 跨父移动 / 插槽 / 晚失败回滚 / 环与作用域拒绝, 详见 `docs/dev/p1-session-evidence.md`.
- [ ] (插件) `RenderNode` 分派, `key(nodeId)`, 未知类型 -> 占位 `Text("<unknown: type>")` 仅 debug 构建, release 由宿主提前拒绝.
- [ ] (测试) JVM: `NodeStoreTest` (补丁应用与拒绝), `ThemeMapperTest` / `ValueMapperTest` (颜色解析 `#RRGGBB` / `#AARRGGBB` / 命名色, 尺寸 dp / sp, 文本样式); instrumentation: 空树 / 单节点树装配.

### P2.2 布局与基础组件

- [ ] (插件) Column / Row / Box / Spacer (含 `verticalArrangement` / `horizontalAlignment` / `spacing` 等), Surface / Card / HorizontalDivider, Text (样式, 对齐, 省略, 最大行数, 可选择), Icon (material-icons-core 名称表 + `ImageVector` 映射), Image (BitmapRef / contentScale / contentDescription).
- [ ] (测试) instrumentation 用 `onNodeWithTag` 断言布局与属性; 目录属性与实现逐项对照 (`CatalogCoverageTest`: 渲染器声明的 COMPONENTS 与实现的 `when` 分支一致).

### P2.3 交互组件

- [ ] (插件) Button / ElevatedButton / FilledTonalButton / OutlinedButton / TextButton / IconButton (`onClick`, `enabled`, 内容插槽), Switch / Checkbox / RadioButton (`checked` 受控 + `onCheckedChange`), Slider (`value` / `range` / `steps` / `onValueChange` / `onValueChangeFinished`).
- [ ] (测试) 事件 payload 与 generation 经宿主 `ComposeSession` 回到调度器; 受控组件在脚本未回写时保持旧值 (受控语义), 文档注明.

### P2.4 输入框

- [ ] (插件) TextField / OutlinedTextField: `TextFieldState` 原生编辑态 (D15), `onValueChange(text, selection, editSeq)` 节流为每帧一次, `edit` 命令按 `editSeq` 仲裁, `label` / `placeholder` / `leadingIcon` / `trailingIcon` / `supportingText` 插槽, `singleLine` / `maxLines` / `keyboardType` / `imeAction` / `isError` / `readOnly` / `visualTransformation(password)`.
- [ ] (测试) instrumentation: 中文输入法组合 (API 33 真机 Gboard / 小米输入法), 连续删除, 粘贴, 光标移动, 脚本回写与用户输入竞争 (旧 editSeq 被拒), IME 弹出时 Scaffold 内容避让 (与宿主 Activity Insets 处理的分工在此定稿, 不双重 padding).

### P2.5 列表, 脚手架与提示

- [ ] (插件) LazyColumn / LazyRow (子节点 key 映射 `items(key)`, `contentPadding`, `spacing`, `scrollTo` 命令, `onScroll` 事件可选节流), Scaffold (topBar / snackbarHost / floatingActionButton 插槽为 1.1 预留, 1.0.0 实现 topBar + content + snackbarHost), TopAppBar (title / navigationIcon / actions 插槽), AlertDialog (`open` 受控, title / text / confirm / dismiss 插槽, `onDismissRequest`), Snackbar (`session.showSnackbar(message, options)` 命令 -> `SnackbarHostState`, `onAction` / `onDismiss` 事件), CircularProgressIndicator / LinearProgressIndicator (确定 / 不确定).
- [ ] (测试) 1000 项 LazyColumn 的补丁应用耗时与滚动帧时间 (记录, 不预设阈值; P5.3 定阈值); 列表重排 (key 移动) 保留条目内 Switch 状态.

### P2.6 Modifier 链与快捷属性

- [ ] (插件) `ModifierMapper`: padding / size / width / height / fillMaxWidth / fillMaxHeight / fillMaxSize / weight (RowScope / ColumnScope) / align (BoxScope) / background (色 + 形状) / border / clip (RoundedCorner / Circle) / alpha / clickable / verticalScroll / horizontalScroll / offset / aspectRatio / testTag / semantics(contentDescription); 顺序严格按 ops 序列组合; 作用域操作出现在错误父节点时宿主已拒绝, 渲染器再次防御.
- [ ] (测试) JVM `ModifierMapperTest` 对每个 op 的参数解析; instrumentation 对 `padding -> background` 与 `background -> padding` 的像素 / 语义差异断言.

---

## P3: 脚本 API `compose`

目标: 宿主 `compose` 全局对象完整可用, ui 模式与悬浮窗两种承载, 节点句柄层与 render 层共用管线.

### P3.1 节点句柄层

- [ ] (宿主) `Compose : Augmentable(), Invokable` (`AugmentableKey("compose")`, 别名 `$compose`), 注册于 `ScriptRuntime` (在 `PluginAutoMountManager.apply` 之前); `compose.isAvailable()`, `compose.version` (插件 / 契约 / Compose 版本, 不可用时 `null`), `compose.createElement(type, props, ...children)` 与按目录生成的元素工厂 `compose.Column / Text / ...` (名称表来自 `ComponentCatalog.V1`, 保证 d.ts 与运行时一致).
- [ ] (宿主) `ComposeNode` 句柄: `type` / `key` / `nodeId` / `parent` / `children`; `set(props)` 与目录声明属性的代理读写 (`node.text = '...'`), `get(name)`, `append / insert / remove / replace / clear`, `slot(name, node)`, `on(event, fn) / off`, 命令 `focus() / blur() / scrollTo(index | { offset }) / edit({ text, selection })`; 未挂载的节点上操作只改本地状态, 挂载后每次操作进入当前批; 跨会话移动节点 -> `NODE_DETACHED` / `INVALID_ARGUMENT`.
- [ ] (宿主) 值转换 `ComposeScriptValues`: 颜色 (与 `colors` 模块一致的解析), 尺寸 (数字默认 dp, 字符串 `16sp` / `8dp`), 文本样式对象, 图片 (`ImageWrapper` / 路径 / `@drawable` 名), 回调 (注册为 callbackId), 子节点 (数组 / 单节点 / 字符串 -> Text 简写); 未知属性 -> `UNKNOWN_PROP`, 类型错误 -> `INVALID_ARGUMENT` (消息含组件名与属性名).
- [ ] (测试) JVM: 元素工厂与目录一致性, 值转换边界 (非法颜色, 负尺寸, 超长字符串, Unicode 补充平面), 代理属性读写, 子节点操作产生的补丁序列.

### P3.2 state + render 层

- [ ] (宿主) `compose.state(initial)` -> `{ value }` 对象: getter 在 render 期间登记依赖, setter 标记脏并在当前调度器 tick 末尾合并调度一次 render (`compose.batch(fn)` 显式合并); `compose.mount(render, options)` 执行 render 得到节点树, 经 `TreeReconciler` 产出补丁; render 抛错 -> `RENDER_FAILED` (含脚本堆栈), 保留上一版界面; render 返回非节点 -> `INVALID_ARGUMENT`.
- [ ] (宿主) render 模式下的节点身份: 同一 render 内 `key` 重复 -> `DUPLICATE_KEY`; 缺 key 的同类兄弟按索引匹配并 warn 一次 (Q3); `compose.ref()` 在 render 中以 `ref` 属性绑定节点, 下一 tick 起 `ref.current` 指向复用后的句柄.
- [ ] (宿主) `compose.post(fn)` (D12), `compose.theme(spec)` (会话级或全局默认).
- [ ] (测试) JVM: 依赖收集与合并调度 (10 次 setter 只触发 1 次 render), 条件分支切换的依赖重算, render 异常回滚, ref 复用; 与节点句柄层混用 (render 返回的树里包含预先创建并手动修改的节点).

### P3.3 ui 模式承载

- [ ] (宿主) `compose.mount(nodeOrRender, options)` 在 `"ui";` 脚本中: 取当前 Activity (无 Activity -> `UI_MODE_REQUIRED`), 创建会话并把渲染器 `view()` 经既有 `setContentViewRhinoRuntime` 路径设为内容 (`ui.view` 指向该 View), 返回 `ComposeSession` 对象 (`root` / `update(fn)` / `post(fn)` / `close()` / `on('close')` / `showSnackbar`); 与 `ui.layout` 二选一, 后者覆盖前者时关闭会话并 warn; Activity `back_pressed` 等事件沿用 `ui` 既有 emitter.
- [ ] (宿主) Insets 分工定稿 (P2.4 结论): 保持 Activity 对 `android.R.id.content` 的系统栏 padding, Compose 侧不再 `systemBarsPadding`; IME 由 Compose `imePadding` 或 Activity `adjustResize` 二选一并写入文档.
- [ ] (宿主) 生命周期: `ScriptExecuteActivity.onDestroy` -> 引擎销毁 -> 会话 `close` (释放 Composition, 注册表, 队列); 停止脚本 (强制 / 正常) 同样触发; 重复 `mount` 先关闭旧会话.
- [ ] (测试) instrumentation (API 33 真机 + API 24 AVD): 计数器, 表单 (TextField + Switch + Button), 1000 项列表三个脚本经 `RunIntentActivity` 运行并以 uiautomator 断言; 反复打开关闭 20 次无泄漏; 停止脚本后无残留回调.

### P3.4 悬浮窗承载

- [ ] (宿主) `compose.floaty(nodeOrRender, options)`: `options.raw` 选择 `rawWindow` / 可调整窗口, 其它选项 (位置, 尺寸, 可触摸, 焦点) 透传; 权限检查复用 `floaty.ensurePermission` 语义 (`PERMISSION_REQUIRED`); 窗口 View 为渲染器 `view()`, owner 由 P1.2 的 `ComposeFloatyOwners` 提供; 返回 `ComposeFloatyWindow` (继承 `JsRawWindow` / `JsResizableWindow` 的 `setPosition / setSize / close / requestFocus / disableFocus / setTouchable` + `session`); 非 ui 脚本的调度器为脚本 looper, 脚本退出时 `floaty.closeAll` 既有路径关闭窗口与会话.
- [ ] (测试) 真机 (HyperOS, Sony API 31) + AVD: 非 ui 脚本创建 HUD 窗口, 工作线程经 `compose.post` 每秒更新文本, 点击按钮关闭; TextField 在悬浮窗内获得焦点并输入 (需 `requestFocus`); 脚本退出窗口消失.

### P3.5 错误, 探测与守卫

- [ ] (宿主) `ComposeError` (`code` / `message` / `nodeId` / `prop` / `cause`), 装载失败消息复用插件中心本地化字符串; 新增字符串 (`error_compose_ui_mode_required`, `error_compose_unknown_component` 等) 10 语言, 按 name 排序.
- [ ] (宿主) 守卫: 单引擎并发会话上限 (附录 B.5), 会话关闭后对句柄的操作 -> `SESSION_CLOSED`; 引擎退出时的全部清理有单元测试.
- [ ] (测试) JVM `ComposeJsErrorsTest` (code 映射, instanceof, 消息前缀); `ComposeSessionLifecycleTest`.

---

## P4: 示例, 无障碍与守卫

### P4.1 示例脚本

- [ ] (插件) `assets/examples/`: `counter.js` (state + render), `form.js` (节点句柄层, TextField / Switch / Slider / 校验), `list.js` (LazyColumn 1000 项, key 重排, scrollTo), `floaty-hud.js` (非 ui 脚本悬浮 HUD), `theme.js` (seed / dark / dynamicColor); `index.json` 列表; 每个示例头部注释说明前置条件.
- [ ] (宿主) 同步到 `app/src/main/assets-app/sample/` 的 Compose 分类 (核对现有分类命名后决定目录名), 示例目录索引更新.
- [ ] (测试) 示例在兼容矩阵至少两台设备运行通过, 作为 P5.1 回归集.

### P4.2 无障碍与选择器

- [ ] (宿主 / 插件) 核实 `testTag` 经 `testTagsAsResourceId` 暴露为 `viewIdResourceName` 的确切字符串, 宿主 `id()` / `idContains()` / `idMatches()` 对其匹配结果, `desc()` 对 `contentDescription` 的匹配, `text()` 对 Text 节点的匹配, `click()` 动作在 Button 语义节点上的可用性; 合并语义节点 (Button 内 Text) 的查找路径.
- [ ] (文档) 把规则写入 `api/compose.md` "无障碍与选择器" 节与协议文档; 不兼容之处如实记录 (例如 tag 不带包前缀, 与旧 View `id()` 的 `pkg:id/` 匹配不同).
- [ ] (测试) instrumentation: 在 ui 模式页面上用宿主 a11y 服务 (`Three Adapt A11y` 或内置) 以 `id('start_button').findOnce().click()` 触发 Compose Button 回调.

### P4.3 守卫测试补全

- [ ] (宿主) 目录 / d.ts / 文档三方一致性脚本 (P6 使用): 从 `ComponentCatalog.V1` 生成组件与属性清单, 与 `aj6-int-compose.d.ts` 和 `api/compose.md` 的组件表比对 (`build/tools/compose_catalog_check.py`, 位于宿主 `build/` 临时目录或 `.python/`).
- [ ] (插件) instrumentation 全集可在 API 24 x86 与 API 35 x86_64 模拟器上通过; 覆盖率记录.

---

## P5: 健壮性, 兼容矩阵, 性能与体积

### P5.1 健壮性

- [ ] (测试) 敌意输入: 5001 个节点, 深度 65, 64 KiB + 1 的字符串, 2001 ops 的批, 非法颜色 / 尺寸 / 枚举, 循环子节点引用, 在回调中 `close()` 会话, render 中再次 `mount`, 工作线程直接改 state (应报错或经 `post`), 插件在会话存活期间被卸载 / 更新 (加载器失效 -> `PLUGIN_UNAVAILABLE`, 无崩溃).
- [ ] (测试) 快速反复 mount / close 100 次, 停止脚本时 render 进行中, Activity 旋转 (引擎销毁语义), 低内存 (`am send-trim-memory`) 后界面仍可更新.
- [ ] (宿主 / 插件) 修复发现的问题并补回归测试; 结论记入 `docs/dev/p5-robustness-evidence.md`.

### P5.2 兼容矩阵

- [ ] (测试) D28 六台设备 / 模拟器: 装载, 计数器, 表单 (含 IME), 列表, 悬浮窗, 停止清理; API 24 的 Compose 1.12 行为 (minSdk 21 以上, 预期可用) 与 API 37 (16 KB 页, 纯 bytecode 应无影响) 记录; HyperOS 悬浮窗焦点条件记录.
- [ ] (测试) inrt 打包 (D30): 打包一个使用 `compose` 的脚本应用, 在已安装 / 未安装插件的设备上运行, 错误提示可理解; 结果写入文档 "打包应用" 节.

### P5.3 性能与体积

- [ ] (测试) 度量并记录 (不预先承诺): 装载首帧 (冷 / 热), 单次 state 更新到界面可见 (ui 模式与 floaty), 100 次连续更新合并后的 render 次数与耗时, 1000 项 LazyColumn 首帧与滚动掉帧, 宿主 PSS 增量, 插件 APK 体积与 dex 方法数; 以此定 1.0.0 的回归阈值写入插件 `AGENTS.md` 第 14 节.
- [ ] (宿主 / 插件) 若 render 整树重建成为瓶颈, 先做宿主侧节点属性快照比较 (跳过无变化子树的 ops), 不改脚本 API; 记录是否需要 F.6 的细粒度更新.

---

## P6: 文档, 声明与 1.0.0 本地 gate

### P6.1 文档

- [ ] (文档) `AutoJs6-Documentation/api/compose.md` (模块总览, 两层范式, 线程, 承载面, 主题, 无障碍, 打包应用, 错误) 与类型页 `composeNodeType.md`, `composeStateType.md`, `composeModifierType.md`, `composeSessionType.md`, `composeFloatyWindowType.md`, `composeThemeType.md`, `composeComponents.md` (核心集逐组件属性 / 插槽 / 事件表, 由 P4.3 脚本生成初稿); `sidebar.md` / `toc.md` / `dataTypes.md` / `progress.md` 更新; 运行 `generator/auto-generate-for-autojs6.bat`, 同步 `AutoJs6-Plugin-Offline-Docs`, 按各仓库 AGENTS 提交.

### P6.2 声明

- [ ] (文档) `AutoJs6-TypeScript-Declarations/declarations/autojs6/aj6-int-compose.d.ts` (`Internal.Compose`, 元素工厂重载按目录生成, `ComposeNode` / `ComposeState<T>` / `ComposeModifier` / `ComposeSession` / `ComposeFloatyWindow` / `ComposeError`, 事件 payload 类型), `index.d.ts` 引用; `aj6dts.bat -Publish` 后同步两个仓库的 `aj6-int-compose.d.ts`, Ace 仓库执行 `:app:generateAutoJs6LspDeclarations`, 版本号与版本名按 AGENTS 规则加一并提交.

### P6.3 README 与 changelog

- [ ] (插件) `.readme` 10 语言: 简介, 功能 (两层范式, 承载面, 核心集), 安装 (插件中心), 快速开始 (计数器 + 悬浮 HUD), 兼容性 (宿主最低版本, Android 7.0+, 打包应用需安装插件), 常见问题 (为什么不能直接调用 Compose 函数, 旋转后状态, 选择器 tag 规则), 发行历史, 许可证; `.changelog` 1.0.0 条目; 生成并 `--check`.
- [ ] (宿主) `.changelog` 条目最终化 (P1.3 的条目补齐 floaty / 主题 / 示例); 协议文档状态改为 "frozen V1 with host commit ...".

### P6.4 本地发布 gate

- [ ] (发布) 插件: `py .python/generate_markdown.py --check`, `testDebugUnitTest`, `assembleDebug assembleDebugAndroidTest lintDebug`, `connectedDebugAndroidTest` (至少一台), `appendDigestToReleasedFiles` 产出 1 个已签名 APK 且 CRC32 一致; 宿主: 相关 JVM 测试 + `assembleDebug`; 全部结果与未执行项写入 `docs/dev/p6-release-gate.md`.
- [ ] (发布) 按 D7 不推送, 不登记索引, 不发 Release; 在本文件 "会话记录" 写明 gate 达成的提交 hash, 等待维护者恢复推送后再做远端步骤 (登记 `official-repositories.json`, Release, 索引生成).

---

## P7: 1.1.0 候选

以下条目在 1.0.0 gate 后由维护者排序 (Q7), 详细说明见附录 F.

- [ ] (宿主 / 文档) F.1 TSX 工厂: `__autojs6Tsx` 按 `type` 路由到 `compose.createElement`, `aj6-jsx-element-extension.d.ts` 增补 Compose 元素类型, TypeScript Engine 插件配合.
- [ ] (宿主 / 插件) F.2 与旧 ui 混合: XML `<compose>` 容器 (`ui.registerWidget` 同机制) 与 Compose `AndroidView` 节点 (以 `ui.inflate` 结果或 View 工厂作为 `view` 属性, 主线程创建).
- [ ] (宿主 / 插件) F.3 `compose.dialog(nodeOrRender, options)`: 任意脚本弹出 Material 3 Dialog / ModalBottomSheet, 返回 Promise 或会话.
- [ ] (插件 / 宿主 / 文档) F.4 宽集组件, 契约 `CONTRACT_VERSION = 2` (只追加).
- [ ] (插件) F.5 组件画廊 Activity (若排期, 需同时遵循独立设置页与图标规范).
- [ ] (宿主) F.6 细粒度更新与脚本侧依赖裁剪 (按 P5.3 数据决定).

---

## 附录 A: 脚本 API 草案

### A.1 全局对象

| 成员 | 说明 |
| --- | --- |
| `compose.isAvailable()` | 插件已安装, 启用, 授权且版本兼容时 `true`; 不抛错 |
| `compose.version` | `{ plugin: string, contract: number, compose: string } \| null` |
| `compose.createElement(type, props?, ...children)` | 通用工厂 (F.1 TSX 接点); `type` 为目录组件名 |
| `compose.Column(props?, children?)` 等 | 按 `ComponentCatalog.V1` 生成的元素工厂, 返回 `ComposeNode`; `children` 接受数组 / 单节点 / 字符串 (Text 简写) |
| `compose.mount(nodeOrRender, options?)` | ui 模式: 设为 Activity 内容, 返回 `ComposeSession`; 非 ui 脚本 -> `UI_MODE_REQUIRED` |
| `compose.floaty(nodeOrRender, options?)` | 悬浮窗会话, 返回 `ComposeFloatyWindow`; `options`: `raw` / `x` / `y` / `width` / `height` / `touchable` / `focusable` / `theme` |
| `compose.state(initial)` | 响应式状态 `{ value }` |
| `compose.batch(fn)` | 合并多次 state 变更为一次 render |
| `compose.ref()` | `{ current: ComposeNode \| null }`, 经节点 `ref` 属性绑定 |
| `compose.modifier()` | 链式 Modifier 构造器 |
| `compose.theme(spec?)` | 设置 / 读取默认主题 (`ThemeSpec` 形态见 A.6) |
| `compose.post(fn, delay?)` | 投递到当前 (或最近) 会话的脚本调度器 |
| `compose.sessions` | 当前引擎存活会话数组 (只读) |
| `ComposeError` | 全局错误类, `code` / `message` / `nodeId` / `prop` |

### A.2 节点句柄 `ComposeNode`

```text
node.type            组件名 (只读)        node.key             key (只读)
node.nodeId          会话内 id, 未挂载为 -1
node.parent / node.children (只读快照)
node.set({...})      批量设置属性          node.get(name)
node.text = '...'    目录声明属性的代理读写 (props 与 modifier 快捷属性)
node.append(child) / insert(index, child) / remove(child | index) / replace([...]) / clear()
node.slot(name, child | null)             命名插槽 (title / actions / label / leadingIcon ...)
node.on(event, fn) / node.off(event, fn?)  事件 (click / longClick / valueChange / checkedChange / dismiss / scroll / focusChange ...)
node.focus() / blur()                     命令
node.scrollTo(index | { offset })          列表 / 可滚动节点
node.edit({ text?, selection? })          TextField 编辑命令 (带 editSeq 仲裁)
node.testTag / node.contentDescription    无障碍属性 (也可在 props 中给出)
```

未挂载时操作只改本地状态; 挂载后每次操作加入当前批, 在脚本调度器 tick 末尾提交.

### A.3 state 与 render

```js
"ui";

let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `已点击 ${count.value} 次`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, '加一'),
]));
```

- `state.value` 读取在 render 期间登记依赖, 写入标记脏并合并调度; 同一 tick 多次写入只 render 一次.
- render 函数必须返回单个 `ComposeNode`; 可以混入预先创建的节点句柄 (保留其 nodeId).
- 动态列表项应给稳定 `key`; 缺省按同类索引匹配并 warn 一次 (Q3).

### A.4 Modifier

```js
compose.modifier()
    .fillMaxWidth().height(56).padding(8, 16)        // 尺寸与内边距 (数字 = dp, 字符串可带单位)
    .background('#FFFFFF', { shape: 'rounded', radius: 12 })
    .border(1, '#E0E0E0', { shape: 'rounded', radius: 12 })
    .clip('circle').alpha(0.9).clickable(fn)
    .weight(1)              // 仅 Column / Row 子节点 (SCOPE_MISMATCH 否则)
    .align('center')        // 仅 Box 子节点
    .verticalScroll().testTag('card');
```

快捷属性: `w` / `h` / `padding` / `bg` / `weight` / `alpha` / `testTag` 等价于链首操作; 与显式 `modifier` 同时存在时快捷属性在前.

### A.5 组件核心集 (1.0.0)

| 组 | 组件 | 关键属性 / 插槽 / 事件 |
| --- | --- | --- |
| 布局 (7) | Column, Row, Box, Spacer, LazyColumn, LazyRow, Scaffold | `arrangement` / `alignment` / `spacing`; Lazy: `contentPadding`, 子节点 `key`, `scrollTo`, `scroll` 事件; Scaffold: `topBar` 插槽, `snackbar` |
| 容器 / 顶栏 (4) | Surface, Card, HorizontalDivider, TopAppBar | `color` / `tonalElevation` / `shape` / `elevation` / `thickness` |
| 文本与图像 (3) | Text, Icon, Image | Text: `text` / `style` (M3 排版名) / `color` / `fontSize` / `fontWeight` / `textAlign` / `maxLines` / `overflow` / `selectable`; Icon: `name` (icons-core) / `tint`; Image: `src` / `contentScale` / `contentDescription` |
| 按钮 (6) | Button, ElevatedButton, FilledTonalButton, OutlinedButton, TextButton, IconButton | `enabled`, `onClick`, 内容插槽 (字符串简写) |
| 选择与输入 (6) | Switch, Checkbox, RadioButton, Slider, TextField, OutlinedTextField | 受控 `checked` / `value` + `onCheckedChange` / `onValueChange` (+ `onValueChangeFinished`); TextField 见 D15 |
| 进度 (2) | CircularProgressIndicator, LinearProgressIndicator | `progress` (缺省不确定) / `color` / `trackColor` |
| 对话框与提示 (2) | AlertDialog, Snackbar (经 `session.showSnackbar`) | AlertDialog: `open` / `title` / `text` / `confirm` / `dismiss` 插槽 / `onDismissRequest`; Snackbar: `message` / `actionLabel` / `duration` / `onAction` / `onDismiss` |

### A.6 会话, 悬浮窗与主题

```text
ComposeSession:   root (ComposeNode) / update(fn) / post(fn) / showSnackbar(message, options) / close() / isClosed() / on('close', fn)
ComposeFloatyWindow extends ComposeSession 能力: setPosition / setSize / getX / getY / getWidth / getHeight / requestFocus / disableFocus / setTouchable / close
ThemeSpec: { seed?: color, colors?: { primary, onPrimary, ... }, dark?: boolean | 'system', dynamicColor?: boolean, typography?: { fontFamily?, scale? } }
```

### A.7 错误

`ComposeError.code` 取附录 B.4 词汇; 装载类错误消息以插件中心既有本地化提示开头; `retryable` 对 `PLUGIN_*` 为 `true`.

### A.8 示例 (节点句柄层 + 悬浮窗)

```js
let status = compose.Text({ text: '准备中...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, '关闭'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `进度 ${i}%` }));
    }
});
```

---

## 附录 B: V1 契约 (`plugin-api/compose-ui-api`)

### B.1 装载面

P0.3 定稿约束: `hostContext` 保留宿主 theme / window / system services, resources / assets 使用与宿主 Configuration 对齐的插件资源, classLoader 使用含原生路径的 parent-first 加载器. 悬浮窗 owner 由宿主提供并随窗口关闭销毁. P1.1 冻结 `.loading` / `.model` / `.catalog` 与根命名常量, 细则见 `docs/dev/compose-ui-plugin-protocol-v1.md`. `api.spike` 版本 -1 只为既有 P0 回归保留, 不属于 V1 冻结面; `classOrigins` / `probe` / `diagnostics` 不进入正式装载面.

```kotlin
interface ComposeUiRendererFactory {
    fun contractVersion(): Int
    fun capabilities(): Bundle                 // CONTRACT_VERSION, COMPONENTS, FEATURES, COMPOSE_VERSION, SHARED_DEPS_FINGERPRINT
    fun create(environment: ComposeUiHostEnvironment): ComposeUiRenderer
}
interface ComposeUiHostEnvironment {
    val hostContext: Context                   // ComposeHostContext (D11)
    val mainExecutor: Executor
    val eventSink: ComposeUiEventSink
    val initialTheme: ThemeSpec
    val sessionId: Int
}
interface ComposeUiRenderer {
    fun view(): View                           // 懒创建的 ComposeView
    fun apply(batch: UiPatchBatch)             // 主线程; 整批成功或整批拒绝 (抛 ComposeUiContractException(code))
    fun execute(command: UiCommand)            // focus / blur / scrollTo / edit / showSnackbar
    fun setTheme(theme: ThemeSpec)
    fun dispose()
}
fun interface ComposeUiEventSink { fun enqueue(event: UiEvent) }
```

工厂实现类由 Manifest meta-data `org.autojs.plugin.compose.RENDERER_FACTORY` 指定, 必须有公开无参构造器.

### B.2 数据模型

| 类型 | 字段 |
| --- | --- |
| `UiNode` | `nodeId: Int`, `type: String`, `key: String?`, `props: Map<String, UiValue>`, `modifier: List<ModifierOp>`, `children: List<Int>`, `slots: Map<String, Int>`, `callbacks: Map<String, Int>` (事件名 -> callbackId) |
| `UiTree` | `rootId: Int`, `nodes: List<UiNode>`, 闭合子树; 校验引用, 唯一所有权, 孤立节点, 循环, 深度与兄弟 key |
| `UiPatch` | `SetProps(nodeId, props, modifier?, callbacks?)`, `Insert(parentId, index, subtree: UiTree)`, `Remove(parentId, nodeId)`, `Move(parentId, nodeId, toIndex)`, `ReplaceSlot(parentId, slot, subtree: UiTree?)`, `SetRoot(tree: UiTree)` |
| `UiPatchBatch` | `sessionId: Int`, `generation: Long`, `patches: List<UiPatch>` |
| `UiEvent` | `sessionId: Int`, `generation: Long`, `nodeId`, `type`, `callbackId`, `payload: Bundle` (valueChange: `text` / `selectionStart` / `selectionEnd` / `editSeq`; checkedChange: `checked`; slider: `value`; scroll: `firstVisibleIndex` / `offset`) |
| `UiCommand` | `Focus(nodeId)`, `Blur(nodeId)`, `ScrollTo(nodeId, index?, offset?)`, `Edit(nodeId, text?, selection?, editSeq)`, `ShowSnackbar(message, actionLabel?, duration, callbackId?)` |
| `UiValue` | `Str`, `Num`, `Bool`, `Color(argb)`, `Dp(v)`, `Sp(v)`, `Enum(name)`, `TextStyle(...)`, `BitmapRef(bitmap)`, `IconName(name)`, `Shape(kind, radius)`, `ListOf(values)`, `Null` |
| `ModifierOp` | `name: String`, `args: List<UiValue>`, `scope: ScopeKind` (ANY / COLUMN_ROW / BOX) |
| `ThemeSpec` | `seedArgb?`, `colorOverrides: Map<String, Int>`, `dark: Boolean?`, `dynamicColor: Boolean`, `fontFamily?`, `fontScale?` |

全部为不可变 Kotlin 值模型并实现 `Parcelable`; 事件 Bundle 输入与读取均复制, 仅允许标量. `BitmapRef` 借用脚本位图, 以进程 nonce + 弱句柄进行同进程 Parcel 往返, 不复制 / 回收像素; 外进程 nonce 或失效所有者被类型化拒绝, 未来进程外退路需另定图片传输. 1.0.0 不经 Binder. `SetProps.props` 完整替换, null modifier / callbacks 表示保留, 空集合表示清空; 命令与批次语义见协议文档.

### B.3 组件目录格式

`ComponentCatalog.V1.components: List<ComponentSpec>` (30 项, 29 个节点组件 + 仅命令的 Snackbar); `ComponentSpec(name, props: List<PropSpec>, slots: List<SlotSpec>, events: List<EventSpec>, childrenPolicy: NONE / SINGLE / MANY / LAZY_ITEMS, scope: ScopeKind)`; `PropSpec(name, type: UiValue 种类, required, default?, enumValues?)`; `EventSpec(name, payloadFields)`. 渲染器 capabilities 的 `COMPONENTS` 必须是目录名称子集; 宿主对不在渲染器集合内的组件抛 `UNKNOWN_COMPONENT`.

### B.4 错误码

| 代码 | 含义 |
| --- | --- |
| `PLUGIN_UNAVAILABLE` / `PLUGIN_DISABLED` / `PLUGIN_UNAUTHORIZED` / `PLUGIN_INCOMPATIBLE` | 装载门禁 (D18) |
| `UI_MODE_REQUIRED` | `compose.mount` 在非 ui 脚本或 Activity 不可用时 |
| `PERMISSION_REQUIRED` | 悬浮窗权限缺失 |
| `INVALID_ARGUMENT` / `UNKNOWN_COMPONENT` / `UNKNOWN_PROP` / `INVALID_MODIFIER` / `SCOPE_MISMATCH` / `DUPLICATE_KEY` | 构树与校验 |
| `LIMIT_EXCEEDED` | 附录 B.5 任一上限 |
| `RENDER_FAILED` | render 函数抛错 (消息含脚本堆栈) |
| `SESSION_CLOSED` / `NODE_DETACHED` | 会话或节点生命周期 |
| `RENDERER_FAILED` | 渲染器 apply / execute 抛出 (含插件侧消息) |
| `INTERNAL` | 其它 |

### B.5 上限常量 (D17, 写入 `ComposeUiLimits`)

| 常量 | 值 |
| --- | --- |
| `MAX_NODES_PER_SESSION` | 5000 |
| `MAX_DEPTH` | 64 |
| `MAX_PATCHES_PER_BATCH` | 2000 |
| `MAX_STRING_CHARS` | 65536 |
| `MAX_CHILDREN_PER_NODE` | 2000 (LazyColumn / LazyRow 10000) |
| `MAX_MODIFIER_OPS` | 64 |
| `MAX_CALLBACKS_PER_SESSION` | 4096 |
| `MAX_SESSIONS_PER_ENGINE` | 8 (ui 内容 1 + 悬浮窗 7) |
| `MAX_BITMAP_PIXELS` | 32,000,000 (十进制像素数) |
| `EVENT_QUEUE_CAPACITY` | 1024 (溢出丢弃最旧并 warn) |

P1.1 补充容器边界: `MAX_VALUE_DEPTH=64`, `MAX_VALUE_ITEMS=2000`, `MAX_PROPERTIES_PER_NODE=128`, `MAX_SLOTS_PER_NODE=32`, `MAX_EVENT_FIELDS=32`, `MAX_THEME_COLORS=64`, `MAX_PARCEL_BYTES=8 MiB` (仅序列化帧, 不限制无序列化的普通进程内调用). Lazy 子节点上限不会覆盖整个会话的 5000 节点上限.

### B.6 版本协商

- 宿主读取 `capabilities()` 的 `CONTRACT_VERSION`, 要求落在 `[ComposeUiContract.MIN_SUPPORTED, CONTRACT_VERSION]`; 插件 Manifest `requiresHostVersion` 由宿主比较 `VERSION_BUILD`.
- `SHARED_DEPS_FINGERPRINT` (D26): 插件编译时宿主共享依赖表的 SHA-256; 宿主计算自身表的指纹, 不一致 -> `PLUGIN_INCOMPATIBLE` (消息指出需要升级的一方).
- 新增组件 / 属性 / 事件 / 命令只追加目录与 capabilities, `CONTRACT_VERSION` 加一; 旧宿主对未知组件按 `UNKNOWN_COMPONENT` 拒绝, 旧插件对未知属性由宿主目录提前拒绝.

---

## 附录 C: 宿主改动清单 (按文件)

| 文件 / 目录 | 改动 | 阶段 |
| --- | --- | --- |
| `settings.gradle.kts` | `pluginApi += "compose-ui-api"` | P1.1 |
| `app/build.gradle.kts` | `implementation(project(":plugin-api:compose-ui-api"))` | P1.1 |
| `plugin-api/compose-ui-api/**` | 新模块 (4.3) | P1.1 |
| `app/src/main/java/org/autojs/autojs/core/plugin/compose/**` | 装载器, 会话, 差分, 注册表, 主题桥, 错误 | P1.2 |
| `app/src/main/java/org/autojs/autojs/runtime/api/augment/compose/**`, `runtime/api/compose/**` | 脚本 API | P3 |
| `app/src/main/java/org/autojs/autojs/runtime/ScriptRuntime.kt` | `Compose.augmentWithRuntime(target, this)`; 引擎退出时关闭会话 | P3.1 |
| `app/src/main/java/org/autojs/autojs/runtime/api/Floaty.kt` | 复用 `rawWindow(view)` / `window(view)`; 如需 owner 钩子增加 `ComposeFloatyOwners` 接线 | P3.4 |
| `app/src/main/java/org/autojs/autojs/core/plugin/center/wizard/PluginInstallWizardCatalog.kt` | `entry(official("compose.ui"), "Compose UI", UI)` | P1.3 |
| `app/src/main/AndroidManifest.xml` | 如 INFO 发现不足以可见则 `<queries>` 加包名 | P1.3 |
| `app/src/main/res/values*/strings.xml` | `error_compose_*` 10 语言 | P3.5 |
| `app/src/main/assets-app/sample/<Compose 分类>/` | 示例 | P4.1 |
| `.changelog/lang_*.json` | feature / hint | P1.3, P6.3 |
| `docs/dev/compose-ui-plugin-protocol-v1.md`, `compose-ui-spike-evidence.md` | 协议与证据 | P0.3, P1.3 |
| `app/src/test/**` | `ComposeUiContractTest`, `ComponentCatalogConsistencyTest`, `ComposeUiHostCompileGuardTest`, `TreeReconcilerTest`, `ComposeSessionLimitsTest`, `CallbackRegistryTest`, `ComposeUiPluginHostSelectionTest`, `ComposeUiSharedClasspathTest`, `ComposeJsErrorsTest`, `PluginCenterComposeUiRegistrationTest` | P1 - P3 |
| `app/src/androidTest/**` | `ComposeUiLoaderTest`, ui 模式 / 悬浮窗 端到端 | P1.2, P3 |

不改动: `runtime/api/augment/ui/UI.kt` 的对象模型 (`ui.view` 仍为 `View`), `com/stardust/**`, 旧 `DynamicLayoutInflater`.

---

## 附录 D: 待决事项

### Q1 (P0.3 前): parent-first 加载不可行时的取舍

**已拍板 (2026-10-02)**: 维护者选择 (b), 正式采用 lifecycle 2.9.4 / savedstate 1.3.2 / emoji2 1.4.0 / window 1.5.0. 维持 D10 parent-first, 不启用隔离 classloader 或进程外退路.

若 P0.2 发现宿主 AndroidX 版本低于 Compose 1.12 传递依赖的下界且不可升级, 或共享 classloader 产生不可解的 `LinkageError`: (a) 启用附录 E.3 (隔离加载 + 插件自持 owner + 资源委托, 宿主内存多一份 AndroidX 副本), (b) 升级宿主 AndroidX 到满足下界的版本 (影响宿主其它模块, 需回归), (c) 改进程外 (E.2). 推荐 (b) 优先, 其次 (a).

### Q2 (P2.2 前): 图标集范围

`material-icons-core` (约 50 个常用图标, 体积小) 还是 `material-icons-extended` (约 2000 个, APK 增加数 MB, R8 可裁剪未引用项但名称表驱动的动态查找会阻止裁剪). 推荐 core + 允许 `Image` 以 `ImageWrapper` 自带图标; extended 列入 F.4 评估.

### Q3 (P3.2 前): 缺 key 的动态列表项

按索引匹配并每会话 warn 一次 (推荐), 还是直接 `INVALID_ARGUMENT`. 影响 render 层的容错与文档措辞.

### Q4 (P3.4 前): 悬浮窗 API 归属

`compose.floaty(...)` (推荐, 与 `compose.mount` 对称, 文档集中) 还是 `floaty.compose(...)` (与 `floaty.window` / `rawWindow` 并列). 两者只能选一, 不做别名.

### Q5 (P5.3 后): 性能回归阈值

由 P5.3 实测数据提出 (首帧, 单次更新, 列表滚动, PSS, APK 体积), 维护者确认后写入插件 `AGENTS.md`.

### Q6 (P6.4 前): 启动器图标源图

维护者提供两张黑白透明 PNG (亮色模式用深色图案, 暗色模式用浅色图案) 以替换 P0.1 的临时图标; 无 launcher 入口, 只需基础 `mipmap*/ic_launcher.png` 系列.

### Q7 (P7 前): 1.1.0 顺序

F.1 TSX, F.2 与旧 ui 混合, F.3 `compose.dialog`, F.4 宽集, F.5 画廊, F.6 细粒度更新的先后.

---

## 附录 E: 证据等级, 设备池与退路

### E.1 证据等级

| 等级 | 含义 |
| --- | --- |
| E0 | 仅静态检查 (grep / 代码审阅), 不得用于勾选功能条目 |
| E1 | JVM 单元测试通过 (测试类名 + 提交 hash) |
| E2 | 模拟器 instrumentation 或脚本 smoke 通过 (AVD API 级别 + 提交 hash) |
| E3 | 真机通过 (型号 + API + 宿主 build + 插件 build) |
| E4 | 兼容矩阵全部设备通过, 或明确记录未覆盖设备与原因 |

功能条目至少 E2, 涉及 IME / 悬浮窗 / 无障碍 / 性能的条目至少 E3; 未执行的验证在条目后如实写 "未执行: 原因".

设备池: D28 六台; 多台设备时用明确 serial, 每次会话重新读取 SDK 与宿主版本; 不卸载用户已安装应用, 测试脚本放 `/sdcard/Android/data/<宿主包>/` 或临时目录; 既有记录: Xiaomi Pad 两栏设置, HyperOS 悬浮窗焦点, API <= 28 应用切换锁, 新安装需先授予存储权限才能经 `RunIntentActivity` 读取脚本.

### E.2 退路: 进程外插件 Activity

协议 (B.2) 与渲染器 (4.2 `renderer/`) 不变; 新增插件 `ComposeUiHostActivity` + Binder `IComposeUiSession` (`apply(batch)` / `execute(command)` / `setTheme`) + `IComposeUiCallback.onEvent` (oneway), `UiPatchBatch` 经 `Parcelable` 传输 (大批走 PFD); 宿主 `ComposeSession` 换传输实现. 代价: 独立任务与窗口, 不能嵌入 ui 模式 Activity, 悬浮窗需插件自持 `SYSTEM_ALERT_WINDOW`, 事件多一次 IPC. 只在 Q1 选 (c) 时启用.

### E.3 退路: 隔离 classloader + 插件自持 owner

使用 `createPackageContext(CONTEXT_INCLUDE_CODE)` 的隔离加载器 (或 parent = boot 的 `PathClassLoader`); 契约类型经 "契约 jar 由宿主注入到插件加载器的 parent" 的两级加载器解决 (`ContractClassLoader(parent = boot) <- PluginClassLoader`), 宿主用同一 `ContractClassLoader` 定义契约类; 插件自带 AndroidX 副本, 在 `ComposeView` 上 `setViewTreeLifecycleOwner` / `setViewTreeSavedStateRegistryOwner` 为插件自持的 owner (由宿主经契约 `ComposeUiHostEnvironment.lifecycleEvents` 推进), 并以 `WindowRecomposerPolicy.setFactory` 绕过内容子视图的 Lifecycle 查找. 代价: 宿主内存多一份 AndroidX, 契约加载器需在宿主启动早期建立.

---

## 附录 F: 预留 (1.1+)

### F.1 TSX 工厂

TypeScript Engine 插件已把 TSX 编译为 `__autojs6Tsx(type, props, ...children)`; 宿主在 `type` 为 Compose 组件名 (或 `compose.*` 引用) 时路由到 `compose.createElement`, 其余保持 XML 渲染; `aj6-jsx-element-extension.d.ts` 增补 `JSX.IntrinsicElements` 的 Compose 条目 (按目录生成); 新旧两套不可在同一棵树混用 (F.2 落地后经 `<compose>` 容器桥接).

### F.2 与旧 ui 混合

XML `<compose>` 容器: 经 `ui.registerWidget` 同机制注册, `id` 可被 `ui.xxx` 取到 (返回容器 View), 容器内容由 `compose.attach(ui.xxx, nodeOrRender)` 挂载; Compose `AndroidView` 节点: `view` 属性接受 `ui.inflate` 结果或 View 工厂 (主线程创建), 更新经 `node.set({ view })`; 文档明确 Compose 节点句柄不是 `android.view.View`.

### F.3 `compose.dialog`

`compose.dialog(nodeOrRender, { type: 'alert' | 'bottomSheet', cancelable, ... })` 在任意脚本中以 Material 3 Dialog / ModalBottomSheet 显示 (ui 模式用 Activity 窗口, 否则用悬浮窗类型), 返回会话; 与 `dialogs` 模块互相引用.

### F.4 宽集组件

NavigationBar / NavigationRail / NavigationDrawer / TabRow / ModalBottomSheet / DropdownMenu / DatePicker / TimePicker / HorizontalPager / LazyVerticalGrid / AssistChip / FilterChip / InputChip / Badge / SegmentedButton / FloatingActionButton / SearchBar / Tooltip / PullToRefresh; `material-icons-extended` 评估 (Q2); 契约 V2.

### F.5 组件画廊

插件内 Compose Activity 展示核心集与示例代码 (复制到剪贴板 / 发送给宿主运行); 若排期需遵循独立设置页与四 alias 图标规范.

### F.6 细粒度更新

脚本侧按 state 依赖只重跑受影响的 render 片段 (`compose.memo(fn, deps)`), 宿主侧属性快照比较; 按 P5.3 数据决定.

---

## 附录 G: 参考

- 宿主快照 `77b5a3b0c5` 的锚点见第 3 节; ImGui 插件 (`D:/idea-projects/AutoJs6-Plugin-ImGui`) 为进程内插件装载与 INFO 注册的现成实现; Readium EPUB Reader 插件为进程外 Activity + Binder 会话的实现 (E.2 参考); 3-Shell Terminal / Angus Mail 为路线图, AGENTS, 锁文件与 CI 的格式参考; 3-Setup Installer 为无原生库的骨架参考.
- 规范: `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md`, `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md`; `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 仅在 F.5 排期后适用.
- Compose: BOM 映射表 `https://developer.android.com/develop/ui/compose/bom`, 2026-08 发布说明 (Compose 1.12, compileSdk 37 / AGP 9.1.1+), 2026-09-09 BOM `2026.09.00`; 互操作 (`ComposeView` / `AndroidView`), `ViewCompositionStrategy`, 语义与 `testTagsAsResourceId`, Compose 编译器插件随 Kotlin 版本发布.
- 历史议题 _[`issue #84`](http://issues.autojs6.com/84)_.
- 许可证: 插件 MPL-2.0 (与宿主一致); Compose / AndroidX Apache-2.0; 宿主 AAR MPL-2.0.

---

## 会话记录

### 2026-10-02

- 维护者两轮拍板 D1-D8: 进程内渲染; 节点句柄 + render / state 两层都做; 仓库 `AutoJs6-Plugin-Compose-UI` 与全局对象 `compose`; 1.0.0 承载面为 ui 模式 Activity 内容与悬浮窗; 核心集组件; TSX 排 1.1; 仅本地提交; 无独立界面.
- 探查宿主 `77b5a3b0c5`: 进程内装载先例 (`plugins.load` / ImGui 注册表) 及其隔离加载器局限, ui 模式与 floaty 基建, classic TSX 契约, 插件中心注册点, 共享依赖版本; 据此派生 D9-D31 与附录 A-F.
- 只落盘本路线图; 目录未 `git init`; 下一会话从 P0.1 开始, P0.2 的 spike 结论决定是否触发 Q1.

### 2026-10-02 (P0.1)

- 完成 P0.1 仓库骨架, 6 笔本地提交 (e7efcea, aeb0af8, 9879baa, 6cc0d52, 657f021, 本提交), `VERSION_BUILD=6`; 平台链 Gradle 9.5.0 / AGP 9.3.2 / Kotlin 2.3.20, Compose BOM 2026.09.00 与 AGP 内置 Kotlin 下的 Compose 编译器插件均正常; 19 项 JVM 测试与 lint 0 问题; 设备契约测试在 Xiaomi Pad (API 35) 与 Sony G8441 (API 28) 真机通过.
- 发现并修正: Compose 传递依赖 `androidx.graphics:graphics-path` 1.0.1 带四个 ABI 的 `libandroidx.graphics.path.so`, D22 "无原生库" 改为 "无插件自有原生代码 + 单 APK 内置四种 ABI + 发布门禁校验 16 KB 对齐" (D22 批注, `AGENTS.md` 第 9 节); 需维护者确认该批注.
- 未做 / 延后: `compose-ui-api.aar` 与 D26 共享依赖锁 (P0.2), `requiresHostVersion=5308` 回填 (P1.3), 正式图标源图 (Q6), 模拟器矩阵 (CI / P0.2), `docs/dev/` 随 P0.3 证据文件创建.
- 下一会话从 P0.2 进程内装载 spike 开始 (宿主临时入口 + 最小渲染器 + 加载器验证清单); 加载器验证清单新增 "插件原生库搜索路径 `<apk>!/lib/<abi>`" 一项.


### 2026-10-02 (P0.2)

- P0.2 四项完成: 独立宿主 debug 入口, 三组件计数器, 共享依赖 / owner / 资源 / JNI / release R8 / PSS / 退出验证, 区分失败门禁. 原宿主存在其它会话改动, 因此所有宿主改动在独立 worktree `AutoJs6-ComposeUi-Spike` 的 `spike/compose-ui-p0` 分支, 未合入原 master.
- 设备: Redmi 22120RN86C API 33 arm64 (E3), API 37 16 KB x86_64 AVD (E2), 宿主 build 5309 / 插件 build 6; 各 5 项宿主测试在 debug 与 release 插件上通过, 独立进程 PSS 各 1 项. 插件 JVM 24 项通过. 详细数据与复现命令见 `docs/dev/p0-spike-evidence.md`.
- D26 发现: lifecycle / savedstate / emoji2 / window 下界超过宿主原解析版本, 只在 debug 实验中升级并建立 51 项锁. Kotlin 需供 INFO 独立进程使用, 保留打包且在 R8 中保持 ABI; R8 的 Kotlin 专用方法仍会破坏 parent-first, 因而 P0 保留裁剪和混淆但关闭优化; compileOnly lifecycle 的反射构造规则显式补齐. Q1 推荐升级宿主共享依赖, 待维护者确认后进入 P0.3.
- 未做: 正式宿主依赖升级与 loader 转正, V1 契约冻结 (P1.1), 最低正式宿主版本回填 (P1.3), minified 宿主 / 全设备矩阵 / TalkBack 手工验收 / LeakCanary (后续 gate), 正式图标 (Q6). `compose` 脚本 API 仍未交付, 10 语言状态文案明确只有专用测试宿主可验证计数器.
- 最终插件 build 7 的 release 在两台设备重新通过全部 5 项宿主探针. 本地签名门禁通过, 产物 `autojs6-plugin-compose-ui-v1.0.0-f7a437db.apk`; lint debug 0 问题, release 0 错误 / 3 个身份资源未使用警告. 下一会话从 P0.3 / Q1 开始. 本次只做本地提交, 不推送, 不发布.

- 提交定位: 宿主 `21dff98f26` (独立分支), 插件为本条所在提交 (`VERSION_BUILD=7`). 临时测试宿主 / 插件 / 两个 instrumentation 包均已从两台测试设备移除, 原宿主保留 (Redmi 5310, AVD 5304); 本次启动的 API 37 AVD 已关闭.


### 2026-10-02 (P0.3)

- 维护者明确批准 Q1(b), 将 lifecycle 2.9.4 / savedstate 1.3.2 / emoji2 1.4.0 / window 1.5.0 从 debug 实验提升为宿主 app / inrt 的 debug / release 共享依赖. 版本目录单点声明, 两仓库共享锁值与指纹保持一致; 新增 ComposeUiSharedClasspathTest, 四种 runtime classpath 均通过.
- 完成 D10 / D11 / D22 / D26 与附录 B 的证据回填. 临时 ComposeSpikeActivity 从宿主 APK 删除, 草案 AAR 为 androidTestImplementation, 探针为 instrumentation 中的 ComposeSpikeSession, 复用宿主 AboutActivity 与 RawWindow. P0.3 不提前实现 V1 API 或生产装载器.
- 验证: 宿主 JVM 18 项, 插件 JVM 24 项; app debug / debug androidTest / release 装配与 release 原生对齐通过. 新建 API 24 x86 AVD 和 API 33 Redmi 使用宿主 5309 / 已签名插件 7, 各 12 项探针 / 抽屉生命周期 / 插件中心回归通过. 首轮抽屉失败经前台进程证据定位到测试包权限弹窗, 为专用测试包准备权限后原始严格断言全部通过, 未修改抽屉生产代码或保留放宽的测试.
- 未执行: 宿主全量 JVM / 完整 lint, inrt APK 装配与设备运行, 其它设备矩阵 / TalkBack / IME / LeakCanary (后续 gate), minified 宿主 (当前宿主 release 配置关闭 minify), 远端 CI (D7). 本轮插件仅文档与锁来源变化, 设备沿用实现未变的 build 7 APK; build 8 为本次本地提交计数.
- P0 全部完成. 下一会话从 P1.1 契约冻结开始; P1.2 完成正式装载器, P1.3 回填实际最低宿主版本. 宿主工作继续保存在独立 worktree 的 spike/compose-ui-p0 分支, 原宿主未提交内容保留, 不推送或发布.

- 清理: 两台设备本次安装的 3 个临时包均已卸载, 新建 API 24 AVD 已停止. 自动审批以 `blocked by policy` 拒绝删除该 AVD 缓存, 因此保留独立宿主 `build/compose-p03/avd-api24` 与其本地注册; 构建目录被忽略, 不影响工作区提交.

- 提交定位: 宿主 `fb784f034a` (独立分支), 插件为本条所在提交 (`VERSION_BUILD=8`). 插件与宿主独立 worktree 均按本次范围本地提交; 原宿主工作区未参与暂存或提交.


### 2026-10-02 (P1.1)

- 维护者已手动清理上次被工具策略拦截的 AVD 缓存, 本会话确认专用 AVD 注册与缓存目录均不存在, 不再作为遗留项.
- 本会话从 P1.1 契约冻结继续, 宿主原工作区有其它会话改动, 继续在独立 worktree 的 spike/compose-ui-p0 分支实施.

- P1.1 六项完成: V1 常量 / 装载接口 / 不可变 Parcelable 模型 / 30 项目录与 Modifier 词汇 / 快照与边界测试 / release AAR 同步. 宿主以 implementation 提供契约, 插件 compileOnly 消费, 身份常量改为引用契约内联值. 生产渲染器和 compose 脚本 API 尚未交付.
- 定稿细节: UiTree 闭合子树, SetProps 完整属性替换且 null/空集合区分保留与清空; Bundle 仅标量并双向复制; BitmapRef 仅弱引用句柄同进程往返, 外进程需另定图片传输. 目录计数校正为 30 (补齐 P2.5 已列出的 TopAppBar, 选择与输入实为 6 项); 新增容器 / Parcel 边界并记录精确值.
- 验证与产物: API JVM 13, 宿主 JVM 3, 插件 JVM 24; API 24 / 33 Parcel 测试各 6, Redmi API 33 宿主装载及独立 INFO 共 6, 插件 debug 契约 5. 本地签名产物 autojs6-plugin-compose-ui-v1.0.0-fb94ae8d.apk (build 9), 1,460,194 B; lint debug 无问题, release 0 错误 / 3 个既有身份资源警告. 完整证据见 docs/dev/p1-contract-evidence.md.
- 下一会话从 P1.2 开始. 本次未跑宿主 release / inrt 装配与全量回归, 未实现生产渲染器 / JS 全局对象; 最低正式宿主版本仍待 P1.3. 保留已有 P0 负版本接口只为回归, 不把它当作 V1 能力. 继续仅本地提交, 不推送或发布.

- 提交定位: 宿主 `d9b090fd68` (独立分支), 插件为本条所在提交 (`VERSION_BUILD=9`). 本次 6 个测试包安装已清理, 未新建或停止用户现有 AVD; 两仓库按本次范围本地提交.

### 2026-10-02: P1.2 正式装载器与会话核心

- 从插件 340c699 (build 9) 与宿主独立分支 d9b090fd68 继续, 完成 P1.2 的七项交付. 插件本次提交 build 10, 宿主仍为 6.8.0 / 5309. 未修改原宿主工作树.
- 正式实现按序门禁 / 本地化错误 / 包更新缓存失效 / 当前进程 ABI 原生搜索路径, 并将资源与 floaty owners 从测试夹具迁入宿主. 构建时对四种宿主变体强制验证共享依赖锁.
- 会话按脚本 tick 合并目标树, 主线程串行应用批次, 成功后同步发布 generation 与回调绑定. TreeReconciler 保留 key / 同类索引身份, 支持移动及插槽; 失败回滚, 事件队列上限与关闭清理有 JVM / 设备证据.
- P0 工厂不能满足 V1 验收, 因而提前实现三组件 V1 预览与 P2.1 的 NodeStore. P0 实现和宿主探针已删除. 冻结 API / AAR 摘要 / 51 项共享依赖不变, 未修改契约版本. 预览能力边界与完整 P2 待办已明确记录.
- debug/release 插件均完成宿主内往返; 本地签名产物为 autojs6-plugin-compose-ui-v1.0.0-dd04919c.apk (1460582 B), 四 ABI 原生库 / 签名 / CRC32 / 16 KB 对齐通过. 10 语言 README / 说明 / changelog 已同步, 36 个产物与图标检查通过.
- 初期可访问性测试受到已有检查窗口和旧节点缓存影响; 测试限定本包窗口并刷新节点, 使用独立 API 24 AVD 完成最终验收. 未关闭用户检查窗口, 未操作 /sdcard 内容. 详见证据文档中的运行记录与边界.
- 下一会话从 P1.3 注册与最低宿主版本回填开始. 完整渲染器, JS 全局对象与引擎退出接线仍待后续阶段; 宿主 release/inrt 装配, 全量回归, 热更新中的活动会话和 P6 泄漏/性能验收未在本次宣称通过. 继续仅本地提交, 不推送或发布.
- 清理完成: 重用设备上的 10 个本次安装包均已卸载; 自建 compose_ui_p1_api24 AVD 已停机并经 avdmanager 删除, 注册与数据路径均不存在; 原有 AVD 仍保留.
