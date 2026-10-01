# AutoJs6-Plugin-Compose-UI AGENTS.md

本文件是本仓库的工程约定, 由 `D:/idea-projects/AUTOJS6_PLUGIN_NEW_REPO_AGENTS.md` (AutoJs6 新插件仓库参考规范) 裁剪而来, 只保留对本仓库真实有效的条款. 路线图与阶段性决策见 `ROADMAP.md`; 本文件描述的是 "怎样改仓库", 路线图描述的是 "改什么". 同目录的 `AUTOJS6_PLUGIN_BLACK_N_WHITE_ADAPTIVE_ICON_AGENTS.md` (图标) 在图标相关改动时同样适用.

## 1. 规则等级与本仓库的适用范围

- `MUST`: 必须遵循. `SHOULD`: 默认遵循, 偏离时在仓库文档中说明原因. `CONDITIONAL`: 仅在对应能力落地后适用.
- 用户在当前任务中的明确要求优先于本文件.
- 本仓库是 **进程内渲染器插件** (路线图 D10 / D23): 宿主用 `PathClassLoader(插件 APK, parent = 宿主类加载器)` 在宿主进程内实例化渲染器工厂, 不存在 Binder 能力服务, 不存在插件自有进程中的界面. 参考规范中 Binder 服务, AIDL 冻结, 前台服务, 跨包 `queries` 的条款不适用.
- 单一通用 APK (D22, 经 P0.1 证据修正): 插件自身没有原生代码, 不使用 ABI 拆分; 唯一的原生库是 Compose `ui-graphics` 传递依赖 `androidx.graphics:graphics-path` 1.0.1 自带的 `libandroidx.graphics.path.so` (四种 ABI, 各约 10 KB, 16 KB 页对齐). 不使用 `autojs6-native-alignment` 插件, 没有原生库重建配方; `appendDigestToReleasedFiles` 校验 "原生库集合恰好为这四个文件, 未压缩, ELF `PT_LOAD` 与 zip 数据偏移均 16 KB 对齐" (第 9 节).
- 没有独立界面, 没有启动器入口 (D8): 不适用 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md`; 图标只需 `mipmap/ic_launcher.png` 与 `mipmap-night/ic_launcher.png` (插件中心与 README 使用), 不需要四个自适应 alias.
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
| 宿主契约标识 | 契约包 `org.autojs.plugin.compose.api` (宿主 `plugin-api/compose-ui-api`, 路线图 P1.1), `CONTRACT_VERSION = 1`; P0 阶段 `ComposeUiPlugin` 以字面量声明同一组值, P1.1 落地后改为引用 `ComposeUiIds` / `ComposeUiContract` |
| 最低宿主 versionCode | `ComposeUiPlugin.REQUIRED_HOST_VERSION` = 5308 (**临时值**, D29: 仓库创建时宿主快照为 6.8.0 / 5307, 首个可携带 `compose-ui-api` 与装载器的宿主构建为 5308; P1.3 回填确认后同步 Manifest, `common.json`, changelog 与测试) |
| 平台版本插件 | `io.github.supermonster003.autojs6-platform-versions` 1.8.3 (与兄弟仓库统一升级时再更新); 不使用 `autojs6-native-alignment` |
| Compose 版本 | BOM `2026.09.00` (runtime / ui / foundation / animation 1.12.1, material3 1.4.0, material-icons-core 1.7.8), 由 `gradle/libs.versions.toml` 单点声明; Compose 编译器插件版本 = `System.getProperty("gradle.kotlin.version")` (D25) |
| 发布文件名 | `autojs6-plugin-compose-ui-v{VERSION_NAME}-{CRC32}.apk` (1 个) |
| 原生库 / ABI | 无插件自有原生代码; `libandroidx.graphics.path.so` (graphics-path 1.0.1, 随 Compose BOM 变动) x `arm64-v8a` / `armeabi-v7a` / `x86_64` / `x86`, 单 APK 内置; `nativeAbis` / `allowedNativeLibraries` / `nativePageAlignment` 在 `app/build.gradle.kts` 单点声明 |
| 图标源图 | `.python/icons/compose-ui-ic-launcher-light.png` / `-dark.png` (1024 x 1024 RGBA, alpha 一致, 图案 `#272727` / `#D8D8D8`); `UI_GLYPH = 0.66`. **当前为临时占位图** (路线图 Q6), 维护者提供正式源图后替换并重新生成 |

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

- 维护者指示 (路线图 D7): 本插件仓库与对应的宿主改动当前仅作本地提交, 不推送到 GitHub 远端, 不登记官方索引, 不发 Release. 后续会话继续遵守, 直至维护者明确恢复推送.
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
|-- .github/workflows/          build.yml (JVM / APK / lint + API 24 x86 / API 35 x86_64 模拟器契约测试), markdown.yml
|-- .python/                    generate_markdown.py (+ .bat, check_markdown.bat), generate_launcher_icons.py, icons/
|-- .readme/                    common.json, lang_*.json x 10, template_readme.md, template_plugin_instruction.md, README-*.md (生成)
|-- app/
|   |-- src/main/java/io/github/supermonster003/autojs6/plugin/compose/ui/
|   |   |-- ComposeUiPlugin.kt                 身份常量
|   |   |-- ComposeUiPluginRuntimeInfo.kt      PluginInfo 的纯数据视图 (JVM 可测)
|   |   |-- ComposeUiPluginInfo.kt             Context -> RuntimeInfo -> PluginInfo / capabilities
|   |   |-- ComposeUiPluginInfoService.kt      IPluginInfoProvider
|   |   |-- WakeActivity.kt
|   |   `-- renderer/                          (路线图 P0.2 / P2 起: ComposeUiRendererFactoryImpl, 渲染器, 组件目录, 补丁应用)
|   |-- src/main/res/           values*/ x 11 (strings), mipmap*/ (生成), raw*/plugin_instruction.md (生成), xml/data_extraction_rules.xml
|   |-- src/main/assets/doc/    CHANGELOG*.md (生成)
|   |-- src/test/               JVM 契约与资源守卫 (第 13.1 节)
|   |-- src/androidTest/        激活 / 发现 / 打包契约 instrumentation (第 13.2 节)
|   |-- sm003.jks               本地签名密钥, Git 忽略
|   |-- build.gradle.kts / proguard-rules.pro
|-- build-logic/                org.autojs.build.{utils,versions,signs,properties,jvm-convention}
|-- docs/dev/                   各阶段证据 (P0.2 起)
|-- gradle/                     wrapper, libs.versions.toml
|-- libs/                       common-plugin-api.aar (host-api-aars.lock 锁定); README.md
|-- locks/                      host-api-aars.lock
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
- `common-plugin-api.aar` 为 `implementation` (INFO 服务在插件自身进程回答宿主); 未来的 `compose-ui-api.aar` 为 `compileOnly` (类由宿主提供, D26).
- Compose 依赖 (runtime / ui / foundation / material3 / animation / material-icons-core) 以 `implementation` 打进插件 APK, 由 BOM 管理版本; `ui-tooling` 只在 `debugImplementation`. P0.2 的 "共享依赖锁" (D26) 落地后, 与宿主重叠且必须由宿主提供的构件改为 `compileOnly` 并在此处登记.
- 新增依赖优先 Maven Central / Google Maven. 不引入 `appcompat` / Material Components (XML 主题) 等本插件不需要的 View 体系库.

### 5.3 签名与发布构建

- `sign.properties` 与 `app/sm003.jks` 从宿主复制到相同相对路径, 由 `.gitignore` 忽略 (`git check-ignore` 已验证).
- `appendDigestToReleasedFiles` 依赖 `assembleRelease`, 签名缺失时失败, 校验产物恰好为 `autojs6-plugin-compose-ui-v{VERSION_NAME}.apk` 一个, 校验签名, 校验原生库集合与 16 KB 对齐 (第 9 节), 再追加 CRC32 复制到 `releases/`.
- 构建产物不入库 (`releases/` 被忽略).

## 6. Manifest 与激活协议

- `org.autojs.permission.PLUGIN` 是唯一权限; 无 `<queries>`, 无 `uses-sdk` 覆盖, 无 `uses-feature`.
- application 级 meta-data 恰好四项: `org.autojs.plugin.WAKE_ACTIVITY` = `.WakeActivity`, `org.autojs.plugin.info.AUTHOR` = `@string/plugin_author`, `org.autojs.plugin.compose.RENDERER_FACTORY` = 渲染器工厂全限定类名, `requiresHostVersion` = 最低宿主 versionCode (application 级, 与 ImGui 插件同形; 宿主插件中心从 `PluginInfo.capabilities` 读取同一值).
- `WakeActivity`: exported, `Theme.NoDisplay`, PLUGIN 权限, `excludeFromRecents`, `finishOnTaskLaunch`, WAKE action + DEFAULT category, `onCreate` 立即 `finish()`.
- `ComposeUiPluginInfoService`: exported, enabled, PLUGIN 权限, intent-filter `org.autojs.plugin.INFO` + category `compose-ui`, 无 meta-data, 默认进程.
- 不存在 launcher intent-filter, activity-alias, receiver, provider; 导出组件只有上述两个且都受 PLUGIN 权限保护 (`ManifestContractTest` 守卫).
- `allowBackup=false`, `fullBackupContent=false`, `dataExtractionRules` 排除全部域; 不设 application `theme` 与 `name` (渲染器需要 `Application` 子类时再加并说明).
- 渲染器工厂类由宿主在宿主进程内按 meta-data 类名反射创建; 它不是 Android 组件, 不在 Manifest 中注册, 但 R8 规则 MUST 保留其类名与无参构造 (`proguard-rules.pro`).

## 7. PluginInfo 与能力协商

- `name` 来自不可翻译的 `app_name`, `description` 来自当前 locale 的 `plugin_description`, `instruction` 来自 `@raw/plugin_instruction`, `versionName` / `versionCode` 来自已安装包, `versionDate` 来自 `plugin_version_date` resValue, `id` / `engine` / `variant` / `author` 来自 `ComposeUiPlugin`.
- `supportedAbis` 恒为空数组 (D22: 单 APK 内置全部四种 ABI 的 graphics-path 辅助库, 对设备没有 ABI 限制), 在 `ComposeUiPluginInfoService.getInfo()` 中显式写出以便审计.
- `capabilities` 在 P0 只含 `PluginCapabilityKeys.REQUIRES_HOST_VERSION`; 契约版本, 组件目录与 Compose 版本随渲染器工厂 `capabilities()` 在 P2.1 协商 (路线图附录 B), 不提前声明尚未实现的能力.
- 新增可选能力时先协商, 不通过捕获异常猜测协议版本.

## 8. 进程内渲染器与公共 API 设计 (CONDITIONAL, P0.2 起)

- 常量, 属性键, 事件名, capability key, 错误码, 上限集中在宿主 `compose-ui-api` 契约 (路线图附录 B); 插件不散落字符串字面量.
- 渲染器只依赖契约接口与 Android / Compose 公共 API, 不反射宿主内部类; 需要宿主能力 (主题快照, 资源, 调度器) 时经契约接口由宿主注入.
- 线程模型按 D12: 渲染器只经 `ComposeUiEventSink.enqueue` 投递事件, 不在组合 / 测量 / 布局 / 绘制期间同步执行 JS; 补丁应用在主线程; 禁止双向等待.
- 全部输入按契约上限校验, 超限返回类型化错误, 不崩溃; 渲染器内部异常不得传播到宿主主线程, 统一转为契约错误回传脚本.
- 类加载边界 (D10 / D26): 契约类与 Android / Kotlin 标准库来自宿主 (parent 加载器), Compose 来自插件 APK; 任何新增的 `compileOnly` 依赖都要在第 5.2 节与 `THIRD_PARTY_NOTICES.md` 登记并由 P0.2 的加载探针覆盖.
- 日志不记录脚本声明的界面文本正文, 只记录会话 id, 节点数, 耗时与错误码.

## 9. 原生库 (仅 Compose 传递依赖)

- 本仓库不包含 C / C++ 代码, 不使用 NDK, 不使用 `splits.abi`. APK 内唯一的原生库是 `androidx.graphics:graphics-path` 的 `libandroidx.graphics.path.so` (Compose `ui-graphics` 在 API 24 - 33 上用它迭代 Path; API 34+ 走平台 `PathIterator`), 四种 ABI 全部打包在同一个 APK 中, 不按 ABI 拆分 (每个约 10 KB, 拆分无收益).
- `app/build.gradle.kts` 的 `nativeAbis` / `allowedNativeLibraries` / `nativePageAlignment` 是唯一清单; `appendDigestToReleasedFiles` 校验 release APK 的 `lib/` 条目恰好是该集合, 每个条目未压缩 (`STORED`), ELF `PT_LOAD` 对齐 >= 16384 且 zip 数据偏移为 16384 的整数倍; `ComposeUiPluginContractTest` 在设备上校验同一集合并 `System.loadLibrary("androidx.graphics.path")`.
- 升级 Compose BOM 后若原生库集合变化 (新增 / 移除 / 更名), 同一提交内更新上述清单, androidTest 期望, `THIRD_PARTY_NOTICES.md`, README 文案与 changelog `dependency` 条目; 不得为通过校验而放宽对齐要求.
- 宿主经 D10 的 `PathClassLoader` 装载渲染器时, 必须把插件 APK 的原生库搜索路径 (`<apk>!/lib/<abi>`) 传给加载器, 否则 API 24 - 33 上的 Path 迭代会 `UnsatisfiedLinkError`; 这是 P0.2 加载探针的验证项.
- 若未来确有插件自有原生代码的需求, 先修订路线图 D22 并恢复参考规范第 5.4 节的全部 CONDITIONAL 条款.

## 10. 字符串资源

- 11 个目录: `values/` (默认英语) 与 `values-en/` 逐字相同, 另有 ar, es, fr, ja, ko, ru, zh, zh-rHK, zh-rTW. 没有 `locales_config.xml` (无界面, 不参与系统的按应用语言设置).
- `strings.xml` 按 `name` 升序; 不可翻译项 (`app_name`) 放 `strings_donottranslate.xml`.
- 全部 locale 使用 ASCII 标点 (含日语, 韩语, 阿拉伯语的逗号与句号), 省略号写 `...` (lint 已全局禁用 `TypographyEllipsis`). `ApplicationTextPunctuationTest` 扫描 `app/src/main`, `.readme`, `.changelog`, `docs`, `README.md`, `ROADMAP.md`, `AGENTS.md`, `THIRD_PARTY_NOTICES.md` 的 xml / md / json.
- `plugin_description` 句尾无点号, 不含 "AutoJs6" 字样, 含 "Jetpack Compose" (`StringResourceParityTest`). 繁体中文 (台灣) 用 "指令碼 / 介面 / 轉譯", 繁体中文 (香港) 用 "腳本 / 界面 / 渲染".
- 图标由 `.python/generate_launcher_icons.py` 从 `.python/icons/compose-ui-ic-launcher-light.png` 确定性生成, 不手工编辑 `mipmap*/` 输出; 修改源图或比例后重新生成, 运行 `--check`, 并更新第 2 节与 changelog. 不创建同名自适应 XML 或圆形图标 (`StringResourceParityTest` 守卫).

## 11. README, 插件说明与 changelog

- `.readme/lang_*.json` (10 语言, 键集合一致, 列表键 `features` / `usage_steps` / `compatibility_points` / `faq_items` / `security_points`) 与 `.changelog/lang_*.json` 是唯一文案源; 生成物 (`README.md`, `.readme/README-*.md`, `app/src/main/assets/doc/CHANGELOG*.md`, `app/src/main/res/raw*/plugin_instruction.md`, 共 36 个) 不手工编辑.
- 修改 JSON 或模板后运行 `py .python/generate_markdown.py` 再 `--check`; CI `markdown.yml` 在 Windows 上执行 `.python/check_markdown.bat`.
- 根 `README.md` 为简体中文, 与 `.readme/README-zh-Hans.md` 同源. 快速开始示例以路线图附录 A 为准 (A.3 计数器, A.8 悬浮窗 HUD); 渲染能力交付前, 状态段落 MUST 如实说明 "P0 开发预览, 尚不能渲染".
- changelog 分类只用 `hint` / `feature` / `fix` / `improvement` / `dependency`; 简体中文依赖条目用 `附加` / `升级` / `降级` / `替换` / `移除`; 当前版本 key 为 `v{VERSION_NAME}` (忽略后缀), `released_date` 为当日 `YYYY/MM/DD`; 涉及 feature / fix / improvement / dependency 的提交 MUST 更新 10 语言 JSON.
- 文案面向使用者, 不写内部类拆分, 类加载细节或测试数量; 行为变化, 权限, 默认值与兼容性必须如实记录.

## 12. 独立界面与设置 (不适用)

- 本插件没有独立界面, 设置页, 关于页或发行历史页 (D8); 插件中心展示 `plugin_instruction` 与 `CHANGELOG-*.md` 即可. 若路线图修订引入界面, 再按 `AUTOJS6_PLUGIN_STANDALONE_SETTINGS_AGENTS.md` 补齐.

## 13. 测试要求

### 13.1 JVM

- `ManifestContractTest` (权限, 无 queries, application meta-data 四项, Wake Activity, INFO 服务发现契约, 无 launcher / alias / receiver / provider, 导出组件集合), `ComposeUiPluginRuntimeInfoTest` (PluginInfo 纯数据映射, 空 ABI, 身份常量对齐 `common.json` / `build.gradle.kts` / `proguard-rules.pro` / `settings.gradle.kts`), `StringResourceParityTest` (键集合与排序, 描述规则, 11 份 `plugin_instruction.md`, 图标文件), `ApplicationTextPunctuationTest`, `HostApiAarLockTest` (锁与文件摘要, AAR 纯字节码, 声明文件与 `libs/README.md` 复述摘要, 构建脚本消费的 id 集合).
- P0.2 起: 类加载边界与依赖锁的 JVM 守卫; P2 起: 组件目录 / 补丁合并 / Modifier 链 / 事件队列的纯逻辑测试.

### 13.2 Android instrumentation

- `ComposeUiPluginContractTest`: Wake Activity 契约与四项 application meta-data, 无启动器入口, INFO 服务 `getInfo()` 往返 (空 `supportedAbis`, capabilities 仅 `requiresHostVersion`), APK 为单文件且原生库恰好为四个 ABI 的 `libandroidx.graphics.path.so` 并可在当前设备加载, 无导出 provider.
- P0.2 起: 宿主侧加载探针 (在宿主仓库); P2 起: 渲染器在宿主进程内的组合 / 事件 / 生命周期用例 (宿主 androidTest), 本仓库保留不依赖宿主的渲染器单元 instrumentation.
- 设备池与证据等级见 `ROADMAP.md` 附录 E; 多台设备时用明确 serial; 不卸载用户的已安装应用, 不清空启动器数据, 不删除用户的 `/sdcard` 内容.

### 13.3 CI

- `build.yml`: JVM 测试编译, `testDebugUnitTest`, androidTest 与 release APK, lint debug / release; API 24 x86 与 API 35 x86_64 模拟器运行契约测试 (含 graphics-path 原生库加载).
- `markdown.yml`: Windows 上 `check_markdown.bat`.
- 仓库未推送期间工作流只做本地语法与路径校验.

## 14. 验证顺序

```powershell
py .python/generate_markdown.py --check
py .python/generate_launcher_icons.py --check
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:testDebugUnitTest
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug :app:assembleRelease
.\gradlew.bat '-Pautojs.gradle.build.number.auto.increment.enabled=false' '-Pautojs.gradle.build.time.update.enabled=false' :app:connectedDebugAndroidTest
```

- 纯文档改动只需前两条; 涉及源码的改动至少跑 JVM 测试与 debug 装配; 涉及 Manifest, 渲染器工厂, 类加载边界或依赖集合的改动必须在至少一台真机或 AVD 上跑 instrumentation, 并在宿主侧跑加载探针 (P0.2 起).
- Release 前额外执行 `:app:appendDigestToReleasedFiles`, 检查 `releases/` 恰好 1 个已签名 APK 且 CRC32 与内容一致.
- 性能回归阈值 (路线图 P6 落档后填入): 首帧耗时, 补丁应用延迟, APK 大小.
- 任何未执行的验证都在最终说明中明确列出原因.

## 15. 许可证, 安全与隐私

- `LICENSE` 为 MPL-2.0 完整文本, README 徽章与 `THIRD_PARTY_NOTICES.md` 一致; Jetpack Compose / AndroidX (Apache-2.0) 与宿主 AAR (MPL-2.0) 在声明文件中列出.
- `allowBackup=false` 且 `dataExtractionRules` 排除全部数据; 导出组件最小化并受签名权限保护; 不申请任何运行时权限.
- 渲染器在宿主进程内运行, 沿用宿主的脚本权限模型, 不扩大脚本可访问的系统能力; 不记录界面文本正文.
- 第三方组件的版本, 来源, 许可证与 SHA-256 (宿主 AAR) 记录在 `THIRD_PARTY_NOTICES.md`.

## 16. 参考项目路由

- 构建骨架, AAR 锁, 路线图 / AGENTS 格式, CI 矩阵, JVM / instrumentation 契约测试写法: `D:/idea-projects/AutoJs6-Plugin-Three-Shell-Terminal`, `D:/idea-projects/AutoJs6-Plugin-Three-Setup-Installer`
- INFO-only 注册与 application 级 `requiresHostVersion` 的插件形态: `D:/idea-projects/AutoJs6-Plugin-ImGui`
- 宿主: 插件中心 (`InstalledPluginRepository`, `PluginCapabilityResolver`), 契约模块布局 (`plugin-api/`), 既有 `ui` / `floaty` 模块与脚本调度器: `D:/idea-projects/AutoJs6`
- Jetpack Compose 公共文档 (只读): `https://developer.android.com/compose`

参考时以这些仓库的当前代码为准; 复制骨架后必须替换身份字段, URL, 文案, 常量, 版本与测试数据.
