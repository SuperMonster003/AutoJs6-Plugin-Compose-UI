Compose UI 是 AutoJs6 的界面渲染插件. 脚本通过宿主提供的 `compose` / `$compose` 入口声明界面, 插件在宿主进程内以 Jetpack Compose 与 Material 3 完成渲染. 当前预览同时支持 `"ui";` 模式的 Activity 内容与非 ui 脚本的悬浮窗.

P4 开发预览: 可调用的 compose / $compose 入口, 29 个节点工厂, 长期持有的句柄, 响应式 state/render/ref, batch/post/theme, ui 脚本挂载及 raw / 可调整悬浮窗已可在匹配的本地 AutoJs6 宿主构建中使用. 已包含可用性探测, 类型化错误与会话清理. 已附带计数器, 表单, 1000 项列表, 悬浮 HUD 与主题五个示例, 同步至匹配宿主的 Compose UI 示例分类. 完整 API 文档, 类型声明与 P5 完整验证矩阵仍待后续交付. 当前仅为本地预览, 尚无官方发行版.

### 使用方式

1. 安装包含 compose 脚本入口的匹配本地 AutoJs6 构建 (最低 6.8.0 / 5316)
2. 安装本插件 APK (无需打开, 插件没有启动器入口)
3. 在 AutoJs6 的插件中心确认 Compose UI 已被识别并处于启用状态
4. 在脚本中使用 `compose` 或 `$compose`; 通过 `compose.mount` 挂载 Activity 内容, 或先授予宿主悬浮窗权限再使用 `compose.floaty`

### 兼容性

- 最低 AutoJs6 版本: 6.8.0 (5316) 或更高; 低于该版本的宿主会在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 处理器架构: arm64-v8a / armeabi-v7a / x86_64 / x86 (单一 APK 内置全部四种, 无需按架构选择安装包)
- Compose 版本: 由插件自带 (BOM 2026.09.00), 不依赖宿主的 Compose 运行时
- 契约版本: 1; 宿主与插件通过契约版本协商, 不匹配时拒绝加载并给出明确错误

### 常见问题

- 为什么安装后找不到插件图标? 插件没有独立界面, 也不会在启动器显示, 请在 AutoJs6 的插件中心查看
- 为什么找不到 `compose`? 全局对象由匹配的本地宿主构建提供, 单独安装插件 APK 不会添加该入口
- 是否需要卸载其它界面插件? 不需要, Compose UI 与现有 `ui` 模块及其它插件互不影响
- 插件更新后脚本需要修改吗? 契约版本不变时无需修改; 契约升级会在更新日志中明确标注
- 悬浮窗需要什么条件? 先授予宿主悬浮窗权限, 输入文字前调用 `window.requestFocus()`. 若 HyperOS 未显示窗口, 请先回到桌面. 缺少权限会返回 PERMISSION_REQUIRED, 不会自动弹出授权界面

### 权限与安全

- 组件保护: Wake Activity 与 INFO 服务均受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 宿主可以访问
- 无后台行为: 插件没有常驻服务, 广播接收器或定时任务, 不被宿主加载时不消耗资源
- 数据边界: 插件不读写脚本数据与用户文件, 界面状态只存在于宿主进程内存中
- 备份策略: 已禁用应用备份与设备迁移, 插件本身不持有任何需要迁移的数据

更多信息 (快速开始, 构建说明, 路线图) 见项目主页: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
