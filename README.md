<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>为 AutoJs6 脚本提供 Jetpack Compose 与 Material 3 界面渲染能力的插件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 语言

******

本文档支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### 简介

******

3-Block Compose 是 AutoJs6 的界面渲染插件. 脚本通过宿主提供的 `compose` / `$compose` 入口声明界面, 插件在宿主进程内以 Jetpack Compose 与 Material 3 完成渲染. 当前预览同时支持 `"ui";` 模式的 Activity 内容与非 ui 脚本的悬浮窗.

插件内置可视化设计器与组件画廊: 从启动器打开即进入设计器首页, 可新建或导入脚本工程并在真实渲染器上编辑界面, 画廊则展示全部组件的预览与示例脚本; 脚本界面的渲染仍在宿主进程中进行. 宿主通过 INFO 服务发现插件并读取版本与兼容信息, 再按契约 (`org.autojs.plugin.compose.api`) 在自身进程内实例化渲染器工厂.

******

### 当前状态

******

1.8.0 是当前公开发行版 (GitHub Release, 已登记官方插件索引). 需要 AutoJs6 6.8.0 (5322) 或更高版本中包含 3-Block Compose 新身份的构建 (宿主提交 f12665ffde 之后); 发布时 AutoJs6 6.8.0 尚未正式发布, 需要使用包含该提交的 AutoJs6 构建. AutoJs6 编辑器菜单的 "在 3-Block Compose 设计器中打开" 需要宿主提交 db7e6a1734 之后的构建, 设计器的 TSX 工程需要 TypeScript Engine 0.7.0, UI (XML) 工程的布局预览需要包含预览服务的构建 (宿主提交 306a8f7834 之后). 本插件提供 UI 页面, 悬浮窗, 可视化设计器 (Compose 与 UI 工程), 五个示例, API 参考, 设计器指南页与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 图标已采用维护者提供的正式图稿. 1.7.0 起 UI (XML) 工程的交互预览由 AutoJs6 真实运行整份脚本, 需要包含预览契约 v2 的 AutoJs6 构建 (宿主提交 4c332c2053 之后) 与 Android 13 或更高版本. 1.8.0 起 UI 工程的布局可以写在 XML 文件中并成为文件夹工程: 预览使用工程文件需要预览契约 v3 (宿主提交 9df0a154b 之后), 在 AutoJs6 中运行或导入文件夹工程需要宿主提交 faf412ef7 之后.

******

### 功能特性

******

当前开发预览的核心能力:

- 可视化设计器: 脚本文件即工程, 真实渲染器所见即所得地预览界面树, 大纲 / 组件面板 / 属性抽屉覆盖全部 55 个组件, 支持撤销重做与草稿恢复; 导出的脚本可复制或直接发送到 AutoJs6 运行; 画布拖放按指针位置插入, 打开的弹层画在画布内, Image 可显示 @drawable 与工程图片目录中的图片, 状态面板可临时改变 state 的预览值, 交互预览在受限的沙箱中运行 JavaScript 工程的脚本 (Android 8.0 或更高版本), 点击与输入按脚本逻辑更新画布, 控制台显示脚本的输出与错误; 可新建 JavaScript 或 TSX 工程, TSX 工程经 TypeScript Engine 0.7.0 或更高版本解析, 生成规范的 TSX
- 与 AutoJs6 衔接: 工程可发送运行, 在 AutoJs6 编辑器中打开 (双向同步), 导入 AutoJs6 脚本目录, 复制或导出; 设计器也能从 AutoJs6 编辑器或文件管理器直接打开 .js / .tsx 文件并保存回原文件; 导入的工程可在编辑页菜单中手动写回来源或从来源重新导入
- UI (XML) 工程: 设计器也能打开传统 "ui" 模式脚本 (E4X XML 布局) 并原样编辑, 未触碰的字节 (空白, 注释, 属性顺序, 引号, {表达式}) 保持不变; 画布由 AutoJs6 的预览服务用 ui 模式的布局器绘制 (Android 11 起嵌入真实视图, 更低版本为位图快照), 属性抽屉按文档站的标签与属性目录编辑, 组件面板列出 48 个标签, 可为元素追加 `ui.<id>.on` 事件桩, 绑定值面板临时覆盖 `{{ }}` 的值
- UI (XML) 工程的交互预览 (1.7.0): 画布上方的 "交互预览" 开关打开后, AutoJs6 把整份脚本当作一次 ui 模式运行来执行, 画布显示脚本的 ui.layout 并直接接收点击与触摸, 事件处理函数, `{{ }}` 绑定与其它模块都按脚本逻辑真实运行, 控制台页显示输出, 错误与结束原因; 修改工程后约 0.4 秒重新运行, 关闭开关或离开设计器即停止, 文档与文件不变
- XML 布局文件与文件夹工程 (1.8.0): UI 工程的布局可以写在 XML 文件中 (ui.layoutFile, ui.inflate(files.read(...)), AutoJs6 XML 或 Android 原生 XML); 带布局文件的工程是含 main.js 与 project.json 的文件夹, 可导出导入为 zip 或文件夹, 并作为项目交给 AutoJs6 运行或导入; Android 原生 XML 按 AutoJs6 实际设置的属性编辑
- 声明式界面: `compose.state` + `compose.mount(render)` 按状态变化自动重绘, 同时提供可长期持有的节点句柄 (`compose.Text({...})` 等) 直接修改属性与子节点
- Material 3 核心集: 29 个节点工厂覆盖布局, 文本, 图标, 图片, 按钮, 输入, 选择控件, 惰性列表, 对话框与进度; Snackbar 是会话命令, 不是 compose.Snackbar 工厂
- 链式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作顺序, 作用域操作在宿主侧校验
- 两种承载方式: `"ui";` 脚本通过 `compose.mount` 或可调用的 `compose` / `$compose` 挂载 Activity 内容; `compose.floaty` 创建 raw 或可调整悬浮窗, 同样支持非 ui 脚本
- 进程内渲染: 插件在宿主进程应用界面更新, 事件排队回到所属脚本线程; 工作线程通过 compose.post 请求更新
- 一个 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 无插件自有原生代码; 随包的 AndroidX graphics-path 辅助库仍需满足 Android, 宿主与插件兼容条件
- 原生文本编辑保留选区与输入法组合状态, 支持焦点和显式编辑, 拒绝覆盖较新输入的延迟编辑; 开关与滑块仍由脚本控制状态
- 集成守卫: 插件缺失或不兼容时可用性探测返回不可用, 错误使用 `ComposeError`, 关闭会话或停止脚本会释放所属窗口与回调
- 计数器, 表单校验, 1000 项键控列表, 非 ui 悬浮 HUD 与主题五个可运行示例, 包含前置条件与索引, 同步至匹配宿主的 3-Block Compose 示例分类
- TSX 支持 `<compose.Column>`, `<compose:Text>`, 节点工厂引用, Fragment, 插槽及响应式回调; 同一棵树不能混用 Compose 与旧 XML 节点
- XML `<compose>` 容器与 compose.attach 可在 UI 页面或旧悬浮窗内嵌入独立 Compose 会话; compose.AndroidView 可承载现有 Android View 或同步工厂返回的 View
- compose.dialog 返回可更新和关闭的会话, 支持普通对话框与模态底部弹层, 可在 UI 或普通脚本中使用
- Material 3 扩展组件: 导航栏与抽屉, 标签页, 底部弹层与菜单, 日期与时间选择器, 分页与网格, 芯片, 徽标, 分段按钮, 悬浮按钮, 搜索栏, 提示与下拉刷新
- 组件画廊: 启动器入口展示全部 55 个组件的 Material 3 预览与可运行示例脚本, 可复制或发送到 AutoJs6; 设置页的语言, 夜间模式与主题色默认跟随 AutoJs6, 并提供四种启动器图标

******

### 使用方式

******

1. 安装包含 compose 脚本入口的匹配本地 AutoJs6 构建 (最低 6.8.0 / 5322)
2. 安装本插件 APK; 从启动器打开 3-Block Compose 进入设计器首页, 可新建脚本工程, 或从顶部栏打开组件画廊与设置
3. 在 AutoJs6 的插件中心确认 3-Block Compose 已被识别并处于启用状态
4. 在脚本中使用 `compose` 或 `$compose`; 通过 `compose.mount` 挂载 Activity 内容, 或先授予宿主悬浮窗权限再使用 `compose.floaty`
5. 编辑页菜单提供发送运行, 在 AutoJs6 编辑器中打开, 导入 AutoJs6 脚本目录, 复制与导出; 在 AutoJs6 编辑器菜单 (需 AutoJs6 包含对应更新) 或文件管理器中选择 3-Block Compose 也可以直接编辑脚本文件

******

### 快速开始

******

以下计数器与悬浮窗 HUD 均可在匹配的本地预览宿主中运行. 运行 HUD 前需授予宿主显示在其他应用上层的权限:

```js
"ui";

// 计数器 (声明式 render 层)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `已点击 ${count.value} 次`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, '加一'),
]));
```

```js
// 悬浮窗 HUD (节点句柄层)
let worker = null;
let status = compose.Text({ text: '准备中...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, '关闭'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `进度 ${i}%`;
        });
    }
});
```

五个可运行脚本由 assets/examples/index.json 列出, 同步至匹配宿主的 3-Block Compose 示例分类. 每例头部说明模式与权限. 节点, 修饰链, 主题, 会话与悬浮窗详情请使用同批本地 API 文档及 TypeScript/编辑器声明; 在线站点不一定已包含这些本地变更.

******

### 兼容性

******

插件的运行要求与限制:

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更高; 低于该版本的宿主会在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 处理器架构: arm64-v8a / armeabi-v7a / x86_64 / x86 (单一 APK 内置全部四种, 无需按架构选择安装包)
- Compose 版本: 由插件自带 (BOM 2026.09.00), 不依赖宿主的 Compose 运行时
- 契约版本: 2; 宿主与插件通过契约版本协商, 不匹配时拒绝加载并给出明确错误
- 打包应用仍需另外安装兼容的 3-Block Compose 插件, 启用/授权记录属于该应用; 兼容性检查依据内置 AutoJs6 运行时, 不是打包应用自身的 versionCode
- View 工厂在渲染前于主线程执行. 无效替换保留当前内容; 同一 View 不可属于两个节点, 也不会从其他父视图被抢占. 借用的 View 保留原有监听器, 外部资源仍由调用方管理
- cancelable=false 同时禁止返回键, 点击外部和下滑关闭; 主动关闭与脚本退出仍会清理弹窗, 保留已有页面和其他会话
- 本构建使用 3-Block Compose 契约 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用旧 V1 渲染器的原有组件, 宽集组件需要 V2 渲染器
- compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中复用依赖未变化的片段; 本插件无需更新, TSX 配套需要 TypeScript Engine 0.6.8
- 契约类 (org.autojs.plugin.compose.api) 自 1.2.0 起随插件打包供独立进程使用; 在 AutoJs6 内仍由宿主提供同名类, 插件副本不生效
- 设计器在插件自身进程运行, 不依赖 AutoJs6; 发送运行, 在 AutoJs6 编辑器中打开与导入 AutoJs6 脚本目录需要已安装 AutoJs6, 编辑器菜单的 "在 3-Block Compose 设计器中打开" 需要包含该入口的 AutoJs6 构建
- 设计器的 TSX 工程需要 TypeScript Engine 0.7.0 或更高版本 (提供语法树解析服务), 发送运行时由 AutoJs6 经 TypeScript Engine 编译; 未安装或版本过低时 TSX 工程以只读源码方式打开, 可查看与复制
- 设计器在交互预览中运行脚本需要 Android 8.0 (API 26) 或更高版本 (沙箱中的 JavaScript Date 基于 java.time); 更低版本的设备上, JavaScript 工程按 TSX 工程的方式预览
- UI (XML) 工程的布局预览需要包含预览服务的 AutoJs6 6.8.0 构建 (宿主提交 306a8f7834 之后; 插件按服务是否存在判断, 不按版本号), Android 11 (API 30) 起画布嵌入 AutoJs6 绘制的真实视图, 更低版本显示位图快照; 没有该服务时 UI 工程仍可编辑, 画布说明原因
- UI (XML) 工程的交互预览需要包含预览契约 v2 的 AutoJs6 6.8.0 构建 (宿主提交 4c332c2053 之后) 与 Android 13 (API 33) 或更高版本; Android 13 - 14 上画布中的输入框收不到键盘输入, Android 15 起可以; 不满足条件时不显示开关, 画布上方说明原因, 可改用 "在 AutoJs6 中运行"; 只有契约 v1 的 AutoJs6 构建仍可提供布局预览
- 带布局文件的 UI 工程 (1.8.0): 预览使用工程文件 (res 资源, 布局文件) 需要包含预览契约 v3 的 AutoJs6 6.8.0 构建 (宿主提交 9df0a154b 之后), 在 AutoJs6 中运行或导入文件夹工程需要宿主提交 faf412ef7 之后; 更早的构建仍按 XML 文本预览, 两个去向换成一行提示

******

### 常见问题

******

- 画廊里的示例如何运行? 点击 "在 AutoJs6 中运行" 会把脚本交给已安装的 AutoJs6 执行; 画廊自身只展示预览与代码, 不执行脚本
- 设计器能打开任意脚本吗? 它只解释顶层的 `compose.state` 声明与唯一的 `compose.mount` / `floaty` / `dialog` 调用, 其它语句按源码原样保留; 根节点不是 compose 工厂, 多处挂载, 渲染函数无 return 等无法映射的结构会拒绝打开并指出行列; TSX 工程中 `compose.X` / `compose:X` 标签是可编辑的节点, 其它标签与 `{}` 表达式按原文保留, XML 标签与展开属性会拒绝
- 设计器会执行脚本吗? 只在交互预览中. 编辑时它只解析脚本 (JavaScript 用 Rhino 解析器, TSX 用 TypeScript Engine 的解析服务) 并用插件自带的渲染器预览, 事件处理函数不会执行, state 显示初值或状态面板中设置的预览值. 打开交互预览后, JavaScript 工程的脚本在插件内受限的沙箱中运行 (需要 Android 8.0 或更高版本): 只提供 compose, console, toast 与定时器, 不能访问 Java / Android 类, 文件, 网络或其它 AutoJs6 模块, 单次运行超过约 3 秒会被停止, 运行结果只显示在画布上, 不会修改工程. TSX 工程 (以及 Android 8.0 以下设备上的工程) 不运行脚本, 绑定了 state 的控件只改变预览值. 真正运行脚本要交给 AutoJs6. UI (XML) 工程的画布由 AutoJs6 的预览服务只做布局 (创建视图并绘制), 不运行工程的脚本; `{{ }}` 绑定的值来自脚本顶层的字面量声明 (在沙箱中求值) 或绑定值面板的覆盖. 1.7.0 起 UI (XML) 工程的交互预览不同: 打开开关后 AutoJs6 会像 "在 AutoJs6 中运行" 一样真实运行整份脚本 (没有沙箱的限制, 副作用真实发生, 每个工程首次打开时先确认), 只是 ui.layout 的内容显示在画布上; 结果仍不写入工程
- 为什么找不到 `compose`? 全局对象由匹配的本地宿主构建提供, 单独安装插件 APK 不会添加该入口
- 是否需要卸载其它界面插件? 不需要, 3-Block Compose 与现有 `ui` 模块及其它插件互不影响
- 插件变化时会怎样? 更新, 卸载或停用插件会关闭活动会话并报告对应错误; 兼容且启用的插件允许重新挂载
- 悬浮窗需要什么条件? 先授予宿主悬浮窗权限, 输入文字前调用 `window.requestFocus()`. 若 HyperOS 未显示窗口, 请先回到桌面. 缺少权限会返回 PERMISSION_REQUIRED, 不会自动弹出授权界面
- 能否使用 TSX 或任意 Compose 函数? 配合匹配的宿主与 TypeScript Engine, TSX 可使用文档列出的 Compose 节点工厂. 不支持任意 Kotlin Composable 函数或自定义 TSX 组件
- 旋转是否丢失状态? 当前宿主自行处理普通方向变化, 保留脚本引擎. 真实的 Activity 重建或销毁会关闭该引擎及所属会话, 不自动恢复业务状态
- 选择器如何查找组件? testTag 按原样暴露为 ID, 不添加包名前缀. id/testTag 与 desc/contentDescription 是不同信息; Button 的文字可能是子节点, 必要时沿 parent() 查找可点击祖先

******

### 权限与安全

******

插件不申请任何 Android 运行时权限, 也不访问网络, 存储或传感器.

- 组件保护: Wake Activity 与 INFO 服务均受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 宿主可以访问; 设计器编辑页是无权限的导出组件, 只接收用户交出的脚本文档 Uri, 读写都限于该 Uri
- 后台行为: 插件没有常驻服务与定时任务, 仅在自身被更新时接收一次系统广播以校正启动器图标组件; 不被宿主加载且未打开设计器或画廊时不消耗资源
- 数据边界: 渲染器不读写脚本数据与用户文件, 界面状态只存在于宿主进程内存中; 设计器的工程与草稿存于插件私有目录, 只经系统文件选择器导入导出, 或按用户交出的 Uri 读写单个脚本文件; 工程目录仅通过插件自己的内容提供器在交给 AutoJs6 时按次授权; 导入时会保留所选文件的访问权限 (提供方允许时), 但只在用户选择 "写回来源" 或 "从来源重新导入" 时读写该文件
- 脚本沙箱: 设计器的交互预览在插件进程内以 Rhino 解释器运行 JavaScript 工程的脚本, 只提供 compose, console, toast 与定时器; 不能访问 Java / Android 类, 文件, 网络或其它 AutoJs6 模块, 每次运行都有指令数与时长上限, 运行结果只用于画布, 不会写入工程
- 备份策略: 已禁用应用备份与设备迁移, 插件本身不持有任何需要迁移的数据
- 布局预览: UI (XML) 工程的画布由已安装的 AutoJs6 绘制: 插件只把 XML 片段与绑定值经 AutoJs6 的预览服务 (受 PLUGIN 签名权限保护) 交给它, AutoJs6 以 ui 模式的布局器创建视图并画回, 不运行工程的脚本, 也不能访问插件的工程文件; 该预览运行时出现在 AutoJs6 的运行列表中, 可随时停止; 1.8.0 起, 对支持的 AutoJs6 (预览契约 v3), 插件还会交出工程文件的只读 zip 副本 (文件夹工程, 或脚本文件旁的 res 目录), 供布局与运行解析资源
- 交互预览 (UI 工程): 打开开关后, 插件把脚本文本交给 AutoJs6 的预览服务, AutoJs6 以普通 ui 模式运行它 (与 "在 AutoJs6 中运行" 相同的引擎与脚本权限, 副作用真实发生), 只把 ui.layout 的内容显示在画布上并把控制台输出回传; 每个工程首次打开时需要确认, 运行出现在 AutoJs6 的运行列表中, 关闭开关, 离开设计器或从 AutoJs6 停止即结束; 脚本不在插件进程中运行, 插件也不扩大脚本的权限
- 文件夹授权与工程 zip (1.8.0): 以内容 Uri 打开的脚本只在用户于系统文件夹选择器中选定, 且核对含有该脚本的文件夹里读写布局文件 (只保留该文件夹的读写授权); 文件夹工程以 zip 经插件自己的内容提供器按次授予读取权限交给 AutoJs6, AutoJs6 核对每个条目都在工程内后才解压

宿主加载渲染器时沿用自身的脚本权限模型, 插件不扩大脚本可访问的系统能力.

******

### 插件接口

******

面向宿主的接口标识如下:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

宿主通过 `org.autojs.plugin.INFO` 发现插件并读取 `requiresHostVersion` 等能力信息; 渲染器工厂类名由 `org.autojs.plugin.compose.RENDERER_FACTORY` 元数据声明, 宿主以插件 APK 路径创建类加载器 (父加载器为宿主) 并在宿主进程内实例化.

******

### 路线图

******

插件的里程碑, 设计决策与验收标准统一记录在路线图中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### 版本历史

******

#### v1.8.0

_2026/10/16_

- `提示` 1.8.0 的部分功能需要更新的 AutoJs6 6.8.0 构建: 预览使用工程文件 (res 资源, 布局文件) 需要宿主提交 9df0a154b 之后 (预览契约 v3), 在 AutoJs6 中运行或导入文件夹工程需要宿主提交 faf412ef7 之后; AutoJs6 版本较旧时设计器会提示并退回原有行为
- `新增` UI (XML) 工程的布局可以写在单独的 XML 文件中: 以字面量路径调用 ui.layoutFile("...") 与 ui.inflate(files.read("...")), 可以是 AutoJs6 XML 或 Android 原生 XML (ui.useAndroidLayout); 设计器读取并编辑这些文件, 未改动的内容原样保留, 片段切换器以文件名列出它们
- `新增` 片段切换器旁的菜单 "存为 XML 文件" 与 "新建 XML 文件": 前者把直接写在 ui.layout(...) 或 ui.inflate(...) 中的布局移到文件并改写调用, 后者新建只含一个空容器的文件 (AutoJs6 XML 或 Android 原生 XML) 并在脚本末尾追加 ui.inflate 一行; 两者都可撤销, 保存时才写文件
- `新增` 文件夹工程: 带布局文件保存的私有工程变为文件夹 (main.js, 布局文件与 AutoJs6 认可的 project.json), 可在 AutoJs6 中作为项目运行与打包; 新建 UI 工程时也可以直接创建这样的文件夹 (AutoJs6 XML 或 Android 原生 XML 模板); 首页以 "名称/" 显示文件夹工程
- `新增` 文件夹工程可导出为一个 zip, 首页菜单可导入 zip 或文件夹 ("导入脚本或 zip", "导入文件夹")
- `新增` "在 AutoJs6 中运行" 与 "导入 AutoJs6 脚本目录" 把带布局文件的工程以 zip 交给 AutoJs6: 运行时解压到缓存目录按项目运行, 结束后清理; 导入时解压到同名文件夹, 已存在时询问覆盖或另存为新文件夹
- `新增` 从 AutoJs6 编辑器打开的脚本可以读写同一文件夹中的布局文件: 第一次需要在系统文件夹选择器中选择脚本所在的文件夹, 之后保留授权
- `新增` Android 原生 XML 布局按 AutoJs6 实际支持的属性编辑: 属性抽屉列出各控件类可设置的 android: / app: 属性与父布局的参数, id 写作 @+id/..., 组件面板插入 Android 控件类, 问题列表提示 AutoJs6 会忽略或无法设置的属性
- `新增` UI 工程的问题列表单列 "预览差异": 脚本中在预览里与在 AutoJs6 中运行行为不同的写法 (activity, ui.emitter, 窗口级函数, 对话框, 主线程上的无限循环等), 每条带行号与说明, 点按打开文档, 不计入错误数
- `优化` 布局预览与交互预览把工程文件交给 AutoJs6, 布局可以引用 res 中的字符串, 颜色, 尺寸, 样式, 图片与 `<include>` 布局, 交互预览中的 ui.layoutFile 也能正常运行 (需要预览契约 v3); AutoJs6 版本较旧时问题列表提示预览可能与实际运行不同
- `优化` 向只含空白的容器插入元素时不再留下空行
- `依赖` 升级 ui-preview-api.aar 契约制品至 ui 预览契约 v3 (追加随布局交出工程资源与随运行交出工程 zip 的请求键; 只有 v2 或 v1 的 AutoJs6 构建仍提供原有预览)

#### v1.7.0

_2026/10/15_

- `提示` UI (XML) 工程的交互预览现在由 AutoJs6 真实运行整份脚本 (需要包含预览契约 v2 的 AutoJs6 6.8.0 构建, 宿主提交 4c332c2053 之后, 以及 Android 13 或更高版本): 脚本的副作用与在 AutoJs6 中运行相同, 每个工程首次打开开关时会先确认; 不满足条件时画布上方说明原因, 可改用 "在 AutoJs6 中运行"
- `新增` UI (XML) 工程的交互预览: 打开画布上方的 "交互预览" 开关后, AutoJs6 把整份脚本当作一次 ui 模式运行来执行, 画布显示脚本的 ui.layout 并直接接收点击与触摸, 事件处理函数, {{ }} 绑定, 定时器与其它模块都按脚本逻辑真实运行; 修改工程后约 0.4 秒重新运行, 关闭开关, 设计器转到后台或关闭编辑页时停止, 画布回到布局预览; 文档, 工程文件与草稿都不会被改动. Android 13 - 14 上画布中的输入框收不到键盘输入, Android 15 起可以
- `新增` UI 工程的 "控制台" 页在交互预览期间显示 AutoJs6 运行脚本的输出 (按等级着色), 运行状态与结束原因; 脚本出错时画布上方显示错误 (带行号), 点击打开控制台; 运行以工程文件名出现在 AutoJs6 的运行列表中, 也可以从那里停止
- `修复` 设计器打开 UI 工程时偶尔显示 "AutoJs6 预览服务现在不可用 (at most 2 sessions per client)" 的问题: 画布尺寸在布局稳定前变化两次时, 插件会向 AutoJs6 重复创建预览会话, 触到每个客户端的会话上限; 现在同一时间只创建一个会话, 期间的尺寸与外观变化随后补上
- `依赖` 升级 ui-preview-api.aar 契约制品至 ui 预览契约 v2 (在 v1 之后追加运行与停止脚本的接口和控制台 / 错误 / 结束回调; 只有 v1 的 AutoJs6 构建仍可提供布局预览)

#### v1.6.0

_2026/10/15_

- `提示` 设计器新增 UI (XML) 工程: 可以打开与编辑传统 "ui" 模式脚本 (E4X XML 布局), 画布由 AutoJs6 绘制. 布局预览需要包含预览服务的 AutoJs6 6.8.0 构建 (宿主提交 306a8f7834 之后), Android 11 (API 30) 起画布嵌入 AutoJs6 绘制的真实视图, 更低版本显示位图快照; 没有该服务时仍可编辑, 画布说明原因
- `新增` 设计器支持 UI (XML) 工程: 新建工程时可选 "UI (XML)", 导入或从 AutoJs6 编辑器 / 文件管理器打开 "ui" 模式脚本也按 UI 工程读取; 首页列表以 JS / TSX / UI 标注工程类型. 脚本中的 XML 布局以带位置的语法树保存, 未触碰的部分 (空白, 注释, 属性顺序, 引号, {表达式}) 逐字节保留, 只有被编辑的开始标签, 文本与新插入的元素会被改写; 脚本里的其它 XML 字面量也能切换到画布编辑
- `新增` UI 工程的编辑页: 大纲 (折叠, 上下移, 删除, 长按拖拽, 多个布局时切换), 属性抽屉 (标签, id, 文本内容, 已设置的属性与按 View 类分组的目录属性, 文本 / `{{ }}` 绑定 / {表达式} 三态, 裸属性, 任意属性名), 组件面板 (文档的 48 个标签, 按分类列出并带默认属性), 绑定值面板 (`{{ }}` 名称的求值结果与临时覆盖, 只影响画布), 问题列表 (按文档站的标签与属性目录校验) 与控制台 (AutoJs6 绘制布局时报告的诊断); 保存, 草稿恢复, 来源同步, 发送运行, 在 AutoJs6 编辑器中打开, 导入 AutoJs6 脚本目录, 复制与导出对 UI 工程同样可用
- `新增` UI 工程的画布由 AutoJs6 绘制 (布局预览): 插件把 XML 片段与绑定值交给 AutoJs6 的预览服务, AutoJs6 用 ui 模式的布局器创建真实视图并画回画布, 不运行工程的脚本; 点选与拖拽在画布上照常工作, 绘制失败时定位到出错的元素; 预览运行时以 "3-Block Compose 预览" 出现在 AutoJs6 的运行列表中, 可从那里停止
- `新增` UI 工程的属性抽屉可以为选中元素添加事件: 在 ui.layout 调用之后追加一段 `ui.<id>.on('事件', ...)` 处理函数桩 (元素没有 id 时自动补一个), 处理逻辑在脚本编辑器中编写; 脚本已注册的事件显示为 "脚本中已处理"
- `依赖` 附加 ui-preview-api.aar 契约制品 (AutoJs6 ui 布局预览契约 v1), 供设计器请求 AutoJs6 绘制 UI 工程的画布

##### 更多版本历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

******

### 项目编译与构建

******

克隆仓库后可直接使用 Gradle Wrapper 构建, 所需的 Android Gradle 插件与 Kotlin 版本由平台版本插件按当前 IDE 环境自动选定.

构建调试包:

```powershell
.\gradlew.bat :app:assembleDebug
```

运行 JVM 单元测试并打包设备契约测试:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

构建发布包 (需要 `sign.properties` 与签名密钥):

```powershell
.\gradlew.bat :app:assembleRelease
```

校验签名并生成带摘要后缀的发布文件:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校验多语言文档与源文件一致:

```powershell
py .python\generate_markdown.py --check
```

构建需要 JDK 21 及以上. 修改 `.readme` 或 `.changelog` 目录下的源文件后, 运行 `py .python\generate_markdown.py` 重新生成所有文档.

******

### 文档资源布局

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

README, 插件中心说明与更新日志均由 `.readme` 与 `.changelog` 目录下的 JSON 源文件生成, 请勿直接编辑生成的 Markdown 文件.

******

### 许可证

******

本项目基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE) 发布. 第三方组件的许可信息见 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目主页: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- compose 模块文档: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
