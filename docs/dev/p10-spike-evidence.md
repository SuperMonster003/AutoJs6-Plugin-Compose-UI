# P10.0 嵌入与快照 spike 证据 (2026-10-15)

路线图 `docs/dev/p10-ui-xml-designer-roadmap.md` 的 P10.0: 用一个临时的宿主 Messenger 服务与插件 debug 页, 验证 "宿主进程用真实 `ui` 模块 inflate, 经 `SurfaceControlViewHost` 嵌进设计器画布" 的每个疑点. 全部代码为一次性 spike, 不是契约: 宿主在分支 `spike/ui-preview-p10` (提交 `c80736621f`, 基于 master `686f402e55`, 以 `-PcomposeUiSpike=true` 打成独立包名 `org.autojs.autojs6.compose.spike`), 插件在 `app/src/debug/.../spike/UiPreviewSpikeActivity.kt` (仅 debug 包, `am start -n <插件>/.spike.UiPreviewSpikeActivity`). 驱动脚本 (uiautomator 点击, 截图, logcat 解析) 只在本会话的临时目录, 未入库.

## 1. 结论

| 疑点 | 结论 | 证据等级 |
| --- | --- | --- |
| 宿主能否在非 Activity 容器里跑 `ui.layout` | 能. `UiPreviewHost` 接口 (约 30 行 `UI.kt` 改动) 以引擎标签取代 `ScriptExecuteActivity`, 以主线程的 `LoopBasedJavaScriptEngine.execute` 运行脚本 (与 ui 模式同形), 内容进入嵌入根; 非主线程运行时 WebView 调用即抛错, 因此预览执行必须是主线程 ui 模式语义 | E2 / E3 |
| 嵌入显示 | API 30 / 33 / 35 / 37 模拟器与真机都显示; 根容器要自己套上 `ScriptTheme` 的 `windowBackground`, 否则黑底 | E3 |
| 触摸到达嵌入树 | 只有 `SurfaceView.setZOrderOnTop(true)` 时成立: 表面在宿主窗口之下 (默认或 `setZOrderMediaOverlay`) 时输入分发器把嵌入窗口排在 Activity 窗口之后, 触点落在 Activity 窗口上; 置顶后输入分发器出现 `EmbeddedWindow{...}` 条目 (API 30) / `Embedded{...}` (API 33+), 点击, 计数, 下拉框都到达 | E3 |
| 输入法 | API 35 (小米平板, HyperOS) 与 API 37 模拟器弹出键盘并输入成功, 两者用 API 35 的 `InputTransferToken` 构造 (客户端 `SurfaceView.rootSurfaceControl.inputTransferToken`), API 37 另加 `LayoutParams(w, h, focusable = true)`; API 30 / 33 (模拟器与 Redmi) 用 `IBinder hostToken` 构造, 输入框能获得焦点但键盘不弹, 输入分发器标记 `canReceiveKeys=false` | E3 |
| 弹出层 (Spinner 下拉, 同机制覆盖 Toolbar 菜单) | API 33 / 35 / 37 正常 (系统窗口, 锚定在嵌入控件上); API 30 直接让宿主进程崩溃 (`BadTokenException: token ViewRootImpl$W is not valid`, PopupWindow 走 `WindowManagerGlobal.addView`) | E3 |
| `dialogs.alert` | Redmi (API 33, MIUI) 与 API 33 模拟器显示为宿主的悬浮窗类型对话框, 遮住整个设计器; 小米平板 (HyperOS) 未显示, adb `appops` 授予的悬浮窗权限不被 HyperOS 承认, 需用户在系统设置授予 | E3 |
| 画布缩放 | 客户端 `graphicsLayer` 缩放 SurfaceView 生效 (内容随之放大); 宿主以 1.5 倍 `densityDpi` 的 `createConfigurationContext` 重排后尺寸完全不变 (AutoJs6 的尺寸解析不走视图 Context 的密度). 缩放在插件侧做, 命中按缩放换算; 缩放态下的触摸未测 | E2 |
| 夜间模式 | 宿主按夜间配置重排与系统深色模式切换 (`notifyConfigurationChanged`, API 33) 都不崩溃且无视觉变化: `ScriptTheme` 只有浅色, 真实 ui 脚本同样如此, 不是预览偏差 | E2 |
| 后台 / 旋转 / 杀进程 | 后台 (`surfaceDestroyed`) 后恢复必须向宿主重新索取 `SurfacePackage` (`getSurfacePackage()` 可重复调用) 再 `setChildSurfacePackage`, 直接复用旧包画面为空; 旋转经 `configChanges` 不重建, `surfaceChanged` -> 宿主 `relayout` 正常; 杀掉插件进程后服务解绑并释放会话 | E3 |
| 快照 (API 24 / 28) | 离屏 `measure / layout / draw` 可行: API 24 x86 模拟器 1032 x 448 (1.85 MB 原始像素) 宿主绘制 44 ms, 往返 64 ms; G8441 (API 28, 横屏 1152 x 90) 绘制 131 ms, 往返 139 ms; 位图经调用方 fd 写入并以 SHA-256 核对 | E2 / E3 |
| 节点边界 | 包装 `ui` 模块的 `LayoutInflaterDelegate.afterCreateView`, 以 DOM 元素先序号为键回传 18 项 (12 个元素 + 列表项实例重复模板的先序号), 布局后约 200 ms 内到达, 坐标即客户端 SurfaceView 像素 | E3 |
| 首帧耗时 | 创建会话到内容进入嵌入根 (含引擎创建) 0.5 - 1.2 s (API 33 模拟器 0.7 s, 小米平板 1.4 s, Redmi 3.3 s 含冷启动), 再次运行 0.2 s | E3 |

## 2. 设备

| 设备 | API | 模式 | 说明 |
| --- | --- | --- | --- |
| AVD_API_30 (emulator-5560) | 30 | embedded | 触摸 (置顶后) 通过, 输入法否, Spinner 弹出层崩溃宿主; `screencap` 在 SurfaceView 存在时整屏黑 (合成伪影, 真机与 API 33 无此现象) |
| AVD_API_33 (emulator-5586) | 33 | embedded | 触摸, 弹出层, 对话框, 缩放, 夜间, 配置变更, 后台 / 旋转 / 杀进程 |
| Xiaomi Pad 23046RP50C (968e9f18, HyperOS) | 35 | embedded | 触摸, 输入法 (API 35 路径), 弹出层, 恢复后重新附着; 对话框被系统拦截 |
| Redmi 22120RN86C (bek749scrwv4wo8h, MIUI) | 33 | embedded | 触摸, 弹出层, 对话框, 恢复后重新附着; 输入法否 |
| AVD_API_37.1_16K (emulator-5590) | 37 | embedded | 输入法 (API 35 路径 + focusable 参数), 弹出层, 恢复后重新附着 |
| AVD_API_24 只读实例 (emulator-5570, Nexus 5 尺寸) | 24 | snapshot | 位图与边界 |
| Sony G8441 (BH900ASK9E) | 28 | snapshot | 位图与边界 |

AVD_API_30 / 33 是另一会话 (3-Maple Image) 启动并在跑测试的实例, 本会话在不知情时使用了约 30 分钟并已告知对方; 三台真机经该会话同意借用. 结束时所有设备上的 spike 宿主与插件 debug 包已卸载, 三台真机装回 1.5.1 release (versionCode 144); 已安装的 AutoJs6 未改动.

## 3. 对照: 列表绑定报错不是预览偏差

spike 脚本的 `<list>` 模板 `{{name}}` 在预览中每项都抛 `ReferenceError: "name" is not defined`, 以同一脚本在 spike 宿主的真实 ui 模式 (`RunIntentActivity` + `script`), 以及官方示例 `sample/控件/列表控件.js` 在 Redmi 上已安装的正式 AutoJs6 (master `6e09d9abfd` 构建) 中运行, 都得到同样的错误 (175 行日志). 这是宿主当前构建的列表绑定问题, 与预览无关; 已作为独立任务建议交给维护者.

## 4. 回填路线图的设计决定

- 预览执行 = 主线程 ui 模式语义: 宿主预览会话以 `ScriptEngineManager.createEngineOfSourceOrThrow` 创建引擎, 设置工作目录 / 源码 / 预览承载标签后在主线程 `LoopBasedJavaScriptEngine.execute`, 脚本体结束后引擎保持存活, 释放时 `forceStop` + `destroy`; 不经 `ScriptEngineService.execute` (Q20 的 (c) 形态, spike 为此在 `ScriptEngineService` 加了一个 `getScriptEngineManager()` 访问器).
- 嵌入表面必须 `setZOrderOnTop(true)`, 因此插件的 Compose overlay 画不到表面之上: 编辑模式的指针拦截, 点选命中与高亮矩形由宿主根容器完成并经回调回传 (spike 的 `setIntercept` / `highlight` / `onTap`); 画布缩放在插件侧做 (SurfaceView 缩放), 宿主不改密度.
- 会话生命周期: `surfaceDestroyed` 后保留会话, `surfaceCreated` 时索取新的 `SurfacePackage` (契约加 `reattach`); 旋转与分栏只走 `resize`.
- 根容器使用主题 `windowBackground`; 边界以 DOM 元素先序号为键, 列表项实例与模板同号 (契约应标记项实例或只回传模板).
- 版本门禁 (Q21 的建议): 嵌入显示 API 30+; 交互预览 (放行指针) 要求 API 33+ (API 30 - 32 的弹出层会让宿主进程崩溃, 不能靠捕获异常兜底); 嵌入树内的输入法要求 API 35+ 并用 `InputTransferToken` 构造, API 33 - 34 上 `<input>` 在预览中提示 "需要 Android 15 才能在预览中输入"; API 24 - 29 快照.
- 残余偏差新增: HyperOS 等需要用户在系统设置授予宿主的悬浮窗权限, 否则 `dialogs.*` 在预览中不显示 (与非 ui 脚本运行时一致).

## 5. 文件

- 宿主分支 `spike/ui-preview-p10`: `core/ui/preview/UiPreviewHost.kt` (承载接口 + 追踪 delegate), `core/ui/preview/spike/UiPreviewSpikeService.kt` (Messenger 服务, 会话, 根容器, 边界, 快照, 重新附着, API 35 / 37 路径), `runtime/api/augment/ui/UI.kt` (`layout` 分流到预览承载, `inflate` 无 Activity 时用承载的 Context), `engine/ScriptEngineService.java` (引擎管理器访问器), `AndroidManifest.xml` (服务, PLUGIN 权限, action `org.autojs.autojs6.action.UI_PREVIEW_SPIKE`).
- 插件 debug: `spike/UiPreviewSpikeActivity.kt` (Messenger 客户端, SurfaceView + overlay, 快照显示, 开关: 模式 / 拦截 / 置顶 / 缩放 / 密度 / 夜间 / 视口), `src/debug/AndroidManifest.xml` (页面与 spike 宿主包的 `<queries>`).
