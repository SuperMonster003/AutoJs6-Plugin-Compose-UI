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

1.4.0 本地开发预览: 需要包含 3-Block Compose 新身份的 AutoJs6 宿主构建 (宿主提交 f12665ffde 之后), 并安装和启用本插件; AutoJs6 编辑器菜单的 "在 3-Block Compose 设计器中打开" 需要宿主提交 db7e6a1734 之后的构建. 本地配套提供 UI 页面, 悬浮窗, 可视化设计器, 五个示例, API 参考, 设计器指南页与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 当前未登记官方索引, 尚无官方发行版. 图标已采用维护者提供的正式图稿.

******

### 功能特性

******

当前开发预览的核心能力:

- 可视化设计器: 脚本文件即工程, 真实渲染器所见即所得地预览界面树, 大纲 / 组件面板 / 属性抽屉覆盖全部 55 个组件, 支持撤销重做与草稿恢复; 导出的脚本可复制或直接发送到 AutoJs6 运行; 画布拖放按指针位置插入, 打开的弹层画在画布内, Image 可显示 @drawable 与工程图片目录中的图片, 状态面板与交互预览可临时改变 state 的预览值; 可新建 JavaScript 或 TSX 工程, TSX 工程经 TypeScript Engine 0.7.0 或更高版本解析, 生成规范的 TSX
- 与 AutoJs6 衔接: 工程可发送运行, 在 AutoJs6 编辑器中打开 (双向同步), 导入 AutoJs6 脚本目录, 复制或导出; 设计器也能从 AutoJs6 编辑器或文件管理器直接打开 .js / .tsx 文件并保存回原文件; 导入的工程可在编辑页菜单中手动写回来源或从来源重新导入
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

******

### 常见问题

******

- 画廊里的示例如何运行? 点击 "在 AutoJs6 中运行" 会把脚本交给已安装的 AutoJs6 执行; 画廊自身只展示预览与代码, 不执行脚本
- 设计器能打开任意脚本吗? 它只解释顶层的 `compose.state` 声明与唯一的 `compose.mount` / `floaty` / `dialog` 调用, 其它语句按源码原样保留; 根节点不是 compose 工厂, 多处挂载, 渲染函数无 return 等无法映射的结构会拒绝打开并指出行列; TSX 工程中 `compose.X` / `compose:X` 标签是可编辑的节点, 其它标签与 `{}` 表达式按原文保留, XML 标签与展开属性会拒绝
- 设计器会执行脚本吗? 不会. 它只解析脚本 (JavaScript 用 Rhino 解析器, TSX 用 TypeScript Engine 的解析服务)并用插件自带的渲染器预览, 事件处理函数不会执行, state 显示初值或状态面板中设置的预览值; 交互预览中绑定了 state 的控件 (开关, 滑块, 输入框等) 可以直接改变预览值, 但处理函数与渲染函数仍不运行. 运行要交给 AutoJs6
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
- 备份策略: 已禁用应用备份与设备迁移, 插件本身不持有任何需要迁移的数据

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

#### v1.4.0

_2026/10/13_

- `提示` 设计器的 TSX 工程需要 TypeScript Engine 0.7.0 或更高版本; 未安装或版本过低时, TSX 工程只能以只读源码方式查看与复制
- `新增` 设计器支持 TSX 工程: 新建工程时可选 JavaScript 或 TSX, 也可以导入 .tsx 文件, 或从 AutoJs6 编辑器与文件管理器直接打开 .tsx 文件; TSX 源码由 TypeScript Engine 解析后在画布中编辑, 保存为统一格式的 TSX (4 空格缩进, 单引号属性, 每个子元素一行); 设计器无法编辑的写法 (例如别名标签, 条件或循环生成的子元素) 按原文保留, XML 标签与展开属性等无法表示的写法会拒绝打开并指出行列
- `新增` 设计器中 TSX 工程的 "运行" 会把工程文件交给 AutoJs6, 由 TypeScript Engine 编译后运行; 有未保存的修改时先保存
- `新增` TypeScript Engine 未安装, 版本过低或解析服务暂时不可用时, 设计器以只读方式显示 TSX 工程的源码并说明原因, 可以复制源码或重试; 安装或更新 TypeScript Engine 后回到设计器会自动重试
- `修复` 设计器保存无法写回的外部脚本时, 文件名含空格, 括号等工程名不允许的字符会使插件闪退, 而不是另存为工程的问题; 现在这些字符换成 "-" 后另存, 导入脚本也按同一规则命名
- `优化` TSX 工程中编辑事件处理函数时由 TypeScript Engine 检查语法, 可以使用类型标注等 TypeScript 写法
- `依赖` 附加 typescript-parser-api.aar 契约制品 (TypeScript 语法树解析契约 v1), 供设计器读取 TSX 工程

#### v1.3.0

_2026/10/13_

- `新增` 设计器中经 "导入脚本" 得到的工程可在编辑页菜单中 "写回来源" 或 "从来源重新导入": 导入时保留对来源文件的访问权限 (文件提供方允许时), 来源或工程在上次同步后被修改过时先询问, 来源文件无法打开时可重新选择以恢复关联; 保存工程不会自动写回来源
- `新增` 设计器画布可预览 Image 的图片: '@drawable/名称' 取自已安装的 AutoJs6, 相对路径在编辑页菜单 "图片目录" 选定的文件夹中查找 (仅保留只读访问权限); 网址与绝对路径仍显示占位, 原因列在问题列表中
- `新增` 设计器编辑页新增 "状态" 面板: 列出脚本顶层的 compose.state 与其初值, 可以临时修改预览值, 只影响画布, 不写入脚本也不产生撤销步骤; 打开画布右上角的 "交互预览" 后可以直接操作画布中的组件, 绑定了状态的开关, 滑块, 输入框, 选择项, 分页等控件会修改对应的预览值 (脚本中的事件处理函数不会执行), 关闭后恢复点选与拖动
- `修复` 界面中某个容器前部的子节点被插入, 删除或移到别处时, 其后的子节点会被整体重建, 丢失它们自己的状态 (例如 LazyColumn 的滚动位置) 的问题; 现在这些子节点保持原有状态, 也不再重复构建
- `修复` 设计器属性抽屉修改输入框 (TextField / OutlinedTextField) 的 text 后, 画布中的输入框仍显示旧文字的问题
- `优化` 设计器画布中长按拖动节点时, 会按目标容器的排列方向 (纵向, 横向, 网格) 显示插入线, 松手后节点放到指针所在的位置, 不再只能追加到容器末尾
- `优化` 设计器画布中的 AndroidView 改为按其尺寸修饰 (如 h, fillMaxWidth) 显示浅色占位区域, 并在其中显示 view 表达式的源码
- `优化` 设计器属性抽屉中的修饰链可以按住每项左侧的手柄拖动排序, 松手前显示插入线; 上移 / 下移按钮仍然保留
- `优化` 设计器画布中打开状态 (open 为 true) 的弹层组件改为按接近真实的样式画在画布的逻辑屏幕内: AlertDialog 与 ModalBottomSheet 连同遮罩盖在页面上方, DropdownMenu 与 Tooltip 显示在锚点下方或上方, 点选弹层中的节点即可编辑; 不再以卡片形式插在页面中, 也不会弹出真实窗口
- `优化` 设计器画布在每次编辑后只重新组合发生变化的节点, 并减少记录节点位置的开销, 较大的文档 (约 250 个节点) 撤销 / 重做后的画面刷新明显更快

#### v1.2.0

_2026/10/13_

- `提示` 插件由 Compose UI 更名为 3-Block Compose: 应用 ID 由 io.github.supermonster003.autojs6.plugin.compose.ui 改为 io.github.supermonster003.autojs6.plugin.three.block.compose. Android 将其视为新应用, 旧包可以保留并存, 设置与授权不会自动迁移, 新包需要在插件中心重新启用和授权
- `提示` 需要包含新插件身份的 AutoJs6 6.8.0 宿主构建 (宿主提交 f12665ffde 之后); 更早的宿主按旧包名发现插件, 不会识别本版本
- `提示` 设计器的使用说明见文档站 "3-Block Compose 设计器" 页; AutoJs6 编辑器菜单的 "在 3-Block Compose 设计器中打开" 需要宿主提交 db7e6a1734 之后的 AutoJs6 6.8.0 构建, 其它去向只需已安装 AutoJs6
- `新增` 可视化设计器: 从启动器进入设计器首页, 可新建 / 导入 / 打开私有目录中的脚本工程; 编辑页提供真实渲染器预览画布, 大纲树, 组件面板与属性抽屉, 支持点选与长按拖拽换父, 大纲内重排, 撤销 / 重做, 草稿自动保存与恢复, 承载选项编辑, 契约目录校验报告, 以及复制脚本或发送到 AutoJs6 运行; 横屏或平板上画布与面板左右排列; 组件画廊与设置改从设计器首页进入
- `新增` 设计器与 AutoJs6 的衔接: 编辑页菜单可把当前脚本在 AutoJs6 编辑器中打开 (AutoJs6 保存后设计器自动重新加载), 导入 AutoJs6 脚本目录, 或导出到任意位置; 设计器也可以从 AutoJs6 编辑器或文件管理器直接打开 .js 文件并保存回原文件, 没有写入权限时另存为工程; 工程文件通过插件自己的内容提供器按次授权, 不对外公开其它目录
- `优化` 应用标题, 插件 ID (three-block-compose), 包名与组件名, 发布产物, 文档及 GitHub 仓库 (AutoJs6-Plugin-Three-Block-Compose) 随更名同步更新; 脚本 API compose, 契约版本与渲染行为不变
- `优化` 启动器, 插件中心与系统应用信息图标改用维护者提供的正式图稿, 亮暗两套图案共用同一形状
- `优化` 随包示例与宿主示例分类改为 3-Block Compose
- `优化` 契约 AAR compose-ui-api 改为随插件打包并完整保留 (此前只在编译期引用), 供插件自身进程使用; 在 AutoJs6 内仍优先使用宿主提供的同名契约类, 渲染行为与契约版本不变
- `依赖` 附加 Rhino 解析器精简 jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, 与 AutoJs6 宿主同一构建), 仅供可视化设计器解析脚本, 不执行脚本; release 包只保留被引用的解析类

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
