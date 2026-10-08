# AutoJs6-Plugin-Compose-UI AGENTS.md

本文件是本仓库的工程约定, 由 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么". 同目录的 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标) 在图标相关改动时同样适用.

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库是 **进程内渲染器插件** (路线图 D10 / D23): 宿主用 `PathClassLoader(插件 APK, parent = 宿主类加载器)` 在宿主进程内实例化渲染器工厂, 不存在 Binder 能力服务, 不存在插件自有进程中的界面. 参考规范中 Binder 服务, AIDL 冻结, 前台服务, 跨包 `queries` 的条款不适用.
- 单一通用 APK (D22, 经 P0.1 证据修正): 插件自身没有原生代码, 不使用 ABI 拆分; 唯一的原生库是 Compose `ui-graphics` 传递依赖 `androidx.graphics:graphics-path` 1.0.1 自带的 `libandroidx.graphics.path.so` (四种 ABI, 各约 10 KB, 16 KB 页对齐). 不使用 `autojs6-native-alignment` 插件, 没有原生库重建配方; `appendDigestToReleasedFiles` 校验 "原生库集合恰好为这四个文件, 未压缩, ELF `PT_LOAD` 与 zip 数据偏移均 16 KB 对齐" (第 9 节).
- 独立界面只有组件画廊与设置页 (D8 于 2026-10-08 按维护者要求修订, 路线图 F.5 / D32): 画廊从启动器打开, 在插件自身进程以 Compose 展示组件预览与示例脚本, 只复制脚本或交给已安装的 AutoJs6 运行, 自身不执行脚本; 设置页按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 提供语言 / 夜间模式 / 主题色 / 启动器图标, 前三项默认跟随 AutoJs6 (`AutoJs6HostSettingsContract` Provider), 图标按 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` 用四个固定 alias 持久化. 渲染器仍在宿主进程运行, 画廊不改变装载方式, 契约或最低宿主.
- 没有特权进程 (Shizuku / Root), 没有隐藏 API, 不访问网络.

## 2. 仓库身份

下列值在 Gradle, Manifest, Kotlin 常量 (`ComposeUiPlugin`), 资源, 文档, 测试和宿主注册信息中 MUST 完全一致. 修改任一值时同步修改全部位置, 并运行 `ManifestContractTest` 与 `ComposeUiPluginRuntimeInfoTest`.

| 项目 | 值 |
|---|---|
| 仓库与目录名 | `AutoJs6-Plugin-Compose-UI` |
| `rootProject.name` | `autojs6-plugin-compose-ui` |
| 应用标题 (不可翻译) | `Compose UI` |
| `applicationId` / namespace / Kotlin 包 | `io.github.supermonster003.autojs6.plugin.compose.ui` |
| 插件 ID / engine / variant | `compose-ui` / `compose` / `default` |
| INFO 服务 | `ComposeUiPluginInfoService`, action `org.autojs.plugin.INFO`, category `compose-ui` (D23: 仅 INFO 注册, 与 ImGui 插件同形) |
| 渲染器工厂 meta-data | application 级 `org.autojs.plugin.compose.RENDERER_FACTORY` = `io.github.supermonster003.autojs6.plugin.compose.ui.renderer.ComposeUiRendererFactoryImpl` (类随 P0.2 技术验证落地) |
| 宿主契约标识 | 契约包 `org.autojs.plugin.compose.api` (宿主 `plugin-api/compose-ui-api`, 路线图 P1.1), `CONTRACT_VERSION = 2`; P1.1 已冻结 V1, F.4 只追加 V2 目录, `ComposeUiPlugin` 引用 `ComposeUiIds` / `ComposeUiContract` 的内联常量 |
| 最低宿主 versionCode | `ComposeUiPlugin.REQUIRED_HOST_VERSION` = 5322 (F.4 的 V2 目录与协商需要配套 AutoJs6 6.8.0 / 5322; 历史 V1 最低宿主 5316 保留在冻结常量中. Manifest, `common.json`, changelog 与测试保持一致) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.9.0 (与兄弟仓库统一升级时再更新); 不使用 `autojs6-native-alignment` |
| Compose 版本 | BOM `2026.09.00` (runtime / ui / foundation / animation 1.12.1, material3 1.4.0, material-icons-core 1.7.8), 由 `gradle/libs.versions.toml` 单点声明; Compose 编译器插件版本 = `System.getProperty("gradle.kotlin.version")` (D25) |
| 发布文件名 | `autojs6-plugin-compose-ui-v{VERSION_NAME}-{CRC32}.apk` (1 个) |
| 原生库 / ABI | 无插件自有原生代码; `libandroidx.graphics.path.so` (graphics-path 1.0.1, 随 Compose BOM 变动) x `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86`, 单 APK 内置; `nativeAbis` / `allowedNativeLibraries` / `nativePageAlignment` 在 `app/build.gradle.kts` 单点声明 |
| 图标源图 | Icon Studio recipe `.icons/recipe.json` + `.icons/assets/` (亮 / 暗图稿为 P0.1 生成的 432 x 432 占位 `ic_launcher.png`, 图案 `#272727` / `#D8D8D8`, 底色 `#FAFAFA` / `#212121`, `launcher: true` 另生成四个 alias 的 `ic_launcher_system*` 与 `ic_launcher_monochrome`); P0.1 原始占位源 `.python/icons/compose-ui-ic-launcher-light.png` / `-dark.png` (1024 x 1024 RGBA, `UI_GLYPH = 0.66`) 保留备查. **当前为临时占位图** (路线图 Q6), 维护者提供正式源图后替换并重新生成 |
| 独立界面 | `app.GalleryActivity` (画廊, 启动器 alias `launcher.Adaptive{Light,Dark,Auto}IconAlias` / `launcher.TransparentIconAlias` 的目标), `app.SettingsActivity`; 宿主运行入口 `org.autojs.autojs.external.open.RunIntentActivity` + extra `script`; 主题色派生 `com.materialkolor:material-color-utilities` 4.1.1 |

## 3. 工作区与提交

### 3.1 会话开始

- MUST 运行 `git status --short`, 检查当前分支, 最近提交和相关文件差异.
- MUST 将已有未提交内容视为用户工作. 不覆盖, 不回滚, 不擅自整理与当前任务无关的改动.
- 禁止使用 `git reset --hard`, `git checkout -- <path>` 或其他可能丢失用户内容的命令, 除非用户明确授权.
- 先阅读 `ROADMAP.md` 的 "阶段总览" 与最后一条 "会话记录", 从路线图建议的起点开始.
- 涉及宿主 (`D:/idea-projects/AutoJs6`) 的条目, 同样先检查其工作树; 其它会话可能在宿主有未提交改动, 只按明确文件列表暂存与提交.

### 3.2 开发过程

- 每个行为改动应同时考虑实现, 测试, 10 语言资源, README, changelog, 宿主侧契约与装载器.
- 不提交本地缓存, IDE 状态, 调试输出或无意生成的二进制文件; `releases/` 与构建产物不入库.
- Gradle 自动修改 `BUILD_TIME` 时, 在确认来源后与相关变更一并处理. 若 Gradle 修改 `VERSION_BUILD`, 必须按第 3.4 节的提交计数规则校正; `VERSION_NAME` 只按语义化版本规则调整. 本地构建 SHOULD 传入 `-Pautojs.gradle.build.number.auto.increment.enabled=false -Pautojs.gradle.build.time.update.enabled=false` (PowerShell 下加引号) 避免无意变更.
- 修改第三方依赖或宿主 AAR 时同步记录版本, 来源, 校验值与许可证 (`THIRD_PARTY_NOTICES.md`, `libs/README.md`, `locks/host-api-aars.lock`), 并在 changelog 的 `dependency` 分类记录.
- 路线图条目完成后在 `ROADMAP.md` 勾选并写入证据 (设备, API, 度量值, 提交), 不勾选没有证据的条目.

### 3.3 提交

- 维护者于 2026-10-04 明确授权本插件创建公开 GitHub 仓库并推送源码, 使用 GitHub noreply 邮箱; 此指示取代 D7 的插件源码暂不推送约束. 图稿可同步至官方索引资源库; 正式 APK 与可下载条目仍以真实 Release 和准入记录为准. 宿主仓库不在本次推送范围内.
- 除非用户明确要求本次会话不要提交, 会话结束前 MUST 将本次范围内的全部文件按逻辑提交, 一个路线图子项一个提交.
- 使用 Conventional Commits 风格: `feat:`, `fix:`, `docs:`, `build:`, `test:`, `ci:`, `chore:`, 可加作用域, 例如 `feat(renderer): ...`, `feat(info): ...`.
- 一个提交表达一个完整意图; 行为实现, 对应测试和对应 changelog 通常放在同一提交.
- 提交前 MUST 审阅 `git diff --check`, `git diff --cached`, `git status --short`, 确认没有密钥, 本地路径, 临时 APK 或无关改动.
- 会话结束时最终 `git status --short` 无输出; 若发现无法纳入本次提交的用户改动, 停止自动提交并向用户说明.

### 3.4 提交计数

- `VERSION_BUILD` MUST 与当前分支 `HEAD` 可达的 Git 提交数一致.
- 每次准备新提交时, 先用当前提交数加 1 得到即将产生的 build number, 写入 `version.properties`, 再把该文件与本次逻辑改动一并提交. 不要先写成当前提交数再提交.

```bash
next=$(( $(git rev-list --count HEAD 2>/dev/null || echo 0) + 1 ))
sed -i "s/^VERSION_BUILD=.*/VERSION_BUILD=$next/" version.properties
```

最后一笔提交完成后 MUST 验证 `VERSION_BUILD == git rev-list --count HEAD` 且 `git status --short` 无输出. 若发现不一致, 将 `VERSION_BUILD` 设置为 "当前提交数 + 1" 并创建一笔有明确含义的校正提交.

### 3.5 版本名称

- `VERSION_NAME` 从 1.0.0 开始, 按语义化版本管理, 与提交数量不绑定.
- 修改 `VERSION_NAME` 时同步更新全部 changelog JSON 的版本 key, README, 发布文件名断言与测试夹具, 再运行文档生成器.

## 4. 仓库结构

```text
AutoJs6-Plugin-Compose-UI/
|-- .changelog/                 lang_*.json x 10 + template_changelog.md (文案源)
|-- .github/workflows/          build.yml (JVM / APK / lint + API 24 x86 / API 35 x86_64 模拟器契约测试), markdown.yml, icon-studio.yml
|-- .icons/                     recipe.json + assets/ (Icon Studio 图稿与参数, 第 10 节与文末 System application icon 节)
|-- .python/                    generate_markdown.py (+ .bat, check_markdown.bat), generate_icon_studio.py + icon_studio_runtime.py, generate_launcher_icons.py (兼容入口), sync_examples.py, verify_apk_classpath.py, verify_standalone_launch.py, icons/ (P0.1 占位源图)
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/
|   |-- src/main/java/io/github/supermonster003/autojs6/plugin/compose/ui/
|   |   |-- ComposeUiPlugin.kt                 身份常量
|   |   |-- ComposeUiPluginRuntimeInfo.kt      PluginInfo 的纯数据视图 (JVM 可测)
|   |   |-- ComposeUiPluginInfo.kt             Context -> RuntimeInfo -> PluginInfo / capabilities
|   |   |-- ComposeUiPluginInfoService.kt      IPluginInfoProvider
|   |   |-- WakeActivity.kt
|   |   |-- renderer/                          ComposeUiRendererFactoryImpl, 渲染器, 组件目录, 补丁应用, V2 宽集组件; dialog/ (F.3 弹窗工厂), interop/ (F.2 AndroidView 工厂)
|   |   `-- app/                               F.5 独立界面 (不引用契约类): GalleryActivity / GalleryCatalog / GalleryPreviews, SettingsActivity / SettingsDialogs, StandaloneActivity, Appearance (宿主设置 Provider 读取与偏好), StandalonePalette (HCT 强调色与中性色), LauncherIcons, ScriptLauncher
|   |-- src/main/res/           values*/ x 11 (strings), values*/{icon_studio_backgrounds,standalone_colors,themes}.xml, values/ic_launcher_background*.xml, mipmap*/ (生成; 含 alias 的 ic_launcher_system* 与 anydpi / notnight XML), raw*/plugin_instruction.md (生成), raw/keep_*.xml (Icon Studio 保留规则), xml/{data_extraction_rules,locales_config}.xml
|   |-- src/debug/              RendererTestActivity (仅 debug, 承载本仓库渲染器 instrumentation)
|   |-- src/main/assets/doc/    CHANGELOG*.md (生成)
|   |-- src/main/assets/examples/ counter.js, form.js, list.js, floaty-hud.js, theme.js, index.json
|   |-- src/test/               JVM 契约与资源守卫 (第 13.1 节)
|   |-- src/androidTest/        激活 / 发现 / 打包契约 instrumentation (第 13.2 节)
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   |-- build.gradle.kts / proguard-rules.pro
|-- build-logic/                org.autojs.build.{utils,versions,signs,properties,jvm-convention}
|-- docs/dev/                   各阶段证据 (P0.2 起)
|-- gradle/                     wrapper, libs.versions.toml
|-- libs/                       common-plugin-api.aar, compose-ui-api.aar (host-api-aars.lock 锁定); README.md
|-- locks/                      host-api-aars.lock, host-shared-deps.lock
|-- AGENTS.md, ROADMAP.md, README.md (生成, 简体中文), LICENSE (MPL-2.0), THIRD_PARTY_NOTICES.md
|-- build.gradle.kts, settings.gradle.kts, gradle.properties, version.properties, gradlew(.bat)
`-- sign.properties             本地签名配置, Git 忽略
```

## 5. Gradle 与版本平台

### 5.1 在线平台版本插件

- 平台插件只在根 `settings.gradle.kts` 应用一次, 位于 `includeBuild("build-logic")` 之前; `build-logic/settings.gradle.kts` 不重复应用. 禁止 `mavenLocal()`.
- 根 `build.gradle.kts` 用 `System.getProperty("gradle.agp.version")` 声明 `com.android.application`, 用 `System.getProperty("gradle.kotlin.version")` 声明 `org.jetbrains.kotlin.plugin.compose`, 均 `apply false`; 模块只应用插件, 不写版本. 不声明 `org.jetbrains.kotlin.android` (AGP 9 内置 Kotlin 已覆盖, `JvmConventionPlugin` 在 AGP >= 9 时跳过该插件).
- `app` 模块从 `version.properties` 与 `org.autojs.build.versions` 读取 compileSdk / minSdk / targetSdk / versionCode / versionName.
- 版本逃生门只用 `version.properties` 的 `OVERRIDDEN_*`, 常规构建保持 `NONE`. 不提交 `gradle/data` 消费端覆盖.

平台验收命令 (Temurin 环境模拟, 必须只输出一段版本决策):

```powershell
.\gradlew.bat --no-daemon '-Djava.vendor=Eclipse Adoptium' '-Djava.vendor.version=Temurin-21.0.12.1+1' '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:testDebugUnitTest
```

### 5.2 仓库边界与依赖

- Gradle 构建 MUST 自包含, 禁止引用兄弟仓库或宿主的路径, JAR / AAR 或 `flatDir`.
- 宿主 AAR 只从 `libs/` 消费, 由 `locks/host-api-aars.lock` 锁定 SHA-256; `app/build.gradle.kts` 在配置期拒绝缺失文件, debug 产物, 占位哈希, 多余锁条目与摘要不符. 更新任一 AAR 时同一提交内更新锁文件, `libs/README.md` 与 `THIRD_PARTY_NOTICES.md`.
- `common-plugin-api.aar` 为 `implementation` (INFO 服务在插件自身进程回答宿主); `compose-ui-api.aar` 为 `compileOnly` (类由宿主提供, D26), 测试为 `testImplementation` / `androidTestImplementation`. JVM 测试提供整份共享依赖锁的运行时副本; 设备测试 APK 只携带插件自身也打包的部分 (下一条), 宿主独有构件在 `debugAndroidTestRuntimeClasspath` 同样排除, 以免测试进程掩盖插件进程缺类. P1.1 已冻结 `.loading` / `.model` / `.catalog` 的 V1. F.2 在独立 `.interop` 包附加通过能力协商加载的 AndroidView 接口; F.3 在 `.dialog` 包附加可选弹窗工厂与参数, 原 206 个 V1 / F.2 class 字节不变. 无引用且不属于冻结面的 `.spike` 版本 -1 已在本次制品维护中移除.
- Compose 依赖 (runtime / ui / foundation / material3 / animation / material-icons-core) 以 `implementation` 打进插件 APK, 由 BOM 管理版本; `ui-tooling` 只在 `debugImplementation`. P0.2 的共享依赖表 `locks/host-shared-deps.lock` 锁定宿主 app / inrt 的 debug / release 共用的 51 个构件, 全部 `compileOnly`; D32 (F.5, 2026-10-08 修订) 起其中插件 APK 在运行时引用的 47 项 (`standaloneRuntime` 集合: activity / annotation / arch core-common+core-runtime / collection / concurrent / core / customview-poolingcontainer / emoji2 / interpolator / lifecycle common+livedata-core+process+runtime+viewmodel+savedstate / profileinstaller / savedstate / startup / tracing / versionedparcelable / window / kotlinx-coroutines / kotlinx-serialization) 不再从 runtime classpath 排除, 由 Compose 传递带入并用 Gradle constraints 钉在锁定版本, 供画廊在插件自身进程运行; 只有无人引用的 appcompat 2 项仍排除 (livedata-core 与 kotlinx-serialization 由完整 keep 后保留的 savedstate / lifecycle 成员引用, 因此同样打包). build42 漏掉 arch core-runtime / poolingcontainer / emoji2 / window, 画廊从启动器打开即因 `NoClassDefFoundError` 崩溃, 而当时测试 APK 携带的宿主副本掩盖了缺口, 因此现在测试 APK 不再携带宿主独有构件. 宿主内 parent-first 使这些副本被宿主类遮蔽; `proguard-rules.pro` 对这些包与 `kotlin.**` 一样完整 `-keep` (不只是 `-keepnames`): build44 候选包只 `-keepnames` 时, R8 由 "插件程序从不初始化 EmojiCompat" 推断 `EmojiCompat.get()` 永不正常返回, 把 Compose 的 `EmojiCompatStatus` 编译成 `EmojiCompat.get(); throw null`, 在宿主内 (宿主已初始化 EmojiCompat) 每次文本布局都抛 NullPointerException; 完整 keep 使 R8 不再对共享副本做整程序假设, 代价是这些包不再裁剪. Compose 集成构件 (activity-compose / lifecycle-runtime-compose / savedstate-compose) 照旧打包. Kotlin stdlib 2.4.0 保留 `implementation`, 因 INFO / Wake 在插件自身进程也需要它, 装载渲染器时仍为 parent-first. `:app:verifySharedClasspath` 校验编译版本, 运行时打包集合恰好等于 `standaloneRuntime` 且版本等于锁, 测试 APK 不含宿主独有构件; `.python/verify_apk_classpath.py` 校验 APK 含独立运行时类而不含 appcompat 与契约类, 对 R8 发布包加 `--shrunk` 要求其引用的每个类都在 APK, 平台 (含 androidx.window 按需反射的 OEM 扩展) 或契约中; `.python/verify_standalone_launch.py` 在设备上安装 APK, 像启动器一样启动已启用的 alias, 打开首个条目与设置页并确认进程未崩溃 (测试 APK 与插件同进程, 只有这样才能证明插件进程自身可启动). Q1(b) 已由维护者于 2026-10-02 批准, 宿主版本在 `gradle/libs.versions.toml` 声明, `ComposeUiSharedClasspathTest` 与 `verifyComposeUiSharedClasspath` 守卫四个 runtime classpath. V1 契约现由宿主 `implementation` 打包; P1.2 的正式装载器与会话使用 V1, 负版本探针已退役. 最低正式宿主版本已于 P1.3 确认为 5316.
- 新增依赖优先 Maven Central / Google Maven. 不引入 `appcompat` / Material Components (XML 主题) 等 View 体系库; 独立界面全部用 Compose Material 3 实现. 主题色派生使用 `com.materialkolor:material-color-utilities` 4.1.1 (MIT, Material Color Utilities 的 Kotlin 移植), `StandalonePaletteTest` 用 Material Components 1.13.0 产出的 21 组色值校验与其他独立插件一致.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, 由 `.gitignore` 忽略 (`git check-ignore` 已验证).
- `appendDigestToReleasedFiles` 依赖 `assembleRelease`, 签名缺失时失败, 校验产物恰好为 `autojs6-plugin-compose-ui-v{VERSION_NAME}.apk` 一个, 校验签名, 校验原生库集合与 16 KB 对齐 (第 9 节), 再追加 CRC32 复制到 `releases/`.
- 构建产物不入库 (`releases/` 被忽略).

## 6. Manifest 与激活协议

- `org.autojs.permission.PLUGIN` 是唯一权限; `<queries>` 仅声明宿主包 `org.autojs.autojs6` (读取设置 Provider 与解析运行入口); 无 `uses-sdk` 覆盖, 无 `uses-feature`.
- application 级 meta-data 恰好四项: `org.autojs.plugin.WAKE_ACTIVITY` = `.WakeActivity`, `org.autojs.plugin.info.AUTHOR` = `@string/plugin_author`, `org.autojs.plugin.compose.RENDERER_FACTORY` = 渲染器工厂全限定类名, `requiresHostVersion` = 最低宿主 versionCode (application 级, 与 ImGui 插件同形; 宿主插件中心从 `PluginInfo.capabilities` 读取同一值).
- `WakeActivity`: exported, `Theme.NoDisplay`, PLUGIN 权限, `excludeFromRecents`, `finishOnTaskLaunch`, WAKE action + DEFAULT category, `onCreate` 立即 `finish()`.
- `ComposeUiPluginInfoService`: exported, enabled, PLUGIN 权限, intent-filter `org.autojs.plugin.INFO` + category `compose-ui`, 无 meta-data, 默认进程.
- F.5 组件: `.app.GalleryActivity` 与 `.app.SettingsActivity` 不导出, `configChanges="uiMode|locale|layoutDirection"`; 四个 `activity-alias` (`.launcher.AdaptiveLightIconAlias` / `AdaptiveDarkIconAlias` / `AdaptiveAutoIconAlias` / `TransparentIconAlias`) 指向画廊并各带 MAIN / LAUNCHER, 图标分别为 `ic_launcher_system_light` / `ic_launcher_system` / `ic_launcher_system_auto` / `ic_launcher`, 只有 Auto 默认启用; `.app.LauncherIconUpdateReceiver` 不导出, 只接收 `MY_PACKAGE_REPLACED`; `androidx.startup.InitializationProvider` 以 `tools:node="remove"` 移除, 安装包不声明任何 provider. 受 PLUGIN 权限保护的导出组件仍只有 Wake 与 INFO (`ManifestContractTest` 守卫).
- `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules` 排除全部域; application `theme` 为 `@style/Theme.ComposeUi` (仅窗口背景, 无 ActionBar), `localeConfig` 列出 10 语言; 不设 application `name`.
- 渲染器工厂类由宿主在宿主进程内按 meta-data 类名反射创建; 它不是 Android 组件, 不在 Manifest 中注册, 但 R8 规则 MUST 保留其类名与无参构造 (`proguard-rules.pro`). P0.2 额外要求保持 `kotlin.**` ABI, 关闭 R8 优化变换 (仍裁剪 / 混淆 / 缩减资源), 以插件私有包承载混淆类名, 并补回 compileOnly lifecycle 的 ViewModel 无参构造规则; 原因与失败栈见 `docs/dev/p0-spike-evidence.md`. D32 打包的共享 AndroidX / kotlinx-coroutines 副本同样完整 `-keep` (第 5.2 节): `-dontoptimize` 不阻止 R8 的整程序推断 (如 "永不正常返回" 的调用点被替换为 `throw null`), 只有完整 keep 才能让插件副本在宿主内与宿主副本行为一致.

## 7. PluginInfo 与能力协商

- `name` 来自不可翻译的 `app_name`, `description` 来自当前 locale 的 `plugin_description`, `instruction` 来自 `@raw/plugin_instruction`, `versionName` / `versionCode` 来自已安装包, `versionDate` 来自 `plugin_version_date` resValue, `id` / `engine` / `variant` / `author` 来自 `ComposeUiPlugin`.
- `supportedAbis` 恒为空数组 (D22: 单 APK 内置全部四种 ABI 的 graphics-path 辅助库, 对设备没有 ABI 限制), 在 `ComposeUiPluginInfoService.getInfo()` 中显式写出以便审计.
- INFO `capabilities` 仍只含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION`. P2 渲染器工厂通过 V1 `capabilities()` 协商契约版本, Compose 运行时版本, 共享依赖指纹和 `RendererCatalog` 声明的完整 30 项 V1 目录 (29 个节点组件与仅命令的 Snackbar), FEATURES 现在包含可选 `android-view-interop-v1` / `dialog-v1`, 配套类名 key 为 `androidViewFactoryV1` / `dialogFactoryV1`; 旧 V1 COMPONENTS 仍为 30 项, 新宿主协商后才加载独立扩展入口. 基础工厂与普通渲染路径不得直接引用扩展共享类型, 保证旧宿主类加载兼容性. P2.4 / P2.5 已补齐原生输入框, 列表, Scaffold, 顶部应用栏, 对话框与进度指示器. P3.1 - P3.5 已在匹配的本地宿主交付可调用的 `compose` / `$compose`, 29 个节点工厂, 句柄, 响应式 state/render/ref, batch/post/theme, ui 模式挂载与非 ui 脚本的 raw / 可调整悬浮窗, 以及错误 / 探测 / 生命周期守卫. 悬浮窗复用宿主授权, 缺失权限返回 `PERMISSION_REQUIRED`, 不自动弹出授权界面. P4 五例已随包提供并同步至匹配宿主的 Compose UI 示例分类; P5 健壮性, 六设备矩阵, 真实打包应用与性能基线见阶段证据. P6 已交付完整 API 文档, TypeScript 声明及配套 Ace / Offline Docs 本地同步; 性能复核规则按已确认的 Q5 记录在第 14 节.
- F.4 将基础契约版本提升为 2, 最低宿主改为 5322. `api.v2.ComposeUiV2` 追加 25 个节点 (19 类组件和 6 个配套子项), 基础工厂 COMPONENTS 共 55 项, 与已协商 AndroidView 合用时共 56 项. 宿主接受 V1/V2, 旧插件只允许其实际声明的组件; V2 插件在旧宿主的版本门禁被拒绝. 原 V1 目录, Parcelable, F.2 / F.3 接口继续保留, 不把历史 V1 扩展目录直接替换为 V2.
- V2 节点的交叉属性, 直接父子关系与树边界由共享验证器在宿主和插件发布前校验. Pager / Grid 的每节点子项上限遵循原 UiNode 的 2000, 会话总节点仍为 5000. 可见状态仍由脚本控制; 交互只排队发送事件, 不在 Compose 回调中直接执行 JS.
- F.6 的 `compose.memo` 是宿主 6.8.0 / 5323 的脚本 API, 只在宿主侧复用未变化的 render 片段, 不改变渲染器契约, 组件目录或本插件的最低宿主 5322; TSX 使用需要 TypeScript Engine 0.6.8. 证据见 `docs/dev/p7-f6-evidence.md`.
- 新增可选能力时先协商, 不通过捕获异常猜测协议版本.
- F.3 弹窗由插件创建真正的 Compose Dialog, UI 脚本使用其 Activity 窗口, 非 UI 使用宿主既有悬浮窗权限及 WindowTypeCompat. 宿主仅持有非交互 composition anchor, 会话仍计入共同上限. 锁定 Material3 1.4.0 的 ModalBottomSheet 没有公开的 show 前 overlay 类型参数, 因此底部弹层由公开 BottomSheetScaffold / SheetState 与模态 Dialog 组合, 不使用私有反射或提前升级依赖. 取消经独立生命周期事件排队返回宿主, 不在组合中调用 JS.

## 8. 进程内渲染器与公共 API 设计 (CONDITIONAL, P0.2 起)

- 常量, 属性键, 事件名, capability key, 错误码, 上限集中在宿主 `compose-ui-api` 契约 (路线图附录 B); 插件不散落字符串字面量.
- 渲染器只依赖契约接口与 Android / Compose 公共 API, 不反射宿主内部类; 需要宿主能力 (主题快照, 资源, 调度器) 时经契约接口由宿主注入.
- 线程模型按 D12: 渲染器只经 `ComposeUiEventSink.enqueue` 投递事件, 不在组合 / 测量 / 布局 / 绘制期间同步执行 JS; 补丁应用在主线程; 禁止双向等待.
- 全部输入按契约上限校验, 超限返回类型化错误, 不崩溃; 渲染器内部异常不得传播到宿主主线程, 统一转为契约错误回传脚本.
- 类加载边界 (D10 / D26): 契约类与 Android / Kotlin 标准库来自宿主 (parent 加载器), Compose 来自插件 APK (Kotlin 为 INFO 独立进程保留 APK 副本); 任何新增的 `compileOnly` 依赖都要在第 5.2 节与 `THIRD_PARTY_NOTICES.md` 登记并由 P0.2 的加载探针覆盖.
- AndroidView 工厂由宿主在主线程预先执行, 插件只接收原生 View 绑定, 不在 Compose 重组中调用工厂. 借用 View 保留旧 ui 监听器和自身行为, 不被转换为 Compose 回调队列; 不擅自销毁 WebView 或清除调用方监听器. View 的进程级弱身份占用由宿主 parent loader 中的互操作 helper 管理.
- 日志不记录脚本声明的界面文本正文, 只记录会话 id, 节点数, 耗时与错误码.

## 9. 原生库 (仅 Compose 传递依赖)

- 本仓库不包含 C / C++ 代码, 不使用 NDK, 不使用 `splits.abi`. APK 内唯一的原生库是 `androidx.graphics:graphics-path` 的 `libandroidx.graphics.path.so` (Compose `ui-graphics` 在 API 24 - 33 上用它迭代 Path; API 34+ 走平台 `PathIterator`), 四种 ABI 全部打包在同一个 APK 中, 不按 ABI 拆分 (每个约 10 KB, 拆分无收益).
- `app/build.gradle.kts` 的 `nativeAbis` / `allowedNativeLibraries` / `nativePageAlignment` 是唯一清单; `appendDigestToReleasedFiles` 校验 release APK 的 `lib/` 条目恰好是该集合, 每个条目未压缩 (`STORED`), ELF `PT_LOAD` 对齐 >= 16384 且 zip 数据偏移为 16384 的整数倍; `ComposeUiPluginContractTest` 在设备上校验同一集合并 `System.loadLibrary("androidx.graphics.path")`.
- 升级 Compose BOM 后若原生库集合变化 (新增 / 移除 / 更名), 同一提交内更新上述清单, androidTest 期望, `THIRD_PARTY_NOTICES.md`, README 文案与 changelog `dependency` 条目; 不得为通过校验而放宽对齐要求.
- 宿主经 D10 的 `PathClassLoader` 装载渲染器时, 必须把插件 APK 的原生库搜索路径 (`<apk>!/lib/<abi>`) 传给加载器, 否则 API 24 - 33 上的 Path 迭代会 `UnsatisfiedLinkError`; 这是 P0.2 加载探针的验证项.
- 若未来确有插件自有原生代码的需求, 先修订路线图 D22 并恢复参考规范第 5.4 节的全部 CONDITIONAL 条款.

## 10. 字符串资源

- 11 个目录: `values/` (默认英语) 与 `values-en/` 逐字相同, 另有 ar, es, fr, ja, ko, ru, zh, zh-rHK, zh-rTW. `xml/locales_config.xml` 列出 10 语言 (画廊与设置页参与系统的按应用语言设置). 外观与启动器图标相关文案 (`app_language_*`, `app_settings_*`, `launcher_icon_*`, `theme_picker_*`, `about_*`) 与其他独立插件同源, 新增的画廊文案以 `gallery_*` 为前缀.
- `strings.xml` 按 `name` 升序; 不可翻译项 (`app_name`) 放 `strings_donottranslate.xml`.
- 全部 locale 使用 ASCII 标点 (含日语, 韩语, 阿拉伯语的逗号与句号), 省略号写 `...` (lint 已全局禁用 `TypographyEllipsis`). `ApplicationTextPunctuationTest` 扫描 `app/src/main`, `.readme`, `.changelog`, `docs`, `README.md`, `ROADMAP.md`, `AGENTS.md`, `THIRD_PARTY_NOTICES.md` 的 xml / md / json.
- `plugin_description` 句尾无点号, 不含 "AutoJs6" 字样, 含 "Jetpack Compose" (`StringResourceParityTest`). 繁体中文 (台灣) 用 "指令碼 / 介面 / 轉譯", 繁体中文 (香港) 用 "腳本 / 界面 / 渲染".
- 图标由 Icon Studio 从 `.icons/recipe.json` 与 `.icons/assets/` 确定性生成 (`py .python/generate_icon_studio.py`; `generate_launcher_icons.py` 在 recipe 存在时转交同一入口), 不手工编辑 `mipmap*/` 输出. recipe 的亮 / 暗图稿即 P0.1 由 `.python/icons/compose-ui-ic-launcher-light.png` 生成的两张占位 `ic_launcher.png`, 正式源图到位后替换 recipe 图稿并重新生成, 运行 `--check`, 并更新第 2 节与 changelog. 不创建同名自适应 XML 或圆形 `ic_launcher` (`StringResourceParityTest` 守卫); 系统应用信息图标 `ic_icon_studio_application` 的自适应资源按文末 System application icon 节处理. recipe `launcher: true` 生成四个 alias 的 PNG (`ic_launcher_system`, `_light`, 两个前景与 `ic_launcher_monochrome`); `mipmap*/ic_launcher_system*.xml` (anydpi-v26 / notnight) 与 `values/ic_launcher_background*.xml` 为手工维护的静态资源, Auto 使用独立资源 ID, 不用 values alias.

## 11. README, 插件说明与 changelog

- `.readme/lang_*.json` (10 语言, 键集合一致, 列表键 `features` / `usage_steps` / `compatibility_points` / `faq_items` / `security_points`) 与 `.changelog/lang_*.json` 是唯一文案源; 生成物 (`README.md`, `.readme/README-*.md`, `app/src/main/assets/doc/CHANGELOG*.md`, `app/src/main/res/raw*/plugin_instruction.md`, 共 36 个) 不手工编辑.
- 修改 JSON 或模板后运行 `py .python/generate_markdown.py` 再 `--check`; CI `markdown.yml` 在 Windows 上执行 `.python/check_markdown.bat`.
- 根 `README.md` 为简体中文, 与 `.readme/README-zh-Hans.md` 同源. 快速开始示例以路线图附录 A 为准 (A.3 计数器, A.8 悬浮窗 HUD); 当前状态段落 MUST 如实说明 "1.1.0 本地开发预览, 两层 API 与 UI / 悬浮窗承载已实现, 五个示例已随包交付, 完整 API 参考与 TypeScript / Ace / Offline Docs 本地配套内容已提供, 需要匹配的本地宿主构建, 尚无官方发行版". 兼容性段落明确 TSX 需要匹配的 5319 宿主与内置 Compose 声明的 TypeScript Engine, 这些是历史版本的最低要求; 当前 V2 包统一需要匹配的 5322 宿主, TSX 配套需要 TypeScript Engine 0.6.7. `compose.memo` 由宿主 5323 提供, 兼容性列表与 1.1.0 提示须写明插件无需更新, TSX 需要 TypeScript Engine 0.6.8. 新宿主仍可使用旧 V1 插件的原有组件. `assets/examples/` 由 `index.json` 列出计数器, 表单, 1000 项列表, 非 ui 悬浮 HUD 与主题五例, 每例头部说明模式及权限前提; API 文档与声明按 P6 的目录守卫维护; HUD 示例关闭时停止自身工作线程, 不在关闭后继续更新节点.
- 修改示例后, 使用 `py .python/sync_examples.py --host <明确的宿主仓库路径>` 同步五个 JS 至宿主 `app/src/main/assets-app/sample/Compose UI/`, 清单同步至 `app/src/main/assets-app/indices/compose-examples.json`, 再以 `--check` 确认字节一致. 宿主通过 `AssetManager.list` 动态发现分类, 没有静态分类总表; 不在可执行示例目录放清单 JSON. 插件 Gradle 构建不依赖宿主目录.
- changelog 分类只用 `hint` / `feature` / `fix` / `improvement` / `dependency`; 简体中文依赖条目用 `附加` / `升级` / `降级` / `替换` / `移除`; 当前版本 key 为 `v{VERSION_NAME}` (忽略后缀), `released_date` 为当日 `YYYY/MM/DD`; 涉及 feature / fix / improvement / dependency 的提交 MUST 更新 10 语言 JSON.
- 文案面向使用者, 不写内部类拆分, 类加载细节或测试数量; 行为变化, 权限, 默认值与兼容性必须如实记录.

## 12. 独立界面与设置 (F.5 起适用)

- 画廊与设置页在插件自身进程运行, 不能引用 `org.autojs.plugin.compose.api` 契约类 (它们由宿主提供, 插件进程没有); `app/` 包只依赖 Android / Compose / `common-plugin-api` (设置 Provider 契约常量). 插件进程只有插件 APK 自带的类: 发布包引用的每个类都必须在 APK, 平台或契约中 (`verify_apk_classpath.py --shrunk`), 设备测试 APK 不携带宿主独有构件, 真实可启动性由 `verify_standalone_launch.py` 在设备上证明 (第 5.2 / 13.2 节). 画廊示例脚本用 `GalleryNode` 数据描述并序列化为 JS, `GalleryCatalogTest` 在 JVM 上用真实目录校验每个节点的属性 / 别名 / 快捷属性 / 插槽 / 事件 / 子节点策略, 每个节点组件与 Snackbar 恰好一条示例.
- 设置页遵循 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`: 外观分组顺序语言 -> 夜间模式 -> 主题色 -> 启动器图标, 关于分组列出版本 / 最低宿主 / 开发者; 选择器先改草稿, 确定才保存一次, 取消 / 返回 / 外部点击不保存; 主题色对话框提供跟随 AutoJs6, 16 色预设, HEX / RGB 输入与局部预览; 中性灰阶表面, 强调色按 HCT 规则派生并保证 4.5:1 可读. 跟随宿主只读取 `AutoJs6HostSettingsContract` Provider, 校验协议版本与包名, 宿主缺失时回退系统, 不写回宿主.
- 启动器图标按图标规范: 选择持久化为四个 alias 的组件启用状态, `LauncherIcons.select` 回滚失败并迁移可变快捷方式归属, 包更新后 `LauncherIconUpdateReceiver` 归一化; 默认 Auto.
- 发行历史仍由插件中心展示 `CHANGELOG-*.md`, 不另做页面.

## 13. 测试要求

### 13.1 JVM

- `ManifestContractTest` (权限, 仅宿主包 queries, application meta-data 四项与主题 / localeConfig, Wake 与画廊 / 设置 Activity, INFO 服务发现契约, 四个启动器 alias 及其图标与默认启用态, 更新接收器, startup provider 移除, 导出组件集合), `ComposeUiPluginRuntimeInfoTest` (PluginInfo 纯数据映射, 空 ABI, 身份常量对齐 `common.json` / `build.gradle.kts` / `proguard-rules.pro` / `settings.gradle.kts`), `StringResourceParityTest` (键集合与排序, 描述规则, 11 份 `plugin_instruction.md`, 图标文件), `ApplicationTextPunctuationTest`, `HostApiAarLockTest` (锁与文件摘要, AAR 纯字节码, 声明文件与 `libs/README.md` 复述摘要, 构建脚本消费的 id 集合).
- P1.1: 宿主契约模块的 `ComposeUiContractTest`, `ComponentCatalogConsistencyTest`, `ComposeUiModelTest`, `ComposeUiHostCompileGuardTest` 及设备 `ComposeUiParcelTest` 守卫冻结面与传输边界. 详细模型语义见 `docs/dev/compose-ui-plugin-protocol-v1.md`.
- P0.2 起: 类加载边界与依赖锁的 JVM 守卫; P2 起: 组件目录 / 补丁合并 / Modifier 链 / 事件队列的纯逻辑测试.
- F.5: `GalleryCatalogTest` (示例覆盖与属性校验), `AppearancePreferencesTest` (外观解析优先级与回退), `StandalonePaletteTest` (HCT 色值对照 Material Components 1.13.0, 可读性, 输入解析), `LauncherIconStatePolicyTest`; `StringResourceParityTest` 另校验 `locales_config.xml` 与 alias 图标资源.

### 13.2 Android instrumentation

- `ComposeUiPluginContractTest`: Wake Activity 契约与四项 application meta-data, 恰好一个已启用的启动器 alias 指向画廊且四个 alias 各有独立图标资源, INFO 服务 `getInfo()` 往返 (空 `supportedAbis`, capabilities 仅 `requiresHostVersion`), APK 为单文件且原生库恰好为四个 ABI 的 `libandroidx.graphics.path.so` 并可在当前设备加载, 不声明任何 provider.
- F.5 画廊 (在插件进程运行): `GalleryDeviceTest` (56 条示例逐一打开并组合预览, 复制到剪贴板, 运行 Intent 指向宿主并在宿主已安装时可解析, 设置入口), `SettingsDeviceTest` (选择不保存 / 取消无变化 / 确定只保存一次, 主题色输入校验与预设, 图标选择持久化, 宿主外观可选), `LauncherIconDeviceTest` (每种模式只剩一个启动器条目且资源独立, 混合状态归一化). 这些测试与插件同进程且测试 APK 带有契约 AAR 与测试库, 不能单独证明插件进程可启动: `py .python/verify_standalone_launch.py --serial <serial> --apk <apk>` 安装 debug 或已签名 release APK, 从已启用的启动器 alias 启动画廊, 打开首个条目与设置页, 以进程存活与 logcat 无 FATAL 为准, 本地与 CI 模拟器作业都在 instrumentation 后执行. 更换打包集合或 R8 规则后 MUST 重新在宿主侧跑完整设备矩阵, 证明 parent-first 装载未受影响.
- P0.2 起: 宿主侧加载探针 (在宿主仓库); P2 起: 渲染器在宿主进程内的组合 / 事件 / 生命周期用例 (宿主 androidTest), 本仓库保留不依赖宿主的渲染器单元 instrumentation.
- P4 示例守卫: 宿主 `ComposeExampleCatalogTest` 校验目录发现, Rhino 语法与运行模式; `ComposeExamplesDeviceTest` 逐字节比较已安装插件与宿主的示例资产并执行实际脚本, 不在示例内加入测试开关. 设备必须安装包含当前示例的插件 APK.
- 可选覆盖率使用 `-PcomposeUiCoverage=true`, JaCoCo 0.8.14 由版本目录锁定, 仅为测试插桩与报告使用, 不进入 release 包. 度量值与设备结果以阶段证据为准, 不以启用插桩代替通过验证.
- 设备池与证据等级见 `ROADMAP.md` 附录 E; 多台设备时用明确 serial; 不卸载用户的已安装应用, 不清空启动器数据, 不删除用户的 `/sdcard` 内容.

### 13.3 CI

- `build.yml`: JVM 测试编译, `testDebugUnitTest`, `verifySharedClasspath`, androidTest 与 release APK, lint debug / release, `verify_apk_classpath.py` (debug) 与 `--shrunk` (release); API 24 x86 与 API 35 x86_64 模拟器运行本仓库完整 instrumentation 并收集覆盖率 (含 graphics-path 原生库加载), 随后用 debug APK 执行 `verify_standalone_launch.py --uninstall`.
- `markdown.yml`: Windows 上 `check_markdown.bat`. `icon-studio.yml`: Ubuntu / Windows 上 `generate_icon_studio.py --check`.
- 源码仓库自 2026-10-04 公开后, 推送会触发上述工作流; 推送前仍须先完成第 14 节的本地验证, 远端结果不替代本地证据.
- `reactivecircus/android-emulator-runner` 把 `script` 的每一行作为独立命令执行, 不支持反斜杠续行; Gradle 命令 MUST 写在一行内 (2026-10-04 首次远端运行的两个模拟器作业因续行符被当作任务名而失败).

## 14. 验证顺序

```powershell
py .python/generate_markdown.py --check
py .python/generate_icon_studio.py --check
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:testDebugUnitTest
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease :app:verifySharedClasspath
py .python/verify_apk_classpath.py app/build/outputs/apk/debug/autojs6-plugin-compose-ui-v1.1.0.apk
py .python/verify_apk_classpath.py --shrunk app/build/outputs/apk/release/autojs6-plugin-compose-ui-v1.1.0.apk
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:connectedDebugAndroidTest
py .python/verify_standalone_launch.py --serial <serial> --apk app/build/outputs/apk/release/autojs6-plugin-compose-ui-v1.1.0.apk
```

- 纯文档改动只需前两条; 涉及源码的改动至少跑 JVM 测试与 debug 装配; 涉及 Manifest, 渲染器工厂, 类加载边界或依赖集合的改动必须在至少一台真机或 AVD 上跑 instrumentation 与独立进程启动检查 (`verify_standalone_launch.py`, 第 13.2 节), 并在宿主侧跑加载探针 (P0.2 起).
- Release 前额外执行 `:app:appendDigestToReleasedFiles`, 检查 `releases/` 恰好 1 个已签名 APK 且 CRC32 与内容一致.
- 性能基线见 `docs/dev/p5-performance-evidence.md` 与 `docs/dev/p5-performance-summary.json`. 维护者于 2026-10-03 按 Q5 确认以下第一版复核规则: 耗时采用实测 p90 约加 50% (向上取整至 5 ms), APK 与 DEX 方法引用数采用实测值加 10% (向上取整). 必须在相同设备, 工作负载, 预热 / 样本数与观察方法下比较, 并记录宿主 / 插件版本及源码提交; 改变环境时不得直接把候选值当作新基线.

| 可见性确认耗时 p90 | API24 x86 AVD | API35 Xiaomi Pad |
| --- | --- | --- |
| 暖 UI 首帧 | 750 ms | 435 ms |
| UI 单次 state 更新 | 130 ms | 190 ms |
| floaty 单次 state 更新 | 125 ms | 210 ms |
| UI 同回调 100 次赋值 | 115 ms | 160 ms |
| floaty 同回调 100 次赋值 | 125 ms | 240 ms |
| 暖 1000 项列表首帧 | 2090 ms | 1410 ms |

- APK 复核线为 **2612576 B**, DEX 方法引用总数为 **49194**; 对应原实测 release19 的 2375069 B / 44721 引用. 方法引用按各 DEX 的 `method_ids` 求和, 不称为唯一方法定义数. 每次收集 release 产物时记录这两项, 依赖或功能增长也需按同一规则复核.
- 超出任一复核线时 MUST 复测或记录可审查的原因与证据, 并由维护者明确确认是否接受. 在确认前保留既有基线和超线记录, 不通过静默放宽数值或重采样择优来消除差异. 工具输出和单次样本不代替维护者对波动及合理功能增长的判断.
- 冷启动, PSS 和全部滚动指标继续记录, 后续有稳定性证据再定复核线. 当前冷启动每设备仅 3 个样本, PSS 每设备仅 1 个进程序列; 帧预算超时与实际显示掉帧必须区分. 可见性确认计时包含查询 / 两帧回调 / 截图开销, 不能称为纯渲染耗时.
- 任何未执行的验证都在最终说明中明确列出原因.

## 15. 许可证, 安全与隐私

- `LICENSE` 为 MPL-2.0 完整文本, README 徽章与 `THIRD_PARTY_NOTICES.md` 一致; Jetpack Compose / AndroidX (Apache-2.0) 与宿主 AAR (MPL-2.0) 在声明文件中列出.
- `allowBackup=false` 且 `dataExtractionRules` 排除全部数据; 受 PLUGIN 权限保护的导出组件只有 Wake 与 INFO, 四个启动器 alias 是唯一的无权限导出组件; 不申请任何运行时权限, 不访问网络 (开发者页面只经系统 VIEW Intent 打开). 画廊不执行脚本, 复制只写入剪贴板, 运行只把脚本文本交给宿主的公开入口; 设置偏好存于插件私有 SharedPreferences, 不写回宿主.
- 渲染器在宿主进程内运行, 沿用宿主的脚本权限模型, 不扩大脚本可访问的系统能力; 不记录界面文本正文.
- 第三方组件的版本, 来源, 许可证与 SHA-256 (宿主 AAR) 记录在 `THIRD_PARTY_NOTICES.md`.

## 16. 参考项目路由

- 构建骨架, AAR 锁, 路线图 / AGENTS 格式, CI 矩阵, JVM / instrumentation 契约测试写法: `D:/idea-projects/AutoJs6-Plugin-Three-Shell-Terminal`, `D:/idea-projects/AutoJs6-Plugin-Three-Setup-Installer`
- INFO-only 注册与 application 级 `requiresHostVersion` 的插件形态: `D:/idea-projects/AutoJs6-Plugin-ImGui`
- 宿主: 插件中心 (`InstalledPluginRepository`, `PluginCapabilityResolver`), 契约模块布局 (`plugin-api/`), 既有 `ui` / `floaty` 模块与脚本调度器: `D:/idea-projects/AutoJs6`
- Jetpack Compose 公共文档 (只读): `https://developer.android.com/compose`

参考时以这些仓库的当前代码为准; 复制骨架后必须替换身份字段, URL, 文案, 常量, 版本与测试数据.

## System application icon (2026-10-05)

- The maintainer requested themed backgrounds for every official plugin's Android App info icon. The application icon and roundIcon now use `@mipmap/ic_icon_studio_application`; this supersedes earlier application-level `ic_launcher` or fixed-dark `ic_launcher_system` wiring. Existing transparent brand resources and launcher alias choices retain their roles.
- `.icons/recipe.json` and the portable Icon Studio 1.3 renderer generate separate system resources from the saved artwork, geometry and backgrounds. Default and night adaptive XML prevent legacy night PNGs from overriding the adaptive icon on modern Android. Projects supporting API 24/25 use circular compatibility PNGs; projects with minSdk >= 26 use unqualified anydpi XML without legacy application PNGs. Android system surfaces follow the system theme; Plugin Center follows the host theme.
- Regenerate and verify with `.python/generate_icon_studio.py --check`, including the application Manifest references. Launcher entries are limited to the four F.5 icon aliases that target the gallery (section 6); do not add other launcher entries, alter component identities or paint the Plugin Center PNG background. System icon updates require a rebuilt and installed APK; catalog updates alone do not replace it.
