<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 指令碼提供 Jetpack Compose 與 Material 3 介面轉譯能力的外掛程式</p>

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
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
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

Compose UI 是 AutoJs6 的介面轉譯外掛程式. 指令碼透過宿主提供的 `compose` / `$compose` 入口宣告介面, 外掛程式在宿主處理程序內以 Jetpack Compose 與 Material 3 完成轉譯. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 指令碼的懸浮視窗.

外掛程式不包含任何獨立介面, 也不在啟動器中顯示進入點. 宿主透過 INFO 服務探索外掛程式並讀取版本與相容資訊, 再依契約 (`org.autojs.plugin.compose.api`) 在宿主處理程序內載入轉譯器. 介面樹, 狀態與事件在指令碼端描述, 轉譯器只負責把修補套用到 Compose 組合並把使用者事件回傳給指令碼.

******

### 目前狀態

******

1.1.0 本機開發預覽: 需要相符的 AutoJs6 宿主建置, 並安裝和啟用本外掛. 本機配套提供 UI 頁面, 浮動視窗, 五個範例, API 參考與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在藍圖中. 目前未登錄官方索引, 尚無官方發行版. 圖示圖案仍為臨時預留, 等待維護者提供正式來源圖片.

******

### 功能特性

******

目前開發預覽的核心能力:

- 宣告式介面: `compose.state` + `compose.mount(render)` 依狀態變化自動重繪, 同時提供可長期持有的節點控制代碼 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 核心集: 29 個節點工廠涵蓋版面配置, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性清單, 對話方塊與進度; Snackbar 是工作階段命令, 不是 compose.Snackbar 工廠
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 範圍限定操作在宿主端驗證
- 兩種承載方式: `"ui";` 指令碼透過 `compose.mount` 或可呼叫的 `compose` / `$compose` 掛載 Activity 內容; `compose.floaty` 建立 raw 或可調整懸浮視窗, 同樣支援非 ui 指令碼
- 行程內繪製: 外掛在宿主行程套用介面更新, 事件排隊回到所屬指令碼執行緒; 工作執行緒透過 compose.post 請求更新
- 一個 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 無外掛自有原生程式碼; 隨附的 AndroidX graphics-path 輔助程式庫仍需符合 Android, 宿主與外掛相容條件
- 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆寫較新輸入的延遲編輯; 開關與滑桿仍由指令碼控制狀態
- 整合防護: 外掛程式缺少或不相容時可用性探測傳回不可用, 錯誤使用 `ComposeError`, 關閉工作階段或停止指令碼會釋放所屬視窗與回呼
- 計數器, 表單驗證, 1000 項鍵控清單, 非 ui 浮動 HUD 與主題五個可執行範例, 包含前置條件與索引, 同步至相符宿主的 Compose UI 範例分類
- TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊浮動視窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View

******

### 使用方式

******

1. 安裝包含 compose 指令碼入口的相符本機 AutoJs6 建置 (最低 6.8.0 / 5316)
2. 安裝本外掛程式 APK (無需開啟, 外掛程式沒有啟動器進入點)
3. 在 AutoJs6 的外掛程式中心確認 Compose UI 已被識別並處於啟用狀態
4. 在指令碼中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮視窗權限再使用 `compose.floaty`

******

### 快速開始

******

以下計數器與懸浮視窗 HUD 均可在相符的本機預覽宿主中執行. 執行 HUD 前需授予宿主在其他應用程式上層顯示的權限:

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
// 懸浮視窗 HUD (節點控制代碼層)
let worker = null;
let status = compose.Text({ text: '準備中...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, '關閉'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `進度 ${i}%`;
        });
    }
});
```

五個可執行指令碼由 assets/examples/index.json 列出, 同步至相符宿主的 Compose UI 範例分類. 每例開頭說明模式與權限. 節點, 修飾鏈, 主題, 工作階段與浮動視窗詳情請使用同批本機 API 文件及 TypeScript/編輯器宣告; 線上網站不一定已包含這些本機變更.

******

### 相容性

******

外掛程式的執行需求與限制:

- 最低 AutoJs6 版本: 6.8.0 (5316) 或更新; 低於該版本的宿主會在外掛程式中心提示不相容
- Android 版本: 7.0 (API 24) 或更新
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內建全部四種, 無需依架構選擇安裝套件)
- Compose 版本: 由外掛程式自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 執行階段
- 契約版本: 1; 宿主與外掛程式透過契約版本協商, 不相符時拒絕載入並給出明確錯誤
- 打包應用程式仍需另外安裝相容的 Compose UI 外掛, 啟用/授權記錄屬於該應用程式; 相容性檢查依據內建 AutoJs6 執行階段, 不是打包應用程式自身的 versionCode
- TSX 需要相符的 AutoJs6 6.8.0 / 5319 本機宿主與內建 Compose 宣告的配套 TypeScript Engine 建置; 單獨安裝轉譯器不會增加 TSX 支援
- 互操作需要相符的 AutoJs6 6.8.0 / 5320 宿主與支援 AndroidView 擴充的 Compose UI 建置; TSX 另需 TypeScript Engine 0.6.5. 基礎 V1 轉譯最低宿主仍為 5316
- View 工廠在轉譯前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父檢視被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫端管理

******

### 常見問題

******

- 為什麼安裝後找不到外掛程式圖示? 外掛程式沒有獨立介面, 也不會在啟動器顯示, 請在 AutoJs6 的外掛程式中心查看
- 為什麼找不到 `compose`? 全域物件由相符的本機宿主建置提供, 單獨安裝外掛程式 APK 不會加入該入口
- 是否需要解除安裝其它介面外掛程式? 不需要, Compose UI 與現有 `ui` 模組及其它外掛程式互不影響
- 外掛變更時會怎樣? 更新, 解除安裝或停用外掛會關閉作用中的工作階段並報告對應錯誤; 相容且啟用的外掛允許重新掛載
- 懸浮視窗需要什麼條件? 先授予宿主懸浮視窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示視窗, 請先返回桌面. 缺少權限會傳回 PERMISSION_REQUIRED, 不會自動彈出授權介面
- 能否使用 TSX 或任意 Compose 函式? 配合相符的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留指令碼引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬工作階段, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

******

### 權限與安全

******

外掛程式不申請任何 Android 執行階段權限, 也不存取網路, 儲存空間或感應器.

- 元件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 宿主可以存取
- 無背景行為: 外掛程式沒有常駐服務, 廣播接收器或排程工作, 不被宿主載入時不消耗資源
- 資料邊界: 外掛程式不讀寫指令碼資料與使用者檔案, 介面狀態只存在於宿主處理程序記憶體中
- 備份原則: 已停用應用程式備份與裝置移轉, 外掛程式本身不持有任何需要移轉的資料

宿主載入轉譯器時沿用自身的指令碼權限模型, 外掛程式不擴大指令碼可存取的系統能力.

******

### 外掛程式介面

******

面向宿主的介面識別碼如下:

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

宿主透過 `org.autojs.plugin.INFO` 探索外掛程式並讀取 `requiresHostVersion` 等能力資訊; 轉譯器工廠類別名稱由 `org.autojs.plugin.compose.RENDERER_FACTORY` 中繼資料宣告, 宿主以外掛程式 APK 路徑建立類別載入器 (父載入器為宿主) 並在宿主處理程序內實例化.

******

### 藍圖

******

外掛程式的里程碑, 設計決策與驗收標準統一記錄在藍圖中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### 版本歷史

******

#### v1.1.0

_2026/10/07_

- `提示` 1.1.0 本機開發預覽: 需要相符的 AutoJs6 宿主建置, 並安裝和啟用本外掛. 本機配套提供 UI 頁面, 浮動視窗, 五個範例, API 參考與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在藍圖中. 目前未登錄官方索引, 尚無官方發行版. 圖示圖案仍為臨時預留, 等待維護者提供正式來源圖片
- `提示` TSX 需要相符的 AutoJs6 6.8.0 / 5319 本機宿主與內建 Compose 宣告的配套 TypeScript Engine 建置; 單獨安裝轉譯器不會增加 TSX 支援
- `提示` 互操作需要相符的 AutoJs6 6.8.0 / 5320 宿主與支援 AndroidView 擴充的 Compose UI 建置; TSX 另需 TypeScript Engine 0.6.5. 基礎 V1 轉譯最低宿主仍為 5316
- `新增` TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- `新增` XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊浮動視窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- `最佳化` Android 系統應用程式資訊圖示與圖示工作台共用圖稿及明暗底色, 保留外掛程式中心透明圖稿與現有啟動器選項
- `最佳化` View 工廠在轉譯前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父檢視被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫端管理
- `相依` 升級 compose-ui-api.aar 契約檔案, 保留凍結 V1 並附加可選 AndroidView 互操作擴充

#### v1.0.0

_2026/10/03_

- `提示` 1.0.0 本機開發預覽: 需要相符的 AutoJs6 宿主建置, 並安裝和啟用本外掛. 本機配套提供 UI 頁面, 浮動視窗, 五個範例, API 參考與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在藍圖中. 目前未登錄官方索引, 尚無官方發行版. 圖示圖案仍為臨時預留, 等待維護者提供正式來源圖片
- `提示` 需要 AutoJs6 6.8.0 (5316) 或更新版本
- `提示` 打包應用程式仍需另外安裝相容的 Compose UI 外掛, 啟用/授權記錄屬於該應用程式; 相容性檢查依據內建 AutoJs6 執行階段, 不是打包應用程式自身的 versionCode
- `提示` 保留亮色/深色兩類 mipmap 入口; 目前圖案為預留, 待維護者提供正式黑白來源圖片後替換
- `新增` 可呼叫的 compose / $compose, 長期節點控制代碼, 響應式 state/render/ref, 批次變更, 排隊調度與主題控制
- `新增` Material 3 核心集: 29 個節點工廠涵蓋版面配置, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性清單, 對話方塊與進度; Snackbar 是工作階段命令, 不是 compose.Snackbar 工廠
- `新增` UI 指令碼可掛載 Activity 內容; compose.floaty 同時支援非 UI 指令碼的 raw / 可調整視窗, 像素位置與尺寸, 觸控/焦點控制及所屬資源清理
- `新增` 全部 20 種 Modifier 操作保留宣告順序, 支援版面配置作用域驗證, 捲動與無障礙標籤
- `新增` Material 3 主題支援種子色, 明暗模式, Android 12+ 系統動態色, 字型與字級縮放
- `新增` 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆寫較新輸入的延遲編輯; 開關與滑桿仍由指令碼控制狀態
- `新增` 核心圖示與 ImageWrapper/Bitmap, 本機檔案和宿主 drawable 圖片; 繪製器不自動回收呼叫端持有的圖片資源
- `新增` 計數器, 表單驗證, 1000 項鍵控清單, 非 ui 浮動 HUD 與主題五個可執行範例, 包含前置條件與索引, 同步至相符宿主的 Compose UI 範例分類
- `新增` 配套 API 參考與 TypeScript 宣告, 以及 10 語言 README, 外掛中心說明和更新日誌
- `新增` 外掛中心探索與宿主版本, 契約, 授權檢查, 無獨立介面和啟動器入口
- `修正` Compose UI 在轉譯回呼中拒絕再次掛載頁面或浮動視窗並保留目前頁面, 支援外掛程式更新後重新掛載頁面
- `最佳化` 可用性探測與 ComposeError 統一報告外掛程式缺少, 停用, 未授權, 不相容, 權限不足及工作階段關閉; 生命週期清理涵蓋原生視窗附加前即被取消的情況; 外掛程式更新, 解除安裝或停用時關閉作用中的工作階段並回報對應錯誤
- `相依` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
- `相依` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `相依` 附加與 AutoJs6 6.8.0 (5316) 對齊的 compose-ui-api.aar V1 (MPL 2.0, 雜湊鎖定), 共用相依性與宿主對齊
- `相依` 附加由 BOM 2026.09.00 管理的 Compose UI Test (Apache 2.0, 僅用於測試)
- `相依` 附加 JaCoCo 版本 0.8.14 (僅用於選用測試涵蓋率, 不隨發行套件打包)

##### 更多版本歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

******

### 專案編譯與建置

******

複製存放庫後可直接使用 Gradle Wrapper 建置, 所需的 Android Gradle 外掛程式與 Kotlin 版本由平台版本外掛程式依目前 IDE 環境自動選定.

建置偵錯套件:

```powershell
.\gradlew.bat :app:assembleDebug
```

執行 JVM 單元測試並封裝裝置契約測試:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

建置發行套件 (需要 `sign.properties` 與簽章金鑰):

```powershell
.\gradlew.bat :app:assembleRelease
```

驗證簽章並產生帶摘要後綴的發行檔案:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

驗證多語言文件與來源檔案一致:

```powershell
py .python\generate_markdown.py --check
```

建置需要 JDK 21 及以上. 修改 `.readme` 或 `.changelog` 目錄下的來源檔案後, 執行 `py .python\generate_markdown.py` 重新產生所有文件.

******

### 文件資源配置

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

README, 外掛程式中心說明與更新日誌均由 `.readme` 與 `.changelog` 目錄下的 JSON 來源檔案產生, 請勿直接編輯產生的 Markdown 檔案.

******

### 授權條款

******

本專案依據 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE) 發行. 第三方元件的授權資訊見 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案首頁: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- compose 模組文件: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
