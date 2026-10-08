Compose UI 是 AutoJs6 的介面轉譯外掛程式. 指令碼透過宿主提供的 `compose` / `$compose` 入口宣告介面, 外掛程式在宿主處理程序內以 Jetpack Compose 與 Material 3 完成轉譯. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 指令碼的懸浮視窗.

1.1.0 本機開發預覽: 需要相符的 AutoJs6 宿主建置, 並安裝和啟用本外掛. 本機配套提供 UI 頁面, 浮動視窗, 五個範例, API 參考與 TypeScript 宣告. 已驗證的相容性及效能範圍記錄在藍圖中. 目前未登錄官方索引, 尚無官方發行版. 圖示圖案仍為臨時預留, 等待維護者提供正式來源圖片.

### 使用方式

1. 安裝包含 compose 指令碼入口的相符本機 AutoJs6 建置 (最低 6.8.0 / 5322)
2. 安裝本外掛程式 APK (無需開啟, 外掛程式沒有啟動器進入點)
3. 在 AutoJs6 的外掛程式中心確認 Compose UI 已被識別並處於啟用狀態
4. 在指令碼中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮視窗權限再使用 `compose.floaty`

### 相容性

- 最低 AutoJs6 版本: 6.8.0 (5322) 或更新; 低於該版本的宿主會在外掛程式中心提示不相容
- Android 版本: 7.0 (API 24) 或更新
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內建全部四種, 無需依架構選擇安裝套件)
- Compose 版本: 由外掛程式自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 執行階段
- 契約版本: 2; 宿主與外掛程式透過契約版本協商, 不相符時拒絕載入並給出明確錯誤
- 打包應用程式仍需另外安裝相容的 Compose UI 外掛, 啟用/授權記錄屬於該應用程式; 相容性檢查依據內建 AutoJs6 執行階段, 不是打包應用程式自身的 versionCode
- View 工廠在轉譯前於主執行緒執行. 無效替換保留目前內容; 同一 View 不可屬於兩個節點, 也不會從其他父檢視被搶佔. 借用的 View 保留原有監聽器, 外部資源仍由呼叫端管理
- cancelable=false 同時禁止返回鍵, 點擊外部和下滑關閉; 主動關閉與指令碼結束仍會清理彈窗, 保留現有頁面和其他工作階段
- 此版本使用 Compose UI 契約 V2, 需要配套 AutoJs6 6.8.0 / 5322; TSX 配套需要 TypeScript Engine 0.6.7. 新宿主仍可使用舊 V1 轉譯器的原有元件, 擴充元件需要 V2 轉譯器
- compose.memo 由 AutoJs6 6.8.0 / 5323 提供, 可在 render 中重用依賴未變化的片段; 此外掛無需更新, TSX 配套需要 TypeScript Engine 0.6.8

### 常見問題

- 為什麼安裝後找不到外掛程式圖示? 外掛程式沒有獨立介面, 也不會在啟動器顯示, 請在 AutoJs6 的外掛程式中心查看
- 為什麼找不到 `compose`? 全域物件由相符的本機宿主建置提供, 單獨安裝外掛程式 APK 不會加入該入口
- 是否需要解除安裝其它介面外掛程式? 不需要, Compose UI 與現有 `ui` 模組及其它外掛程式互不影響
- 外掛變更時會怎樣? 更新, 解除安裝或停用外掛會關閉作用中的工作階段並報告對應錯誤; 相容且啟用的外掛允許重新掛載
- 懸浮視窗需要什麼條件? 先授予宿主懸浮視窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示視窗, 請先返回桌面. 缺少權限會傳回 PERMISSION_REQUIRED, 不會自動彈出授權介面
- 能否使用 TSX 或任意 Compose 函式? 配合相符的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留指令碼引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬工作階段, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

### 權限與安全

- 元件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽章權限保護, 只有 AutoJs6 宿主可以存取
- 無背景行為: 外掛程式沒有常駐服務, 廣播接收器或排程工作, 不被宿主載入時不消耗資源
- 資料邊界: 外掛程式不讀寫指令碼資料與使用者檔案, 介面狀態只存在於宿主處理程序記憶體中
- 備份原則: 已停用應用程式備份與裝置移轉, 外掛程式本身不持有任何需要移轉的資料

更多資訊 (快速開始, 建置說明, 藍圖) 見專案首頁: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
