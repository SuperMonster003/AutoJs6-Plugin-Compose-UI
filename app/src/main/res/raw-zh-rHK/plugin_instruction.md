Compose UI 是 AutoJs6 的界面渲染插件. 腳本透過宿主提供的 `compose` / `$compose` 入口宣告界面, 插件在宿主進程內以 Jetpack Compose 與 Material 3 完成渲染. 目前預覽同時支援 `"ui";` 模式的 Activity 內容與非 ui 腳本的懸浮窗.

1.1.0 本地開發預覽: 需要匹配的 AutoJs6 宿主構建, 並安裝和啟用本插件. 本地配套提供 UI 頁面, 懸浮窗, 五個示例, API 參考與 TypeScript 宣告. 已驗證的兼容性及效能範圍記錄在路線圖中. 目前未登記官方索引, 尚無官方發行版. 圖示圖案仍為臨時佔位, 等待維護者提供正式源圖.

### 使用方式

1. 安裝包含 compose 腳本入口的匹配本地 AutoJs6 構建 (最低 6.8.0 / 5322)
2. 安裝此插件 APK; 從啟動器打開 Compose UI 可查看組件展示與設定
3. 在 AutoJs6 的插件中心確認 Compose UI 已被識別並處於啟用狀態
4. 在腳本中使用 `compose` 或 `$compose`; 透過 `compose.mount` 掛載 Activity 內容, 或先授予宿主懸浮窗權限再使用 `compose.floaty`

### 兼容性

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

### 常見問題

- 展示中的示例如何運行? 點擊 "在 AutoJs6 中運行" 會把腳本交給已安裝的 AutoJs6 執行; 展示本身只顯示預覽與代碼, 不執行腳本
- 為甚麼找不到 `compose`? 全域物件由匹配的本地宿主構建提供, 單獨安裝插件 APK 不會加入該入口
- 是否需要卸載其它界面插件? 不需要, Compose UI 與現有 `ui` 模組及其它插件互不影響
- 插件變更時會怎樣? 更新, 解除安裝或停用插件會關閉活動會話並報告對應錯誤; 兼容且啟用的插件允許重新掛載
- 懸浮窗需要甚麼條件? 先授予宿主懸浮窗權限, 輸入文字前呼叫 `window.requestFocus()`. 若 HyperOS 未顯示窗口, 請先返回桌面. 缺少權限會回傳 PERMISSION_REQUIRED, 不會自動彈出授權界面
- 能否使用 TSX 或任意 Compose 函式? 配合匹配的宿主與 TypeScript Engine, TSX 可使用文件列出的 Compose 節點工廠. 不支援任意 Kotlin Composable 函式或自訂 TSX 元件
- 旋轉是否遺失狀態? 目前宿主自行處理一般方向變化, 保留腳本引擎. 真正的 Activity 重建或銷毀會關閉該引擎及所屬會話, 不自動還原業務狀態
- 選擇器如何尋找元件? testTag 按原樣公開為 ID, 不加入套件名稱前綴. id/testTag 與 desc/contentDescription 是不同資訊; Button 的文字可能是子節點, 必要時沿 parent() 尋找可點擊祖先

### 權限與安全

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以存取
- 後台行為: 插件沒有常駐服務與定時任務, 僅在自身被更新時接收一次系統廣播以校正啟動器圖示組件; 未被宿主載入且未打開展示時不消耗資源
- 數據邊界: 插件不讀寫腳本數據與用戶檔案, 界面狀態只存在於宿主進程記憶體中
- 備份策略: 已停用應用備份與裝置遷移, 插件本身不持有任何需要遷移的數據

更多資訊 (快速開始, 構建說明, 路線圖) 見專案主頁: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
