3-Block Compose 是 AutoJs6 的介面轉譯外掛程式. 指令碼透過宿主提供的 `compose` / `$compose` 入口宣告介面, 外掛程式在宿主處理程序內以 Jetpack Compose 與 Material 3 完成轉譯. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 指令碼的懸浮視窗.

1.6.0 是目前的公開發行版 (GitHub Release, 已登記官方外掛索引). 需要 AutoJs6 6.8.0 (5322) 或更新版本中包含 3-Block Compose 新身分的建置 (宿主提交 f12665ffde 之後); 發佈時 AutoJs6 6.8.0 尚未正式發佈, 需要使用包含該提交的 AutoJs6 建置. AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中開啟" 需要宿主提交 db7e6a1734 之後的建置, 設計器的 TSX 專案需要 TypeScript Engine 0.7.0, UI (XML) 專案的佈局預覽需要包含預覽服務的建置 (宿主提交 306a8f7834 之後). 本外掛提供 UI 頁面, 懸浮窗, 視覺化設計器 (Compose 與 UI 專案), 五個範例, API 參考, 設計器指南頁與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在路線圖中. 圖示已採用維護者提供的正式圖稿.

### 使用方式

1. 安裝包含 compose 指令碼入口的相符本機 AutoJs6 建置 (最低 6.8.0 / 5322)
2. 安裝此外掛 APK; 從啟動器開啟 3-Block Compose 進入設計器首頁, 可新增指令碼專案, 或從頂部列開啟元件展示與設定
3. 在 AutoJs6 的外掛程式中心確認 3-Block Compose 已被識別並處於啟用狀態
4. 在指令碼中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮視窗權限再使用 `compose.floaty`
5. 編輯頁選單提供傳送執行, 在 AutoJs6 編輯器中開啟, 匯入 AutoJs6 指令碼目錄, 複製與匯出; 在 AutoJs6 編輯器選單 (需 AutoJs6 包含對應更新) 或檔案管理員中選擇 3-Block Compose 也可以直接編輯指令碼檔案

### 相容性

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

### 常見問題

- 展示中的範例如何執行? 點選 "在 AutoJs6 中執行" 會把指令碼交給已安裝的 AutoJs6 執行; 展示本身只呈現預覽與程式碼, 不執行指令碼
- 設計器能開啟任意指令碼嗎? 它只解釋頂層的 `compose.state` 宣告與唯一的 `compose.mount` / `floaty` / `dialog` 呼叫, 其它陳述式按原始碼原樣保留; 根節點不是 compose 工廠, 多處掛載, 轉譯函式無 return 等無法對應的結構會拒絕開啟並指出行列; TSX 專案中 `compose.X` / `compose:X` 標籤是可編輯的節點, 其它標籤與 `{}` 運算式按原文保留, XML 標籤與展開屬性會拒絕
- 設計器會執行指令碼嗎? 只在互動預覽中. 編輯時它只解析指令碼 (JavaScript 用 Rhino 解析器, TSX 用 TypeScript Engine 的解析服務) 並用外掛自帶的轉譯器預覽, 事件處理函式不會執行, state 顯示初值或狀態面板中設定的預覽值. 開啟互動預覽後, JavaScript 專案的指令碼在外掛內受限的沙盒中執行 (需要 Android 8.0 或更新版本): 只提供 compose, console, toast 與計時器, 不能存取 Java / Android 類別, 檔案, 網路或其他 AutoJs6 模組, 單次執行超過約 3 秒會被停止, 執行結果只顯示在畫布上, 不會修改專案. TSX 專案 (以及 Android 8.0 以下裝置上的專案) 不執行指令碼, 繫結了 state 的控制項只改變預覽值. 真正執行指令碼要交給 AutoJs6. UI (XML) 專案的畫布由 AutoJs6 的預覽服務只做佈局 (建立檢視並繪製), 不執行專案的指令碼; `{{ }}` 繫結的值來自指令碼頂層的字面值宣告 (在沙盒中求值) 或繫結值面板的覆寫
- 為什麼找不到 `compose`? 全域物件由相符的本機宿主建置提供, 單獨安裝外掛程式 APK 不會加入該入口
- 是否需要解除安裝其它介面外掛程式? 不需要, 3-Block Compose 與現有 `ui` 模組及其它外掛程式互不影響
- 外掛變更時會怎樣? 更新, 解除安裝或停用外掛會關閉作用中的工作階段並報告對應錯誤; 相容且啟用的外掛允許重新掛載
- 懸浮視窗需要什麼條件? 先授予宿主懸浮視窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示視窗, 請先返回桌面. 缺少權限會傳回 PERMISSION_REQUIRED, 不會自動彈出授權介面
- 能否使用 TSX 或任意 Compose 函式? 配合相符的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留指令碼引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬工作階段, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

### 權限與安全

- 元件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 宿主可以存取; 設計器編輯頁是無權限的匯出元件, 只接收使用者交出的指令碼文件 Uri, 讀寫都限於該 Uri
- 背景行為: 外掛沒有常駐服務與定時工作, 僅在自身被更新時接收一次系統廣播以校正啟動器圖示元件; 不被宿主載入且未開啟設計器或展示頁時不消耗資源
- 資料邊界: 轉譯器不讀寫指令碼資料與使用者檔案, 介面狀態只存在於宿主處理程序記憶體中; 設計器的專案與草稿存於外掛私有目錄, 只經系統檔案選擇器匯入匯出, 或按使用者交出的 Uri 讀寫單個指令碼檔案; 專案目錄僅透過外掛自己的內容提供者在交給 AutoJs6 時按次授權; 匯入時會保留所選檔案的存取權限 (提供者允許時), 但只在使用者選擇 "寫回來源" 或 "從來源重新匯入" 時讀寫該檔案
- 指令碼沙盒: 設計器的互動預覽在外掛程序內以 Rhino 直譯器執行 JavaScript 專案的指令碼, 只提供 compose, console, toast 與計時器; 不能存取 Java / Android 類別, 檔案, 網路或其他 AutoJs6 模組, 每次執行都有指令數與時長上限, 執行結果只用於畫布, 不會寫入專案
- 備份原則: 已停用應用程式備份與裝置移轉, 外掛程式本身不持有任何需要移轉的資料
- 佈局預覽: UI (XML) 專案的畫布由已安裝的 AutoJs6 繪製: 外掛只把 XML 片段與繫結值經 AutoJs6 的預覽服務 (受 PLUGIN 簽章權限保護) 交給它, AutoJs6 以 ui 模式的佈局器建立檢視並畫回, 不執行專案的指令碼, 也不能存取外掛的專案檔案; 該預覽執行階段出現在 AutoJs6 的執行清單中, 可隨時停止

更多資訊 (快速開始, 建置說明, 藍圖) 見專案首頁: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
