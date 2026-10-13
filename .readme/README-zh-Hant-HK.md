<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 腳本提供 Jetpack Compose 與 Material 3 界面渲染能力的插件</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 語言

******

本文件支援以下語言:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- 繁體中文 (香港) [zh-Hant-HK] # 當前
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### 簡介

******

3-Block Compose 是 AutoJs6 的界面渲染插件. 腳本透過宿主提供的 `compose` / `$compose` 入口宣告界面, 插件在宿主進程內以 Jetpack Compose 與 Material 3 完成渲染. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 腳本的懸浮窗.

插件內置可視化設計器與組件展示: 從啟動器打開即進入設計器首頁, 可新建或匯入腳本工程並在真實渲染器上編輯界面, 組件展示則呈現全部組件的預覽與示例腳本; 腳本界面的渲染仍在宿主進程中進行. 宿主通過 INFO 服務發現插件並讀取版本與兼容資訊, 再按契約 (`org.autojs.plugin.compose.api`) 在自身進程內實例化渲染器工廠.

******

### 當前狀態

******

1.4.0 是首個公開發行版 (GitHub Release, 已登記官方插件索引). 需要 AutoJs6 6.8.0 (5322) 或更高版本中包含 3-Block Compose 新身份的構建 (宿主提交 f12665ffde 之後); 發佈時 AutoJs6 6.8.0 尚未正式發佈, 需要使用包含該提交的 AutoJs6 構建. AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中打開" 需要宿主提交 db7e6a1734 之後的構建, 設計器的 TSX 工程需要 TypeScript Engine 0.7.0. 本插件提供 UI 頁面, 懸浮窗, 可視化設計器, 五個示例, API 參考, 設計器指南頁與 TypeScript 聲明. 已驗證的兼容性及性能範圍記錄在路線圖中. 圖標已採用維護者提供的正式圖稿.

******

### 功能特性

******

目前開發預覽的核心能力:

- 可視化設計器: 腳本檔案即工程, 真實渲染器所見即所得地預覽界面樹, 大綱 / 組件面板 / 屬性抽屜覆蓋全部 55 個組件, 支援撤銷重做與草稿恢復; 匯出的腳本可複製或直接發送到 AutoJs6 運行; 畫布拖放按指標位置插入, 開啟的彈出層畫在畫布內, Image 可顯示 @drawable 與工程圖片目錄中的圖片, 狀態面板可暫時改變 state 的預覽值, 互動預覽在受限的沙盒中運行 JavaScript 工程的腳本 (Android 8.0 或更高版本), 點擊與輸入按腳本邏輯更新畫布, 控制台顯示腳本的輸出與錯誤; 可新建 JavaScript 或 TSX 工程, TSX 工程經 TypeScript Engine 0.7.0 或更高版本解析, 生成規範的 TSX
- 與 AutoJs6 銜接: 工程可發送運行, 在 AutoJs6 編輯器中打開 (雙向同步), 匯入 AutoJs6 腳本目錄, 複製或匯出; 設計器也能從 AutoJs6 編輯器或檔案管理器直接打開 .js / .tsx 檔案並儲存回原檔案; 匯入的工程可在編輯頁選單中手動寫回來源或從來源重新匯入
- 宣告式界面: `compose.state` + `compose.mount(render)` 按狀態變化自動重繪, 同時提供可長期持有的節點句柄 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 核心集: 29 個節點工廠涵蓋佈局, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性列表, 對話框與進度; Snackbar 是會話命令, 不是 compose.Snackbar 工廠
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 作用域操作在宿主側校驗
- 兩種承載方式: `"ui";` 腳本透過 `compose.mount` 或可呼叫的 `compose` / `$compose` 掛載 Activity 內容; `compose.floaty` 建立 raw 或可調整懸浮窗, 同樣支援非 ui 腳本
- 進程內渲染: 插件在宿主進程套用介面更新, 事件排隊回到所屬腳本執行緒; 工作執行緒透過 compose.post 請求更新
- 一個 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 無插件自有原生程式碼; 隨包的 AndroidX graphics-path 輔助庫仍需符合 Android, 宿主與插件兼容條件
- 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆蓋較新輸入的延遲編輯; 開關與滑桿仍由腳本控制狀態
- 整合守衛: 插件缺失或不相容時可用性探測回傳不可用, 錯誤使用 `ComposeError`, 關閉會話或停止腳本會釋放所屬窗口與回呼
- 計數器, 表單驗證, 1000 項鍵控列表, 非 ui 懸浮 HUD 與主題五個可執行示例, 包含前置條件與索引, 同步至匹配宿主的 3-Block Compose 示例分類
- TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊懸浮窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- compose.dialog 傳回可更新和關閉的工作階段, 支援一般對話框與模態底部面板, 可在 UI 或一般腳本中使用
- Material 3 擴充元件: 導覽列與抽屜, 分頁標籤, 底部面板與選單, 日期與時間選擇器, 分頁與網格, 資訊方塊, 徽章, 分段按鈕, 懸浮按鈕, 搜尋列, 提示與下拉重新整理
- 組件展示: 啟動器入口展示全部 55 個組件的 Material 3 預覽與可運行示例腳本, 可複製或發送到 AutoJs6; 設定頁的語言, 夜間模式與主題色預設跟隨 AutoJs6, 並提供四種啟動器圖示

******

### 使用方式

******

1. 安裝包含 compose 腳本入口的匹配本地 AutoJs6 構建 (最低 6.8.0 / 5322)
2. 安裝此插件 APK; 從啟動器打開 3-Block Compose 進入設計器首頁, 可新建腳本工程, 或從頂部欄打開組件展示與設定
3. 在 AutoJs6 的插件中心確認 3-Block Compose 已被識別並處於啟用狀態
4. 在腳本中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮窗權限再使用 `compose.floaty`
5. 編輯頁選單提供發送運行, 在 AutoJs6 編輯器中打開, 匯入 AutoJs6 腳本目錄, 複製與匯出; 在 AutoJs6 編輯器選單 (需 AutoJs6 包含對應更新) 或檔案管理器中選擇 3-Block Compose 也可以直接編輯腳本檔案

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

五個可執行腳本由 assets/examples/index.json 列出, 同步至匹配宿主的 3-Block Compose 示例分類. 每例開首說明模式與權限. 節點, 修飾鏈, 主題, 會話與懸浮窗詳情請使用同批本地 API 文件及 TypeScript/編輯器宣告; 線上站點不一定已包含這些本地變更.

******

### 兼容性

******

插件的運行要求與限制:

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更高; 低於該版本的宿主會在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內置全部四種, 無需按架構選擇安裝包)
- Compose 版本: 由插件自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 運行時
- 契約版本: 2; 宿主與插件透過契約版本協商, 不匹配時拒絕載入並給出明確錯誤
- 打包應用仍需另外安裝兼容的 3-Block Compose 插件, 啟用/授權記錄屬於該應用; 兼容性檢查依據內置 AutoJs6 執行時, 不是打包應用自身的 versionCode
- View 工廠在渲染前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父視圖被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫方管理
- cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與腳本結束仍會清理彈窗, 保留現有頁面和其他工作階段
- 此版本使用 3-Block Compose 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 渲染器的原有元件, 擴充元件需要 V2 渲染器
- compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此插件無需更新, TSX 配套需要 TypeScript Engine 0.6.8
- 契約類 (org.autojs.plugin.compose.api) 自 1.2.0 起隨插件打包供獨立進程使用; 在 AutoJs6 內仍由宿主提供同名類, 插件副本不生效
- 設計器在插件自身進程運行, 不依賴 AutoJs6; 發送運行, 在 AutoJs6 編輯器中打開與匯入 AutoJs6 腳本目錄需要已安裝 AutoJs6, 編輯器選單的 "在 3-Block Compose 設計器中打開" 需要包含該入口的 AutoJs6 構建
- 設計器的 TSX 工程需要 TypeScript Engine 0.7.0 或更高版本 (提供語法樹解析服務), 發送運行時由 AutoJs6 經 TypeScript Engine 編譯; 未安裝或版本過低時 TSX 工程以唯讀源碼方式打開, 可查看與複製
- 設計器在互動預覽中運行腳本需要 Android 8.0 (API 26) 或更高版本 (沙盒中的 JavaScript Date 基於 java.time); 更低版本的裝置上, JavaScript 工程按 TSX 工程的方式預覽

******

### 常見問題

******

- 展示中的示例如何運行? 點擊 "在 AutoJs6 中運行" 會把腳本交給已安裝的 AutoJs6 執行; 展示本身只顯示預覽與代碼, 不執行腳本
- 設計器能打開任意腳本嗎? 它只解釋頂層的 `compose.state` 聲明與唯一的 `compose.mount` / `floaty` / `dialog` 調用, 其它語句按源碼原樣保留; 根節點不是 compose 工廠, 多處掛載, 渲染函數無 return 等無法映射的結構會拒絕打開並指出行列; TSX 工程中 `compose.X` / `compose:X` 標籤是可編輯的節點, 其它標籤與 `{}` 表達式按原文保留, XML 標籤與展開屬性會拒絕
- 設計器會執行腳本嗎? 只在互動預覽中. 編輯時它只解析腳本 (JavaScript 用 Rhino 解析器, TSX 用 TypeScript Engine 的解析服務) 並用插件自帶的渲染器預覽, 事件處理函式不會執行, state 顯示初值或狀態面板中設定的預覽值. 開啟互動預覽後, JavaScript 工程的腳本在插件內受限的沙盒中運行 (需要 Android 8.0 或更高版本): 只提供 compose, console, toast 與計時器, 不能存取 Java / Android 類別, 檔案, 網絡或其他 AutoJs6 模組, 單次運行超過約 3 秒會被停止, 運行結果只顯示在畫布上, 不會修改工程. TSX 工程 (以及 Android 8.0 以下裝置上的工程) 不運行腳本, 綁定了 state 的控制項只改變預覽值. 真正運行腳本要交給 AutoJs6
- 為甚麼找不到 `compose`? 全域物件由匹配的本地宿主構建提供, 單獨安裝插件 APK 不會加入該入口
- 是否需要卸載其它界面插件? 不需要, 3-Block Compose 與現有 `ui` 模組及其它插件互不影響
- 插件變更時會怎樣? 更新, 解除安裝或停用插件會關閉活動會話並報告對應錯誤; 兼容且啟用的插件允許重新掛載
- 懸浮窗需要甚麼條件? 先授予宿主懸浮窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示窗口, 請先返回桌面. 缺少權限會回傳 PERMISSION_REQUIRED, 不會自動彈出授權界面
- 能否使用 TSX 或任意 Compose 函式? 配合匹配的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留腳本引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬會話, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

******

### 權限與安全

******

插件不申請任何 Android 運行時權限, 也不存取網絡, 儲存空間或感應器.

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以訪問; 設計器編輯頁是無權限的導出組件, 只接收用戶交出的腳本文檔 Uri, 讀寫都限於該 Uri
- 後台行為: 插件沒有常駐服務與定時任務, 僅在自身被更新時接收一次系統廣播以校正啟動器圖標組件; 不被宿主加載且未打開設計器或畫廊時不消耗資源
- 數據邊界: 渲染器不讀寫腳本數據與用戶檔案, 界面狀態只存在於宿主進程內存中; 設計器的工程與草稿存於插件私有目錄, 只經系統檔案選擇器匯入匯出, 或按用戶交出的 Uri 讀寫單個腳本檔案; 工程目錄僅通過插件自己的內容提供器在交給 AutoJs6 時按次授權; 匯入時會保留所選檔案的存取權限 (提供者允許時), 但只在用戶選擇 "寫回來源" 或 "從來源重新匯入" 時讀寫該檔案
- 腳本沙盒: 設計器的互動預覽在插件進程內以 Rhino 解譯器運行 JavaScript 工程的腳本, 只提供 compose, console, toast 與計時器; 不能存取 Java / Android 類別, 檔案, 網絡或其他 AutoJs6 模組, 每次運行都有指令數與時長上限, 運行結果只用於畫布, 不會寫入工程
- 備份策略: 已停用應用備份與裝置遷移, 插件本身不持有任何需要遷移的數據

宿主載入渲染器時沿用自身的腳本權限模型, 插件不擴大腳本可存取的系統能力.

******

### 插件介面

******

面向宿主的介面標識如下:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

宿主透過 `org.autojs.plugin.INFO` 發現插件並讀取 `requiresHostVersion` 等能力資訊; 渲染器工廠類名由 `org.autojs.plugin.compose.RENDERER_FACTORY` 元數據宣告, 宿主以插件 APK 路徑建立類載入器 (父載入器為宿主) 並在宿主進程內實例化.

******

### 路線圖

******

插件的里程碑, 設計決策與驗收標準統一記錄在路線圖中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### 版本歷史

******

#### v1.5.0

_2026/10/14_

- `提示` 設計器的互動預覽現在會運行 JavaScript 工程的腳本 (需要 Android 8.0 或更高版本). 腳本在插件內受限的沙盒中運行: 只能使用 compose, console, toast 與計時器, 不能存取 Java / Android 或其他 AutoJs6 模組, 運行結果只顯示在畫布上, 不會修改工程. TSX 工程與 Android 8.0 以下的裝置沿用之前的互動預覽
- `新增` 互動預覽運行腳本: 開啟 "互動預覽" 後, JavaScript 工程的渲染函式與事件處理函式真實運行, 點擊按鈕, 輸入文字, 撥動開關等會按腳本的邏輯更新畫布; 狀態面板中的預覽值作為 state 的初值, 修改工程或預覽值後腳本自動重新運行
- `新增` 新增 "控制台" 頁: 顯示預覽中腳本的 console / log 輸出, toast 與錯誤; 腳本出錯時畫布底部顯示錯誤, 點擊可開啟控制台; 單次運行超過約 3 秒 (例如無限循環) 時預覽會停止腳本並說明原因, 設計器本身不會卡住
- `新增` 腳本運行時, "互動預覽" 開關上顯示運行圖標
- `修復` 編輯模式下, 畫布中的輸入框有時會自動取得焦點並彈出鍵盤 (多見於 Android 7.x), 遮住下方的面板; 現在編輯模式下畫布中的組件不會取得焦點, 退出互動預覽時也會收起鍵盤

#### v1.4.0

_2026/10/13_

- `提示` 設計器的 TSX 工程需要 TypeScript Engine 0.7.0 或更高版本; 未安裝或版本過低時, TSX 工程只能以唯讀源碼方式查看與複製
- `新增` 設計器支援 TSX 工程: 新建工程時可選 JavaScript 或 TSX, 也可以匯入 .tsx 檔案, 或從 AutoJs6 編輯器與檔案管理器直接打開 .tsx 檔案; TSX 源碼由 TypeScript Engine 解析後在畫布中編輯, 儲存為統一格式的 TSX (4 空格縮排, 單引號屬性, 每個子元素一行); 設計器無法編輯的寫法 (例如別名標籤, 條件或循環生成的子元素) 按原文保留, XML 標籤與展開屬性等無法表示的寫法會拒絕打開並指出行列
- `新增` 設計器中 TSX 工程的 "運行" 會把工程檔案交給 AutoJs6, 由 TypeScript Engine 編譯後運行; 有未儲存的修改時先儲存
- `新增` TypeScript Engine 未安裝, 版本過低或解析服務暫時不可用時, 設計器以唯讀方式顯示 TSX 工程的源碼並說明原因, 可以複製源碼或重試; 安裝或更新 TypeScript Engine 後回到設計器會自動重試
- `修復` 設計器儲存無法寫回的外部腳本時, 檔名含空格, 括號等工程名稱不允許的字元會使插件閃退, 而不是另存為工程的問題; 現在這些字元換成 "-" 後另存, 匯入腳本也按同一規則命名
- `優化` TSX 工程中編輯事件處理函數時由 TypeScript Engine 檢查語法, 可以使用類型標註等 TypeScript 寫法
- `依賴` 附加 typescript-parser-api.aar 契約製品 (TypeScript 語法樹解析契約 v1), 供設計器讀取 TSX 工程

#### v1.3.0

_2026/10/13_

- `新增` 設計器中經 "匯入腳本" 得到的工程可在編輯頁選單中 "寫回來源" 或 "從來源重新匯入": 匯入時保留對來源檔案的存取權限 (檔案提供者允許時), 來源或工程在上次同步後被修改過時先詢問, 來源檔案無法開啟時可重新選擇以恢復關聯; 儲存工程不會自動寫回來源
- `新增` 設計器畫布可預覽 Image 的圖片: '@drawable/名稱' 取自已安裝的 AutoJs6, 相對路徑在編輯頁選單 "圖片資料夾" 選定的資料夾中尋找 (僅保留唯讀存取權限); 網址與絕對路徑仍顯示佔位, 原因列在問題清單中
- `新增` 設計器編輯頁新增 "狀態" 面板: 列出腳本頂層的 compose.state 與其初值, 可以暫時修改預覽值, 只影響畫布, 不寫入腳本也不產生復原步驟; 開啟畫布右上角的 "互動預覽" 後可以直接操作畫布中的元件, 綁定了狀態的開關, 滑桿, 輸入框, 選擇項, 分頁等控制項會修改對應的預覽值 (腳本中的事件處理函式不會執行), 關閉後恢復點選與拖曳
- `修復` 界面中某個容器前部的子節點被插入, 刪除或移到別處時, 其後的子節點會被整體重建, 遺失它們自己的狀態 (例如 LazyColumn 的捲動位置) 的問題; 現在這些子節點保持原有狀態, 也不再重複建構
- `修復` 設計器屬性抽屜修改輸入框 (TextField / OutlinedTextField) 的 text 後, 畫布中的輸入框仍顯示舊文字的問題
- `優化` 設計器畫布中長按拖曳節點時, 會按目標容器的排列方向 (縱向, 橫向, 網格) 顯示插入線, 放開後節點放到指標所在的位置, 不再只能追加到容器末尾
- `優化` 設計器畫布中的 AndroidView 改為按其尺寸修飾 (如 h, fillMaxWidth) 顯示淺色佔位區域, 並在其中顯示 view 表達式的源碼
- `優化` 設計器屬性抽屜中的修飾鏈可以按住每項左側的把手拖曳排序, 放開前顯示插入線; 上移 / 下移按鈕仍然保留
- `優化` 設計器畫布中開啟狀態 (open 為 true) 的彈出層元件改為按接近真實的樣式畫在畫布的邏輯螢幕內: AlertDialog 與 ModalBottomSheet 連同遮罩蓋在頁面上方, DropdownMenu 與 Tooltip 顯示在錨點下方或上方, 點選彈出層中的節點即可編輯; 不再以卡片形式插在頁面中, 也不會彈出真實視窗
- `優化` 設計器畫布在每次編輯後只重新組合發生變化的節點, 並減少記錄節點位置的開銷, 較大的文件 (約 250 個節點) 復原 / 重做後的畫面重新整理明顯更快

##### 更多版本歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-HK.md)

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

本專案基於 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE) 發佈. 第三方組件的許可資訊見 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案主頁: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- compose 模組文件: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
