3-Block Compose 是 AutoJs6 的界面渲染插件. 脚本通过宿主提供的 `compose` / `$compose` 入口声明界面, 插件在宿主进程内以 Jetpack Compose 与 Material 3 完成渲染. 当前预览同时支持 `"ui";` 模式的 Activity 内容与非 ui 脚本的悬浮窗.

1.3.0 本地开发预览: 需要包含 3-Block Compose 新身份的 AutoJs6 宿主构建 (宿主提交 f12665ffde 之后), 并安装和启用本插件; AutoJs6 编辑器菜单的 "在 3-Block Compose 设计器中打开" 需要宿主提交 db7e6a1734 之后的构建. 本地配套提供 UI 页面, 悬浮窗, 可视化设计器, 五个示例, API 参考, 设计器指南页与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 当前未登记官方索引, 尚无官方发行版. 图标已采用维护者提供的正式图稿.

### 使用方式

1. 安装包含 compose 脚本入口的匹配本地 AutoJs6 构建 (最低 6.8.0 / 5322)
2. 安装本插件 APK; 从启动器打开 3-Block Compose 进入设计器首页, 可新建脚本工程, 或从顶部栏打开组件画廊与设置
3. 在 AutoJs6 的插件中心确认 3-Block Compose 已被识别并处于启用状态
4. 在脚本中使用 `compose` 或 `$compose`; 通过 `compose.mount` 挂载 Activity 内容, 或先授予宿主悬浮窗权限再使用 `compose.floaty`
5. 编辑页菜单提供发送运行, 在 AutoJs6 编辑器中打开, 导入 AutoJs6 脚本目录, 复制与导出; 在 AutoJs6 编辑器菜单 (需 AutoJs6 包含对应更新) 或文件管理器中选择 3-Block Compose 也可以直接编辑脚本文件

### 兼容性

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

### 常见问题

- 画廊里的示例如何运行? 点击 "在 AutoJs6 中运行" 会把脚本交给已安装的 AutoJs6 执行; 画廊自身只展示预览与代码, 不执行脚本
- 设计器能打开任意脚本吗? 它只解释顶层的 `compose.state` 声明与唯一的 `compose.mount` / `floaty` / `dialog` 调用, 其它语句按源码原样保留; 根节点不是 compose 工厂, 多处挂载, 渲染函数无 return 等无法映射的结构会拒绝打开并指出行列
- 设计器会执行脚本吗? 不会. 它只用 Rhino 解析器解析脚本并用插件自带的渲染器预览, 事件处理函数不会执行, state 显示初值或状态面板中设置的预览值; 交互预览中绑定了 state 的控件 (开关, 滑块, 输入框等) 可以直接改变预览值, 但处理函数与渲染函数仍不运行. 运行要交给 AutoJs6
- 为什么找不到 `compose`? 全局对象由匹配的本地宿主构建提供, 单独安装插件 APK 不会添加该入口
- 是否需要卸载其它界面插件? 不需要, 3-Block Compose 与现有 `ui` 模块及其它插件互不影响
- 插件变化时会怎样? 更新, 卸载或停用插件会关闭活动会话并报告对应错误; 兼容且启用的插件允许重新挂载
- 悬浮窗需要什么条件? 先授予宿主悬浮窗权限, 输入文字前调用 `window.requestFocus()`. 若 HyperOS 未显示窗口, 请先回到桌面. 缺少权限会返回 PERMISSION_REQUIRED, 不会自动弹出授权界面
- 能否使用 TSX 或任意 Compose 函数? 配合匹配的宿主与 TypeScript Engine, TSX 可使用文档列出的 Compose 节点工厂. 不支持任意 Kotlin Composable 函数或自定义 TSX 组件
- 旋转是否丢失状态? 当前宿主自行处理普通方向变化, 保留脚本引擎. 真实的 Activity 重建或销毁会关闭该引擎及所属会话, 不自动恢复业务状态
- 选择器如何查找组件? testTag 按原样暴露为 ID, 不添加包名前缀. id/testTag 与 desc/contentDescription 是不同信息; Button 的文字可能是子节点, 必要时沿 parent() 查找可点击祖先

### 权限与安全

- 组件保护: Wake Activity 与 INFO 服务均受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 宿主可以访问; 设计器编辑页是无权限的导出组件, 只接收用户交出的脚本文档 Uri, 读写都限于该 Uri
- 后台行为: 插件没有常驻服务与定时任务, 仅在自身被更新时接收一次系统广播以校正启动器图标组件; 不被宿主加载且未打开设计器或画廊时不消耗资源
- 数据边界: 渲染器不读写脚本数据与用户文件, 界面状态只存在于宿主进程内存中; 设计器的工程与草稿存于插件私有目录, 只经系统文件选择器导入导出, 或按用户交出的 Uri 读写单个脚本文件; 工程目录仅通过插件自己的内容提供器在交给 AutoJs6 时按次授权; 导入时会保留所选文件的访问权限 (提供方允许时), 但只在用户选择 "写回来源" 或 "从来源重新导入" 时读写该文件
- 备份策略: 已禁用应用备份与设备迁移, 插件本身不持有任何需要迁移的数据

更多信息 (快速开始, 构建说明, 路线图) 见项目主页: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
