3-Block Compose 是 AutoJs6 的界面渲染插件. 腳本透過宿主提供的 `compose` / `$compose` 入口宣告界面, 插件在宿主進程內以 Jetpack Compose 與 Material 3 完成渲染. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 腳本的懸浮窗.

1.4.0 是首個公開發行版 (GitHub Release, 已登記官方插件索引). 需要 AutoJs6 6.8.0 (5322) 或更高版本中包含 3-Block Compose 新身份的構建 (宿主提交 f12665ffde 之後); 發佈時 AutoJs6 6.8.0 尚未正式發佈, 需要使用包含該提交的 AutoJs6 構建. AutoJs6 編輯器選單的 "在 3-Block Compose 設計器中打開" 需要宿主提交 db7e6a1734 之後的構建, 設計器的 TSX 工程需要 TypeScript Engine 0.7.0. 本插件提供 UI 頁面, 懸浮窗, 可視化設計器, 五個示例, API 參考, 設計器指南頁與 TypeScript 聲明. 已驗證的兼容性及性能範圍記錄在路線圖中. 圖標已採用維護者提供的正式圖稿.

### 使用方式

1. 安裝包含 compose 腳本入口的匹配本地 AutoJs6 構建 (最低 6.8.0 / 5322)
2. 安裝此插件 APK; 從啟動器打開 3-Block Compose 進入設計器首頁, 可新建腳本工程, 或從頂部欄打開組件展示與設定
3. 在 AutoJs6 的插件中心確認 3-Block Compose 已被識別並處於啟用狀態
4. 在腳本中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮窗權限再使用 `compose.floaty`
5. 編輯頁選單提供發送運行, 在 AutoJs6 編輯器中打開, 匯入 AutoJs6 腳本目錄, 複製與匯出; 在 AutoJs6 編輯器選單 (需 AutoJs6 包含對應更新) 或檔案管理器中選擇 3-Block Compose 也可以直接編輯腳本檔案

### 兼容性

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

### 常見問題

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

### 權限與安全

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以訪問; 設計器編輯頁是無權限的導出組件, 只接收用戶交出的腳本文檔 Uri, 讀寫都限於該 Uri
- 後台行為: 插件沒有常駐服務與定時任務, 僅在自身被更新時接收一次系統廣播以校正啟動器圖標組件; 不被宿主加載且未打開設計器或畫廊時不消耗資源
- 數據邊界: 渲染器不讀寫腳本數據與用戶檔案, 界面狀態只存在於宿主進程內存中; 設計器的工程與草稿存於插件私有目錄, 只經系統檔案選擇器匯入匯出, 或按用戶交出的 Uri 讀寫單個腳本檔案; 工程目錄僅通過插件自己的內容提供器在交給 AutoJs6 時按次授權; 匯入時會保留所選檔案的存取權限 (提供者允許時), 但只在用戶選擇 "寫回來源" 或 "從來源重新匯入" 時讀寫該檔案
- 腳本沙盒: 設計器的互動預覽在插件進程內以 Rhino 解譯器運行 JavaScript 工程的腳本, 只提供 compose, console, toast 與計時器; 不能存取 Java / Android 類別, 檔案, 網絡或其他 AutoJs6 模組, 每次運行都有指令數與時長上限, 運行結果只用於畫布, 不會寫入工程
- 備份策略: 已停用應用備份與裝置遷移, 插件本身不持有任何需要遷移的數據

更多資訊 (快速開始, 構建說明, 路線圖) 見專案主頁: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
