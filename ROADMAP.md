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

以下决策 D1-D8 已由维护者于 2026-10-02 分两轮确认, 后续阶段不再重新讨论; D9-D32 为据此派生的技术决策, 进入对应阶段前可推翻 (推翻点见附录 D), 之后视同固定.

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D1 | 命名 | 仓库 `AutoJs6-Plugin-Compose-UI`; `rootProject.name=autojs6-plugin-compose-ui`; 显示名 `Compose UI` (英文, 不可翻译); `applicationId` / namespace / Kotlin 包 `io.github.supermonster003.autojs6.plugin.compose.ui`; 插件 ID `compose-ui`, engine `compose`, variant `default`; 脚本全局对象 `compose` (别名 `$compose`); 文档页 `api/compose.md`, 声明 `aj6-int-compose.d.ts`. 不并入 Three 系列. 2026-10-13 修订 (Q9, 维护者拍板): 并入 Three 系列, 仓库 `AutoJs6-Plugin-Three-Block-Compose`, `rootProject.name=autojs6-plugin-three-block-compose`, 显示名 `3-Block Compose` (产品与设计器同名), 包 `io.github.supermonster003.autojs6.plugin.three.block.compose`, 插件 ID / INFO category `three-block-compose`; engine `compose`, 脚本对象, 契约模块 / 包 / 类名, 文档页与声明文件名不变; 版本 1.1.0 -> 1.2.0 与 P8 同版本; 宿主只改契约 `ComposeUiIds` 的两个身份常量 (`f12665ffde`). 改名做法与保留清单见 `AGENTS.md` 第 2 节 |
| D2 | 进程内渲染 | 插件 APK 内预编译的 Kotlin Compose 渲染器装入宿主进程, `ComposeView` 直接嵌入 `ScriptExecuteActivity` 与 floaty 窗口; 协议层 (节点 / 补丁 / 事件) 与传输无关, 进程外插件 Activity 只作为退路 (附录 E.2), 不在 1.0.0 实现 |
| D3 | API 范式: 两者都做 | 节点句柄 (`compose.Text({...})` 返回可长期持有的 `ComposeNode`, 直接改属性与增删子节点) 是底层协议对象; `compose.state` + `compose.mount(render)` 的声明式 render 层是上层糖, 两层共用同一条补丁管线, 1.0.0 同时交付 (附录 A.2 / A.3) |
| D4 | 1.0.0 承载面 | `"ui";` 模式 Activity 内容 (`compose.mount`) 与悬浮窗 (`compose.floaty`); "与旧 ui 混合" (XML 内 `<compose>` 容器, Compose 内 `AndroidView`) 与 "独立对话框 API" (`compose.dialog`) 排 1.1 (附录 F.2 / F.3) |
| D5 | 组件范围: 核心集 | 1.0.0 实现附录 A.5 的核心集 (30 项: 布局 7, 容器 / 顶栏 4, 文本 / 图标 / 图片 3, Button 家族 5 + IconButton, 选择与输入 6, 进度 2, 对话框与提示 2); 导航 / Tabs / 底部弹层 / 菜单 / 日期时间选择 / Pager / Grid / Chips 等宽集排 1.1 (附录 F.4) |
| D6 | TSX 排 1.1 | 1.0.0 只提供函数式元素工厂; TSX 工厂 (经 TypeScript Engine 插件现有 classic TSX 契约, 输出 Compose 节点而非 XML) 为 1.1 阶段 (附录 F.1), 1.0.0 的节点协议为其预留 `compose.createElement(type, props, ...children)` 接点 |
| D7 | 仅本地提交 | 与 3-Shell Terminal / 3-Setup Installer 当前策略一致: 本仓库与宿主改动均仅本地 Conventional Commits, 不推送 GitHub, 不登记官方索引, 不发 Release, 直至维护者明确恢复; 宿主 `PluginInstallWizardCatalog` 条目先落地. 2026-10-04 修订: 维护者授权公开本插件源码仓库并以 noreply 邮箱推送 (会话记录 "源码公开与图稿同步"), 取代源码推送约束; 宿主改动, 官方索引登记与 APK Release 仍待各自明确授权与准入. 2026-10-13 再修订: 维护者授权项目随时推送并发布 Release, 并选定 3-Block Compose 1.4.0, TypeScript Engine 0.7.0, 官方索引登记与文档站 / 离线文档推送; 1.4.0 为首个公开发行版 (tag `v1.4.0` = `bd624a6`), 官方索引 `b3085e8` 准入; 1.5.0 于 2026-10-14 发布 (tag `v1.5.0` = `8bc620a`), 官方索引 `8b095f6` 准入 |
| D8 | 独立界面仅限画廊与设置 (2026-10-08 修订) | 原决策为无独立界面; 维护者于 2026-10-08 排期 F.5 后修订: 插件有启动器入口, 组件画廊与设置页 (附录 F.5, D32), 它们在插件自身进程运行, 只展示预览与示例脚本, 复制到剪贴板或交给宿主运行, 不执行脚本; 宿主内的渲染器入口, Wake Activity 与 INFO 服务不变; 示例脚本仍随插件 `assets/examples/` 提供并同步到宿主示例目录; 发行历史仍由插件中心展示. 2026-10-13 修订 (P8, D38): 独立界面扩展为设计器首页 + 画廊 + 设置; 设计器以 Rhino `Parser` 解析脚本但仍不执行脚本 (永不进入 Rhino `Context`), 启动器 alias, 宿主内渲染器入口, Wake Activity 与 INFO 服务不变. 2026-10-13 再修订 (P9.3 第二步, D43): 编辑时仍不执行脚本; 交互预览中, JavaScript 工程的脚本在插件进程内的沙箱 (`designer/sandbox`) 以 Rhino 解释器运行 (安全标准对象, 拒绝一切 Java 类的 `ClassShutter`, 指令 / 墙钟 / 栈深上限, 只经字符串桥接), 结果只用于画布, 不改动文档; 需要 Android 8.0 (Rhino 的 Date 基于 java.time), 更低版本与 TSX 工程沿用第一步 |
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
| D21 | 生命周期 | 沿用 ui 模式现状: `ScriptExecuteActivity` 通过 Manifest 的 `configChanges` 自行处理旋转 / 尺寸等配置变化, 保留原引擎; 实际 Activity 重建或销毁时, `onDestroy` 销毁引擎, 不承诺恢复. 会话在引擎退出, Activity destroy, 窗口 close 时释放 Composition (`ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool` + 显式 `dispose`), 回调注册表与补丁队列; `rememberSaveable` 不暴露给脚本 (1.0.0). P5.1 以真实旋转后更新及显式 `recreate()` 验证, 修正原括注将旋转等同于重建的表述, 不改变宿主行为 |
| D22 | 插件 APK 形态 | 无插件自有原生代码, 不启用 ABI 拆分; 单个发布文件 `autojs6-plugin-compose-ui-v{VERSION_NAME}-{CRC32}.apk`, `getInfo()` 显式 `supportedAbis = emptyArray()`. Compose graphics-path 1.0.1 自带 `libandroidx.graphics.path.so` x arm64-v8a / armeabi-v7a / x86_64 / x86, 各约 10 KB, APK 中未压缩, 发布门禁校验 ELF 与 zip 偏移 16 KB 对齐. P0.2 已验证 D10 的 `<apk>!/lib/<进程 ABI>` 搜索路径. release 保留 R8 裁剪 / 混淆与 `isShrinkResources = true`, 保持 Kotlin ABI, 使用插件私有混淆包名, 补回 compileOnly lifecycle 的 ViewModel 构造规则; 因 R8 专用 Kotlin 方法不在宿主副本中, P0 禁用优化变换 (`-dontoptimize`), 恢复优化前必须重跑宿主内装载验证 |
| D23 | 插件中心与宿主注册 | INFO 服务 (action `org.autojs.plugin.INFO`, category `compose-ui`, 实现 `IPluginInfoProvider`), Wake Activity, meta-data `requiresHostVersion` 与 `org.autojs.plugin.compose.RENDERER_FACTORY`; 没有 Binder 能力服务 (与 ImGui 同形), 因此不进 `SERVICE_ACTION_BY_ENGINE`, 由 INFO 通用发现列出; 默认启用遵循 `PluginDefaultEnabledPolicy` 现状 (不新增例外); 宿主向导 `entry(official("compose.ui"), "Compose UI", UI)` 与 ImGui 同组 |
| D24 | 宿主退化 | 宿主只保留契约模块, 装载器, 会话核心与脚本 API; 插件缺失 / 禁用 / 未授权 / 不兼容时 `compose.mount` / `compose.floaty` 抛 D18 的对应错误, 宿主不内置任何 Compose 渲染实现, 也不回退到 XML 布局 |
| D25 | Compose 依赖与编译器 | 插件 `platform("androidx.compose:compose-bom:2026.09.00")` + `runtime` / `ui` / `foundation` / `material3` / `animation` / `material-icons-core` (extended 图标集见 Q2), `debugImplementation ui-tooling`; Compose 编译器插件 `org.jetbrains.kotlin.plugin.compose` 在根 `build.gradle.kts` 以 `version System.getProperty("gradle.kotlin.version") apply false` 声明 (与平台插件选定的 Kotlin 同版本), 不硬编码; 若 AGP 9 内置 Kotlin 不接受该编译器插件, 经构建验证后在插件仓库声明 `org.jetbrains.kotlin.android` 并在 `AGENTS.md` 注明理由 (规范 5.2 的例外条款) |
| D26 | 宿主共享依赖的版本纪律 | 2026-10-02 Q1(b) 定稿: 宿主 app / inrt 的 debug / release 共用 lifecycle 2.9.4, savedstate 1.3.2, emoji2 1.4.0, window 1.5.0, 由宿主版本目录单点声明; 插件编译依赖不能高于宿主, `locks/host-shared-deps.lock` 锁定 51 项解析版本. AndroidX / coroutines / serialization 共享项为插件 `compileOnly`, Kotlin stdlib 2.4.0 为 INFO 独立进程保留打包副本, 宿主内仍 parent-first 且 R8 必须保持 ABI. 宿主 `ComposeUiSharedClasspathTest` 与 `verifyComposeUiSharedClasspath` 守卫四个 runtime classpath, 插件 `verifySharedClasspath` 守卫编译与排除集合; 任一版本变化时同步两仓库锁与正式最低宿主版本. 依赖解析一致不等于 minified 宿主 ABI 已通过, P1 需补正式契约 / keep 规则 / 装载器并回填最低版本. 2026-10-13 修订 (P8.1, D34): `compose-ui-api` 由 `compileOnly` 改为随插件打包并与 D32 的共享副本一样完整 keep, 宿主内仍 parent-first 遮蔽, 版本协商不变; `verify_apk_classpath.py` 的 "不含契约类" 断言反转为 "含契约类且 `--shrunk` 闭包可由 APK 内副本满足" |
| D27 | 图片与大对象 | `Image` 的 `src` 接受 `ImageWrapper` (`images.read` 等), 文件路径, 以及宿主 `R.drawable` 名称; 进程内直接传递 `Bitmap` 引用 (`UiValue.BitmapRef`), 不编码; 位图所有权归脚本 (`recycle` 由脚本决定), 渲染器只持弱引用并在节点移除时放手; URL 图片不在 1.0.0 |
| D28 | 兼容矩阵 | API 24 AVD x86, API 28 Sony G8441 (arm64), API 31 Sony XQ-AT72, API 33 Redmi 22120RN86C, API 35 Xiaomi 23046RP50C (HyperOS), API 37 AVD (16 KB 页); 每台设备记录宿主 build 与插件 build; HyperOS 上悬浮窗只在桌面之上活动且需 `requestFocus` (既有记录), 作为 P3.4 验收的已知条件 |
| D29 | 契约与最低宿主版本 | V1 `CONTRACT_VERSION = 1`, `MIN_SUPPORTED = 1`; P1.3 将 `REQUIRED_HOST_VERSION_CODE` 确认为 5316 (AutoJs6 6.8.0), 对应集成装载器 / 会话核心 / 插件中心注册的构建. 同步 Manifest, 公共文案, JVM / 设备断言与锁定 AAR, 只更新部署元数据, 不变更 V1 wire 语义. 原 5308 为建仓占位值, 保留于历史证据中. F.4 起插件声明 `CONTRACT_VERSION = 2` 并要求匹配宿主 5322; 宿主同时接受 V1 / V2 插件, V1 常量与 5316 保留 |
| D30 | 打包应用 (inrt) | 打包的脚本应用使用 `compose` 时需目标设备已安装并启用本插件 (与 epub / mail 等插件模块一致); P5.2 验证 inrt 构建中的装载路径与错误提示, 文档写明 |
| D31 | 示例与守卫 | 插件 `assets/examples/*.js` (计数器, 表单, 列表, 悬浮 HUD, 主题) 由 `assets/examples/index.json` 列出, P4.1 同步到宿主 `app/src/main/assets-app/sample/` 的对应分类; 示例必须能在兼容矩阵上运行, 作为 P5 回归用例 |
| D32 | 独立进程运行时 | 画廊与设置页在插件自身进程运行, 插件 APK 在运行时引用的全部共享构件 (activity / core / lifecycle / savedstate / startup / tracing / kotlinx-coroutines, 以及 2026-10-08 修订补入的 arch core-runtime / customview-poolingcontainer / emoji2 / lifecycle-process / window 与完整 keep 后被保留成员引用的 lifecycle-livedata-core / kotlinx-serialization, 共 47 项, `app/build.gradle.kts` 的 `standaloneRuntime`) 以宿主共享依赖锁的精确版本随插件打包并用 constraints 钉死; 宿主内 parent-first 装载使这些副本被宿主类遮蔽, R8 对这些包与 `kotlin.**` 一样完整 `-keep` (2026-10-08 由 `-keepnames` 改为 `-keep`: 只保留名称时 R8 仍按插件程序的整程序事实推断, 例如认定插件从不初始化的 `EmojiCompat.get()` 永不返回而把 Compose 文本布局编译成 `throw null`, 在宿主内崩溃); 只有无人引用的 appcompat 2 项继续排除. 守卫: `verify_apk_classpath.py --shrunk` 要求发布包引用的每个类都在 APK, 平台或契约中; 设备测试 APK 不携带宿主独有构件; `verify_standalone_launch.py` 在设备上从启动器 alias 真实启动画廊. 修订原因: build42 的 36 项集合漏掉上述 4 组构件, 画廊从启动器打开即崩溃 (`NoClassDefFoundError: ArchTaskExecutor`), 而当时的设备测试被测试 APK 携带的宿主副本掩盖. 独立界面不得引用 `compose-ui-api` 契约类 (插件进程没有它们). 2026-10-13 修订 (P8.1): 打包集合追加契约 AAR `compose-ui-api` (完整 keep) 与宿主 Rhino slim jar (仅 `designer/parse` 引用, 允许 R8 裁剪到 Parser / AST 闭包), 独立界面自 P8 起可引用契约类; `verify_standalone_launch.py` 改为启动设计器首页, 再进入画廊与设置页 |
| D33 | 设计器归属 (2026-10-13) | 可视化设计器做在本插件的独立应用内, 在插件自身进程运行 (P8); 宿主只增加可选的反向打开入口 (P8.4, 由维护者并行协调); 脚本 API, 契约版本与宿主内渲染路径不变 |
| D34 | 设计期预览 (2026-10-13) | 用真实渲染器所见即所得: `compose-ui-api` 随插件打包并完整 keep (修订 D26 / D32), 设计器把文档派生为 `RenderFrame` 复用 `RenderNode` / `ModifierMapper` / `ThemeMapper`; 不另写简化预览 |
| D35 | 工程与往返 (2026-10-13) | 脚本文件即工程, 不另存 JSON; 以宿主 Rhino slim jar (SHA-256 锁定) 的 `Parser` 解析完整 AST 回树, 只解析不执行; 可识别子集见 P8.2, 其余按源码范围不透明保留; 无法映射即拒绝并定位, 不静默丢弃; 首版只导出 JavaScript, TSX 留后续版本. 2026-10-13 修订 (P9.3 第二步): `designer/parse` 仍只解析不执行, 进入 Rhino `Context` 的只有 `designer/sandbox` (`RhinoImportGuardTest`); R8 为沙箱保留解释器闭包, 另 keep Rhino 按名反射构造的 `Interpreter` 与 `VMBridge_jdk18` |
| D36 | 交互形态与范围 (2026-10-13) | 大纲树 + 预览画布 + 组件面板 + 属性抽屉; 拖拽在大纲内重排 / 换父, 画布长按拖动并高亮合法落点; 全部 55 个组件由契约目录驱动, 弹层类以展开开关或占位呈现; modifier 链完整可视化编辑 (增删, 排序, 参数, 作用域过滤) |
| D37 | 逻辑生成范围 (2026-10-13) | 事件生成处理函数桩; 受控组件可绑定 `compose.state` 变量并自动生成声明与赋值; 处理函数源码可在属性抽屉内直接编辑 (纯文本, 保存时语法检查); 其它逻辑在宿主编辑器编写并以不透明段保留 |
| D38 | 独立应用导航 (2026-10-13, 修订 D8) | 启动器进入设计器首页 (工程列表), 画廊与设置降为次级页; 四个 alias 与图标不变; `verify_standalone_launch.py` 与设备测试随之调整 |
| D39 | 存储与宿主衔接 (2026-10-13) | 工程存插件私有目录, 以系统文件选择器导入导出, `FileProvider` 对外授权; 去向: 发送运行, 在宿主编辑器打开, 导入宿主脚本目录, 复制剪贴板 (均复用宿主既有 `external.open` 入口, 宿主无需改动); 反向打开: 宿主以 content Uri + 写授权启动设计器, 设计器保存时直接写回, 宿主编辑器返回时重载 |
| D40 | 可靠性 (2026-10-13) | 全部编辑走命令模式, 支持多步撤销 / 重做; 草稿自动保存在私有目录, 进程被杀可恢复; 只有显式保存才写脚本文件 (含反向打开的宿主文件) |
| D41 | P9 版本切分 (2026-10-13, Q12) | 1.3.0 = P9.1 画布与预览精度 + P9.3 第一步 (预览状态面板); 1.4.0 = P9.2 TSX 工程; 1.5.0 = P9.3 第二步 (沙箱执行), 是否立项在 1.4.0 后按 D43 评估 |
| D42 | TSX 解析 (2026-10-13, Q13) | 语法树由 TypeScript Engine 提供: 新增独立的解析契约 (宿主 `plugin-api` 中的独立 AIDL 与独立 action, 受 PLUGIN 权限保护), TypeScript Engine 在同一 WebView 运行时内以 TypeScript 编译器只解析单个源文件, 返回带源码位置的语法树与语法诊断, 不占编译槽位, 现有编译契约保持冻结; 设计器把语法树映射为 `DesignDocument`, TSX 的生成仍由插件内 `TsxWriter` 完成. 未安装 TypeScript Engine 或版本过低时, `.tsx` 以只读源码方式打开 (可查看与复制, 不能编辑树, 没有画布预览) 并提示所需版本 |
| D43 | 交互预览 (2026-10-13, Q14) | 先做预览状态面板 (不执行脚本, 只临时改变预览中的 state 值); 插件内沙箱执行在 1.4.0 之后评估, 立项前单独拍板并修订 D8 / D35; 不做由宿主真实运行脚本的实时预览协议. 2026-10-13 维护者于 1.4.0 发布后按路线图方案立项第二步 (1.5.0), D8 / D35 同日修订 |
| D44 | 图片与 AndroidView 预览 (2026-10-13, Q15) | Image: `@drawable/name` 经宿主包 Context 读取; 本地文件由用户为每个工程授予一个图片目录 (系统文件选择器的目录树, 持久授权), 相对路径在该目录下解析; 不申请运行时权限, 网络地址与宿主一致地拒绝. AndroidView 保持不可从组件面板插入, 脚本中已有的照常保留, 占位显示 `view` 表达式源码 |
| D45 | 导入来源同步 (2026-10-13, Q16) | 导入时取持久授权并记录来源 (改名时迁移); 编辑页菜单提供 "写回来源" 与 "从来源重新导入", 以内容基线检测来源侧修改并询问; 不在保存时自动写回 |

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
- 组件画廊 Activity 与设置页 (附录 F.5, 1.1.0; 维护者 2026-10-08 排期并修订 D8).

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

以下为建仓时的目标架构草案; 实际文件布局 (例如组件按 `AdvancedComponents` / `Wide*` 拆分, `dialog/` 与 `interop/` 扩展入口) 以 `AGENTS.md` 第 4 节与源码为准.

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
app/                                    F.5 独立界面 (插件进程): StandaloneActivity / GalleryActivity / SettingsActivity
designer/                               P8 可视化设计器 (插件进程, 1.2.0)
  model/                                DesignDocument / DesignNode / ModifierEntry, 不可变模型与 EditCommand 撤销重做栈
  parse/                                Rhino Parser -> AST -> DesignDocument (只解析不执行), DesignerParseError 定位; 唯一允许引用 org.mozilla 的包
  codegen/                              DesignDocument -> 脚本文本 (风格同 GalleryNode.render), 往返稳定
  preview/                              DesignDocument -> RenderFrame 适配, 设计期命令 / 事件 / 文本控制器本地实现
  ui/                                   DesignerHomeActivity (工程列表), DesignerActivity (画布 / 大纲 / 面板 / 抽屉)
  storage/                              私有目录工程与草稿, SAF 导入导出, FileProvider, 宿主去向 (运行 / 编辑 / 导入 / 剪贴板)
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
| 仓库与目录名 | `AutoJs6-Plugin-Three-Block-Compose` (2026-10-13 前为 `AutoJs6-Plugin-Compose-UI`) |
| `rootProject.name` | `autojs6-plugin-three-block-compose` |
| 应用标题 (不可翻译) | `3-Block Compose` (2026-10-13 前为 `Compose UI`) |
| `applicationId` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.three.block.compose` |
| 插件 ID / engine / variant | `three-block-compose` / `compose` / `default` |
| INFO 服务 | `ThreeBlockComposeInfoService`, action `org.autojs.plugin.INFO`, category `three-block-compose` |
| 渲染器入口 meta-data | `org.autojs.plugin.compose.RENDERER_FACTORY` = `io.github.supermonster003.autojs6.plugin.three.block.compose.renderer.ComposeUiRendererFactoryImpl` |
| 宿主契约包 | `org.autojs.plugin.compose.api` (`plugin-api/compose-ui-api`) |
| 契约版本 / 最低宿主 | 当前 `CONTRACT_VERSION = 2` (P1.1 冻结 V1, F.4 只追加 V2), 最低宿主 5322 (6.8.0); V1 历史最低宿主 5316 (P1.3 回填) 保留在冻结常量中 |
| 脚本全局对象 | `compose` (别名 `$compose`), 错误类 `ComposeError` |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.9.0 (建仓起点 1.8.3) |
| Compose | BOM `2026.09.00`; 编译器插件版本 = `gradle.kotlin.version` |
| 发布文件名 | `autojs6-plugin-three-block-compose-v{VERSION_NAME}-{CRC32}.apk` (单 APK, D22) |
| 向导条目 | `entry(official("three.block.compose"), "Three Block Compose", UI)` |
| 文档 / 声明 | `api/compose.md` + 类型页; `aj6-int-compose.d.ts` |

---

## 5. 阶段总览

| 阶段 | 内容 | 目标版本 | 状态 |
| --- | --- | --- | --- |
| P0 | 仓库骨架; 进程内装载 spike (加载器, owner, 资源, 计数器闭环) | 1.0.0 | 已完成 (2026-10-02, P0.1 - P0.3, Q1(b)) |
| P1 | 宿主契约模块, 装载器, 会话核心, 注册与协议文档 | 1.0.0 | 已完成 (2026-10-03, P1.1 - P1.3) |
| P2 | 插件渲染器: 骨架, 核心集组件, 输入框, Modifier 链, 主题 | 1.0.0 | 已完成 (P2.1 - P2.6) |
| P3 | 脚本 API `compose`: 节点句柄层, state + render 层, ui 模式与悬浮窗承载, refs / 命令 / 错误 | 1.0.0 | 已完成 (2026-10-03, P3.1 - P3.5) |
| P4 | 示例, 无障碍与选择器, 守卫测试 | 1.0.0 | 已完成 (2026-10-03, P4.1 - P4.3) |
| P5 | 健壮性, 兼容矩阵 (含 inrt), 性能与体积 | 1.0.0 | 已完成; Q5 第一版复核规则已于 2026-10-03 确认 |
| P6 | 文档, 声明, README / changelog, 1.0.0 本地 gate | 1.0.0 | 已完成本地 gate (2026-10-04); 正式图案待源图, 远端步骤遵守 D7 |
| P7 | 1.1.0 候选: TSX, 与旧 ui 混合, `compose.dialog`, 宽集组件, 组件画廊, 细粒度更新 | 1.1.0 | F.1 - F.6 已全部完成本地实现与门禁 (F.5 经维护者 2026-10-08 排期并修订 D8 后实施) |
| P8 | 1.2.0 可视化设计器: 契约 / Rhino 打包, 模型与往返, 设计器界面, 存储与宿主衔接, 文档与 gate | 1.2.0 | 2026-10-13 需求拍板 (D33 - D40, 修订 D8 / D26 / D32), Q8 - Q11 已拍板; 同日完成 P8.0 改名 (修订 D1), P8.1 依赖边界, P8.2 模型 / 解析 / 生成, P8.3 设计器界面, P8.4 存储 / 宿主衔接 (含宿主编辑器入口) 与 P8.5 文档 / changelog / 本地 gate (证据 `docs/dev/p8-designer-evidence.md`); 1.2.0 本地 gate 已完成, 远端步骤遵守 D7 |
| P9 | 设计器增强: 画布与预览精度, TSX 工程, 设计器内交互预览 | 1.3.0 - 1.5.0 (D41) | 2026-10-13 维护者选定三个方向, Q12 - Q16 同日拍板 (D41 - D45); 1.2.0 发布基线 6db6da2. 1.3.0 (P9.1 全部, P9.3 第一步与模式切换) 同日完成并过本地 gate (build 129 源码, 证据 `docs/dev/p9-designer-evidence.md`), 未推送; 1.4.0 (P9.2 TSX 工程, 宿主 `fce22c83a6` 与 TypeScript Engine 0.7.0 `1e92487` 的解析契约) 同日完成并过本地 gate (证据 `docs/dev/p9-tsx-evidence.md`), 并作为首个公开发行版发布 (D7 再修订, GitHub Release `v1.4.0` 与官方索引 `b3085e8`); P9.3 第二步 (1.5.0) 由维护者于同日按 D43 立项, 2026-10-14 完成并过本地 gate (build 139 源码, 证据 `docs/dev/p9-sandbox-evidence.md`), 发布为 GitHub Release `v1.5.0` 并由官方索引 `8b095f6` 准入 |

依赖关系: P0.2 的结论决定 D10 / D11 是否调整 (P0.3); P1 依赖 P0.3; P2 与 P3 可在 P1.1 契约冻结后并行 (P2 先于 P3.3 的端到端验收); P4 - P6 依赖 P2 / P3; P7 依赖 1.0.0 gate; P8 依赖 P7 全部完成 (1.1.0 本地 gate), P8.2 / P8.3 依赖 P8.1, P8.4 依赖 P8.2, P8.5 依赖 P8.1 - P8.4. P9 依赖 1.2.0 本地 gate; 按 D41 先 P9.1 与 P9.3 第一步 (1.3.0), 再 P9.2 (1.4.0, 依赖宿主与 TypeScript Engine 的解析契约), P9.3 第二步在 1.4.0 后评估并依赖 P9.1 的弹层画布内绘制.

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
- [x] (插件) `.github/workflows/build.yml` (JVM 测试, debug / release 装配, lint, androidTest 装配; API 24 x86 与 API 35 x86_64 模拟器契约测试) 与 `markdown.yml`; 未推送期间只做本地语法与路径校验. 证据: 提交 6cc0d52; 工作流已落盘, 未推送 (D7), 仅本地语法与路径校验; 本地等效命令 `testDebugUnitTest assembleDebugAndroidTest lintDebug assembleRelease` 通过, lint 0 问题. 额外 (E3): `ComposeUiPluginContractTest` 5 项在 Xiaomi Pad 23046RP50C (HyperOS, API 35, arm64-v8a) 与 Sony G8441 (API 28, arm64-v8a) 上全部通过 (含 `System.loadLibrary("androidx.graphics.path")`), 测试后无残留安装. 未执行: API 24 x86 / API 35 x86_64 / 16 KB 页 AVD 的模拟器矩阵 (留给 CI 与 P0.2). 2026-10-08 复核: 源码公开后首个远端运行 (8d2528c) 的构建作业通过, 两个模拟器作业因 emulator-runner 逐行执行 `script`, 把续行符 `\` 当作 Gradle 任务名而在测试开始前失败; 提交 3e30f25 改为单行命令, 同一命令在专用 API 35 x86_64 google_apis AVD 上完整 64 项 instrumentation 通过并生成覆盖率 (指令 81%, 分支 61%). 远端重跑待下次推送.
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

- [x] (插件) `ComposeUiRendererFactoryImpl` (契约版本, capabilities: COMPONENTS 列表, FEATURES, COMPOSE_VERSION, SHARED_DEPS_FINGERPRINT), `ComposeUiRendererImpl` (`ComposeView(ComposeHostContext)`, `setViewCompositionStrategy`, `setContent { MaterialTheme(ThemeMapper(theme)) { RenderNode(rootState.value) } }`, `apply(batch)` 在主线程更新不可变树, `execute(command)`, `setTheme`, `dispose`).
- [x] (插件) `NodeStore` 不可变树与补丁应用 (setProps / insert / remove / move / replaceSlot), 全部校验失败整批拒绝并抛契约异常 (宿主转为 `ComposeError`), 不留下半应用状态. P1.2 为正式宿主往返提前落地, 6 项 JVM 测试覆盖增删 / 跨父移动 / 插槽 / 晚失败回滚 / 环与作用域拒绝, 详见 `docs/dev/p1-session-evidence.md`.
- [x] (插件) `RenderNode` 分派, `key(nodeId)`, 未知类型 -> 占位 `Text("<unknown: type>")` 仅 debug 构建, release 由宿主提前拒绝.
- [x] (测试) JVM: `NodeStoreTest` (补丁应用与拒绝), `ThemeMapperTest` / `ValueMapperTest` (颜色解析 `#RRGGBB` / `#AARRGGBB` / 命名色, 尺寸 dp / sp, 文本样式); instrumentation: 空树 / 单节点树装配.

### P2.2 布局与基础组件

- [x] (插件) Column / Row / Box / Spacer (含 `verticalArrangement` / `horizontalAlignment` / `spacing` 等), Surface / Card / HorizontalDivider, Text (样式, 对齐, 省略, 最大行数, 可选择), Icon (material-icons-core 名称表 + `ImageVector` 映射), Image (BitmapRef / contentScale / contentDescription).
- [x] (测试) instrumentation 用 `onNodeWithTag` 断言布局与属性; 目录属性与实现逐项对照 (`CatalogCoverageTest`: 渲染器声明的 COMPONENTS 与实现的 `when` 分支一致).

### P2.3 交互组件

- [x] (插件) Button / ElevatedButton / FilledTonalButton / OutlinedButton / TextButton / IconButton (`onClick`, `enabled`, 内容插槽), Switch / Checkbox / RadioButton (`checked` 受控 + `onCheckedChange`), Slider (`value` / `range` / `steps` / `onValueChange` / `onValueChangeFinished`).
- [x] (测试) 事件 payload 与 generation 经宿主 `ComposeSession` 回到调度器; 受控组件在脚本未回写时保持旧值 (受控语义), 文档注明.

P2.1 - P2.3 证据 (2026-10-03): `docs/dev/p13-p2-evidence.md`. 能力表与分派同源, 实现 20 种组件及其目录属性 / 插槽; 图标为 core 的 280 个显式样式名称. 插件 JVM 45 项, API 24 x86 / API 35 arm64 各 17 项插件测试 (含 12 项渲染 / 像素 / 生命周期) 与 9 项宿主集成测试通过; minified release 插件各 4 项计数器 / INFO / 受控组件往返通过. 主题覆盖 48 个颜色角色及动态色 / 字体, seed 色板使用 CIELAB/LCh, 未宣称 Google HCT. 空批次与首批 generation 0, Slider 派生刻度上限和失效节点命令均有边界守卫.

### P2.4 输入框

- [x] (插件) TextField / OutlinedTextField: `TextFieldState` 原生编辑态 (D15), `onValueChange(text, selection, editSeq)` 节流为每帧一次, `edit` 命令按 `editSeq` 仲裁, `label` / `placeholder` / `leadingIcon` / `trailingIcon` / `supportingText` 插槽, `singleLine` / `maxLines` / `keyboardType` / `imeAction` / `isError` / `readOnly` / `visualTransformation(password)`.
- [x] (测试) instrumentation: 中文输入法组合 (API 33 真机 Gboard / 小米输入法), 连续删除, 粘贴, 光标移动, 脚本回写与用户输入竞争 (旧 editSeq 被拒), IME 弹出时 Scaffold 内容避让 (与宿主 Activity Insets 处理的分工在此定稿, 不双重 padding).

P2.4 证据 (2026-10-03): 插件 `cebd1d3`, 宿主 `60bae7f8c5`; `docs/dev/p24-p25-evidence.md`. API 24 / 33 / 35 各 9 项输入框测试通过, 覆盖原生 InputConnection 组合 / 删除 / 粘贴 / UTF-16 选区 / editSeq 竞争与密码语义; API 33 小米搜狗实键拼音与"你好"候选提交单独通过. 宿主为唯一 Insets owner, 挂载期间选择 adjustResize 并按 max(systemBars.bottom, ime.bottom) 避让, Compose 不叠加系统栏或 IME padding; API 24 / 33 / 35 跨 APK 几何与恢复验证通过. 修复 Material 装饰层晚订阅丢失自动恢复 Focus 的竞态, 保留原始组件替换验收.

### P2.5 列表, 脚手架与提示

- [x] (插件) LazyColumn / LazyRow (子节点 key 映射 `items(key)`, `contentPadding`, `spacing`, `scrollTo` 命令, `onScroll` 事件可选节流), Scaffold (topBar / snackbarHost / floatingActionButton 插槽为 1.1 预留, 1.0.0 实现 topBar + content + snackbarHost), TopAppBar (title / navigationIcon / actions 插槽), AlertDialog (`open` 受控, title / text / confirm / dismiss 插槽, `onDismissRequest`), Snackbar (`session.showSnackbar(message, options)` 命令 -> `SnackbarHostState`, `onAction` / `onDismiss` 事件), CircularProgressIndicator / LinearProgressIndicator (确定 / 不确定).
- [x] (测试) 1000 项 LazyColumn 的补丁应用耗时与滚动帧时间 (记录, 不预设阈值; P5.3 定阈值); 列表重排 (key 移动) 保留条目内 Switch 状态.

P2.5 证据 (2026-10-03): 插件 `cebd1d3`, 宿主 `ee82363172`; `AdvancedRendererTest` 5 项在 API 24 / 33 / 35 通过. 1000 行 / 3001 节点的增量补丁分别为 18.610 / 89.125 / 34.624 ms, 滚动帧 p95 为 16.677 / 50.855 / 6.972 ms (debug 小样本, 不作为发布阈值). 同轮数据增长 / 缩减与 scrollTo, key 移动后的受控 Switch, 对话框, 进度与 Snackbar 终止结果均有断言. V1 29 个节点 + 命令式 Snackbar 全覆盖; 命令回调跨树提交保留并一次性释放. API 24 / 35 的 minified 插件跨 APK 各 6 项通过, 公开 compose 脚本入口仍属 P3.

### P2.6 Modifier 链与快捷属性

- [x] (插件) `ModifierMapper`: padding / size / width / height / fillMaxWidth / fillMaxHeight / fillMaxSize / weight (RowScope / ColumnScope) / align (BoxScope) / background (色 + 形状) / border / clip (RoundedCorner / Circle) / alpha / clickable / verticalScroll / horizontalScroll / offset / aspectRatio / testTag / semantics(contentDescription); 顺序严格按 ops 序列组合; 作用域操作出现在错误父节点时宿主已拒绝, 渲染器再次防御.
- [x] (测试) JVM `ModifierMapperTest` 对每个 op 的参数解析; instrumentation 对 `padding -> background` 与 `background -> padding` 的像素 / 语义差异断言.

---

P2.6 证据 (2026-10-03): 完整 Modifier 链为本轮基础 / 交互组件提供能力, 因依赖关系一并交付, 路线图小节保持不变. 5 项 JVM 用例覆盖 20 个 op 与命令注册生命周期; 设备像素用例验证 padding/background 顺序, 触摸用例验证外层 clickable 不被内层禁用及长按不触发普通点击, 样式更新保持滚动位置. API 24 通过实际 View 树软件位图读回像素, API 26+ 使用 PixelCopy.

## P3: 脚本 API `compose`

目标: 宿主 `compose` 全局对象完整可用, ui 模式与悬浮窗两种承载, 节点句柄层与 render 层共用管线.

### P3.1 节点句柄层

- [x] (宿主) `Compose : Augmentable(), Invokable` (`AugmentableKey("compose")`, 别名 `$compose`), 注册于 `ScriptRuntime` (在 `PluginAutoMountManager.apply` 之前); `compose.isAvailable()`, `compose.version` (插件 / 契约 / Compose 版本, 不可用时 `null`), `compose.createElement(type, props, ...children)` 与按目录生成的元素工厂 `compose.Column / Text / ...` (名称表来自 `ComponentCatalog.V1`, 保证 d.ts 与运行时一致).
- [x] (宿主) `ComposeNode` 句柄: `type` / `key` / `nodeId` / `parent` / `children`; `set(props)` 与目录声明属性的代理读写 (`node.text = '...'`), `get(name)`, `append / insert / remove / replace / clear`, `slot(name, node)`, `on(event, fn) / off`, 命令 `focus() / blur() / scrollTo(index | { offset }) / edit({ text, selection })`; 未挂载的节点上操作只改本地状态, 挂载后每次操作进入当前批; 跨会话移动节点 -> `NODE_DETACHED` / `INVALID_ARGUMENT`.
- [x] (宿主) 值转换 `ComposeScriptValues`: 颜色 (与 `colors` 模块一致的解析), 尺寸 (数字默认 dp, 字符串 `16sp` / `8dp`), 文本样式对象, 图片 (`ImageWrapper` / 路径 / `@drawable` 名), 回调 (注册为 callbackId), 子节点 (数组 / 单节点 / 字符串 -> Text 简写); 未知属性 -> `UNKNOWN_PROP`, 类型错误 -> `INVALID_ARGUMENT` (消息含组件名与属性名).
- [x] (测试) JVM: 元素工厂与目录一致性, 值转换边界 (非法颜色, 负尺寸, 超长字符串, Unicode 补充平面), 代理属性读写, 子节点操作产生的补丁序列.

### P3.2 state + render 层

- [x] (宿主) `compose.state(initial)` -> `{ value }` 对象: getter 在 render 期间登记依赖, setter 标记脏并在当前调度器 tick 末尾合并调度一次 render (`compose.batch(fn)` 显式合并); `compose.mount(render, options)` 执行 render 得到节点树, 经 `TreeReconciler` 产出补丁; render 抛错 -> `RENDER_FAILED` (含脚本堆栈), 保留上一版界面; render 返回非节点 -> `INVALID_ARGUMENT`.
- [x] (宿主) render 模式下的节点身份: 同一 render 内 `key` 重复 -> `DUPLICATE_KEY`; 缺 key 的同类兄弟按索引匹配并 warn 一次 (Q3); `compose.ref()` 在 render 中以 `ref` 属性绑定节点, 下一 tick 起 `ref.current` 指向复用后的句柄.
- [x] (宿主) `compose.post(fn)` (D12), `compose.theme(spec)` (会话级或全局默认).
- [x] (测试) JVM: 依赖收集与合并调度 (10 次 setter 只触发 1 次 render), 条件分支切换的依赖重算, render 异常回滚, ref 复用; 与节点句柄层混用 (render 返回的树里包含预先创建并手动修改的节点).

### P3.3 ui 模式承载

- [x] (宿主) `compose.mount(nodeOrRender, options)` 在 `"ui";` 脚本中: 取当前 Activity (无 Activity -> `UI_MODE_REQUIRED`), 创建会话并把渲染器 `view()` 经既有 `setContentViewRhinoRuntime` 路径设为内容 (`ui.view` 指向该 View), 返回 `ComposeSession` 对象 (`root` / `update(fn)` / `post(fn)` / `close()` / `on('close')` / `showSnackbar`); 与 `ui.layout` 二选一, 后者覆盖前者时关闭会话并 warn; Activity `back_pressed` 等事件沿用 `ui` 既有 emitter.
- [x] (宿主) Insets 分工定稿 (P2.4 结论): 保持 Activity 对 `android.R.id.content` 的系统栏 padding, Compose 侧不再 `systemBarsPadding`; IME 由 Compose `imePadding` 或 Activity `adjustResize` 二选一并写入文档.
- [x] (宿主) 生命周期: `ScriptExecuteActivity.onDestroy` -> 引擎销毁 -> 会话 `close` (释放 Composition, 注册表, 队列); 停止脚本 (强制 / 正常) 同样触发; 重复 `mount` 先关闭旧会话.
- [x] (测试) instrumentation (API 33 真机 + API 24 AVD): 计数器, 表单 (TextField + Switch + Button), 1000 项列表三个脚本经 `RunIntentActivity` 运行并以 uiautomator 断言; 反复打开关闭 20 次无泄漏; 停止脚本后无残留回调.

P3.1 - P3.3 证据 (2026-10-03): `docs/dev/p31-p33-evidence.md`. 宿主已提供 compose / $compose 与 29 个节点工厂, 句柄事务, state/render/ref/batch/post/theme 和真实 ui Activity 挂载. 111 项宿主相关 JVM (含节点图 18, 脚本 API 16, 响应式 9, 值转换 13, 错误 6) 及 13 项冻结契约测试通过. API 24 x86 与 API 33 Redmi 各 7 项 RunIntentActivity 真实脚本验收通过: 计数器, 表单及输入框移除/再插入, 1000 项 key 列表, 主线程 render 错误保留旧界面并恢复, 20 次挂载/关闭, XML 覆盖清理, 停止引擎后的延迟回调取消. UI 采用 P2.4 的宿主单一 Insets owner; 未指定主题保留宿主动态默认值. ComposeError 基础构造器随这些入口提供, P3.5 的完整集成守卫仍按原小节继续, 本轮不标记 P3.4 / P3.5 完成.

### P3.4 悬浮窗承载

- [x] (宿主) `compose.floaty(nodeOrRender, options)`: `options.raw` 选择 `rawWindow` / 可调整窗口, 其它选项 (位置, 尺寸, 可触摸, 焦点) 透传; 权限检查复用 `floaty.ensurePermission` 语义 (`PERMISSION_REQUIRED`); 窗口 View 为渲染器 `view()`, owner 由 P1.2 的 `ComposeFloatyOwners` 提供; 返回 `ComposeFloatyWindow` (继承 `JsRawWindow` / `JsResizableWindow` 的 `setPosition / setSize / close / requestFocus / disableFocus / setTouchable` + `session`); 非 ui 脚本的调度器为脚本 looper, 脚本退出时 `floaty.closeAll` 既有路径关闭窗口与会话.
- [x] (测试) 真机 (HyperOS, Sony API 31) + AVD: 非 ui 脚本创建 HUD 窗口, 工作线程经 `compose.post` 每秒更新文本, 点击按钮关闭; TextField 在悬浮窗内获得焦点并输入 (需 `requestFocus`); 脚本退出窗口消失.

### P3.5 错误, 探测与守卫

- [x] (宿主) `ComposeError` (`code` / `message` / `nodeId` / `prop` / `cause`), 装载失败消息复用插件中心本地化字符串; 新增字符串 (`error_compose_ui_mode_required`, `error_compose_unknown_component` 等) 10 语言, 按 name 排序.
- [x] (宿主) 守卫: 单引擎并发会话上限 (附录 B.5), 会话关闭后对句柄的操作 -> `SESSION_CLOSED`; 引擎退出时的全部清理有单元测试.
- [x] (测试) JVM `ComposeJsErrorsTest` (code 映射, instanceof, 消息前缀); `ComposeSessionLifecycleTest`.

P3.4 - P3.5 证据 (2026-10-03): `docs/dev/p34-p35-evidence.md`. 宿主相关 JVM 154 项 (生命周期 19, 错误 12, 控制投递与回执 17 等) 与冻结契约 13 项通过. API 24 x86 AVD / Sony API 31 / HyperOS API 35 各 8 项真实非 ui 悬浮脚本通过, 含每秒工作线程更新, raw / 可调整窗口, 真实 IME, 立即 closeAll, 普通队列饱和时原生事件 / 关闭, 控制名额耗尽后的临时窗口清理; 两端额外各 7 项页面回归, Sony 额外 1 项既有窗口 owner 回归, 合计 39 项. 修复脚本 looper 退出后的迟到投递, 丢弃清理队列后的 owner 重试, 初始化失败和名额拒绝的资源释放. 原生控制与普通 post 队列均有界且分离. 宿主 app debug / androidTest / release, 插件 JVM 63 项, 51 项共享依赖及签名 / 对齐 / DEX 门禁通过. P3 全部完成, 下一起点为 P4.1; HyperOS 仍按桌面与 requestFocus 条件验收, 完整矩阵与长期内存测量留在原 P5.

---

## P4: 示例, 无障碍与守卫

### P4.1 示例脚本

- [x] (插件) `assets/examples/`: `counter.js` (state + render), `form.js` (节点句柄层, TextField / Switch / Slider / 校验), `list.js` (LazyColumn 1000 项, key 重排, scrollTo), `floaty-hud.js` (非 ui 脚本悬浮 HUD), `theme.js` (seed / dark / dynamicColor); `index.json` 列表; 每个示例头部注释说明前置条件.
- [x] (宿主) 同步到 `app/src/main/assets-app/sample/` 的 Compose 分类 (核对现有分类命名后决定目录名), 示例目录索引更新.
- [x] (测试) 示例在兼容矩阵至少两台设备运行通过, 作为 P5.1 回归集.

### P4.2 无障碍与选择器

- [x] (宿主 / 插件) 核实 `testTag` 经 `testTagsAsResourceId` 暴露为 `viewIdResourceName` 的确切字符串, 宿主 `id()` / `idContains()` / `idMatches()` 对其匹配结果, `desc()` 对 `contentDescription` 的匹配, `text()` 对 Text 节点的匹配, `click()` 动作在 Button 语义节点上的可用性; 合并语义节点 (Button 内 Text) 的查找路径.
- [x] (文档) 把规则写入 `api/compose.md` "无障碍与选择器" 节与协议文档; 不兼容之处如实记录 (例如 tag 不带包前缀, 与旧 View `id()` 的 `pkg:id/` 匹配不同).
- [x] (测试) instrumentation: 在 ui 模式页面上用宿主 a11y 服务 (`Three Adapt A11y` 或内置) 以 `id('start_button').findOnce().click()` 触发 Compose Button 回调.

### P4.3 守卫测试补全

- [x] (宿主) 目录 / d.ts / 文档三方一致性脚本 (P6 使用): 从 `ComponentCatalog.V1` 生成组件与属性清单, 与 `aj6-int-compose.d.ts` 和 `api/compose.md` 的组件表比对 (`build/tools/compose_catalog_check.py`, 位于宿主 `build/` 临时目录或 `.python/`).
- [x] (插件) instrumentation 全集可在 API 24 x86 与 API 35 x86_64 模拟器上通过; 覆盖率记录.

P4 证据 (2026-10-03): `docs/dev/p4-evidence.md`. 五个实际打包示例与宿主副本逐字节一致, API 24 x86 / API 35 x86_64 各 5 项示例和 2 项真实宿主无障碍选择器测试通过; 插件 instrumentation 全集各 31 项通过, 合计 76 项. 宿主相关 JVM 175, API JVM 16, 插件 JVM 63, Python 守卫 14 项通过. JaCoCo 实测源码行覆盖率 85.93%, 分支 59.60%; 完整记录包含分母与测量范围. 目录工具从真实 V1 导出 30 项 / 114 个规范属性 / 12 个别名, 能生成并严格比对 TS 与文档标记块; P6 正式声明和组件表尚未交付, 缺失时明确失败, 不宣称三方最终文档已通过. 外部文档新增无障碍章节及全量本地生成, 完整 API / 声明 / 离线插件同步仍按 P6. P4 全部完成, 下一起点为 P5.1.


---

## P5: 健壮性, 兼容矩阵, 性能与体积

### P5.1 健壮性

- [x] (测试) 敌意输入: 5001 个节点, 深度 65, 64 KiB + 1 的字符串, 2001 ops 的批, 非法颜色 / 尺寸 / 枚举, 循环子节点引用, 在回调中 `close()` 会话, render 中再次 `mount`, 工作线程直接改 state (应报错或经 `post`), 插件在会话存活期间被卸载 / 更新 (加载器失效 -> `PLUGIN_UNAVAILABLE`, 无崩溃).
- [x] (测试) 快速反复 mount / close 100 次, 停止脚本时 render 进行中, Activity 旋转 (沿用既有配置处理, 实际重建时销毁引擎), 低内存 (`am send-trim-memory`) 后界面仍可更新.
- [x] (宿主 / 插件) 修复发现的问题并补回归测试; 结论记入 `docs/dev/p5-robustness-evidence.md`.

P5.1 证据 (2026-10-03): `docs/dev/p5-robustness-evidence.md`, 插件 build19 / 宿主5317. 宿主相关 JVM189 + API16, 插件 JVM63; API24 / API35 插件各32项通过, 最终宿主矩阵中的健壮性各7项通过. 专用 API24 AVD 真实更新 / 卸载各1项通过, 覆盖 release18 -> 19 和同引擎新loader/JNI重挂. 修复活动会话包失效通知, render内递归挂载, 迟到请求驱逐新loader, attach异常提前结束引擎, Compose1.12祖先context跨loader污染; 公共owner包装保留原宿主Lifecycle/registry. API37插件ComposeTestRule受Espresso已移除反射入口阻断的27项如实记录, 该设备真实宿主矩阵另行通过.

### P5.2 兼容矩阵

- [x] (测试) D28 六台设备 / 模拟器: 装载, 计数器, 表单 (含 IME), 列表, 悬浮窗, 停止清理; API 24 的 Compose 1.12 行为 (本项目 minSdk 24) 与 API 37 (16 KB 页, 按 D22 校验 graphics-path 原生辅助库) 记录; HyperOS 悬浮窗焦点条件记录.
- [x] (测试) inrt 打包 (D30): 打包一个使用 `compose` 的脚本应用, 在已安装 / 未安装插件的设备上运行, 错误提示可理解; 结果写入文档 "打包应用" 节.

P5.2 证据 (2026-10-03): `docs/dev/p5-compatibility-evidence.md`, 宿主5317 / release插件19. 六设备最终110项全部通过, HyperOS显式申请窗口焦点, API37实测16KB页. 真实RuntimeKit经独立APK Builder生成应用版本1 / 内嵌运行时5317的x86_64 APK, 默认应用签名, 不请求插件签名权限; 已安装 / 实际卸载插件两种条件各连续5次通过. 修复由inrt暴露的先启动Activity后登记execution竞态, 登记表并发与快速结束有5项确定性回归, 宿主相关JVM合计200项. APK Builder提交 `4884075` 校正构建别名, JVM38项通过; 外部文档 `7dc1add`, 145模块本地生成与freshness通过. 完整API / 声明 / Offline Docs仍按P6, 无推送或公开发布.

### P5.3 性能与体积

- [x] (测试) 度量并记录 (不预先承诺): 装载首帧 (冷 / 热), 单次 state 更新到界面可见 (ui 模式与 floaty), 100 次连续更新合并后的 render 次数与耗时, 1000 项 LazyColumn 首帧与滚动掉帧, 宿主 PSS 增量, 插件 APK 体积与 dex 方法数; 以此定 1.0.0 的回归阈值写入插件 `AGENTS.md` 第 14 节.
- [x] (宿主 / 插件) 若 render 整树重建成为瓶颈, 先做宿主侧节点属性快照比较 (跳过无变化子树的 ops), 不改脚本 API; 记录是否需要 F.6 的细粒度更新.

P5.3 实测证据 (2026-10-03): `docs/dev/p5-performance-evidence.md` 与紧凑JSON, 宿主测量实现 `472f52e39b` / 插件19. API24 AVD与API35 Pad各118条, 共236条完整记录; 分析工具16项自测通过. 已覆盖冷/暖首帧, UI/floaty更新, 同回调100赋值(所有样本均1次render), 1000项列表首帧/真实滚动帧, 配对PSS与APK/DEX. APK2375069B, 7143类/44721方法引用. 1000节点初始构建成本明显; 既有TreeReconciler已比较属性/Modifier/回调快照并跳过空补丁, 滚动没有重跑JS render, 本轮不据此增加F.6或预先优化. 维护者同日确认Q5, 具体首版复核线及超线复测/说明和维护者确认规则已写入AGENTS第14节, 本项完成. 冷启动/PSS/全部滚动指标继续观察, 待补稳定性证据再定线.

---

## P6: 文档, 声明与 1.0.0 本地 gate

### P6.1 文档

- [x] (文档) `AutoJs6-Documentation/api/compose.md` (模块总览, 两层范式, 线程, 承载面, 主题, 无障碍, 打包应用, 错误) 与类型页 `composeNodeType.md`, `composeStateType.md`, `composeModifierType.md`, `composeSessionType.md`, `composeFloatyWindowType.md`, `composeThemeType.md`, `composeComponents.md` (核心集逐组件属性 / 插槽 / 事件表, 由 P4.3 脚本生成初稿); `sidebar.md` / `toc.md` / `dataTypes.md` / `progress.md` 更新; 运行 `generator/auto-generate-for-autojs6.bat`, 同步 `AutoJs6-Plugin-Offline-Docs`, 按各仓库 AGENTS 提交.

P6.1 证据 (2026-10-03): 文档 `2832a4d` (完整8页及导航), `2de38ab` (同步计数90); 冻结目录30项/114属性与两份真实声明比对通过, 152模块全量生成及freshness通过, 搜索6543条. BAT先dry-run再verify-offline, Offline Docs6.8.6/build71同步208资产, 除5个已有文本文件的规范LF转换外与来源完全一致, JVM2/Python4与debug/release APK内容门禁通过. 完整经过见 `docs/dev/p6-release-gate.md`.

### P6.2 声明

- [x] (文档) `AutoJs6-TypeScript-Declarations/declarations/autojs6/aj6-int-compose.d.ts` (`Internal.Compose`, 元素工厂重载按目录生成, `ComposeNode` / `ComposeState<T>` / `ComposeModifier` / `ComposeSession` / `ComposeFloatyWindow` / `ComposeError`, 事件 payload 类型), `index.d.ts` 引用; `aj6dts.bat -Publish` 后同步两个仓库的 `aj6-int-compose.d.ts`, Ace 仓库执行 `:app:generateAutoJs6LspDeclarations`, 版本号与版本名按 AGENTS 规则加一并提交.

P6.2 证据 (2026-10-03): 声明4.30.0 / `43b29d7`, Ace1.22.0 / build124; 实际Publish生成并同步配套宿主Java/资源/库, 手写声明镜像与30/114目录通过. TypeScript5.1.3正例及39负例+2项exact-nullish用例通过, 新声明本体不跳过检查也无诊断. 实际TS6.0.3语言服务默认/全部声明组均验证29工厂, 签名与错误输入诊断, 新回归接入原verifier; Ace JVM171与debug/原生对齐通过. 未开启额外默认声明组或改变运行时API, 未触碰Ace预存未跟踪releases目录. 详见 `docs/dev/p6-release-gate.md`.

### P6.3 README 与 changelog

- [x] (插件) `.readme` 10 语言: 简介, 功能 (两层范式, 承载面, 核心集), 安装 (插件中心), 快速开始 (计数器 + 悬浮 HUD), 兼容性 (宿主最低版本, Android 7.0+, 打包应用需安装插件), 常见问题 (为什么不能直接调用 Compose 函数, 旋转后状态, 选择器 tag 规则), 发行历史, 许可证; `.changelog` 1.0.0 条目; 生成并 `--check`.
- [x] (宿主) `.changelog` 条目最终化 (P1.3 的条目补齐 floaty / 主题 / 示例); 协议文档状态改为 "frozen V1 with host commit ...".

P6.3 证据 (2026-10-03): 插件10语言/36产物与宿主10语言历史最终化, 协议两份一致并标记host `ec34abc00f`; 20段本地化README脚本语法通过, HUD关闭停止worker并保护迟到更新, 五个既有实测示例字节未改. 保留本地预览/正式图案待源图/D7事实, 删除阶段性实现过程和未经测量的延迟泛化. 详见 `docs/dev/p6-release-gate.md`.

### P6.4 本地发布 gate

- [x] (发布) 插件: `py .python/generate_markdown.py --check`, `testDebugUnitTest`, `assembleDebug assembleDebugAndroidTest lintDebug`, `connectedDebugAndroidTest` (至少一台), `appendDigestToReleasedFiles` 产出 1 个已签名 APK 且 CRC32 一致; 宿主: 相关 JVM 测试 + `assembleDebug`; 全部结果与未执行项写入 `docs/dev/p6-release-gate.md`.
- [x] (发布) 按 D7 不推送, 不登记索引, 不发 Release; 在本文件 "会话记录" 写明 gate 达成的提交 hash, 等待维护者恢复推送后再做远端步骤 (登记 `official-repositories.json`, Release, 索引生成).

P6.4 证据 (2026-10-04): 插件build26签名APK `autojs6-plugin-compose-ui-v1.0.0-d9cdec3c.apk`, 2378609B, 7143类/44721方法引用, 未超过已批准Q5复核线. JVM63, 独立API35 x86_64 AVD的connectedDebugAndroidTest32, 最终release宿主实际五例/JNI/INFO共7项通过; debug lint无问题, 签名/CRC32/依赖归属/四ABI原生对齐通过. 宿主JVM200/API16与debug装配通过. 配套文档/声明/插件验收和未执行项见 `docs/dev/p6-release-gate.md`; 两类图标资源有效, 图案仍保留占位源图. AVD和自有包已清理, 未操作远端.

---

## P7: 1.1.0 候选

以下条目在 1.0.0 gate 后按维护者已确认的 Q7 顺序推进, 详细说明见附录 F.

- [x] (宿主 / 文档) F.1 TSX 工厂: `__autojs6Tsx` 按 `type` 路由到 `compose.createElement`, `aj6-jsx-element-extension.d.ts` 增补 Compose 元素类型, TypeScript Engine 插件配合.
- [x] (宿主 / 插件) F.2 与旧 ui 混合: XML `<compose>` 容器 (`ui.registerWidget` 同机制) 与 Compose `AndroidView` 节点 (以 `ui.inflate` 结果或 View 工厂作为 `view` 属性, 主线程创建).
- [x] (宿主 / 插件) F.3 `compose.dialog(nodeOrRender, options)`: 任意脚本弹出 Material 3 Dialog / ModalBottomSheet, 返回 Promise 或会话.
- [x] (插件 / 宿主 / 文档) F.4 宽集组件, 契约 `CONTRACT_VERSION = 2` (只追加).
- [x] (插件) F.5 组件画廊 Activity (遵循独立设置页与图标规范). 2026-10-08 复核曾因 D8 与 Q6 搁置; 同日维护者明确排期并接受包体积增量, D8 修订为允许画廊与设置页, 启动器图标暂用 Q6 占位图稿生成, 正式图稿到位后只替换图稿. 证据见下方 F.5 证据与 `docs/dev/p7-f5-evidence.md`.
- [x] (宿主) F.6 细粒度更新与脚本侧依赖裁剪 (按 P5.3 数据决定).

F.1 证据 (2026-10-04): `docs/dev/p7-tsx-evidence.md`. 宿主 5319 支持真实组件工厂引用 / 别名及 `compose:Name`, 有界中立 Fragment 保留根 / 插槽 / 跨脚本身份限制, XML 混域在替换页面前拒绝. 声明 4.31.0 的 29 项 JSX 映射由冻结目录生成, Ace 1.23.0 与 TypeScript Engine 0.6.4 使用实际配套声明; Engine 保留公开 2.1.3 tarball 锁, 明示本地补充来源. 宿主相关 JVM 173 + API 16, 插件 JVM 63, 目录 Python 16, Engine 相关 JVM 534 + 构建逻辑 4, Ace JVM 171 通过. API 35 x86_64 自建 AVD 上, 最终签名编译器 / 渲染器通过 6 项真实 TSX 与 5 项原有 JS 示例, 编译器 debug 声明清单另 2 项通过. 文档 152 模块 / 6548 搜索条目与 Offline Docs 同步 / 签名门禁通过. 插件 1.1.0 / 27 为本地预览, 普通工厂最低宿主 5316 不变; JSX 表达式精度与 namespace 属性补全限制已明确记录. 自建 AVD 与 5 个测试安装均已清理, 下一起点为 F.2.


F.2 证据 (2026-10-07): `docs/dev/p7-interop-evidence.md`. XML `<compose>` / `compose.attach` 与 AndroidView 借用 View / 主线程工厂均已交付, 包含多容器, 旧悬浮窗, 生命周期清理及失败回滚. 可选 interop 能力独立协商, 冻结 V1 的 197 个保留 class 字节相同; 新旧宿主 / 渲染器双向验证通过. 宿主相关 JVM 197/API21, 插件 JVM64/设备40, 目录 Python19, Engine相关 JVM534/构建逻辑4, Ace JVM171及实际语言服务通过. API24/35各6个混合场景, 最终签名编译器/渲染器7个真实TSX场景通过. 配套声明4.33.0, Engine0.6.5, Ace1.26.0, 文档156模块与Offline Docs212文件完成本地同步. 插件1.1.0/build30, APK2472197B/DEX45285引用低于Q5复核线. 两台自建AVD及8个测试安装已清理, 下一起点为F.3.


F.3 证据 (2026-10-07): `docs/dev/p7-dialog-evidence.md`. compose.dialog 返回原会话, 支持 alert / bottomSheet, cancelable 与 theme; UI 使用真实绑定 Activity, 非 UI 复用悬浮窗权限与保活. 独立 dialog-v1 仅附加3个共享类, 原206类字节不变. 锁定 Material3 原 ModalBottomSheet 无公开 overlay 类型入口, 本节明确使用公开 Compose Dialog + M3 BottomSheetScaffold/SheetState/scrim 组合, 未改变路线图条目或依赖版本. 宿主 JVM214/API23, 插件JVM64/API35设备49与API24弹窗9, 宿主两设备各6个弹窗场景, 两设备权限负例, 最终宿主20项回归和签名包8个TSX+5个JS示例通过. 声明4.34.0, Engine0.6.6, Ace1.28.0, 文档与Offline Docs完成本地配套. 插件1.1.0/build31, 2545465B/46345方法引用低于Q5线, 下一起点F.4.


F.4 证据 (2026-10-07 - 2026-10-08): `docs/dev/p7-wide-evidence.md`. 19 类宽集组件与 6 个配套 item 节点已实现, 契约 V2 的基础目录为 55 项, 协商 AndroidView 后为 56 项. 原 209 个共享 class 中 208 个字节相同, 仅 ComposeUiContract 的版本上限改为 2, 另追加 6 个 V2 类; 匹配宿主最低 5322, 保留实际 V1 provider 兼容性. 最终插件 1.1.0/build33 的 JVM 70 项, API24/35 各完整 64 项, 宿主 JVM 224/API 29 与两设备各完整 29 场景通过. 签名包 ffee2730 为 3022152 B/53842 方法引用; 维护者于 2026-10-07 接受 d4f266d2 的 F.4 功能增长 (3022148 B/53838 引用), 最终原生输入守卫及元数据/计数对齐带来的 +4 B/+4 引用另行记录, 不改原 Q5 门槛或 236 条 P5 观测. Declarations 4.35.0, 文档 6.8.0/96 与 Offline Docs 6.8.6/build81 已完成本地来源与内容检查. Engine 0.6.7/build89 与 Ace 1.29.0/build143 的最终 JVM, 完整编译/语言服务及签名门禁均通过; 最终签名组合的9个TSX和5个原JS示例共14项通过. 各仓库按逻辑本地提交, 两台专用AVD与10个自有安装已清理, 本节完成.


F.5 证据 (2026-10-08): `docs/dev/p7-f5-evidence.md`. 画廊 (`app.GalleryActivity`, 启动器 alias 目标) 以 9 个分类展示 56 条示例 (55 个节点组件与仅命令的 Snackbar), 每条有 Material 3 实时预览与生成的 `"ui";` 脚本, 可复制到剪贴板或经宿主公开的 `RunIntentActivity` 交给已安装的 AutoJs6 运行, 画廊自身不执行脚本. 设置页按独立设置规范提供语言 / 夜间模式 / 主题色 / 启动器图标与关于分组, 前三项默认经 `AutoJs6HostSettingsContract` Provider 跟随 AutoJs6, 宿主缺失时回退系统; 选择器先改草稿, 确定才保存. 启动器图标为四个固定 alias (自适应亮 / 暗 / 自动与透明), 默认自动, 包更新后归一化; Icon Studio recipe 改为 `launcher: true` 且底色 #FAFAFA / #212121. D32 打包 36 项宿主锁定版本的 AndroidX 运行时副本并附加 material-color-utilities 4.1.1, 宿主内仍 parent-first 使用宿主类; 渲染器, 契约 V2 与最低宿主 5322 不变. JVM 84 项通过; lint 0 错误 / 10 警告 (Icon Studio 生成的同内容图标与 localeConfig 的 API 级别提示, 与其他独立插件相同); `verifySharedClasspath` 51 项中 36 项打包; `verify_apk_classpath.py` 对 debug 与签名包通过. 专用 API35 x86_64 AVD: 插件 完整 73 项 instrumentation 通过 (含新增 GalleryDeviceTest 3 / SettingsDeviceTest 4 / LauncherIconDeviceTest 2 项), 另在同一 AVD 安装常规包宿主 `org.autojs.autojs6` (6.8.0 / 5325 debug) 后复跑画廊与设置 7 项通过 (运行 Intent 可解析, 外观 Provider 可读); 隔离宿主 `org.autojs.autojs6.compose.spike` (master `b2404d3ae9`, 5325) 的 compose 设备包 74 项中 61 项通过, 13 项为需显式参数的性能 / 旧 provider / 打包应用 / 包变更 / 权限负例阶段按假设跳过; 首轮 5 个示例场景因宿主工作树的示例资产在合并检出时被 autocrlf 转为 CRLF 而字节不等, 经 `sync_examples.py` 回写 LF 并在宿主 `.gitattributes` 钉死 `eol=lf` (宿主 `de11353cc0`) 后通过, 2 个无障碍场景在启用隔离宿主的 AccessibilityServiceUsher 后通过; TSX 10 项与 F.2 / F.3 / F.4 / F.6 场景均使用 build40 渲染器 (与 build42 的渲染器字节相同, build42 只改动插件进程的图标归一化与测试). 签名包 `autojs6-plugin-compose-ui-v1.1.0-2f1127ce.apk` 为 3624288 B, 10473 类 / 60613 DEX 方法引用, SHA-256 `505ea68bb2a23ff81d01ad0735048fa5835fc9ac36d12c503b137c1290c35992`, 较 build38 +600552 B / +6771 方法引用, 记入 `docs/dev/p7-f5-size-review.json`; 维护者于 2026-10-08 预先接受该增量, 原 Q5 复核线不变且仍被超出. 未执行: 真机与 API24 本地 instrumentation (远端 CI 的 API24 作业覆盖同一套测试), 第三方启动器截图, 六设备矩阵, 宿主全量 JVM / lint.

F.5 补正 (2026-10-08, build44): 维护者在 Xperia XQ-DQ72 (API 33) 与自有 AVD 上发现启动器图标可见但点击无法进入. 设备 logcat 为 `NoClassDefFoundError: androidx.arch.core.executor.ArchTaskExecutor` (`LifecycleRegistry.addObserver` <- `ComponentActivity.<init>` <- `GalleryActivity`): build42 的 36 项集合只收录 arch `core-common`, 未收录 `core-runtime`; 对收缩后的发布包做引用闭包分析, 除契约类外还缺 `customview-poolingcontainer` (ComposeView 释放策略), `emoji2` (Compose 文本) 与 `androidx.window` (WindowInfo.containerSize). 设备测试没有发现, 是因为测试 APK 携带整份共享锁的运行时副本, 与画廊同进程而掩盖了缺口. 修正: `standaloneRuntime` 扩为 47 项 (补入 core-runtime / poolingcontainer / emoji2 / lifecycle-process / window x3, 以及完整 keep 后被保留的 savedstate / lifecycle 成员引用的 lifecycle-livedata-core / kotlinx-serialization x3; 共享锁 49 项非 Kotlin 构件只剩无人引用的 appcompat x2 不打包); 首个候选包只对新增包 `-keepnames`, 在隔离宿主的设备套件中 7 项 RENDER_FAILED 并最终进程崩溃: R8 由 "插件程序从不初始化 EmojiCompat" 推断 `EmojiCompat.get()` 永不正常返回, 把 Compose `EmojiCompatStatus.getFontLoadState` 编译成 `EmojiCompat.get(); throw null`, 宿主内 (EmojiCompat 已初始化) 每次文本布局都抛 NullPointerException; 因此共享包改为与 `kotlin.**` 一样完整 `-keep`, 代价是这些包不再裁剪; `debugAndroidTestRuntimeClasspath` 同样排除 6 项宿主独有构件且 `verifySharedClasspath` 守卫; `verify_apk_classpath.py --shrunk` 要求发布包引用的每个类都在 APK, 平台或契约中 (旧包在此检查下报 15 个不可加载的类); 新增 `verify_standalone_launch.py` 在设备上安装 APK, 从已启用 alias 启动画廊, 经无障碍树打开首个条目与设置页并确认进程存活 (旧包在该检查下复现崩溃), 本地验证顺序与 CI 两个模拟器作业在 instrumentation 后执行. 验证 (自有 API35 x86_64 AVD `compose_f5c_api35`, 维护者 Icon Studio 的未提交改动在主工作树中, 构建与测试在独立 worktree 进行): JVM 84 项通过, lint 0 错误 / 9 警告, `verifySharedClasspath` 51 项中 47 项打包 / appcompat 2 项不入测试 APK, `verify_apk_classpath.py` debug 与 `--shrunk` release 通过 (未解析引用 0), debug 与签名 release 的启动检查均通过 (画廊 / Column 条目 / 设置页), 插件 instrumentation 73 项全部通过 (0 失败 / 0 跳过; 测试 APK 已不携带宿主独有构件); 隔离宿主 `org.autojs.autojs6.compose.spike` (宿主 master `910aec9495`, 独立 worktree 构建) 的 compose 设备包 87 项中 71 项通过, 15 项按假设跳过 (需显式参数的性能 / 旧 provider / 打包应用 / 包变更 / 权限负例阶段, 小米 IME 与旧宿主 V2 门禁), 1 项失败. 10 项 TSX 场景在完整运行中因新建 AVD 未安装 TypeScript Engine 而失败, 临时安装 0.6.8 后复跑 10 项全部通过 (计入 71 项); 唯一失败 ComposeUiLoaderTest.loaderCachesByIdentityButRechecksEnablementAndSharesParentClasses 断言 contractVersion() 等于 1 (宿主测试 2026-10-02 写入, 早于 F.4 把契约提升为 2), 属宿主侧既有过期断言, 与本次改动无关; 本轮 87 项比 F.5 当日的 74 项多出 core/plugin/compose 装载套件 13 项. 首个仅 -keepnames 的候选包在同一套件 7 项 RENDER_FAILED 并最终宿主进程崩溃, 完整 keep 后不再出现. 签名包 `autojs6-plugin-compose-ui-v1.1.0-5d2ce3ef.apk` 为 4673146 B, 13482 类 / 82466 DEX 方法引用, SHA-256 `9856d4f57b361d7597cfcee39c9e50b5466cca644029a6081127c057e2890dd7`, 较 build42 +1048858 B / +21853 方法引用, 仍在维护者预先接受的 F.5 增量之内, 记入 `docs/dev/p7-f5-size-review.json`.

F.6 证据 (2026-10-08): `docs/dev/p7-f6-evidence.md`. P5.3 未测的 "大树挂载期间单次 state 更新" 经宿主测量工具 `list-updates` 补测 (宿主 `51b7760c49`, 专用 API35 x86_64 AVD, 插件 build36): 1000 行渲染层可见更新 722.50 / 732.29 ms (中位 / p90), 其中 JS 重建 483 ms, 句柄层 156.44 / 178.53 ms, 据此实施. 宿主 `1adc2b8639` (5323) 交付 `compose.memo(fn, deps, options?)`: 渲染期专用, 按渲染函数或外层片段作用域内的 key / 调用次序及函数字面量匹配, 依赖值与所读 state 版本均未变且节点仍在上一棵树时复用, 失败帧保留, 拒绝帧清空, 256 项依赖 / 每次渲染 5000 次上限. 同一 AVD 的 memo 行为 307.22 / 322.22 ms (渲染层 752.78 / 791.42, 句柄层 155.81 / 173.66), 构建耗时由 489.53 降至 99.94 ms; 单 AVD 决策观测, 不替代 Q5 基线. 宿主 JVM 208 + 契约 29, 设备 UI 8 项 (含 300 行 memo) 及签名 Engine 0.6.8 下 10 个 TSX (含 memo) + 5 个 JS 示例共 15 项通过. 配套: Declarations 4.36.0 `5102e03`, Ace 1.30.0/144 `6748217`, Engine 0.6.8/90 `27e6100`, 文档 6.8.0/97 `a8f7972` `b882abc`, Offline Docs 6.8.6/82 `918e881`, 各自门禁通过. 插件渲染器, 契约与最低宿主 5322 不变; build38 签名包 `2740f0ee` 为 3023736 B (较 F.4 最终包 +1584 B 文本资源) / 53842 方法引用不变, 记入 `docs/dev/p7-f6-size-review.json`, 维护者于 2026-10-08 接受该增量, 原 Q5 复核线不变. 未执行: 六设备矩阵, API24, inrt memo, 宿主全量 JVM/lint, 新 Q5 基线与远端 CI.

---

## P8: 可视化设计器 (1.2.0)

目标: 插件独立应用从 "活的文档" 变为创作工具: 在插件自身进程内, 用真实渲染器所见即所得地编辑 compose 脚本的界面树, 以脚本文件为工程, 经 Rhino `Parser` 解析回树, 产物交给宿主运行, 编辑或导入. 脚本 API, 契约版本与宿主内渲染路径不变.

需求由维护者于 2026-10-13 分四轮拍板, 结论写入 D33 - D40 并修订 D8 / D26 / D32; Q8 - Q11 已于同日拍板 (见附录 D). 本阶段依赖 P7 全部完成 (1.1.0 本地 gate); 建议顺序 P8.1 -> P8.2 -> P8.3 -> P8.4 -> P8.5, 其中 P8.2 的解析 / 生成与 P8.3 的界面可在 P8.1 完成后并行; P8.4 的 (宿主) 条目由维护者并行协调, 不阻塞其它条目.

### P8.0 改名为 3-Block Compose (2026-10-13)

- [x] (插件 / 宿主) 维护者在 Q9 拍板: 产品与设计器同名 `3-Block Compose`, 仓库 `AutoJs6-Plugin-Three-Block-Compose`, 并入 Three 系列 (修订 D1, 身份表见 4.4 与 `AGENTS.md` 第 2 节). 按 `AUTOJS6_PLUGIN_THREE_SERIES_RENAME_AGENTS.md` 执行: 包目录 `git mv`, 身份类 `ThreeBlockComposePlugin` / `ThreeBlockComposeInfoService` / `ThreeBlockComposeRuntimeInfo` / `ThreeBlockComposePluginInfo` 及测试改名, 有序字符串替换 (122 个文件), 版本 1.2.0 / build 100, 10 语言 changelog 1.2.0 条目与 README 状态文案, 维护者正式图稿 (2048 x 2048) 经 Icon Studio 重新生成 24 项资源. 宿主 `f12665ffde`: 契约 `ComposeUiIds.PLUGIN_PACKAGE_NAME` / `PLUGIN_ID` 与快照, 向导条目, 11 语言错误字符串, 控制台前缀与会话警告, 示例分类 `sample/3-Block Compose`, 相关 JVM / 设备测试, zh-Hans 6.8.0 changelog; 契约 AAR 重生并换锁 (`557b7efc...`). 保留不改的机制名称与历史记录见 `AGENTS.md` 第 2 节 "改名记录".
- [x] (发布 / 文档) 同日完成: GitHub 仓库经 `gh repo rename` 改为 `SuperMonster003/AutoJs6-Plugin-Three-Block-Compose` (仓库 ID `R_kgDOU7fuQA` 不变, 描述改为功能性一句话 "Jetpack Compose and Material 3 user interface renderer plugin for AutoJs6 scripts"), 本地 `origin` 与目录同步改名; 官方索引 `3b8b307` 把 catalog-icons 图稿登记换到新包与新图稿 (`icons/io.github.supermonster003.autojs6.plugin.three.block.compose/` 日 / 夜 PNG, 旧包目录删除, README 文案同步, 75 项工具测试通过); 文档站 `b3f5f1c` (compose 参考页, ui / dialogs 交叉引用, 侧栏与目录改为新名, 157 模块重新生成并 `--check`), TypeScript Declarations `ce6a3e9` 与 Ace Editor `f63bb98` (仅未发布版本的 changelog 条目), Offline Docs `1bb5d8e` (同步 157 个 HTML, `source_base_commit` 指向 `b3f5f1c`, 元数据重新生成). 插件, 宿主, 索引与文档仓库的提交均未推送.

P8.0 证据 (2026-10-13): 插件 `7dbc3b2` (build 100): JVM 84 项通过, `verifySharedClasspath`, debug / androidTest / release 装配, lint 0 错误 12 警告 (既有), `verify_apk_classpath.py` debug 与 `--shrunk` release 通过 (release 4664030 B), `generate_markdown.py --check` 36 项, `generate_icon_studio.py --check` 24 项; Xiaomi Pad 23046RP50C (API 35) 上 11 个 instrumentation 类 74 项全部通过, `verify_standalone_launch.py` 以 release APK 从 `AdaptiveAutoIconAlias` 启动画廊首条目与设置页通过. 宿主 `f12665ffde`: 契约模块 JVM 测试 (含 V1 快照) 与 `PluginCenterComposeUiRegistrationTest` / `ComposeExampleCatalogTest` / `ComposeJsErrorsTest` 共 18 项通过, `sync_examples.py --check` 5 脚本 + 1 清单字节一致. 未执行: 宿主侧完整设备矩阵与 compose 设备测试 (打包集合与 R8 规则未变, 仅字面量改动), 其它四台真机与 API 24 AVD 的插件 instrumentation, 远端 CI (未推送).

### P8.1 依赖边界: 契约与解析器打包

- [x] (插件) `compose-ui-api.aar` 由 `compileOnly` 改为 `implementation` (D34, 修订 D26): 与 D32 的共享副本同样完整 `-keep` 并保持 ABI, 宿主内 parent-first 装载继续使用宿主副本, 渲染器行为与版本协商不变; `locks/host-api-aars.lock` 不变. `:app:verifySharedClasspath` 与 `.python/verify_apk_classpath.py` 的 "不含契约类" 断言反转为 "含契约类, `--shrunk` 闭包可由 APK 内副本满足"; 设备测试 APK 不再需要单独携带契约 AAR.
- [x] (插件) Rhino 解析器 (D35): 从宿主 `slimRhinoJar` 产物复制 `libs/org-mozilla-rhino-2_0_0-SNAPSHOT-slim.jar` (约 2.2 MB, 已去掉 JLine / optimizer / JFR), 新建 `locks/rhino-jar.lock` 记录 SHA-256 与来源宿主提交, 校验方式同契约 AAR; `implementation` 引入, 只允许 `designer/parse` 包引用 (JVM 守卫测试扫描其它包不得 import `org.mozilla.javascript`); R8 不加 keep, 允许裁剪到 Parser / AST 闭包; 宿主内该副本永不被触及 (渲染器不引用), 与宿主 Rhino 同名类的遮蔽关系与 D32 一致.
- [x] (插件) 只解析不执行 (维持 D8 的 "不执行脚本"): 经 `CompilerEnvirons` + `Parser` 直接得到 AST, 永不 `Context.enter`, 不编译, 不求值; 语言版本与宿主引擎一致, 开启注释记录以保留不透明段原文.
- [x] (插件) `AGENTS.md` 第 5.2 / 12 / 13 节与 README 兼容性段同步: 独立界面自 P8 起可引用契约类; 契约 AAR 与 Rhino jar 的版本纪律; 体积预算按 Q8 回填.
- [x] (测试) JVM: 锁文件与 jar 一致性, 包引用守卫; 本仓库 `verify_apk_classpath.py` (debug 与 `--shrunk`) 通过; 宿主侧: 打包集合与 R8 规则变化, 按 `AGENTS.md` 13.2 的 MUST 重跑宿主侧完整设备矩阵, 证明 parent-first 装载与渲染器不受影响 (证据等级同 P5.2). 本轮只覆盖一台 API 35 真机, 完整矩阵见证据中的 "未执行".

P8.1 证据 (2026-10-13): `compose-ui-api.aar` 改为 `implementation` 并由既有 `-keep class org.autojs.plugin.compose.api.** { *; }` 完整保留, 设备测试 APK 不再单独携带契约; `locks/rhino-jar.lock` (SHA-256 `cebd05fa...`, 来源宿主 `f12665ffde`) 锁定宿主 `:app:slimRhinoJar` 产物 (2161825 B), `implementation` 引入, R8 不 keep 并 `-dontwarn org.mozilla.**`; `verify_apk_classpath.py` 改为要求契约类在 APK 内且 `--shrunk` 无任何 APK / 平台之外的引用. 新增 `RhinoJarLockTest` (4) 与 `RhinoImportGuardTest` (2), JVM 共 90 项通过; lint 0 错误 12 警告 (既有); debug APK 13559171 B (契约 233 类, Rhino 1811 类), release APK 4835611 B (契约 230 类, Rhino 0 类, unresolved 0), 较 P8.0 的 4664030 B 增加 171581 B (Q8 预算内); `verifySharedClasspath`, Markdown 36 项与图标 24 项 `--check` 通过; changelog 1.2.0 增加 dependency / improvement 条目, README 兼容性要点增加契约打包说明 (10 语言). 设备: Xiaomi Pad 23046RP50C (API 35): 插件 instrumentation 74 项通过 (debug, build 101), `verify_standalone_launch.py` 以 release APK 通过; 宿主侧以 `-PcomposeUiSpike=true` 隔离宿主 (工作树含 `f12665ffde`) 配合已安装的 release 插件运行 6 个 compose 设备测试类共 46 项: 41 项通过, 5 项按既有 assumption 跳过 (需要存档的旧版 V1 / 对话框前渲染器 build 27, 需要悬浮窗权限被拒绝的否定阶段, 需要显式选择加入的包变更阶段), 0 失败; 悬浮窗相关用例在手动授予 SYSTEM_ALERT_WINDOW 后通过, 随后卸载 spike 宿主与测试包, 插件 release 保留在设备上. 未执行: 其余五台设备与 API 24 AVD 的插件与宿主设备测试 (完整矩阵), 远端 CI (未推送); 这些在 P8.5 gate 前补齐.

### P8.2 模型, 命令, 解析与生成

- [x] (插件) `designer/model`: 不可变 `DesignDocument` (承载 `mount` / `floaty` / `dialog` 与各自 options, 根节点, `compose.state` 声明表, 前后不透明代码段), `DesignNode` (`type`, `key`, `props`: 字面量 `UiValue` / 不透明表达式 / state 绑定三态, `modifiers: List<ModifierEntry>` 有序链, `children`, `slots`, `handlers`: 事件名 -> 处理函数源码); 一切编辑经 `EditCommand` (插入 / 删除 / 移动 / 改属性 / 改 modifier / 绑定 state / 改处理函数 / 改承载选项) 进入撤销 / 重做栈 (D40), 连续修改同一属性可合并为一步, 每个命令有逆命令.
- [x] (插件) 校验由契约目录驱动 (D36): `ComponentCatalog` V2 的 `PropSpec` 类型 / 枚举 / 必填 / 默认值, `childrenPolicy`, `allowedParents`, `scope` 与 `modifiers` 列表决定可放置性与属性表单; 上限引用 `ComposeUiLimits` (节点数, 深度, 列表项); 违反项在大纲与抽屉标红但不阻止保存, 导出时汇总提示.
- [x] (插件) `designer/parse` (D35): 识别 `"ui";` 指令, `compose.mount | floaty | dialog(nodeOrRender, options)` (render 为 `function () { return <tree>; }` 或箭头函数, 直接传节点亦可), `let / const / var x = compose.state(init)`, 节点工厂 `compose.X({props}, child, ...)` / `compose.X({props}, [children])` / `compose.X(child, ...)`, 字符串子节点, 快捷属性与 `compose.modifier().a().b()` 链, `key`, 事件属性的函数表达式 (源码原样保留), `state.value` 读写与处理函数内对 state 的赋值 (识别为绑定). 其余顶层语句按源码范围原样保留为不透明段, 属性值中无法映射为字面量的表达式保留为不透明表达式; 结构上无法映射 (条件 / 循环生成子节点, 非 compose 工厂调用作为节点等) 时抛 `DesignerParseError(line, column, reason)` 并拒绝打开, 不静默丢弃.
- [x] (插件) `designer/codegen`: 由模型生成脚本, 风格与画廊 `GalleryNode.render` 一致 (4 空格缩进, 单引号, 子节点换行), 不透明段与处理函数源码原样写回; 往返稳定性为硬要求: `parse(codegen(doc)) == doc`, 且 `codegen(parse(script))` 对设计器生成的脚本幂等.
- [x] (测试) JVM: 56 条画廊示例与 `assets/examples/` 5 个示例全部解析成功并往返; 错误定位用例 (每类拒绝原因至少 1 条); 命令栈不变量 (撤销后模型逐字段相等, 合并命令只占一步); 目录驱动校验与 `GalleryCatalogTest` 共用真实目录.

P8.2 证据 (2026-10-13): 包 `designer/model` (DesignModel.kt: JsLiteral / PropValue 三态 / HandlerSource / ModifierEntry / NodeEntry 有序条目 / DesignChild / DesignNode / MountDecl / Segment / DesignDocument, 节点 id 文档内唯一; EditCommand.kt: ReplaceNode / SetEntries / SetProp / SetKey / SetSlot / SetHandler / SetModifiers / InsertChild / RemoveChild / RemoveNode / MoveChild / SetHost / SetRenderPrelude / SetStates 与 EditHistory 的合并 / seal / undo / redo; DesignValidator.kt: JsValueConverter 按宿主 ComposeScriptValues 的顺序转换字面量, 校验器按 `ComposeUiV2.interopCatalog` 检查属性 / 别名 / 快捷属性 / 插槽 / 事件 / 修饰链签名与作用域 / 子节点策略 / V2 父子规则 / 重复 key / 契约上限, 命名颜色与图片为警告), `designer/parse` (ScriptReader.kt: Rhino `CompilerEnvirons` + `Parser` + `ErrorCollector`, ideMode, ES6; 识别顶层 state 声明与唯一挂载, 其余按源码范围不透明保留; 渲染前置语句从行首切片后统一去缩进), `designer/codegen` (ScriptWriter.kt). JVM 新增 4 个测试类 28 项, 全仓 116 项通过; 56 条画廊脚本中绝大多数首次生成即为不动点, 5 个示例 (含节点句柄写法, 悬浮窗选项表达式, 渲染前置循环) 均往返相等. release APK 4839799 B (设计器尚未被任何入口引用, R8 裁掉了设计器与 Rhino 类, release 内各 0 个; P8.3 接入界面后再记录实际增量), lint 0 错误. 设计器界面 (P8.3) 尚未开始, 本小节不涉及设备验证.

### P8.3 设计器界面 (插件进程)

- [x] (插件) 导航 (D38, 修订 D8): 四个启动器 alias 的目标改为 `DesignerHomeActivity` (工程列表: 新建 / 最近 / 私有目录工程 / 从文件导入), 画廊与设置从首页顶部栏进入; `verify_standalone_launch.py` 改为启动首页, 再进入画廊首条目与设置页; `GalleryDeviceTest` / `LauncherIconDeviceTest` 相应调整.
- [x] (插件) `DesignerActivity` 四区 (D36): 预览画布 (真渲染器: `designer/preview` 把 `DesignDocument` 派生为 `RenderFrame`, 复用 `RenderNode` / `ModifierMapper` / `ThemeMapper`; 命令注册表, Snackbar 与文本控制器为设计期本地实现, 事件只用于选中, 不回调脚本); 大纲树 (多级折叠, 拖拽重排与换父, 与画布选中同步); 组件面板 (底部弹层, 按画廊 9 个分类列出 55 个组件, 目录驱动); 属性抽屉 (目录驱动表单: 文本 / 数字 / 布尔 / 颜色选择 / dp / sp / 枚举 / 图标名 / 形状 / 文本样式各自编辑器; 不透明表达式以代码框显示并可改回字面量; state 绑定下拉 (D37); 事件列表与处理函数多行源码编辑 (D37, 纯文本, 保存时经 Rhino `Parser` 做语法检查); modifier 链有序列表: 增删, 拖动排序, 逐项参数编辑, 作用域项按父容器过滤).
- [x] (插件) 画布交互: 选中高亮 (overlay 边框与名称标签), 点击选中, 长按拖动至落点 (以 `onGloballyPositioned` 记录各节点边界, 落点按 `childrenPolicy` / `allowedParents` / `scope` 判定并高亮合法容器与插入位置); 画布缩放, 亮 / 暗与主题色预览跟随设置页; 弹层类组件 (AlertDialog / ModalBottomSheet / DropdownMenu / DatePicker / TimePicker / Tooltip / Snackbar) 以 "展开" 开关或占位卡片呈现, 不弹真实窗口.
- [x] (插件) 承载选项编辑: `mount` options, `floaty` (x / y / raw / 可触摸 / 焦点等, 与附录 A.6 一致), `dialog` (alert / bottomSheet, cancelable, theme); 撤销 / 重做与草稿自动保存 (D40): 草稿按工程存私有目录 `drafts/`, 编辑停顿与 `onStop` 时写入, 进程被杀后重开可恢复; 显式保存才写脚本文件.
- [x] (插件) 字符串 10 语言, 半角标点 (第 10 节); 设计器页面名按 Q9; 无障碍: 大纲, 面板与抽屉控件有 contentDescription / testTag.
- [x] (测试) instrumentation (API 24 x86 与 API 35 x86_64 AVD, 至少一台真机): 新建工程 -> 拖入 Column / Text / Button -> 改属性与 modifier -> 预览更新 -> 保存 -> 重开往返一致; 56 条画廊示例逐一导入并渲染; 大纲拖拽换父; 撤销 / 重做; 杀进程后草稿恢复; `verify_standalone_launch.py` 覆盖首页 / 画廊 / 设置.


P8.3 证据 (2026-10-13): 包 `designer/preview` (PreviewFrames.kt: 文档 -> `RenderFrame` / `UiTree`, 字面量经 `JsValueConverter`, state 绑定取初值, 文本类属性的表达式显示源码, 不透明子节点 / 插槽与不可渲染组件为灰色占位 Text (id 自 1000000 起并映射回所属节点), 打开态的 AlertDialog / ModalBottomSheet / DropdownMenu / Tooltip 以内联 Card 呈现而不弹真实窗口, 重复 key 降级为无 key; DesignCanvas.kt: `ThemeMapper.Content` + `RendererWindowContext` + `RenderNode` 真渲染器, `LocalNodeDecorator` (renderer/NodeDecorator.kt, 宿主内为 null) 记录节点边界, overlay 独占指针: 点选, 长按拖至合法容器并高亮, 选中框与 `类型 #key` 名称标签; 画布为与视口同大的有界逻辑屏幕, 缩放经 `LocalDensity` 放大并可双向平移), `designer/storage` (ProjectStore.kt: `filesDir/designer/projects/*.js` 与 `drafts/`, 新建 / 读写 / 删除 / 重命名 / SAF 导入 / 模板), `designer/model/NodeTemplates.kt` (55 个组件的默认模板, 可插入集合不含 AndroidView 与 Snackbar, 子节点接受规则), `designer/ui` (DesignerHomeActivity: 启动器目标, 工程列表 / 新建 / 导入 / 删除 / 画廊 / 设置; DesignerActivity: 顶栏撤销 / 重做 / 保存 / 菜单 (承载选项, 问题, 缩放, 复制, 发送运行, 画廊), 画布 (权重 1.1) 与大纲 / 属性抽屉 TabRow (权重 0.9), FAB 组件面板, 解析失败页, 草稿恢复对话框, 退出确认; DesignerSession: 选中 / 拖拽 / 历史 / 诊断; OutlinePanel: 折叠, 上下移, 删除, 长按拖拽 BEFORE / INTO / AFTER; InspectorPanel: key, 目录驱动属性三态编辑 (布尔 / 枚举 / 文本样式 / 形状 / 颜色 / 数值 / 列表 / 字符串各自编辑器), 快捷属性, 插槽, 事件处理函数 (应用前 `ScriptReader.checkHandler` 语法检查), 修饰链增删 / 上下移 / 按父容器过滤; DesignerDialogs: 组件面板 (画廊 9 类), 承载选项 (mount / floaty / dialog 与键值), 问题列表). Manifest 新增两个不导出 Activity, 四个 alias 改指首页; 字符串 `designer_*` 65 键 x 11 目录; `verify_standalone_launch.py` 改走首页 -> 画廊首条目 -> 首页 -> 设置. 与条目的差异: 修饰链排序用上下移按钮而非拖动; 画布拖拽只高亮目标容器并追加到末尾, 插入位置在大纲中拖拽指定; 组件面板可插入 54 个 (55 个节点组件去掉需要工厂函数的 AndroidView; Snackbar 为会话级命令, 不在面板), 脚本中已有的两者仍被解析保留; 进程被杀后的草稿恢复以预写草稿文件模拟. 验证: JVM 121 项通过 (新增 PreviewFramesTest 5 项); instrumentation Xiaomi Pad 23046RP50C (API 35, serial 968e9f18) 81 / 81 通过 (含 DesignerDeviceTest 6 项, DesignerHomeDeviceTest 1 项, 更新后的 ThreeBlockComposeContractTest), AVD_API_24 (x86) 81 / 81 通过; 本机误连五台设备的首轮 (全部设备安装运行) 另在 Redmi 22120RN86C (API 33), OnePlus OPD2413 (API 35), Sony G8441 (API 28) 上通过除画布无限约束与首页返回两项外的全部用例 (两项已修复后在上述两台复跑), Sony XQ-AT72 安装失败未计; `verify_standalone_launch.py` (本小节扩展: 首页 -> 画廊首条目 -> 设置 -> 新建工程进入编辑页并等到模板文本出现在画布 -> 删除工程, 证明 R8 后的 Rhino 解析器 (mapping 中保留 130 个 org.mozilla 类) 与真渲染器在插件进程可用) 用 release APK 在 emulator-5554 (API 24) 与 968e9f18 (API 35) 通过; 小米上两次紧随模拟器步骤之后运行时曾卡在首页到画廊的切换, 单独复跑与脚本加入 `KEYCODE_WAKEUP` / `wm dismiss-keyguard` 后均通过, 根因未定位; lint 0 错误; `verify_apk_classpath.py` debug 与 `--shrunk` release 通过; release APK 5074033 B (P8.2 为 4839799 B, 设计器与 Rhino 解析器自本小节起进入 release; debug 13945907 B), DEX 方法引用 87447; 第 14 节的 APK / DEX 复核线已超出, 原因为设计器界面与 Rhino 解析器属计划内功能增长 (Q8 已放宽), 待维护者按第 14 节规则确认新基线.

### P8.4 工程存储与宿主衔接

- [x] (插件) 存储 (D39): 工程即脚本文件, 存 `filesDir/designer/projects/*.js`, 草稿存 `drafts/`; SAF 导入 (`ACTION_OPEN_DOCUMENT`, 复制到私有目录并记录来源 Uri) 与导出 (`ACTION_CREATE_DOCUMENT`); `FileProvider` (authority `${applicationId}.designer`) 暴露 `projects/`, 按需授予读或读写.
- [x] (插件) 四个去向 (D39, 宿主无需改动): 发送运行 (`RunIntentActivity` + `script` extra, 复用 `ScriptLauncher`); 在宿主编辑器打开 (`EditIntentActivity` + content Uri + `FLAG_GRANT_READ_URI_PERMISSION | FLAG_GRANT_WRITE_URI_PERMISSION | FLAG_GRANT_PERSISTABLE_URI_PERMISSION`, 宿主已接受 content Uri 并取持久权限); 导入宿主脚本目录 (`ImportIntentActivity` + content Uri, 显示名以 `.js` 结尾); 复制到剪贴板. 宿主未安装或入口不可解析时沿用画廊的提示.
- [x] (插件) 反向打开 (D39): `DesignerActivity` 注册 exported intent-filter (`ACTION_EDIT` / `ACTION_VIEW`, scheme `content` / `file`, mimeType `application/x-javascript` / `text/javascript` / `text/plain`), 打开时解析 Uri 内容; 保存时直接写回该 Uri (content: `openOutputStream`, 需写授权; 无写授权则降级为 "另存到私有目录" 并提示); 顺带使系统文件管理器可用设计器打开 `.js`.
- [x] (宿主) 编辑器菜单 "在 Compose UI 设计器中打开" (维护者并行协调, 不阻塞本阶段其它条目, Q10): 先保存当前文件, 经宿主 `FileProvider` 生成 content Uri 并授予读写, `setPackage` 指向插件后以 `ACTION_EDIT` 启动; 插件未安装或 INFO 版本低于 1.2.0 时隐藏或提示; 返回 `onResume` 时以 `lastModified` / 内容哈希检测变化并复用 `EditorView` 既有重载路径; 宿主 `.changelog` 10 语言; 最低宿主版本不变 (可选能力).
- [x] (测试) instrumentation: `FileProvider` 授权与四个去向在宿主已安装时可解析 (同 `GalleryDeviceTest` 写法), 反向打开 content Uri 的解析与写回; 宿主侧 (条目落地后): 反向打开 -> 设计器写回 -> 宿主重载的设备证据.


P8.4 证据 (2026-10-13): 插件侧 `designer/storage` 新增 DocumentSource.kt (`DocumentSource.Local` 私有工程 / `External` content 或 file 文档, 可写性 = Intent 写授权或 `checkCallingOrSelfUriPermission` / `File.canWrite`; `DocumentIo` 统一读写, 外部保存 `openOutputStream("wt")` 失败或不可写时另存为私有工程并切换编辑页来源, 外部文档草稿按 Uri SHA-1 键入 `drafts/`), ProjectStore 记录导入来源 `sources.properties`; Manifest 新增唯一 provider `androidx.core.content.FileProvider` (authority `${applicationId}.designer`, 不导出, grantUriPermissions, `xml/designer_paths.xml` 只含 `files-path designer/projects/`), `DesignerActivity` 导出并声明 EDIT / VIEW + content / file + application/x-javascript / text/javascript / text/plain; `ScriptLauncher` 新增 `editIntent` (`EditIntentActivity`, 读 + 写 + 可持久授权) / `importIntent` (`ImportIntentActivity`, 读授权) / `projectUri` / `start`; 编辑页菜单: 发送运行, 在 AutoJs6 编辑器中打开, 导入 AutoJs6 脚本目录, 复制, 导出到文件 (`CreateDocument`), 去向前有未保存修改先保存; `onResume` 按内容基线检测外部修改, 无本地修改静默重载, 有则询问 (`designer-external-reload` / `designer-external-keep`); 字符串 `designer_*` 新增 13 键 x 11 目录. 宿主侧 (宿主提交 db7e6a1734, 只提交本会话文件): `core/plugin/compose/ThreeBlockComposeDesigner.kt` (已安装插件版本 >= 1.2.0 判定, EDIT + application/x-javascript + `setPackage` Intent, 内容 SHA-256 指纹), 编辑器 "更多" 菜单项 `action_open_in_three_block_compose_designer` (仅插件可用时可见), `EditorView.openInThreeBlockComposeDesigner` (有修改先 `save()`, file Uri 经 `AppFileProvider` 转 content Uri, 读 + 写授权) 与 `reloadAfterDesignerIfChanged` (`EditActivity.onResume` 调用: 指纹变化且无本地修改时复用 `reloadWithEncoding`, 有修改时弹 `dialog_button_reload` / `dialog_button_cancel`), 11 语言字符串 3 键, zh-Hans 6.8.0 changelog 一条 (其它语言由维护者同步), 宿主 JVM `ThreeBlockComposeDesignerTest` 2 项通过, `:app:compileAppDebugKotlin` 通过. 与条目的差异: 宿主判定用 PackageManager 的已安装版本 (与 INFO 版本同源) 而非绑定 INFO 服务; 宿主重载比较内容 SHA-256 而非 lastModified. 验证 (为避免与另一会话争用真机, 本小节全部在本机 AVD 上进行, 模拟器预装普通宿主 debug 包 `:app:assembleAppDebug` 的 x86_64 / x86 产物): JVM 121 项通过; instrumentation AVD_API_30 (x86_64) 与 AVD_API_24 (x86) 各 87 项, 首轮各 1 项失败 (`readOnlyExternalDocumentIsSavedAsAPrivateProject` 的文本断言同时匹配标题与提示, 功能本身正确), 修正断言后 `DesignerHostDeviceTest` 6 / 6 在两台复跑通过; 宿主编辑器 -> 菜单 "在 3-Block Compose 设计器中打开" -> 设计器以 content Uri 打开 `/sdcard/Download/designer-handoff.js` -> 大纲删除 Text 节点并保存 -> 文件内容更新 -> 返回宿主编辑器后 EditText 内容已是新文本, logcat 无 FATAL (AVD_API_30, adb / uiautomator 人工驱动); `verify_standalone_launch.py` release APK 在 emulator-5556 (API 24) 与 emulator-5554 (API 30) 通过 (首页 -> 画廊 -> 设置 -> 新建工程进入编辑页 -> 删除); lint 0 错误; `verify_apk_classpath.py` debug 与 `--shrunk` release 通过; release APK 5086901 B (P8.3 为 5074033 B), DEX 方法引用 87629, 复核线状态同 P8.3. 未做: 导出到文件 (`CreateDocument`) 需要系统文件选择器, 未自动化; 真机未参与本小节.

### P8.5 文档, README, changelog 与 1.2.0 本地 gate

- [x] (文档) README 10 语言: 简介 / 功能特性 / 使用方式增加设计器 (入口, 工程, 四个去向, 反向打开); `AutoJs6-Documentation` 的设计器页面按 Q11; 离线文档同步; `aj6-int-compose.d.ts` 与 Ace 声明无变化 (脚本 API 未改).
- [x] (插件) `.changelog` 1.2.0 (10 语言, 通俗, 半角标点); 版本名 1.2.0; 体积增量与 Q8 预算对照写入证据; 图标与 alias 不变.
- [x] (发布) 本地 gate: `AGENTS.md` 第 14 节全部步骤 + P8.1 要求的宿主侧完整设备矩阵 + 设计器 instrumentation; 远端步骤遵守 D7; 证据 `docs/dev/p8-designer-evidence.md`.

P8.5 证据 (2026-10-13): README 10 语言校订 (`p_introduction_how` / 使用步骤 / 功能特性在 P8.3 与 P8.4 已加入设计器与去向; 本小节改写 `p_status` (设计器, 指南页, 宿主编辑器入口需 db7e6a1734), 新增兼容性条目 (设计器不依赖宿主, 去向与宿主入口的要求), FAQ 两条 (可识别子集, 不执行脚本), 安全条目三条改写 (导出的编辑页只收文档 Uri, 设计器与画廊的后台行为, 工程与草稿的数据边界); 西班牙语不用倒问号). `.changelog` 1.2.0 复核: hint 2 条改名 / 宿主要求 + 1 条设计器指南与宿主入口要求, feature 2 条 (设计器, 衔接), improvement 4 条, dependency 1 条 (Rhino), 10 语言同步, 半角标点. 文档站 (Q11): `AutoJs6-Documentation` 8538de3 新增 `api/composeDesigner.md` (入口, 工程与文件, 编辑页, 可识别子集与往返规则, 与 AutoJs6 的衔接, 限制), `api/compose.md` 表格加一行链接, 侧栏 / 目录 / 站点 changelog / progress / 10 语言仓库 changelog, 全站生成 158 模块与离线搜索索引, `--check` 通过; 219dd49 为离线同步后的计数器 103. 离线文档: `auto-generate-for-autojs6.bat --dry-run` (新增 1, 变更 158) 与 `--verify-offline` 通过, Offline Docs 5f7bf9f (build 132) (assets 同步, provenance `source_base_commit` 指向 8538de3, 10 语言 changelog 一条, VERSION_BUILD 132). `aj6-int-compose.d.ts` 与 Ace 声明无变化 (脚本 API 未改). 体积: 1.2.0 release 与 1.1.0 build44 (4673146 B / 82466 引用) 对照见 `docs/dev/p8-designer-evidence.md`, 增量约 0.4 MiB, 在 Q8 预算内; 第 14 节复核线仍待维护者确认新基线 (同日已确认: 以 build 106 的 5094361 B / 87629 引用为新基线, 复核线改为 5603798 B / 96392, 见 `AGENTS.md` 第 14 节). 本地 gate (JVM 121 项; lint 0 错误; classpath debug / release 通过; 插件 instrumentation AVD_API_30 与 AVD_API_24 各 87 / 87; release 启动检查两台通过; 宿主侧 compose 矩阵 15 类在两台 AVD 上通过, 例外为 API 24 的 TSX 类受 TypeScript Engine 编译器 Worker 超时阻塞, 以及 ComposeUiLoaderTest 需用 debug 插件包) 全部记录在 `docs/dev/p8-designer-evidence.md`; 为避免与另一会话争用真机, gate 全部在本机 AVD 完成.

---

## P9: 设计器增强 (规划, 尚未实施)

目标: 在 1.2.0 设计器的基础上补齐三个方向: 画布与预览精度 (P9.1), TSX 工程 (P9.2), 设计器内交互预览 (P9.3). 维护者于 2026-10-13 选定这三个方向, 要求本轮只做规划; 附录 D 的 Q12 - Q16 已于同日拍板, 结论见 D41 - D45. 1.2.0 的发布基线为 6db6da2 (build 118, 发布时以该提交打 tag 并收集签名产物); master 自 build 119 起为 1.3.0 开发版本 (P9.1 与 P9.3 第一步). 下面的调研摘要来自 2026-10-13 对本仓库, 宿主与 TypeScript Engine 本地代码的只读调研.

调研摘要:

- 画布: 长按拖放只解析到目标容器, 编辑页忽略指针位置, `DesignerSession.endDrag` 经 `moveInto` 追加到末尾; 节点边界已由 `CanvasBounds.rects` (经 `LocalNodeDecorator`) 记录, `moveTo(id, parentId, index)` 已存在; 惰性列表中未组合的子项没有边界, 过期边界不会清理, 容器的排布方向 (Row / Column / Box / Flow / Grid / Pager) 没有建模. 大纲拖拽按行的上下四分之一判定 BEFORE / AFTER; 修饰链只有上下移与删除按钮.
- 预览: 打开态的 AlertDialog / ModalBottomSheet / DropdownMenu / Tooltip 是内联卡片; DatePicker / TimePicker 由渲染器直接内联绘制; Image 没有 `BitmapRef` 时显示 `[Image]` 占位; AndroidView 不在渲染目录中, 显示 `[AndroidView]` 且不可从面板插入. 宿主解析图片的规则 (`ComposeScriptPlatform.resolveImage`): `@drawable/name` 取宿主资源, 含 `://` 的路径一律拒绝, 其余按脚本目录读取本地文件.
- 导入来源: 导入时把 `name=uri` 写入 `sources.properties`, 但没有取持久授权 (`takePersistableUriPermission`), 记录的 Uri 会失效; `ProjectStore.rename` 不迁移记录, `sourceOf` 尚无调用方.
- TSX: 宿主经 AIDL `ITypeScriptCompilerPlugin` (action `org.autojs.plugin.typescript.COMPILER`, signature 级 PLUGIN 权限, 服务在独立进程 `:typescript_compiler`) 调用 TypeScript Engine, 编译器运行在无头 WebView 的 Web Worker 里, 默认超时 30 s (API 24 x86 AVD 上的 `ERR_AUTOJS6_TYPESCRIPT_COMPILATION_TIMEOUT` 来自这里的预热探测); 编译参数固定为 `jsxFactory: "__autojs6Tsx"` / `jsxFragmentFactory: "__autojs6TsxFragment"`, `<compose.Column p={1}>` 编译为 `__autojs6Tsx(compose.Column, {p: 1}, ...)`, `<compose:Text/>` 为字符串类型. 该 AIDL 只编译, 不提供语法树; 宿主侧客户端约 4000 行; 工作区内没有可复用的 Kotlin / Java JSX 解析器 (Rhino 只有 E4X). 宿主的 compose TSX 样例在 `app/src/androidTest/assets/compose-tsx/` (10 个).
- 交互预览: 宿主的 `compose` 脚本 API 全部用 Kotlin 实现 (`runtime/api/augment/compose/` 17 个文件约 3300 行, `core/plugin/compose/` 18 个文件约 1800 行), 依赖 `ScriptRuntime`, 脚本调度器, loopers, floaty, images / files 等运行时服务; 其中 `ComposeSession` / `TreeReconciler` / `CallbackRegistry` 只引用契约类. 插件的 Rhino slim jar 含解释器 (`Context` / `Interpreter`, 无 optimizer), 但 release 经 R8 只保留解析闭包 (130 类), `RhinoImportGuardTest` 禁止主源码进入 Rhino Context. 宿主没有 "运行脚本并把界面树回传给其它应用" 的协议, 发送运行的 `script` extra 只是一次普通的 Rhino 运行.

### P9.1 画布与预览精度

- [x] (插件) 画布拖放的精确插入位置: 按目标容器的排布方向 (纵向: Column / LazyColumn / 纵向 Pager, Grid 按行优先; 横向: Row / LazyRow / 横向 Pager; 叠放: Box 系只追加) 比较指针与各子节点边界的中线, 得到插入序号并在画布上画出插入线, 落下时经既有 `moveTo` 生效; 不可见的惰性子项不参与计算, 每次重组前清理过期边界. 面板插入改为 "插到选中节点之后" (选中的是可容纳子节点的容器时仍追加为末子节点).
  证据 (2026-10-13, build 119): `designer/preview/DropPlacements.kt` (`LayoutAxes` 排布方向表: 纵向 Column / LazyColumn / Card / NavigationRail / NavigationDrawer / ModalBottomSheet / DropdownMenu / SearchBar, 横向 Row / LazyRow / 按钮类 / NavigationBar / TabRow / SegmentedButton / HorizontalPager, 网格 LazyVerticalGrid, 其余叠放; `DropPlacements.place` 按首末子节点判断方向, 从右到左与倒序同样适用, 未组合或表达式子节点保留位置但不参与比较, 返回拖走节点后的序号与插入线), `DesignerSession.placeDrop` / `endDrag` (有序号时经 `moveTo`, 叠放追加), 画布拖动时画插入线, 每次预览更新清理已不存在节点的边界. 面板插入 "插到选中节点之后, 选中容器则追加为末子节点" 在 1.2.0 的 `DesignerSession.insert` 已是此行为, 未改动. JVM `DropPlacementsTest` 6 项 (含 "目录中每个可容纳多个子节点的容器都有排布方向" 守卫, 首次运行补上 SearchBar), 全仓 127 项通过; 设备 `DesignerDeviceTest.canvasDragDropsTheNodeWhereThePointerIs` (长按 Gamma 拖到 Alpha 上方, 保存后 Gamma 排第一) 在 AVD_API_30 与本类其余 7 项一起通过. 文档站设计器页的画布与限制描述在 P9.1 收尾时统一更新.
- [x] (插件) 大文档编辑延迟: 1.2.0 基线 (`docs/dev/p8-designer-performance.md`) 中 Redmi 22120RN86C 上 251 节点文档的撤销 / 重做到画布约 0.7 - 0.8 s, 其中包含测试框架的同步与查询开销; 先用帧回调或 `gfxinfo` 分清真实帧耗时, 若确认偏慢, 让预览按节点增量更新 (保持渲染器节点 key 稳定, 只重建变化的子树) 并让大纲行惰性计算, 再以同一测试复测对比.
  证据 (2026-10-13, build 125 / 126): 先分清测试开销: 挂着 compose 测试规则时重组由测试时钟驱动, 不查询就不出帧, 因此新增不挂规则的 `DesignerFramePerformanceDeviceTest` (默认跳过, `-e designerPerformance true` 开启), 在主线程直接调用编辑会话的 undo / redo, 不查询地等帧稳定, 以窗口 `FrameMetrics` 与主线程 CPU 时间计量. AVD_API_30 上真实帧耗时约为可见性计时的一半 (undo / redo 到首帧 p50 约 226 / 210 ms, 同时的可见性计时约 420 / 360 ms), 仍偏慢. simpleperf (`--trace-offcpu`) 采样定位到两处根因: (1) 渲染器按位置循环调用子节点, `RenderNode` 内部的 `key(id, type)` 只区分同一调用位置上的节点, 顶部插入或删除一行使其后每个子节点换到另一个调用位置并整体重建组合; 宿主中同样如此, 前面插入兄弟会让后面的节点丢失 remember 的状态 (如 LazyColumn 的滚动位置). 各循环调用处改为 `key(childId)` (`RenderNode` 的 Column / Row / Box / 内容, `WideComponents` 的内容 / 标签页 / 菜单 / 分段按钮, 设计器弹层), 新增 `AdvancedRendererTest.aSiblingInsertedBeforeANodeLeavesThatNodesStateInPlace` (修正前失败, 修正后通过), changelog 记为修复 (build 125). (2) 设计器每次预览都交给渲染器新的 `RenderFrame`, 未变化的节点也无法跳过: 新增 `designer/preview/LiveFrame.kt`, 保持同一帧实例, 节点各存一个快照状态, 只写入契约值有变化的节点, 缺失的节点晚一轮移除 (JVM `LiveFrameTest` 2 项); 修正 (1) 后的第二大开销是画布以 `SnapshotStateMap` 记录节点边界, 每次 put 都整表比较, 一次布局为平方级, 改为普通表加一个版本号状态 (build 126). 结果 (同一 AVD 与同一 androidTest APK, 改动前后各跑两次, 主机同时运行 4 个模拟器, CPU 约 93%): undo 到首帧 p50 365 - 376 ms 降为 140 - 154 ms, redo 301 - 352 ms 降为 130 - 138 ms, 主线程 CPU 同比下降约 55 - 60%; 每次编辑执行的 `RenderNode` 由 252 / 247 次降为 7 / 2 次 (临时计数, 未提交). 详见 `docs/dev/p8-designer-performance.md` 的 P9.1 一节. 大纲已是按行 id 设键的 LazyColumn, 只组合可见行, 行列表计算很轻, 未改. 剩余开销 (编辑页自身组合, 预览派生约 16 - 26 ms, 校验, 新行组合与 50 行 Column 的测量) 随文档线性增长, 1.3.0 不再处理. 未执行: 1.2.0 基线中的真机 (Redmi / G8441) 本轮未复测.
- [x] (插件) 修饰链拖动排序: 属性抽屉的修饰链列表支持长按拖动, 上下移按钮保留为无障碍与键盘路径; 一次拖动对应一条 `SetModifiers` 命令.
  证据 (2026-10-13, build 123): `InspectorPanel.ModifierChainEditor` 每项左侧加拖动手柄 (`detectDragGestures`, 无需长按, 在属性抽屉的纵向滚动中由手柄消费拖动), 按各行中线计算落点, 被拖项变淡并在落点画插入线, 松手后以前后 `seal` 隔开的一条 `SetModifiers` 提交 (一步撤销); 上移 / 下移 / 移除按钮保留. 设备 `DesignerDeviceTest.modifierChainDragMovesAnOperationWhereItIsReleased` (把 `.height(20)` 拖到 `.padding(4)` 之前, 保存后链为 `.height(20).padding(4).width(40)`) 在 AVD_API_30 通过.
- [x] (插件) 弹层真实预览: 打开态的 AlertDialog / ModalBottomSheet / DropdownMenu / Tooltip 在画布逻辑屏幕内用真实组件绘制 (画布内遮罩层加组件本体, 不创建系统窗口), 卡片形式保留为后备; 多个弹层同时打开时按树序叠放.
  证据 (2026-10-13, build 124): `PreviewFrames` 把打开态弹层以 `open = false` 交给渲染器 (契约树保持闭合, 渲染器不开窗口, 原位只绘制菜单 / 提示的锚点), 另列 `Overlay` (所属节点, 种类, 插槽与子节点的 UiNode id), 进入节点时先占位, 因此按树的先序排列, 内层弹层画在外层之上; 新增 `designer/preview/DesignOverlays.kt` 以 Material 3 外观在画布内绘制: 对话框 (32% 遮罩, 28 dp 圆角面板, 图标 / 标题 headlineSmall / 正文 bodyMedium / 右对齐的取消与确认), 底部弹层 (遮罩, 顶部圆角, 拖动条), 菜单 (锚点起始边下方, 下方放不下时翻到上方, 宽 112 - 280 dp), 纯文本提示 (inverseSurface, 优先在锚点上方, 放不下时在下方); 插槽与子节点仍由真实渲染器 `RenderNode` 绘制. `DesignCanvas` 记录各弹层面板的边界: 落在面板上的点击与拖动只在该弹层的子树内选取 (最上层面板优先), 没有原位边界的弹层 (对话框, 底部弹层) 的选中框与名称标签跟随面板. 实际实现中所有打开态弹层都能绘制, 未保留卡片后备, 1.3.0 早先为卡片补的内边距 (build 120) 随之移除, changelog 中对应的修复条目改为本项的优化条目. 菜单宽度取 112 - 280 dp 内的可用宽度, 不像真实 DropdownMenu 那样按内容的内在宽度收窄 (内在测量遇到 SubcomposeLayout 会抛异常, 设计器中不冒这个风险). JVM `PreviewFramesTest` 改写 1 项 (打开的底部弹层以关闭态进入帧并列为弹层, 关闭的不列) 并新增 1 项 (底部弹层内的菜单排在其后, 之后的对话框再其后), 全仓 131 项通过; 设备 `DesignerOverlayDeviceTest` 5 项 (对话框: 遮罩与面板在画布内, 无 Dialog / Popup 窗口, 取消在确认左侧, 点按钮文字选中 shorthand Text, 点按钮内边距选中 TextButton; 底部弹层: 内容位于画布下半部, 点拖动条区域选中 ModalBottomSheet 而非下面的页面; 菜单: 位于锚点下方, 点项选中 DropdownMenuItem; 提示: 顶边锚点的提示翻到下方, 其余在上方; 底部弹层内的菜单: 锚点在弹层底部时菜单翻到锚点上方, 点项选中菜单项) 在 AVD_API_30 通过, 截图核对四种外观与选中框; 设计器 6 个设备测试类共 30 项一起通过.
- [x] (插件) Image 预览 (D44): `@drawable/name` 经宿主包 Context 读取资源 (宿主包已在 `<queries>` 中, 无需权限); 工程菜单可选择一个图片目录 (系统文件选择器的目录树, 持久授权, 记录在工程旁的元数据中), 相对路径在该目录下解析, 未授权或找不到时仍为占位; 网络地址与宿主一致地拒绝, 并在问题列表中说明原因.
  证据 (2026-10-13, build 122): `designer/preview/DesignImages.kt` (`ImageSource` / `ImageLookup`; `@drawable/name` 经 `getResourcesForApplication(AutoJs6)` 读取, 非位图 drawable 按固有尺寸绘制; 含 `://` 拒绝, 绝对路径不预览; 相对路径在图片目录中按段查找, 目录为系统目录选择器给出的文档树 (取持久只读授权) 或 file Uri, 拒绝 `..` 越界; 解码按 2 的幂降采样到长边不超过 1024 px, 同一来源只解码一次并强引用位图, 因为契约位图句柄是弱引用), `ProjectStore.imageFolderOf` / `setImageFolder` (`images.properties`, 按草稿键, 私有工程与外部文档通用, 改名迁移, 删除清理), `PreviewFrames.build(images = ...)` 只对字面量 `src` 查找, 缺失原因进入问题列表; 编辑页菜单 "图片目录" (字符串 2 键 x 11 目录). JVM `PreviewFramesTest` 新增 1 项 (全仓 130 项通过); 设备 `DesignerImagePreviewDeviceTest` 3 项 (相对路径 / 越界 / 网址 / 绝对路径 / 无目录; 宿主 `@drawable/ic_about`; 菜单选目录后画布的 `[Image]` 占位消失) 在 AVD_API_30 通过. 首轮发现两处测试与实现问题已修正: 只读尺寸的解码按设计返回 null, 曾被误判为失败; 只带 action 的监视器会拦截没有 action 的 Intent (包括启动编辑页的那个), 改为编辑页启动后再挂监视器.
- [x] (插件) AndroidView 占位改进 (D44): 占位显示 `view` 表达式源码与尺寸修饰; 仍不可从面板插入.
  证据 (2026-10-13, build 120): `PreviewFrames.standIn`: 目录中有定义但设计期无法绘制的组件 (AndroidView) 派生为浅色 Surface, 带节点的快捷属性与修饰链 (尺寸, 权重等), 内含 `[AndroidView] <view 表达式源码>` 标签 (超过 60 字符截断, 8 dp 内边距), 归属原节点以便点选; 未知组件仍为单行占位. 顺带修复 1.2.0 已有的问题: 打开态弹层卡片内的 Column 以数值而非 dp 传 padding / spacing, 规范化失败后两者被丢弃. JVM `PreviewFramesTest` 新增 2 项 (占位保留 height 修饰与表达式源码; 弹层卡片内容带 padding 与 spacing), 全仓 129 项通过; AVD_API_30 上设计器三个设备测试类 17 项通过, 截图核对占位外观.
- [x] (插件) 导入来源同步 (D45): 导入时取读写持久授权 (失败时降为只读并提示), 改名时迁移来源记录; 编辑页菜单增加 "写回来源" 与 "从来源重新导入", 以内容基线检测来源侧的修改并询问; 授权丢失时提示重新选择文件.
  证据 (2026-10-13, build 121): `ProjectStore.Source` (来源 Uri 与上次同步时双方文本的 SHA-256 基线, `sources.properties` 每行 `name=uri<TAB>baseline`, 兼容 1.2.0 的 `name=uri`), 导入时依次尝试读写 / 只读持久授权, `rename` 迁移记录, `relink` / `markSynced`; `storage/SourceSync.kt` 判断哪一侧在上次同步后变化 (无基线时两侧文本相同视为同步); 编辑页菜单 "写回来源" (先静默保存, 来源有变化时确认覆盖) 与 "从来源重新导入" (工程有变化或未保存时确认替换, 无法解析时提示并保留工程), 来源打不开时提示并可经系统文件选择器重新选择后重试; 字符串 15 键 x 11 目录; 导入 MIME 列表提为 `ProjectStore.SCRIPT_DOCUMENT_TYPES`; README 安全要点 10 语言补充 "导入保留访问权限, 只在用户选择写回 / 重新导入时读写". 设备 `DesignerSourceSyncDeviceTest` 4 项 (以插件 FileProvider Uri 充当来源走真实导入; 记录与改名迁移, 写回与覆盖确认, 重新导入与替换确认, 来源丢失后经监视器代答的选择器重新关联) 与设计器其余 3 个类共 21 项在 AVD_API_30 通过. 未执行: 持久授权跨进程重启的设备实测, AVD_API_30 的 DocumentsUI 不响应 adb 点击 (从 shell 直接发起的普通文件选择同样选不中文件), 维护者已让出的真机本轮不再占用; FileProvider Uri 不可持久授权, 代码对此回退为当次授权. 补 (build 127): 持久授权已在真机实测, 见下面 "(测试)" 条目的 `verify_source_sync.py`.
- [x] (测试) JVM: 插入序号计算 (各排布方向, 首 / 中 / 尾, 不可见子项), 来源记录迁移; instrumentation: 画布拖放到指定序号, 修饰链拖动与撤销, 弹层在画布内绘制且不新增系统窗口, `@drawable` 预览, 来源写回 (androidTest 自带的 DocumentsProvider 提供可持久授权的 Uri).
  证据 (2026-10-13, build 127): 插入序号 JVM `DropPlacementsTest` 6 项 (目录中每个多子项容器都有排布方向; 纵向, 横向含从右到左, 网格按行, 叠放与空容器; 没有边界的子项保留序号但不参与比较); 画布拖放 `DesignerDeviceTest.canvasDragDropsTheNodeWhereThePointerIs`; 修饰链拖动与撤销 `DesignerDeviceTest.modifierChainDragMovesAnOperationWhereItIsReleased` (本次补上: 保存后一次撤销恢复原链, 证明一次拖动只占一步); 弹层 `DesignerOverlayDeviceTest` 5 项 (`isDialog()` / `isPopup()` 均为 0); `@drawable` `DesignerImagePreviewDeviceTest.hostDrawablesResolveWhenAutoJs6IsInstalled`; 均在 AVD_API_30 通过 (全套 instrumentation 109 项, build 126 源码). 与计划的两处偏差: (1) 来源记录迁移没有 JVM 测试, `ProjectStore` 依赖 `Context` 与 `android.net.Uri`, 本仓库 JVM 测试不带 Android 运行时, 由设备测试 `DesignerSourceSyncDeviceTest.importRecordsTheSourceAndRenameKeepsIt` (导入记录来源与基线, 改名迁移记录) 覆盖; (2) 可持久授权的来源不用 androidTest 自带的 DocumentsProvider: 导入走 `ACTION_OPEN_DOCUMENT`, 只有系统选择器能处理并在返回结果时授权, 测试进程内以监视器代答的结果不经系统, 不带任何授权, 自带的 DocumentsProvider 也就走不到真实的导入路径. 改为新增 `.python/verify_source_sync.py`, 在真机上走真实的系统选择器: 推送脚本到 /sdcard/Download 并等媒体库收录, 从设计器首页 "导入脚本" 选中它, 插入 Button 后 "写回来源" (来源文件出现 `compose.Button`), 停止插件并在 shell 中原地改写来源, 在新进程中打开工程 "从来源重新导入" (画布显示改写后的文字), 再插入 Button 并写回 (来源同时含改写的文字与 `compose.Button`), 最后删除工程与来源. Redmi 22120RN86C (API 33, com.google.android.documentsui) 上插件 1.3.0 release 包 (build 126 源码) 两次通过 (进程 24865 -> 25739, 26388 -> 27038). 调试中发现: 用 `adb push` 覆盖来源会让媒体库换新条目, 选择器给出的 Uri 随之失效, 设计器按设计提示 "来源文件不可用" 并可重新选择, 因此脚本改为原地改写; 另有一次在第二次插入时组件面板未滚到 Button (共用的 `insert_from_palette` 偶发), 重跑通过.

### P9.2 TSX 工程

- [x] (宿主 / TypeScript Engine) 解析契约 (D42): 宿主 `plugin-api` 新增独立的 TypeScript 解析契约 (AIDL 与 action, 例如 `org.autojs.plugin.typescript.PARSER`, PLUGIN 权限; 契约版本与快照测试同既有契约模块), 输入单个源文件文本与文件名, 输出带起止偏移的语法树 (JSX 元素, 属性, 展开属性, 表达式容器, 文本, Fragment, 变量声明与调用表达式给出结构, 其余节点只给种类与范围) 及语法诊断; TypeScript Engine 在现有 WebView 运行时内用 TypeScript 编译器的 `createSourceFile` 实现, 不占编译槽位, 有独立超时与并发上限, 冷启动沿用预热; 两个关联仓库的条目由维护者协调.
    - 证据 (2026-10-13): 宿主 `fce22c83a6` (`plugin-api/typescript-parser-api`: `ITypeScriptParserPlugin` 四个方法, action `org.autojs.plugin.typescript.PARSER`, 契约 v1 / 哈希 `6c8a1a80...`, 树 schema revision 1, 限额与错误码; `TypeScriptParserContractTest` 校验哈希与 AIDL transaction 顺序; 协议文档 `docs/dev/typescript-parser-plugin-protocol-v1.md`; 宿主 app 不打包该模块). TypeScript Engine `1e92487` (0.7.0, build 123): 同进程 `:typescript_compiler` 中的独立解析页面 `typescript-parser-r1` 与专用 Worker (`ts.createSourceFile`, 不占编译槽位, 并发 2, 默认超时 15 s, 空闲 60 s 回收), `verifyParserWorkerDesktop` 挂在 `check`, `ParserServiceInstrumentedTest` 3 项. 冷启动预热未沿用编译页面 (解析页面首次请求时创建): 小米平板 23046RP50C 上安装后的首次打开约 5.7 s, 之后约 0.8 s. "API 24 上的冷启动超时" 未执行: API 24 / 25 镜像自带的 WebView (约 53) 低于 TypeScript Engine 声明的最低 119; 解析服务在 AVD_API_30 (WebView 83) 上可用, 见测试条目.
- [x] (插件) 解析客户端与映射 (D42): 引入解析契约 AAR (与 `compose-ui-api` 一样以 SHA-256 锁定), `<queries>` 增加 TypeScript Engine 包; 新增 `designer/parse/TsxReader`, 把语法树中的 JSX 元素树 (`compose.X` / `compose:X` 标签, 属性, `{}` 表达式容器, 文本子节点, `<>...</>` Fragment), 顶层 `compose.state` 声明与唯一的挂载调用映射为 `DesignDocument`, 其余源码按节点范围从原文切片作为不透明段保留; 无法映射时与 JavaScript 子集一样拒绝并定位. 模型增加 `dialect` (JS / TSX), 校验与预览路径不变. 未安装 TypeScript Engine, 版本过低或解析服务不可用时以只读源码方式打开 (可查看与复制, 不能编辑树, 没有画布预览), 并提示所需版本.
    - 证据 (build 131): `libs/typescript-parser-api.aar` (宿主 `fce22c83a6` 的 release AAR, 重建字节一致, SHA-256 `c5651a0c...`) 入锁, `implementation`; `<queries>` 增加 TypeScript Engine 包. `TsxParserClient` (首次使用时绑定, 编辑页存活期间保持; 未安装 / 无解析服务或契约不符 / 绑定失败分别报 NOT_INSTALLED / TOO_OLD / UNAVAILABLE; 树经缓存目录文件描述符传回并校验长度与 SHA-256), `TsTree` (契约 JSON), `TsxReader` (与 `ScriptReader` 同样的段落与节点规则; `compose.X` / `compose:X` 元素, 属性按 JSX 取值, JSX 文本按 TypeScript 的空白折叠与实体表, Fragment 展开, 其它标签与 `{}` 子表达式按源码保留, `compose.state<T>` 的类型参数保留; XML 标签 / 展开属性 / 命名空间属性名 / 带参数或类型参数的渲染函数 / 挂载变量类型标注拒绝并定位), `DesignDocument.dialect`. 只读打开: 编辑页显示原因与版本, 可复制与重试, `onRestart` 自动重试. 设备: 小米平板 23046RP50C (TypeScript Engine 0.7.0) 与 Redmi 22120RN86C (0.6.8, 只读) 见测试条目.
- [x] (插件) TSX 生成: `designer/codegen/TsxWriter` 采用与 `ScriptWriter` 一致的规范格式 (4 空格缩进, 单引号字符串属性, 每个子元素一行), 往返不动点为硬要求; 事件属性与 state 绑定写成 `{...}` 表达式.
    - 证据: `TsxWriter` (单引号字符串属性, 其它值在 `{}` 中, 空元素自闭合, 唯一纯文本子节点同行, 不能原样写成 JSX 的字符串与相邻文本的后一段写 `{'...'}`, `compose:X` 写回 `compose.X`), `DocumentWriter` 按方言分派; 新建模板 `ProjectStore.TSX_TEMPLATE` 是其输出. 不动点由 `TsxRoundTripTest` 以解析服务的真实树验证 (见测试条目).
- [x] (插件) 工程与去向: `ProjectStore` 接受 `.tsx`, 新建工程可选 JavaScript / TSX; 编辑页 intent-filter 增加宿主实际交出的 TypeScript MIME 类型; TSX 工程的发送运行改为交出工程文件的 content Uri (宿主按显示名 `.tsx` 路由到 TypeScript Engine), 不再使用 `script` extra; 复制与导出不变.
    - 证据: `ProjectStore` 接受 `.js` / `.tsx` (列表, 新建, 导入按显示名, 外部文档草稿键与另存), 首页新建对话框选择 JavaScript / TSX (未装解析服务时提示所需版本); 宿主实际交出的类型: 编辑器一律 `application/x-javascript` (已在 filter 中), 资源管理器对 TypeScript 用 `text/typescript`, 因此 filter 增加 `text/typescript`. TSX 运行 `ScriptLauncher.runIntent` (VIEW + 工程 content Uri + 读授权, `text/typescript`); TSX 导出以 `application/octet-stream` 创建以保住 `.tsx` 文件名. 小米平板上 "在 AutoJs6 中运行" 经宿主 5352 + TypeScript Engine 0.7.0 编译运行 TSX 工程, 按钮点击两次后标签为 `TSX e2e 2`.
- [x] (宿主) 若编辑器菜单 "在 3-Block Compose 设计器中打开" 目前只对 `.js` 可见, 扩展到 `.tsx`, 宿主 `.changelog` 10 语言; 由维护者协调.
    - 结论 (2026-10-13): 不需要改动. 宿主菜单只按已安装插件版本 (>= 1.2.0) 判断可见 (`ThreeBlockComposeDesigner.isAvailable`), 对任何扩展名都显示, 交出时类型固定为 `application/x-javascript`. 1.2.0 / 1.3.0 打开 `.tsx` 会显示 "无法打开" 与行列; 是否在宿主侧为 `.tsx` 要求 1.4.0, 留给维护者决定 (宿主工作树当时有其它任务的未提交改动, 本会话未改宿主).
- [x] (文档) `composeDesigner.md` 增加 TSX 小节 (可识别子集, 与 JavaScript 的差异, 编辑与运行都需要 TypeScript Engine, 缺少时的只读打开), 离线文档同步; README 与 changelog 10 语言; TypeScript Engine 与宿主契约的文档由各自仓库更新.
    - 证据: 文档站 `bdf76d6` (设计器指南新增 "TSX 工程" 一节, 入口 / 工程 / 去向 / 限制各节注明 `.tsx`, progress 记录), `2908e2e` (生成页面), `759a3ae` (计数 106); Offline Docs `fc82a6b` (build 135, provenance `bdf76d6`, 6.8.6 条目 10 语言, `--verify-offline` 通过). 插件 README 10 语言 (状态 1.4.0, 设计器与衔接特性, 两条 FAQ, 新增兼容性一条) 与 changelog v1.4.0 10 语言 (hint, 三条 feature, improvement, dependency). 宿主契约文档 `docs/dev/typescript-parser-plugin-protocol-v1.md` (宿主 `fce22c83a6`), TypeScript Engine `docs/architecture/PARSER_SERVICE_V1.md` (`1e92487`).
- [x] (测试) JVM: 宿主的 10 个 compose TSX 样例 (复制到本仓库测试资源) 与 56 条画廊示例的 TSX 形式, 以 TypeScript Engine 输出的语法树快照为输入, 全部映射并往返 (快照随解析契约版本更新); 错误定位用例 (泛型箭头函数, 非 compose 标签, 展开属性, 条件 / 循环生成的子元素); instrumentation: 新建 TSX 工程 -> 编辑 -> 保存 -> 重开一致; 装有 TypeScript Engine 时经解析服务打开与经宿主运行 TSX 工程的设备证据, 未安装时只读打开; TypeScript Engine 侧的解析契约测试 (含 API 24 上的冷启动超时) 在该仓库完成.
    - 证据: 快照 `app/src/test/resources/tsx/` (101 棵树, TypeScript Engine `1e92487` 的 parser-worker.js + TypeScript 6.0.3 在 Node 中生成, `SNAPSHOTS.txt` 以 SHA-256 绑定源文件, `.python/generate_tsx_snapshots.py` 重生成). `TsxRoundTripTest`: 56 条画廊与 5 个示例的 TSX 形式映射为与 JavaScript 完全相等的文档并为不动点; 宿主 10 个样例中 7 个映射并往返, 3 个 (interop 用 `compose.attach`, legacy-xml 无 compose 挂载, non-ui 的挂载在 `try` 中) 与 JavaScript 规则一样以 "no compose.mount" 拒绝 (条目原文 "全部映射" 对这 3 个不成立); 手写用例覆盖文本 / 实体 / 属性 / 插槽 / 表达式. `TsxReaderErrorTest`: 16 个错误用例的行列 (泛型渲染函数, XML 标签, 展开属性等); 泛型箭头函数作为事件函数, 条件 / 循环生成的子元素按源码保留 (见 `cases/expressions.tsx`). JVM 共 154 项. 设备 `DesignerTsxDeviceTest`: 小米平板 23046RP50C (TypeScript Engine 0.7.0) 新建 -> 插入 -> 改 key -> TypeScript 语法处理函数 -> 保存 -> 重开再保存字节相同, 处理函数语法错误 `1:17 Expression expected.`; Redmi 22120RN86C (0.6.8) 只读打开并显示版本; 经宿主运行 TSX 工程见工程与去向条目; AVD_API_30 全套 115 项通过. TypeScript Engine 侧: `verifyParserWorkerDesktop` 与 `ParserServiceInstrumentedTest` 在该仓库; API 24 未执行 (见解析契约条目). 汇总 `docs/dev/p9-tsx-evidence.md`.

### P9.3 设计器内交互预览

- [x] (插件) 第一步, 预览状态面板 (不执行脚本): 列出顶层 `compose.state`, 可临时修改其值 (只影响预览, 不写回脚本); 预览模式下受控组件的交互 (开关, 输入, 选择) 直接修改这些预览值; 条件 / 循环生成的内容仍为占位.
  证据 (2026-10-13, build 128): 编辑页第三个标签页 "状态" (`designer/ui/StatePanel.kt`): 每个顶层 state 显示名称, 脚本中的初值 (字面量或表达式源码) 与预览值编辑器 (布尔为开关, 数字与字符串为输入框, 其余按 JavaScript 字面量输入并用设计器自己的解析器读取, 读不出时不改), 单项 "重置" 与 "全部重置"; 预览值存于 `DesignerSession.previewValues`, 不进入文档, 撤销历史与草稿, 重新加载文档时清空. `PreviewFrames.build(values = ...)` 以预览值替换绑定的初值. 受控关系不按组件列表维护: 节点属性绑定了 state (`checked: on.value`), 且同一组件某个事件的载荷字段与该属性同名 (`checkedChange` 的 `checked`) 时, 该事件即修改这个 state (`designer/preview/PreviewControls.kt`); 契约目录中符合这一命名的共 17 组 (开关, 复选框, 单选按钮的 checked; 滑块的 value; 两种输入框的 text; 筛选 / 输入纸片与分段按钮的 selected; 分页的 page; 提示与抽屉的 open; 搜索栏的 expanded; 日期选择的 selectedDateMillis 与 displayMode; 时间选择的 hour 与 minute), JVM 测试逐项锁定, 目录变化时会先在这里暴露; 这些节点在帧中带上对应回调. 画布右上角 "交互预览" 开关 (`DesignerSession.interactive`): 打开后画布不再拦截指针, 也不画选中框, 渲染器发出的事件经 `CanvasControllers.emit` 交给会话, 只改预览值, 脚本中的事件处理函数不执行. 输入框: 渲染器把 `text` 当初值 (宿主拒绝修改声明文字, 改文字走编辑命令), 为此在 `TextFieldController` 增加只供设计期画布调用的 `replaceText`, 画布在文档或预览值改变声明文字时调用, 但跳过输入框自己刚报告的文字 (否则打字时会被滞后的帧改回); 这同时修复了 1.2.0 起的问题: 在属性抽屉修改输入框的 text 后画布不更新 (新增 `DesignerDeviceTest.aTextFieldOnTheCanvasFollowsItsTextProperty`, 去掉同步时该测试失败). 字符串 8 键 x 11 目录. JVM `PreviewStateTest` 5 项 (目录中的受控组合, 受控关系推导, 载荷转字面量, 预览值进入帧且绑定属性带回调, 会话处理事件只改预览值, 重新加载清空), 全仓 138 项通过; 设备 `DesignerPreviewStateDeviceTest` (面板开关与文字驱动画布开关, 标签与输入框; 编辑模式点画布开关只选中; 交互预览中点画布开关与在画布输入框打字回写面板; 结束后脚本文件不变且没有草稿) 在 AVD_API_30 通过, 截图核对; 设计器 7 个设备测试类共 32 项一起通过.
- [x] (插件) 第二步 (D43: 1.4.0 之后评估, 立项前单独拍板): 修订 D8 / D35 与 `RhinoImportGuardTest`, 在插件进程内以 Rhino 解释器执行渲染函数与事件处理函数: 独立的 `designer/sandbox` 包, `ClassShutter` 拒绝一切 Java 访问, 设置指令计数与墙钟上限, 只提供 `compose` 的预览实现 (state / render / memo / batch / 事件), `toast` / `log` 写入设计器控制台, 其它宿主模块的调用抛出 "预览中不可用"; 执行结果只用于画布, 不改动文档模型; R8 只为沙箱保留解释器闭包, 体积增量按 Q8 记录.
  证据 (2026-10-14, build 139, 详见 `docs/dev/p9-sandbox-evidence.md`): `designer/sandbox` (SandboxEngine: 每次进入一个 Context, 解释模式, ES6, `initSafeStandardObjects`, 全拒 ClassShutter, 每 10 000 条指令检查一次, 超过 2 亿条或 3 s 以 `Error` 中止且之后停用, 解释器栈 1 000 帧, 字符串桥接; PreviewSandbox: 每次运行一个 8 MB 栈线程与定时器调度; SandboxPreview / SandboxTrees / SandboxEvents / SandboxCatalog) 与预览运行时 `preview-runtime.js` (compose 的预览实现, 按宿主规则校验并沿用其错误码, 渲染按 key / 类型序号复用节点身份, 句柄属性代理与命令, 输入框编辑序号, ref, 定时器, 控制台; 其它 AutoJs6 名字是值为抛错 Proxy 的普通全局变量, 脚本仍可声明同名变量). 画布以 `LiveTree` / `LiveHooks` 绘制运行时的树 (节点 id 即运行时 id, 修饰回调令牌自 2000000 起, 输入框文字经带序号的 `UiCommand.Edit` 同步, 句柄命令在帧绘制后执行); 脚本即保存时的脚本, 预览值作为初值, 修改后 400 ms 重新运行, 后台与关闭开关时停止; 交互预览开关显示运行图标, 新增 "控制台" 标签页, 脚本错误与停止显示在画布底部与问题列表. 实施中发现: Rhino 的 Date 基于 java.time, API 24 / 25 上初始化即 `NoClassDefFoundError`, desugaring 会改写宿主内加载的 Compose 类, 故沙箱只在 API 26+ 启用, 更低版本沿用第一步; API 24 - 27 在非触摸模式下把焦点交给画布中第一个输入框并弹出键盘, 编辑模式改为禁止画布内组件获得焦点. R8 keep `Interpreter` 与 `jdk18.VMBridge_jdk18` (按名反射构造), release 包在 AVD_API_30 上经文档入口运行覆盖正则 / JSON / Date / Map / Set 等的脚本, 结果与 Node 逐字相同. 体积: release 5615650 B / 96185 引用 (1.4.0 为 5221337 B), APK 超出复核线 5603798 B 11852 B, 原因为解释器闭包与沙箱属计划内增长 (Q8: 10 MiB 内不阻断), 待维护者按 AGENTS 第 14 节确认.
- [x] (插件) 编辑模式 (点选 / 拖拽) 与预览模式 (组件可交互, 事件进入预览值, 第二步立项后进入沙箱) 的切换; 预览错误显示在问题列表, 不阻止编辑与保存.
  证据 (2026-10-13, build 128): 与第一步一同实现, 见上一条的 "交互预览" 开关; 预览问题沿用编辑页的问题列表 (`session.previewProblems`), 不影响编辑与保存. "进入沙箱" 一节随第二步, 第二步未立项前事件只进入预览值.
  补充 (2026-10-14, 第二步): JavaScript 工程 (API 26+) 的交互预览事件进入沙箱, 沙箱的错误与停止也列入问题列表; 第一个树到达前画布显示文档且不响应输入.
- [x] (文档) 指南页说明预览模式的能力边界 (可用的 API, 与真实运行的差异), README 与 changelog 10 语言.
  进度 (2026-10-13, 第一步): 文档站 `1e8894d` 的设计器指南新增 "预览状态与交互预览" 一节 (状态面板, 会回写预览值的控件, 与真实运行的差异), 插件 README `a3b78d5` 与 changelog (build 128) 10 语言已更新; "可用的 API" 一项待第二步的沙箱确定后补写.
  证据 (2026-10-14, 第二步): 文档站 `34def8e` 新增 "在预览中运行脚本 (1.5.0)" 小节 (可用 API 表, 不可用的模块与 Java, 约 3 秒上限, 脚本错误, 控制台, 编辑序号, memo / floaty / dialog / AndroidView 的差异, 后台停止, Android 8.0), 并更新 "用途", 编辑页表格与 "限制"; Offline Docs 同步见会话记录. 插件 README 10 语言 (设计器功能, FAQ "设计器会执行脚本吗", 新安全条目 "脚本沙箱", Android 8.0 兼容条目) 与 changelog v1.5.0 (提示, 三项新增, 输入框焦点修复) 随 build 139.
- [x] (测试) JVM: 沙箱拒绝 Java 访问, 死循环在上限内中止, 预览 `compose` 实现在计数器 / 表单 / 列表示例上与宿主语义一致; instrumentation: 预览模式点击计数器按钮后画布更新, 退出预览后文档不变.
  进度 (2026-10-13, 第一步): 不执行脚本的部分已有测试: JVM `PreviewStateTest` 5 项, 设备 `DesignerPreviewStateDeviceTest` (交互预览中操作画布控件回写预览值, 结束后脚本不变且无草稿); 计数器按钮需要执行处理函数, 随第二步.
  证据 (2026-10-14, 第二步): JVM `SandboxEngineTest` 14 项 (Java 不可达, 模块名仍可声明, 不可用模块报错与行号, 时钟 / 指令上限中止死循环且 catch / finally 不运行, 处理函数中的死循环, 深递归可恢复, 控制台与定时器, 处理函数报错后继续, 计数器 / 表单 / 列表示例的身份复用 / 句柄 / 命令 / ref / 输入框序号, 修饰回调令牌, compose 错误码) 与改写的 `RhinoImportGuardTest` 3 项, 全仓 JVM 173 项通过; 设备 `DesignerSandboxDeviceTest` 3 项 (计数器按钮点击后画布更新且退出后文件不变无草稿; 控制台输出, toast 与错误, 出错后继续; 死循环停止预览而设计器保持响应) 与改写的 `DesignerPreviewStateDeviceTest` (API 26+ 走沙箱, 以下走第一步) 在 AVD_API_30 与 API 24 实例 (Nexus 5 尺寸, 沙箱测试按假设跳过) 通过; 全套 instrumentation: AVD_API_30 100 项中 95 项通过, 4 项沙箱 / 状态测试在合并语义树中找不到运行图标 (FilterChip 合并了图标的 testTag, 改为 useUnmergedTree 后重跑通过), 1 项 TextFieldRendererTest 因模拟器剪贴板桥接使 system_server 崩溃 (重跑通过), API 24 实例 119 项中 117 项通过, 2 项渲染器测试 (AdvancedRendererTest / WideRendererTest) 为 AppNotIdleException, 以 18f5861 (CI 全绿, 两项在 CI 的 API 24 上通过) 构建的包在同一实例上同样失败, 属本地实例环境, 与设计器无关.

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

**已拍板 (2026-10-03)**: 采用推荐的 core 图标集, 自带图标通过 `Image` / `ImageWrapper` 使用; extended 留在 F.4 评估. 与已实现范围一致.

`material-icons-core` (约 50 个常用图标, 体积小) 还是 `material-icons-extended` (约 2000 个, APK 增加数 MB, R8 可裁剪未引用项但名称表驱动的动态查找会阻止裁剪). 推荐 core + 允许 `Image` 以 `ImageWrapper` 自带图标; extended 列入 F.4 评估.

### Q3 (P3.2 前): 缺 key 的动态列表项

**已拍板 (2026-10-03)**: 按索引匹配, 每会话 warn 一次; 与现有实现一致.

按索引匹配并每会话 warn 一次 (推荐), 还是直接 `INVALID_ARGUMENT`. 影响 render 层的容错与文档措辞.

### Q4 (P3.4 前): 悬浮窗 API 归属

**已拍板 (2026-10-03)**: 只提供 `compose.floaty(...)`, 不增加 `floaty.compose(...)` 别名; 与 P3.4 实现一致.

`compose.floaty(...)` (推荐, 与 `compose.mount` 对称, 文档集中) 还是 `floaty.compose(...)` (与 `floaty.window` / `rawWindow` 并列). 两者只能选一, 不做别名.

2026-10-03 P3.4 按正文既定入口实现 `compose.floaty(...)`, 不增加 `floaty.compose` 别名.

### Q5 (P5.3 后): 性能回归阈值

由 P5.3 实测数据提出 (首帧, 单次更新, 列表滚动, PSS, APK 体积), 维护者确认后写入插件 `AGENTS.md`.

**已拍板 (2026-10-03)**: 接受 P5.3 现有数值作为第一版复核规则, 同设备同方法的暖首帧 / 更新 / 列表首帧耗时采用约 +50%, APK 和 DEX 方法引用数采用 +10%. 超线必须复测或说明原因, 并由维护者确认是否接受; 不静默放宽或自动接受合理增长. 冷启动/PSS 和全部滚动指标继续记录, 后续补充稳定性证据再定线. 具体数值见 `AGENTS.md` 第 14 节与 `docs/dev/p5-performance-evidence.md`.

### Q6 (P6.4 前): 启动器图标源图

**资源形式已确认 (2026-10-03)**: 仅需暗色和亮色两类 `mipmap*/ic_launcher.png`, 用于插件中心, 应用内图标与 README banner; 不需要 launcher alias 或额外自适应图标. 当前源图仍为 P0.1 占位素材, 正式图案源图待补.

维护者提供两张黑白透明 PNG (亮色模式用深色图案, 暗色模式用浅色图案) 以替换 P0.1 的临时图标; 无 launcher 入口, 只需基础 `mipmap*/ic_launcher.png` 系列.

2026-10-08 补充: F.5 排期后启动器需要四个 alias 的自适应图标 (`ic_launcher_system*`, `ic_launcher_monochrome`), 由 Icon Studio 从同一图稿生成, recipe 底色 #FAFAFA / #212121; 正式图稿到位后只替换 recipe 图稿并重新生成, 资源形式不再变化.

### Q7 (P7 前): 1.1.0 顺序

**已拍板 (2026-10-03)**: 按既定建议顺序 F.1 TSX -> F.2 与旧 ui 混合 -> F.3 `compose.dialog` -> F.4 宽集 -> F.5 画廊 -> F.6 细粒度更新推进; 1.0.0 gate 在先, F.6 是否需要仍以性能证据为依据.

F.1 TSX, F.2 与旧 ui 混合, F.3 `compose.dialog`, F.4 宽集, F.5 画廊, F.6 细粒度更新的先后.

---

### Q8 (P8.1 前): 体积预算

**已拍板 (2026-10-13)**: 体积是考量因素但上限无需过严; 增量不足 10 MiB 时不作为阻断条件, 实测增量仍写入 P8.5 证据, 明显异常时先排查 keep 规则.

原建议: 契约 AAR (约 230 class) 与 Rhino slim jar (release 经 R8 裁剪到 Parser / AST 闭包) 进入插件 APK, release APK 增量上限 1.0 MB.

### Q9 (P8.3 前): 设计器页面名

**已拍板 (2026-10-13)**: 插件改名为 `Three-Block Compose` 系列成员, 产品与设计器同名 `3-Block Compose` (不翻译, 即 `app_name`); 设计器首页标题沿用 `app_name`, 不另设可翻译的 "设计器" 产品名. 应用图标采用维护者提供的 `three-block-compose-ic-launcher-light.png` / `-dark.png`. 改名已作为 P8.0 完成.

原建议: 页面标题可翻译 (zh-Hans "设计器", en "Designer"), 工程列表页标题沿用 `app_name`.

### Q10 (P8.4 前): 宿主反向入口的时机与判定

**已拍板 (2026-10-13)**: 按建议实施. 以插件 INFO 的版本 >= 1.2.0 判定可用 (不新增契约常量, 契约版本仍为 2); 时机可晚于插件 1.2.0 发布, 插件侧四个去向不依赖它; (宿主) 条目仍由维护者并行协调.

### Q11 (P8.5 前): 文档页归属

**已拍板 (2026-10-13)**: 按建议实施, `AutoJs6-Documentation` 新增独立指南页 (设计器使用, 工程与往返规则, 可识别的脚本子集), `api/compose.md` 只加一处链接; 离线文档同步.

### Q12 (P9 前): 版本切分与顺序

**已拍板 (2026-10-13)**: 按建议 (D41).

原建议: 1.3.0 = P9.1 (画布与预览精度) + P9.3 第一步 (预览状态面板); 1.4.0 = P9.2 (TSX 工程); 1.5.0 = P9.3 第二步 (沙箱执行, 取决于 Q14). 理由: P9.1 风险最低, 直接改善 1.2.0 的已知限制; TSX 需要新解析器并与宿主协调; 沙箱执行要修订 D8 / D35 且涉及安全面, 放在最后.

### Q13 (P9.2 前): TSX 解析方案

**已拍板 (2026-10-13)**: 选 (c), 由 TypeScript Engine 提供解析为语法树的操作, 且做成独立的解析 AIDL (不在现有编译契约末尾追加, 不占编译槽位); 未安装或版本过低时 `.tsx` 以只读源码方式打开 (D42).

原选项: (a) 插件内 TSX 子集解析器 (Kotlin, 离线, 与 D35 "只解析不执行" 一致; 代价是自写扫描器, 要覆盖 TypeScript 类型语法以免误伤不透明段); (b) 经 TypeScript Engine 的编译 AIDL 取编译结果 (只得到 JavaScript 与诊断, 没有语法树, 无法写回 TSX, 且要复制约 4000 行客户端); (c) 在 TypeScript Engine 契约中追加 "解析为语法树" 的操作 (三个仓库的契约变更, 运行时依赖该插件). 建议 (a), 可选地在保存时经 (b) 做语法诊断 (未安装 TypeScript Engine 时跳过).

### Q14 (P9.3 前): 交互预览方案

**已拍板 (2026-10-13)**: 按建议, 先预览状态面板, 沙箱执行在 1.4.0 后再评估, 不做宿主实时预览协议 (D43).

原选项: (a) 只做预览状态面板 (不执行脚本, 风险最低, 条件 / 循环仍为占位); (b) 插件内沙箱解释执行 (Rhino 解释器 + `ClassShutter` + 指令上限, 需修订 D8 / D35, 自行实现 `compose` 的预览语义, 有与宿主语义漂移的风险); (c) 宿主实时预览协议 (宿主真实运行脚本并经新的 Binder 契约把界面树回传设计器: 语义完全一致, 但脚本以完整权限运行, 预览会产生真实副作用, 需要宿主与契约改动). 建议先 (a), 再评估 (b); 不建议 (c).

### Q15 (P9.1 前): 图片与 AndroidView 的预览范围

**已拍板 (2026-10-13)**: 按建议 (b) + (i): `@drawable` 加按工程授权的图片目录, AndroidView 保持不可插入 (D44).

原图片选项: (a) 只支持 `@drawable/name`, 本地文件仍为占位 (不申请任何权限); (b) 另允许按工程授予一个图片目录 (SAF 目录树, 持久授权), 相对路径在该目录下解析; (c) 申请存储 / 媒体运行时权限 (与 `AGENTS.md` 第 15 节 "不申请任何运行时权限" 冲突). AndroidView 选项: (i) 保持不可从面板插入; (ii) 允许以 `view: null` 的不透明桩插入. 建议 (b) + (i).

### Q16 (P9.1 前): 导入来源同步策略

**已拍板 (2026-10-13)**: 按建议 (a), 手动写回与重新导入 (D45).

原选项: (a) 手动: 菜单 "写回来源" / "从来源重新导入", 检测来源侧修改并询问; (b) 每次保存时自动同时写回来源; (c) 不做, 保持复制式导入. 建议 (a).

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

2026-10-08 状态: 维护者排期并修订 D8 后已实施; 画廊 56 条示例, 设置页与四 alias 启动器图标随 1.1.0 / build42 交付, 独立进程运行时见 D32, 证据见 `docs/dev/p7-f5-evidence.md`. 同日补正 (build44): build42 的独立进程缺少 arch core-runtime / poolingcontainer / emoji2 / window, 从启动器打开即崩溃; 打包集合扩为 47 项并改为完整 keep, 新增发布包引用闭包检查与设备启动检查 (D32 修订, 同一证据文件).

### F.6 细粒度更新

脚本侧按 state 依赖只重跑受影响的 render 片段 (`compose.memo(fn, deps)`), 宿主侧属性快照比较; 按 P5.3 数据决定.

2026-10-08 状态: P5.3 未覆盖的大树单次更新经补测证实为主要成本, 已按本节以 `compose.memo(fn, deps, options?)` 实施 (宿主 5323), 证据见 `docs/dev/p7-f6-evidence.md`.

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

### 2026-10-03: P1.3 与 P2 基础渲染交付

- 从插件 d383be0 (build 10) 与宿主独立分支 9c139db474 继续. 宿主主线 2082edef14 无冲突同步为 8372f53eee; P1.3 提交 0ee2953425, 插件集成提交 4b89901 (build 11), 最低宿主确认 6.8.0 / 5316. 原宿主目录的其他未提交改动保持不动.
- 完成原路线图 P1.3, P2.1, P2.2, P2.3 与 P2.6. 公告如实说明开发预览, 不在 P3 前宣称 compose 脚本 API 已交付. 未增加, 分拆或丢弃阶段; Modifier 作为基础组件依赖在本轮一并完成.
- 20 个组件, 20 个 Modifier, core 图标显式名称映射, 完整 V1 主题字段, 原子补丁与受控事件均通过本地验证. 宿主 / 插件保持同一 51 项共享指纹, AAR 只调整最低宿主部署元数据, V1 wire 不变.
- 补强边界: 接受首批 generation 0 与空场景批次; 嵌套 clickable 独立启用; 隐式点击焦点事件; 样式插入保持 scroll state; 卸载前已移除节点的命令拒绝; 错误 nodeId/prop 回到脚本调度器. 绘制 / 测量异常通过容器隔离, 借用位图每次绘制重新取引用.
- 验收详见 `docs/dev/p13-p2-evidence.md`: 插件 JVM 45, 宿主相关 JVM 25 + API JVM 13; 两台设备各 17 项插件 / 9 项 debug 宿主 / 4 项 release 往返. 宿主 app debug/release 和插件全部装配通过, lint debug 清洁, release 仅 3 个既有资源警告.
- 下一起点为 P2.4 输入框, 然后 P2.5 列表 / 脚手架 / 提示; P3 脚本入口, IME, TalkBack, inrt 执行, 全矩阵和性能 / 长期内存门禁未在本轮交付. 图标 Q6 仍为占位源图. 继续只做本地提交, 不推送 / 发 Release / 登记远端索引.
- 本轮最终插件签名产物: autojs6-plugin-compose-ui-v1.0.0-6b41b408.apk (build 12, 1958242 B). 测试安装全部清理, 自建 compose_ui_p2_api24 AVD 的注册与数据目录已验证删除.
- 提交定位: 宿主主线同步 8372f53eee, P1.3 注册 0ee2953425, P2 事件 / 验证 6812bdbd9f; 插件最低版本接入 4b89901, P2 渲染器为本条所在提交 (build 12).

### 2026-10-03: P3.4 悬浮窗与 P3.5 生命周期守卫

- 从插件 eec4889 (build 15) 与宿主独立分支 6fba451cae 继续, 先同步已提交主线 2fd0809e57 为 8edfb7ac84. 宿主原工作区中其它会话的代码生成 / 图标等改动未参与本任务暂存与提交.
- 完成原 P3.4 / P3.5, P3 全部勾选. `compose.floaty` 支持 ui / 非 ui, raw / 可调整窗口, 位置 / 尺寸 / 焦点 / 可触摸控制与 session; 每个悬浮会话拥有独立保活令牌, 与 UI 共享 8 会话上限. 18 个错误码与 10 语言资源, 关闭后句柄守卫和退出清理均有证据. 未增加, 分拆或丢弃路线图小节.
- 边界修复: 立即 closeAll 防止异步重现窗口; 原生登记集合移除锁反转并保留并发新增项; 退出 looper 的迟到投递按取消处理; owner 线程补做被丢弃的清理; 原生控制与普通 post 队列隔离且有界; 初始化失败和控制名额拒绝都释放临时窗口与保活令牌. 完整经过与限制见 `docs/dev/p34-p35-evidence.md`.
- 验证: 宿主相关 JVM 154, 冻结 API JVM 13, 插件 JVM 63; API 24 AVD / Sony API 31 / HyperOS API 35 各 8 项悬浮脚本, 连同页面 / 既有窗口回归合计 39 项通过. 宿主 app debug / androidTest / release, 共享依赖守卫, 插件签名 / 原生对齐 / DEX 归属及 36 个文档产物检查通过.
- 提交定位: 宿主 f7fcac31af (本地化错误), 065c8148e8 (原生控制 / 退出清理), 4d649bca96 (悬浮 API / 集成守卫 / 多语言历史), 证据另行提交. 插件本条所在提交为 build 16, 签名产物 autojs6-plugin-compose-ui-v1.0.0-f82f337c.apk, 2365090 B; 渲染器代码, 冻结 AAR, 51 项共享依赖与最低宿主 5316 保持不变, 脚本 API 需匹配当前本地宿主构建.
- 清理: 三台设备本次安装的 3 个包均已卸载并确认不存在; 自建 compose_p34_api24 AVD 已停止, SDK 按名称删除后确认数据与注册均不存在. 用户应用, 文件和其它 AVD 未处理. 继续只做本地提交, 不推送 / 登记索引 / 发 Release.
- 下一会话从 P4.1 示例脚本开始. 完整兼容 / inrt / 性能 / 长期内存验证继续按 P5, 用户文档与声明按 P6; Q6 正式图标仍待源图. 本轮没有执行宿主全量 JVM / lint, inrt 装配与远端 CI, 不把所选回归外推为这些门禁已通过.


### 2026-10-03: P4 示例, 无障碍与守卫

- 从插件 a4e9e66 (build 16) 与宿主 8e6b8b7e9a 继续; 已提交主线 d9dd9529c0 同步为 c6ef588315, 宿主 build 5317. 原工作区的其它数据集工作未修改. 完成原 P4.1 - P4.3, 未增加, 分拆或丢弃小节.
- 五例覆盖计数器, 表单, 1000 项 key 列表, 非 ui HUD 与主题, 随插件 assets/examples 打包并显式同步到宿主 sample/Compose UI. 宿主沿用动态目录枚举, 索引在 assets-app/indices. 测试直接执行真实资产并比较插件 / 宿主全部字节.
- 无障碍实测: 原样 testTag, id/idContains/idMatches, text/desc 与 Button 点击祖先; 使用真实内置服务和 Rhino, 不是以 UiAutomation 动作替代宿主验证. 外部文档 api/compose.md 及导航 / progress / HTML / JSON / 搜索索引已更新, 共 145 模块全量生成与 freshness 检查通过.
- 守卫: JVM 强类型导出冻结目录, Python 生成 / 比对真实 TS 属性接口与 Markdown 表; 14 个正反例及 tsc 校验通过, 对缺失 P6 文件或标记明确失败. API main / AAR / 共享依赖与最低宿主 5316 保持不变. 可选 debug JaCoCo 覆盖率与 CI 报告收集已接入, release 不带覆盖率运行时.
- 验证: 宿主相关 JVM 175, API JVM 16, 插件 JVM 63; API 24 x86 与 API 35 x86_64 插件各 31 项, 最终 release18 的示例 / 无障碍各 7 项通过. 覆盖率源码行 1545/1798 (85.93%), 分支 869/1458 (59.60%). 宿主 app debug / androidTest / release, 插件签名 / 对齐 / DEX 门禁通过; lint debug 无问题, release 仅 3 个既有身份资源警告. 证据详见 docs/dev/p4-evidence.md.
- 修正与环境: 表单提示移动到输入框旁, 同文本的选区 / 组合通知不再覆盖已保存状态. instrumentation 强停进程后只重绑已启用的自有无障碍服务. API24 的旧系统 DEAD 绑定以重启专用 AVD 恢复; SDK 预先修改的 ramdisk 会启动崩溃的 Magisk stub, 最终使用已验证原始备份的工作区副本显式覆盖启动, 未修改共享 SDK 或 Magisk 配置 / 授权.
- 提交定位: 插件 168268f (示例, build 17), 本条所在提交为 P4 收尾 (build 18); 宿主 d86d7a28a4 (目录工具), 24eb7ca2dc (示例), 214f8eedae (无障碍), 文档与证据另行提交. 外部文档 ac56189. 最终签名 APK autojs6-plugin-compose-ui-v1.0.0-3fc65a76.apk, 2373541 B, 7142 classes / 44715 method refs.
- 清理: 两台专用 AVD 上 4 个自有测试包全部移除并确认不存在; compose_p4_api24 / compose_p4_api35 的数据与注册均已核对后由 SDK 删除. 其它 AVD, 实体设备和原宿主其它工作保留. 本轮仍仅本地提交, 不推送 / 登记索引 / 发 Release.
- 下一会话从 P5.1 健壮性继续, 然后 P5.2 完整矩阵 / inrt 与 P5.3 性能体积. 本轮未执行宿主全量 JVM / lint, inrt 装配或远端 CI; 完整 API / 类型声明 / 离线插件同步留在 P6, 不提前勾选. Q6 正式图标仍待源图.

### 2026-10-03: P5 健壮性, 六设备 / inrt 与性能基线

- 从插件 `99c4d1d` (build18) 与宿主独立分支 `9332c85d43` 继续, 已提交主线 `166910c968` 同步为 `cadb1ddac6`. 原宿主其它会话的未提交工作未修改或暂存. 原路线图 P5.1 / P5.2 完成, P5.3 测量与优化判断完成, 回归线按 Q5 保留待确认; 未新增, 分拆或丢弃小节.
- P5.1: 敌意输入与事务回滚, 100次mount/close, 回调关闭, 工作线程post, 渲染中停止, 真实旋转 / Activity重建, 真实系统trim-memory, 活动会话包更新 / 卸载 / 禁用均有证据. 修复包身份订阅, 迟到请求缓存驱逐, render内递归挂载, attach异常结束整个引擎, Compose1.12祖先context跨loader污染. 公共owner包装仍委托宿主原Lifecycle和SavedStateRegistry, 无私有反射或AAR变更.
- P5.2: API24 x86 / Sony28 / Sony31 / Redmi33 / HyperOS35 / API37 16KB 六设备最终110项通过. 真正生成并安装应用版本1 / runtime5317的默认签名inrt APK, 插件存在和真实卸载后各连续5轮通过; 额外修复执行记录晚于Activity启动的登记竞态, 新增5项确定性测试. 外部文档7个文件更新, 145模块生成与freshness通过, Offline Docs保持P6范围.
- P5.3: API24与API35各118条, 共236条完整记录, 分析工具16项自测通过. 所有100赋值样本均1次render; 暖首帧p90约499/287ms, UI更新约86/124ms, floaty约83/137ms, 1000项列表首帧约1392/937ms. 这些是含查询/截图开销的可见性确认上界. 已有快照比较与空补丁跳过, 本轮不改API或加入推测性优化; 冷3样本/PSS单进程/AVD帧波动如实作为观察项. Q5已提出同环境p90约加50%及APK/DEX加10%复核预算, 尚未收到维护者确认, 不作为门禁, 首个复合条目仍未勾选.
- 验证: 宿主选定JVM200, API16, 插件JVM63, APK Builder38, Python16; 插件API24/35各32项通过. API37额外插件ComposeTestRule尝试中5契约项通过, 27项受Espresso已移除InputManager反射入口阻断, 不计入通过; 该设备真实宿主16项和inrt通过. 宿主debug / androidTest / inrt RuntimeKit, 插件debug / androidTest / release与debug lint通过; 完整宿主JVM / lint, 长期堆泄漏测试, release宿主性能基线及远端CI未执行.
- 提交定位: 插件 `004f9fd` (P5.1, build19), `5013b57` (P5.2, build20), 本条所在提交 (P5.3基线, build21); 宿主 `472f52e39b` (健壮性), `ec34abc00f` (启动登记与inrt), `ef707fd667` (性能工具与基线); APK Builder `4884075` (两构建别名与计数50对齐); 外部文档 `7dc1add`. 宿主仍在独立 `spike/compose-ui-p0` 分支.
- 最终插件签名产物 `autojs6-plugin-compose-ui-v1.0.0-5c0881dc.apk`, build21, 2376581B, SHA-256 `0978455b1bc5c04415fc4b6860be49dfd749deb9472b1392be67525aa0578640`, 7143类 / 44721方法引用. 与实测release19相比仅版本/预览说明改变, renderer源码不变; 最终APK在API24的JNI/计数器/停止清理3项及API37真实inrt1项均通过. 性能基线继续绑定原实测19, 未把新APK冒称重测.
- 清理: 六设备本次安装的23个自有包全部卸载并确认不存在. 两台专用AVD `compose_p5_api24` / `compose_p5_api37_16k` 经名称, 已解析数据路径与注册核对后停机并由SDK删除, 注册和数据均不存在. 四台真机原宿主版本与既有APK Builder保留, 其它设备/AVD与共享SDK未处理. 四个本次修改仓库均按逻辑本地提交; 无推送, 官方索引登记或公开Release.
- 下一起点为Q5回归线确认与P6.1完整API文档, 随后按P6继续声明/Ace与Offline Docs同步. Q6正式图标仍待维护者源图. 完整证据见 `docs/dev/p5-robustness-evidence.md`, `docs/dev/p5-compatibility-evidence.md`, `docs/dev/p5-performance-evidence.md`.

### 2026-10-04: 决策确认与 P6 全部本地交付

- 维护者确认 Q2 core+ImageWrapper, Q3 缺 key 每会话 warn, Q4 仅 compose.floaty, Q5 首版耗时约+50% / APK与DEX+10%人工复核规则, Q6 仅亮/暗两类mipmap图标, Q7 按F.1至F.6顺序. Q5数值及超线复测或说明并由维护者确认的流程写入AGENTS第14节, 原236条实测/采样参数/摘要未改变. 冷启动/PSS/滚动继续观察. P5全部完成.
- P6.1: 完整compose模块页和七个类型/组件页, 所有方法/属性/回调/生命周期边界按实际实现核对, 30项/114属性与声明双向守卫. 文档152模块, 搜索6543条; BAT完成dry-run与verify-offline, Offline Docs同步208资产并通过JVM2/Python4/debug-release内容与签名验证. 文档内容版本仍6.8.0, 项目计数90, 离线插件6.8.6/build71.
- P6.2: Declarations4.30.0与Ace1.22.0/build124, 运行实际aj6dts -Publish和Ace聚合, 只作本地文件同步. 39个严格负例+2个exact-nullish用例通过; 实际TS6.0.3默认/全组语言服务验证29工厂, 字段与签名, 并在常规verifier新增回归. 修复缺少可选原生声明时error-any吞掉非法图片/颜色输入的声明问题, 保留完整图原生可赋值性与默认分组策略. Ace JVM171/debug/原生对齐通过.
- P6.3: 插件与宿主10语言使用/历史说明最终化, 36个插件生成物一致; README HUD关闭时中断worker并避免迟到更新, 20个本地化脚本语法通过, 五个已实测示例未变. 协议标记frozen V1 with host commit ec34abc00f. V1 AAR与51项共享依赖, 渲染器生产代码均未改变.
- P6.4: 插件JVM63, debug/androidTest/release, debug lint, 定向独立API35 x86_64 AVD的connectedDebugAndroidTest32全部通过. 宿主选定JVM200/API16与debug装配通过; 最终release宿主实际五例+JNI/INFO共7项通过. 首次门禁发现路线图中的两处非ASCII标点, 修正后完整通过, 未弱化断言. 完整证据和未执行项见 `docs/dev/p6-release-gate.md`.
- 提交定位: 插件 `27be441` (Q2-Q7, build22), `27a397a` (P6.1, build23), `63b22f0` (P6.2, build24), `5cb77ef` (P6.3, build25), 本条所在提交 (本地gate, build26). 宿主 `d29eb47782` (Q5), `417f469975` (最终协议/历史), `4823e04a78` (gate证据). 文档 `2832a4d` / `2de38ab`, Offline Docs `396f191`, Declarations `43b29d7`, Ace `07a4831`.
- 最终签名APK `autojs6-plugin-compose-ui-v1.0.0-d9cdec3c.apk`, 2378609B, SHA-256 `c2d7ddae7d8311b4b82c991b7fe6f1c92547b4b1df7dfd018f1f43b0be7be8cc`, 7143类/44721方法引用. 相对P5基线仅增加3540B, 方法引用不变, 均在已批准复核线内; 未把P5时序与内存数据冒称本轮新测量.
- 清理与边界: 自有安装包均已移除; compose_p6_api35核对名称/序列号/已解析路径后由SDK删除, 数据与注册不存在. 物理设备, 其它AVD, 共享SDK和原宿主工作区未修改. 六个任务仓库均按范围提交, Ace保留原有未跟踪releases目录, 未整理或删除. 无Git推送/npm发布/官方索引/公开Release.
- P0-P6的既定本地实现与gate完成; 图标两类资源有效, 正式图案仍待源图. 完整宿主JVM/lint, 本轮再跑六设备矩阵, release宿主性能/长期堆泄漏/新的冷启动PSS滚动稳定性证据和远端CI未执行. 下一起点为已批准顺序的P7 F.1 TSX工厂, 远端步骤继续等待D7明确解除.

### 2026-10-04: P7 F.1 TSX 本地交付

- 按既定顺序完成 F.1, 没有增加, 分拆或丢弃路线图小节. 宿主在同一 classic TSX ABI 内接入 29 个 Compose 工厂, 引用别名与命名空间标签, 中立 Fragment, 根 / 插槽限制及混域保留旧页面. 真实设备暴露的 NativeObject/Map 解包导致节点身份丢失问题已修复, 严格回归通过; 未改通用解包器或冻结 V1 契约.
- 配套版本: 宿主 6.8.0/5319 (`b67d668f48`), Declarations 4.31.0 (`3213fb0`), Engine 0.6.4/84 (`be69923`), Ace 1.23.0/125 (`a1b7045`), 文档 6.8.0/91 (`4c3e292`, `55c033f`), Offline Docs 6.8.6/72 (`cbb7183`). 插件为本条所在 F.1 提交, 1.1.0/build27. 实际 aj6dts -Publish, Ace 聚合与离线 BAT 同步均已执行, 全部仅本地.
- 验证: 宿主相关 JVM 173/API16, 插件63, Engine相关534/构建逻辑4, Ace171, catalog Python16, 真实TS5.1.3/6.0.3与语言服务回归通过. 自建API35 x86_64 AVD的最终签名编译器/渲染器通过6个真实TSX场景和5个原有JS示例; 编译器debug声明清单另2项通过. 原有36个XML标签保留, namespace属性补全及JSX表达式静态精度限制已在文档明确.
- 最终插件签名产物 `autojs6-plugin-compose-ui-v1.1.0-0a1cb1c0.apk`, 2381737B, 44721 DEX方法引用, 都低于Q5已批准复核线; 完整摘要与各仓库验证见 `docs/dev/p7-tsx-evidence.md`. 原236条P5数据与门槛不变. 本轮未重跑完整设备矩阵/宿主全量JVM及lint/打包应用TSX/长期性能测量/远端CI.
- 清理: 5个自有安装包全部移除, 自建compose_f1_api35停机并核对路径后经SDK删除, 注册与数据不存在. 原宿主并发codegen工作及Ace既有未跟踪releases保留, 物理设备与其他AVD未处理. Q6正式图案仍待源图. 下一起点为F.2与旧ui混合; D7继续有效.


### 2026-10-04 源码公开与图稿同步

维护者已授权创建公开 GitHub 仓库并推送本插件源码, 提交采用 noreply 邮箱. 本条更新 D7 的源码推送门控; 既有开发阶段和实测记录保持其原有含义. 本次工作整理并提交当前工作区, 同步图稿与生成材料, 不以源码公开代替 APK 发布和发行验收. 宿主改动另行处理.

首次公开前已将 27 个本地提交的邮箱改为 noreply, 保留作者名称, 时间, 消息和全部文件树. `docs/dev/commit-identity-map.json` 提供原本地提交与公开提交的对应关系, 供既有证据溯源.


### 2026-10-07: P7 F.2 XML 与 Compose 互嵌本地交付

- 完成原 F.2, 未增加, 分拆或丢弃路线图小节. XML 容器通过 compose.attach 承载 Compose, AndroidView 接受实际 View 或主线程同步工厂, ui.inflate 支持 XML TSX 及真实原生点击. 多容器相互独立, 支持离窗预创建, 聚焦, 旧 raw/resizable 悬浮窗, 关闭和引擎退出清理; 非法混域及原生归属冲突在替换前拒绝.
- 宿主独立分支合入固定主线 8b5bef91f3 为 e05218974a, F.2 实现 d54f21b1ea. 保留颜色/主题与多语言历史. 新 AAR SHA-256 为 0aaff93a27d405e8db7a513172ba1a2ebe60a3b8a09d4f27d8c9eaa03544d4a3; 197 个保留 V1 class 字节相同, 新增9个可选互操作类, 按既定约定退役7个无引用 P0 spike 类. 51项共享锁与基础最低宿主5316不变, F.2使用匹配宿主5320.
- 配套提交: Declarations4.33.0为26dcf6c, TypeScript Engine0.6.5/build86为c9cded8, 隔离Ace1.26.0/build137为aab24b8, 文档6.8.0/93为95b65af及ab541a5, Offline Docs6.8.6/build76为11911a1. 实际aj6dts -Publish以干净宿主提交为源并显式同步隔离Ace, Engine以已提交声明回填精确来源. 原宿主和原Ace的并发工作未修改或暂存.
- 回归: 宿主相关JVM197/API21, 插件JVM64/设备40, 目录Python19, Engine相关JVM534(529+单独5)/构建逻辑4, Ace完整JVM171与最终语言服务通过. 真实TS5.1.3/6.0.3通过原25及新增31个负例; Ace默认/全原生组各30类非法输入保持拒绝. API24/35各6个互嵌场景, 最终签名包7个TSX场景, 旧宿主+新渲染器7项, 新宿主+实际旧渲染器1项均通过. 不能正常提交的原生恢复操作移到发布前; 原生回调中关闭后不再恢复旧View或claim.
- 插件最终签名包autojs6-plugin-compose-ui-v1.1.0-a1de352a.apk, 2472197B, SHA-256 3dd4f45976568218335c879d67078dfe28a119d97603ac01bf5d4d8b1f45530d, 7237类/45285方法引用. 低于Q5的2612576B/49194引用复核线; 原236条性能记录与规则不变. 文档156模块/6673搜索条目, 离线212文件/12720268B, 全部生成/签名门禁通过. lint均无错误, 插件4项图标警告, Engine7项与Ace49项既有警告如实记录.
- 两台自建compose_f2_api24/compose_f2_api35和8个自有安装均经身份/路径核验后清理, 注册与数据不存在. 物理设备, 其它AVD, 共享SDK及原工作区保持. 本轮仅本地提交, 没有推送/npm发布/官方索引登记/公开Release; 2026-10-04的插件源码公开授权仍按原记录有效.
- 下一起点为F.3 compose.dialog. 本节已包含宿主, 渲染器, 编译器, 编辑器, 声明和离线文档完整链路, 本轮不开始F.3. 完整设备矩阵, 打包应用F.2, 宿主全量JVM/lint, 新的可比性能/长期内存基线和远端CI未执行, 不外推为通过. 详细修正过程, 环境波动, 产物和限制见F.2证据.


### 2026-10-07: P7 F.3 独立对话框与底部弹层

- 完成原 F.3, 返回可更新, 关闭和监听close的普通ComposeSession. alert为默认, bottomSheet直接展开; cancelable=false同时禁止返回/外点/拖动关闭. 不替换已有页面或其他会话, 共用8会话上限. UI绑定稳定engine Activity, 非UI使用已有悬浮窗权限, 受管理透明1px anchor和保活; closeAll/退出/回滚均正确清理. 未增加, 分拆或丢弃路线图小节.
- 保留既有依赖, 冻结V1/F2的206个class字节不变, 新增3个dialog类. 当前AAR SHA-256为e3a4f2003bb0336338e229cfc9220c2296a5de462957fd540a20c37d86996fe8. 原ModalBottomSheet缺少公开show前overlay类型入口, 因而组合公开Compose Dialog与M3 BottomSheetScaffold/SheetState/scrim实现同一模态承载, 不使用私有反射或移植内部源码. 基础最低宿主5316不变, F.3匹配宿主5321/Engine0.6.6.
- 修正真实边界: 首次组合改为附着后可捕获的初始化, 保留致命窗口失败report后close的独立生命周期通道; 空Activity显式初始化ViewTree owners, 由Activity Context提供默认application token; Focus等待真实窗口焦点, 有界FIFO和旧代次监听正确释放. 全局activity别名不能改变承载归属. 校验与回调始终保持原脚本调度器边界.
- 验证: 宿主JVM214/API23, 插件JVM64, API35插件49项/API24弹窗9项; 宿主API24/35各6个真实弹窗场景和各1个权限拒绝例; 最终宿主F3/F2/旧悬浮窗20项及签名编译器+渲染器8个TSX/5个JS例均通过. 新宿主+真实旧renderer30与旧宿主5320+新renderer均验证. TS5.1.3/6.0.3通过F1/F2/F3的25/31/22负例及P6 strict/exact; Engine JVM534/构建逻辑4/full Worker/C8/配额, Ace JVM171及完整语言服务通过.
- 提交定位: 宿主主线同步fbef5018f8, 实现8cba2ce0e3; 声明4512f0e (4.34.0), Engine c961271 (0.6.6/build87), Ace主线合并1942076及F3实现0793f37 (1.28.0/build141), 文档e830d58/97d0fc4 (6.8.0/94), Offline Docs d6cd51a (6.8.6/build77). 插件为本条所在build31提交. 实际aj6dts -Publish基于干净宿主, 明确同步隔离Ace; 保留原宿主和原Ace的并发工作, 各任务工作区按逻辑本地提交.
- 签名产物autojs6-plugin-compose-ui-v1.1.0-c8dddb90.apk, 2545465B, SHA-256 469314d90a9d85271e9278d5309130c75c433bda97fe5144e59294a3aaa7752d, 7478类/46345方法引用, 低于Q5的2612576B/49194线. 原236条性能观测与规则保持. 文档156模块/6675搜索条目, 离线212文件/12735581B同步和签名门禁通过. lint无错误, 插件4项/Engine7项/Ace49项既有警告保留记录.
- 环境与夹具: API24旧模拟器显示后端发生system_server/goldfish_dma崩溃, 仅切换自有AVD到受支持的swiftshader后重跑. 实际drag等待公开可交互状态, 焦点序列使用真实活动窗口输入模式, 清理检查绑定具体anchor对象, IME检查区分focus请求与真实点击显示键盘, 未增加超时或跳过平台. 两台自建AVD及9个自有安装的最终清理记录见F.3证据.
- 下一起点为F.4宽集组件/契约V2. 本轮没有推送/npm发布/官方索引登记/公开Release, 不把既有源码公开授权等同于本轮已发布. 全设备矩阵, 打包应用F.3, 宿主全量JVM/lint, 新可比性能/长期内存与远端CI未执行, 明确保留验证边界.


### 2026-10-07 - 2026-10-08: P7 F.4 宽集组件与契约 V2 本地交付

- 按原 F.4 实现全部 19 类组件和 6 个 item 节点, 基础目录 55 项, AndroidView 协商后 56 项. V1 目录, 模型, Parcel tag, 上限, 修饰符与 F.2/F.3 可选接口保留; 新旧实际宿主/renderer 的准入与回滚均已验证. 未增加, 分拆或丢弃路线图小节, 未开始 F.5/F.6.
- 窗口与输入边界已修正: ModalBottomSheet 沿用公开 Dialog + BottomSheetScaffold, 跨 popup 仍保留外层 overlay 类型; raw 与旧 ui 悬浮容器通过公开 owner 接收 Back, 保留原生焦点和调用方 View. Drawer 区分用户请求与原生 anchor/动画校正; DatePicker 模式切换先同步已接受日期和月份; TimePicker 保留有界待确认字段. SearchBar 拒绝 65537 个 UTF-16 单元的原生输入并报告 LIMIT_EXCEEDED, 保留 65536 边界, 忽略受控 query 的同值回声且继续允许同值 search.
- 最终匹配产物验证: 插件 1.1.0/build33 的 JVM 70, lint 0 错误/5 警告, 签名/摘要/类归属/原生对齐均通过. API24 x86 与 API35 x86_64 各完整 64 项插件测试通过; 宿主 6.8.0/5322 的 JVM 224/API 29, 两设备使用最终签名 renderer 的各完整 29 场景通过. 最终完整运行证据以插件 `docs/dev/p7-wide-evidence.md` 为准, 不用分批重跑的并集替代.
- 当前来源: 干净宿主 `6a166c38415e7299ba034cd062dcc4c51a4b543a`, 声明 4.35.0 为 `de51858b59ec88f75c56d94bcf4af600c54ff34f`; 实际 aj6dts 使用该宿主及生成器 `525bebdaa157087b1295d80cef0dae861152eda5`. TS5.1.3/6.0.3 的 F1/F2/F3/F4 25/31/22/42 负例与 P6 strict/exact, 声明体检查通过. 文档 6.8.0/96 为 `c5a9658`, 内容 `479d8f1`; Offline Docs 6.8.6/build81 为 `f1bfc004d189750949df48b3a9673418eebfc1fe`, 212 资产/12828821 B 与最终签名内容门禁通过. 并发平台提交及其已包含的 F.4 文件保留既有历史, 具体来源见证据.
- 最终 Compose APK `autojs6-plugin-compose-ui-v1.1.0-ffee2730.apk` 为 3022152 B, 9031 类/53842 DEX 方法引用, SHA-256 `3ece8a8885b0a2b9ab7edf2e1eeaf5aeeeebc3b6a0b3f4936b868da5ae0ab755`. Q5 于 2026-10-07 批准的候选为 d4f266d2 (3022148 B/53838 引用); 最终 +4 B/+4 引用和审批参照同时记录在 `docs/dev/p7-wide-size-review.json`, 不冒称维护者重新确认了新精确值. 原复核线和 236 条 P5 观测不变, 未重新定义性能基线.
- 配套已完成: Engine 0.6.7/build89 提交 `527d892`, Ace 1.29.0/build143 提交 `5b1b0e9`; Engine JVM534/构建逻辑4/full Worker/C8/配额, Ace JVM171/完整浏览器语言服务以及各自签名门禁通过. 最终签名 Engine + renderer 的9个真实TSX和5个原JS示例共14项通过. 插件为本条所在build33提交, 各插件计数一致. 两台专用AVD与10个自有安装经身份和路径核对后已清理. 下一起点保留原F.5条件项与F.6性能判断, 本轮不增加或拆分小节. 原宿主和原 Ace 没有被本次工作编辑或暂存; 已有外部提交变化, 不沿用原宿主早先的脏工作区描述. 本轮未执行宿主全量 JVM/lint, 新六设备矩阵, inrt F.4, 新计时/内存基线或远端 CI, 也没有本轮 Git 推送/npm 发布/官方索引登记/公开 Release.

### 2026-10-08: 已勾选项复核与 P7 F.6 细粒度更新

- 复核已勾选条目: 公开仓库首次远端 CI (8d2528c) 的两个模拟器作业因 emulator-runner 逐行执行 script 而把续行符当作 Gradle 任务名失败, 改为单行命令 (3e30f25), 同一命令在专用 API35 AVD 上完整 64 项 instrumentation 通过并生成覆盖率. 移除已无消费方的 P0 spike keep 规则 (9f4b2ce, release 资源名集合不变). 路线图身份表, D7 / D29 注释, AGENTS 结构 / 图标 / CI 描述与当前契约 V2, 宿主 5322, 平台 1.9.0, Icon Studio 及源码公开状态对齐 (821df26). 香港繁体 V2 提示误用台湾 "轉譯器", 改为 "渲染器" (efa0198).
- F.5: 组件画廊需要启动器入口与独立界面, 与固定决策 D8 冲突, 且启动器图标依赖 Q6 正式图稿; 仍为未排期条件项, 需维护者排期并修订 D8 后实施, 本轮不实施.
- F.6: 按条目条件先补测 P5.3 缺失的大树单次更新数据, 证实整树重建为主要成本后实施 compose.memo 并完成宿主 / 声明 / Ace / Engine / 文档 / 离线文档全链路本地配套, 详见上方 F.6 证据与 `docs/dev/p7-f6-evidence.md`. 插件仅更新说明文案与路线图, 渲染器和最低宿主不变.
- 宿主 Compose 工作仍在独立 worktree 的 `spike/compose-ui-p0` 分支 (本轮新增 51b7760c49, 1adc2b8639, 9fe6dde65f), 未合入宿主 master. 本地插件 master 领先 origin; 本轮未推送, 未发布 npm, 未登记官方索引, 未公开 Release. 专用 AVD 与自有安装在会话结束时清理.


### 2026-10-08: P7 F.5 组件画廊, 分支合并与推送

- 维护者答复 2026-10-08 复核中的四个待决点: 明确做组件画廊 (修订 D8), 接受插件包体积增量, 允许推送插件仓库, 合并并删除旧分支. 宿主 `spike/compose-ui-p0` 已合入宿主 master (`8186010ea3`, README 日期回填 `4bec8bf589`, 5323) 并删除分支与 worktree; 隔离 Ace `compose-ui-f2` 已合入 Ace master (`7125e44`, 1.30.0/146) 并删除. 宿主与 Ace 本轮只合并, 未推送.
- F.5 按原条目实施, 未增加, 分拆或丢弃路线图小节: 画廊 + 设置页 + 四 alias 启动器图标, 新增 D32 独立进程运行时 (36 项宿主锁定 AndroidX 副本随插件打包, 宿主内 parent-first 遮蔽) 与 material-color-utilities 4.1.1. 渲染器, 契约 V2, 最低宿主 5322 与 INFO / Wake 不变; AGENTS 第 1 / 2 / 4 / 5.2 / 6 / 10 / 12 / 13 / 15 节同步修订.
- 验证: JVM 84 项通过, lint 0 错误 / 10 警告 (Icon Studio 生成的同内容图标与 localeConfig 的 API 级别提示, 与其他独立插件相同); 专用 API35 x86_64 AVD `compose_f5_api35` 上插件 完整 73 项 instrumentation 通过 (含新增 GalleryDeviceTest 3 / SettingsDeviceTest 4 / LauncherIconDeviceTest 2 项), 另在同一 AVD 安装常规包宿主 `org.autojs.autojs6` (6.8.0 / 5325 debug) 后复跑画廊与设置 7 项通过 (运行 Intent 可解析, 外观 Provider 可读), 隔离宿主 `org.autojs.autojs6.compose.spike` (master `b2404d3ae9`, 5325) 的 compose 设备包 74 项中 61 项通过, 13 项为需显式参数的性能 / 旧 provider / 打包应用 / 包变更 / 权限负例阶段按假设跳过; 首轮 5 个示例场景因宿主工作树的示例资产在合并检出时被 autocrlf 转为 CRLF 而字节不等, 经 `sync_examples.py` 回写 LF 并在宿主 `.gitattributes` 钉死 `eol=lf` (宿主 `de11353cc0`) 后通过, 2 个无障碍场景在启用隔离宿主的 AccessibilityServiceUsher 后通过; TSX 10 项与 F.2 / F.3 / F.4 / F.6 场景均使用 build40 渲染器 (与 build42 的渲染器字节相同, build42 只改动插件进程的图标归一化与测试). 签名包 `autojs6-plugin-compose-ui-v1.1.0-2f1127ce.apk` 为 3624288 B, 10473 类 / 60613 DEX 方法引用, SHA-256 `505ea68bb2a23ff81d01ad0735048fa5835fc9ac36d12c503b137c1290c35992`, 较 build38 +600552 B / +6771 方法引用, 维护者预先接受, Q5 复核线不变. Icon Studio 20 项资源与 10 语言 36 份生成文档校验通过.
- 图标判断: recipe 底色由透明改为 #FAFAFA / #212121, 使亮 / 暗 alias 与系统应用信息图标有可区分的底色 (与其他独立插件一致); 图案仍为 Q6 占位, 正式图稿到位后只替换图稿并重新生成. 宿主侧未改代码, 画廊运行入口复用既有公开 `RunIntentActivity`.
- 推送 build41 后的远端 CI: Markdown 与 Icon Studio 工作流通过; Build integrity 的 API24 x86 作业 73 项全部通过, API35 x86_64 作业 10 项失败 (画廊剪贴板读取为 null, 以及 9 个依赖窗口焦点 / 弹窗 / 返回键 / IME 的既有渲染器场景超时). 剪贴板在 API 29+ 只对持有窗口焦点的应用可读, 而前一项测试留下的设置页 Activity 使新画廊窗口在较慢的 CI 模拟器上取得焦点更晚. 修正 (build42): 画廊测试先等待窗口焦点并轮询剪贴板, 设置页入口测试结束时关闭其打开的 Activity, `LauncherIcons.normalize` 在 alias 状态已一致时不再写入组件状态, 工作流在测试后上传 logcat 与窗口状态. 本地新建 API35 AVD 复跑 73 项中 72 项通过 (画廊 / 设置 / 图标 9 项含剪贴板修正全部通过); 唯一失败为既有的 F.4 场景 `WideRendererTest.pullToRefreshUsesActualNestedScrollAndHonorsDisabledState`, 该 AVD 严重掉帧 (Choreographer 跳过 102 帧, HWUI 单帧 2.3 s) 使刷新指示器动画期间主线程 60 s 内未空闲 (Espresso AppNotIdleException), 单独重复 3 次为 2 失败 1 通过; 同一场景此前在另一 AVD 三轮完整运行与远端 API24 / API35 作业均通过, 本轮未改动该场景或渲染器, 记为环境性波动; 远端复跑 (工作流运行 37734947171): API35 x86_64 作业 73 项全部通过; API24 x86 作业首次因 CI 模拟器 adb 守护进程连接失败, 安装包未写入设备而 0 项执行 (基础设施问题, 上传的 logcat 无 PackageManager 错误), 重跑后 73 项全部通过; Markdown 与 Icon Studio 工作流通过.
- 未执行: 真机, API24 本地 instrumentation, 第三方启动器截图, 六设备矩阵, 宿主全量 JVM / lint, 新 Q5 基线; 专用 AVD 与自有安装在会话结束时清理.

### 2026-10-08: F.5 补正, 画廊从启动器打开即崩溃

- 维护者报告: Xperia XQ-DQ72 (API 33) 与自有 AVD 上启动器出现 Compose UI 图标, 点击后无法进入. 设备 logcat 显示插件进程在 `GalleryActivity` 构造时抛 `NoClassDefFoundError: androidx.arch.core.executor.ArchTaskExecutor`. 根因: D32 的 36 项打包集合只收录 arch `core-common`, 漏掉 `core-runtime`; 对收缩后的发布包做 DEX 引用闭包分析, 除契约类外还缺 `customview-poolingcontainer`, `emoji2` 与 `androidx.window` 共 15 个类. 设备测试全部通过却未发现, 是因为测试 APK 携带整份共享锁的运行时副本并与画廊同进程, 掩盖了缺口; R8 也不会报告, 因为 compileOnly 构件作为库类路径传给 R8.
- 修正 (build44): `standaloneRuntime` 扩为 47 项 (补入 arch core-runtime, customview-poolingcontainer, emoji2, lifecycle-process, window / window-core / window-core-android, 以及完整 keep 后被保留的 savedstate / lifecycle 成员引用的 lifecycle-livedata-core 与 kotlinx-serialization bom / core / core-jvm; 共享锁 49 项非 Kotlin 构件只剩无人引用的 appcompat x2 不打包). 首个候选包只对新增包 `-keepnames`, 插件自身的 73 项 instrumentation 与启动检查全部通过, 但隔离宿主的 Compose 设备套件 7 项 RENDER_FAILED 且宿主进程最终崩溃; dexdump 发布包显示 Compose `EmojiCompatStatus.getFontLoadState` 被 R8 编译为 `EmojiCompat.get(); throw null`: 插件程序从不初始化 EmojiCompat (startup provider 已移除), R8 据此推断 `EmojiCompat.get()` 永不正常返回, 而宿主内 parent-first 使用的是宿主已初始化的 EmojiCompat, `get()` 正常返回后即执行 `throw null`. `-dontoptimize` 不阻止这种整程序推断, 因此共享包改为与 `kotlin.**` 相同的完整 `-keep` (代价是这些包不再裁剪), 与 P0.2 的 Kotlin 结论一致. 测试 APK 不再携带宿主独有构件 (appcompat x2), `verifySharedClasspath` 守卫; `verify_apk_classpath.py --shrunk` 要求 R8 发布包引用的每个类都在 APK, 平台 (含 androidx.window 按需反射的 OEM 扩展) 或宿主契约中; 新增 `verify_standalone_launch.py`, 在设备上安装 APK, 像启动器一样启动已启用的 alias, 经 uiautomator 无障碍树打开首个条目与设置页, 以进程存活与 logcat 无 FATAL 为准. `build.yml` 的 JVM 作业对 release 包加 `--shrunk`, 两个模拟器作业在 instrumentation 后用 debug APK 执行启动检查. D32, AGENTS 第 4 / 5.2 / 12 / 13.2 / 13.3 / 14 节与 10 语言 changelog 的依赖条目同步修订.
- 验证: 旧签名包 `2f1127ce` 在自有 API35 AVD `compose_f5c_api35` 上以启动检查复现崩溃, `--shrunk` 检查报 15 个不可加载类; 新包: JVM 84 项通过, lint 0 错误 / 9 警告, `verifySharedClasspath` 51 项中 47 项打包 / appcompat 2 项不入测试 APK, debug 与 `--shrunk` release 的类路径检查通过, debug 与签名 release 的启动检查通过 (画廊 / Column 条目 / 设置页, 进程存活), 插件 instrumentation 73 项全部通过 (0 失败 / 0 跳过; 测试 APK 已不携带宿主独有构件); 隔离宿主 `org.autojs.autojs6.compose.spike` (宿主 master `910aec9495`) 的 compose 设备包 87 项中 71 项通过, 15 项按假设跳过 (需显式参数的性能 / 旧 provider / 打包应用 / 包变更 / 权限负例阶段, 小米 IME 与旧宿主 V2 门禁), 1 项失败. 10 项 TSX 场景在完整运行中因新建 AVD 未安装 TypeScript Engine 而失败, 临时安装 0.6.8 后复跑 10 项全部通过 (计入 71 项); 唯一失败 ComposeUiLoaderTest.loaderCachesByIdentityButRechecksEnablementAndSharesParentClasses 断言 contractVersion() 等于 1 (宿主测试 2026-10-02 写入, 早于 F.4 把契约提升为 2), 属宿主侧既有过期断言, 与本次改动无关; 本轮 87 项比 F.5 当日的 74 项多出 core/plugin/compose 装载套件 13 项. 首个仅 -keepnames 的候选包在同一套件 7 项 RENDER_FAILED 并最终宿主进程崩溃, 完整 keep 后不再出现. 签名包 `autojs6-plugin-compose-ui-v1.1.0-5d2ce3ef.apk` 4673146 B, 13482 类 / 82466 DEX 方法引用, SHA-256 `9856d4f57b361d7597cfcee39c9e50b5466cca644029a6081127c057e2890dd7`, 较 build42 +1048858 B / +21853 方法引用, 记入 `docs/dev/p7-f5-size-review.json`, 仍属维护者预先接受的 F.5 增量, Q5 复核线不变.
- 工作树说明: 会话中主工作树出现维护者未提交的 Icon Studio 改动 (`.icons/recipe.json` 几何参数, 新图稿资产与再生成的 mipmap 资源, 17:29:58 写入), 其生成的 `mipmap-anydpi-v26/ic_launcher_system*.xml` 把底色写成字面量 `#212121` / `#fafafa`, aapt2 拒绝链接 (`'#212121' is incompatible with attribute drawable`). 这些文件未被触碰也未纳入提交; 本轮构建, 测试与签名包均在以 HEAD 创建的独立 worktree 中完成. 宿主侧未改代码, 为跑设备矩阵在短路径独立 worktree 构建隔离宿主 (长路径 worktree 因 Windows 文件名长度限制检出失败).
- 远端 CI (推送 `ab46e00`, build44): Markdown 工作流通过; Build integrity 的 JVM / APK 作业与 API35 x86_64 作业 (73 项 + 启动检查) 通过, API24 x86 作业 73 项全部通过但新加的启动检查超时: Nexus 5 (1080x1920) 上画廊详情页的复制 / 运行按钮位于首屏之下, uiautomator 只导出可见节点. 自建 API24 x86 Nexus 5 AVD `compose_f5c_api24` 复现 (可见节点为 Preview / First / Second / Third / Script / 脚本文本 / Column, 且 API24 偶发首次 `cat` 导出文件返回 255). 修正 (build45, 仅脚本与文档): 详情页判定改为 "脚本文本含 `compose.<条目>(` 或出现复制按钮", 读取导出文件时容忍首次失败并重试, 超时时把可见标签写入错误信息; 同一 AVD 上签名包 `5d2ce3ef` 的启动检查通过 (画廊 / Column 条目 / 设置页). 插件与宿主侧结论不变.
- 远端 CI (推送 `50b0c02`, build45): Markdown 通过; JVM / APK 作业与 API24 x86 作业 (73 项 + 启动检查) 通过; API35 x86_64 作业 73 项中 10 项失败, 与 build41 首轮相同的窗口焦点 / 弹窗 / IME 簇 (WideRendererTest 搜索 / 抽屉 / 底部弹层 / 下拉菜单 / 工具提示, TextField IME, Dialog 焦点 x2, Advanced Scaffold, 以及画廊剪贴板), 该簇在 build42 / build43 的远端复跑与本地两台 AVD 上均通过, 记为 CI 模拟器的环境性波动; 随后的启动检查把屏幕上残留的 ANR 对话框按钮 "Close app" 当成了首个条目. 修正 (build46, 仅脚本与文档): 启动检查只考虑 `package` 为插件包名的节点, 超时信息仍列出全部可见标签以便看出系统对话框; 重建的 API35 AVD 上签名包 `5d2ce3ef` 的启动检查通过.
- 远端 CI (推送 `81c7388`, build46, 工作流运行 37768208379): Markdown 通过; Build integrity 三个作业全部通过, API24 x86 与 API35 x86_64 作业各 73 项 instrumentation 通过且启动检查 `STANDALONE_LAUNCH_OK` (画廊 / Column 条目 / 设置页). 本次 F.5 补正至此在插件自身进程 (两种 API 级别的模拟器, 本地与远端), 宿主进程 (隔离宿主设备套件) 与静态引用闭包三个层面都有证据.
- 未执行: 真机复测 (维护者的 Xperia 与 AVD 未被占用; 请以 build44 / build45 包重新安装验证), API24 本地 instrumentation (远端 API24 作业覆盖), 第三方启动器截图, 六设备矩阵, 宿主全量 JVM / lint; 自有 AVD 与两个独立 worktree 在会话结束时清理.


### 2026-10-08 Icon Studio background resource linking fix

- 维护者报告在 Icon Studio 应用图标调整后, 四个 `ic_launcher_system*` 自适应 XML 的背景写成 `#212121` / `#fafafa`, 导致 aapt2 拒绝资源链接. 根因为 Icon Studio 1.3.0 的固定亮 / 暗背景输出把颜色字面量写入只接受资源引用的 `android:drawable`.
- Icon Studio 1.3.1 与本仓库的便携内核已同步修复: 固定亮 / 暗背景引用独立的 `@color/ic_icon_studio_background_day` / `night`, 仅在默认 values 定义以避免被系统夜间模式覆盖; 自动模式继续使用独立的默认 / notnight XML. 内核兼容旧配方, 对既有错误字面量快照也会修复, 合法且未改动的引用保持原样. 本项目四个 XML 与颜色资源由保存配方重新生成.
- 维护者的图标参数和 14 个 PNG 在修复前后逐字节核对一致, Manifest 也未改变. 本次只改变背景的资源引用方式, 未改动图案或几何. 10 语言 changelog 与生成文档已同步.
- 验证: 工作区 v1.1.0 / build47 的 assembleDebug, assembleRelease, 84 项 JVM 测试, lintDebug / lintRelease (均 0 错误), verifySharedClasspath 全部通过; debug 与 shrunk release 的 APK 类路径检查通过. release 为 4670014 B / 13482 类 / 82466 方法引用, 方法引用与 F.5 已验收记录相同. 图标重现检查 24 项, Markdown 10 语言 / 36 项通过. Icon Studio 72 项 Python 测试与 13 项浏览器测试通过, 其中新增固定亮暗 / 自动模式, 自定义与透明背景, 单双引号 XML, 旧字面量恢复及便携生成回归.
- 本轮不涉及 Manifest, 渲染器, 契约, 类加载边界或依赖集合, 未另行执行设备安装与 instrumentation. 维护者先前的未提交图稿修改仍保留在工作区; 按 AGENTS 第 3.3 节暂不自动提交这些工作区文件.

### 2026-10-11: API24 远端插桩的焦点 / 注入失败簇

- 现象: 推送 `cf72ef8` 与 `7e7e0df` 后, Build integrity 的 API24 x86 作业两次失败 (运行 38125259694 / 38127408457). 画廊 `copyPutsTheScriptOnTheClipboardAndRunResolvesTheInstalledHost` 在等待窗口焦点处超时 (第 7 项), 之后依赖返回键 / Tab 注入或窗口焦点的渲染器场景报 `Injecting to another application requires INJECT_EVENTS permission` 或焦点超时: 第一次运行 12 项全部失败, 第二次前 6 项失败, WideRendererTest 的 6 项又恢复通过; 两次套件后的启动检查都通过. 10-08 的通过运行与失败运行使用同一模拟器 (37.2.12) 与 API24 google_apis x86 镜像 (r27), 其中 runner 镜像 20261004.327.1 在 10-08 也通过; 10-11 的提交只涉及图标, 标点测试与 `.gitattributes`.
- 根因: 与插件代码和画廊测试无关. 画廊测试从不启动宿主或其他应用 (运行按钮未被点击, 只构造并解析 Intent). 失败簇的形态是另一个进程的系统错误对话框从第一个需要焦点的检查之前就占据最上层窗口与输入焦点: API 24 的 InputDispatcher 对焦点窗口属于其他 uid 的注入返回 PERMISSION_DENIED, 即上述 SecurityException; Compose 测试的点击直接派发到视图层级, 不受影响, 所以其余场景照常通过. API 24 的 ANR 对话框不可取消, 只在点击或该进程死亡时消失 (`AppErrors`, `AppNotRespondingDialog` 源码), 与 "持续到中途或套件结束" 一致; build45 的 API35 远端作业也曾以同一失败簇伴随残留的 ANR 对话框. 远端产物无法确认是哪个进程: 工作流在启动检查之后才保存 logcat, 而启动检查先执行 `logcat -c`, 逐测试 logcat 也未上传.
- 复现: 自有 `AVD_API_24` (只读启动, 宿主以 `pm hide` 隐藏以对齐 CI) 与按 CI 参数新建的临时 API24 Nexus 5 AVD (2 核, swiftshader, 每次全新启动) 上原样执行 CI 命令, 共 5 次自然运行均未复现 (其中一次因模拟器 goldfish 图形 DMA 段错误导致 system_server 重启, 与本问题无关). 在启动器上制造真实的 ANR 对话框 (冻结 launcher 进程并排队两个按键) 后, 基线代码在两台 AVD 上都得到与运行 38125259694 完全相同的 12 项失败.
- 修正 (build98 `9ab4286` 测试, build99 工作流与本记录): androidTest 新增 `ForeignErrorDialogGuard`, 作为 runner `listener` 在每项测试开始前读取 `ActivityManager.getProcessesInErrorState()`, 对其他进程的 ANR 对话框点 "等待" (进程继续运行), 对崩溃对话框发送 `CLOSE_SYSTEM_DIALOGS`, 并在 logcat 记录进程名; 插件自身 (含测试 APK) 的错误只记录不处理. 正常情况下不连接 UiAutomation; API 31+ 该接口只返回调用方自身的进程, 守卫在那里不会处理其他应用的对话框. GalleryDeviceTest 的剪贴板测试只在 API 29+ (读取剪贴板需要焦点) 等待焦点; 运行按钮拆为独立测试: 宿主缺失时点击只显示 "未安装" 提示且不启动任何 Activity, 宿主已安装时只验证 Intent 可解析而不启动, 以免宿主窗口留在后续测试前面 (插件测试由 73 项变为 74 项). 工作流在套件结束后, 启动检查之前保存 logcat (main / system / crash / events) 与窗口状态, 并上传逐测试 logcat (`outputs/androidTest-results/connected`).
- 验证 (自有 `AVD_API_24`, 宿主隐藏, CI 命令 `createDebugAndroidTestCoverageReport -PcomposeUiCoverage=true`): 有 ANR 对话框时 74 项全部通过, 守卫在第一项测试前点掉 launcher 的对话框并记录, 随后启动检查 `STANDALONE_LAUNCH_OK`; 无对话框时 74 项全部通过, 守卫无输出, 启动检查通过; 取消隐藏宿主后 GalleryDeviceTest 4 项通过且宿主未被启动. 临时 AVD 上有对话框的完整运行 74 项中 73 项通过, 唯一失败为既有的 `WideRendererTest.pullToRefreshUsesActualNestedScrollAndHonorsDisabledState` 主线程不空闲 (AppNotIdleException, 见 2026-10-08 记录), 在守卫已连接 UiAutomation 的情况下单独复跑 WideRendererTest 15 项全部通过. `prepush_check.py` 全部通过.
- 未执行: 本地 API35 运行, 远端 CI (本次只本地提交, 未推送); 远端两次失败中具体是哪个进程的对话框仍待推送后的 logcat 确认. 临时 AVD 已删除, `AVD_API_24` 以只读方式启动, 用后关闭.

### 2026-10-13: P8 设计器, 1.2.0 本地 gate, 真机补验证与 P9 规划

- 维护者分四轮拍板 P8 (D33 - D40, 修订 D1 / D8 / D26 / D32, Q8 - Q11), 同日完成 P8.0 改名 (插件 7dbc3b2 / c0fc2aa, 宿主 f12665ffde), P8.1 f19a201, P8.2 398a3c7, P8.3 37b418d, P8.4 16e7430 (宿主 db7e6a1734), P8.5 1e09e86 (文档站 8538de3 / 219dd49, 离线文档 5f7bf9f, 宿主 f4b53b28df 修正 `ComposeUiLoaderTest` 的过期断言); 各小节证据见对应段落与 `docs/dev/p8-designer-evidence.md`.
- 维护者随后确认: 接受 1.2.0 gate 包为新体积基线 (d991e80, 复核线 5603798 B / 96392 引用, 写入 `AGENTS.md` 第 14 节); 暂不推送, 五个仓库只做本地收尾; 用 Sony G8441 / Redmi 22120RN86C / Xiaomi Pad 补真机; P9 的三个方向只做规划.
- 文档: 文档站 f21f168 把 compose 模块页开头的适用版本由 1.1.0 改为 1.2.0, 1395253 在设计器页补充横竖屏布局, 9b4350a 计数器 104; 离线文档 0b81293 (build 133, provenance 指向 1395253, 未发布的 6.8.6 设计器条目 10 语言补充横竖屏布局).
- 真机补验证: 首轮 (gate 包 build 106) Redmi 87 / 87; G8441 (横放, 640 x 360 dp) 83 / 87, 设置页 3 项点击了滚出可视区的行或选项, 大纲上移 1 项被叠放布局下的 "+" 按钮挡住, 复跑相同. 修正: 23f1814 编辑页在宽大于高时画布与面板左右分栏, 大纲与属性列表底部留出 "+" 按钮的空间 (1.2.0 设计器条目 10 语言补充一句); 4c41ca2 设置页测试先滚动再点击, 新增导出到文件的两个用例 (Instrumentation 监视器代答系统文件选择器). 新脚本 `verify_designer_recovery.py` (真实 `am kill`) 在 G8441 上发现 "放弃" 退出后 onStop 又写回草稿的缺陷, 0c632e9 修复并增加 `DesignerDeviceTest.discardingOnExitLeavesNoDraftBehind`; 脚本提交 9cdadf7 (另有 `verify_host_handoff.py`). build 110: 两台真机插件 instrumentation 各 90 / 90, 启动检查与草稿恢复在两台真机和 AVD_API_30 通过, 宿主编辑器往返在 AVD_API_30 通过; Redmi 上宿主侧 compose 矩阵 12 类 68 项 (53 通过, 15 按假设跳过, 0 失败) 与 `ComposeUiLoaderTest` (debug 插件) 7 / 7. JVM 121 项, lint 0 错误, classpath debug / `--shrunk` 通过, release 5094845 B / 87646 引用.
- 设备协调: 3-Maple Image 与 Ace 1.44.0 两个会话同时使用这批真机. Xiaomi Pad 的首轮在对方开始测试后停止并让出 (设备上的插件由 release build 104 换为 build 106); G8441 上一次与 Ace 会话的重叠使一个 IME 用例失败, 完整复跑通过. 结束时两台真机上的插件, 测试包与 spike 宿主均已卸载, AutoJs6 宿主与 TypeScript Engine 未改动.
- P9 (1897550): 调研本仓库, 宿主与 TypeScript Engine 的现状后写入画布与预览精度 / TSX 工程 / 交互预览三个小节与附录 D 的 Q12 - Q16, 全部未勾选. 维护者随即分两轮拍板 Q12 - Q16, 结论写入 D41 - D45; 与建议不同的是 Q13: TSX 语法树由 TypeScript Engine 的独立解析契约提供 (而非插件内解析器), 缺少引擎时 `.tsx` 只读打开, P9.2 条目据此改写为宿主 / TypeScript Engine 的解析契约与插件侧客户端两部分.
- 未执行: 真机上的宿主编辑器往返 (两台真机的 AutoJs6 6.8.0 / 5352 是 10-12 安装的, 早于 db7e6a1734 且版本号相同, 未覆盖维护者设备上的宿主; spike 宿主的 `AppFileProvider.AUTHORITY` 写死为 `org.autojs.autojs6.fileprovider`, 授权失败, 不能代替); 真机上的宿主无障碍类与 TSX 类 (需改设备无障碍设置; 设备上 TypeScript Engine 为 0.6.3); API 24 AVD 的 TSX 编译器 Worker 超时 (TypeScript Engine 侧的预热探测, 默认 30 s); 设计器性能基线; 远端 CI 与 D7 的远端步骤 (按维护者决定暂不推送); 宿主 6.8.0 changelog 的其余 9 种语言 (由维护者同步).

### 2026-10-13 (续): 真机上的宿主往返, TSX 类与设计器性能基线

- 维护者授权在真机上按需覆盖安装宿主与插件, 也可自行构建最新版本. 以宿主 master `6e09d9abfd` 构建正式包名的 debug 宿主 (`:app:assembleAppDebug`, 不自增构建号, 同签名覆盖保留数据) 安装到 G8441 与 Redmi; Redmi 的 TypeScript Engine 由 0.6.3 升为 0.6.8 debug (同签名). 设备使用事先与 3-Maple Image, Ace 1.44.0 两个会话协调.
- 宿主编辑器与设计器往返 (`verify_host_handoff.py`) 在 Redmi (API 33) 与 G8441 (API 28) 通过; 脚本修正 f690b2f: 横屏 G8441 上编辑器溢出菜单超出屏高需滚动到 "More", Redmi 上组件面板可能停在 Button 被弹层顶边裁切处 (点击落在拖动手柄), 改为先回滚露出再点. 草稿恢复脚本在两台真机复跑通过.
- 宿主侧 `ComposeTsxDeviceTest` 在 Redmi 10 / 10 通过; API 24 AVD 的失败原因查明: 镜像 WebView 为 53.0.2785.124, TypeScript Engine 要求 Chrome / WebView 119 以上并对 API 24 / 25 自带的旧 provider 直接拒绝, 属运行环境限制, 与本插件无关. `ComposeAccessibilityDeviceTest` 需在设备设置中开启无障碍服务, 仍未在真机运行.
- 设计器性能基线 (355654e): 新增默认跳过的 `DesignerPerformanceDeviceTest` (`-e designerPerformance true` 开启), 在 AVD_API_24 / AVD_API_30 / G8441 / Redmi 采集解析 / 校验 / 预览派生 / 生成, 编辑页打开与撤销 / 重做到画布的耗时, 记录在 `docs/dev/p8-designer-performance.md`; 管线各阶段 p90 不超过约 50 ms (56 个示例合计), 编辑页打开 0.2 - 0.7 s, Redmi 上 251 节点文档的撤销 / 重做约 0.7 - 0.8 s. 暂不设复核线, P9.1 增加一条先分清测试同步开销与真实帧耗时, 再决定是否做增量预览的条目.
- 结束时两台真机为新宿主 + 3-Block Compose 1.2.0 release (build 110), spike 宿主与测试包已卸载.
- 仍未执行: 真机上的宿主无障碍类 (需改设备无障碍设置), 远端 CI 与 D7 远端步骤 (按维护者决定暂不推送), 宿主 6.8.0 changelog 其余 9 种语言 (由维护者同步).

### 2026-10-13 (晚): P9.1 与 P9.3 第一步, 1.3.0 本地 gate

- P9.1 全部完成 (build 119 - 127): 画布拖放按排列方向插入, AndroidView 占位, 导入来源同步, Image 预览, 修饰链拖动, 弹层画在画布内, 大文档编辑延迟, 测试汇总项; 各条证据见 P9.1. P9.3 第一步 (状态面板, 交互预览) 与编辑 / 预览模式切换完成 (build 128); P9.3 的文档与测试条目都覆盖第二步 (沙箱的可用 API, 计数器按钮点击后画布更新), 第一步部分已在条目下注明进度, 两条仍未勾选.
- 大文档编辑延迟的调查: 挂着 compose 测试规则时重组由测试时钟驱动, 新增不挂规则的 `DesignerFramePerformanceDeviceTest` 后用 `FrameMetrics` 与 simpleperf `--trace-offcpu` 定位到两处根因: 渲染器按位置调用子节点 (前面插入或删除兄弟会重建其后所有子节点的组合, 宿主同样受影响, `0ec4998` 修复并在 changelog 记为修复), 设计器每次给渲染器新帧 (`LiveFrame` 让未变化的节点跳过); 修正后又发现画布以 `SnapshotStateMap` 记录节点边界是平方级. 同一 AVD 上 undo / redo 到首帧 p50 约由 365 / 300 ms 降到 140 / 130 ms, Redmi 上可见性计时 p50 由 795 / 693 ms 降到 306 / 257 ms.
- 实现交互预览时确认 1.2.0 起的缺陷: 在属性抽屉修改输入框的 text 后画布不更新 (渲染器把 text 当初值); 设计期钩子 `TextFieldController.replaceText` 修复, changelog 记为修复.
- 真机: 经 3-Maple Image 与 Ace 1.44.0 会话同意借用 Redmi 22120RN86C (bek749scrwv4wo8h), 复测编辑耗时并以新脚本 `verify_source_sync.py` 走真实系统选择器验证导入来源的持久授权 (两次通过); 用完卸载 debug 包, 装回插件 1.3.0 release (build 126), 删除测试文件并通知两会话. 未使用 968e9f18 / QV770340J7 (3-Maple Image 大库测试独占), QV710AF65F (维护者手动测试 Ace) 与 G8441.
- 文档: 文档站 `1e8894d` (设计器指南按 1.3.0 更新, 新增 "预览状态与交互预览" 一节) 与 `6aa273c` (计数 105); Offline Docs `ed59755` (build 134, `--verify-offline` 通过); 插件 README 10 语言 `a3b78d5`.
- 1.3.0 本地 gate (build 129 源码): Markdown / 图标检查, JVM 138, 构建 + lint + 共享类路径, APK 类路径 (release 5167317 B / 88223 引用, 在复核线 5603798 B / 96392 之内), AVD_API_30 全套 instrumentation 111 项, release 包的独立启动 / 草稿恢复 / 宿主往返脚本均通过; 结果表见 `docs/dev/p9-designer-evidence.md`.
- 未执行: 宿主侧 compose 设备矩阵 (本版唯一影响宿主内渲染的是 `0ec4998`, 插件自身的渲染器 instrumentation 已覆盖), `appendDigestToReleasedFiles`, 远端 CI 与推送 (维护者决定暂不推送), 其它真机.
- 下一步: P9.2 TSX 工程 (1.4.0) 需要宿主 `plugin-api` 新增解析 AIDL 与 TypeScript Engine 实现 (D42), 涉及两个关联仓库, 开工前与维护者确认排期; 或由维护者决定 1.2.0 / 1.3.0 的推送与发布节奏 (D7).

### 2026-10-13 (夜): P9.2 TSX 工程, 1.4.0 本地 gate

- 维护者批准开始 D42 并同意宿主与插件协同修改; 经提问确定: 解析在 TypeScript Engine 同进程的独立页面与 Worker 中进行, 修订 TypeScript Engine 路线图 "新能力一律走 additive Bundle key" 一条并注明解析契约是唯一例外, TypeScript Engine 版本 0.7.0.
- 宿主 `fce22c83a6`: `plugin-api/typescript-parser-api` (契约 v1, 宿主 app 不打包). TypeScript Engine `1e92487` (0.7.0): 解析服务, 桌面 Worker 门禁与设备测试; 该仓库 `check` 中 `verifyOptionalPluginIndependence` 与 S10 漂移任务在本次之前已失败, 与解析服务无关, 未处理.
- 插件 `85d518c` (build 131, 功能与测试), `dab591b` (132, README), 本条所在提交 (133, 路线图与证据): 契约 AAR 入锁, `TsxParserClient`, `TsTree`, `TsxReader`, `JsxText`, `TsxWriter`, `DocumentReader` / `DocumentWriter`, `Dialect`, `.tsx` 工程与新建选项, TSX 运行交 content Uri, 只读打开, 处理函数的 TypeScript 语法检查; 树快照与生成脚本. 各条证据见 P9.2.
- 设备: 经 Ace 1.44.0 与 3-Maple Image 会话同意使用小米平板 23046RP50C (968e9f18, TypeScript Engine 由 0.6.8 换成 0.7.0 release, 插件为 1.4.0 debug) 与 Redmi 22120RN86C (bek749scrwv4wo8h, 插件 1.4.0 debug); 未使用 QV710AF65F (维护者手测 Ace), 未动 3-Maple Image 与 Ace 的安装包. 平板全套 instrumentation 有 5 项因测试按系统语言查找提示文字而设计器跟随 AutoJs6 语言 (英语) 失败, 在 AVD 上全部通过; 新测试改用 testTag.
- 1.4.0 本地 gate: Markdown / 图标检查, JVM 154, 构建 + lint + 共享类路径, APK 类路径 (release 5219321 B / 88672 引用, 在复核线之内), AVD_API_30 全套 instrumentation 115 项, release 包的独立启动 / 草稿恢复 / 宿主往返脚本通过; 结果表见 `docs/dev/p9-tsx-evidence.md`.
- 未执行: 宿主侧 compose 设备矩阵 (本版不改宿主内渲染), `appendDigestToReleasedFiles`, 远端 CI 与推送 (维护者决定暂不推送), API 24 上的解析服务.
- 同日另修 1.2.0 起的缺陷 (build 135): 无法写回的外部脚本另存为工程时, 文件名含空格或括号 (如 `test (1).js`) 会在 `ProjectStore.requireValidName` 抛异常使插件闪退; `ProjectStore.stemOf` 统一清理导入与另存的文件名 (非法字符换成 `-`, 去掉首尾与重复的点, 截到 64 字符). AVD_API_30 上未修复的构建复现了该异常, 修复后 `DesignerHostDeviceTest` 9 项与新增的 `ProjectNameTest` 通过.
- 待维护者决定: 宿主编辑器菜单是否对 `.tsx` 要求插件 1.4.0 (现对任何文件显示, 1.2.0 / 1.3.0 打开 `.tsx` 会显示无法打开); 1.2.0 - 1.4.0 与 TypeScript Engine 0.7.0 的推送与发布节奏 (D7); P9.3 第二步是否立项 (D43).

### 2026-10-14: 1.4.0 发布与索引准入, P9.3 第二步立项

- 维护者授权项目随时推送并发布 Release (D7 再修订), 经提问选定发布范围 (3-Block Compose 1.4.0, TypeScript Engine 0.7.0, 官方索引登记与准入, 文档站与离线文档推送), 并按路线图方案立项 P9.3 第二步 (D43).
- 推送: 插件 master (含 1.2.0 - 1.4.0 的全部提交), TypeScript Engine, 文档站与 Offline Docs. TypeScript Engine `v0.7.0` (5 个 APK + `SHA256SUMS`) 发布; 其 CI 的原生对齐作业因 NDK 下载失败重跑后通过.
- 插件 CI: API 24 作业首次因模拟器 system_server 崩溃中断, 重跑后两项设备测试失败. 在本机 API 24 模拟器 (Nexus 5 尺寸 1080x1920 / 480 dpi) 复现: 状态面板第一行开关在小屏上位于面板可见区之外, 大纲第 3 行被 "已重新导入" 提示条盖住, 点击都落空; 另查到 API 24 在非触摸模式启动时把焦点交给画布中的输入框并弹出键盘. `18f5861` 只改测试 (滚动到开关, 等提示条消失, 放宽 API 24 上 Activity 销毁的等待), CI 全部通过; 画布焦点问题随 1.5.0 修复. 发布 tag 仍指向 APK 的来源提交 `bd624a6` (与 `18f5861` 的应用源码相同).
- 发布: GitHub Release `v1.4.0` (APK 5221337 B, SHA-256 `7fb611c3...`, 与 `SHA256SUMS`); 官方索引 `b3085e8` 登记仓库并准入 3-Block Compose 1.4.0 (manifest 136) 与 TypeScript Engine 0.7.0 (manifest 123, 4 个 ABI), 生成器校验 GitHub Release 通过, 索引测试与 Markdown 检查通过.

### 2026-10-14 (续): P9.3 第二步, 1.5.0 发布与索引准入

- 按路线图方案实施第二步 (修订 D8 / D35 / D43), 证据见 P9.3 与 `docs/dev/p9-sandbox-evidence.md`; 设计要点: 沙箱只经字符串桥接, 运行时的树以 `LiveTree` 交给原有的帧构建器, 输入框经带序号的编辑命令同步, 沙箱在 API 26+ 启用.
- 1.5.0 本地 gate: generate_markdown / generate_icon_studio --check, JVM 173 项 (TSX 快照记录器照常跳过), assembleDebug / assembleDebugAndroidTest / lintDebug (只有 1.5.0 之前已有的问题) / assembleRelease / verifySharedClasspath, verify_apk_classpath (debug, release --shrunk unresolved=0); release 包在 AVD_API_30 上 verify_standalone_launch (sandbox=ran) 与 verify_designer_recovery 通过 (第一次未能把面板滑到 Button, 重跑通过).
- 设备: emulator-5560 (AVD_API_30) 与只读 AVD_API_24 实例 emulator-5570 (Nexus 5 尺寸), 期间按 3-Maple Image 会话的请求借出过; 未使用真机. 全套 instrumentation 的结果与两处非回归失败的对照见 P9.3 测试一项.
- 未执行: verify_host_handoff.py (宿主与交接未改动); 真机.
- 推送与 CI: 插件 `fe61b63` (功能, build 139) / `8bc620a` (路线图与证据), CI 全部通过 (单元测试与 APK, API 24 与 API 35 全套 instrumentation); 文档站 `34def8e` / `757f098` (重新生成, 158 个模块) / `0e9a0e9` (计数 107), Offline Docs `b7c5443` (VERSION_BUILD 137, `--verify-offline` 同步), CI 通过.
- 发布: GitHub Release `v1.5.0` (tag = `8bc620a`, 与 `fe61b63` 的应用源码相同; APK 5615650 B, SHA-256 `c6616272...`, 与 `SHA256SUMS`), 最终 APK 在 AVD_API_30 上再跑 `verify_standalone_launch.py` 通过 (`sandbox=ran`); 官方索引 `8b095f6` 准入 3-Block Compose 1.5.0 (manifest 139), 索引测试与 Markdown 检查通过.
- 待维护者决定: 1.5.0 APK 超出复核线 (AGENTS 第 14 节) 11852 B 是否接受 (方法引用未超线); 确认前复核线保持 5603798 B / 96392.
