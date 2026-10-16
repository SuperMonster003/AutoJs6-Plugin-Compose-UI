<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>為 AutoJs6 指令碼提供 Jetpack Compose 與 Material 3 介面轉譯能力的外掛程式</p>

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
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- 繁體中文 (台灣) [zh-Hant-TW] # 目前
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

3-Block Compose 是 AutoJs6 的介面轉譯外掛程式. 指令碼透過宿主提供的 `compose` / `$compose` 入口宣告介面, 外掛程式在宿主處理程序內以 Jetpack Compose 與 Material 3 完成轉譯. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 指令碼的懸浮視窗.

外掛內建視覺化設計器與元件展示: 從啟動器開啟即進入設計器首頁, 可新增或匯入指令碼專案並在真實轉譯器上編輯介面, 元件展示則呈現全部元件的預覽與範例指令碼; 指令碼介面的轉譯仍在宿主處理程序中進行. 宿主透過 INFO 服務發現外掛並讀取版本與相容資訊, 再依契約 (`org.autojs.plugin.compose.api`) 在自身處理程序內實例化轉譯器工廠.

******

### 目前狀態

******

1.8.0 是目前的公開發行版 (GitHub Release, 已登記官方外掛索引). 需要 AutoJs6 6.8.0 (5322) 或更新版本中包含 3-Block Compose 新身分的建置 (宿主提交 f12665ffde 之後); 發佈時 AutoJs6 6.8.0 尚未正式發佈, 需要使用包含該提交的 AutoJs6 建置. AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中開啟" 需要宿主提交 db7e6a1734 之後的建置, 設計器的 TSX 專案需要 TypeScript Engine 0.7.0, UI (XML) 專案的佈局預覽需要包含預覽服務的建置 (宿主提交 306a8f7834 之後). 本外掛提供 UI 頁面, 懸浮窗, 視覺化設計器 (Compose 與 UI 專案), 五個範例, API 參考, 設計器指南頁與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在路線圖中. 圖示已採用維護者提供的正式圖稿. 1.7.0 起 UI (XML) 專案的互動預覽由 AutoJs6 真實執行整份指令碼, 需要包含預覽契約 v2 的 AutoJs6 建置 (宿主提交 4c332c2053 之後) 與 Android 13 或更新版本. 1.8.0 起 UI 專案的佈局可以寫在 XML 檔案中並成為資料夾專案: 預覽使用專案檔案需要預覽契約 v3 (宿主提交 9df0a154b 之後), 在 AutoJs6 中執行或匯入資料夾專案需要宿主提交 faf412ef7 之後.

******

### 功能特性

******

目前開發預覽的核心能力:

- 視覺化設計器: 指令碼檔案即專案, 真實轉譯器所見即所得地預覽介面樹, 大綱 / 元件面板 / 屬性抽屜涵蓋全部 55 個元件, 支援復原重做與草稿還原; 匯出的指令碼可複製或直接傳送到 AutoJs6 執行; 畫布拖放按指標位置插入, 開啟的彈出層畫在畫布內, Image 可顯示 @drawable 與專案圖片目錄中的圖片, 狀態面板可暫時改變 state 的預覽值, 互動預覽在受限的沙盒中執行 JavaScript 專案的指令碼 (Android 8.0 或更新版本), 點擊與輸入按指令碼邏輯更新畫布, 主控台顯示指令碼的輸出與錯誤; 可新建 JavaScript 或 TSX 專案, TSX 專案經 TypeScript Engine 0.7.0 或更新版本解析, 產生規範的 TSX
- 與 AutoJs6 銜接: 專案可傳送執行, 在 AutoJs6 編輯器中開啟 (雙向同步), 匯入 AutoJs6 指令碼目錄, 複製或匯出; 設計器也能從 AutoJs6 編輯器或檔案管理員直接開啟 .js / .tsx 檔案並儲存回原檔案; 匯入的專案可在編輯頁選單中手動寫回來源或從來源重新匯入
- UI (XML) 專案: 設計器也能開啟傳統 "ui" 模式指令碼 (E4X XML 佈局) 並原樣編輯, 未觸碰的位元組 (空白, 註解, 屬性順序, 引號, {運算式}) 保持不變; 畫布由 AutoJs6 的預覽服務用 ui 模式的佈局器繪製 (Android 11 起嵌入真實檢視, 更舊版本為點陣圖快照), 屬性抽屜按文件站的標籤與屬性目錄編輯, 元件面板列出 48 個標籤, 可為元素附加 `ui.<id>.on` 事件樣板, 繫結值面板暫時覆寫 `{{ }}` 的值
- UI (XML) 專案的互動預覽 (1.7.0): 畫布上方的 "互動預覽" 開關開啟後, AutoJs6 把整份指令碼當作一次 ui 模式執行, 畫布顯示指令碼的 ui.layout 並直接接收點擊與觸控, 事件處理函式, `{{ }}` 繫結與其它模組都按指令碼邏輯真實執行, 主控台頁顯示輸出, 錯誤與結束原因; 修改專案後約 0.4 秒重新執行, 關閉開關或離開設計器即停止, 文件與檔案不變
- XML 佈局檔案與資料夾專案 (1.8.0): UI 專案的佈局可以寫在 XML 檔案中 (ui.layoutFile, ui.inflate(files.read(...)), AutoJs6 XML 或 Android 原生 XML); 帶佈局檔案的專案是含 main.js 與 project.json 的資料夾, 可匯出匯入為 zip 或資料夾, 並作為專案交給 AutoJs6 執行或匯入; Android 原生 XML 按 AutoJs6 實際設定的屬性編輯
- 宣告式介面: `compose.state` + `compose.mount(render)` 依狀態變化自動重繪, 同時提供可長期持有的節點控制代碼 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 核心集: 29 個節點工廠涵蓋版面配置, 文字, 圖示, 圖片, 按鈕, 輸入, 選擇控制項, 惰性清單, 對話方塊與進度; Snackbar 是工作階段命令, 不是 compose.Snackbar 工廠
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 範圍限定操作在宿主端驗證
- 兩種承載方式: `"ui";` 指令碼透過 `compose.mount` 或可呼叫的 `compose` / `$compose` 掛載 Activity 內容; `compose.floaty` 建立 raw 或可調整懸浮視窗, 同樣支援非 ui 指令碼
- 行程內繪製: 外掛在宿主行程套用介面更新, 事件排隊回到所屬指令碼執行緒; 工作執行緒透過 compose.post 請求更新
- 一個 APK 包含 arm64-v8a / armeabi-v7a / x86_64 / x86, 無外掛自有原生程式碼; 隨附的 AndroidX graphics-path 輔助程式庫仍需符合 Android, 宿主與外掛相容條件
- 原生文字編輯保留選取範圍與輸入法組合狀態, 支援焦點和明確編輯, 拒絕覆寫較新輸入的延遲編輯; 開關與滑桿仍由指令碼控制狀態
- 整合防護: 外掛程式缺少或不相容時可用性探測傳回不可用, 錯誤使用 `ComposeError`, 關閉工作階段或停止指令碼會釋放所屬視窗與回呼
- 計數器, 表單驗證, 1000 項鍵控清單, 非 ui 浮動 HUD 與主題五個可執行範例, 包含前置條件與索引, 同步至相符宿主的 3-Block Compose 範例分類
- TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊浮動視窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- compose.dialog 傳回可更新和關閉的工作階段, 支援一般對話框與模態底部面板, 可在 UI 或一般指令碼中使用
- Material 3 擴充元件: 導覽列與抽屜, 分頁標籤, 底部面板與選單, 日期與時間選擇器, 分頁與網格, 資訊方塊, 徽章, 分段按鈕, 懸浮按鈕, 搜尋列, 提示與下拉重新整理
- 元件展示: 啟動器入口展示全部 55 個元件的 Material 3 預覽與可執行範例指令碼, 可複製或傳送到 AutoJs6; 設定頁的語言, 夜間模式與主題色預設跟隨 AutoJs6, 並提供四種啟動器圖示

******

### 使用方式

******

1. 安裝包含 compose 指令碼入口的相符本機 AutoJs6 建置 (最低 6.8.0 / 5322)
2. 安裝此外掛 APK; 從啟動器開啟 3-Block Compose 進入設計器首頁, 可新增指令碼專案, 或從頂部列開啟元件展示與設定
3. 在 AutoJs6 的外掛程式中心確認 3-Block Compose 已被識別並處於啟用狀態
4. 在指令碼中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮視窗權限再使用 `compose.floaty`
5. 編輯頁選單提供傳送執行, 在 AutoJs6 編輯器中開啟, 匯入 AutoJs6 指令碼目錄, 複製與匯出; 在 AutoJs6 編輯器選單 (需 AutoJs6 包含對應更新) 或檔案管理員中選擇 3-Block Compose 也可以直接編輯指令碼檔案

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

五個可執行指令碼由 assets/examples/index.json 列出, 同步至相符宿主的 3-Block Compose 範例分類. 每例開頭說明模式與權限. 節點, 修飾鏈, 主題, 工作階段與浮動視窗詳情請使用同批本機 API 文件及 TypeScript/編輯器宣告; 線上網站不一定已包含這些本機變更.

******

### 相容性

******

外掛程式的執行需求與限制:

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更新; 低於該版本的宿主會在外掛程式中心提示不相容
- Android 版本: 7.0 (API 24) 或更新
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內建全部四種, 無需依架構選擇安裝套件)
- Compose 版本: 由外掛程式自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 執行階段
- 契約版本: 2; 宿主與外掛程式透過契約版本協商, 不相符時拒絕載入並給出明確錯誤
- 打包應用程式仍需另外安裝相容的 3-Block Compose 外掛, 啟用/授權記錄屬於該應用程式; 相容性檢查依據內建 AutoJs6 執行階段, 不是打包應用程式自身的 versionCode
- View 工廠在轉譯前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父檢視被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫端管理
- cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與指令碼結束仍會清理彈窗, 保留現有頁面和其他工作階段
- 此版本使用 3-Block Compose 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 轉譯器的原有元件, 擴充元件需要 V2 轉譯器
- compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此外掛無需更新, TSX 配套需要 TypeScript Engine 0.6.8
- 契約類別 (org.autojs.plugin.compose.api) 自 1.2.0 起隨外掛打包供獨立處理程序使用; 在 AutoJs6 內仍由宿主提供同名類別, 外掛副本不生效
- 設計器在外掛自身處理程序執行, 不依賴 AutoJs6; 傳送執行, 在 AutoJs6 編輯器中開啟與匯入 AutoJs6 指令碼目錄需要已安裝 AutoJs6, 編輯器選單的 "在 3-Block Compose 設計器中開啟" 需要包含該入口的 AutoJs6 建置
- 設計器的 TSX 專案需要 TypeScript Engine 0.7.0 或更新版本 (提供語法樹解析服務), 傳送執行時由 AutoJs6 經 TypeScript Engine 編譯; 未安裝或版本過舊時 TSX 專案以唯讀原始碼方式開啟, 可檢視與複製
- 設計器在互動預覽中執行指令碼需要 Android 8.0 (API 26) 或更新版本 (沙盒中的 JavaScript Date 以 java.time 為基礎); 更舊版本的裝置上, JavaScript 專案按 TSX 專案的方式預覽
- UI (XML) 專案的佈局預覽需要包含預覽服務的 AutoJs6 6.8.0 建置 (宿主提交 306a8f7834 之後; 外掛按服務是否存在判斷, 不按版本號), Android 11 (API 30) 起畫布嵌入 AutoJs6 繪製的真實檢視, 更舊版本顯示點陣圖快照; 沒有該服務時 UI 專案仍可編輯, 畫布說明原因
- UI (XML) 專案的互動預覽需要包含預覽契約 v2 的 AutoJs6 6.8.0 建置 (宿主提交 4c332c2053 之後) 與 Android 13 (API 33) 或更新版本; Android 13 - 14 上畫布中的輸入框收不到鍵盤輸入, Android 15 起可以; 不符合條件時不顯示開關, 畫布上方說明原因, 可改用 "在 AutoJs6 中執行"; 只有契約 v1 的 AutoJs6 建置仍可提供佈局預覽
- 帶佈局檔案的 UI 專案 (1.8.0): 預覽使用專案檔案 (res 資源, 佈局檔案) 需要包含預覽契約 v3 的 AutoJs6 6.8.0 建置 (宿主提交 9df0a154b 之後), 在 AutoJs6 中執行或匯入資料夾專案需要宿主提交 faf412ef7 之後; 更早的建置仍按 XML 文字預覽, 兩個去向換成一行提示

******

### 常見問題

******

- 展示中的範例如何執行? 點選 "在 AutoJs6 中執行" 會把指令碼交給已安裝的 AutoJs6 執行; 展示本身只呈現預覽與程式碼, 不執行指令碼
- 設計器能開啟任意指令碼嗎? 它只解釋頂層的 `compose.state` 宣告與唯一的 `compose.mount` / `floaty` / `dialog` 呼叫, 其它陳述式按原始碼原樣保留; 根節點不是 compose 工廠, 多處掛載, 轉譯函式無 return 等無法對應的結構會拒絕開啟並指出行列; TSX 專案中 `compose.X` / `compose:X` 標籤是可編輯的節點, 其它標籤與 `{}` 運算式按原文保留, XML 標籤與展開屬性會拒絕
- 設計器會執行指令碼嗎? 只在互動預覽中. 編輯時它只解析指令碼 (JavaScript 用 Rhino 解析器, TSX 用 TypeScript Engine 的解析服務) 並用外掛自帶的轉譯器預覽, 事件處理函式不會執行, state 顯示初值或狀態面板中設定的預覽值. 開啟互動預覽後, JavaScript 專案的指令碼在外掛內受限的沙盒中執行 (需要 Android 8.0 或更新版本): 只提供 compose, console, toast 與計時器, 不能存取 Java / Android 類別, 檔案, 網路或其他 AutoJs6 模組, 單次執行超過約 3 秒會被停止, 執行結果只顯示在畫布上, 不會修改專案. TSX 專案 (以及 Android 8.0 以下裝置上的專案) 不執行指令碼, 繫結了 state 的控制項只改變預覽值. 真正執行指令碼要交給 AutoJs6. UI (XML) 專案的畫布由 AutoJs6 的預覽服務只做佈局 (建立檢視並繪製), 不執行專案的指令碼; `{{ }}` 繫結的值來自指令碼頂層的字面值宣告 (在沙盒中求值) 或繫結值面板的覆寫. 1.7.0 起 UI (XML) 專案的互動預覽不同: 開啟開關後 AutoJs6 會像 "在 AutoJs6 中執行" 一樣真實執行整份指令碼 (沒有沙箱的限制, 副作用真實發生, 每個專案首次開啟時先確認), 只是 ui.layout 的內容顯示在畫布上; 結果仍不寫入專案
- 為什麼找不到 `compose`? 全域物件由相符的本機宿主建置提供, 單獨安裝外掛程式 APK 不會加入該入口
- 是否需要解除安裝其它介面外掛程式? 不需要, 3-Block Compose 與現有 `ui` 模組及其它外掛程式互不影響
- 外掛變更時會怎樣? 更新, 解除安裝或停用外掛會關閉作用中的工作階段並報告對應錯誤; 相容且啟用的外掛允許重新掛載
- 懸浮視窗需要什麼條件? 先授予宿主懸浮視窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示視窗, 請先返回桌面. 缺少權限會傳回 PERMISSION_REQUIRED, 不會自動彈出授權介面
- 能否使用 TSX 或任意 Compose 函式? 配合相符的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留指令碼引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬工作階段, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

******

### 權限與安全

******

外掛程式不申請任何 Android 執行階段權限, 也不存取網路, 儲存空間或感應器.

- 元件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 宿主可以存取; 設計器編輯頁是無權限的匯出元件, 只接收使用者交出的指令碼文件 Uri, 讀寫都限於該 Uri
- 背景行為: 外掛沒有常駐服務與定時工作, 僅在自身被更新時接收一次系統廣播以校正啟動器圖示元件; 不被宿主載入且未開啟設計器或展示頁時不消耗資源
- 資料邊界: 轉譯器不讀寫指令碼資料與使用者檔案, 介面狀態只存在於宿主處理程序記憶體中; 設計器的專案與草稿存於外掛私有目錄, 只經系統檔案選擇器匯入匯出, 或按使用者交出的 Uri 讀寫單個指令碼檔案; 專案目錄僅透過外掛自己的內容提供者在交給 AutoJs6 時按次授權; 匯入時會保留所選檔案的存取權限 (提供者允許時), 但只在使用者選擇 "寫回來源" 或 "從來源重新匯入" 時讀寫該檔案
- 指令碼沙盒: 設計器的互動預覽在外掛程序內以 Rhino 直譯器執行 JavaScript 專案的指令碼, 只提供 compose, console, toast 與計時器; 不能存取 Java / Android 類別, 檔案, 網路或其他 AutoJs6 模組, 每次執行都有指令數與時長上限, 執行結果只用於畫布, 不會寫入專案
- 備份原則: 已停用應用程式備份與裝置移轉, 外掛程式本身不持有任何需要移轉的資料
- 佈局預覽: UI (XML) 專案的畫布由已安裝的 AutoJs6 繪製: 外掛只把 XML 片段與繫結值經 AutoJs6 的預覽服務 (受 PLUGIN 簽章權限保護) 交給它, AutoJs6 以 ui 模式的佈局器建立檢視並畫回, 不執行專案的指令碼, 也不能存取外掛的專案檔案; 該預覽執行階段出現在 AutoJs6 的執行清單中, 可隨時停止; 1.8.0 起, 對支援的 AutoJs6 (預覽契約 v3), 外掛還會交出專案檔案的唯讀 zip 副本 (資料夾專案, 或指令碼檔案旁的 res 目錄), 供佈局與執行解析資源
- 互動預覽 (UI 專案): 開啟開關後, 外掛把指令碼文字交給 AutoJs6 的預覽服務, AutoJs6 以普通 ui 模式執行它 (與 "在 AutoJs6 中執行" 相同的引擎與指令碼權限, 副作用真實發生), 只把 ui.layout 的內容顯示在畫布上並把主控台輸出回傳; 每個專案首次開啟時需要確認, 執行出現在 AutoJs6 的執行清單中, 關閉開關, 離開設計器或從 AutoJs6 停止即結束; 指令碼不在外掛程序中執行, 外掛也不擴大指令碼的權限
- 資料夾授權與專案 zip (1.8.0): 以內容 Uri 開啟的指令碼只在使用者於系統資料夾選擇器中選定, 且核對含有該指令碼的資料夾裡讀寫佈局檔案 (只保留該資料夾的讀寫授權); 資料夾專案以 zip 經外掛自己的內容提供者按次授予讀取權限交給 AutoJs6, AutoJs6 核對每個項目都在專案內後才解壓縮

宿主載入轉譯器時沿用自身的指令碼權限模型, 外掛程式不擴大指令碼可存取的系統能力.

******

### 外掛程式介面

******

面向宿主的介面識別碼如下:

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

宿主透過 `org.autojs.plugin.INFO` 探索外掛程式並讀取 `requiresHostVersion` 等能力資訊; 轉譯器工廠類別名稱由 `org.autojs.plugin.compose.RENDERER_FACTORY` 中繼資料宣告, 宿主以外掛程式 APK 路徑建立類別載入器 (父載入器為宿主) 並在宿主處理程序內實例化.

******

### 藍圖

******

外掛程式的里程碑, 設計決策與驗收標準統一記錄在藍圖中:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### 版本歷史

******

#### v1.8.0

_2026/10/16_

- `提示` 1.8.0 的部分功能需要更新的 AutoJs6 6.8.0 建置: 預覽使用專案檔案 (res 資源, 佈局檔案) 需要宿主提交 9df0a154b 之後 (預覽契約 v3), 在 AutoJs6 中執行或匯入資料夾專案需要宿主提交 faf412ef7 之後; AutoJs6 版本較舊時設計器會提示並退回原有行為
- `新增` UI (XML) 專案的佈局可以寫在獨立的 XML 檔案中: 以字面量路徑呼叫 ui.layoutFile("...") 與 ui.inflate(files.read("...")), 可以是 AutoJs6 XML 或 Android 原生 XML (ui.useAndroidLayout); 設計器讀取並編輯這些檔案, 未變更的內容原樣保留, 片段切換器以檔名列出它們
- `新增` 片段切換器旁的選單 "存為 XML 檔案" 與 "新增 XML 檔案": 前者把直接寫在 ui.layout(...) 或 ui.inflate(...) 中的佈局移到檔案並改寫呼叫, 後者新增只含一個空容器的檔案 (AutoJs6 XML 或 Android 原生 XML) 並在指令碼末尾加上 ui.inflate 一行; 兩者都可復原, 儲存時才寫入檔案
- `新增` 資料夾專案: 帶佈局檔案儲存的私有專案變為資料夾 (main.js, 佈局檔案與 AutoJs6 認可的 project.json), 可在 AutoJs6 中作為專案執行與打包; 新增 UI 專案時也可以直接建立這樣的資料夾 (AutoJs6 XML 或 Android 原生 XML 範本); 首頁以 "名稱/" 顯示資料夾專案
- `新增` 資料夾專案可匯出為一個 zip, 首頁選單可匯入 zip 或資料夾 ("匯入指令碼或 zip", "匯入資料夾")
- `新增` "在 AutoJs6 中執行" 與 "匯入 AutoJs6 指令碼目錄" 把帶佈局檔案的專案以 zip 交給 AutoJs6: 執行時解壓縮到快取目錄以專案方式執行, 結束後清除; 匯入時解壓縮到同名資料夾, 已存在時詢問覆寫或另存為新資料夾
- `新增` 從 AutoJs6 編輯器開啟的指令碼可以讀寫同一資料夾中的佈局檔案: 第一次需要在系統資料夾選擇器中選擇指令碼所在的資料夾, 之後保留授權
- `新增` Android 原生 XML 佈局按 AutoJs6 實際支援的屬性編輯: 屬性抽屜列出各元件類別可設定的 android: / app: 屬性與父佈局的參數, id 寫作 @+id/..., 元件面板插入 Android 元件類別, 問題清單提示 AutoJs6 會忽略或無法設定的屬性
- `新增` UI 專案的問題清單單列 "預覽差異": 指令碼中在預覽裡與在 AutoJs6 中執行行為不同的寫法 (activity, ui.emitter, 視窗層級函式, 對話框, 主執行緒上的無限迴圈等), 每條帶行號與說明, 點按開啟文件, 不計入錯誤數
- `最佳化` 佈局預覽與互動預覽把專案檔案交給 AutoJs6, 佈局可以參照 res 中的字串, 顏色, 尺寸, 樣式, 圖片與 `<include>` 佈局, 互動預覽中的 ui.layoutFile 也能正常執行 (需要預覽契約 v3); AutoJs6 版本較舊時問題清單提示預覽可能與實際執行不同
- `最佳化` 向只含空白的容器插入元素時不再留下空行
- `相依` 升級 ui-preview-api.aar 契約製品至 ui 預覽契約 v3 (新增隨佈局交出專案資源與隨執行交出專案 zip 的請求鍵; 只有 v2 或 v1 的 AutoJs6 建置仍提供原有預覽)

#### v1.7.0

_2026/10/15_

- `提示` UI (XML) 專案的互動預覽現在由 AutoJs6 真實執行整份指令碼 (需要包含預覽契約 v2 的 AutoJs6 6.8.0 建置, 宿主提交 4c332c2053 之後, 以及 Android 13 或更新版本): 指令碼的副作用與在 AutoJs6 中執行相同, 每個專案首次開啟開關時會先確認; 不符合條件時畫布上方說明原因, 可改用 "在 AutoJs6 中執行"
- `新增` UI (XML) 專案的互動預覽: 開啟畫布上方的 "互動預覽" 開關後, AutoJs6 把整份指令碼當作一次 ui 模式執行, 畫布顯示指令碼的 ui.layout 並直接接收點擊與觸控, 事件處理函式, {{ }} 繫結, 計時器與其它模組都按指令碼邏輯真實執行; 修改專案後約 0.4 秒重新執行, 關閉開關, 設計器轉到背景或關閉編輯頁時停止, 畫布回到佈局預覽; 文件, 專案檔案與草稿都不會被改動. Android 13 - 14 上畫布中的輸入框收不到鍵盤輸入, Android 15 起可以
- `新增` UI 專案的 "主控台" 頁在互動預覽期間顯示 AutoJs6 執行指令碼的輸出 (按等級著色), 執行狀態與結束原因; 指令碼出錯時畫布上方顯示錯誤 (帶行號), 點擊開啟主控台; 執行以專案檔名出現在 AutoJs6 的執行清單中, 也可以從那裡停止
- `修正` 設計器開啟 UI 專案時偶爾顯示 "AutoJs6 預覽服務現在無法使用 (at most 2 sessions per client)" 的問題: 畫布尺寸在佈局穩定前變化兩次時, 外掛會向 AutoJs6 重複建立預覽工作階段, 觸到每個用戶端的工作階段上限; 現在同一時間只建立一個工作階段, 期間的尺寸與外觀變化隨後補上
- `相依` 升級 ui-preview-api.aar 契約製品至 ui 預覽契約 v2 (在 v1 之後附加執行與停止指令碼的介面和主控台 / 錯誤 / 結束回呼; 只有 v1 的 AutoJs6 建置仍可提供佈局預覽)

#### v1.6.0

_2026/10/15_

- `提示` 設計器新增 UI (XML) 專案: 可以開啟與編輯傳統 "ui" 模式指令碼 (E4X XML 佈局), 畫布由 AutoJs6 繪製. 佈局預覽需要包含預覽服務的 AutoJs6 6.8.0 建置 (宿主提交 306a8f7834 之後), Android 11 (API 30) 起畫布嵌入 AutoJs6 繪製的真實檢視, 更舊版本顯示點陣圖快照; 沒有該服務時仍可編輯, 畫布說明原因
- `新增` 設計器支援 UI (XML) 專案: 新建專案時可選 "UI (XML)", 匯入或從 AutoJs6 編輯器 / 檔案管理器開啟 "ui" 模式指令碼也按 UI 專案讀取; 首頁清單以 JS / TSX / UI 標註專案類型. 指令碼中的 XML 佈局以帶位置的語法樹保存, 未觸碰的部分 (空白, 註解, 屬性順序, 引號, {運算式}) 逐位元組保留, 只有被編輯的開始標籤, 文字與新插入的元素會被改寫; 指令碼裡的其他 XML 字面值也能切換到畫布編輯
- `新增` UI 專案的編輯頁: 大綱 (摺疊, 上下移, 刪除, 長按拖曳, 多個佈局時切換), 屬性抽屜 (標籤, id, 文字內容, 已設定的屬性與按 View 類別分組的目錄屬性, 文字 / `{{ }}` 繫結 / {運算式} 三態, 裸屬性, 任意屬性名稱), 元件面板 (文件的 48 個標籤, 按分類列出並帶預設屬性), 繫結值面板 (`{{ }}` 名稱的求值結果與暫時覆寫, 只影響畫布), 問題清單 (按文件站的標籤與屬性目錄檢驗) 與主控台 (AutoJs6 繪製佈局時回報的診斷); 儲存, 草稿復原, 來源同步, 傳送執行, 在 AutoJs6 編輯器中開啟, 匯入 AutoJs6 指令碼目錄, 複製與匯出對 UI 專案同樣可用
- `新增` UI 專案的畫布由 AutoJs6 繪製 (佈局預覽): 外掛把 XML 片段與繫結值交給 AutoJs6 的預覽服務, AutoJs6 用 ui 模式的佈局器建立真實檢視並畫回畫布, 不執行專案的指令碼; 點選與拖曳在畫布上照常運作, 繪製失敗時定位到出錯的元素; 預覽執行階段以 "3-Block Compose 預覽" 出現在 AutoJs6 的執行清單中, 可從那裡停止
- `新增` UI 專案的屬性抽屜可以為選取的元素新增事件: 在 ui.layout 呼叫之後附加一段 `ui.<id>.on('事件', ...)` 處理函式樣板 (元素沒有 id 時自動補一個), 處理邏輯在指令碼編輯器中編寫; 指令碼已註冊的事件顯示為 "指令碼中已處理"
- `相依` 附加 ui-preview-api.aar 契約製品 (AutoJs6 ui 佈局預覽契約 v1), 供設計器請求 AutoJs6 繪製 UI 專案的畫布

##### 更多版本歷史可參閱

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-zh-Hant-TW.md)

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

本專案依據 [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE) 發行. 第三方元件的授權資訊見 [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### 相關連結

******

- AutoJs6 專案首頁: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 文件: https://docs.autojs6.com
- compose 模組文件: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- 第三方聲明: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
