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

插件內置組件展示, 可從啟動器打開瀏覽全部組件的預覽與示例腳本; 腳本界面的渲染仍在宿主進程中進行. 宿主通過 INFO 服務發現插件並讀取版本與兼容資訊, 再按契約 (`org.autojs.plugin.compose.api`) 在自身進程內實例化渲染器工廠.

******

### 當前狀態

******

1.1.0 本地開發預覽: 需要匹配的 AutoJs6 宿主構建, 並安裝和啟用本插件. 本地配套提供 UI 頁面, 懸浮窗, 五個示例, API 參考與 TypeScript 宣告. 已驗證的兼容性及效能範圍記錄在路線圖中. 目前未登記官方索引, 尚無官方發行版. 圖示圖案仍為臨時佔位, 等待維護者提供正式源圖.

******

### 功能特性

******

目前開發預覽的核心能力:

- 宣告式界面: `compose.state` + `compose.mount(render)` 按狀態變化自動重繪, 同時提供可長期持有的節點句柄 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 核心集: 29 個節點工廠涵蓋佈局, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性列表, 對話框與進度; Snackbar 是會話命令, 不是 compose.Snackbar 工廠
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 作用域操作在宿主側校驗
- 兩種承載方式: `"ui";` 腳本透過 `compose.mount` 或可呼叫的 `compose` / `$compose` 掛載 Activity 內容; `compose.floaty` 建立 raw 或可調整懸浮窗, 同樣支援非 ui 腳本
- 進程內渲染: 插件在宿主進程套用介面更新, 事件排隊回到所屬腳本執行緒; 工作執行緒透過 compose.post 請求更新
- 一個 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 無插件自有原生程式碼; 隨包的 AndroidX graphics-path 輔助庫仍需符合 Android, 宿主與插件兼容條件
- 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆蓋較新輸入的延遲編輯; 開關與滑桿仍由腳本控制狀態
- 整合守衛: 插件缺失或不相容時可用性探測回傳不可用, 錯誤使用 `ComposeError`, 關閉會話或停止腳本會釋放所屬窗口與回呼
- 計數器, 表單驗證, 1000 項鍵控列表, 非 ui 懸浮 HUD 與主題五個可執行示例, 包含前置條件與索引, 同步至匹配宿主的 Compose UI 示例分類
- TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊懸浮窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- compose.dialog 傳回可更新和關閉的工作階段, 支援一般對話框與模態底部面板, 可在 UI 或一般腳本中使用
- Material 3 擴充元件: 導覽列與抽屜, 分頁標籤, 底部面板與選單, 日期與時間選擇器, 分頁與網格, 資訊方塊, 徽章, 分段按鈕, 懸浮按鈕, 搜尋列, 提示與下拉重新整理
- 組件展示: 啟動器入口展示全部 55 個組件的 Material 3 預覽與可運行示例腳本, 可複製或發送到 AutoJs6; 設定頁的語言, 夜間模式與主題色預設跟隨 AutoJs6, 並提供四種啟動器圖示

******

### 使用方式

******

1. 安裝包含 compose 腳本入口的匹配本地 AutoJs6 構建 (最低 6.8.0 / 5322)
2. 安裝此插件 APK; 從啟動器打開 Compose UI 可查看組件展示與設定
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

五個可執行腳本由 assets/examples/index.json 列出, 同步至匹配宿主的 Compose UI 示例分類. 每例開首說明模式與權限. 節點, 修飾鏈, 主題, 會話與懸浮窗詳情請使用同批本地 API 文件及 TypeScript/編輯器宣告; 線上站點不一定已包含這些本地變更.

******

### 兼容性

******

插件的運行要求與限制:

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更高; 低於該版本的宿主會在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內置全部四種, 無需按架構選擇安裝包)
- Compose 版本: 由插件自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 運行時
- 契約版本: 2; 宿主與插件透過契約版本協商, 不匹配時拒絕載入並給出明確錯誤
- 打包應用仍需另外安裝兼容的 Compose UI 插件, 啟用/授權記錄屬於該應用; 兼容性檢查依據內置 AutoJs6 執行時, 不是打包應用自身的 versionCode
- View 工廠在渲染前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父視圖被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫方管理
- cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與腳本結束仍會清理彈窗, 保留現有頁面和其他工作階段
- 此版本使用 Compose UI 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 渲染器的原有元件, 擴充元件需要 V2 渲染器
- compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此插件無需更新, TSX 配套需要 TypeScript Engine 0.6.8

******

### 常見問題

******

- 展示中的示例如何運行? 點擊 "在 AutoJs6 中運行" 會把腳本交給已安裝的 AutoJs6 執行; 展示本身只顯示預覽與代碼, 不執行腳本
- 為甚麼找不到 `compose`? 全域物件由匹配的本地宿主構建提供, 單獨安裝插件 APK 不會加入該入口
- 是否需要卸載其它界面插件? 不需要, Compose UI 與現有 `ui` 模組及其它插件互不影響
- 插件變更時會怎樣? 更新, 解除安裝或停用插件會關閉活動會話並報告對應錯誤; 兼容且啟用的插件允許重新掛載
- 懸浮窗需要甚麼條件? 先授予宿主懸浮窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示窗口, 請先返回桌面. 缺少權限會回傳 PERMISSION_REQUIRED, 不會自動彈出授權界面
- 能否使用 TSX 或任意 Compose 函式? 配合匹配的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留腳本引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬會話, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

******

### 權限與安全

******

插件不申請任何 Android 運行時權限, 也不存取網絡, 儲存空間或感應器.

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以存取
- 後台行為: 插件沒有常駐服務與定時任務, 僅在自身被更新時接收一次系統廣播以校正啟動器圖示組件; 未被宿主載入且未打開展示時不消耗資源
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
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
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

#### v1.1.0

_2026/10/08_

- `提示` 1.1.0 本地開發預覽: 需要匹配的 AutoJs6 宿主構建, 並安裝和啟用本插件. 本地配套提供 UI 頁面, 懸浮窗, 五個示例, API 參考與 TypeScript 宣告. 已驗證的兼容性及效能範圍記錄在路線圖中. 目前未登記官方索引, 尚無官方發行版. 圖示圖案仍為臨時佔位, 等待維護者提供正式源圖
- `提示` 此版本使用 Compose UI 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 渲染器的原有元件, 擴充元件需要 V2 渲染器
- `提示` compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此插件無需更新, TSX 配套需要 TypeScript Engine 0.6.8
- `提示` 展示與設定頁在插件自身進程運行, 不改變宿主內的渲染方式與最低宿主版本; 運行示例腳本需要已安裝並啟用此插件的 AutoJs6
- `新增` TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- `新增` XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊懸浮窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- `新增` compose.dialog 傳回可更新和關閉的工作階段, 支援一般對話框與模態底部面板, 可在 UI 或一般腳本中使用
- `新增` Material 3 擴充元件: 導覽列與抽屜, 分頁標籤, 底部面板與選單, 日期與時間選擇器, 分頁與網格, 資訊方塊, 徽章, 分段按鈕, 懸浮按鈕, 搜尋列, 提示與下拉重新整理
- `新增` 組件展示: 從啟動器打開, 瀏覽全部 55 個組件的 Material 3 預覽與示例腳本, 可複製到剪貼板或發送到已安裝的 AutoJs6 運行
- `新增` 設定頁: 語言, 夜間模式與主題色預設跟隨 AutoJs6, 可單獨設定; 啟動器圖示提供自適應 (亮色 / 暗色 / 自動) 與透明背景四種選擇
- `優化` Android 系統應用程式資訊圖示與圖示工作台共用圖稿及明暗底色, 保留外掛程式中心透明圖稿與現有啟動器選項
- `優化` View 工廠在渲染前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父視圖被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫方管理
- `優化` cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與腳本結束仍會清理彈窗, 保留現有頁面和其他工作階段
- `優化` 插件中心圖示與系統應用程式資訊圖示使用亮色 #FAFAFA / 暗色 #212121 底色, 與其他獨立插件一致
- `依賴` 升級 compose-ui-api.aar 契約檔案, 保留凍結 V1 並附加可選 AndroidView 互操作擴充
- `依賴` 附加可選對話框功能至 compose-ui-api.aar, 保留現有 V1 與 AndroidView 契約
- `依賴` 附加 compose-ui-api.aar V2 元件目錄, 保留現有節點模型和 V1 元件語義
- `依賴` 附加 material-color-utilities 版本 4.1.1 (MIT), 用於設定頁與其他獨立插件一致的主題色推導
- `依賴` 附加 AndroidX activity, core, lifecycle, savedstate, emoji2, window 與 kotlinx-coroutines 等的宿主鎖定版本副本, 供插件自身進程的展示使用; 宿主內仍優先使用宿主副本

#### v1.0.0

_2026/10/03_

- `提示` 1.0.0 本地開發預覽: 需要匹配的 AutoJs6 宿主構建, 並安裝和啟用本插件. 本地配套提供 UI 頁面, 懸浮窗, 五個示例, API 參考與 TypeScript 宣告. 已驗證的兼容性及效能範圍記錄在路線圖中. 目前未登記官方索引, 尚無官方發行版. 圖示圖案仍為臨時佔位, 等待維護者提供正式源圖
- `提示` 需要 AutoJs6 6.8.0 (5316) 或更高版本
- `提示` 打包應用仍需另外安裝兼容的 Compose UI 插件, 啟用/授權記錄屬於該應用; 兼容性檢查依據內置 AutoJs6 執行時, 不是打包應用自身的 versionCode
- `提示` 保留亮色/深色兩類 mipmap 入口; 目前圖案為佔位, 待維護者提供正式黑白源圖後替換
- `新增` 可呼叫的 compose / $compose, 長期節點句柄, 響應式 state/render/ref, 批次變更, 排隊調度與主題控制
- `新增` Material 3 核心集: 29 個節點工廠涵蓋佈局, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性列表, 對話框與進度; Snackbar 是會話命令, 不是 compose.Snackbar 工廠
- `新增` UI 腳本可掛載 Activity 內容; compose.floaty 同時支援非 UI 腳本的 raw / 可調整視窗, 像素位置與尺寸, 觸摸/焦點控制及所屬資源清理
- `新增` 全部 20 種 Modifier 操作保留聲明順序, 支援佈局作用域校驗, 捲動與無障礙標籤
- `新增` Material 3 主題支援種子色, 明暗模式, Android 12+ 系統動態色, 字體與字號縮放
- `新增` 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆蓋較新輸入的延遲編輯; 開關與滑桿仍由腳本控制狀態
- `新增` 核心圖示與 ImageWrapper/Bitmap, 本地檔案和宿主 drawable 圖片; 渲染器不自動回收呼叫方持有的圖片資源
- `新增` 計數器, 表單驗證, 1000 項鍵控列表, 非 ui 懸浮 HUD 與主題五個可執行示例, 包含前置條件與索引, 同步至匹配宿主的 Compose UI 示例分類
- `新增` 配套 API 參考與 TypeScript 宣告, 以及 10 語言 README, 插件中心說明和更新日誌
- `新增` 插件中心發現與宿主版本, 契約, 授權檢查, 無獨立介面和啟動器入口
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
