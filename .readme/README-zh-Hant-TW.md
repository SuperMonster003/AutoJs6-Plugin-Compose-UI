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

1.3.0 本地開發預覽: 需要包含 3-Block Compose 新身份的 AutoJs6 宿主建置 (宿主提交 f12665ffde 之後), 並安裝和啟用本外掛; AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中開啟" 需要宿主提交 db7e6a1734 之後的建置. 本地配套提供 UI 頁面, 懸浮窗, 視覺化設計器, 五個範例, API 參考, 設計器指南頁與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在路線圖中. 目前未登記官方索引, 尚無官方發行版. 圖示已採用維護者提供的正式圖稿.

******

### 功能特性

******

目前開發預覽的核心能力:

- 視覺化設計器: 指令碼檔案即專案, 真實轉譯器所見即所得地預覽介面樹, 大綱 / 元件面板 / 屬性抽屜涵蓋全部 55 個元件, 支援復原重做與草稿還原; 匯出的指令碼可複製或直接傳送到 AutoJs6 執行
- 與 AutoJs6 銜接: 專案可傳送執行, 在 AutoJs6 編輯器中開啟 (雙向同步), 匯入 AutoJs6 指令碼目錄, 複製或匯出; 設計器也能從 AutoJs6 編輯器或檔案管理員直接開啟 .js 檔案並儲存回原檔案
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

******

### 常見問題

******

- 展示中的範例如何執行? 點選 "在 AutoJs6 中執行" 會把指令碼交給已安裝的 AutoJs6 執行; 展示本身只呈現預覽與程式碼, 不執行指令碼
- 設計器能開啟任意指令碼嗎? 它只解釋頂層的 `compose.state` 宣告與唯一的 `compose.mount` / `floaty` / `dialog` 呼叫, 其它陳述式按原始碼原樣保留; 根節點不是 compose 工廠, 多處掛載, 轉譯函式無 return 等無法對應的結構會拒絕開啟並指出行列
- 設計器會執行指令碼嗎? 不會. 它只用 Rhino 解析器解析指令碼並用外掛自帶的轉譯器預覽, 事件不會觸發, state 顯示初值; 執行要交給 AutoJs6
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
- 資料邊界: 轉譯器不讀寫指令碼資料與使用者檔案, 介面狀態只存在於宿主處理程序記憶體中; 設計器的專案與草稿存於外掛私有目錄, 只經系統檔案選擇器匯入匯出, 或按使用者交出的 Uri 讀寫單個指令碼檔案; 專案目錄僅透過外掛自己的內容提供者在交給 AutoJs6 時按次授權
- 備份原則: 已停用應用程式備份與裝置移轉, 外掛程式本身不持有任何需要移轉的資料

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

#### v1.3.0

_2026/10/13_

- `最佳化` 設計器畫布中長按拖曳節點時, 會依目標容器的排列方向 (縱向, 橫向, 網格) 顯示插入線, 放開後節點放到指標所在的位置, 不再只能附加到容器末尾

#### v1.2.0

_2026/10/13_

- `提示` 外掛由 Compose UI 更名為 3-Block Compose: 應用程式 ID 由 io.github.supermonster003.autojs6.plugin.compose.ui 改為 io.github.supermonster003.autojs6.plugin.three.block.compose. Android 會將其視為新應用程式, 舊套件可以保留並存, 設定與授權不會自動遷移, 新套件需要在外掛中心重新啟用和授權
- `提示` 需要包含新外掛身分的 AutoJs6 6.8.0 宿主建置 (宿主提交 f12665ffde 之後); 更早的宿主按舊套件名稱發現外掛, 不會識別本版本
- `提示` 設計器的使用說明見文件站 "3-Block Compose 設計器" 頁; AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中開啟" 需要宿主提交 db7e6a1734 之後的 AutoJs6 6.8.0 建置, 其它去向只需已安裝 AutoJs6
- `新增` 視覺化設計器: 從啟動器進入設計器首頁, 可新增 / 匯入 / 開啟私有目錄中的指令碼專案; 編輯頁提供真實轉譯器預覽畫布, 大綱樹, 元件面板與屬性抽屜, 支援點選與長按拖曳換父, 大綱內重排, 復原 / 重做, 草稿自動儲存與還原, 承載選項編輯, 契約目錄驗證報告, 以及複製指令碼或傳送到 AutoJs6 執行; 橫向畫面或平板上畫布與面板左右排列; 元件展示與設定改從設計器首頁進入
- `新增` 設計器與 AutoJs6 的銜接: 編輯頁選單可把目前指令碼在 AutoJs6 編輯器中開啟 (AutoJs6 儲存後設計器自動重新載入), 匯入 AutoJs6 指令碼目錄, 或匯出到任意位置; 設計器也可以從 AutoJs6 編輯器或檔案管理員直接開啟 .js 檔案並儲存回原檔案, 沒有寫入權限時另存為專案; 專案檔案透過外掛自己的內容提供者按次授權, 不對外公開其它目錄
- `最佳化` 應用程式標題, 外掛 ID (three-block-compose), 套件名稱與元件名稱, 發行產物, 文件及 GitHub 儲存庫 (AutoJs6-Plugin-Three-Block-Compose) 隨更名同步更新; 指令碼 API compose, 契約版本與轉譯行為不變
- `最佳化` 啟動器, 外掛中心與系統應用程式資訊圖示改用維護者提供的正式圖稿, 亮暗兩套圖案共用同一形狀
- `最佳化` 隨套件範例與宿主範例分類改為 3-Block Compose
- `最佳化` 契約 AAR compose-ui-api 改為隨外掛打包並完整保留 (先前只在編譯期引用), 供外掛自身處理程序使用; 在 AutoJs6 內仍優先使用宿主提供的同名契約類別, 轉譯行為與契約版本不變
- `相依` 附加 Rhino 解析器精簡 jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, 與 AutoJs6 宿主同一建置), 僅供視覺化設計器解析指令碼, 不執行指令碼; release 套件只保留被引用的解析類別

#### v1.1.0

_2026/10/08_

- `提示` 1.1.0 本機開發預覽: 需要相符的 AutoJs6 宿主建置, 並安裝和啟用本外掛. 本機配套提供 UI 頁面, 浮動視窗, 五個範例, API 參考與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在藍圖中. 目前未登錄官方索引, 尚無官方發行版. 圖示圖案仍為臨時預留, 等待維護者提供正式來源圖片
- `提示` 此版本使用 Compose UI 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 轉譯器的原有元件, 擴充元件需要 V2 轉譯器
- `提示` compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此外掛無需更新, TSX 配套需要 TypeScript Engine 0.6.8
- `提示` 展示與設定頁在外掛自身處理程序執行, 不改變宿主內的轉譯方式與最低宿主版本; 執行範例指令碼需要已安裝並啟用此外掛的 AutoJs6
- `新增` TSX 支援 `<compose.Column>`, `<compose:Text>`, 節點工廠參照, Fragment, 插槽及響應式回呼; 同一棵樹不能混用 Compose 與舊 XML 節點
- `新增` XML `<compose>` 容器與 compose.attach 可在 UI 頁面或舊浮動視窗內嵌入獨立 Compose 工作階段; compose.AndroidView 可承載現有 Android View 或同步工廠傳回的 View
- `新增` compose.dialog 傳回可更新和關閉的工作階段, 支援一般對話框與模態底部面板, 可在 UI 或一般指令碼中使用
- `新增` Material 3 擴充元件: 導覽列與抽屜, 分頁標籤, 底部面板與選單, 日期與時間選擇器, 分頁與網格, 資訊方塊, 徽章, 分段按鈕, 懸浮按鈕, 搜尋列, 提示與下拉重新整理
- `新增` 元件展示: 從啟動器開啟, 瀏覽全部 55 個元件的 Material 3 預覽與範例指令碼, 可複製到剪貼簿或傳送到已安裝的 AutoJs6 執行
- `新增` 設定頁: 語言, 夜間模式與主題色預設跟隨 AutoJs6, 可個別設定; 啟動器圖示提供自適應 (亮色 / 暗色 / 自動) 與透明背景四種選擇
- `修正` 修復使用 Icon Studio 調整啟動器圖示背景後無法建置的問題
- `最佳化` Android 系統應用程式資訊圖示與圖示工作台共用圖稿及明暗底色, 保留外掛程式中心透明圖稿與現有啟動器選項
- `最佳化` View 工廠在轉譯前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父檢視被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫端管理
- `最佳化` cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與指令碼結束仍會清理彈窗, 保留現有頁面和其他工作階段
- `最佳化` 外掛中心圖示與系統應用程式資訊圖示使用亮色 #FAFAFA / 暗色 #212121 底色, 與其他獨立外掛一致
- `相依` 升級 compose-ui-api.aar 契約檔案, 保留凍結 V1 並附加可選 AndroidView 互操作擴充
- `相依` 附加可選對話框功能至 compose-ui-api.aar, 保留現有 V1 與 AndroidView 契約
- `相依` 附加 compose-ui-api.aar V2 元件目錄, 保留現有節點模型和 V1 元件語義
- `相依` 附加 material-color-utilities 版本 4.1.1 (MIT), 用於設定頁與其他獨立外掛一致的主題色推導
- `相依` 附加 AndroidX activity, core, lifecycle, savedstate, emoji2, window 與 kotlinx-coroutines 等的宿主鎖定版本副本, 供外掛自身處理程序的展示使用; 宿主內仍優先使用宿主副本

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
