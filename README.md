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

Compose UI 是 AutoJs6 的界面渲染插件. 脚本通过宿主内置的 `compose` 全局对象声明界面, 插件在宿主进程内以 Jetpack Compose 与 Material 3 完成渲染, 为 `"ui";` 模式 Activity 内容与悬浮窗提供统一的声明式界面方案.

插件不包含任何独立界面, 也不在启动器中显示入口. 宿主通过 INFO 服务发现插件并读取版本与兼容信息, 再按契约 (`org.autojs.plugin.compose.api`) 在宿主进程内加载渲染器. 界面树, 状态与事件在脚本侧描述, 渲染器只负责把补丁应用到 Compose 组合并把用户事件回传给脚本.

******

### 当前状态

******

当前版本为 P0 开发预览. 仓库已包含可构建的插件骨架, INFO 服务与 Wake Activity 激活协议, 但渲染器与脚本 API 尚未交付, 安装后脚本暂时不能通过 `compose` 渲染任何界面. 后续里程碑与进度见路线图.

******

### 功能特性

******

插件计划交付的核心能力:

- 声明式界面: `compose.state` + `compose.mount(render)` 按状态变化自动重绘, 同时提供可长期持有的节点句柄 (`compose.Text({...})` 等) 直接修改属性与子节点
- Material 3 组件核心集: 布局 (Column / Row / Box / LazyColumn 等), 文本, 按钮, 输入框, 开关, 滑块, 进度条, 卡片, 对话框等
- 链式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作顺序, 作用域操作在宿主侧校验
- 两种承载面: `"ui";` 脚本的 Activity 内容 (`compose.mount`) 与任意脚本的悬浮窗 (`compose.floaty`)
- 进程内渲染: 渲染器在宿主进程中运行, 没有跨进程界面桥接, 事件与状态更新低延迟
- 纯字节码 APK: 不含原生库, 不区分 ABI, 单包适配所有设备

******

### 使用方式

******

1. 安装 AutoJs6 6.8.0 (5308) 或更高版本
2. 安装本插件 APK (无需打开, 插件没有启动器入口)
3. 在 AutoJs6 的插件中心确认 Compose UI 已被识别并处于启用状态
4. 在脚本中直接使用 `compose` 全局对象 (渲染能力将随 1.0.0 版本交付)

******

### 快速开始

******

以下示例展示目标 API 形态 (以路线图附录 A 为准, 渲染能力交付前尚不能运行):

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

完整的 API 说明 (组件目录, Modifier 操作, 会话对象, 错误码) 见 AutoJs6 文档的 compose 模块章节.

******

### 兼容性

******

插件的运行要求与限制:

- AutoJs6 版本: 6.8.0 (5308) 或更高; 低于该版本的宿主会在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 处理器架构: 不限 (纯字节码 APK, 无原生库)
- Compose 版本: 由插件自带 (BOM 2026.09.00), 不依赖宿主的 Compose 运行时
- 契约版本: 1; 宿主与插件通过契约版本协商, 不匹配时拒绝加载并给出明确错误

******

### 常见问题

******

- 为什么安装后找不到插件图标? 插件没有独立界面, 也不会在启动器显示, 请在 AutoJs6 的插件中心查看
- 为什么脚本里的 `compose` 还不能用? 当前为 P0 开发预览, 渲染器与脚本 API 将在后续里程碑交付
- 是否需要卸载其它界面插件? 不需要, Compose UI 与现有 `ui` 模块及其它插件互不影响
- 插件更新后脚本需要修改吗? 契约版本不变时无需修改; 契约升级会在更新日志中明确标注

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
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5308 (6.8.0)
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

#### v1.0.0

_2026/10/02_

- `提示` P0 开发预览: 仓库骨架可构建并可被宿主识别, 渲染器与脚本 API 尚未交付
- `提示` 需要 AutoJs6 6.8.0 (5308) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
- `新增` 插件仓库骨架: 平台版本插件构建链, Jetpack Compose BOM 2026.09.00 依赖, Wake Activity 激活协议与 INFO 服务 (类别 compose-ui)
- `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
- `依赖` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 哈希锁定)
- `依赖` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)

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
