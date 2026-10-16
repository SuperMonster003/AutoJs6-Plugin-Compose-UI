# P11 路线图: 设计器性能复核线, 预览差异的明示与缩小, XML 文件工程, TSX ui 布局, compose 联动与宿主编辑器预览 (1.8.0 - 2.0.0)

本文是 `ROADMAP.md` 阶段 P11 的专门路线图, 由维护者于 2026-10-15 经四轮选择题拍板 (D54 - D61). 它承接 P10 (1.6.0 / 1.7.0 的 UI (XML) 工程与交互预览) 结束时余下的三类事项: 尚未设复核线的设计器性能指标, P10 第 4 节列为非目标的四项, 以及 3.5 节 "只能明示" 的残余偏差. 记法与 P10 路线图相同: 决策固定, 条目可勾选, 证据写在条目之下; 待决事项在附录 D, 进入对应阶段前拍板.

---

## 0. 结论

- 设计器性能第一次有复核线: 只以 Redmi 22120RN86C 为准, 规则沿用 P5 (p90 加 50% 向上取整至 5 ms), 以 1.7.0 第二轮的 16 项为初值, 由脚本在每次 release 前自动比对. 冷启动 / PSS / 滚动继续只记录.
- 嵌入预览与真实运行的差异分两步: 先在问题列表里按脚本逐条明示 (1.8.0), 再由宿主在预览会话中缩小四项: 对话框画在画布内, 生命周期 pause / resume 与返回键, 窗口级函数的可见效果, 忙等保护 (1.9.0, 契约 v3).
- 四个 P10 非目标全部进入: `ui.layoutFile` / `useAndroidLayout(true)` 的 XML 文件可编辑可预览, 外部文档与私有工程都支持, 私有工程按需升格为文件夹 (1.8.0); `.tsx` 中的 ui 布局经 TypeScript Engine 解析并规范化写回 (1.9.0); `<compose>` 容器内的 compose 内容由插件叠画并可编辑 (1.9.0); AutoJs6 编辑器内的布局预览面板 (2.0.0, 宿主侧).
- 宿主改动全部按 "契约只追加, Binder 身份仍答 v1, 新能力经 `getPreviewInfo()` 宣告, 插件按能力降级" 的既有做法落地, 留在本地 master, 不等待宿主发布; 插件的 README / changelog 像 1.6.0 / 1.7.0 一样写明所需的宿主提交.

---

## 1. 维护者决策 (2026-10-15, 四轮选择题)

| 决策 | 内容 |
| --- | --- |
| D54 设计器性能复核线 | 只设设计器性能线 (两个 opt-in 测试类的 16 项), 只以 Redmi 22120RN86C (1.2.0 基线机) 为准, 规则为 p90 加 50% 向上取整至 5 ms, 初值取 1.7.0 第二轮 (`docs/dev/p8-designer-performance.md` 的建议表); 新增 `.python/verify_designer_performance.py` 在指定设备跑两个测试类并与线比对, 超线即失败, 每次 release 前在 Redmi 上执行; Pad 与模拟器只记录不比较; 冷启动 / PSS / 滚动没有新样本, 继续只记录 |
| D55 残余偏差 | 明示与缩小都做. 明示: 解析时扫描脚本用到的 API, 在问题列表增加 "预览差异" 一类 (信息级, 不算错误), 每条带行号与一句说明, 点开跳到文档. 缩小四项: 对话框画在画布内, 生命周期事件 (设计器不可见 / 可见时发 pause / resume, 画布上方行加返回键发 back_pressed), 窗口级函数的可见效果 (仿状态栏与背景), 忙等保护 (宿主对预览运行挂指令计数观察器, 阈值默认 5 秒, 插件设置页可调, 经 `runScript` 请求传给宿主). 输入法 API 33 - 34 与弹出层 API 30 - 32 是平台限制, 不列 |
| D56 XML 文件 | `ui.layoutFile` / `useAndroidLayout(true)` 的 XML 文件可编辑 + 预览; 外部文档 (宿主编辑器 / 文件管理器交来的脚本) 旁边同目录的 XML 文件按片段打开, 无损编辑并写回原文件; 私有工程也支持: 工程仍是单文件, 第一次引用或新建 XML 文件时升格为文件夹 (`name/` 下 `main.js` 与资源文件), 导入 / 导出支持文件夹, 运行与导入 AutoJs6 改为交出文件夹的 content Uri |
| D57 TSX ui 布局 | classic TSX 的 `ui.layout(<vertical>...)` 经 TypeScript Engine 解析服务读出, 保存时被编辑的 `ui.layout` 实参按 `TsxWriter` 规范 (4 空格, 单引号, 每子元素一行) 重写, 其它源码原样; 与现有 Compose TSX 工程一致 |
| D58 compose 联动 | 布局预览中宿主绘制 `<compose>` 容器的占位并回传边界, 插件用自己的真渲染器把容器对应的 `compose.attach` 渲染函数的节点树画在该边界上, 节点可点选并用现有 Compose 属性抽屉编辑 (混合文档模型); 交互预览中由宿主真实运行 (宿主装载插件渲染器) |
| D59 宿主编辑器预览 | AutoJs6 编辑器对 `"ui"` 脚本增加 "预览" 动作: 编辑器下半区 / 底部弹层用宿主自己的预览服务 (进程内调用) 按当前缓冲区绘制布局, 停顿 400 ms 刷新, 点元素跳到源码行; 不需要插件 |
| D60 版本切分 | 1.8.0 = 复核线 + 差异明示 + XML 文件工程; 1.9.0 = 契约 v3 与宿主侧缩小项 + TSX ui 布局 + compose 联动; 2.0.0 = 宿主编辑器内预览 (宿主发布配合) |
| D61 宿主依赖 | 预览契约只追加 v3 (对话框入画布, 生命周期事件, 忙等阈值, 新能力标志), Binder 的 `getContractVersion()` / `getContractHash()` 仍答 v1, v3 经 `getPreviewInfo()` 宣告; 插件在旧宿主上自动退回现有行为并在差异列表说明; 宿主改动留在本地 master (D7), 插件的发布不等待宿主 |
| D62 导出格式 (2026-10-16, Q23) | 文件夹工程只导出为 zip 单文件; 导入仍接受 zip 与目录; 宿主接收入口用同一种 zip |
| D63 project.json (2026-10-16) | 升格为文件夹时生成满足宿主完整校验的 `project.json` (`name` / `main` / `packageName` / `versionName` / `versionCode`), 设计器在重命名与导入 / 导出时维护, 容忍手改的其它字段 |
| D64 交给 AutoJs6 (2026-10-16) | 宿主新增入口接收工程 zip: 运行时解到缓存目录并以其为工作目录运行, 结束后清理; 导入时解到脚本目录下同名文件夹, 重名时弹窗询问覆盖或另存; 旧宿主上插件对文件夹工程隐藏这两个去向. 修订 D60 的 "1.8.0 不改宿主" |
| D66 新建 XML 文件的入口 (2026-10-16) | 三种都做: (1) 片段菜单 "存为 XML 文件": 把直接写在 `ui.layout(...)` / `ui.inflate(...)` 中的布局字面量移到文件 (脚本改为 `ui.layoutFile("...")` / `ui.inflate(files.read("..."), ...)`), 含 `{...}` 表达式或经变量传递的字面量不提供; (2) 新建工程对话框的 ui 类型可选 "脚本内布局" 或文件夹模板; (3) 片段菜单 "新建 XML 文件": 只含一个根容器的新文件, 脚本末尾追加 `let <名称>View = ui.inflate(files.read("..."));`. (1) 与 (3) 是可撤销的编辑, 保存时才写文件并把单文件私有工程升格; 只能写回同目录的外部文档 (content 方案在 SAF 树权限之前不行) 不提供 |
| D67 新文件的 XML 写法 (2026-10-16) | 提取时原样移动; 文件夹模板与 "新建 XML 文件" 可选 AutoJs6 XML 或 Android 原生 XML (原生模板写 `ui.useAndroidLayout();`, 文件放 `res/layout/`, 根元素声明 `xmlns:android`); 脚本已写 `ui.useAndroidLayout(true / false)` 时新文件随之, 不再询问 |
| D65 宿主先行 (2026-10-16) | 插件侧 P11.3 之前先在宿主完成: 预览服务的 `res/` 支持 (契约 v3 的资源目录一项提前) 与动态布局器的 Android XML 属性集扩充 (SDK `attrs.xml` 元数据 + 反射 setter 兜底 + 映射表 + 不支持清单与诊断), 宿主另写专门路线图; 插件原生属性目录由宿主元数据表生成 (修订 D56) |
| D68 TSX 的事件桩 (2026-10-16, P11.7 前) | 实测 (AVD_API_33, TypeScript Engine 0.7.0, 宿主 debug): 3.5 节原定的 `ui.<id>.on('event', (event, view) => ...)` 在 TSX 中编译失败 (`strict` 下 TS2339 `ui` 上没有该 id, TS7006 参数隐式 any), `(ui as any).<id>.on(...)` 与 `ui.findView('<id>')?.click(...)` 可编译运行. 维护者选择: click 写成 JSX 内联 `onClick={() => { ... }}` (与 TypeScript Engine 模板一致, 宿主的 TSX 运行时只特殊支持 onClick), 其它事件追加 `(ui as any).<id>.on('<event>', (_event: any, _view: any) => { ... })` (参数带类型与下划线, 满足 `noImplicitAny` / `noUnusedParameters`) |
| D69 TSX 的交互预览 (2026-10-16, P11.7 前) | 宿主的预览运行只按 JavaScript 执行脚本文本. 维护者选择由宿主支持: 预览契约再追加 v5 (`runScript` 可交 `.tsx`, 宿主先经 TypeScript Engine 编译再在会话中运行, 编译错误按行回报), 作为宿主阶段 P11.H3; 插件在 v5 宿主上为 TSX UI 工程打开交互预览, 旧宿主上开关处说明需在 AutoJs6 中运行. 1.9.0 的发布顺延到 P11.H3 之后 |

---

## 2. 现状事实 (2026-10-15, 1.7.0 发布后)

- 性能: `docs/dev/p8-designer-performance.md` 有 1.2.0 基线 (四设备), 1.3.0 的帧时间复核与 1.7.0 第二轮 (Redmi / Pad / 两台模拟器); Redmi 两轮 p50 逐项相差约 15% 以内, 帧行 p90 相差最多 40%; 建议复核线表已算出但未生效. 两个测试类 `DesignerPerformanceDeviceTest` / `DesignerFramePerformanceDeviceTest` 默认跳过, `-e designerPerformance true` 开启, 结果在 logcat `DesignerPerformance` 标签的 `DESIGNER_PERF {json}` 行.
- 差异: P10 路线图 3.5 节列有九行 (activity 全局, 生命周期事件, 对话框, 弹出层 / 输入法, 窗口与屏幕, 绑定与数据, 副作用, API 24 - 29, 触摸与遮盖, 死循环与主线程); 文档站 "在预览中运行 UI 脚本" 小节已对使用者明示; 设计器内没有按脚本的提示. 宿主 `UiContentHost` 对窗口级函数空操作并经控制台各提示一次; `dialogs.*` 在没有 Activity 时走悬浮窗类型窗口; 预览运行的脚本体与处理函数在宿主主线程执行, 忙等会卡住宿主.
- XML 文件: `UiScriptReader` 只识别脚本内的 E4X 字面量; `ui.layoutFile(path)` 与 `ui.inflate` 的文件参数, `useAndroidLayout(true)` 的原生命名空间 XML 不在画布中 (非目标). 私有工程是 `files/designer/projects/<name>.js` 单文件, 设计器 FileProvider 只暴露该目录, 运行 / 导入经 `RunIntentActivity` + 脚本文本或 `ImportIntentActivity` + 单文件 content Uri.
- TSX: Compose TSX 工程 (P9.2) 经 `typescript-parser-api` v1 读树, `TsxReader` 只映射 `compose.X` 元素; `ui.layout(<...>)` 的 JSX 子树被当作不透明段保留. 树带行列 (错误定位用), 规范化写回不需要源码范围.
- compose 联动: 1.1.0 起 XML `<compose>` 容器与 `compose.attach(view, render)` 可在 ui 页面内嵌入 Compose 会话; UI 工程的画布目前把 `<compose>` 显示为宿主的占位视图 (布局预览) 或由宿主真实运行 (交互预览, 宿主装载插件渲染器, 尚无设备证据). `ScriptReader` 已能把 `compose.state` 声明与一个渲染函数读成 `DesignDocument`, 但只认 `compose.mount / floaty / dialog` 的挂载.
- 宿主编辑器: 宿主编辑器 "更多" 菜单有 "在 3-Block Compose 设计器中打开" (宿主 `db7e6a1734`); 宿主自身的预览服务 (`UiPreviewService`) 已能按 XML 文本绘制布局并回传边界, 目前只经 Binder 供插件使用.

---

## 3. 目标架构

### 3.1 复核线与脚本

- 线写在 `AGENTS.md` 第 14 节 (表格, 与 P5 的表并列) 与 `docs/dev/designer-performance-lines.json` (脚本读取; 每项 `key`, `line_ms`, `source` 为采样日期与提交). 初值即 `p8-designer-performance.md` "Second round (1.7.0)" 的建议表.
- `.python/verify_designer_performance.py --serial <serial> --apk <debug apk> --test-apk <androidTest apk> [--lines <json>] [--record <out json>]`: 安装两个包, 清 logcat 并把缓冲区设为 16 MB, 依次跑两个测试类 (各一次, 第一项若被挤出缓冲区则单独复跑), 解析 `DESIGNER_PERF` 行, 按 `line_ms` 逐项比较 p90, 输出每项 `PASS` / `OVER` 与数值, 任一 `OVER` 即退出码 1; `--record` 把本轮 p50 / p90 追加到 `docs/dev/p8-designer-performance.md` 的同格式表 (人工审阅后提交). 设备不是 Redmi 时只记录不判定 (`--serial` 对应的型号须为 `22120RN86C`, 否则打印 `RECORD_ONLY`).
- 超线处理沿用第 14 节规则: 复测或记录可审查的原因, 由维护者确认是否接受并重定线; 重定线时同一提交内更新 JSON, AGENTS 表与性能文档.

### 3.2 预览差异的明示

- `designer/model/PreviewDifferences`: 在 `UiScriptReader` 的 AST 遍历中收集命中项 (成员访问与调用, 带行列): `activity.*`; `ui.finish`, `ui.keepScreenOn`, `ui.statusBarColor`, `ui.statusBarIconLight*`, `ui.backgroundColor`, `ui.navigationBarColor`, `ui.navigationBarIconLight*`, `ui.emitter.on('resume' | 'pause' | 'back_pressed' | 'activity_result')`; `dialogs.*`; 可能忙等的形态 (`while (true)` / `for (;;)` 顶层或处理函数内无 `break` 的循环, `sleep(...)` 在 ui 线程); `threads.*` / `events.*` 只提示 "按真实运行执行". 每条映射到 3.5 节的一行与文档锚点.
- `UiValidator` 之外的独立类别 `Severity.INFO` ("预览差异"), 进入 `UiDesignerSession.diagnostics`, 问题列表按行号排序显示在错误 / 警告之后, 菜单的 "问题 N" 只数错误; 交互预览支持与否不影响明示 (布局预览也列出, 说明 "交互预览时 ..."). 随宿主能力 (v3 宣告) 自动去掉已缩小的项: 宿主支持对话框入画布时不再列 `dialogs.*`, 以此类推.
- 实现说明 (2026-10-16, P11.2 落地时与上两条的偏离): (1) 扫描是独立的 `designer/parse/PreviewDifferenceScanner` (Rhino 只允许在 parse / sandbox 包), 不在 `UiScriptReader` 的遍历中: 会话每次发布时从写出的脚本重扫 (与 `declaredNames` 共用同一次写出), 行号因此随插入 / 删除的 XML 行移动; 桌面 JVM 上 12 个示例平均 0.18 ms, 最大的 4143 字符 0.30 ms. (2) 差异不并入 `diagnostics` 也不加 `Severity.INFO`: 校验诊断按元素给出英文消息, 差异按脚本行给出本地化说明与文档链接, 单列为 `UiDesignerSession.differences` / `visibleDifferences` (按 `hostCapabilities` 过滤), 问题列表把它们放在校验问题与预览问题之后, 菜单错误数本就只数 `ERROR`, 效果与原计划相同. (3) 不列 `sleep(...)`: 宿主在 ui 线程调用 `sleep` 时真实运行同样抛错 (`Global.sleep` 的 `isUiThread` 检查), 不是预览差异. (4) 八类 (`PreviewDifferenceKind`): `ACTIVITY`, `LIFECYCLE` (`ui.emitter` 监听 resume / pause / back_pressed), `ACTIVITY_EVENTS` (`ui.emitter` 的其它用法), `WINDOW_COLOR` (statusBarColor / navigationBarColor / backgroundColor), `WINDOW_NO_OP` (finish / keepScreenOn / 四个图标函数), `DIALOGS` (`dialogs.*` 与全局 alert / confirm / prompt / rawInput), `BUSY_WAIT`, `THREADS` (`threads.*` / `events.*`); 脚本自己声明的同名变量 / 函数 / 参数不算 (例如 `let events = []`), 每类每行一条. (5) "跳到文档" 以 VIEW Intent 在浏览器打开在线文档站 `composeDesigner` 页的 "预览差异 (Preview Differences)" 一节 (`PreviewDifferences.DOCS_URL`, Docsify id 由标题决定): 宿主的文档页 (`DocumentationActivity`, 导出, extra `url`) 按用户设置走离线插件或在线站点, 离线锚点 (`composedesigner_preview_differences`) 与 Docsify id 不同, 在线源按 `<页面>.html#锚点` 路径加载 (`DocsUtils.getOnlineUrl`), 不是在线站点的 `#/页面?id=` 路由, 同一个链接无法在两种来源中都定位到小节.

### 3.3 预览契约 v3 (只追加, 宿主)

- `getPreviewInfo()` 追加: `dialogsInCanvas` (bool), `lifecycleEvents` (bool), `busyWaitLimitSupported` (bool), `windowFunctionsVisible` (bool); `contractVersion` 3 与新 `contractHash`; Binder 的两个 getter 仍答 v1 (`BASE_VERSION` / `HASH_V1`), `createSession` 接受 1 / 2 / 3.
- `runScript` 请求追加 `busyWaitLimitMs` (long, 1000 - 60000, 缺省 5000, 0 为关闭): 宿主对该次运行的引擎 `Context` 设置指令计数观察器, 主线程连续执行脚本超过阈值即以 `error` 结束 (`onScriptError` 的 `errorMessage` 为 "预览中主线程忙等超过 N 秒", 行号为当前行), 不影响定时器驱动的脚本.
- 新事务 `sendLifecycle(String sessionId, Bundle event)` (`event` 为 `pause` / `resume` / `back_pressed`): 宿主向运行中的脚本的 `ui.emitter` 发同名事件 (前提: 预览运行目前没有 `ui.emitter`, 它由 `ScriptExecuteActivity` 提供, 预览中为 null, 见 P11.2 证据; v3 宿主须先让预览承载提供一个事件发射器并发 `create`); `back_pressed` 未被处理函数 `consume` 时宿主以 `exit` 结束运行 (对应真实运行的 finish). 新回调 `onLifecycleHandled(sessionId, Bundle)` 回报是否被消费.
- 对话框入画布: 预览会话实现宿主 `dialogs` 模块的 "对话框宿主" 接口: 运行中的脚本调用 `dialogs.*` 与 `ui` 的 Material 对话框时, 宿主把对话框作为子视图 (带遮罩) 挂在嵌入根上, 不再创建悬浮窗类型窗口; 关闭 / 返回键 / 释放会话时清理. 只在 `dialogsInCanvas` 为 true 的宿主上发生, 插件不传参.
- 窗口级函数的可见效果: 预览会话的 `UiContentHost` 把 `statusBarColor` / `navigationBarColor` 画成根容器顶部 / 底部的色条 (高度按 24 dp / 48 dp 的仿制 inset), `backgroundColor` 设到根, `keepScreenOn` / `ui.finish` 照旧空操作并提示; 布局预览与交互预览都生效.

### 3.4 XML 文件工程

- 文档模型: `UiDocument` 之外新增 `LayoutFile` (路径, 文本, `XmlCst` 片段, `useAndroidLayout` 标志); `UiScriptReader` 识别 `ui.layoutFile("...")`, `ui.inflate(files.read("..."))` 等形态中的字面量路径并记录为片段引用; 大纲的片段切换器列出脚本内字面量与文件两类片段 (文件片段标文件名). 原生命名空间 XML (`useAndroidLayout(true)`) 的属性目录由 `generate_ui_catalog.py` 从 Android SDK 的 `attrs.xml` 生成第二份目录 (只含常用 View / ViewGroup / 文本 / 图片类属性, 其余按 "未知属性" 提示), 标签为完整类名或 `android.widget.*` 简写.
- 外部文档: 脚本 Uri 的同目录 XML 文件经 `DocumentFile` / 文件系统读写 (content 方案用 SAF 的树权限: 第一次需要用户选目录, 之后持久授权; file 方案直接读写); 写回与草稿同脚本的规则 (D39).
- 私有工程: 第一次引用或新建 XML 文件时 `ProjectStore` 把 `name.js` 升格为 `name/main.js` (首页列表显示为同名工程, 徽标加 "文件夹"), 资源文件放在 `name/` 下; 草稿键与来源记录随之迁移; 导出为 zip (`CreateDocument` + `application/zip`) 或 SAF 目录, 导入支持 zip 与目录; FileProvider 路径扩展到工程文件夹; 运行与导入 AutoJs6 交出 `main.js` 的 content Uri 并授予整个文件夹 (宿主的 `RunIntentActivity` 已支持以 content Uri 运行项目, 需核实), 不再只传脚本文本.
- 预览: 文件片段与脚本内片段走同一条 `setLayout` 路径 (XML 文本), `useAndroidLayout` 片段由宿主以原生 `LayoutInflater` 解析 (契约 v3 的 `layout` 请求追加 `androidLayout` 标志).

### 3.5 TSX ui 布局

- `TsxReader` 识别 `"ui"` 指令与 `ui.layout(<...>)` / `ui.inflate(<...>)` 的 JSX 实参, 把 JSX 树转为 `XmlNode` (元素 / 属性 / 文本; JSX 表达式容器作为 `XmlNode.Expr` 只读), 文档类型为 UI 工程 (方言 TSX); 画布, 大纲, 抽屉与 JS 的 UI 工程相同.
- 写回: 文档记录每个 JSX 片段在源码中的范围 (解析服务的树带 `pos` / `end`, 契约 v1 已有行列, 核实是否有偏移; 没有则升解析契约 v2 追加偏移), 保存时把被编辑的片段整体按 `TsxWriter` 规范重写, 其它源码原样 (D57); 未编辑的片段字节不变.
- 事件桩: 对 TSX 工程追加 `ui.<id>.on('event', (event, view) => { ... })` 桩, 语法用 TypeScript 的箭头函数; 处理函数语法检查经解析服务 (已有).
- 运行: TSX 的 UI 工程交出工程文件的 content Uri, 宿主经 TypeScript Engine 编译 (与 Compose TSX 相同).

实现说明 (P11.7): ui 分支是独立的 `TsxUiReader` (`DocumentReader` 对 TSX 源码先判断是否 ui 脚本), 不放进 `TsxReader`; 解析服务的树已带 UTF-16 的 `start` / `end`, 解析契约不需要 v2. 事件桩按 D68 改写 (click 为 JSX 内联 `onClick`, 其它事件为 `(ui as any).<id>.on(...)`), 交互预览按 D69 等宿主契约 v5 (P11.H3).

### 3.6 compose 联动 (混合文档模型)

- `UiDocument` 增加 `composeAttachments`: 由 `UiScriptReader` 找到 `compose.attach(ui.<id>, <渲染函数或节点>)` 调用, 用 `ScriptReader` 的渲染函数解析把实参读成 `DesignDocument` (复用 state 声明解析, 作用域为整个脚本), 以容器 id 关联到 `<compose id="...">` 元素; 读不出的实参保持不透明并在问题列表提示.
- 画布: 宿主回传 `<compose>` 容器边界后, `XmlCanvas` 在该矩形上以 `DesignCanvas` 的逻辑屏幕画出对应 `DesignDocument` 的 `RenderFrame` (真渲染器), 点选时先命中 compose 节点再命中 XML 元素; 选中 compose 节点时下半区切到 Compose 的属性抽屉与大纲子树 (`DesignerSession` 的子会话), 编辑经 `EditCommand` 进入同一撤销栈 (以复合命令包装).
- 写回: `ScriptWriter` 把被编辑的渲染函数按 Compose 工程的规范格式重写到 `compose.attach` 调用的实参范围, 其它源码原样 (与 D35 一致); 脚本内的 `compose.state` 声明可在状态面板编辑预览值.
- 交互预览: 宿主真实运行时 `<compose>` 容器由宿主装载插件渲染器画出, 本阶段补设备证据 (Compose 渲染器在预览会话的嵌入根中运行).

### 3.7 宿主编辑器内预览面板 (宿主)

- 编辑器工具栏对 `"ui"` 脚本显示 "预览" 开关: 开启后编辑器下半区 (横屏为右半区) 放一个 `UiPreviewSession` 风格的进程内预览视图 (复用 `UiPreviewSession` 的布局应用与边界跟踪, 不经 Binder), 按当前缓冲区文本解析出第一个 `ui.layout` 的 XML 字面量与顶层字面量声明 (复用宿主已有的 Rhino 解析), 停顿 400 ms 刷新; 点元素把光标跳到对应行列; 布局失败在面板内说明.
- 面板只做布局预览 (不运行脚本); 面板右上角 "在 3-Block Compose 设计器中打开" 复用现有入口. 不需要插件改动; 插件侧只在文档与 README 的 "与 AutoJs6 的衔接" 说明.

---

## 4. 范围与非目标

范围: 第 3 节全部; 两个仓库的测试, 文档站 `api/composeDesigner.md` 与 `api/ui.md` 的对应小节, 插件 README / changelog 10 语言, AGENTS 修订, 每版的本地 gate 与发布.

非目标 (另立条目再议): 冷启动 / PSS / 滚动的复核线 (继续记录); API 30 - 32 的交互预览与 API 33 - 34 的输入法 (平台限制); `.tsx` 的无损写回 (D57 选规范化); compose 联动在布局预览中由宿主渲染 (D58 选插件叠画); 宿主编辑器内的交互预览 (D59 只做布局面板); 低于 API 24.

---

## 5. 阶段总览

| 阶段 | 内容 | 落点 | 目标版本 | 依赖 |
| --- | --- | --- | --- | --- |
| P11.1 | 设计器性能复核线: 线文件, AGENTS 第 14 节表, `verify_designer_performance.py`, Redmi 首次比对 | 插件 | 1.8.0 | 无 |
| P11.2 | 预览差异明示: API 扫描, 问题列表 "预览差异" 类, 文档锚点 | 插件 | 1.8.0 | 无 |
| P11.H1 | 预览服务的 `res/` 支持: 契约 v3 的 "随布局请求交出资源目录", `androidLayout` 标志与交互预览的工程目录提前 (D65; 宿主路线图 R1 - R3; v3 经新信息键宣告, 旧键仍答 v2, 原计划的 P11.5 各项顺延为 v4) | 宿主 | 宿主本地 master | P11.2 |
| P11.H2 | 动态布局器的 Android XML 属性集扩充: SDK 元数据表, 反射 setter 兜底, 映射表, 布局参数, 不支持属性跳过并警告一次, 资源限定符目录与 vector drawable, 文档 (D65; 宿主专门路线图 `docs/dev/android-xml-attributes-roadmap.md` 的 A1 - A9 (A9 为覆盖矩阵发现的 ConstraintLayout 辅助类), 维护者同日拍板其 Q1 - Q3) | 宿主 | 宿主本地 master | P11.2 |
| P11.3 | XML 文件工程: 文件片段模型与读写, 原生命名空间目录 (由宿主元数据生成), 私有工程升格为文件夹 (含 project.json), zip 导出 / 导入, 运行 / 导入 AutoJs6 的宿主 zip 入口 (D62 - D64) | 插件 + 宿主 (zip 入口) | 1.8.0 | P11.2, P11.H1, P11.H2 |
| P11.4 | 1.8.0 文档, changelog, gate 与发布 | 文档 + 发布 | 1.8.0 | P11.1 - P11.3 |
| P11.5 | 预览契约 v4 (原计划 v3, 见 P11.H1) 与宿主侧缩小项: 对话框入画布, 生命周期事件与返回键, 窗口级函数可见效果, 忙等保护 (`androidLayout` 标志已在 v3 交付) | 宿主 | 宿主本地 master | P11.4 |
| P11.6 | 插件对接 v4: 返回键按钮, pause / resume, 忙等阈值设置项, 能力降级与差异列表联动 | 插件 | 1.9.0 | P11.5 |
| P11.7 | TSX ui 布局: `TsxReader` 的 ui 分支, 片段范围, 规范化写回, 事件桩 (D68) | 插件 | 1.9.0 | P11.3 |
| P11.H3 | 预览契约 v5: `runScript` 运行 `.tsx` (宿主经 TypeScript Engine 编译, 编译错误回报), 插件为 TSX UI 工程打开交互预览 (D69) | 宿主 + 插件 | 宿主本地 master / 1.9.0 | P11.7 |
| P11.8 | compose 联动: `compose.attach` 解析, 混合文档与子会话, 叠画与点选, 写回, 交互预览设备证据 | 插件 | 1.9.0 | P11.3 |
| P11.9 | 1.9.0 文档, changelog, gate 与发布 | 文档 + 发布 | 1.9.0 | P11.6 - P11.8, P11.H3 |
| P11.10 | 宿主编辑器内布局预览面板 | 宿主 | 宿主本地 master | P11.5 |
| P11.11 | 2.0.0 文档 (衔接小节), gate 与发布; 宿主发布配合时补真机矩阵 | 文档 + 发布 | 2.0.0 | P11.10 |

版本切分理由 (D60, 2026-10-16 由 D64 / D65 修订: 1.8.0 的 XML 文件工程依赖宿主的 zip 入口, 预览 `res/` 支持与属性集扩充, 宿主改动仍留本地 master, 插件在旧宿主上降级): 1.8.0 的三项原本都不改宿主, 可独立发布; 1.9.0 集中依赖契约 v3 与解析器扩展的项; 2.0.0 只剩宿主侧的编辑器面板, 插件侧为文档与衔接, 其版本号留作与宿主正式发布对齐的标记.

---

## 6. 条目

### P11.1 设计器性能复核线 (插件, 1.8.0)

- [x] (插件) `docs/dev/designer-performance-lines.json`: 16 项的 `key` / `line_ms` / `source` (1.7.0 第二轮 Redmi p90 加 50% 取整: 画廊读 65, 校验 25, 预览帧 35, 写 25; 大文档读 15, 校验 10, 预览帧 15, 写 10; 打开最大示例 1000, 打开 251 节点 1200; 撤销可见 570, 重做可见 540; 撤销首帧 560, 撤销主线程 CPU 630, 重做首帧 555, 重做主线程 CPU 640); `AGENTS.md` 第 14 节加同一表与 "只以 Redmi 为准, 其它设备只记录" 的说明.
  证据 (2026-10-15): `docs/dev/designer-performance-lines.json` (`device_model` 22120RN86C, 规则与来源, 16 项各带 `p90_ms` 与 `line_ms`); 单测 `test_the_committed_lines_file_is_consistent` 按 D54 规则重新算每项并与文件比对; `AGENTS.md` 第 14 节加复核线段落与六行汇总表, 验证顺序加 `python -m unittest discover -s .python/tests` 与脚本命令.
- [x] (插件) `.python/verify_designer_performance.py` (3.1 节): 安装, 跑两个测试类, 解析 `DESIGNER_PERF`, 逐项比对, `--record` 追加表; `--serial` 非 Redmi 时 `RECORD_ONLY`; JVM 单测 `test_verify_designer_performance.py` 覆盖解析与比对 (假数据).
  证据 (2026-10-15): 脚本复用 `verify_standalone_launch.Adb`, 16 MB 日志缓冲, 按类运行并按 kind 检查 `DESIGNER_PERF` 行, 缺失的 kind 以 `类#方法` 单独复跑, 失败 / 跳过 (`-e designerPerformance` 未生效) / 未结束都以 `SystemExit` 报出; 判定只在 `ro.product.model` 等于线文件的 `device_model` 时进行, 否则 `DESIGNER_PERF_RECORD_ONLY`; `--record` 以 "Round <日期> (<型号>, API, build, judged / recorded only)" 标题追加 p50 / p90 表到性能文档; `--restore` 以 `install -r -d` 装回发行包并卸载测试包 (真机不卸载插件, 保住设计器工程). 单测 `.python/tests/test_verify_designer_performance.py` 5 项 (解析分组与数值, 线文件一致性, 判定 PASS / OVER / MISSING, 非判定设备只记录, 记录表), `python -m unittest discover -s .python/tests` 通过; CI `build.yml` 的单元测试作业加同一命令.
- [x] (测试) 在 Redmi 上执行一次完整比对 (1.8.0 的 debug 包), 结果写入性能文档 "第三轮" 表并记录脚本输出; 超线项按第 14 节处理.
  证据 (2026-10-15): 脚本在 Redmi 22120RN86C (API 33, debug build 174, 竖屏, 空闲) 首次比对 16 项全部 PASS (`DESIGNER_PERF_OK`; 例如画廊读 p90 38.1 对线 65, 打开 251 节点文档 785.9 对 1200, 撤销到首帧 393.9 对 560, 重做主线程 CPU 310.0 对 640), `--record` 追加的 "Round 2026-10-15 (22120RN86C, API 33, build 174, judged)" 表已入库, `--restore` 装回 1.7.0 发行包并移除测试包; 1.8.0 的版本号尚未提升 (P11.4 才升), 本次用的是 1.7.0 源码树的 debug 包 (build 174 的构建, 与当前应用源码相同); 1.8.0 gate 时再跑一次.

### P11.2 预览差异明示 (插件, 1.8.0)

- [x] (插件) `PreviewDifferences` 扫描 (3.2 节): 命中项, 行列, 映射到 3.5 节行与文档锚点; `Severity.INFO` 与问题列表的 "预览差异" 分组, 菜单计数不含信息级; 布局预览与交互预览的措辞区分; 按宿主宣告的能力过滤.
  证据 (2026-10-16, 提交 `39929e1`): `designer/model/PreviewDifferences.kt` (`PreviewCapability` 四项, `PreviewDifferenceKind` 八类各带 `reducedBy`, `PreviewDifference(kind, line, column, api)`, `PreviewDifferences.visible` 与 `DOCS_URL`) 与 `designer/parse/PreviewDifferenceScanner.kt` (Rhino 解析不求值; 规则与对原计划的偏离见 3.2 节 "实现说明"); `UiDesignerSession.differences` 在每次发布时从写出的脚本重扫, `hostCapabilities` (默认空, P11.6 接入) 经 `visibleDifferences` 过滤; `XmlProblemsPanel` 在校验问题与预览问题之后列出 "i 第 N 行  写法" 与一句说明, 组首提示按 `session.interactive && interactiveSupported` 分为布局预览 / 运行中两种措辞, 点按以 VIEW + BROWSABLE 打开文档站, 无可用应用时 toast; 菜单 "校验结果 (N 个错误)" 仍只数 `ERROR`. 12 个宿主示例的命中: android-layout 6 条 (三条 `WINDOW_COLOR`, 三条 `WINDOW_NO_OP`), console 1 条 (`threads.start`; 子线程里的 `while (true)` 与 `console.rawInput` 不算), template 5 条 (两条 `ui.emitter` 的选项菜单事件, `alert`, `activity.setSupportActionBar`, `ui.finish`), todo 5 条 (`ui.statusBarColor`, `confirm`, `ui.emitter.on('pause')`, 两条 `dialogs.rawInput`), 其余 8 个为 0. 扫描耗时 (桌面 JVM, 预热后) 12 个示例平均 0.18 ms, 最大的 4143 字符 0.30 ms. 设备探针 (emulator-5580, AVD_API_33, 宿主 debug 5439, 临时测试未入库) 证实说明所依据的事实: 交互预览中 `typeof activity` 为 "undefined", `activity.getIntent()` 抛 ReferenceError; `String(ui.emitter)` 为 "null", `ui.emitter.on('pause', ...)` 抛 TypeError 并以 `error` 结束; `threads.start` 的线程照常运行; `ui.statusBarColor` / `ui.finish` 的 "已忽略" 警告没有出现在回传的控制台中 (宿主 `UiPreviewSession.onWindowFunctionIgnored` 写到会话自身运行时的控制台), 因此说明里不写 "AutoJs6 会提示一次", P10 路线图 3.5 节两行与文档站 1.7.0 段落已更正.
- [x] (插件) 文案 `designer_ui_diff_*` 10 语言 x 11 目录 (每类差异一句); 文档站 "在预览中运行 UI 脚本" 小节加 "问题列表中的预览差异" 一段.
  证据 (2026-10-16): 12 键 x 11 目录 (`activity`, `activity_events`, `busy_wait`, `dialogs`, `docs_unavailable`, `header_layout`, `header_running`, `lifecycle`, `line`, `threads`, `window_color`, `window_no_op`; 八类各一句, 两种组首, 行号与打不开文档的提示), 只用半角标点, `StringResourceParityTest` / `ApplicationTextPunctuationTest` 通过. 文档站 (`AutoJs6-Documentation` 提交 `47af615`, 本地, 随 1.8.0 发布推送与离线同步): "在预览中运行 UI 脚本" 之后新增 "预览差异 (Preview Differences)" 一节 (Docsify id `预览差异-preview-differences`, 离线锚点 `composedesigner_preview_differences`; 八行写法 / 行为表, 同名声明, 每类每行一条, 版本限制不在列表中), 编辑页表 "问题" 行指向它, 页首版本范围改为 1.3.0 - 1.8.0, 1.7.0 段落的 `ui.emitter` 与窗口级函数提示按探针更正; `normalize-markdown.py --check` 与 `auto-generate.py --check` 通过. 在本机 http 服务的文档站中 `#/composeDesigner?id=` 加该 id 的页面里存在同 id 的 h3 (浏览器面板未显示, 滚动本身未能目测).
- [x] (测试) JVM `PreviewDifferencesTest` (12 个 ui 示例的命中清单快照, 行列, 过滤); `UiDesignerDeviceTest` 增加一项: 含 `dialogs.alert` 与 `ui.statusBarColor` 的脚本在问题列表显示两条信息级差异且 "问题 N" 计数为 0.
  证据 (2026-10-16): `PreviewDifferencesTest` 8 项 (示例快照, 位置与每类每行一条, 自声明的同名全局, `ui.emitter` 的事件分类, 无限循环规则 (能离开的循环, switch / 内层循环的 break, 带标签的 break, return / throw, `threads.start` 内与按名交给它的函数), 解析失败为空, 能力过滤, 会话插入元素后行号后移, 设置能力后隐藏, 撤销后恢复, 差异不进 `diagnostics`); JVM 全部 239 项通过 (1 项既有跳过), `assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` (0 错误). `UiDesignerDeviceTest.previewDifferencesAreListedAsInformationAndNotCounted` (两条差异的种类与行号, 菜单 "校验结果 (0 个错误)", 组首为布局预览措辞, 两条的写法与说明, 点按经 Instrumentation 监视器截住指向 `docs.autojs6.com` 的 VIEW + BROWSABLE 意图, 文档未变脏): emulator-5580 (AVD_API_33, 宿主 debug 5439) 上 `UiDesignerDeviceTest` 7 项, `UiPreviewDeviceTest` 1 项, `UiInteractiveDeviceTest` 3 项全部通过; emulator-5570 (API 24, `wm size 1080x1920` / `wm density 480`, 即 CI 的 Nexus 5 尺寸) 上 `UiDesignerDeviceTest` 7 项与 `UiPreviewDeviceTest` 1 项通过 (两次首轮遇到模拟器自身的 GL 中止 `CanvasContext.cpp:505`, SystemUI / 启动器同时崩溃, 等界面稳定后单独复跑通过). 真机未跑 (本条不涉及宿主与设备差异).

### P11.H1 预览服务的 `res/` 支持 (宿主, 本地 master; 宿主路线图 R1 - R3)

- [x] (宿主) 预览契约 v3 (只追加, 无新事务): `setLayout` 的 `resourcesFd` / `resourcesSha256` / `layoutPath` / `androidLayout`, `runScript` 的 `projectFd` / `projectSha256` / `scriptPath`, 诊断 `ATTRIBUTE_IGNORED`; 三层身份 (Binder getter 答 v1, 信息旧键答 v2, 新键 `supportedContractVersions` / `contractHashes` 列出 1 / 2 / 3); 宿主实现归档的校验与解压, 布局按工程资源解析, 交互预览以工程副本为工作目录.
  证据 (2026-10-16): 宿主提交 `2da2d4119` (契约 v3, `HASH` `196e52cd...`, 信息旧键改答冻结的 v2, 协议文档 v3 小节), `9df0a154b` (实现), `035053bce` (测试: JVM 预览包 26 项; 设备 `UiPreviewServiceTest` 14 项在 AVD_API_33 / API 24 / AVD_API_35_16K 通过, 官方示例 "专注时光" 无资源时失败, 带 `res/` zip 时成功). 契约 AAR `ui-preview-api-release.aar` 20618 B, SHA-256 `4746f9f3...8b94`, 重建字节一致; 插件在 P11.3 换用并更新锁. 1.7.0 插件不受影响 (信息旧键仍答 v2 身份). 原计划在 P11.5 的契约升级相应顺延为 v4.

### P11.H2 Android XML 属性集扩充 (宿主, 本地 master; 宿主路线图 A1 - A9)

- [x] (宿主) 属性元数据表, setter 与布局参数兜底, 不支持 / 未知属性跳过并警告一次 (D7) 与预览诊断 `ATTRIBUTE_IGNORED`, 覆盖矩阵, 资源限定符目录与 vector drawable (D8), ConstraintLayout 辅助类, 文档与更新日志.
  证据 (2026-10-16, 宿主本地提交, 未推送): `e44094af6` / `e4d1f1eb8` (A1 属性表 `app/src/main/assets/ui/android-attrs.json`: SDK API 24 - 37 与 8 个 AndroidX / Material AAR, 只收公开 View 子类), `f7e03ef7d` (A2 公开方法兜底, 书写顺序), `6f23cf745` (A3 布局参数), `eca9b0b3f` (A4 原生 XML 跳过并警告一次, 预览诊断带 `o` 字段 `unsupported` / `unknown`, AutoJs6 XML 行为不变), `d59ccab5f` (A5 设备覆盖矩阵与快照 `app/src/androidTest/assets/ui/android-attrs-coverage.json`, 及其暴露的缺口: Material 组件主题补齐, AppCompat 别名, RelativeLayout 三条规则, scrollbars 关闭语义, adjustViewBounds 顺序, 样式 / 主题带来的属性不再误报), `2b679ec0d` (A7 限定符目录, 配置取自脚本 Activity 或预览会话 (含会话尺寸)), `ee1a1456a` (A8 vector), `f9a0c859a` (A9 ConstraintLayout 容器自身属性与 Barrier / Group / Flow / Guideline), `c42bb59f1` (A6 文档页生成器), `dbbb62a44` (zh-Hans 更新日志). 覆盖 (API 37): View 属性 1314 个中可设置 985 个 (75 %), 子视图布局参数 135 个中不支持 2 个. 设备测试在 API 24 / 30 / 33 / 35 / 37 模拟器通过. 文档站本地提交 `22d7b1e` (新页面 `uiAndroidAttributes`, `ui.md` "工程资源" 一节, `uiAttributes.md` 修订), 与 `47af615` 一起随 1.8.0 发布推送. 对 P11.3 的输入: 原生命名空间属性目录可读文档页 `api/uiAndroidAttributes.md` 的生成块 (每类一张表: 属性, 格式与枚举 / 标志名, 起始 API, 设置方式), 或直接读宿主属性表与覆盖快照; 预览诊断的 `o` 字段供问题列表区分 "无法设置" 与 "未知属性".

### P11.3 XML 文件工程 (插件, 1.8.0)

- [x] (插件) 文档模型与读取: `LayoutFile` 片段, `ui.layoutFile` / `ui.inflate(files.read(...))` 的字面量路径识别, 片段切换器的文件片段; `useAndroidLayout(true)` 标志与第二份属性目录 (`generate_ui_catalog.py --android-attrs <SDK attrs.xml>`, 目录 JSON 另存 `catalog-android.json`).
- [x] (插件) 外部文档的同目录 XML 读写 (SAF 树权限的一次授权与持久化; file 方案直接读写), 草稿与写回失败另存规则同 D39.
- [x] (插件) 私有工程升格为文件夹 (3.4 节): `ProjectStore` 的文件夹工程, 首页徽标, 草稿键 / 来源记录迁移, zip 与目录的导入 / 导出, FileProvider 路径, 运行与导入 AutoJs6 的文件夹 content Uri (核实宿主 `RunIntentActivity` / `ImportIntentActivity` 对项目文件夹 Uri 的支持, 缺失时在宿主侧补入口并记录提交).
- [x] (插件) 预览: 文件片段经同一 `setLayout` 路径; `androidLayout` 标志在 v3 之前的宿主上退回 "按 AutoJs6 标签解析并提示可能不一致".
- [x] (测试) JVM: `LayoutFileReaderTest` (路径识别, 片段引用, 无损写回), `UiCatalogAndroidTest` (第二份目录), `ProjectFolderTest` (升格, 迁移, zip 往返); 设备: `UiLayoutFileDeviceTest` (外部文档旁的 XML 文件打开 -> 编辑 -> 写回原文件; 私有工程新建 XML 文件 -> 升格 -> 导出 zip -> 重新导入一致; 运行交出文件夹 Uri 并在宿主可解析); `verify_designer_recovery.py` 对文件夹工程的分支.

  证据 (2026-10-16, 插件提交均已推送):
  - 文件片段: `a3b2c05` (`XmlCst.parseFile` 保留 BOM / 声明 / 注释, 拒绝 DOCTYPE; `UiScriptReader` 识别 `ui.layoutFile("...")`, `ui.inflate(files.read("..."))` 与 `ui.useAndroidLayout(...)`, 画布根取最早的布局调用; 事件桩支持文件片段; `UiScriptWriter.writeFiles` 无损写回并还原 CRLF).
  - 文件夹工程存储: `1727df7` (`<stem>/main.js`, `promote` 迁移脚本 / 草稿 / `drafts/<键>.files/` / 来源记录 / 图片目录, 路径不越出文件夹; 设计器从同目录读写布局文件, 只保存与草稿改动的文件; 读不到的文件列入问题面板; 首页 `demo/`).
  - 新入口 (D66 / D67): `aa0238b` (`UiLayoutFiles`: 直接写在 `ui.layout(...)` / `ui.inflate(...)` 中的字面量移到文件, 新建只含根容器的文件并在脚本末尾追加 `let <名称>View = ui.inflate(files.read(...))`, 均为改写脚本后读回的整份文档 `ReplaceDocument`, 一步撤销; 片段行菜单与路径对话框; 保存时单文件私有工程升格并写满足宿主完整校验的 `project.json`; 新建工程的 ui 类型可选 "脚本内" 或两种文件夹模板; 原生判断跟随 `useAndroidLayout`, 与宿主 `UI.kt` 一致).
  - 预览: `8a5fbef` (契约 v3 宿主收到文档文件夹的 zip: `setLayout` 带 `resourcesFd` / `layoutPath` / `androidLayout`, `runScript` 带 `projectFd` / `scriptPath`; 文件夹工程整体, 文件 Uri 旁只取 `res/`, 设计器当前的布局文件文本替代盘上内容; 指纹不变不重写 zip; v3 之前的宿主在问题标签页提示).
  - zip 导出 / 导入 (D62): `8b7f9c2` (导出 `main.js` 在顶层, 缺 `project.json` 时补上; 首页导入 zip 与文件夹 (`OpenDocumentTree`): 顶层或唯一外层文件夹内的 `main.js`, 或唯一顶层脚本改名为 `main.js`, 单个脚本成单文件工程; 不安全的条目名, 超限或无脚本整体拒绝).
  - 交给 AutoJs6 (D64): 宿主本地提交 `faf412ef7` (`RunProjectIntentActivity` 解到缓存目录并以工程方式运行, 没有脚本在其中运行后删除; `ImportProjectIntentActivity` 解到脚本目录下同名文件夹, 已存在时询问覆盖 (zip 内路径替换, 其它文件保留) 或另存为 `name-2`; 只接受显式 Intent 的 content Uri; `ProjectArchiveTest` 4 项) 与 `ff46ff57b` (zh-Hans 更新日志); 插件 `a0e706e` (文档带布局文件时运行 / 导入经 `projects/.handoff/` 的 zip 交给这两个入口, 旧宿主上两个去向换成一行 "需要更新版本的 AutoJs6").
  - 原生命名空间目录 (D65): `9dfd237` (`generate_ui_catalog.py --android <宿主> --sdk <SDK>` 由宿主属性表与设备覆盖快照生成 `catalog-android.json`: 110 个类 1314 个属性 (985 可设置, 329 不可设置), 135 个布局参数, 202 个标签; 类继承链取自 SDK 与宿主同一组 AAR; 原生片段的校验 (前缀, 未知属性 / 布局参数, 宿主不能设置, 值格式), 检查器 (`android:id` 写作 `@+id/...`, 按类与父布局列出可设置属性), 面板插入视图类).
  - content 文档的文件夹: `ce2aa73` (读不到布局文件或使用布局文件菜单时请用户一次性选择脚本所在文件夹 (读写持久授权), 核对其中有该脚本, 之后经 DocumentsContract 读写并按需创建目录与文件).
  - 设备 (本提交): `UiLayoutFileDeviceTest` 6 项 (提取后保存成文件夹工程, 新建 Android XML 文件并插入 TextView, 文件 Uri 旁的布局文件写回原文件且不产生私有副本, zip 往返, 交给宿主运行 (项目脚本读自身文件并写出标记) 与导入 (脚本目录出现同名文件夹), 预览解析工程 `res/` 的字符串与尺寸) 在 AVD_API_33 (嵌入, 只读实例) 与 AVD_API_24 (快照, 只读实例) 全部通过, 宿主为含 `faf412ef7` 的 debug 构建; `.python/verify_layout_files.py` 驱动安装与运行; `verify_designer_recovery.py --kind folder` (新增分支) 在 API 33 通过 (杀进程后恢复的草稿带回布局文件中插入的按钮); 回归 `UiDesignerDeviceTest` / `UiPreviewDeviceTest` / `UiInteractiveDeviceTest` / `DesignerHomeDeviceTest` / `DesignerHostDeviceTest` 共 21 项在 API 33 通过. JVM 277 项.
  - content 文档的文件夹授权 (真机, 维护者操作系统选择器): Sony XQ-AT72 (QV710AF65F, Android 12, 宿主 debug 5451) 上 AutoJs6 编辑器 "更多 > 在 3-Block Compose 设计器中打开" 交出 `Scripts/designer-folder-check/main.js` (只以 `ui.layoutFile("layout.xml")` 布局) 的 content Uri; 布局文件读不到时报错页提供选文件夹按钮 (本次补上, 原先只在编辑页的问题标签页与布局文件菜单里有), 选定后重新读取; `.python/verify_document_folder.py check` 四项通过: 记录了文件夹, 大纲出现 `text #hello`, 选项启用的 `DocumentFolderDeviceTest` 经持久授权读回 `layout.xml` 并在新子目录写出 `res/layout/probe.xml`, 文件出现在存储上; 之后 `cleanup --uninstall` 删除测试文件夹并卸载插件与测试包 (该机原本未装).

### P11.4 1.8.0 文档与发布 (文档 + 发布)

- [x] (文档) 文档站 `api/composeDesigner.md`: "XML 文件与文件夹工程" 小节 (识别形态, 升格规则, 导入 / 导出, 原生命名空间目录来源), "预览差异" 一段; README / changelog `v1.8.0` 10 语言; `AGENTS.md` 第 2 / 4 / 12 / 13 / 14 (复核线表与脚本) / 15 节.
- [x] (发布) 1.8.0 本地 gate (第 14 节顺序 + `verify_designer_performance.py` 在 Redmi), 推送 / Release / 索引.

  证据 (2026-10-16):
  - 文档: 文档站本地提交 `9e3b834` (`api/composeDesigner.md` 新节 "XML 文件与文件夹工程 (1.8.0)": 识别形态, 新建 XML 文件, 文件夹工程, 外部文档, 预览中的工程文件, Android 原生 XML; 用途 / 入口 / 工程与文件 / 可识别子集 / 编辑页 / 衔接 / 限制各处的 1.8.0 说明), 与此前的 `47af615` (预览差异) 和 `22d7b1e` (宿主的 Android XML 属性页与 "工程资源") 一起推送 (维护者 2026-10-16 决定文档站 "全部推送", 含 MCP Server 会话的 `c381a59` / `efe4cee`); 插件 `8ea8a7f` (changelog `v1.8.0` 10 语言: 1 提示 8 新增 2 优化 1 依赖; README 状态 / 功能 / 兼容性 / 安全 10 语言; `VERSION_NAME` 1.8.0, build 197); `AGENTS.md` (本地, 被 Git 忽略) 第 2 / 4 / 12 / 13 / 14 / 15 节.
  - 1.8.0 本地 gate (build 197, 源码即 `8ea8a7f`): markdown / icon `--check`; JVM 277; debug / androidTest / lint (0 错误, 15 警告) / release / verifySharedClasspath; classpath debug 与 release `--shrunk` (unresolved 0); release 带摘要包 `autojs6-plugin-three-block-compose-v1.8.0-bd7dab34.apk` 5993742 B (SHA-256 `9ca55f7b...a250`), DEX 方法引用 99366, 均在复核线 6177215 B / 105804 内; `.python/tests` 通过. 全套 instrumentation 139 项 (宿主为含 `faf412ef7` 的 debug 构建): AVD_API_33 只读实例 132 通过 7 跳过, AVD_API_24 只读实例 125 通过 14 跳过, 均无失败. release 包: API 24 `verify_standalone_launch` (`ui_preview=snapshot`) 与 `verify_host_handoff` 通过; API 33 `verify_standalone_launch` (`sandbox=ran`, `ui_preview=frame`), `verify_designer_recovery --kind all` (compose / ui / folder 三类均恢复) 与 `verify_host_handoff` 通过. 设计器性能复核线 (Redmi 22120RN86C, debug build 197): 16 项全部在线内 (例: 画廊读取 p90 57.2 / 65 ms, 打开最大示例 616.5 / 1000 ms, 撤销恢复一行 323.1 / 570 ms), 记录于 `docs/dev/p8-designer-performance.md`, 之后装回 1.8.0 release 并移除测试包 (该机宿主 5352 未动). content 文档的文件夹授权的真机证据见 P11.3.
  - 发布: 插件 `43cedbf` (tag `v1.8.0`, 应用源码与 `8ea8a7f` 相同) 推送; GitHub Release `v1.8.0` (APK `autojs6-plugin-three-block-compose-v1.8.0-bd7dab34.apk` 5993742 B, SHA-256 `9ca55f7b...a250`, 与 `SHA256SUMS`, 说明为 changelog 的中英文摘要与所需宿主提交); 官方索引 `ab8d019` 准入 3-Block Compose 1.8.0 (manifest 197, 生成器经 gh 令牌访问 GitHub API, 76 项索引测试与 Markdown 检查通过); 文档站推送至 `ba126b3` (内容 `47af615` / `22d7b1e` / `dc27392` (`9e3b834` 合入生成的 HTML / JSON 后的提交), 另一会话的 `c381a59` / `efe4cee`, 计数 119), Offline Docs `4634ab3` (同步 `dc27392`, 来源记录两处, 6.8.6 changelog 10 语言的设计器条目加 1.8.0 一句并新增 Android XML 属性条目, build 147; 另一会话的 `50a492e` 一并推送; 推送前检查 6 项通过). 推送时插件, 文档站与 Offline Docs 的 Markdown / Icon Studio 工作流通过, 插件 Build integrity (含两档模拟器 instrumentation) 运行中. 宿主 `faf412ef7` / `ff46ff57b` 等仍只在本地 master (D7).

### P11.5 预览契约 v4 与宿主侧缩小项 (宿主, 本地 master)

契约编号: 3.3 节与附录 A 写的 "v3" 在实施时为 v4 (v3 已由 P11.H1 用于工程资源); `androidLayout` 已在 v3 交付, 本阶段不再追加.

- [x] (宿主) 契约 v4 只追加 (3.3 节): `getPreviewInfo()` 的四个能力标志, `supportedContractVersions` 列出 1 - 4 (v3 冻结为 `VERSION_V3` / `HASH_V3`), `runScript` 的 `busyWaitLimitMs`, `sendLifecycle` / `onLifecycleHandled`; `HASH` 更新, Binder getter 仍 v1, 信息旧键仍 v2; `UiPreviewContractTest` 覆盖 v1 / v2 / v3 前缀兼容.
- [x] (宿主) 对话框入画布: 预览会话实现对话框宿主, `dialogs.*` 与 `dialogs.build` 的对话框挂在嵌入根上, 返回键 / 关闭 / 释放清理.
- [x] (宿主) 生命周期: 预览承载提供 `ui.emitter` (此前为 null), `sendLifecycle` 投递到它, `back_pressed` 未消费时以 `exit` 结束; 窗口级函数的仿状态栏 / 导航栏色条与根背景; 忙等观察器 (阈值来自请求, 缺省 5 s).
- [x] (测试) `UiPreviewServiceTest` 追加: v4 信息, 对话框在根内 (以 `dialogs.alert` 脚本找根下的对话框视图), `sendLifecycle` 到达 `ui.emitter` 与未消费返回键结束运行, `while (true)` 脚本在阈值内以 error 结束且主线程恢复, `statusBarColor` 色条的像素; API 33 / 35 模拟器通过.

  证据 (2026-10-16): 宿主本地提交 `ef88268be` (契约 v4: `VERSION` 4, `HASH` `b7591e9e...81b2`, 冻结 `HASH_V3` `196e52cd...`; 协议文档新节 "Runs closer to an Activity (V4)"), `2d7944d10` (实现), `418ba88a6` (测试与 `.python/ui_preview_device_tests.py`). 实现要点:
  - 对话框: material-dialogs 分支新增 `MaterialDialogPresenter` (经 Context 链解析, 与主题委托同法): 被呈现的对话框不创建窗口, `show` / `dismiss` / `isShowing` / 取消标志由呈现者接管, 关闭监听按窗口的消息顺序投递. 宿主 `Dialogs` 先取 `UiContentHost.dialogContextOf(runtime)`, 预览运行返回会话的 `UiPreviewDialogContext` (会话配置 + Light 主题), 对话框视图移入根上的 `UiPreviewDialogLayer` (遮罩, 按会话尺寸取对话框宽度与最大高度, 点外部 / 返回键按其取消标志取消, 运行结束时全部移除).
  - 生命周期: 运行自带 `EventEmitter` (`UiContentHost.lifecycleEmitter`, `ui.emitter` 先取它), 开始时像 `ScriptExecuteActivity` 一样在投递脚本主体后发 `create` / `resume`; `sendLifecycle` 校验后在调用返回后投递, `back_pressed` 先交给顶层对话框, 再交给 `ui.emitter`, 未消费以 `exit` 结束; `onLifecycleHandled` 回报 `consumed`.
  - 窗口颜色: `UiContentHost.onWindowColor`, 根视图分为内容框 (`ui.root`), 两条色条 (24 / 48 dp) 与对话框层; 下一次运行和无运行时的布局回到无色条的根. 其余窗口级函数的警告改写到运行自己的控制台 (P11.2 证据中 "警告不回传" 的缺口随之修复), 每次运行每个函数一次.
  - 忙等: `UiPreviewBusyWatch` 由 Rhino 指令观察器 (`InterruptibleAndroidContextFactory`) 调用, 以运行的全局作用域找到主线程上正在执行的运行, 心跳 (主循环每 200 ms) 过旧超过阈值即抛出脚本 `catch` 接不到的中断异常, `onScriptError` 带 `ERR_AUTOJS6_UI_PREVIEW_BUSY_WAIT`, 行号与 `elapsedMs`; `stopScript` / 替换 / 释放先中断忙碌的脚本, 阈值为 0 时也有效. 另: 事件处理函数与定时器中的未捕获错误现在也经 `onScriptError` 回报 (此前只以 `exit` 结束).
  - 验证: JVM 契约 8 项, 预览包 28 项 (新增忙等上限与生命周期事件校验); 设备 `UiPreviewServiceTest` 20 项在 AVD_API_33 (emulator-5584) 与 AVD_API_35_16K (emulator-5590) 只读实例全部通过 (新增 4 项: 对话框入根并接管返回键, 生命周期与返回键, 色条与背景像素和运行控制台警告, 忙等在 1 s 上限内以 error 结束 (第 6 行) 且阈值为 0 时 `stopScript` 能停下忙碌的点击处理函数). 兼容: 插件 1.8.0 (v3 客户端) 对 v4 宿主跑 `UiInteractiveDeviceTest` / `UiPreviewDeviceTest` 4 项与 `verify_layout_files.py` 6 项通过. 宿主对话框相关回归 (`EditorPopupMenuThemeInstrumentationTest`, `ExplorerRenameDialogMenuInstrumentationTest`, `CircularMenuOperationDialogInstrumentationTest`, 共 8 项) 通过; `InfoDialogInstrumentationTest` 在两台模拟器上因 MainActivity 45 s 内不空闲 (主线程持续派发 vsync, 进程中没有预览运行) 而失败, 与本阶段无关, 未处理.
  - 契约 AAR `ui-preview-api-release.aar` 21769 B, SHA-256 `29abeb01...d392`, 重建字节一致 (副本在会话 scratchpad); 插件在 P11.6 换用并更新锁. 宿主 zh-Hans changelog 条目留到 P11.6 (届时写明所需插件版本).

### P11.6 插件对接 v4 (插件, 1.9.0)

- [x] (插件) `UiPreviewClient` 读 v4 能力 (原文 "v3", 见 P11.5 的编号说明); 画布上方行加 "返回" 按钮 (运行中可见), 不可见 / 可见时发 pause / resume 而不再重跑 (v3 宿主) 或维持现状 (旧宿主); 设置页 "交互预览忙等上限" (1 - 60 秒, 默认 5) 经 `runScript` 传给宿主; 差异列表按能力过滤 (P11.2 已备好 `UiDesignerSession.hostCapabilities` 与 `PreviewDifferences.visible`, 这里只需把 v3 宣告映射为 `PreviewCapability`; v3 宿主提供 `ui.emitter` 后 `ACTIVITY_EVENTS` 的说明要改为 "事件不会发生" 而不是 "这一行出错"); `androidLayout` 标志随片段发送.
- [x] (测试) `UiInteractiveDeviceTest` 追加 (v4 宿主时): 对话框脚本的对话框出现在画布内 (经 `UiAutomation` 点击其按钮), 返回键结束运行并显示 `exit`, 隐藏 / 显示发 pause / resume 而脚本不重跑 (计数器脚本的值保留), 忙等脚本在阈值内结束且设计器可用; 旧宿主上这些项按假设跳过.

  证据 (2026-10-16): 插件提交 `ada93a0` (契约 AAR 换为 v4: 宿主 `ef88268be`, 21769 B, SHA-256 `29abeb01...d392`, 锁 / `libs/README.md` / `THIRD_PARTY_NOTICES.md` / `UiPreviewContractGuardTest`), `2c39f27` (实现与测试). 实现要点:
  - 握手按 v4 / v3 / v2 / v1 依次协商 (v3 改认冻结的 `VERSION_V3` / `HASH_V3`, `androidLayout` 的门槛随之改为 `>= VERSION_V3`), v4 的四个信息标志经 `UiPreviewClient.capabilitiesOf` 映射为 `PreviewCapability` (只在可运行脚本时生效), 编辑页交给 `UiDesignerSession.hostCapabilities`; v4 宿主上 `ACTIVITY_EVENTS` 的说明换为 `designer_ui_diff_activity_events_v4` ("预览中的 ui.emitter 只会收到 create, resume, pause 与 back_pressed ..."), 窗口颜色 / 对话框 / 生命周期 / 忙等四类差异被过滤.
  - `lifecycleSupported` 时编辑页的运行效果不再以 `visible` 为键: 隐藏 / 显示只发 `pause` / `resume` (`UiPreviewClient.sendLifecycle`, 运行中才发), 运行继续; 画布上方行在运行中显示返回键按钮 (`designer-ui-back`); 确认对话框换用 `designer_ui_interact_confirm_message_v4` (说明后台时收到 pause / resume). 旧宿主保持 1.8.0 行为 (隐藏即停止, 显示重跑).
  - 设置页新增 "设计器" 分组的 "主线程占用上限" (`InteractivePreviewSettings`: 1 / 2 / 3 / 5 / 10 / 15 / 30 / 60 秒, 默认 5, 选项对话框加一段说明; 偏好 `designer` / `ui-busy-wait-limit-seconds`), 每次运行以 `busyWaitLimitMs` 交给声明 `busyWaitLimitSupported` 的宿主; 宿主以 `ERR_AUTOJS6_UI_PREVIEW_BUSY_WAIT` 结束运行时 `UiPreviewClient.busyWait` 记下行号 / 耗时 / 上限, 画布上方与控制台显示 `designer_ui_interact_busy_wait_line` ("AutoJs6 停止了脚本: 它占用主线程超过 N 秒 (第 L 行)"). `onLifecycleHandled` 记入 `lastLifecycle`. 新字符串 10 个 x 10 语言.
  - 验证: JVM 280 项 (新增 `UiPreviewCapabilitiesTest` 2 项, `UiPreviewContractGuardTest` 改为 v4 并追加 1 项); lint 0 错误 15 警告 (与 1.8.0 相同). 设备 (宿主为 P11.5 的 debug 构建, 含 `2d7944d10`): `UiInteractiveDeviceTest` 在 AVD_API_33 (emulator-5584) 与 AVD_API_35_16K (emulator-5590) 只读实例各 5 项通过 1 项跳过 (v2 的 "隐藏即停止" 在 v4 宿主上按假设跳过); API 35 换装 P11.5 之前的宿主 (契约 v3) 时 3 项通过 (含 "隐藏即停止") 3 项 v4 用例跳过, 之后装回. 新增用例: `dialogs.build` 的确认按钮位置由脚本经控制台报告后经 `UiAutomation` 点击 (嵌入树不在插件进程的无障碍树中: 按文字查找与逐子节点遍历都止于 SurfaceView), `dialogs.confirm` 接管返回键 (`consumed` 为 true, 脚本回调 false, 运行继续); 计数器脚本隐藏 / 显示收到 `pause at 1` / `resume at 1`, 不重跑, 之后点击得到 `clicked 2`, 无处理函数时返回键以 `exit` 结束; 上限设为 1 秒时 `while (true)` 在 10 秒内以 error 结束, 行号在 8 - 9 行内, 画布上方文字与本地化字符串一致, 关闭开关后回到布局预览. 回归: `SettingsDeviceTest` 4 项, `UiPreviewDeviceTest` 1 项通过; `UiDesignerDeviceTest` 的预览差异用例改为先等握手, 按宿主宣告的能力决定窗口颜色与对话框两条是否列出 (脚本另含 `ui.finish` 与 `ui.emitter.on('options_item_selected')` 两条任何宿主都不消除的差异), 7 项通过. 模拟器 `screencap` 拍不到置顶的嵌入表面 (画布全黑, 无对话框时也一样), 不作为证据.
  - 宿主 zh-Hans changelog 条目 (P11.5 留到本阶段): 宿主本地提交 `0ea8b114f` (交互预览的对话框, 色条, 忙等上限与生命周期事件, 写明所需插件版本; 只暂存本条, 其它会话的未提交改动保留). 插件 changelog / README 的 1.9.0 条目按 1.8.0 的做法留到 P11.9.

### P11.7 TSX ui 布局 (插件, 1.9.0)

- [x] (插件) `TsxReader` 的 ui 分支 (3.5 节): 指令识别, JSX 实参 -> `XmlNode`, 表达式容器只读, 片段源码范围 (核实解析树的偏移, 缺失时解析契约 v2 只追加偏移并换 AAR), 规范化写回, TypeScript 语法的事件桩; `Dialect.TSX` 的 UI 工程进入现有画布与面板.
- [x] (测试) JVM `TsxUiRoundTripTest` (宿主 TSX 示例中的 ui 布局读出, 规范化写回后再读相等, 未编辑片段字节不变), `TsxUiReaderErrorTest`; 设备 `DesignerTsxDeviceTest` 追加 ui 工程一项 (需 TypeScript Engine 0.7.0+ 的设备).

  证据 (2026-10-16): 插件提交 `de7bc58` (实现与测试), `2253cf8` (沙箱列表示例的时间上限, 见下). 实现要点:
  - 读: `parse/TsxUiReader.kt` 接收解析服务的树 (偏移为 UTF-16 的 `start` / `end`, 契约 v1 已有, 不升 v2). `"ui"` 指令之后, `ui.layout(...)` / `ui.inflate(...)` 的 JSX 实参与以 `const` / `let` 声明的 JSX 变量 (`boundTo`, 经变量交给 `ui.layout` 的为画布根) 成为片段; 元素 / 属性 / 文本照 JS 的 UI 工程建模, 字符串字面量的表达式容器按字符串读, 数字 / 布尔 / 负数字面量记入 `UiDocument.literalValues` 供预览, 其它表达式容器只读. 处理函数识别 `ui.<id>.on`, `(ui as any).<id>.on`, `click` / `longClick` 简写, `ui.findView('<id>')?.click` 与内联 `onClick`; `ui.useAndroidLayout`, `ui.registerWidget` 照 JS. 宿主 TSX 运行时不接受的写法报错并给行列: `ui.layoutFile` (TSX 暂不支持), `<compose.X>` 标签, 属性展开, 同名属性两次, 片段有两个根, JSX 变量被重新赋值, `ui.layout` 的实参不是 JSX, 语法错误 (来自解析诊断), 缺指令. 桩的插入点为 `ui.layout` 语句及其后紧随的处理函数语句之后, 缩进取该语句的缩进.
  - 写: `codegen/TsxUiWriter.kt`, 未编辑的片段原样写回 (`XmlFragment.tsx` 记原文与读出的根, 根未变即原文), 被编辑的片段整体按规范重写 (D57): 子元素各占一行缩进 4 格, 只有一段安全文本时内联, 无子元素自闭合 `<tag ... />`, 属性值为 `name='value'`, 含引号 / 换行 / 花括号等时为 `name={'...'}`, 表达式容器去掉公共缩进后按新位置缩进. 新建工程模板 (`ProjectStore.TSX_UI_TEMPLATE`) 就是规范形式.
  - 预览: `preview/TsxLayoutXml.kt` 把 TSX 根降为宿主 TSX 运行时会交给布局器的 XML (去掉 `on[A-Z]` 属性, 无值属性为 `true`, 属性排序, 只含注释的表达式容器去掉, 文本类元素的已知文本子节点合并为 `text`, 元素间的空白文本去掉), 未知表达式照 JS 的 UI 工程提示. 校验 (`UiValidator`): TSX 中 `on[A-Z]` 属性不报未知属性, 除 `onClick` 外的事件属性报错 (宿主只绑定 onClick), `onClick` 不是函数表达式报错, 文本子节点的规则同宿主.
  - 事件桩 (D68): click 在元素上加 `onClick={() => { // TODO }}` (已有则不加), 不需要 id; 其它事件追加 `(ui as any).<id>.on('<event>', (_event: any, _view: any) => { ... })`. 检查器的 "添加" 对 TSX 的 click 可用, 已有内联 `onClick` 时显示为已处理.
  - 工程: 新建工程对话框的 ui 布局位置新增 "TSX 脚本内" (需要 TypeScript Engine 的提示同 Compose TSX); TSX ui 工程的运行 / 导出按 `.tsx` 文件交给 AutoJs6; 交互预览开关关闭并显示 `designer_ui_interact_tsx_needs_host` (P11.H3 前); 布局文件菜单只对 JS 方言提供. 新字符串 2 个 x 11 目录.
  - 快照: `TsxSnapshotRecorder` 新增 ui 源码 (`tsx/ui/` 手写样例 3 个, 新建模板, 宿主 `legacy-xml.tsx`) 与其规范形式 `tsx/ui-canonical/`, 错误样例 `tsx/ui-errors/` 9 个; 树经 `.python/generate_tsx_snapshots.py --engine` (TypeScript Engine 解析 worker, typescript 6.0.3) 生成, 记录器连跑两次结果相同.
  - 验证: JVM 292 项 (1 项跳过; 新增 `TsxUiRoundTripTest` 8 项: 五个 ui 源码未编辑字节不变, 规范形式再读形状相同且为不动点, 编辑只重写所在片段且撤销后回到原文, 字面量 / 处理函数 / 插入点, D68 桩, 预览 XML, 校验规则; `TsxUiReaderErrorTest` 4 项覆盖 9 个错误样例的消息与行列); lint 0 错误 15 警告. 设备 (宿主为 P11.5 的 debug 构建, TypeScript Engine 0.7.0 debug): `DesignerTsxDeviceTest` 在 AVD_API_33 (emulator-5584) 与 AVD_API_35_16K (emulator-5590) 只读实例各 4 项通过 1 项跳过 (无解析服务的用例按假设跳过); 新增用例新建 TSX ui 工程, 宿主预览 3 个元素且元素边界含 `title` / `ok`, 交互开关显示需新宿主的说明, 按钮的 click 显示为已处理且无 "添加", 给 `title` 加 long_click 桩后保存的文本等于模板加 `(ui as any).title.on('long_click', ...)` 桩, 重开再保存不变. 回归 (API 33): `UiDesignerDeviceTest`, `UiPreviewDeviceTest`, `UiInteractiveDeviceTest`, `UiLayoutFileDeviceTest` 共 20 项通过.
  - 编译检查: 新增 `.python/verify_tsx_compile.py` (把 `.tsx` 推到 `Scripts/<目录>/` 经 AutoJs6 的运行入口启动, 以宿主控制台判断 TypeScript Engine 是否编译通过, 每个文件一行 PASS / FAIL); emulator-5584 上新建模板, 设备用例保存的带 long_click 桩的文本, TypeScript Engine ui 模板的规范形式三个文件均 PASS (宿主编译选项为 `strict`, `noUnusedParameters`, `noEmitOnError`). D68 之前的探针 (原定的 `ui.<id>.on(...)` 写法 TS2339 / TS7006 失败) 记在决策表.
  - 另: P11.6 推送后的 CI 运行 (`d07d86e`) 中 `SandboxEngineTest` 的 1000 项列表示例偶发超出预览的 3 秒墙钟上限 (重跑通过); 该测试检查键与滚动引用而不是速度, `2253cf8` 把它的上限放宽到 30 秒.

### P11.H3 预览契约 v5: TSX 运行 (宿主 + 插件, D69)

- [x] (宿主) 契约 v5 只追加: `runScript` 的请求键 `scriptDialect` (`js` / `tsx`, 缺省 `js`) 与信息标志 `tsxRunSupported` (宿主找得到 TypeScript Engine 0.7.0+ 的编译服务时为 true); `tsx` 运行先经宿主现有的 TypeScript 编译路径 (与运行 `.tsx` 文件相同的编译选项与声明), 失败时 `onScriptError` 带 `errorCode` `ERR_AUTOJS6_UI_PREVIEW_COMPILE_FAILED` 与首个诊断的行号和文字, 运行以 `error` 结束; v4 身份冻结.
- [x] (插件) v5 宿主上 TSX UI 工程的交互预览开关可用 (工程 zip 与脚本路径照常交出), 旧宿主上开关处说明需在 AutoJs6 中运行; 编译错误显示在画布上方与控制台.
- [x] (测试) 宿主 `UiPreviewServiceTest` 追加 TSX 运行与编译错误 (需安装 TypeScript Engine 0.7.0+, 否则按假设跳过); 插件 `UiInteractiveDeviceTest` 追加 TSX 工程一项.

  证据 (2026-10-16): 宿主本地提交 `fb451f88d` (契约 v5: `VERSION` 5, `HASH` `cd6681bb...4ad0`, 冻结 `VERSION_V4` / `HASH_V4` `b7591e9e...81b2`; 协议文档新节 "TSX runs (V5)"), `204a33ee5` (实现), `407ba2f24` (测试), `58d7621c2` (zh-Hans changelog 的交互预览条目补上 TSX, 只暂存本条). 实现要点:
  - 契约: 不加事务, 只加 `scriptDialect` (`SCRIPT_DIALECTS` = `js` / `tsx`), `tsxRunSupported`, `ERROR_COMPILE_FAILED` (带首个错误诊断的 `line` / `column`, 从 1 起), 以及 "TSX 运行的行号是 TSX 文本的行号" 的约定; 版本列表为 1 - 5.
  - 编译: `UiPreviewScriptRun` 对 `tsx` 运行在工作线程以 `TypeScriptExecutionCompiler.prepare` 编译 `TypeScriptStringSource("<脚本名>.tsx")` (与运行 `.tsx` 文件相同的路径, 文件名由 `UiPreviewRequests.tsxSourceName` 从脚本名得出, 任务列表显示去掉扩展名的脚本名), 完成后在主线程以生成的 JavaScript 照常开始运行; `runScript` 先返回 `runId`, 其间的 `stopScript` / 替换 / 释放让运行在开始前以其原因结束, 编译结果丢弃. 编译失败时每个错误诊断以 level 6 经 `onConsole` 送出 (`<名>.tsx:<行>:<列> - error TS...`), 再 `onScriptError` (`TS<码>: <文字>`, 首个诊断的行列) 与 `onScriptExit` (`error`); 无诊断的失败 (未装编译器, 超时) 行列为 -1.
  - 行号: 运行中未捕获错误与忙等的行号经引擎的源映射 (`SOURCE_MAP_ENGINE_TAG`) 换回 TSX 行. 首轮设备测试中运行期错误的行号为 -1: 引擎在错误回调之后立即销毁, 主线程上报告时源映射已不在; 改为在回调当时换算 (`errorLine`), 忙等在指令观察器中换算 (观察器回调增加源名参数).
  - `tsxRunSupported`: 构建预览信息时在 Binder 线程以 `TypeScriptCompilerPluginClient.isAvailable()` 探测, 最多 3 秒, 主线程调用时为 false.
  - 验证: JVM 契约 8 项, 预览包 29 项 (新增方言与编译文件名一项). 设备 `UiPreviewServiceTest` 21 项在 AVD_API_33 (emulator-5584) 与 AVD_API_35_16K (emulator-5590) 只读实例全部通过; 新增用例 (装有 TypeScript Engine 时): 内联 `onClick` 的 TSX 计数器经点击得到 `clicked 1`, 类型错误文本得到 TS2322 于第 3 行第 7 列且没有任何输出, `throw` 位于 TSX 第 10 行 (其前的 interface 被编译器擦除) 时报告第 10 行, `runScript` 后立即 `stopScript` 只结束一次. API 35 首轮在第二个用例时宿主进程 SIGSEGV (ART JIT 代码缓存中的 Rhino `EmbeddedSlotMap.modify`), 之后两轮 21 项全部通过, 未再出现, 与预览代码无关.
  - 契约 AAR `ui-preview-api-release.aar` 22135 B, SHA-256 `734d47ee...2fec3`, 13 个类文件, 重建字节一致.

  插件提交 `2086c21` (契约 AAR 换为 v5, 锁 / `libs/README.md` / `THIRD_PARTY_NOTICES.md` / `UiPreviewContractGuardTest`), `09a313e` (实现与测试). 实现要点:
  - `UiPreviewClient` 按 v5 / v4 / v3 / v2 / v1 协商; `tsxRunKnown` (v5 宿主且可运行脚本) 与 `tsxRunSupported` (再加 `tsxRunSupported` 标志); `runScript` 收方言参数, TSX 文本带 `scriptDialect` = `tsx` 交出 (工程 zip 中的入口缺省为 `main.tsx`); `compiling` 从 TSX 的 `runScript` 起到运行回报边界, 控制台条目, 错误或结束为止; `compileFailure` 记下 `COMPILE_FAILED` 的行列与文字.
  - 编辑页: TSX UI 工程在 `tsxRunSupported` 时提供交互预览开关; 画布上方行在编译期间显示 "TypeScript Engine 正在编译 TSX 脚本", 编译失败显示 "TSX 脚本未通过编译 (第 N 行): TS...", 点按进入控制台 (每个错误诊断一行); 不能运行时的说明分为 "需要更新的 AutoJs6" (原文案) 与 "需要 TypeScript Engine 插件" (v5 宿主未装 TypeScript Engine) 两种. 新字符串 4 个 x 11 目录.
  - 验证: JVM 293 项 (1 项跳过; 契约守卫新增 v5 一项), lint 0 错误 15 警告. 设备 (宿主为本阶段 debug 构建, TypeScript Engine 0.7.0 debug): AVD_API_33 上 `UiInteractiveDeviceTest`, `DesignerTsxDeviceTest`, `UiDesignerDeviceTest`, `UiPreviewDeviceTest`, `UiLayoutFileDeviceTest` 共 27 项, 25 项通过 2 项按假设跳过 (v4 宿主上的 "隐藏即停止", 无解析服务的 TSX 用例); AVD_API_35_16K 上同一组 24 项通过 2 项跳过 1 项失败: P9.2 的 `aTsxProjectIsEditedThroughTheParserAndSavedAsTsx` 在等待解析服务打开文档的 30 秒内超时, 单独复跑 `DesignerTsxDeviceTest` 两轮均为 4 项通过 1 项跳过. 新增用例: TSX 计数器开启交互预览后经注入点击得到 `clicked 1`, 从开启到首条输出 API 33 约 1.4 s (宿主编译 349 ms), API 35 约 2.8 s (编译 2270 ms); 类型错误的 TSX 工程得到 `compileFailure` 第 7 行第 7 列 TS2322, 画布上方文字与本地化字符串一致, 控制台有该诊断, 脚本没有任何输出; `DesignerTsxDeviceTest` 的 P11.7 用例改为在宿主运行 TSX 时期待开关出现. 旧宿主 (v4 / v3) 上的降级路径沿用 P11.6 的协商结构, 本阶段未换装旧宿主复测 (两者都只存在于本地开发构建).

### P11.8 compose 联动 (插件, 1.9.0)

- [ ] (插件) `compose.attach` 解析与 `composeAttachments`, 子会话与复合撤销命令, 叠画与点选优先级, Compose 抽屉 / 大纲子树的切换, `ScriptWriter` 写回实参范围 (3.6 节).
- [ ] (测试) JVM `ComposeAttachmentTest` (解析, 关联, 写回不动点, 不可解析实参的提示); 设备 `UiComposeLinkDeviceTest` (含 `<compose>` 与 `compose.attach` 的脚本: 画布在容器边界内画出 Compose 节点, 点选后抽屉显示 Compose 属性, 改 Text 文本后保存只改渲染函数; 交互预览中宿主真实画出 Compose 内容 (API 33+ 与 v2 宿主)).

### P11.9 1.9.0 文档与发布 (文档 + 发布)

- [ ] (文档) 文档站: "缩小的差异" 表 (v3 宿主与旧宿主各一列), "TSX 的 UI 工程", "<compose> 容器的联动编辑"; README / changelog `v1.9.0` 10 语言 (写明所需宿主提交); `AGENTS.md` 修订.
- [ ] (发布) 1.9.0 本地 gate, 推送 / Release / 索引; 宿主 ui 回归 (v3 宿主在 API 24 / 30 / 33 / 35 模拟器).

### P11.10 宿主编辑器内布局预览面板 (宿主, 本地 master)

- [ ] (宿主) 编辑器 "预览" 开关与面板 (3.7 节): 进程内预览视图, 缓冲区解析, 400 ms 刷新, 点元素跳行, 失败说明, "在 3-Block Compose 设计器中打开" 入口; 横竖屏布局.
- [ ] (测试) 宿主 `EditorUiPreviewDeviceTest` (打开 ui 示例 -> 开预览 -> 面板出现元素 -> 改一行 -> 面板刷新 -> 点元素光标到行), API 24 / 30 / 33 模拟器.

### P11.11 2.0.0 文档与发布 (文档 + 发布)

- [ ] (文档) 文档站 `api/ui.md` 与 `api/composeDesigner.md` "与 AutoJs6 的衔接" 写编辑器面板 (所需宿主提交 / 版本); README / changelog `v2.0.0` 10 语言; 宿主 changelog 条目 (10 语言, 只加自己的).
- [ ] (发布) 2.0.0 本地 gate, 推送 / Release / 索引; 宿主发布配合时在真机矩阵 (Redmi / Pad / G8441 / 维护者指定) 补证据.

---

## 附录 A: 预览契约 v3 草案 (只追加)

实施时编号为 v4 (P11.5); 定稿见宿主 `docs/dev/ui-preview-plugin-protocol-v1.md` "Runs closer to an Activity (V4)". 与草案的差异: 不追加 `androidLayout` (v3 已有), 错误码另有 `ERR_AUTOJS6_UI_PREVIEW_NOT_RUNNING`, `onScriptError` 的忙等错误另带 `elapsedMs`, `busyWaitLimitMs` 可为 int 或 long.

```aidl
interface IUiPreviewService {
    // v1 ... v2 不变
    Bundle sendLifecycle(String sessionId, in Bundle event);   // event: "pause" | "resume" | "back_pressed"
}
interface IUiPreviewCallback {
    // v1 ... v2 不变
    void onLifecycleHandled(String sessionId, in Bundle result);  // runId, event, consumed
}
```

键: `KEY_DIALOGS_IN_CANVAS` = `dialogsInCanvas`, `KEY_LIFECYCLE_EVENTS` = `lifecycleEvents`, `KEY_BUSY_WAIT_LIMIT_SUPPORTED` = `busyWaitLimitSupported`, `KEY_WINDOW_FUNCTIONS_VISIBLE` = `windowFunctionsVisible` (信息); `KEY_BUSY_WAIT_LIMIT_MS` = `busyWaitLimitMs` (runScript 请求, 1000 - 60000 或 0); `KEY_ANDROID_LAYOUT` = `androidLayout` (layout 请求); `KEY_LIFECYCLE_EVENT` = `event`, `KEY_CONSUMED` = `consumed`; 错误消息 `ERROR_BUSY_WAIT` = `ERR_AUTOJS6_UI_PREVIEW_BUSY_WAIT` (作为 `onScriptError` 的 `errorCode`). 版本: `VERSION` 3, `HASH_INPUT` 以 `autojs6.ui.preview.v3|` 开头并在 v2 列表后追加; `BASE_VERSION` / `HASH_V1` 不变; `HASH_V2` 保留作记录.

## 附录 B: 文件夹工程的布局

```text
files/designer/projects/
|-- counter.js                 单文件工程 (不变)
`-- login/                     文件夹工程 (升格后)
    |-- main.js                脚本 (原 login.js)
    |-- layout.xml             ui.layoutFile("layout.xml") 引用的文件
    `-- res/                   其它资源 (图片等, 本阶段只列出不编辑)
drafts/
|-- counter.js
`-- login/main.js
sources.properties             来源记录: 文件夹工程以 "login/" 为键
```

导出 zip 的根为工程文件夹内容 (`main.js` 在顶层), 导入 zip 或目录时若含 `main.js` 即按文件夹工程创建, 否则取唯一的 `.js` / `.tsx`. 文件夹工程的 "运行" 交出 `main.js` 的 content Uri 并对整个文件夹授予读权限 (写权限只在 "在 AutoJs6 编辑器中打开" 时授予); 宿主以项目方式运行 (工作目录为该文件夹).

## 附录 C: 验证顺序与设备

在 `AGENTS.md` 第 14 节的基础上, 本阶段新增:

```powershell
py .python/verify_designer_performance.py --serial <Redmi serial> --apk <debug apk> --test-apk <androidTest apk>   # 每次 release 前; 其它设备只 --record
```

| 设备 | 用途 |
| --- | --- |
| Redmi 22120RN86C (API 33) | 复核线判定设备 (每次 release); 对话框 / 生命周期 / 忙等 (v3 宿主 debug 包) |
| Xiaomi Pad 23046RP50C (API 35) | 记录性能; API 35 输入法路径; compose 联动交互预览 |
| AVD_API_24 / AVD_API_30 / AVD_API_33 / AVD_API_35_16K | 全套 instrumentation 与宿主 ui 回归; 模拟器性能只记录 |
| G8441 (API 28, 横屏) | 快照模式与分栏布局的文件夹工程回归 |

真机按 P10 路线图附录 E 的授权覆盖安装 debug 宿主 (2026-10-15 维护者确认); 多台设备时用明确 serial; 其它会话占用的设备先经消息协调.

## 附录 D: 待决事项

### Q22 (P11.1 后): 复核线超线后的重定基线流程

超线由维护者确认是否接受; 接受后以何次采样为新基线? 选项: (a) 以当次 release 的 Redmi 采样为新基线 (与 P5 的 1.5.0 做法一致, 建议); (b) 连续两版超线才重定; (c) 每版都重采并以最近三版的最大 p90 定线.

### Q23 (P11.3 前): 文件夹工程的导出格式

(a) zip (建议: 单文件交付, 宿主 "导入" 入口也能收); (b) SAF 目录逐文件写入; (c) 两者都做.

已拍板 (2026-10-16): (a), 见 D62. 拍板前核实: 宿主现有 "导入" 入口只收单文件 (`ScriptIntents.enforceSingleFileImportBoundary`), 不认 zip; "运行" 入口按 content Uri 读入单文件文本, 插件私有文件的工作目录为空, `ui.layoutFile` 的相对路径无法解析. 因此另定 D64 (宿主 zip 入口). 同轮核实: 宿主 `ViewInflater.setAttr` 对 `android` / `app` 前缀与无前缀走同一组处理器, 其它前缀忽略, 且动态布局器不支持完整 Android 属性集; 原生布局文件的 `@drawable` / `@dimen` / `<include>` 需要工程 `res/` 目录 (`AndroidResourceResolver.configure`), 预览服务只收 XML 文本. 维护者因此选择宿主先行 (D65).

### Q24 (P11.8 前): compose 联动的编辑范围

(a) 只编辑 `compose.attach` 的渲染函数节点树 (建议); (b) 也允许在 XML 工程里新建 `<compose>` 容器并生成 `compose.attach` 桩 (类似 D52 的事件桩); (c) 允许把 XML 元素转换成 Compose 节点 (不建议, 语义不对等).

### Q25 (P11.10 前): 编辑器面板的位置与持久化

(a) 下半区 / 右半区分栏, 开关状态按文件记住 (建议); (b) 底部弹层, 不记住; (c) 独立页面 (全屏预览), 从编辑器跳转.
