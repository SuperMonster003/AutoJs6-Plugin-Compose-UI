# P10: ui 模式 (XML / E4X) 工程的可视化编辑与真实预览

本文是 `ROADMAP.md` 阶段 P10 的专门路线图: 让 3-Block Compose 设计器除 Compose 工程外也能打开传统 `"ui";` 模式脚本 (E4X XML 布局), 原样编辑, 并以宿主自己的布局器实时预览. 2026-10-15 完成可行性评估, 维护者同日拍板四项方向性决策 (D46 - D49, 已同步写入 `ROADMAP.md` 固定决策与附录 D 的 Q17 - Q20); 全部条目未开始, 未勾选. 使用方式与 `ROADMAP.md` 相同: 条目完成后勾选并写证据 (提交 hash / 测试类名 / 设备与 API / 度量值), 证据等级按 `ROADMAP.md` 附录 E; 条目前缀 `(插件)` 本仓库, `(宿主)` `D:/idea-projects/AutoJs6`, `(文档)` 文档站与离线文档, `(测试)`, `(发布)`.

工程约定仍以 `AGENTS.md` 为准. 本阶段触及 `AGENTS.md` 第 1 节 "设计器编辑时不执行脚本" 与第 15 节的安全描述, 落地时同步修订 (见 P10.5 / P10.7 条目).

---

## 0. 结论

可行, 分两级交付; "真实无偏差预览" 只能由宿主进程来画, 插件进程画不出来.

- 宿主 `ui.layout` 的链路是 `E4X -> toXMLString -> XmlConverter -> DynamicLayoutInflater`: 108 个 inflater 类, 每个都持有 `ScriptRuntime`; `{{expr}}` 在应用属性时于脚本作用域内 `eval` (`UI.kt` 的 `bindValueForApplyingAttribute`), `<list>` / `<grid>` 项模板在绑定数据时以项目对象为上下文再求值. 这些代码只存在于宿主进程, 并依赖 appcompat / Material 组件 (本插件按 `AGENTS.md` 5.2 不引入 View 体系库). 在插件内复刻一套 View 渲染器体量大且永远有偏差, 排除 (Q17 选项 c).
- Android 11 (API 30) 起的公开 API `SurfaceControlViewHost` 允许一个进程持有 View 树并把 `SurfacePackage` (Parcelable) 交给另一进程的 `SurfaceView` 显示, 触摸由系统直接路由到持有方. 宿主可以在自己进程里用真实布局器与真实运行时 inflate, 嵌进设计器画布. 这是 "真实预览 + 可交互 + 嵌在画布内" 的唯一公开路径 (D46).
- 残余偏差无法消除, 只能明示: 嵌入的 View 树没有 Activity 窗口, `activity.*`, 状态栏 / 导航栏颜色, `ui.finish`, 返回键, 对话框, 部分弹出层与输入法行为会与真实运行不同 (3.5 节清单). 真正的零偏差只有 "在 AutoJs6 中运行" 一条路, 设计器已有该去向.
- 解析侧没有障碍: 宿主 Rhino 分支 (插件 slim jar 同源) 把 `{{msg}}` 保留为 XML 原文, 只把单层 `{expr}` 当 E4X 表达式; 插件现有 `Parser` 默认 `xmlAvailable`, 已能得到 `XmlLiteral` 的 `XmlString` / `XmlExpression` 片段 (2.2 节).

---

## 1. 维护者决策 (2026-10-15)

| 编号 | 决策 | 含义 |
| --- | --- | --- |
| D46 | 预览架构: 宿主内嵌预览服务 | 宿主新增受 `org.autojs.permission.PLUGIN` 保护的 Binder 预览服务与独立契约模块 `plugin-api/ui-preview-api`; 在宿主进程用真实 `DynamicLayoutInflater` + `ScriptRuntime` 渲染, 经 `SurfaceControlViewHost` 嵌入设计器画布 (API 30+), 可交互, 可点选节点 (宿主回传节点边界). 不在插件内复刻 View 渲染; 不以 "外置实时运行" 替代画布预览 (该去向作为既有 "发送到 AutoJs6 运行" 保留) |
| D47 | 脚本执行: 两级 | "布局预览" (默认, 随编辑实时刷新): 宿主只运行合成脚本 `"ui"; ui.layout(<xml>)`, `{{expr}}` 的值由插件沿用 P9.3 的沙箱对顶层声明求值后随请求传给宿主, 用户代码不在宿主运行, 无副作用. "交互预览" (显式开关, 1.7.0): 宿主真实运行整份脚本, `ui.layout` 的内容进入嵌入树, 等价于 "运行" (有副作用, 完整权限), 事件处理函数 / 列表数据源 / 自定义控件全部生效 |
| D48 | API 24 - 29: 位图快照预览 | 宿主侧把 inflate 后的 View 树 measure / layout / draw 为位图, 连同节点边界经调用方拥有的文件描述符传回; 画布显示快照并支持点选, 不可交互; 交互改用 "发送到 AutoJs6 运行". 预览服务多一种 `snapshot` 会话模式, 契约同一份 |
| D49 | 原样编辑: 无损 | XML 字面量以带位置的具体语法树保存: 空白, 注释, 属性顺序, 引号与 `{expr}` 片段全部保留; 编辑以定位替换写回, 未触碰的字节不变; 新插入的元素按相邻元素的缩进生成. 不采用 Compose 工程的 "解析 / 生成不动点" 规范化格式 |
| D50 | 预览的版本门禁 (2026-10-15, Q21) | 三级: 嵌入显示与布局预览 API 30+ (`SurfaceControlViewHost`); 交互预览 (放行指针) API 33+ (API 30 - 32 的弹出层会让宿主进程崩溃); 预览中的输入法 API 35+ 且以 `InputTransferToken` 构造嵌入窗口, API 33 - 34 的 `<input>` 在预览中提示并禁用输入; API 24 - 29 位图快照 (D48) |
| D51 | 预览运行时的可见性 (2026-10-15, Q20) | 维护者确认 P10.2 的实施形态 (建议 (b)): 预览运行时经 `ScriptEngineService.execute` 注册为 `UiPreviewScriptExecution`, 以客户端 `label` 出现在宿主运行列表, 宿主侧停止即释放会话 (`onSessionLost(stopped)`); 不新增旁路. 宿主 changelog 的多语言翻译由维护者后期统一处理 |
| D52 | XML 工程的逻辑生成范围 (2026-10-15, Q19) | 按建议 (b): 设计器只为选中元素一键追加 `ui.<id>.on('<event>', (...) => { // TODO });` 桩, 位置是该片段所属 `ui.layout(...)` 语句之后 (紧随已有的 `ui.<x>.on(...)` / `.click(...)` 语句, 保持添加顺序), 元素没有 `id` 时同一步补 `<tag>n`; 桩写入后即为不透明源码, 设计器不展示也不改写, 正文在脚本编辑器中写; 脚本已注册的同 id 同事件不再追加; 只能为以语句形式传给 `ui.layout` 的字面量 (直接或经一个变量) 追加, `return ui.layout(...)` 与表达式体箭头函数不提供. 不做 (c) 从绑定值面板新增顶层声明, 绑定值面板只做画布覆盖; 设计器从不改写已有源码 (D49 边界不变) |
| D53 | 文档页归属与命名 (2026-10-15, Q18) | 按建议: 指南页保持 `api/composeDesigner.md`, 标题改为 "3-Block Compose 设计器 (Compose 与 UI 工程)", 新增 "UI (XML) 工程" 一章, `api/ui.md` 加一处链接; 工程类型的用户可见名称为 "UI (XML) 工程" (新建对话框的 chip 写 "UI (XML)", 首页徽标 `UI`) |

由 D46 - D49 派生的硬约束:

- 插件进程仍只有插件 APK 自带的类 (D32): 不加载宿主 APK 的代码, 不反射宿主内部类; 与宿主的全部交互只经新契约 AAR (锁入 `locks/host-api-aars.lock`) 与系统 API (`SurfaceView` / `SurfacePackage` / `ParcelFileDescriptor`).
- 布局预览请求里不得包含可执行的用户代码: 送往宿主的是插件已把 `{expr}` 替换为求值结果 (或原文占位) 的 XML 字符串与 `{{}}` 绑定值的 JSON; 宿主对该请求不进入用户脚本.
- 交互预览只在显式开关下运行, 每个工程首次打开时确认, 设计器不可见时停止, 与 "运行" 共用宿主的脚本权限模型, 不扩大也不缩小.
- 预览服务只接受持有 PLUGIN 权限 (宿主签名级) 的调用方; 会话随客户端死亡释放 (`linkToDeath`); 全部输入按契约上限校验.
- 契约演进只追加 (方法追加在 AIDL 末尾, Bundle 加可选键), 以 `getContractVersion()` / `getContractHash()` 协商, 与 `typescript-parser-api` 同形.

---

## 2. 现状事实 (2026-10-15 调研, E0)

### 2.1 宿主 ui 链路

- `runtime/api/augment/ui/UI.kt`: `ui.layout(xml)` 要求全局 `activity` 为 `ScriptExecuteActivity` (`ensureActivity`), 以 `activity.window.decorView` 为父 inflate 后 `setContentView`; `ui.inflate` 在没有 Activity 时用 `ContextThemeWrapper(applicationContext, R.style.ScriptTheme)`; `toXMLString` 对 `org.mozilla.javascript.xmlimpl.XML` 调 `toXMLString()`, 对 DOM `Document` 用 Transformer, 其它取 `toString()`; TSX 布局经 `TypeScriptTsxRuntime.renderOrNull` 先转 XML.
- `core/ui/inflater/DynamicLayoutInflater.kt` (约 900 行) + `inflaters/` 108 个文件 (`JsXxxInflater` 创建 `Js*` 控件, `XxxInflater` 按 Android 类层次设置属性, `when (attrName)` 分支, 非声明式); 构造函数 `(ResourceParser, ScriptRuntime)`, 每个 inflater 持有 `scriptRuntime`; `LayoutInflaterDelegate` 提供 `beforeCreateView / afterCreateView(inflateContext, view, node, viewName, parent) / afterInflateView / beforeApplyAttribute` 等钩子, 其中 `node` 是转换后 XML 的 DOM 节点, 可用于把 View 对应回源元素.
- `core/ui/xml/XmlConverter.kt`: 简写标签与属性转换 (`vertical` / `horizontal` / `text` / `w` / `h` / `size` / `margin` / `align` 等); `<text>` / `<button>` / `<input>` 的文本内容变为 `android:text`; `XmlBareAttributes.normalize` 处理裸属性 (`<console global>`); `SecureXml.parse` 为 DOM 解析. 只遍历元素节点, 元素先序不变.
- 动态属性: `DynamicLayoutInflater.isDynamicValue` 以 `{{` / `}}` 判断; `UI.kt` 的 `bindValueForApplyingAttribute` 在 `ui.bindingContext` (默认脚本顶层作用域) 中逐段 `evalInContext`; `JsListView` 创建项视图时 `FLAG_IGNORES_DYNAMIC_ATTRS`, 绑定数据时把 `bindingContext` 换成项目对象并以 `FLAG_JUST_DYNAMIC_ATTRS` 重新应用.
- 执行: `ScriptEngineService.execute(ScriptSource, ExecutionConfig)`; ui 模式由 `ScriptExecuteActivity` 承载, `prepare()` 把 Activity 写入引擎 (`put("activity", this)`), `ActivityScriptExecution.createEngine` 创建引擎. `StringScriptSource` 可直接从文本构造脚本源.
- 对话框: `Dialogs.java` 从 `runtime.app.getCurrentActivity()` 取 Activity, 没有时走悬浮窗类型窗口.
- 已有的宿主对插件导出面: `OfficialPluginSettingsProvider` (provider), `MailTriggerReceiver` / `ThreeStoveAgentAttachRequestReceiver` (receiver), 均以 `android:permission="org.autojs.permission.PLUGIN"` 导出; 契约模块在 `plugin-api/<name>/` (AIDL + Kotlin 常量对象, 例如 `typescript-parser-api` 的 `ITypeScriptParserPlugin.aidl` + `TypeScriptParserContract.kt`, 树经调用方拥有的 fd 传回并校验长度与 SHA-256), 由 `settings.gradle.kts` 的 `pluginApi` 列表登记.

### 2.2 Rhino 分支的 E4X 行为 (已验证)

以 `C:/Users/wu2du/.jdks/openjdk-25.0.2` 运行宿主 `libs/org-mozilla-rhino-2_0_0-SNAPSHOT.jar` 的 shell:

```js
var msg = "hello";
var x = <vertical><text id="txt" text="{{msg}}" size="32sp" /><text text="a {msg} b"/>{msg}</vertical>;
// x.toXMLString() ->
// <vertical>
//   <text id="txt" size="32sp" text="{{msg}}"/>
//   <text text="a hello b"/>
//   "hello"
// </vertical>
```

以插件 `libs/org-mozilla-rhino-2_0_0-SNAPSHOT-slim.jar` 的 `Parser` (ES6, ideMode, 默认 `xmlAvailable = true`) 解析同一字面量, `XmlLiteral` 的片段为: `XmlString` `<vertical><text id="txt" text="{{msg}}" size="32sp" /><text text="a `, `XmlExpression(msg, isXmlAttribute = true)`, `XmlString` ` b"/>`, `XmlExpression(msg, isXmlAttribute = false)`, `XmlString` `<!-- c --></vertical>` (注释原样保留在 `XmlString` 内). 结论: `{{...}}` 是宿主在应用属性时处理的绑定文本, 对解析器只是普通字符; `{...}` 是 E4X 在构造字面量时求值的表达式, 解析器以 `XmlExpression` 给出位置与源码. `XmlLiteral.getLength()` 在该版本返回 1, 字面量范围要取最后一个片段的末尾. P10.1 补充 (shell 实测): 该分支在属性引号内不把单层 `{expr}` 当表达式 (`a="{msg}"` 与 `a="1 {msg} 2"` 运行时原样保留), 只有紧跟在含 `{{...}}` 的属性之后才会出现属性内表达式; 内容位置的 `{expr}` 照常求值, `{{msg}}` 在内容位置得到 `[object Object]`. 读取器不自行判断, 只按 Rhino 给出的片段处理.

### 2.3 Android 嵌入 API 级别 (SDK `android-37.0/data/api-versions.xml`)

| API | since |
| --- | --- |
| `SurfaceControlViewHost(Context, Display, IBinder hostToken)` | 30 (35 起弃用, 35 新增 `InputTransferToken` 重载) |
| `SurfaceControlViewHost.setView(View, w, h)` / `relayout(w, h)` / `getSurfacePackage()` / `release()` | 30 |
| `SurfaceControlViewHost.SurfacePackage` (Parcelable) | 30; `notifyConfigurationChanged` / `notifyDetachedFromWindow` 33; `getSurfaceControl` 34 |
| `SurfaceView.getHostToken()` | 30 (35 弃用, 改 `getInputTransferToken()`) |
| `SurfaceView.setChildSurfacePackage(SurfacePackage)` | 30; `clearChildSurfacePackage()` 36 |
| `SurfaceControlViewHost.LayoutParams(w, h, focusable)` | 37.0 |

本插件与宿主 `minSdk` 均为 24, 兼容矩阵 (D28) 含 API 24 AVD 与 API 28 G8441, 因此需要 D48 的快照模式.

### 2.4 属性与标签目录的来源

- 文档站 `AutoJs6-Documentation/api/ui.md` "内置标签" 表约 50 行 (含别名组, 如 `linear` / `horizontal`, `button` / `btn`, `input` / `edittext`, `image` / `img`), 标签到实际视图类的映射, 以及 "完整类名作为标签" 与 AndroidX / Material 短类名规则.
- `api/uiAttributes.md` 361 行属性表, 按 Android 视图继承关系分节 (View, ViewGroup, TextView, 输入框附加, ImageView, ProgressBar / SeekBar, 各布局控件, 选择与输入控件, 媒体与专用控件, List / Grid, 创建阶段专用, AndroidX / Material 短类名), 每行含别名, 类型与说明; 另有值格式 (布尔, 数字, 尺寸, 多值, Gravity, 颜色, Drawable, ID / 字符串 / 样式, 动画资源) 与 "AutoJs6 简写转换" 表.
- 宿主 inflater 的 `when (attrName)` 不可直接导出为目录; 文档是唯一结构化来源, 因此目录由文档生成 (附录 C), 并记录来源提交.

### 2.5 样例

宿主 `app/src/main/assets-app/sample/` 中 63 个脚本含 XML 布局, 52 个带 `'ui';` 指令; 典型形态: `ui.layout(<frame>...</frame>)` 位于函数体内且一个文件有多次 `ui.layout` (布局/登录界面.js 的登录页与注册页), `<text>用户名</text>` 文本内容, `w="*"` / `h="auto"` 简写, `<list>` 项模板中的 `{{name}}`, `ui.<id>.on('click', ...)` 事件, 自定义控件 (`ui.registerWidget`, 布局模板), 控制台控件 `<console global>`. 这 52 个脚本是 P10.1 往返测试与 P10.3 预览冒烟的语料 (与 `sync_examples.py` 一样以明确宿主路径读取, 构建不依赖宿主目录).

### 2.6 插件现状中可复用的部分

`DesignerActivity` 的页面骨架 (画布 + 大纲 / 状态 / 属性 / 控制台标签页, 横屏分栏), `EditHistory` / `EditCommand` 的命令模式, `drafts/` 草稿与恢复, `DocumentSource` / `DocumentIo` 的本地与外部文档读写, `ProjectStore` 的工程列表 / 导入 / 模板, `ScriptLauncher` 的四个去向, `SourceSync` 的来源同步, `designer/sandbox` 的 Rhino 沙箱 (求值顶层声明), `StatePanel` 的预览值概念, `ConsolePanel`, `TsxParserClient` 的服务绑定与 fd 传输模式, `HostApiAarLockTest` / `RhinoImportGuardTest` 等守卫. 文档模型, 解析 / 写回, 校验, 画布与属性抽屉的内容需要为 XML 新写 (3.3 节).

---

## 3. 目标架构

### 3.1 数据流

```text
                 插件进程 (设计器)                                    宿主进程 (AutoJs6)
+----------------------------------------------+        +------------------------------------------+
| 脚本文件 (.js, "ui" 模式)                     |        | UiPreviewService (PLUGIN 权限, Binder)   |
|   |  UiScriptReader (Rhino Parser, 只解析)     |        |   会话 = SurfaceControlViewHost (API 30+) |
|   v                                          |        |          或 离屏 View 树 (snapshot)       |
| UiDocument (不透明段 + XML 片段 CST)           |        |   PreviewRuntime: 真实 ScriptRuntime +     |
|   |  EditCommand -> EditHistory (撤销 / 重做)  |        |     DynamicLayoutInflater + XmlConverter   |
|   |  UiScriptWriter (定位替换, 无损)           |        |     合成脚本 "ui"; ui.layout(xml)          |
|   v                                          |        |     bindingContext = 绑定值 JSON           |
| 大纲 / 属性抽屉 / 组件面板 / 问题列表           |        |   边界: 元素先序号 -> View 边界            |
|                                              |        |   (1.7.0) 真实运行整份脚本, 控制台回传      |
| 预览: XmlPreviewFrames                       |        +------------------------------------------+
|   {expr} / {{expr}} -> 沙箱求值 (API 26+)      |  Binder  ^   |  SurfacePackage / 位图 fd / 边界 JSON
|   -> xml 字符串 + 绑定值 JSON                  | -------> |   v
| UiPreviewClient -> SurfaceView 画布            | <-------  (触摸由系统直接路由到宿主 View 树)
|   选中框 / 拖放 overlay (编辑模式拦截指针)      |
+----------------------------------------------+
```

- 编辑模式: overlay 独占指针, 点选按宿主回传的边界命中元素 (先序号对应 CST 元素), 长按拖至合法容器; 画布只显示, 不响应嵌入树的交互.
- 布局预览: 文档或预览值变化后 400 ms 内把 (xml, 绑定值) 发给宿主 (与沙箱重跑同节拍), 宿主重新 inflate 并回传边界; inflate 失败回传错误与元素先序号, 画布保留上一帧并在问题列表定位.
- 交互预览 (1.7.0): 开关打开后 overlay 放行指针, 宿主以嵌入树为 `ui` 承载真实运行整份脚本; 控制台 / 错误 / 退出经回调进入设计器控制台标签页; 关闭开关或设计器不可见即停止.
- 快照模式 (API 24 - 29): 同一客户端, 会话模式为 `snapshot`; 画布显示位图, 点选与边界同上, 不放行指针.
- 表面与指针 (P10.0 结论): 画布的 `SurfaceView` 必须 `setZOrderOnTop(true)` 才能让系统把触摸路由到嵌入树, 插件的 Compose 画不到表面之上, 因此编辑模式的指针拦截, 命中与选中框由宿主根容器完成并经回调回传 (`setPointerMode` / `setHighlight` / `onTap`), 插件只保留画布外的 chrome; 画布缩放在插件侧缩放 SurfaceView (宿主按 1.5 倍密度重排时 AutoJs6 的尺寸解析不变), 命中按缩放换算.
- 会话与表面 (P10.0 结论): 设计器进入后台时 `surfaceDestroyed`, 会话保留; 回到前台 `surfaceCreated` 后向宿主索取新的 `SurfacePackage` (`reattach`) 再 `setChildSurfacePackage`; 旋转与分栏只走 `resize`.

### 3.2 进程与类加载边界

插件进程不加载宿主代码; 宿主进程不加载插件渲染器 (Compose 渲染器仍按 D10 由宿主按需装载, 与本阶段无关). 新契约 AAR `ui-preview-api.aar` 随插件打包 (`implementation`, 完整 keep, 与 `typescript-parser-api` 相同), 宿主 app 以 `implementation` 打包同一模块. 交互预览中运行的脚本是宿主普通脚本执行 (出现在运行列表, 受 `PluginTrustManager` 之外的既有脚本权限模型管理), 只是 `ui` 的承载换成了嵌入树.

### 3.3 插件包结构 (新增)

```text
designer/
|-- model/     UiDocument (Segment.Opaque + Segment.Layout), XmlNode (Element / Text / Comment / Expr), XmlAttr (name, quote, parts: Text | Binding{{}} | Expr{}), XmlSpan (源码位置), UiEditCommand (SetAttr / RemoveAttr / InsertElement / RemoveElement / MoveElement / SetTag / SetText), UiValidator, UiCatalog (由 resources/designer-ui/catalog.json 加载), UiTemplates (可插入标签的默认属性)
|-- parse/     UiScriptReader (AstRoot -> XmlLiteral 片段 -> CST; 画布根 = ui.layout 的参数), XmlCst (带位置的容错 XML 词法层), DocumentReader 按内容分派 (compose / ui)
|-- codegen/   UiScriptWriter (按 CST span 定位替换; 新节点按相邻缩进生成), DocumentWriter 分派
|-- preview/   XmlPreviewFrames ({expr} / {{expr}} 求值与占位, 元素先序号表), UiPreviewClient (绑定 / 会话 / 边界 / 快照), XmlCanvas (AndroidView(SurfaceView) + Compose overlay, 快照位图), 复用 DropPlacements 的方向表 (vertical / horizontal / frame / relative / scroll / card / list)
|-- sandbox/   DeclarationEvaluator (沙箱对顶层 var / let / const 字面量类初始化式求值, 供 {{}} / {expr} 预览), 现有 SandboxEngine 复用
`-- ui/        DesignerActivity 按文档类型切换画布与面板, UiInspectorPanel (标签 / 属性三态 / 文本内容 / 裸属性), UiPalette (按文档 ui.md 分类), BindingsPanel (复用 StatePanel 的预览值模式)
```

### 3.4 宿主包结构 (新增)

```text
plugin-api/ui-preview-api/        IUiPreviewService.aidl, IUiPreviewCallback.aidl, UiPreviewContract.kt (常量 / 键 / 错误码 / 上限), consumer-rules.pro, UiPreviewContractTest
app/.../core/ui/UiContentHost.kt  ui.layout 内容承载接口 (inflaterContext / contentParent / contentRoot / hostWindow / showContent, 窗口级函数忽略钩子, 绑定失败钩子, 强制停止钩子), attached() = 引擎标签, of() = 引擎标签 ?: 全局 activity
app/.../core/ui/preview/          UiPreviewService (Service, exported, PLUGIN 权限, action org.autojs.plugin.ui.PREVIEW, Binder 线程校验 + 主线程限时执行), UiPreviewSession (实现 UiContentHost: SCVH 或离屏根, 配置覆盖, 边界 / 点击 / 高亮, 快照绘制), UiPreviewScriptExecution (经 ScriptEngineService 注册的主线程引擎, Q20 (b)), UiPreviewTrackingDelegate (元素先序号, 失败定位, 快照占位替换), UiPreviewRootView, UiPreviewPlaceholderView, UiPreviewRequests / UiPreviewBounds / UiPreviewFailures (纯 Kotlin, JVM 测试), UiPreviewScript ("ui"; 常量)
app/.../runtime/api/augment/ui/   UI.kt: layout / layoutFile / setContentView / inflate 经 ensureUiHost 取承载; 9 个窗口级函数经 ensureWindowActivity 在预览承载下空操作并经控制台警告一次; {{ }} 求值失败经 UiContentHost.onBindingFailed 决定保留原文还是照旧抛出
app/.../execution/                ScriptExecuteActivity 实现 UiContentHost; ExecutionConfig.uiContentHost (宿主持有, 不参与 Parcel); ScriptEngineService.execute 先按它分派; LoopBasedJavaScriptEngine.forceStop 通知承载
```

### 3.5 残余偏差清单 (与 "在 AutoJs6 中运行" 相比, 文档与预览界面须明示)

| 范围 | 预览中的行为 |
| --- | --- |
| `activity` 全局 | 布局预览: 不存在 (合成脚本无用户代码). 交互预览: `undefined`, 直接访问 `activity.*` 的脚本抛错并显示在控制台; `ui.finish` / `keepScreenOn` / `statusBarColor` 等窗口级 API 为空操作并打印一条警告 |
| 生命周期事件 | `ui.emitter` 只发 `create`; 无 `resume` / `pause` / `back_pressed` / `activity_result` |
| 对话框 | `dialogs.*` 没有 Activity 时走悬浮窗类型窗口 (需宿主悬浮窗权限; HyperOS 等须由用户在系统设置授予, adb appops 不算), 显示在设计器之上而不是画布内 (spike: Redmi / API 33 模拟器显示, 小米平板 HyperOS 被拦截) |
| 弹出层 / 输入法 | Spinner 下拉与 Toolbar 菜单 (PopupWindow) 在 API 33+ 正常, API 30 - 32 会让宿主进程崩溃 (`BadTokenException`), 因此交互预览要求 API 33+, 30 - 32 只做不放行指针的布局预览; `<input>` 的输入法只在 API 35+ (以 `InputTransferToken` 构造嵌入窗口) 可用, API 33 - 34 可获焦点但键盘不弹, 预览中提示; 快照模式不可交互 |
| 触摸与遮盖 (P10.6) | 指针只在 `SurfaceView` 置顶时到达嵌入树, 置顶表面盖住设计器窗口叠在画布上的一切: 交互期间没有选中 / 拖拽 overlay, 开关与运行状态放在画布上方一行, 缩放仍经表面缩放生效 |
| 死循环与主线程 (P10.6) | 脚本体与处理函数在宿主主线程运行 (与真实 ui 运行相同): `while (true)` 之类的忙等会卡住宿主主线程, `stopScript` 也要等它让出, 与 `sandbox` 的指令上限不同; 定时器与事件驱动的脚本可随时停止 |
| 窗口与屏幕 | 画布尺寸即逻辑屏幕 (与 compose 画布一致), 不是设备全屏; 无状态栏 / 导航栏 inset; 主题为 `ScriptTheme`, 夜间模式与语言跟随宿主配置 |
| 绑定与数据 | 布局预览: `{{expr}}` 只对顶层字面量类声明求值, 其它显示原文并提示; `<list>` 项模板显示一项占位 (无数据源); 自定义控件 (`ui.registerWidget`) 显示为未知标签占位. 交互预览全部生效 |
| 副作用 | 布局预览无; 交互预览与运行相同 (文件, 网络, 无障碍, 悬浮窗均按脚本权限真实发生) |
| API 24 - 29 | 仅快照: WebView / SurfaceView / TextureView / VideoView 等需窗口的控件为占位矩形 |

---

## 4. 范围与非目标

范围: `.js` 文件中以 E4X 字面量写出的 `ui.layout(...)` 布局 (含绑定到变量后传入, 多个 `ui.layout`, `ui.inflate` 的片段); AutoJs6 简写标签与属性 (文档目录), 完整类名标签 (以 View / ViewGroup 属性校验并提示), `{{}}` 绑定, `{}` E4X 表达式 (只读), 文本内容, 注释与裸属性; 嵌入预览 (API 30+) 与快照预览 (API 24 - 29); 交互预览 (1.7.0).

非目标 (本阶段不做, 另立条目再议): `ui.layoutFile` 的外部 XML 文件与 `useAndroidLayout(true)` 的原生命名空间 XML (以只读片段显示); `.tsx` 中的 ui 模式布局 (classic TSX 经 TypeScript Engine 转 XML, 需另一条解析路径); `<compose>` 容器内 Compose 内容的联动预览 (容器显示为占位); 为 XML 工程生成事件处理函数或自动生成 `ui.<id>.on(...)` 桩 (D37 的对应物, 待 Q19); 在宿主编辑器中预览; 低于 API 24.

---

## 5. 阶段总览

| 阶段 | 内容 | 落点 | 目标版本 | 依赖 |
| --- | --- | --- | --- | --- |
| P10.0 | 嵌入与快照 spike: 在 AVD_API_30 / API 24 实例与一台真机上验证 SCVH 的触摸, 弹出层, 输入法, 密度缩放, 位图快照与边界回传; 结论回填本文 | 宿主 (spike 分支) + 插件 (临时测试页) | 无发布 | 无发布 (完成, 2026-10-15) | 无 |
| P10.1 | XML 文档模型, 容错词法层, 读取 / 无损写回, 命令, 校验与目录生成器 | 插件 | 1.6.0 | 无 |
| P10.2 | 预览契约 v1, 宿主预览服务 (布局预览: 嵌入 + 快照), `ui` 模块的承载抽象 | 宿主 | 宿主构建 (提交号待回填) | P10.0 |
| P10.3 | 预览客户端, XML 画布 (SurfaceView + overlay / 快照), 点选与拖放, 问题定位 | 插件 | 1.6.0 | P10.1, P10.2 |
| P10.4 | 编辑器集成: 工程类型与模板, 大纲 / 属性抽屉 / 组件面板 / 绑定值面板, 存储, 草稿, 去向, 首页与新建 | 插件 | 1.6.0 | P10.1, P10.3 |
| P10.5 | 文档站, README, changelog, AGENTS 修订, 1.6.0 本地 gate 与发布 | 文档 + 发布 | 1.6.0 | P10.4 |
| P10.6 | 交互预览: 契约追加 `runScript` 与控制台回调, 宿主真实运行整份脚本至嵌入树, 插件开关 / 确认 / 控制台 | 宿主 + 插件 | 1.7.0 | P10.5 |
| P10.7 | 交互预览的文档, changelog, 1.7.0 gate 与发布 | 文档 + 发布 | 1.7.0 | P10.6 |

版本切分理由: 1.6.0 先交付 "编辑 + 布局预览", 它不运行用户代码, 安全面与现状一致, 宿主改动限于新增服务与 `ui` 模块的承载抽象; 1.7.0 的交互预览要让宿主以嵌入树承载一次真实执行, 涉及 `ScriptExecuteActivity` 之外的第二种 ui 承载与控制台回传, 单独成版便于回退. 低版本快照放在 1.6.0 (服务多一种模式, 不另开版本). 全局 `REQUIRED_HOST_VERSION` 保持 5322: XML 预览按 "宿主已安装且 `org.autojs.plugin.ui.PREVIEW` 可解析且契约版本匹配" 判定, 否则只编辑并提示所需宿主版本 (与 TSX 需要 TypeScript Engine 0.7.0 的处理同形).

---

## 6. 条目

### P10.0 嵌入与快照 spike (无发布)

- [x] (宿主) spike 服务: 临时 `Service` (PLUGIN 权限) 接收 `hostToken` / `displayId` / 尺寸, 用 `SurfaceControlViewHost` 承载一个以 `ContextThemeWrapper(appContext, R.style.ScriptTheme)` 创建的 `FrameLayout`, 以合成脚本 `"ui"; ui.layout(xml)` 经真实 `ScriptRuntime` inflate 一段含 `<input>`, `<spinner>`, `<toolbar>` 菜单, `<list>`, `<webview>`, `<button>` 的布局, 返回 `SurfacePackage`.
  证据 (2026-10-15): 宿主分支 `spike/ui-preview-p10` (`UiPreviewHost` 承载接口 + `UI.kt` 的 `layout` / `inflate` 分流约 30 行, Messenger 服务 `UiPreviewSpikeService`); 脚本以主线程 `LoopBasedJavaScriptEngine.execute` 运行 (非主线程时 WebView 调用抛错, 列表绑定同样), 根容器套 `ScriptTheme` 的 `windowBackground`, 边界经包装的 `afterCreateView` 以 DOM 先序号回传. 详见 `docs/dev/p10-spike-evidence.md`.
- [x] (插件) 临时测试页 (仅 debug source set): `AndroidView(SurfaceView)` + `setChildSurfacePackage`, 上层 Compose overlay 可切换拦截 / 放行指针; 记录 `surfaceCreated / Changed / Destroyed` 与会话的对应.
  证据 (2026-10-15): `app/src/debug/.../spike/UiPreviewSpikeActivity.kt` (Messenger 客户端, `AndroidView(SurfaceView)` + overlay, 模式 / 拦截 / 置顶 / 缩放 / 密度 / 夜间 / 视口开关, 快照显示); `surfaceCreated / Changed / Destroyed` 分别对应 会话附着 (首次) 或重新索取 `SurfacePackage` (恢复), `resize`, 保留会话.
- [x] (测试) 在 AVD_API_30, API 33 Redmi, API 35 Xiaomi Pad 逐项记录: 按钮点击到达宿主; `<input>` 获得焦点并弹出输入法; Spinner 下拉与 Toolbar 溢出菜单能否弹出 (弹出位置, 是否被裁切); `dialogs.alert` 的窗口归属; 画布缩放两种做法 (SurfaceView `scaleX / scaleY` vs 宿主以 `createConfigurationContext(densityDpi * zoom)` 重排) 的视觉与命中结果; 夜间模式切换后 `notifyConfigurationChanged` (API 33+) 的效果; 设计器进入后台 / 旋转 / 被杀时宿主会话的释放.
  证据 (2026-10-15, AVD_API_30 / AVD_API_33 / Redmi API 33 / Xiaomi Pad API 35 / AVD_API_37): 触摸只在 `SurfaceView.setZOrderOnTop(true)` 时到达嵌入树; 输入法只在 API 35+ 以 `InputTransferToken` 构造时弹出 (API 30 / 33 输入框可获焦点但 `canReceiveKeys=false`); Spinner 弹出层 API 33 / 35 / 37 正常, API 30 让宿主进程 `BadTokenException` 崩溃; `dialogs.alert` 为宿主悬浮窗类型窗口, HyperOS 需用户在系统设置授予; 缩放: SurfaceView 缩放生效, 宿主改密度重排尺寸不变; 夜间与系统深色切换无崩溃无变化 (`ScriptTheme` 只有浅色); 后台后恢复须重新索取 `SurfacePackage`, 旋转走 `resize`, 杀进程后会话释放. 结果表见 `docs/dev/p10-spike-evidence.md`.
- [x] (测试) 快照模式: 在 AVD_API_24 实例 (Nexus 5 尺寸) 与 G8441 (API 28) 上, 宿主对离屏 View 树 `measure(EXACTLY) / layout / draw` 到 ARGB_8888 位图并写入调用方 fd (原始像素 + 边界 JSON), 记录 `<list>` 有无项目, `<webview>` / `<video>` 的占位, 一张 1080 x 1920 位图的生成与传输耗时.
  证据 (2026-10-15): AVD_API_24 1032 x 448 (1.85 MB 原始像素) 宿主绘制 44 ms, 往返 64 ms; G8441 (API 28, 横屏 1152 x 90) 绘制 131 ms, 往返 139 ms; 位图经调用方 fd 写入并以 SHA-256 核对, 边界同嵌入模式; `<list>` 显示模板项 (绑定报错见证据第 3 节), `<webview>` 占位未单独验证 (视口不足).
- [x] (宿主 + 插件) 结论回填本文 3.5 节与附录 B 的上限 (最大会话数, 最大尺寸, 快照像素上限, 边界 JSON 上限), 决定缩放做法与 IME 可用的最低 API; 若 SCVH 在 API 30 - 32 上不能承载输入法, 嵌入模式在这些 API 上对 `<input>` 显示 "需要 Android 13 才能在预览中输入" 的提示而不降级为快照.
  证据 (2026-10-15): 本文 3.1 / 3.5 / 附录 B / Q21 已按 spike 结论修订 (表面置顶, 宿主侧拦截与高亮, 重新附着, 版本门禁: 嵌入显示 30+, 交互 33+, 输入法 35+, 快照 24 - 29); 缩放做法定为插件侧 SurfaceView 缩放.

### P10.1 XML 文档模型与无损往返 (插件, 1.6.0)

- [x] (插件) `XmlCst`: 带位置的容错 XML 词法层 (元素, 属性 (名, 引号, 值片段), 文本, 注释, CDATA, 处理指令, 自闭合, 裸属性 (按宿主 `XmlBareAttributes` 规则), `{expr}` 占位 (由 `XmlExpression` 片段注入, 记录源码与是否在属性内), `{{...}}` 绑定片段 (属性值内的文本子串, 不跨片段)); 不解析实体以外的任何 XML 命名空间语义; 错误以 `DesignerParseError(line, column)` 定位到脚本文件坐标.
  证据 (2026-10-15, build 150): `designer/parse/XmlCst.kt`: 元素, 属性 (名, 引号, 值片段三态: 文本 / `{{}}` 绑定 / Rhino 报告的 `{expr}`, source 含前导空白), 文本, 注释, CDATA, 处理指令, 自闭合与 tail, 裸属性, 重复属性与不匹配的结束标签报错; 不解码实体; 错误带字面量内偏移, 读取器换算为脚本行列. JVM `XmlCstTest` 7 项.
- [x] (插件) `UiScriptReader`: 复用 `ScriptReader.parseAst` 的 `CompilerEnvirons`; 文档类型判定 = 首个有效语句为 `"ui"` / `'ui'` 指令 (注释与空行可在前) 且存在 `ui.layout(...)` 调用且没有 `compose.mount / floaty / dialog`; 收集全部 `XmlLiteral` 为 `Segment.Layout` (源码范围 = 首片段起点到末片段终点, 其余源码为 `Segment.Opaque`); 画布根 = 源码顺序第一个作为 `ui.layout` 直接参数或经单次变量绑定 (`var / let / const x = <...>` 后 `ui.layout(x)`) 传入的字面量, 其它字面量为可切换的 "片段"; 不是以上形态的脚本 (无 `ui.layout`, 参数为函数调用结果或字符串) 以 `DesignerParseError` 拒绝并定位, 不静默.
  证据 (2026-10-15, build 150): `designer/parse/UiScriptReader.kt` (自带与 `ScriptReader` 相同配置的 `parseAst`): 指令 = 首个语句为 `"ui"` 字符串表达式 (注释可在前); `visitAll` 收集全部 `XmlLiteral` (范围 = 首片段起点到末片段终点, `XmlExpression` 片段作为表达式范围, `isXmlAttribute` 区分属性内外), `ui.layout` 调用与 `ui.registerWidget` 的名字; 画布根 = 源码顺序第一个 `ui.layout` 的字面量参数, 或单次以字面量初始化且未被重新赋值的变量; 其它参数形态, 无指令, 无 `ui.layout`, XML 列表, 字面量语法问题均以 `DesignerParseError` 定位 (嵌套字面量由 Rhino 自身拒绝). `looksLikeUiScript` 供 P10.4 的文档分派. JVM `UiScriptReaderErrorTest` 7 项.
- [x] (插件) `UiDocument` / `XmlNode` / `XmlAttr`: 不可变模型, 元素带文档内 id 与先序号 (先序号只数元素, 与宿主转换后 DOM 的元素顺序一致), 属性值三态 (纯文本 / 含 `{{}}` 绑定 / 含 `{expr}`), 文本子节点, 注释保留; `<text>` / `<button>` / `<input>` 的文本内容在模型中既是子节点也以 "文本内容" 属性视图呈现 (宿主 `XmlConverter.handleText` 规则).
  证据 (2026-10-15, build 150): `designer/model/XmlModel.kt`: `UiDocument` (段, 根片段序号, nextId, 注册的 widget 名), `UiSegment.Opaque / Layout`, `XmlFragment` (用途 LAYOUT / OTHER, 绑定变量, 调用偏移), `XmlNode.Element` (id, tag, attrs, children, selfClosing, startTagTail, startTagSource, endTagSource) 与 Text / Comment / CData / Instruction / Expr, `XmlAttr` 与 `AttrPart`, `XmlEntities` (解码与属性 / 文本编码); 先序号即 `descendants()` 顺序 (与宿主转换后 DOM 一致), `mapElement` / `allocate` / `parentOf` / `elementIndexOf` 与 compose 模型同形; `<text>Hello</text>` 的文本内容经 `textContent` 读取.
- [x] (插件) `UiScriptWriter` (D49): 未变化的节点按 span 原样输出; 变化的开始标签重排属性时保持原顺序与原引号 (`"` 内的 `"` 写 `&quot;`, 与 `XmlConverter.escapeXml` 一致), 新属性追加在末尾 (单属性元素跟随同一行, 多行元素跟随最后一个属性的缩进); 插入元素按前一个兄弟 (无则父元素 + 4 空格) 的缩进生成, 自闭合; 删除元素连带其前导空白; 片段外的脚本文本字节不变. 性质测试: 读取后立即写回 = 原字节; 任意一步编辑只改动触碰元素所在行的范围.
  证据 (2026-10-15, build 150): `designer/codegen/UiScriptWriter.kt`: 段按原文拼接; 元素 = `startTagSource` 或重生成的开始标签 (`<tag` + 各属性的 source 或 ` name="value"` + 原 tail + `>`) + 子节点 + `endTagSource` 或 `</tag>`; 引号沿用原属性, 值含该引号时换另一种. 性质 (JVM `UiScriptRoundTripTest` 8 项): 仓库内拷贝的 12 个宿主示例 (`app/src/test/resources/ui-samples/`, 来源见 SOURCES.txt) 读取后写回字节相同并重读相等; `AUTOJS6_HOST_DIR` 指向宿主 (master `6c151c900e`) 时另读取其 46 个以字面量调用 `ui.layout` 的 ui 示例 (52 个 ui 脚本中其余 6 个经变量, 字符串或调用传入), 同样字节相同; 12 个示例逐元素 SetAttr 后只有该元素的开始标签改变.
- [x] (插件) `UiEditCommand` 进入现有 `EditHistory`: SetAttr (同一属性连续编辑合并), RemoveAttr, SetText, SetTag (保留属性, 校验目录), InsertElement (模板), RemoveElement, MoveElement (BEFORE / INTO / AFTER), SetActiveFragment (不进历史); 撤销逐字段还原, 无变化不记录.
  证据 (2026-10-15, build 150): `designer/model/UiEditCommand.kt` (独立的 `UiEditHistory`, 与 `EditHistory` 同语义, 合并留到 P10.4): SetAttr (同属性连续编辑合并, 保留引号与其它属性的原文, 新属性追加在末尾, `bare` 写裸属性, null 删除), SetTag, SetText (只对无子元素的元素, 自闭合展开), InsertElement (无来源的子树按 4 空格规范化, 按同级邻居或父元素 + 4 空格缩进插入, 自闭合父元素展开为开闭标签), RemoveElement (连带前导的空白行, 根不可删), MoveElement (移除时去掉旧缩进, 插入时加新缩进, 不允许移入自身子树). JVM `UiEditHistoryTest` 6 项; 往返测试中 12 个示例各做插入 / 撤销 / 重做, 登录示例做跨层移动.
- [x] (插件) `UiCatalog` 与生成器 `.python/generate_ui_catalog.py --docs <AutoJs6-Documentation>` (附录 C): 标签表 (标签, 别名, 视图类, 分类, 可容纳子节点, 排布方向), 属性表 (名, 别名, 所属视图类, 类型, 说明), 简写转换表, 视图类继承链 (脚本内手工表); 输出 `app/src/main/resources/designer-ui/catalog.json` 并记录文档站提交; `--check` 校验已提交文件; JVM `UiCatalogTest` 守卫 JSON 结构, 别名唯一, 每个标签都有继承链.
  证据 (2026-10-15, build 150): `.python/generate_ui_catalog.py --docs <AutoJs6-Documentation> [--check]` 生成 `app/src/main/resources/designer-ui/catalog.json` (schemaVersion 1, 文档站提交 `0e9a0e9`): 49 个标签 (`ui.md` 内置标签表, 类名归一为简单名), 40 个类 440 个属性 (`uiAttributes.md` 的表格与列表; 节标题与 "`tag` 另外支持" 行决定所属类; 类型文本归一为 boolean / int / float / dimension / color / drawable / string / id / gravity / enum / other, 另存原文与三级标题作为分组), 32 条简写 ("对应 `layout_...`" 行推导为 `layout_` + 简写), 5 条创建期属性, 7 个类的明确不支持列表, gravity 词表与尺寸单位; 继承链 `CHAINS` 与面板分类 `TAG_META` 是脚本内手工表. `designer/model/UiCatalog.kt` 以 `JsonText` 读取资源 (`attrsFor` 沿链合并, `attr` 接受别名与简写, `unsupported`, `creationOnly`). JVM `UiCatalogTest` 5 项; 目录 JSON 经 `ApplicationTextPunctuationTest` 扫描.
- [x] (插件) `UiValidator`: 未知标签 (完整类名允许并提示 "按 View / ViewGroup 属性校验"), 未知属性 (警告, 宿主会忽略), 值格式 (布尔 / 数字 / 尺寸 `16`, `16sp`, `*`, `auto`, `match_parent`; 颜色 `#RRGGBB` / `#AARRGGBB` / 命名色 (警告); Gravity 词表; 多值尺寸), RelativeLayout 规则只在 `relative` 下有效, `<list>` / `<grid>` 第一个元素子节点是模板, 重复 `id`, 绑定表达式为空, `{expr}` 不校验.
  证据 (2026-10-15, build 150): `designer/model/UiValidator.kt`: 未知标签 (错误; 完整类名按简单名查链, 查不到按 View / ViewGroup 校验并警告; 脚本 `ui.registerWidget` 注册的标签跳过), 未知属性 (警告, 宿主忽略), 明确不支持 (错误), 值格式 (布尔只认 true / false, 整数, 浮点, 尺寸列表含 `*` / `auto` / 百分比 / `?attr`, 颜色 (十六进制 / `@color` / 主题引用通过, 命名色警告), gravity 词表, 枚举值, id), RelativeLayout 分组属性不在 relative 下 (警告), 空 `{{}}` (错误), list / grid 必须恰好一个模板子元素 (错误), 非容器含子元素 (警告), 全文档重复 id (错误); 含命名空间属性的元素不校验属性 (Android 原生布局). 12 个示例 0 错误 (待办示例的 `blue-grey-900` 等命名色为警告). JVM `UiValidatorTest` 4 项.
- [x] (测试) JVM: `UiScriptRoundTripTest` (宿主 52 个 ui 模式示例 (以 `--host` 明确路径读取, 缺省时用仓库内拷贝的 12 个代表性脚本) 读取后写回字节相同; 每个脚本对每个元素做 SetAttr / InsertElement / RemoveElement / MoveElement 后再次读取得到相等模型且其它行不变), `XmlCstTest` (注释, CDATA, 裸属性, 单双引号, `{expr}` 在属性内外, `{{}}` 多段, 实体, 未闭合标签的行列), `UiScriptReaderErrorTest` (无指令, 无 `ui.layout`, 参数为调用, 字面量语法错误), `UiEditHistoryTest`, `UiValidatorTest` (52 个示例 0 错误, 已知警告清单), `UiCatalogTest`.
  证据 (2026-10-15, build 150): 六个测试类共 37 项 (`XmlCstTest` 7, `UiScriptReaderErrorTest` 7, `UiScriptRoundTripTest` 8, `UiEditHistoryTest` 6, `UiCatalogTest` 5, `UiValidatorTest` 4), 全仓 JVM 210 项通过 (`AUTOJS6_HOST_DIR` 设置时宿主 46 个示例的往返也通过); `assembleDebug` 通过. 未执行: lint 与 release 包 (随 P10.5 gate); 设备测试本节无 (纯模型).

### P10.2 预览契约 v1 与宿主预览服务 (宿主, 布局预览)

- [x] (宿主) 契约模块 `plugin-api/ui-preview-api` (附录 B): `IUiPreviewService.aidl` (`getContractVersion`, `getContractHash`, `getPreviewInfo`, `createSession`, `setLayout`, `resize`, `release`, `reattach`, `setPointerMode`, `setHighlight`; 事务顺序冻结, 只追加), `IUiPreviewCallback.aidl` (oneway: `onBounds`, `onLayoutFailed`, `onSessionLost`, `onTap`), `UiPreviewContract.kt` (action `org.autojs.plugin.ui.PREVIEW`, 版本 1, HASH_INPUT / HASH, Bundle 键, 错误码, 上限), consumer-rules, JVM `UiPreviewContractTest` (哈希与键集合), 登记到 `settings.gradle.kts` 的 `pluginApi`; 文档 `docs/dev/ui-preview-plugin-protocol-v1.md`.
  证据 (2026-10-15): 宿主 `8c50a8fabc` (`feat(plugin-api): add the ui-mode layout preview contract v1`): 模块 + `settings.gradle.kts` + `app/build.gradle.kts` (`implementation`) + CI `android.yml` 跑 `:plugin-api:ui-preview-api:testDebugUnitTest`; `HASH` = `680f41b5459648ff07bd1d3d9684c5279d9398b78f61c0307862f50f8dcac0b6`; `UiPreviewContractTest` 8 项 (哈希, 两个接口的事务顺序与 AIDL 文件一致, 回调 oneway, action / 权限 / 错误码前缀, D50 等级与指针模式, 上限自洽, JSON 成员与 Bundle 键不重复) 通过; 协议文档 `docs/dev/ui-preview-plugin-protocol-v1.md`.
- [x] (宿主) `UiContentHost` 抽象: `ScriptExecuteActivity` 实现; `UI.kt` 的 `layout / layoutFile / setContentView / inflate` 经 `ensureUiHost` 取承载 (Activity 或预览承载), 窗口级函数在非 Activity 承载下空操作并经控制台警告一次; 既有行为与消息在 Activity 承载下逐字不变 (宿主 `UI` 相关测试回归).
  证据 (2026-10-15): 宿主 `306a8f7834` (`feat(ui): host the ui-mode layout preview service for the 3-Block Compose designer`): `core/ui/UiContentHost.kt` (`inflaterContext` / `contentParent` / `contentRoot` / `hostWindow` / `showContent`, `onWindowFunctionIgnored` / `onBindingFailed` / `onEngineForceStop` 钩子; `attached()` 读引擎标签 `ui.content.host`, `of()` 回退到全局 `activity`), `ScriptExecuteActivity : UiContentHost` (`showContent` = `setContentView`; 接口方法不能叫 `setContentView`, 否则与 `AppCompatActivity.setContentView(View?)` 的 JVM 签名冲突), `UI.kt` 的 `ensureUiHost` / `ensureWindowActivity` (statusBarColor, statusBarIconLight(By), backgroundColor, navigationBarColor, navigationBarIconLight(By), keepScreenOn, finish 九个函数) 与 `bindValueForApplyingAttribute(scriptRuntime, attrName, value)` 的失败钩子, `ui.root` 改为承载的 `contentRoot`, 无 Activity 的 `ui.inflate` 借用承载的主题上下文; 没有承载时仍走 `ensureActivity` 与原消息. 回归: `ScriptLooperCompatibilityDeviceTest.uiAndOverlayCallbacksFinishAndClose` 与 `JsListViewDataBindingDeviceTest` 在 API 37 (emulator-5590) 与 API 24 (emulator-5570) 通过; JVM `TypeScriptTsxRuntimeTest` / `UIWidgetScopeCompatTest` / `RhinoUtilsScopeCompatTest` 23 项通过.
- [x] (宿主) `UiPreviewService` (exported, `android:permission="org.autojs.permission.PLUGIN"`, 默认进程, Manifest 注册在插件相关组件旁): 校验调用方权限与契约版本; 会话表按客户端 Binder `linkToDeath`; `createSession` 在主线程创建 `SurfaceControlViewHost(context, displayManager.getDisplay(displayId), hostToken)` (`embedded`) 或离屏根 (`snapshot`), 以 `createConfigurationContext` 应用请求中的 densityDpi / 夜间 / 语言 / fontScale 覆盖 (缺省跟随宿主), 主题 `ScriptTheme`.
  证据 (2026-10-15): `306a8f7834`: `core/ui/preview/UiPreviewService.kt` (Manifest 条目位于 `TimedTaskAlarmReceiver` 与 `ThreeStoveAgentAttachRequestReceiver` 之间; 每个事务 `checkCallingOrSelfPermission(PLUGIN)`, `contractVersion` 必须为 1 否则 `CONTRACT_MISMATCH`, 会话按调用 uid 归属 (他人的会话一律 `SESSION_NOT_FOUND`), 回调 Binder `linkToDeath` 释放; Binder 线程校验, 主线程 `FutureTask` 限时执行: `createSession` 15 s, 其余 5 s), `UiPreviewSession.kt` (API 35+ 优先用 `InputTransferToken` 构造, 否则 `hostToken`; API 37 `LayoutParams(w, h, focusable = true)`; `createDisplayContext` + `createConfigurationContext` (densityDpi / UI_MODE_NIGHT / setLocale / fontScale) + `ContextThemeWrapper(ScriptTheme)`; 根容器 `UiPreviewRootView` 带主题 `windowBackground`, 编辑模式拦截指针并回报 `onTap`, 绘制高亮). 设备: API 37 模拟器 `embeddedSessionReturnsASurfacePackageAndBounds` 通过 (InputTransferToken 路径); `hostToken` 路径 (API 30 - 34) 于 P10.3 在 emulator-5560 (API 30) 补跑通过 (宿主 d72ad7402 修正首帧 0 x 0 边界与超时会话悬挂后, `UiPreviewServiceTest` 9 项全过).
- [x] (宿主) `PreviewRuntimeFactory`: 每个会话一个真实 `ScriptRuntime` (经 `ScriptEngineService.execute(StringScriptSource, config)` 运行合成脚本, config 带预览会话标识, 不出现在用户可见的运行列表或以 "预览" 标注, 待 Q20); 合成脚本固定为 `"ui"; ui.bindingContext = JSON.parse(__bindings__); ui.layout(__xml__);` 两个值由宿主以 `ScriptEngine.put` 注入, 不拼接进源码; `{{expr}}` 求值失败时 (预览标识生效) 保留原文, 记入 `onLayoutFailed` 的属性级诊断而不中止 inflate; 每次 `setLayout` 复用同一运行时重新 `ui.layout`.
  证据 (2026-10-15, 按 Q20 建议 (b) 实施, 待维护者确认): `306a8f7834`: 实际形态为 `UiPreviewScriptExecution` (`AbstractScriptExecution`, `ExecutionConfig.uiContentHost` 宿主持有标识, `ScriptEngineService.execute` 优先按它分派; 主线程创建并 `init` 引擎, 标签 `ui.content.host`, `LoopBasedJavaScriptEngine.execute`), 以客户端 `label` (缺省 "3-Block Compose 预览", 11 目录字符串 `ui_preview_default_label`) 出现在运行列表, 宿主侧停止经 `LoopBasedJavaScriptEngine.forceStop` -> `UiContentHost.onEngineForceStop` 释放并回调 `onSessionLost(stopped)` (`stoppingTheRuntimeFromTheHostLosesTheSession`); 合成脚本是常量 `"ui";` (`UiPreviewScript.SOURCE`, JVM 守卫其不含 `{` / `$`), 每次 `setLayout` 在主线程以 `withRhinoContext` 直接调用 `UI.layout(runtime, [xml])`, 绑定上下文由 `JsonParser(cx, topLevelScope).parseValue` 得到 (这个 Rhino 构建的四参数 `NativeJSON.parse` 要求 reviver 非空), 请求内容从不进入源码; `{{missing}}` 求值失败经 `onBindingFailed` 保留原文并记入响应的 `diagnosticsJson` (`bindingFailureIsReportedWithoutStoppingTheFrame`: applied, 诊断 `{code: BINDING_FAILED, i: 1, a: "text"}`, 快照与 3 条边界照常), `setLayout` 之外的失败经 `onLayoutFailed`; 窗口级函数经 `ui_preview_window_function_ignored` 每函数警告一次.
- [x] (宿主) 边界采集: 以 `LayoutInflaterDelegate.afterCreateView` 的 DOM `node` 先序号为键 (只数元素), 布局完成 (`OnGlobalLayoutListener` 去抖) 后回传 `(index, left, top, right, bottom, className, viewId)` 列表 (JSON, 超过内联上限时经调用方 fd), 坐标为嵌入根坐标 = 客户端 `SurfaceView` 像素.
  证据 (2026-10-15): `UiPreviewTrackingDelegate` (包装 ui 模块的 delegate: `beforeInflateView` / `afterInflateView` 维护元素栈, `afterCreateView` 记录 `(view, 先序号, id)`, 先序号表按 Document 缓存, 列表项实例与模板同号并以 `n` 区分), `UiPreviewSession.reportBounds` (嵌入: `OnGlobalLayoutListener` 50 ms 去抖; 快照: 布局后即时), 条目 `{i, l, t, r, b, c, d, n, s}`, 超过 5000 条截断并 `boundsTruncated` (不走 fd, 见附录 B.4). 设备: `snapshotSessionReturnsPixelsAndBoundsAndAppliesBindings` 3 元素 -> 3 条 (`i` 0..2, `d` root / title / ok, 根 0,0,320,480, `w="{{w}}"` 绑定的 120 dp 宽度与 `displayMetrics.density` 换算在 1 px 内); 嵌入会话 3 条且根宽 400, `resize(300, 500)` 后新边界根宽 300.
- [x] (宿主) `snapshot` 模式: `measure(EXACTLY w, EXACTLY h) / layout / draw` 到 ARGB_8888 位图, 原始像素写入请求的 fd (行主序, 无压缩), 返回长度与 SHA-256; 需窗口的控件 (`WebView` / `SurfaceView` / `TextureView` / `VideoView` / `JsCanvasView`) 以带类名的占位矩形代替 (在 `afterCreateView` 替换); 像素上限见附录 B.
  证据 (2026-10-15): `UiPreviewSession.layoutOffscreen` / `drawSnapshot` (主线程绘制并 `copyPixelsToBuffer`), 像素在 Binder 线程求 SHA-256 并经 `AutoCloseOutputStream` 写入调用方 fd (宿主关闭自己的副本; 进程内调用方须传 dup); `replaceCreatedView` 把 `WebView` / `SurfaceView` / `TextureView` (含 `JsCanvasView`) / `VideoView` 换成 `UiPreviewPlaceholderView` (灰底边框 + 类名). 设备: API 37 与 API 24 模拟器上 320 x 480 快照长度 = w * h * 4 且文件 SHA-256 与响应一致, 两个会话 (300 x 200 / 200 x 300) 各得各的像素与边界.
- [x] (宿主) 上限与拒绝: 每客户端会话数, 尺寸, XML 字节, 绑定 JSON 字节, 边界条目数, 单次 inflate 墙钟 (超时释放运行时并返回 `ERR_..._TIMEOUT`); 任何异常转为契约错误码, 不传播到服务进程主线程.
  证据 (2026-10-15): `UiPreviewRequests` (纯 Kotlin: 会话 / 尺寸 / 布局 / 指针模式校验与标签清洗, JVM `UiPreviewRequestsTest` 8 项), `UiPreviewFailures` (SAXParseException -> `XML_INVALID` 带行列与之前最后打开的元素先序号; 其它 -> `INFLATE_FAILED` 带跟踪到的元素与属性; JVM `UiPreviewFailuresTest` 5 项), `UiPreviewBounds` (JVM `UiPreviewBoundsTest` 4 项); 超时: Binder 线程 `FutureTask.get(5 s)` 超时返回 `timed_out` + `TIMEOUT`, 主线程返回后释放会话并 `onSessionLost(timeout)` (代码路径, 未在设备上制造超时). 设备 `requestsBeyondTheLimitsAreRejected`: 5000 px / contractVersion 2 / mode "window" / 超长 XML / snapshot 无 fd / 绑定为数组 / resize 0 / 未知会话 / API < 33 的交互指针 / API < 30 的 embedded 全部返回对应错误码; `twoSessionsAreIndependentAndAThirdIsRefused` 第三个会话 `SESSION_LIMIT`; `invalidXmlReportsThePosition` (`<vertical><text text="a"></vertical>` -> `XML_INVALID`, line >= 1, 元素 1); `inflateFailureNamesTheElement` (`<nope/>` -> `INFLATE_FAILED`, 元素 2, 共 3 元素, 会话随后仍能应用下一帧).
- [x] (测试) 宿主 JVM: 契约常量与哈希, 合成脚本不含请求内容 (注入而非拼接); 宿主 androidTest `UiPreviewServiceTest`: 以 instrumentation 进程 (与宿主同签名) 绑定服务, `embedded` 在 API 30+ 返回 `SurfacePackage` 且边界条目数 = 元素数; `snapshot` 位图长度 = w * h * 4 且 SHA-256 匹配; 非法 XML 返回带元素先序号的错误; 客户端死亡后会话释放; 并发两个会话互不影响.
  证据 (2026-10-15): JVM 25 项新增 (`UiPreviewContractTest` 8, `UiPreviewRequestsTest` 8 (含 `UiPreviewScript.SOURCE == "ui";` 不含 `{` / `$`), `UiPreviewBoundsTest` 4, `UiPreviewFailuresTest` 5) 全部通过; `UiPreviewServiceTest` 9 项: emulator-5590 (API 37, x86_64) 全部通过, emulator-5570 (API 24, x86) 8 项通过 + embedded 按假设跳过 (两台均 `OK (11 tests)`, 各含两项 ui 回归). "客户端死亡后会话释放" 由 `linkToDeath` 实现, 进程内的本地 Binder 不会死亡, 测试无法模拟, 改以宿主侧 `forceStop` 释放 (`stoppingTheRuntimeFromTheHostLosesTheSession`: `onSessionLost(stopped)`, 运行列表中移除, 之后 `SESSION_NOT_FOUND`) 覆盖释放路径; 跨进程死亡由 P10.3 的插件客户端测试 (杀插件进程) 补.
- [x] (宿主) AAR 产出: `:plugin-api:ui-preview-api:assembleRelease`, SHA-256 与来源提交写入插件 `locks/host-api-aars.lock` / `libs/README.md` / `THIRD_PARTY_NOTICES.md` (P10.3 同一提交); 宿主 10 语言 changelog 记录新增服务.
  证据 (2026-10-15): 宿主 `8c50a8fabc` 源码的 `ui-preview-api-release.aar` 18181 B (13 个 class, 无依赖), SHA-256 `bdf89d7460252dec71f384f367bb894848edaa4aaa691c95f8b4e21911f363b9`, 模块 `lintRelease` 0 问题; 入锁与 README / 声明随 P10.3 第一条. 宿主 changelog `v6.8.0` feature 条目 "ui 模式布局预览服务 ..." 10 语言 (`306a8f7834`, 生成的 CHANGELOG / README 同步); 宿主 `.changelog` 另有其它会话遗留的 zh-Hans 独有条目 (feature 1 / fix 3 / improvement 1), 因此 `generate_markdown.py --check` 与 `check_translations.py --record-sync` 在本会话之前就不通过, 本会话不代为翻译.

### P10.3 预览客户端与 XML 画布 (插件, 1.6.0)

- [x] (插件) `libs/ui-preview-api.aar` 入锁, `implementation` 并完整 keep; `HostApiAarLockTest` / `verify_apk_classpath.py` 覆盖; `<queries>` 已含宿主包, 无需新增.
  证据 (2026-10-15): 插件 `8568dba` (`build(designer): stage the ui-mode preview contract AAR`): `libs/ui-preview-api.aar` (宿主 `8c50a8fabc` 的 release AAR, SHA-256 `bdf89d7460252dec71f384f367bb894848edaa4aaa691c95f8b4e21911f363b9`) 入 `locks/host-api-aars.lock`, `app/build.gradle.kts` 的 `hostApiIds` 4 项与 `implementation(files(hostApiAars[3]))`, `proguard-rules.pro` 完整 keep `org.autojs.plugin.ui.preview.**` (并把 AAR 自带的 `org.autojs.plugin.typescript.parser.**` 规则写成显式), `verify_apk_classpath.py` 的 REQUIRED 加 `UiPreviewContract` / `IUiPreviewService$Stub$Proxy` / `IUiPreviewCallback$Stub`, `HostApiAarLockTest` 期望四个锁条目, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md` 记录来源; debug APK `APK_CLASSPATH_OK` (21039 类).
- [x] (插件) `UiPreviewClient`: 可用性 = 宿主已安装 + `PackageManager.resolveService(PREVIEW action, 宿主包)` + `getContractVersion()` 匹配; 不可用时原因 (未安装 / 版本过低 / 服务拒绝) 供编辑页显示; 绑定在编辑页 RESUMED 期间保持, 会话跟随 `SurfaceView` 的 `surfaceCreated / surfaceChanged / surfaceDestroyed` (创建 / resize / 释放); 请求去抖 400 ms, 后一次覆盖前一次; 回调切回主线程.
  证据 (2026-10-15): 插件 `6cc5a87` (`feat(designer): preview ui-mode projects on a canvas drawn by the host`): `designer/preview/UiPreviewClient.kt`: `availability()` 不绑定即区分 `HOST_MISSING` / `HOST_TOO_OLD` (服务不可解析), `bind()` 握手比对版本与哈希 (`CONTRACT_MISMATCH`), 绑定失败 / 空绑定 / 远端异常为 `UNAVAILABLE`; 会话随表面: `surfaceChanged` -> `attachSurface` (首次创建, 后台回来 `reattach` 取新 `SurfacePackage`), `resize` (嵌入 relayout, 快照另请求一帧), `surfaceDestroyed` -> `detachSurface` 保留会话; `submit` 400 ms 去抖且后一次覆盖前一次, Binder 调用在单工作线程 (宿主 `setLayout` 同步到主线程应用完成), 回调经主线程 Handler 进入 Compose 状态; `onSessionLost` / 宿主死亡 -> `Status.Lost` 并由编辑页 ON_RESUME 或 "重试" 重连; 宿主冷启动首个引擎超过其 15 s 等待时 (API 30 x86_64 模拟器实测 17.9 s) 自动重试一次. 与条目的差异: 绑定随编辑页存活 (onDestroy 关闭) 而非只在 RESUMED 期间, 会话在后台保留 (P10.0 结论).
- [x] (插件) `XmlPreviewFrames`: 由 `UiDocument` 生成请求: 活动片段的 XML 文本 (CST 原样序列化, `{expr}` 用 `DeclarationEvaluator` 的值替换, 求不出时替换为源码文本并记诊断; 非属性位置的 `{expr}` 替换为注释占位), 绑定值 JSON (`{{}}` 中出现的标识符 -> 顶层声明求值结果或绑定值面板的覆盖值), 元素先序号表 (命中与问题定位), 配置覆盖 (画布夜间模式 / 语言跟随设计器设置).
  证据 (2026-10-15): 插件 `720af37` (`feat(designer): build ui preview requests and evaluate value-like declarations`): `designer/preview/XmlPreviewFrames.kt` 产出 `XmlPreviewRequest(xml, bindingsJson, elementIds, bindingNames, notes)`: 属性内 `{expr}` 换成求值文本 (实体编码) 否则保留源码并记注; 内容位置的 `{expr}` 求得原始值时写成文本, 否则 `<!-- {expr} -->` 占位 (注释不算元素, 先序不变); `{{ }}` 的标识符按首次出现顺序收集, 只有声明 / 覆盖值中存在的名字进入 JSON; 配置覆盖由客户端 `configure(appearance.dark, appearance.language)` 随会话创建. `XmlPreviewFramesTest` 4 项: 12 个样例先序号表与 CST 一致且无表达式时 XML 与 `UiScriptWriter` 字节相同, 标识符与绑定 JSON, 表达式替换与注记, login.js 的第二片段可单独请求.
- [x] (插件) `DeclarationEvaluator` (API 26+, 复用 `SandboxEngine`): 只对顶层 `var / let / const` 中初始化式为字面量, 数组 / 对象字面量, 模板串, 一元 / 二元运算与 `JSON.*` / `Math.*` 的声明按源码顺序求值, 其余跳过; 一次求值受沙箱既有上限; API 24 / 25 不求值, 绑定显示原文.
  证据 (2026-10-15): `720af37`: `designer/sandbox/DeclarationEvaluator.kt` (与 `SandboxEngine` 同样的 `ContextFactory` 规则: `initSafeStandardObjects`, 全拒 `ClassShutter`, 解释模式, 2000 万指令 / 1 s / 200 栈帧; 未直接复用 `SandboxEngine` 因后者先装 compose 预览运行时) 以 Rhino `Parser` 挑出值类声明 (字面量, 数组 / 对象字面量, 模板串, 一元 / 二元 / 条件运算, 对已选声明或 `Math` / `JSON` 的属性与下标访问, `JSON.*` / `Math.*` 调用; 函数, 其它调用, 未知名字与 `++` 跳过), 逐条 `try` 求值后以 `JSON.stringify` 回传, `{expr}` 在同一作用域求 `String(...)`; `SUPPORTED` = `SandboxPreview.SUPPORTED` (API 26+), 编辑页在其下不求值. `DeclarationEvaluatorTest` 5 项; `RhinoImportGuardTest` 仍通过 (沙箱包是唯一进入 Context 的地方).
- [x] (插件) `XmlCanvas`: 与 compose 画布同样的有界逻辑屏幕与分栏; `AndroidView { SurfaceView }` + `setChildSurfacePackage`, 上层 overlay 在编辑模式独占指针 (点选按边界取最深命中的元素, 长按拖至合法容器并按 `DropPlacements` 方向表画插入线), 选中框与 `标签 #id` 名称标签; 缩放做法按 P10.0 结论; 快照模式以 `Image` 显示位图, 其余相同; 预览不可用或尚无首帧时显示大纲式占位 (标签树文字) 而不是空白.
  证据 (2026-10-15): `6cc5a87`: `designer/preview/XmlCanvas.kt`: `BoxWithConstraints` 的视口即逻辑屏幕, 内容框 `requiredSize(逻辑 x zoom)` 可双向滚动; 嵌入模式 `AndroidView { SurfaceView }` 以 `setZOrderOnTop(false)` 置于窗口之下 (P10.0: 此时指针落在本窗口, 且 SurfaceView 在窗口里挖洞, Compose overlay 画在其上), `graphicsLayer` 缩放, 命中坐标除以 zoom; 快照模式 `Image(bitmap, FillBounds)`; overlay 点选取面积最小的可见边界 (并列取先序号大者), 长按拖拽经 `UiDesignerSession.placeDrop` (容器轴来自目录 `axis`, `linear` 按 `orientation` / 裸 `vertical` 修正) 画插入线, 选中框 + `标签 #id` 名称标签, 失败元素以 danger 色描边; 无首帧时显示标签树文字. 与 3.1 的差异: 编辑态的命中与高亮由插件 overlay 完成 (表面在窗口下), 宿主的 `onTap` / `setHighlight` 在 1.6.0 不使用, 留给 1.7.0 交互预览 (表面置顶时) 使用.
- [x] (插件) 问题与状态: inflate 失败按元素先序号映射到 CST 元素并在大纲 / 画布高亮, 文案进入问题列表; 服务断开 (`onSessionLost` / 宿主被杀) 显示提示并在 `onResume` 重连; 预览可用性提示 (需要含预览服务的 AutoJs6 构建, 显示已安装版本).
  证据 (2026-10-15): `6cc5a87`: `DesignerActivity.UiEditorScreen` 把 `client.failure.elementIndex` 经请求的 `elementIds` 映射为 `session.problemElementId` (画布 danger 框, 大纲行着色, 问题页可点选), 失败 / 属性级诊断 / 请求注记进入 `session.previewProblems`; 状态文案 `designer_ui_preview_{host_missing,host_old,mismatch,unavailable,lost,waiting,snapshot,failed}` 与 `designer_ui_tab_problems` 9 键 x 11 目录 (`StringResourceParityTest` / `ApplicationTextPunctuationTest` 通过), 画布左下角显示并在 Lost / Unavailable 时给 "重试"; `UiPreviewClient` 的 `onBindingDied` / `DeadObjectException` / `onSessionLost` 置 `Lost`, 编辑页 ON_RESUME 时 `retry()` 或 `ensureSession()`.
- [x] (测试) JVM: `XmlPreviewFramesTest` (52 示例的请求可生成, `{expr}` / `{{}}` 替换与诊断, 先序号表与 CST 一致), `DeclarationEvaluatorTest`; 设备 `UiPreviewDeviceTest` (插件进程, 宿主含服务时: 打开示例工程, 画布收到边界且元素数一致, 点选元素更新属性抽屉, 修改 `text` 后新边界到达; 宿主缺服务时以 Assume 跳过并验证提示) 在 AVD_API_30 (嵌入) 与 API 24 实例 (快照) 通过; `verify_standalone_launch.py` 增加 "新建 UI 工程并等待首帧边界" 步骤 (有服务时).
  证据 (2026-10-15): JVM 新增 9 项 (`XmlPreviewFramesTest` 4 以 12 个入库样例为语料, `DeclarationEvaluatorTest` 5), 全套 219 项通过; `UiPreviewDeviceTest` (插件进程) 在 emulator-5560 (API 30, 嵌入, `hostToken` 路径; 会话创建 2.1 s, 首帧 254 ms, 后续 22 - 33 ms) 与 emulator-5570 (API 24, 快照) 通过: 4 元素 4 条边界且 id 为 root / title / ok / note, 经 `UiAutomation` 注入的点击选中按钮并反映在大纲, `SetAttr(text)` 后新帧到达, `SetTag(nope)` 的失败帧定位到该元素并显示在状态区, undo 后恢复, 保存写回修改且无草稿; 宿主缺服务时断言状态提示并只验证大纲选择. 未做: "点选元素更新属性抽屉" 以大纲选中代替 (属性抽屉随 P10.4); `verify_standalone_launch.py` 的 UI 工程步骤随 P10.4 的新建对话框一并加入. 宿主侧随之修正 (宿主 d72ad7402): 嵌入层级首次遍历前不回传 0 x 0 的边界 (API 30 上首帧曾回传根宽 0), `createSession` 超过等待后主线程创建的会话立即释放而不是悬挂到服务销毁. API 37 模拟器不能跑 Compose 设备测试 (Espresso 的 `InputManager.getInstance` 反射在 API 37 不存在), 嵌入路径改在 API 30 实例验证.

### P10.4 编辑器集成 (插件, 1.6.0)

- [x] (插件) 工程类型: 新建对话框增加 "UI (XML) 工程" (模板: `"ui";` + `<vertical padding="16"><text id="title" text="Hello" textSize="24sp"/><button id="ok" text="OK"/></vertical>` + 一条 `ui.ok.on('click', ...)`), 首页列表以图标区分 Compose / TSX / UI; 导入与外部文档 (EDIT / VIEW) 按内容判定类型 (`DocumentReader` 分派), 判定失败时沿用现有错误定位展示.
  证据 (2026-10-15, `533e43f` / `07bce84`): `ProjectStore.Kind` (JS / TSX / UI; `.js` 以 `UiScriptReader.looksLikeUiScript` 判定) 与 `UI_TEMPLATE` (模板的点击处理函数写 `toast('OK')`); 首页新建对话框第三个 chip "UI (XML)" (`home-language-ui`, 文案 `designer_ui_language_ui`), 列表行前缀 `JS` / `TSX` / `UI` 等宽文字徽标 (`home-kind-<name>`, 不另做图标资源); 导入与外部文档沿用 P10.3 的 `DocumentReader` 分派, 判定失败走 `ParseErrorScreen` (`UiDesignerDeviceTest.anUnmappableScriptShowsItsErrorPosition`: `ui.layout(x)` 的变量未以字面量初始化, 错误文本含位置与原因).
- [x] (插件) `DesignerActivity` 按文档类型切换: 画布 (`XmlCanvas`), 大纲 (元素树, 片段切换器, 折叠 / 上下移 / 删除 / 拖拽), 属性抽屉 `UiInspectorPanel` (标签 (目录内可改), `id`, 目录属性按继承链分组且已设置的置顶, 值三态: 文本 / `{{}}` 绑定 (选择顶层声明或手输表达式) / `{expr}` 只读源码, 简写与全名互为别名提示, 文本内容, 裸属性开关, 自由添加任意属性名 (警告), 删除属性), 组件面板 `UiPalette` (按文档分类: 布局 / 文本 / 按钮 / 输入 / 选择 / 进度 / 图片 / 列表 / 容器 / 其它, 每项带默认属性模板, 不可插入 `compose` 与完整类名), 绑定值面板 (复用 `StatePanel` 的预览值: 列出 `{{}}` 引用的顶层声明与求值结果, 可临时覆盖, 不进文档), 问题列表, 控制台标签页 (1.6.0 只显示预览诊断).
  证据 (2026-10-15, `07bce84`): 编辑页五个标签页 (大纲 / 属性 / 绑定 / 问题 / 控制台) 与右下角 "+" 面板. `XmlOutlinePanel`: 折叠开关 (`designer-xml-collapse-<id>`), 上下移 / 删除, 长按拖拽 (上下四分之一为 BEFORE / AFTER, 中间为 INTO, 不接受子元素的目标退化为 AFTER, 经 `UiDesignerSession.moveInto` / `moveTo`), 文档有多个 XML 字面量时顶部 FilterChip 切换片段 (`designer-xml-fragment-<n>`, 传给 `ui.layout` 的标 "(ui.layout)"). `UiInspectorPanel`: 标签下拉 (面板可插入的标签, `SetTag`), `id` 文本框 (新 `id` 写在开始标签首位, `SetAttr` 的 `id` 特例), 无元素子节点的元素可编辑文本内容 (`SetText`, 空容器不显示), "已设置的属性" 按文档顺序置顶, 其余目录属性按所属类 (`UiCatalog.Attr.ownerClass`, 最近的类先, 默认只展开第一组) 分组折叠, 简写行在名下显示全名 (`w` -> `width`), 值三态: 文本 (boolean 为开关, enum 为下拉, 其余文本框, 实体经 `XmlEntities` 解码 / 编码) / `{{ }}` 绑定 (单一绑定编辑花括号内的表达式, 混合文本编辑原文, 旁边下拉列出 `DeclarationEvaluator.declarationsOf` 的顶层声明名) / `{expr}` 只读源码带提示, 行菜单切换文本 / 绑定 / 裸属性 / 清除, 自由属性名添加 (按目录类型给默认值, 未知名由校验器告警); 事件区见 D52. `UiPalette`: `UiTemplates` 的 10 个分类 (layout / text / button / input / choice / progress / image / media / list / other, 文案 `designer_ui_category_*`), 48 个标签各带默认属性 (`UiTemplatesTest`: 每个模板插入根 vertical 后 0 错误且无未知属性告警, 写回再读字节稳定), 不含 `compose` 与类名标签, 行尾显示 View 类. `XmlBindingsPanel`: 当前片段引用的 `{{ }}` 名 (来自 `XmlPreviewRequest.bindingNames`), 每个名显示预览求得的声明值 / 求值失败原因 / "无顶层声明", JSON 覆盖输入 (`JsonText.parse` 校验, 非法时标红且不写入), 单项与全部重置; 覆盖值只进 `bindingOverrides`, 文档, 撤销与草稿不受影响 (`UiDesignerDeviceTest.bindingOverridesChangeOnlyTheCanvas`). 问题列表沿用 P10.3; 控制台 `XmlConsolePanel` 显示预览状态 (宿主版本, 快照 / 等待等) 与宿主回传的失败码 / 位置 / 诊断, 1.6.0 不运行脚本.
- [x] (插件) 存储与去向: `ProjectStore` 模板与类型判定, 草稿与恢复, 外部文档写回, 导入来源同步, 发送运行 / 宿主编辑器 / 导入宿主目录 / 复制 / 导出均复用, 不区分类型.
  证据 (2026-10-15, `07bce84`): UI 编辑页补齐 Compose 编辑页同样的 "写回来源" / "从来源重新导入" 菜单项与三个同步对话框 (`reimport` 只接受 `ParsedUi`), 其余去向与草稿 / 恢复 / 外部修改检测沿用 P10.3. 真实进程被杀的恢复由 `verify_designer_recovery.py` 的 UI 分支证明 (下一条).
- [x] (插件) 10 语言字符串 (`designer_ui_*` 前缀, ASCII 标点), `StringResourceParityTest` 覆盖.
  证据 (2026-10-15, `07bce84`): `designer_ui_*` 新增 36 键 x 11 目录 (属性抽屉 / 面板分类 / 绑定面板 / 控制台 / 事件 / 片段 / 工程类型), `StringResourceParityTest` 与 `ApplicationTextPunctuationTest` 通过.
- [x] (测试) 设备 `UiDesignerDeviceTest` (插件进程, 不依赖宿主服务: 新建 UI 工程 -> 面板插入 `button` -> 改 `id` 与 `text` -> 保存 -> 文件只在对应行变化且重开一致且草稿已清; 大纲上移 / 撤销 / 重做 / 删除; 绑定值面板覆盖; 不可映射脚本的错误定位; 52 个宿主示例 (测试资源拷贝) 逐一打开无解析错误) 在 AVD_API_30 与 API 24 实例通过; `verify_designer_recovery.py` 增加 UI 工程分支.
  证据 (2026-10-15, `b6b0e55`): `UiDesignerDeviceTest` 6 项 (面板插入 -> 属性抽屉改 id / text -> 保存后文件恰为模板多出一行 `<button id="go" text="Go"/>` 且重开一致无草稿; 大纲上移 / 撤销 / 重做 / 删除; 绑定覆盖只改画布 (API 26+ 另断言声明求值为 `"Hi"`); 事件桩追加与撤销 (有 id 的 `ok` 与面板新插入无 id 的 `text1`); 不可映射脚本的错误位置; 12 个入库示例 (`src/test/resources/ui-samples` 以 androidTest resources 形式打包, 不再复制一份) 逐一打开无解析错误且写回字节相同) 在 emulator-5560 (API 30, 嵌入) 与 emulator-5570 (API 24, 快照) 通过, 同批回归 `UiPreviewDeviceTest` 与 `DesignerHomeDeviceTest`. 条目所写 "52 个宿主示例" 为 `AUTOJS6_HOST_DIR` 下的 JVM 往返语料 (P10.1), 设备上只开 12 个拷贝. `verify_designer_recovery.py` 增加 `--kind compose|ui|both` (默认两种都跑): UI 分支新建 UI 工程 -> 面板插入 `button` -> HOME -> 杀进程 -> 恢复草稿后大纲出现 `button` -> 放弃 -> 重开只剩模板; API 24 (emulator-5570, compose + ui) 与 API 30 (emulator-5560, ui) 通过. 脚本两处修正: Android 7 把刚离开的应用留在 "previous app" (adj 700), `am kill` 不杀, 改为对 debuggable 包 `run-as kill -9` (再退到 root adbd 的 `kill`); 面板滚动改按可见行跨度的一半, 原来按屏高 2/3 滑动会在两次 dump 之间整段跳过 `button` 等行 (API 30, 1080 x 2400). `verify_standalone_launch.py` 增加 UI 工程步骤 (新建 "UI (XML)" 工程 -> 大纲出现 `button #ok` -> 记录画布状态 `frame` / `snapshot` / `host_missing` / `waiting`), API 24 上 `ui_preview=snapshot` 通过. 设备测试暴露并修正两个 P10.3 缺陷 (`6d363f9`): 编辑页关闭后宿主才创建完成的会话曾向已关闭的工作线程投递释放任务 (`RejectedExecutionException` 杀掉插件进程), 现在改走一次性线程; 宿主在 `setLayout` 里抛出的非受检异常 (Binder 原样回传 NullPointerException 等) 曾让客户端工作线程崩溃, 现在转为 `ERR_AUTOJS6_UI_PREVIEW_INFLATE_FAILED` 失败帧. 触发它的是 `survey.js` 的 `<datepicker>`: Android 7 的日历在精确尺寸父容器下测量时 `SimpleMonthView.getMonthHeight()` 空指针; 宿主 `ea68a0c0b` 在窗口遍历前先按会话尺寸做一遍测量 / 布局探测 (快照模式本来就有), 异常判为布局失败并移除内容, 宿主主线程不再暴露给这类 View (API 24 实测日志 `layout pass failed: ERR_AUTOJS6_UI_PREVIEW_INFLATE_FAILED: ... SimpleMonthView ...`, 插件进程存活). lint debug 0 错误 (顺带修正 P10.3 遗留的 6 个 `NewApi`: `Embedded` 调用点加显式 SDK 30 检查). JVM 全套 229 项 (新增 `UiHandlerStubTest` 7, `UiTemplatesTest` 3).

### P10.5 文档, README, changelog 与 1.6.0 gate (文档 + 发布)

- [x] (文档) 文档站: `api/composeDesigner.md` 更名或新增总页 (待 Q18) 增加 "UI (XML) 工程" 一章: 可识别的脚本子集, 无损写回规则, 属性目录的来源与版本, 布局预览的能力边界 (3.5 节清单), 低版本快照, 所需宿主版本; `api/ui.md` 加一处链接; Offline Docs 同步 (`--dry-run` / `--verify-offline`).
  证据 (2026-10-15, D53): 文档站 `a248aa4` (标题改为 "3-Block Compose 设计器 (Compose 与 UI 工程)"; 用途 / 入口 / 工程与文件 / 编辑页注 / 限制各加 UI 工程一句; 新章 "UI (XML) 工程 (1.6.0)": 可识别的脚本子集, 无损写回, 编辑页七区表, 事件 (桩文本与规则), 布局预览 (三级条件表, 失败 / 运行列表 / 1.6.0 只做布局), 限制 (含属性目录来源); `api/ui.md` 在属性清单之后加一处链接, 生成锚点 `composedesigner_ui_xml_1_6_0`), `87d1e3c` (计数 112; 另一会话留在工作区的 +1 一并进入该提交). `normalize-markdown.py --check`, 全站生成 (158 模块) 与 `auto-generate.py --check` 通过. Offline Docs: `auto-generate-for-autojs6.bat --dry-run` (变更 3) 与 `--verify-offline` (插件测试与 debug / release APK 门禁) 通过, `9c7aa5a` (build 142, provenance `a248aa4`, 6.8.6 条目的设计器一行 10 语言各加 "UI (XML) 工程 (1.6.0 起 ...)").
- [x] (文档) 插件 README 10 语言 (功能, 用法, 兼容性 "XML 预览需要含预览服务的 AutoJs6 构建 (版本号待回填), Android 11 起可交互嵌入, 更低版本为快照", FAQ "设计器会运行我的脚本吗" 补充布局预览不运行用户代码, 安全条目), changelog `v1.6.0` 10 语言 (feature / dependency (新契约 AAR) / hint); `AGENTS.md` 第 1 / 2 / 4 / 6 / 13 / 15 节按实际落地修订 (设计器文档类型, 新 AAR 与锁, 新测试类, 安全描述).
  证据 (2026-10-15, `0c1ff54`): `.readme` 10 语言 (`p_status` 改为 1.6.0 并加预览服务要求 (宿主提交 306a8f7834 之后; 插件按服务能否解析判断, 不按版本号, 所以不回填版本号), 特性第 3 条 "UI (XML) 工程", 兼容性第 15 条, FAQ "设计器会执行脚本吗" 补布局预览只做布局, 安全第 6 条 "布局预览"), `.changelog` v1.6.0 (hint 1, feature 4, dependency 1) 10 语言, 36 个生成物重生成并 `--check` 通过; `AGENTS.md` 第 1 (例外 2: 绑定预览服务; 独立界面段), 2 (契约表新行), 4 (libs 与 designer 各包), 12 (UI 工程一条), 13.1 / 13.2 (新测试与脚本分支), 14 (文件名 1.6.0), 15 (布局预览安全边界) 节; `version.properties` 1.6.0 / build 163. 第 6 节不需改 (Manifest 与 `<queries>` 未变).
- [x] (发布) 1.6.0 本地 gate (AGENTS 第 14 节顺序 + 本文附录 E), 体积与方法引用按复核线记录; 宿主侧 compose 设备矩阵不受影响 (本阶段不改宿主内 Compose 渲染路径, 但 `UI.kt` 承载抽象改动需跑宿主 ui 相关 androidTest); 推送, Release, 官方索引准入按 D7 现行授权.
  证据 (2026-10-15, build 163): `generate_markdown.py --check` / `generate_icon_studio.py --check`; JVM 229 项; `assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` (0 错误) / `assembleRelease` / `verifySharedClasspath`; `verify_apk_classpath.py` debug (14728464 B, 147895 引用) 与 release `--shrunk` (5812852 B, 98391 引用, unresolved 0); `appendDigestToReleasedFiles` 产出 `autojs6-plugin-three-block-compose-v1.6.0-f6c20431.apk` (5812856 B, SHA-256 `70126814...a07b`, 签名者 `31a681fc...`), 在复核线 6177215 B / 105804 之内 (较 1.5.1 的 5617018 B / 96186 增加 195838 B / 2205 引用: 契约 AAR, 预览客户端, XML 面板与抽屉). 全套 instrumentation 128 项: emulator-5570 (API 24, 快照) 全部通过 (10 项按假设跳过), emulator-5560 (API 30, 嵌入) 全部通过 (6 项跳过); 两台都经 adb 直接运行测试包, 因为 AVD_API_30 (2 GB 内存, 空闲 63 MB) 经 Gradle 的两次运行都在中途因 system_server 死亡中断 (`INSTRUMENTATION_ABORTED: System has crashed`, 一次 8 项后, 一次 109 项后 `DeadSystemException`), 不是插件失败. release 包: API 24 `verify_standalone_launch` (`ui_preview=snapshot`) 与 `verify_host_handoff` (宿主为含 ea68a0c0b 的 debug 包) 通过; API 30 `verify_standalone_launch` (`sandbox=ran`, `ui_preview=frame`) 与 `verify_designer_recovery --kind both` 通过. 宿主 ui 回归 (emulator-5570, 宿主 androidTest 包按当前 master 装配): `UiPreviewServiceTest` 9 项 (嵌入项按假设跳过), `JsListViewDataBindingDeviceTest`, `ScriptLooperCompatibilityDeviceTest` 12 项 (需先启用 AutoJs6 无障碍服务, 只读 AVD 重启后该设置丢失) 全部通过. 未执行: 真机 (与前几版相同, gate 在本机 AVD 完成); 宿主侧 compose 设备矩阵 (本阶段未改宿主内 Compose 渲染路径). 推送, Release 与索引准入见会话记录.

### P10.6 交互预览: 真实运行 (宿主 + 插件, 1.7.0)

- [x] (宿主) 契约 v2 (只追加): `runScript(sessionId, Bundle request)` (源码文本或工程文件 content Uri (由插件 FileProvider 授权, 使 `files.*` 相对路径与运行一致), 显示名, 工作目录), `stopScript(sessionId)`; 回调追加 `onConsole(sessionId, level, text)`, `onScriptError(sessionId, message, line)`, `onScriptExit(sessionId, reason)`; `HASH` 更新, `getContractVersion()` 返回 2, v1 客户端照常.
  证据 (2026-10-15, 宿主 `4c332c2053`): `IUiPreviewService` 在 v1 事务之后追加 `runScript(String, Bundle)` / `stopScript(String)`, `IUiPreviewCallback` 追加 `onConsole` / `onScriptError` / `onScriptExit` (Bundle 载荷: `runId`, `level` (Log 优先级) / `text`, `errorMessage` / `line`, `reason`); `UiPreviewContract.VERSION` = 2, `HASH` `81f7fe8d...`, `HASH_INPUT` 以 v2 前缀列出全部事务. 两处偏离草案: (1) `getContractVersion()` / `getContractHash()` 继续返回冻结的 v1 身份 (`BASE_VERSION` = 1, `HASH_V1` `680f41b5...`), 因为已发布的 1.6.0 客户端严格比较这两个值, 返回 2 会让它的布局预览失效; v2 身份与 `scriptRunSupported` (API 33+) 经 `getPreviewInfo()` 宣告, `createSession` 接受 1 与 2 (`UiPreviewRequestsTest.v1AndV2ClientsBothCreateSessions`). (2) 请求只有源码文本 (`scriptText`, 上限 `MAX_SCRIPT_CHARS` 512 KiB, 须以 `"ui"` 指令开头) 与显示名 (`scriptName`), 不传 content Uri: 设计器工程是私有目录中的单文件, 没有相对路径依赖; 工作目录由宿主定为 `<cache>/ui-preview/<sessionId>` 并随会话删除. 另有 `ERROR_SCRIPT_FAILED`, 退出原因 `exit` / `stopped` / `error` / `replaced` / `released`, `SCRIPT_START_TIMEOUT_MS` 15 s. 契约模块 `UiPreviewContractTest` 8 项 (v1 前缀兼容, 事务与回调列表, 上限); 协议文档 `docs/dev/ui-preview-plugin-protocol-v1.md` 加 "Script runs (V2)".
- [x] (宿主) `PreviewUiHost` 承载真实执行: `ScriptEngineService.execute` 一次普通 ui 模式执行, `ExecutionConfig` 带预览会话标识, 引擎 `prepare` 阶段注入预览承载而不是 Activity (`activity` 保持 `undefined`); `ui.layout` 的内容进入嵌入根; 控制台输出与异常经回调转发; `exit()` / 异常 / `stopScript` / 会话释放均 `forceStop` 并清理; 同一会话再次 `runScript` 先停止前一次; 运行出现在宿主运行列表并标注 "预览".
  证据 (2026-10-15, 宿主 `af4de8b2c`): `UiPreviewScriptRun` (每次运行一个 `UiContentHost`, 根容器委托给会话) 经 `ScriptEngineService.execute(StringScriptSource(name, text), listener, ExecutionConfig(workingDirectory).also { uiContentHost = run })` 以 `UiPreviewScriptExecution` 启动, 引擎在主线程 (与 ui 模式脚本相同), 承载抽象不注入 Activity 因此 `activity` 为 undefined; `ui.layout` 内容经 `showContent` 替换会话根的内容; 控制台经 `ConsoleImpl.LogListener` 转发, 未捕获错误经执行监听器 `onException` 报 `onScriptError(line)` 后以 `error` 结束; `exit()` -> `forceStop` -> `onEngineForceStop` -> `exit`; `stopScript` -> `stopped`; 同会话再次 `runScript` 先以 `replaced` 结束前一次; 会话释放 (客户端 `release`, 客户端死亡, 服务销毁) 以 `released` 结束并删除工作目录; 布局预览运行时与根容器保留, 之后的 `setLayout` 照常. 偏离: 运行以 `scriptName` (工程文件名) 出现在宿主运行列表, 未另加 "预览" 标注 (名称已能区分, 且与 D51 的会话标签同形). 只有嵌入会话且 API 33+ 接受 (`validateScript`, D50), 快照会话与 API 30 - 32 回 `UNSUPPORTED_MODE`, 不以 `"ui"` 开头回 `SCRIPT_FAILED`. JVM `UiPreviewRequestsTest` 10 项 (`UiPreviewBoundsTest` 4, `UiPreviewFailuresTest` 5 不变).
- [x] (插件) 编辑页 "交互预览" 开关对 UI 工程生效 (API 30+ 嵌入模式; 快照模式隐藏开关并提示改用运行): 每个工程首次打开时确认对话框 (说明等价于运行且有副作用), 打开后 overlay 放行指针, 控制台标签页显示回传输出 / 错误 / 退出, 文档或绑定值变化后 400 ms 重跑, 设计器 `onStop` 与关闭开关即 `stopScript`; 退出预览后文档与文件不变, 无草稿.
  证据 (2026-10-15, 插件 `2777b06` AAR v2 入锁 (`e6b56d91...`, 19730 B, 13 class, `UiPreviewContractGuardTest`), `c5a7beb`): `UiPreviewClient` 以冻结的 v1 身份识别服务, 从 `getPreviewInfo()` 读 v2 与 `scriptRunSupported`, 新增 `setInteractiveMode` (开: `setPointerMode(INTERACTIVE)` 并扣住布局请求; 关: `stopScript`, `setPointerMode(EDIT)`, 重发上次布局), `runScript` / `stopScript` / `clearConsole`, 状态 `running` / `runId` / `consoleLines` (上限 500 行) / `scriptError` / `lastExit` / `runRevision` / `sessionRevision`, 会话丢失时清运行状态. `XmlCanvas` 增加 `interactive`: 以 `key(interactive)` 重建 `SurfaceView` 并 `setZOrderOnTop(interactive)` (P10.0 结论: 指针只在表面置顶时到达嵌入树), 会话经 `reattach` 回到新表面, 交互期间不再有 overlay 与画布内状态条. 偏离 (记录): 开关与运行状态不像 compose 画布那样叠在画布右上角, 而是放在画布上方一行 (`designer-interact`, `designer-ui-interact-status` / `designer-ui-script-problem`), 因为置顶表面会盖住本窗口叠在画布上的一切; 不支持时该行提示 "需要 Android 13" (快照与 API 30 - 32) 或 "需要更新的 AutoJs6" (v1 宿主), 运行去向照旧; API 33 - 34 运行中的文字注明预览不接收键盘输入 (D50). 确认对话框按工程草稿键存于 SharedPreferences `designer` / `ui-interactive-confirmed`; 编辑页 `LaunchedEffect(interactive, supported, visible, revision, sessionRevision)` 开启即 `runScript(session.script(), 标题)`, 文档变化后 400 ms 重跑 (绑定值覆盖不参与: 真实运行用脚本自己的值, 绑定面板的提示已说明), 不可见 (`onStop`) 或关闭即 `setInteractiveMode(false)`, 编辑页销毁释放会话 -> 宿主以 `released` 结束运行; 文档 / 文件 / 草稿不被触碰. 控制台标签页 (`XmlConsolePanel`) 交互期间显示运行输出 (按 Log 等级着色), 状态 (启动中 / 运行中 / 已结束 (原因) / 脚本错误) 与清空. 文案 10 键 x 11 目录 (`designer_ui_interact_*`, `designer_ui_console_run_*`). 顺带修复 P10.3 的竞态: `ensureSession` 在上一次 `createSession` 未返回时会再发一次 (画布高度随上方新行两次变化加外观到达), 宿主计两个会话, 第三次触到每客户端上限并让客户端停在 "not available"; 现在同一时间只创建一个会话, 期间变化的尺寸以 `resize` 跟上, 夜间 / 语言变化重建, `SESSION_LIMIT` 像 `TIMEOUT` 一样延迟重试一次.
- [x] (测试) 宿主 androidTest: `runScript` 计数器脚本点击后文本变化 (经边界与 `View` 读回), 控制台回传, `exit()` 后 `onScriptExit`, 死循环脚本可被 `stopScript` 停止; 插件设备测试 `UiInteractiveDeviceTest` (AVD_API_30 + 一台真机: 登录界面示例点击登录后控制台出现 toast 文案; 关闭开关后进程中无该执行) ; 残余偏差清单逐项在设备上确认并回填 3.5 节.
  证据 (2026-10-15): 宿主 `UiPreviewServiceTest` 11 项 (新增 `runScriptRunsTheScriptInTheRootWithConsoleClicksReplacementAndStop`: 计数器脚本的 `started` 回传, 经运行时的 `UiContentHost` 取根并 `findViewByStringId` 点击后文本 `count 1` 与 `clicked 1` 回传, 第二次运行以 `replaced` 结束第一次, `stopScript` 以 `stopped` 结束且运行列表无该执行, 之后 `setLayout` 照常; `scriptErrorsExitAndReleaseAreReportedAndRequestsAreChecked`: `throw` 的行号 3 与 `error`, `exit()` 的 `exit`, 非 ui 脚本 / 超长 / 缺文本的错误码, 释放运行中的会话 -> `released`; 另在信息项校验 v1 身份 + v2 信息, 在上限项校验快照会话拒绝 `runScript`) 在 emulator-5580 (AVD_API_33, x86_64, 4 GiB) 全部通过, 在 emulator-5570 (API 24) 与 emulator-5560 (API 30) 两个运行项按假设跳过其余通过. 插件 `UiInteractiveDeviceTest` 3 项 (API 33+ 且宿主有 `runScript`, 否则按假设跳过): 开关首次询问且取消保持编辑, 确认后宿主运行计数器脚本 (运行图标 `designer-ui-live`), 经 `UiAutomation` 注入的点击穿过置顶表面到达脚本的 click 处理函数 (控制台出现 `started` 与 `clicked 1`), 关闭开关后 `stopped` 且文档 / 文件 / 草稿不变, 重开工程不再询问; 隐藏设计器 -> `stopped`, 显示 -> 再次运行; 启动即 `throw` 的脚本在画布上方与控制台显示错误, 在 emulator-5580 通过 (需 `useUnmergedTree` 取 FilterChip 内的图标), 与 `UiPreviewDeviceTest` / `UiDesignerDeviceTest` 合计 10 项在 API 33 / API 30 / API 24 通过 (API 24 首轮在三台模拟器与两个 Gradle 构建共用显卡时遇到模拟器自身的 GL 中止 `CanvasContext.cpp:505` 并带倒 system_server, 单独复跑通过; `UiPreviewDeviceTest` 一处 "首帧无问题" 断言此前靠时序通过, API 26 以下无声明求值, 宿主对 `{{title}}` 报 ReferenceError, 现按 Android 版本等待对应状态). 另在 AVD_API_35_16K 只读实例 (x86_64, 4 GiB, 端口 5584; 宿主 x86_64 debug) 复跑: 宿主 11 项与插件 `UiInteractiveDeviceTest` 3 项 + `UiPreviewDeviceTest` 全部通过 (API 35 的输入法路径下运行中的提示为 "脚本正在 AutoJs6 中运行"). 未按原文执行的两项: (1) 死循环: ui 脚本的脚本体在宿主主线程运行 (与真实 ui 运行相同), `while (true)` 会卡住主线程与 `stopScript` 的 `onMain`, 因此宿主用 `setInterval` 的长运行脚本验证 `stopScript` / 释放, 死循环列入 3.5 节; (2) 真机: 真机上的宿主是维护者的 6.8.0 (5352) 发行包, 没有预览服务 v2, 且另一会话转达维护者约定 "不要把宿主 debug 包装到真机上", 本会话遵守, 真机证据待含 v2 的宿主发行后再补 (登录示例的 toast 是真实 Toast 而不进控制台, 设备测试改用 `console.log`). 3.5 节已按本阶段结论补两行.

### P10.7 交互预览的文档与 1.7.0 gate (文档 + 发布)

- [x] (文档) 文档站 "在预览中运行 UI 脚本 (1.7.0)" 小节 (与真实运行的差异表, 副作用提示, 控制台, 停止条件, 所需宿主版本), README 与 changelog `v1.7.0` 10 语言, `AGENTS.md` 安全描述修订.
  证据 (2026-10-15): 文档站 `26e53cd` (`api/composeDesigner.md` 的 UI (XML) 章新增 "在预览中运行 UI 脚本 (1.7.0)": 运行方式与副作用提示, 条件表 (契约 v2 / Android 13 / Android 15 输入法 / 回退行), 重跑与停止规则, 运行列表, 结果只用于画布, 与真实运行的差异 (无 Activity 窗口, 窗口级函数, 对话框为悬浮窗, 主线程忙等), 与 JavaScript 工程沙箱的区别; 页首版本范围, 编辑页两张表与限制列表指向该节; 全站生成 160 模块, `normalize-markdown.py --check` 与 `auto-generate.py --check` 通过), 计数 `e84824f` (114) 与 `a056f5f` (115, 离线同步器推进). Offline Docs `d41258b` (`auto-generate-for-autojs6.bat --dry-run` 变更 2, `--verify-offline` 通过: 测试与 debug / release APK 门禁, 16 KB 对齐; provenance 两处指向 `26e53cde02`, 6.8.6 条目的设计器一行 10 语言各加 1.7.0 一句, VERSION_BUILD 144). 插件 `cfb92fb` (版本 1.7.0 / build 170): `.readme` 10 语言 (`p_status` 改 1.7.0 并加契约 v2 与 Android 13 要求, 特性第 4 条, 兼容性第 16 条, FAQ "设计器会执行脚本吗" 补 UI 工程的真实运行, 安全第 7 条 "交互预览 (UI 工程)"), `.changelog` v1.7.0 (hint 1, feature 2, fix 1 (重复创建会话), dependency 1 (契约 AAR 升级 v2)) 10 语言, 36 个生成物 `--check` 通过; `AGENTS.md` 第 1 (例外 2 与独立界面段), 2 (契约表行), 12 (P10.6 一条), 13.1 / 13.2 (新测试与设备说明), 14 (文件名 1.7.0), 15 (交互预览安全边界) 节.
- [x] (发布) 1.7.0 本地 gate, 宿主 ui 相关 androidTest 回归, 推送 / Release / 索引.
  证据 (2026-10-15, build 170): `generate_markdown.py --check` / `generate_icon_studio.py --check`; JVM 231 项; `assembleDebug` / `assembleDebugAndroidTest` / `lintDebug` (0 错误) / `assembleRelease` / `verifySharedClasspath`; `verify_apk_classpath.py` debug (14775664 B, 148113 引用) 与 release `--shrunk` (5857120 B, 98578 引用, unresolved 0); `appendDigestToReleasedFiles` 产出 `autojs6-plugin-three-block-compose-v1.7.0-d4361bd3.apk` (5857120 B, versionCode 170, SHA-256 `9f70a325...b95e`), 在复核线 6177215 B / 105804 之内 (较 1.6.0 的 5812856 B / 98391 增加 44264 B / 187 引用: 契约 v2, 客户端运行状态, 画布开关行与控制台). 全套 instrumentation 131 项: emulator-5570 (API 24, 快照) 118 通过 13 跳过, `DesignerSourceSyncDeviceTest.reimportReplacesTheProjectAndAsksWhenItChanged` 在机器满载 (两个 Gradle 构建, Offline Docs 校验与四台模拟器同时运行) 时一次 10 s 超时, 单独复跑该类 4 项通过; emulator-5580 (AVD_API_33, 4 GiB) 131 项 126 通过 6 跳过 (首轮在模拟器因 GL 错误重启时于第 7 项中断, 单独复跑完整通过). release 包: API 24 `verify_standalone_launch` (`ui_preview=snapshot`) 与 `verify_host_handoff` 通过; API 30 (AVD_API_30 只读实例, 另一会话占用了可写实例) `verify_standalone_launch` (`sandbox=ran`, `ui_preview=frame`) 与 `verify_designer_recovery --kind both` 通过; API 33 `verify_standalone_launch` (`sandbox=ran`, `ui_preview=frame`) 通过, 而 `verify_designer_recovery` / `verify_host_handoff` 在该 AVD (1080 x 2400, 420 dpi) 上停在 "the palette entry Button did not come into view" (脚本以 uiautomator 滚动组件面板的方式不适配这一屏幕几何, Compose 驱动的 `DesignerDeviceTest` 在同一设备通过), 留待后续调整脚本, 两个脚本的证据以 API 24 / 30 为准. 宿主 ui 回归: `UiPreviewServiceTest` 11 项在 API 33 / 35 通过 (P10.6), API 24 / 30 运行项按假设跳过. 未执行: 真机 (同 P10.6: 真机不装宿主 debug 包); 宿主侧 compose 设备矩阵 (未改宿主内 Compose 渲染路径). 推送, Release 与索引准入见会话记录.

---

## 附录 A: 可识别的脚本子集与文档模型草案

### A.1 脚本形态

```js
"ui";                                   // 指令: 首个有效语句, 注释与空行可在前
const msg = "hello";                    // 顶层声明: DeclarationEvaluator 可求值 (字面量类)
const items = [{ name: "a" }];          // 同上
ui.layout(                              // 画布根: 第一个以 XML 字面量 (或单次绑定的变量) 为参数的 ui.layout
    <vertical padding="16">             // 元素, 属性 (简写或全名), 注释与空白全部按 span 保留
        <text id="txt" text="{{msg}}" size="32sp"/>   // {{}} 绑定: 宿主应用属性时求值; 设计器显示绑定值
        <text text="a {msg} b"/>        // {expr}: E4X 构造时求值; 设计器只读, 预览用求值结果或原文
        <button id="ok">确定</button>   // 文本内容 -> text 属性 (宿主 XmlConverter 规则)
        <list id="list"><horizontal><text text="{{name}}"/></horizontal></list>  // 第一个元素子节点是模板
        <console global/>               // 裸属性
    </vertical>,
);
ui.txt.on('click', () => toast("hi"));  // 不透明段, 原样保留
function showOther() { ui.layout(<frame/>); }  // 第二个字面量: 可切换的片段
```

拒绝 (带行列): 没有 `"ui"` 指令或没有 `ui.layout`; `ui.layout` 的参数不是 XML 字面量也不是单次绑定的变量 (如函数调用, 字符串拼接, `ui.inflate` 结果); XML 字面量自身不闭合或属性语法错误; 同一变量被多次赋值后传入.

### A.2 模型

```kotlin
sealed class Segment { data class Opaque(val text: String); data class Layout(val fragment: XmlFragment) }
data class UiDocument(val segments: List<Segment>, val rootFragmentIndex: Int, val activeFragmentIndex: Int, val nextId: Int)
data class XmlFragment(val root: XmlNode.Element, val span: XmlSpan, val boundTo: String?)          // boundTo: 变量名 (经变量传入时)
sealed class XmlNode { abstract val span: XmlSpan
    data class Element(val id: Int, val tag: String, val attrs: List<XmlAttr>, val children: List<XmlNode>, val selfClosing: Boolean, val startTagSpan: XmlSpan, val endTagSpan: XmlSpan?)
    data class Text(val text: String); data class Comment(val text: String); data class CData(val text: String)
    data class Expr(val source: String)   // 非属性位置的 {expr}
}
data class XmlAttr(val name: String, val parts: List<AttrPart>?, val quote: Char?, val span: XmlSpan)   // parts == null: 裸属性
sealed class AttrPart { data class Text(val text: String); data class Binding(val expression: String); data class Expr(val source: String) }
data class XmlSpan(val start: Int, val end: Int)   // 脚本文件内偏移, 行列按需换算
```

元素先序号 = 片段内只数 `Element` 的先序遍历序号, 与宿主转换后 DOM 的元素顺序一致 (`XmlConverter.handleChildren` 只遍历元素), 是客户端与宿主之间唯一的节点身份; 模型 id 只在插件内使用.

### A.3 写回规则 (D49)

- 读取后立即写回: 全部段按 span 拼接, 字节相同 (性质测试).
- SetAttr / RemoveAttr / 添加属性 / SetText / SetTag: 只重写该元素的开始标签 (或文本节点) 的 span; 属性顺序与引号不变; 引号内的同种引号写实体.
- InsertElement: 在目标位置写入 `换行 + 缩进 + 文本`; 缩进取前一个兄弟开始标签所在行的前导空白, 无兄弟时取父元素前导空白 + 4 空格; 父元素原为自闭合时展开为开闭标签.
- RemoveElement: 删除元素 span 及其前导的 "换行 + 空白"; MoveElement = Remove + Insert, 保持被移动文本原样 (缩进按新位置调整首行, 内部行相对缩进保持).
- 片段外的脚本文本从不改写; 需要新增声明或处理函数时 (待 Q19) 以不透明段追加在片段所在语句之后.

---

## 附录 B: 预览契约 v1 草案 (`plugin-api/ui-preview-api`, 包 `org.autojs.plugin.ui.preview`)

### B.1 AIDL

P10.2 (2026-10-15) 已按下面的最终形态落地 (与草案的差异: `setPointerMode` / `setHighlight` 返回 Bundle 以报告结果, `output` 标注 `@nullable`, 边界只内联不走 fd, 新增 `label` / `inputTransferToken` 键与 `diagnosticsJson`); `HASH_INPUT` 列出两个接口的全部事务, `HASH` = `680f41b5459648ff07bd1d3d9684c5279d9398b78f61c0307862f50f8dcac0b6`. 正式描述见宿主 `docs/dev/ui-preview-plugin-protocol-v1.md`.

```java
interface IUiPreviewService {
    // The transaction order is frozen; new methods are append-only.
    int getContractVersion();
    String getContractHash();
    Bundle getPreviewInfo();                                   // 宿主版本, 支持的模式, 上限, 状态
    Bundle createSession(in Bundle request, IUiPreviewCallback callback);   // -> sessionId, (embedded) SurfacePackage
    Bundle setLayout(String sessionId, in Bundle layout, in @nullable ParcelFileDescriptor output);   // 同步返回结果; snapshot 必须给 output
    Bundle resize(String sessionId, int widthPx, int heightPx); // (embedded) relayout; snapshot 记录尺寸供下次布局
    void release(String sessionId);
    Bundle reattach(String sessionId);                      // (embedded) 客户端表面重建后: 同一层级的新 SurfacePackage
    Bundle setPointerMode(String sessionId, int mode);      // 0 = 编辑 (根容器拦截指针并回报 onTap), 1 = 交互 (放行, 仅 API 33+, 否则 UNSUPPORTED_MODE)
    Bundle setHighlight(String sessionId, int elementIndex); // 宿主在根容器上画选中框, -1 清除
    // v2 (1.7.0, 只追加): Bundle runScript(String sessionId, in Bundle request); void stopScript(String sessionId);
}
oneway interface IUiPreviewCallback {
    void onBounds(String sessionId, in Bundle bounds);          // 内联 JSON (boundsJson / boundsCount / boundsTruncated / elementCount)
    void onLayoutFailed(String sessionId, in Bundle failure);   // setLayout 之外的失败 (如列表项稍后绑定): 错误码, 消息, 元素先序号, 属性名 (可选)
    void onSessionLost(String sessionId, in Bundle reason);     // stopped / timeout / service_destroyed / script_error
    void onTap(String sessionId, in Bundle tap);                // 编辑模式: x, y, 最深命中的元素先序号与类名; 后续追加 onLongPress / onDrag
    // v2: onConsole, onScriptError, onScriptExit
}
```

### B.2 Bundle 键 (全部原始类型, `SurfacePackage` 与 `IBinder` 除外)

| 键 | 方向 | 含义 |
| --- | --- | --- |
| `contractVersion` / `mode` | 请求 | 必须为 1; `embedded` / `snapshot` |
| `hostToken` (Binder) / `inputTransferToken` (Parcelable, API 35+ 优先) / `displayId` | 请求 (embedded) | `SurfaceView.getHostToken()` 或 `getRootSurfaceControl().getInputTransferToken()`, 所在显示器 |
| `widthPx` / `heightPx` / `densityDpi` / `nightMode` / `localeTag` / `fontScale` | 请求 | 逻辑屏幕尺寸与配置覆盖; 0 / 空跟随宿主 |
| `clientVersionCode` / `label` | 请求 | 插件版本 (只记日志); 运行列表中显示的名称 (<= 128 字符, 缺省 "3-Block Compose 预览") |
| `xml` / `bindingsJson` / `xmlSha256` | setLayout | 已替换 `{expr}` 的 XML 文本, `{{}}` 绑定上下文 (JSON 对象), 可选校验 |
| `sessionId` / `surfacePackage` (Parcelable) | 响应 | 会话标识, 嵌入包 |
| `outcome` / `errorCode` / `errorMessage` / `elementIndex` / `attrName` / `line` / `column` / `elementCount` / `elapsedMs` / `diagnosticsJson` | 响应 / 回调 | `applied` / `failed` / `timed_out`; 失败定位; 未中止布局的属性级诊断 `[{ "code", "message", "i", "a" }]` |
| `boundsJson` / `boundsCount` / `boundsTruncated` / `elementCount` | 回调 | `[{ "i": 先序号, "l","t","r","b", "c": 类名, "d": id 属性, "n": 实例序号 (列表项), "s": visibility }]`, 超过 5000 条截断并标记 |
| `snapshotByteLength` / `snapshotSha256` / `snapshotFormat` / `snapshotWidthPx` / `snapshotHeightPx` | setLayout 响应 (snapshot) | `argb8888-rowmajor`, 长度 = w * h * 4 |
| `x` / `y` / `elementIndex` / `className` | onTap | 编辑模式点击 |
| `reason` | onSessionLost | `stopped` / `timeout` / `service_destroyed` / `script_error` |
| `contractHash` / `hostVersionCode` / `hostVersionName` / `status` / `modes` / `interactivePointerSupported` / `imeSupported` / `maxSessionsPerClient` / `maxWidthPx` / `maxHeightPx` / `maxXmlChars` / `maxBindingsChars` / `maxBoundsEntries` / `maxSnapshotPixels` / `layoutTimeoutMs` | getPreviewInfo | 能力与上限 |

### B.3 错误码 (前缀 `ERR_AUTOJS6_UI_PREVIEW_`)

`INVALID_REQUEST`, `CONTRACT_MISMATCH`, `UNSUPPORTED_MODE` (API < 30 请求 embedded), `SESSION_LIMIT`, `SESSION_NOT_FOUND`, `XML_INVALID` (含先序号), `INFLATE_FAILED` (含先序号, 属性名), `BINDING_FAILED` (属性级诊断, 不中止), `TIMEOUT`, `OUTPUT_TRANSPORT`, `UNAVAILABLE`; v2 追加 `SCRIPT_RUNNING`, `SCRIPT_FAILED`.

### B.4 上限 (初值, P10.0 回填)

每客户端会话 2; 尺寸 <= 4096 x 4096 px; XML <= 256 K chars; 绑定 JSON <= 64 K chars; 边界条目 <= 5000 (P10.2 落地: 超过时截断并以 `boundsTruncated` 标记, 不走 fd, 因为 oneway 回调没有调用方的描述符, 而 5000 条远低于 Binder 缓冲); 快照 <= 4096 x 4096 像素 (64 MiB 原始); 单次 `setLayout` <= 5 s (Binder 线程计时, 超时返回 `timed_out` 并在主线程返回后释放会话), `createSession` <= 15 s (含引擎创建); `setLayout` 请求去抖由客户端负责, 服务对同一会话按主线程队列串行处理. P10.0 实测: 首帧 (含引擎创建) 0.5 - 1.2 s, 再次运行 0.2 s, 边界在布局后约 0.2 s 回传; 1032 x 448 快照 (1.85 MB) 绘制 44 ms, 往返 64 ms (API 24 x86 模拟器), 1080 x 1920 按像素数估算约 0.2 - 0.3 s, 快照只按视口尺寸生成.

### B.5 安全

服务只对持有 `org.autojs.permission.PLUGIN` (宿主签名级) 的调用方可见; 请求内容不拼接进任何源码 (以 `ScriptEngine.put` 注入); 合成脚本在布局预览中是常量; 会话随客户端死亡释放; 不读写调用方以外的任何文件; 日志只记会话 id, 元素数, 耗时与错误码, 不记 XML 正文.

---

## 附录 C: 属性目录生成规则 (`.python/generate_ui_catalog.py`)

- 输入: `AutoJs6-Documentation/api/ui.md` 的 "内置标签" 表 (标签, 实际视图, 用途) 与 `api/uiAttributes.md` 的全部属性表 (按二级 / 三级标题归入视图类), "AutoJs6 简写转换" 表, "属性值格式" 节的类型名; 脚本内手工维护 "视图类 -> 父类链" 表 (如 `JsButton -> Button -> TextView -> View`, `JsListView -> RecyclerView -> ViewGroup -> View`) 与 "标签 -> 面板分类 / 可容纳子节点 / 排布方向" 表.
- 输出: `app/src/main/resources/designer-ui/catalog.json` (`schemaVersion`, `docsCommit`, `tags[]`, `viewClasses{name: {parent, attrs[]}}`, `shorthands{}`, `valueTypes{}`), 一次生成, `--check` 比对; 文档站更新后重新生成并在 changelog `improvement` 记录.
- 守卫: `UiCatalogTest` (每个标签有视图类且该类在继承链中可达 View; 别名在标签集与属性集内唯一; 简写目标存在于属性集; 文档中 "明确不支持的属性" 不出现在可添加列表); 生成器自身无单元测试, 以 `--check` 的 CI 作业 (markdown.yml 同形) 守卫.
- 完整类名标签 (`<android.widget.TextView>`, `<RecyclerView>`): 不进面板; 校验按 "最近的已登记父类" 规则取属性集, 取不到时按 View / ViewGroup 并提示.

---

## 附录 D: 待决事项

### Q17 (2026-10-15, 已拍板): 预览架构

**已拍板**: (a) 宿主内嵌预览服务 (D46). 原选项: (a) 宿主服务 + `SurfaceControlViewHost` 嵌入; (b) 外置实时运行 (宿主 `ScriptExecuteActivity`, 编辑后自动重跑, 不在画布内); (c) 两者都做. 建议 (a).

### Q18 (P10.5 前): 文档页归属与命名

设计器指南页目前为 `api/composeDesigner.md`, 工程类型扩展后页名与标题 ("3-Block Compose 设计器") 是否保留; 新建对话框中工程类型的用户可见名称 ("UI (XML) 工程" / "传统 ui 工程" / "XML 布局工程"). 建议: 页名不变, 标题改为 "3-Block Compose 设计器 (Compose 与 UI 工程)", 新增一章; 工程类型名 "UI (XML) 工程".

**已拍板 (2026-10-15, D53)**: 按建议实施, 见 P10.5.

### Q19 (P10.4 前): XML 工程的逻辑生成范围

Compose 工程有 D37 (事件处理函数桩, state 绑定自动声明). XML 工程是否: (a) 不生成任何脚本文本 (只编辑字面量, 事件与绑定在宿主编辑器写); (b) 为选中元素一键追加 `ui.<id>.on('click', () => { })` 桩到片段所在语句之后 (不透明段, 之后不再跟踪); (c) 同时支持从绑定值面板新增顶层声明. 建议 (b), 最小且可逆.

**已拍板 (2026-10-15, D52)**: 按建议 (b). 实施见 P10.4: `UiScriptReader` 记录每个 `ui.layout` 片段的桩插入点 (`XmlFragment.stubOffset` / `stubIndent`, 语句之后并越过紧随的处理函数语句) 与脚本已有的处理函数 (`UiDocument.handlers`), `UiEditCommand.AddHandlerStub` 只向该片段之后的不透明段插入文本, 属性抽屉的 "事件" 区按标签 (`UiEvents`: 通用 click / long_click / touch / key, 复合按钮 check, 列表 item_click / item_long_click / item_bind, canvas draw) 列出并显示 "脚本中已处理". (c) 未做, 绑定值面板沿用 `bindingOverrides` 只覆盖画布.

### Q20 (P10.2 前): 预览运行时在宿主中的可见性

布局预览的合成脚本经 `ScriptEngineService` 运行, 会出现在宿主 "正在运行" 列表. (a) 列表中隐藏 (以预览标识过滤); (b) 显示并标注 "3-Block Compose 预览"; (c) 不经 `ScriptEngineService`, 直接构造 `ScriptRuntime` (需要宿主新增工厂, 绕过执行记录). 建议 (b): 可见但标注, 用户能从宿主侧停止, 不新增旁路.

**P10.2 实施 (2026-10-15, 按建议 (b), 待维护者确认)**: 预览运行时经 `ScriptEngineService.execute(StringScriptSource(label, "\"ui\";"), listener, config)` 进入, `ExecutionConfig.uiContentHost` (宿主持有, 不从脚本参数接受) 让服务分派到新的 `UiPreviewScriptExecution` (主线程引擎, 预览根容器取代 Activity), 因此以客户端传入的 `label` (缺省 "3-Block Compose 预览") 出现在运行列表, 从宿主侧停止时 `LoopBasedJavaScriptEngine.forceStop` 经 `UiContentHost.onEngineForceStop` 释放会话并回调 `onSessionLost(stopped)`; spike 的 `getScriptEngineManager()` 旁路未保留. 合成脚本固定为 `"ui";`, XML 与绑定值由 Kotlin 直接作用于运行时 (`UI.layout` 与 `NativeJSON.parse`), 不再如条目草案那样经 `ScriptEngine.put` 注入变量.

### Q21 (P10.0 后): 嵌入模式的输入法最低 API

若 spike 显示 API 30 - 32 的嵌入树不能承载输入法: (a) 这些 API 的 `<input>` 在预览中提示并禁用输入; (b) 整个嵌入模式要求 API 33+, 30 - 32 用快照; 建议 (a).

**已拍板 (2026-10-15, D50)**: 按三级门禁实施 (嵌入显示与布局预览 API 30+; 交互预览 API 33+; 预览中的输入法 API 35+, API 33 - 34 的 `<input>` 提示并禁用输入; API 24 - 29 快照).

**P10.0 结论 (2026-10-15)**: 实测 API 30 / 33 的嵌入树不能承载输入法 (`canReceiveKeys=false`), API 35 (小米平板) 与 API 37 (模拟器) 以 `InputTransferToken` 构造后可以; 另外 API 30 - 32 的弹出层会让宿主进程崩溃. 建议改为三级门禁: 嵌入显示与布局预览 API 30+; 交互预览 (放行指针) API 33+; 预览中的输入法 API 35+, API 33 - 34 的 `<input>` 按 (a) 提示并禁用输入; API 24 - 29 快照.

---

## 附录 E: 验证顺序与设备矩阵

在 `AGENTS.md` 第 14 节的基础上, 本阶段新增:

```powershell
py .python/generate_ui_catalog.py --check
py .python/verify_ui_preview.py --serial <serial> --apk <release apk> --host-apk <含预览服务的宿主 debug 包>   # 嵌入 (API 30+) 或快照 (API 24 - 29): 新建 UI 工程 -> 等首帧边界 -> 面板插入 button -> 边界数 + 1 -> 保存 -> 删除
```

| 设备 | 模式 | 覆盖 |
| --- | --- | --- |
| AVD_API_24 实例 (Nexus 5 尺寸) | snapshot | P10.1 - P10.4 设备测试, 快照耗时 |
| G8441 (API 28, 横屏 640 x 360 dp) | snapshot | 真机快照, 分栏布局 |
| AVD_API_30 | embedded | 全套 instrumentation, spike, 布局预览 (交互预览按 D50 需 API 33+, 在此只验证开关不出现) |
| AVD_API_33 (4 GiB, 端口 5580) | embedded + runScript | P10.6 宿主 `UiPreviewServiceTest` 运行项与插件 `UiInteractiveDeviceTest` |
| AVD_API_35_16K (只读实例, 端口 5584) | embedded + runScript + IME | P10.6 同上, API 35 的输入法路径 |
| Redmi 22120RN86C (API 33) | embedded | 真机触摸 / 输入法 / 弹出层, `notifyConfigurationChanged` |
| Xiaomi Pad 23046RP50C (API 35) | embedded | `InputTransferToken` 路径 (若采用), 平板分栏 |
| AVD API 37 | embedded | `LayoutParams(focusable)` 与 16 KB 页 |

宿主需以 `:app:assembleAppDebug` 构建含预览服务的 debug 包并覆盖安装 (同签名保留数据, 维护者已授权按需覆盖); 两个仓库的提交号与构建号写入证据. 2026-10-15 (P10.6) 补充: 另一会话转达维护者约定 "不要把宿主 debug 包装到真机上", 本阶段起真机只保留维护者安装的发行宿主, 含预览服务的验证在模拟器 (API 24 / 30 / 33 / 35) 完成, 真机项待宿主发行后补. 性能: 记录 `setLayout` 到 `onBounds` 的 p50 / p90 (52 示例), 快照传输耗时, 与 compose 画布的 undo / redo 到首帧对照; 暂不设复核线. 体积: 新契约 AAR 与客户端的增量按 AGENTS 第 14 节复核线记录.
