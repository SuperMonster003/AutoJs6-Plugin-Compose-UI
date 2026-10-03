<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 腳本提供 Jetpack Compose 與 Material 3 界面渲染能力的插件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 語言

******

本文件支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- 繁體中文 (香港) [zh-Hant-HK] # 當前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### 簡介

******

Compose UI 是 AutoJs6 的界面渲染插件. 腳本透過宿主提供的 `compose` / `$compose` 入口宣告界面, 插件在宿主進程內以 Jetpack Compose 與 Material 3 完成渲染. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 腳本的懸浮窗.

插件不包含任何獨立界面, 也不在啟動器中顯示入口. 宿主透過 INFO 服務發現插件並讀取版本與兼容資訊, 再按契約 (`org.autojs.plugin.compose.api`) 在宿主進程內載入渲染器. 界面樹, 狀態與事件在腳本側描述, 渲染器只負責把補丁套用到 Compose 組合並把用戶事件回傳給腳本.

******

### 當前狀態

******

P5 開發預覽: 可呼叫的 compose / $compose 入口, 29 個節點工廠, 長期持有的句柄, 響應式 state/render/ref, batch/post/theme, ui 腳本掛載及 raw / 可調整懸浮窗已可在匹配的本地 AutoJs6 宿主構建中使用. 已包含可用性探測, 型別化錯誤與會話清理. 已附帶計數器, 表單, 1000 項列表, 懸浮 HUD 與主題五個示例, 同步至匹配宿主的 Compose UI 示例分類. 已完成穩健性檢查, 六台裝置/模擬器相容矩陣及打包應用驗證, 並記錄效能基線. 完整 API 文件及型別宣告將於 P6 交付. 目前僅為本地預覽, 尚無官方發行版.

******

### 功能特性

******

目前開發預覽的核心能力:

- 宣告式界面: `compose.state` + `compose.mount(render)` 按狀態變化自動重繪, 同時提供可長期持有的節點句柄 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 組件核心集: 佈局 (Column / Row / Box / LazyColumn 等), 文字, 按鈕, 輸入框, 開關, 滑桿, 進度條, 卡片, 對話框等
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 作用域操作在宿主側校驗
- 兩種承載方式: `"ui";` 腳本透過 `compose.mount` 或可呼叫的 `compose` / `$compose` 掛載 Activity 內容; `compose.floaty` 建立 raw 或可調整懸浮窗, 同樣支援非 ui 腳本
- 進程內渲染: 渲染器在宿主進程中運行, 沒有跨進程界面橋接, 事件與狀態更新低延遲
- 單一安裝包: 不區分 ABI, 不含插件自有原生代碼 (僅隨 Compose 附帶的 AndroidX graphics-path 輔助庫, 四種 ABI 全部內置), 一個 APK 適配所有裝置
- 腳本入口: `compose` / `$compose`, 29 個節點工廠與長期持有的句柄, 以及 `compose.ref`, `compose.batch`, `compose.post`, `compose.theme`
- 整合守衛: 插件缺失或不相容時可用性探測回傳不可用, 錯誤使用 `ComposeError`, 關閉會話或停止腳本會釋放所屬窗口與回呼
- 計數器, 表單驗證, 1000 項鍵控列表, 非 ui 懸浮 HUD 與主題五個可執行示例, 包含前置條件與索引, 同步至匹配宿主的 Compose UI 示例分類

******

### 使用方式

******

1. 安裝包含 compose 腳本入口的匹配本地 AutoJs6 構建 (最低 6.8.0 / 5316)
2. 安裝本插件 APK (無需開啟, 插件沒有啟動器入口)
3. 在 AutoJs6 的插件中心確認 Compose UI 已被識別並處於啟用狀態
4. 在腳本中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮窗權限再使用 `compose.floaty`

******

### 快速開始

******

以下計數器與懸浮窗 HUD 均可在匹配的本地預覽宿主中運行. 運行 HUD 前需授予宿主在其他應用程式上層顯示的權限:

```js
"ui";

// 計數器 (宣告式 render 層)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `已點擊 ${count.value} 次`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, '加一'),
]));
```

```js
// 懸浮窗 HUD (節點句柄層)
let status = compose.Text({ text: '準備中...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, '關閉'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `進度 ${i}%` }));
    }
});
```

五個可執行示例隨包放在 `assets/examples/`, 由 `index.json` 列出; 匹配宿主的 "示例代碼 > Compose UI" 提供相同腳本 (`sample/Compose UI/`). 每例開首說明執行模式與權限前提. 完整 API 說明與 TypeScript 型別宣告仍待交付, 目前 API 形態以路線圖附錄 A 為準.

******

### 兼容性

******

插件的運行要求與限制:

- 最低 AutoJs6 版本: 6.8.0 (5316) 或更高; 低於該版本的宿主會在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內置全部四種, 無需按架構選擇安裝包)
- Compose 版本: 由插件自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 運行時
- 契約版本: 1; 宿主與插件透過契約版本協商, 不匹配時拒絕載入並給出明確錯誤

******

### 常見問題

******

- 為什麼安裝後找不到插件圖示? 插件沒有獨立界面, 也不會在啟動器顯示, 請在 AutoJs6 的插件中心查看
- 為甚麼找不到 `compose`? 全域物件由匹配的本地宿主構建提供, 單獨安裝插件 APK 不會加入該入口
- 是否需要卸載其它界面插件? 不需要, Compose UI 與現有 `ui` 模組及其它插件互不影響
- 插件更新後腳本需要修改嗎? 契約版本不變時無需修改; 契約升級會在更新日誌中明確標註
- 懸浮窗需要甚麼條件? 先授予宿主懸浮窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示窗口, 請先返回桌面. 缺少權限會回傳 PERMISSION_REQUIRED, 不會自動彈出授權界面

******

### 權限與安全

******

插件不申請任何 Android 運行時權限, 也不存取網絡, 儲存空間或感應器.

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以存取
- 無背景行為: 插件沒有常駐服務, 廣播接收器或定時任務, 不被宿主載入時不消耗資源
- 數據邊界: 插件不讀寫腳本數據與用戶檔案, 界面狀態只存在於宿主進程記憶體中
- 備份策略: 已停用應用備份與裝置遷移, 插件本身不持有任何需要遷移的數據

宿主載入渲染器時沿用自身的腳本權限模型, 插件不擴大腳本可存取的系統能力.

******

### 插件介面

******

面向宿主的介面標識如下:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5316 (6.8.0)
```

宿主透過 `org.autojs.plugin.INFO` 發現插件並讀取 `requiresHostVersion` 等能力資訊; 渲染器工廠類名由 `org.autojs.plugin.compose.RENDERER_FACTORY` 元數據宣告, 宿主以插件 APK 路徑建立類載入器 (父載入器為宿主) 並在宿主進程內實例化.

******

### 路線圖

******

插件的里程碑, 設計決策與驗收標準統一記錄在路線圖中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### 版本歷史

******

#### v1.0.0

_2026/10/03_

- `提示` P5 開發預覽: 可呼叫的 compose / $compose 入口, 29 個節點工廠, 長期持有的句柄, 響應式 state/render/ref, batch/post/theme, ui 腳本掛載及 raw / 可調整懸浮窗已可在匹配的本地 AutoJs6 宿主構建中使用. 已包含可用性探測, 型別化錯誤與會話清理. 已附帶計數器, 表單, 1000 項列表, 懸浮 HUD 與主題五個示例, 同步至匹配宿主的 Compose UI 示例分類. 已完成穩健性檢查, 六台裝置/模擬器相容矩陣及打包應用驗證, 並記錄效能基線. 完整 API 文件及型別宣告將於 P6 交付. 目前僅為本地預覽, 尚無官方發行版
- `提示` 需要 AutoJs6 6.8.0 (5316) 或更高版本
- `新增` 插件倉庫骨架: 平台版本插件構建鏈, Jetpack Compose BOM 2026.09.00 依賴, Wake Activity 激活協議與 INFO 服務 (類別 compose-ui)
- `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 源檔案統一生成
- `新增` 預覽渲染支援佈局, 文字, 圖示, 圖片, 按鈕, 選擇控制項與滑桿; 傳入點陣圖仍由呼叫方持有並負責回收
- `新增` 全部 20 種 Modifier 操作保留聲明順序, 支援佈局作用域校驗, 捲動與無障礙標籤
- `新增` Material 3 主題支援種子色, 明暗模式, Android 12+ 系統動態色, 字體與字號縮放
- `新增` 界面更新以完整交易套用, 拒絕更新時保留上一次有效界面; 受控輸入透過佇列回調報告變化, 關閉後釋放回調
- `新增` 預覽輸入框保留選區與輸入法組合文字, 支援焦點和明確編輯, 拒絕覆蓋較新輸入的延遲編輯
- `新增` 預覽新增支援穩定項目 key 與索引捲動的懶加載列表, Scaffold 與頂部應用程式列插槽, 受控對話框, 進度指示器, 以及按佇列回傳操作或關閉結果的 Snackbar
- `新增` 腳本預覽提供可呼叫的 compose / $compose, 29 個節點工廠, 長期持有的句柄, 響應式 state/render/ref, 批次處理, 排程與主題控制
- `新增` ui 腳本可掛載 Compose 內容; 替換掛載或停止腳本時釋放舊會話及回呼
- `新增` 非 ui 腳本可建立 raw 或可調整 Compose 懸浮窗, 修改像素位置和尺寸, 觸摸及焦點設定, 並透過窗口控制項, floaty.closeAll 或腳本退出關閉
- `新增` 計數器, 表單驗證, 1000 項鍵控列表, 非 ui 懸浮 HUD 與主題五個可執行示例, 包含前置條件與索引, 同步至匹配宿主的 Compose UI 示例分類
- `修復` Compose UI 在渲染回呼中拒絕再次掛載頁面或懸浮窗並保留目前頁面, 支援插件更新後重新掛載頁面
- `優化` 可用性探測與 ComposeError 統一報告插件缺失, 停用, 未授權, 不相容, 權限不足及會話關閉; 生命週期清理涵蓋原生窗口附加前即被取消的情況; 插件更新, 解除安裝或停用時關閉使用中的會話並報告對應錯誤
- `依賴` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
- `依賴` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `依賴` 附加與 AutoJs6 6.8.0 (5316) 對齊的 compose-ui-api.aar V1 (MPL 2.0, 摘要鎖定), 共用依賴與宿主對齊
- `依賴` 附加由 BOM 2026.09.00 管理的 Compose UI Test (Apache 2.0, 僅用於測試)
- `依賴` 附加 JaCoCo 版本 0.8.14 (僅用於可選測試覆蓋率, 不隨發佈套件打包)

##### 更多版本歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

******

### 專案編譯與構建

******

克隆倉庫後可直接使用 Gradle Wrapper 構建, 所需的 Android Gradle 插件與 Kotlin 版本由平台版本插件按當前 IDE 環境自動選定.

構建調試包:

```powershell
.\gradlew.bat :app:assembleDebug
```

運行 JVM 單元測試並打包裝置契約測試:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

構建發佈包 (需要 `sign.properties` 與簽名密鑰):

```powershell
.\gradlew.bat :app:assembleRelease
```

校驗簽名並生成帶摘要後綴的發佈檔案:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

校驗多語言文件與源檔案一致:

```powershell
py .python\generate_markdown.py --check
```

構建需要 JDK 21 及以上. 修改 `.readme` 或 `.changelog` 目錄下的源檔案後, 運行 `py .python\generate_markdown.py` 重新生成所有文件.

******

### 文件資源佈局

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

README, 插件中心說明與更新日誌均由 `.readme` 與 `.changelog` 目錄下的 JSON 源檔案生成, 請勿直接編輯生成的 Markdown 檔案.

******

### 許可證

******

本專案基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE) 發佈. 第三方組件的許可資訊見 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案主頁: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- compose 模組文件: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
