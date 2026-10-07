<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>为 AutoJs6 脚本提供 Jetpack Compose 与 Material 3 界面渲染能力的插件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 语言

******

本文档支持以下语言:

- 简体中文 [zh-Hans] # 当前
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### 简介

******

Compose UI 是 AutoJs6 的界面渲染插件. 脚本通过宿主提供的 `compose` / `$compose` 入口声明界面, 插件在宿主进程内以 Jetpack Compose 与 Material 3 完成渲染. 当前预览同时支持 `"ui";` 模式的 Activity 内容与非 ui 脚本的悬浮窗.

插件不包含任何独立界面, 也不在启动器中显示入口. 宿主通过 INFO 服务发现插件并读取版本与兼容信息, 再按契约 (`org.autojs.plugin.compose.api`) 在宿主进程内加载渲染器. 界面树, 状态与事件在脚本侧描述, 渲染器只负责把补丁应用到 Compose 组合并把用户事件回传给脚本.

******

### 当前状态

******

1.1.0 本地开发预览: 需要匹配的 AutoJs6 宿主构建, 并安装和启用本插件. 本地配套提供 UI 页面, 悬浮窗, 五个示例, API 参考与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 当前未登记官方索引, 尚无官方发行版. 图标图案仍为临时占位, 等待维护者提供正式源图.

******

### 功能特性

******

当前开发预览的核心能力:

- 声明式界面: `compose.state` + `compose.mount(render)` 按状态变化自动重绘, 同时提供可长期持有的节点句柄 (`compose.Text({...})` 等) 直接修改属性与子节点
- Material 3 核心集: 29 个节点工厂覆盖布局, 文本, 图标, 图片, 按钮, 输入, 选择控件, 惰性列表, 对话框与进度; Snackbar 是会话命令, 不是 compose.Snackbar 工厂
- 链式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作顺序, 作用域操作在宿主侧校验
- 两种承载方式: `"ui";` 脚本通过 `compose.mount` 或可调用的 `compose` / `$compose` 挂载 Activity 内容; `compose.floaty` 创建 raw 或可调整悬浮窗, 同样支持非 ui 脚本
- 进程内渲染: 插件在宿主进程应用界面更新, 事件排队回到所属脚本线程; 工作线程通过 compose.post 请求更新
- 一个 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 无插件自有原生代码; 随包的 AndroidX graphics-path 辅助库仍需满足 Android, 宿主与插件兼容条件
- 原生文本编辑保留选区与输入法组合状态, 支持焦点和显式编辑, 拒绝覆盖较新输入的延迟编辑; 开关与滑块仍由脚本控制状态
- 集成守卫: 插件缺失或不兼容时可用性探测返回不可用, 错误使用 `ComposeError`, 关闭会话或停止脚本会释放所属窗口与回调
- 计数器, 表单校验, 1000 项键控列表, 非 ui 悬浮 HUD 与主题五个可运行示例, 包含前置条件与索引, 同步至匹配宿主的 Compose UI 示例分类
- TSX 支持 `<compose.Column>`, `<compose:Text>`, 节点工厂引用, Fragment, 插槽及响应式回调; 同一棵树不能混用 Compose 与旧 XML 节点
- XML `<compose>` 容器与 compose.attach 可在 UI 页面或旧悬浮窗内嵌入独立 Compose 会话; compose.AndroidView 可承载现有 Android View 或同步工厂返回的 View
- compose.dialog 返回可更新和关闭的会话, 支持普通对话框与模态底部弹层, 可在 UI 或普通脚本中使用
- Material 3 扩展组件: 导航栏与抽屉, 标签页, 底部弹层与菜单, 日期与时间选择器, 分页与网格, 芯片, 徽标, 分段按钮, 悬浮按钮, 搜索栏, 提示与下拉刷新

******

### 使用方式

******

1. 安装包含 compose 脚本入口的匹配本地 AutoJs6 构建 (最低 6.8.0 / 5322)
2. 安装本插件 APK (无需打开, 插件没有启动器入口)
3. 在 AutoJs6 的插件中心确认 Compose UI 已被识别并处于启用状态
4. 在脚本中使用 `compose` 或 `$compose`; 通过 `compose.mount` 挂载 Activity 内容, 或先授予宿主悬浮窗权限再使用 `compose.floaty`

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

五个可运行脚本由 assets/examples/index.json 列出, 同步至匹配宿主的 Compose UI 示例分类. 每例头部说明模式与权限. 节点, 修饰链, 主题, 会话与悬浮窗详情请使用同批本地 API 文档及 TypeScript/编辑器声明; 在线站点不一定已包含这些本地变更.

******

### 兼容性

******

插件的运行要求与限制:

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更高; 低于该版本的宿主会在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 处理器架构: arm64-v8a / armeabi-v7a / x86_64 / x86 (单一 APK 内置全部四种, 无需按架构选择安装包)
- Compose 版本: 由插件自带 (BOM 2026.09.00), 不依赖宿主的 Compose 运行时
- 契约版本: 2; 宿主与插件通过契约版本协商, 不匹配时拒绝加载并给出明确错误
- 打包应用仍需另外安装兼容的 Compose UI 插件, 启用/授权记录属于该应用; 兼容性检查依据内置 AutoJs6 运行时, 不是打包应用自身的 versionCode
- View 工厂在渲染前于主线程执行. 无效替换保留当前内容; 同一 View 不可属于两个节点, 也不会从其他父视图被抢占. 借用的 View 保留原有监听器, 外部资源仍由调用方管理
- cancelable=false 同时禁止返回键, 点击外部和下滑关闭; 主动关闭与脚本退出仍会清理弹窗, 保留已有页面和其他会话
- 本构建使用 Compose UI 契约 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用旧 V1 渲染器的原有组件, 宽集组件需要 V2 渲染器

******

### 常见问题

******

- 为什么安装后找不到插件图标? 插件没有独立界面, 也不会在启动器显示, 请在 AutoJs6 的插件中心查看
- 为什么找不到 `compose`? 全局对象由匹配的本地宿主构建提供, 单独安装插件 APK 不会添加该入口
- 是否需要卸载其它界面插件? 不需要, Compose UI 与现有 `ui` 模块及其它插件互不影响
- 插件变化时会怎样? 更新, 卸载或停用插件会关闭活动会话并报告对应错误; 兼容且启用的插件允许重新挂载
- 悬浮窗需要什么条件? 先授予宿主悬浮窗权限, 输入文字前调用 `window.requestFocus()`. 若 HyperOS 未显示窗口, 请先回到桌面. 缺少权限会返回 PERMISSION_REQUIRED, 不会自动弹出授权界面
- 能否使用 TSX 或任意 Compose 函数? 配合匹配的宿主与 TypeScript Engine, TSX 可使用文档列出的 Compose 节点工厂. 不支持任意 Kotlin Composable 函数或自定义 TSX 组件
- 旋转是否丢失状态? 当前宿主自行处理普通方向变化, 保留脚本引擎. 真实的 Activity 重建或销毁会关闭该引擎及所属会话, 不自动恢复业务状态
- 选择器如何查找组件? testTag 按原样暴露为 ID, 不添加包名前缀. id/testTag 与 desc/contentDescription 是不同信息; Button 的文字可能是子节点, 必要时沿 parent() 查找可点击祖先

******

### 权限与安全

******

插件不申请任何 Android 运行时权限, 也不访问网络, 存储或传感器.

- 组件保护: Wake Activity 与 INFO 服务均受 `org.autojs.permission.PLUGIN` 签名权限保护, 只有 AutoJs6 宿主可以访问
- 无后台行为: 插件没有常驻服务, 广播接收器或定时任务, 不被宿主加载时不消耗资源
- 数据边界: 插件不读写脚本数据与用户文件, 界面状态只存在于宿主进程内存中
- 备份策略: 已禁用应用备份与设备迁移, 插件本身不持有任何需要迁移的数据

宿主加载渲染器时沿用自身的脚本权限模型, 插件不扩大脚本可访问的系统能力.

******

### 插件接口

******

面向宿主的接口标识如下:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

宿主通过 `org.autojs.plugin.INFO` 发现插件并读取 `requiresHostVersion` 等能力信息; 渲染器工厂类名由 `org.autojs.plugin.compose.RENDERER_FACTORY` 元数据声明, 宿主以插件 APK 路径创建类加载器 (父加载器为宿主) 并在宿主进程内实例化.

******

### 路线图

******

插件的里程碑, 设计决策与验收标准统一记录在路线图中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### 版本历史

******

#### v1.1.0

_2026/10/08_

- `提示` 1.1.0 本地开发预览: 需要匹配的 AutoJs6 宿主构建, 并安装和启用本插件. 本地配套提供 UI 页面, 悬浮窗, 五个示例, API 参考与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 当前未登记官方索引, 尚无官方发行版. 图标图案仍为临时占位, 等待维护者提供正式源图
- `提示` 本构建使用 Compose UI 契约 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用旧 V1 渲染器的原有组件, 宽集组件需要 V2 渲染器
- `新增` TSX 支持 `<compose.Column>`, `<compose:Text>`, 节点工厂引用, Fragment, 插槽及响应式回调; 同一棵树不能混用 Compose 与旧 XML 节点
- `新增` XML `<compose>` 容器与 compose.attach 可在 UI 页面或旧悬浮窗内嵌入独立 Compose 会话; compose.AndroidView 可承载现有 Android View 或同步工厂返回的 View
- `新增` compose.dialog 返回可更新和关闭的会话, 支持普通对话框与模态底部弹层, 可在 UI 或普通脚本中使用
- `新增` Material 3 扩展组件: 导航栏与抽屉, 标签页, 底部弹层与菜单, 日期与时间选择器, 分页与网格, 芯片, 徽标, 分段按钮, 悬浮按钮, 搜索栏, 提示与下拉刷新
- `优化` Android 系统应用信息图标与图标工作台共用图稿和亮暗底色, 保留插件中心透明图稿及现有启动器选项
- `优化` View 工厂在渲染前于主线程执行. 无效替换保留当前内容; 同一 View 不可属于两个节点, 也不会从其他父视图被抢占. 借用的 View 保留原有监听器, 外部资源仍由调用方管理
- `优化` cancelable=false 同时禁止返回键, 点击外部和下滑关闭; 主动关闭与脚本退出仍会清理弹窗, 保留已有页面和其他会话
- `依赖` 升级 compose-ui-api.aar 契约制品, 保留冻结 V1 并附加可选 AndroidView 互操作扩展
- `依赖` 附加可选对话框能力至 compose-ui-api.aar, 保留已有 V1 与 AndroidView 契约
- `依赖` 附加 compose-ui-api.aar V2 组件目录, 保留已有节点模型和 V1 组件语义

#### v1.0.0

_2026/10/03_

- `提示` 1.0.0 本地开发预览: 需要匹配的 AutoJs6 宿主构建, 并安装和启用本插件. 本地配套提供 UI 页面, 悬浮窗, 五个示例, API 参考与 TypeScript 声明. 已验证的兼容性及性能范围记录在路线图中. 当前未登记官方索引, 尚无官方发行版. 图标图案仍为临时占位, 等待维护者提供正式源图
- `提示` 需要 AutoJs6 6.8.0 (5316) 或更高版本
- `提示` 打包应用仍需另外安装兼容的 Compose UI 插件, 启用/授权记录属于该应用; 兼容性检查依据内置 AutoJs6 运行时, 不是打包应用自身的 versionCode
- `提示` 保留亮色/深色两类 mipmap 入口; 当前图案为占位, 待维护者提供正式黑白源图后替换
- `新增` 可调用的 compose / $compose, 长期节点句柄, 响应式 state/render/ref, 批量变更, 排队调度与主题控制
- `新增` Material 3 核心集: 29 个节点工厂覆盖布局, 文本, 图标, 图片, 按钮, 输入, 选择控件, 惰性列表, 对话框与进度; Snackbar 是会话命令, 不是 compose.Snackbar 工厂
- `新增` UI 脚本可挂载 Activity 内容; compose.floaty 同时支持非 UI 脚本的 raw / 可调整窗口, 像素位置与尺寸, 触摸/焦点控制及所属资源清理
- `新增` 全部 20 种 Modifier 操作保留声明顺序, 支持布局作用域校验, 滚动与无障碍标签
- `新增` Material 3 主题支持种子色, 明暗模式, Android 12+ 系统动态色, 字体与字号缩放
- `新增` 原生文本编辑保留选区与输入法组合状态, 支持焦点和显式编辑, 拒绝覆盖较新输入的延迟编辑; 开关与滑块仍由脚本控制状态
- `新增` 核心图标与 ImageWrapper/Bitmap, 本地文件和宿主 drawable 图片; 渲染器不自动回收调用方持有的图片资源
- `新增` 计数器, 表单校验, 1000 项键控列表, 非 ui 悬浮 HUD 与主题五个可运行示例, 包含前置条件与索引, 同步至匹配宿主的 Compose UI 示例分类
- `新增` 配套 API 参考与 TypeScript 声明, 以及 10 语言 README, 插件中心说明和更新日志
- `新增` 插件中心发现与宿主版本, 契约, 授权检查, 无独立界面和启动器入口
- `修复` Compose UI 在渲染回调中拒绝再次挂载页面或悬浮窗并保留当前页面, 支持插件更新后重新挂载页面
- `优化` 可用性探测与 ComposeError 统一报告插件缺失, 禁用, 未授权, 不兼容, 权限不足及会话关闭; 生命周期清理覆盖原生窗口附加前即被取消的情况; 插件更新, 卸载或停用时关闭活动会话并报告对应错误
- `依赖` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 哈希锁定)
- `依赖` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `依赖` 附加与 AutoJs6 6.8.0 (5316) 对齐的 compose-ui-api.aar V1 (MPL 2.0, 摘要锁定), 共用依赖与宿主对齐
- `依赖` 附加由 BOM 2026.09.00 管理的 Compose UI Test (Apache 2.0, 仅用于测试)
- `依赖` 附加 JaCoCo 版本 0.8.14 (仅用于可选测试覆盖率, 不随发布包打包)

##### 更多版本历史可参阅

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hans.md)

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

本项目基于 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE) 发布. 第三方组件的许可信息见 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相关链接

******

- AutoJs6 项目主页: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文档: https://docs.autojs6.com
- compose 模块文档: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方声明: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
