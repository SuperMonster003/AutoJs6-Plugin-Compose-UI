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

Compose UI 是 AutoJs6 的介面轉譯外掛程式. 指令碼透過宿主內建的 `compose` 全域物件宣告介面, 外掛程式在宿主處理程序內以 Jetpack Compose 與 Material 3 完成轉譯, 為 `"ui";` 模式 Activity 內容與懸浮視窗提供統一的宣告式介面方案.

外掛程式不包含任何獨立介面, 也不在啟動器中顯示進入點. 宿主透過 INFO 服務探索外掛程式並讀取版本與相容資訊, 再依契約 (`org.autojs.plugin.compose.api`) 在宿主處理程序內載入轉譯器. 介面樹, 狀態與事件在指令碼端描述, 轉譯器只負責把修補套用到 Compose 組合並把使用者事件回傳給指令碼.

******

### 目前狀態

******

P1 開發預覽: 宿主與外掛的 V1 契約已凍結. 正式轉譯器與 compose 指令碼 API 仍在開發中, 專用測試宿主可執行原型計數器.

******

### 功能特性

******

外掛程式計劃交付的核心能力:

- 宣告式介面: `compose.state` + `compose.mount(render)` 依狀態變化自動重繪, 同時提供可長期持有的節點控制代碼 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 元件核心集: 版面配置 (Column / Row / Box / LazyColumn 等), 文字, 按鈕, 輸入欄位, 開關, 滑桿, 進度列, 卡片, 對話方塊等
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 範圍限定操作在宿主端驗證
- 兩種承載面: `"ui";` 指令碼的 Activity 內容 (`compose.mount`) 與任意指令碼的懸浮視窗 (`compose.floaty`)
- 處理程序內轉譯: 轉譯器在宿主處理程序中執行, 沒有跨處理程序介面橋接, 事件與狀態更新低延遲
- 單一安裝套件: 不區分 ABI, 不含外掛程式自有原生程式碼 (僅隨 Compose 附帶的 AndroidX graphics-path 輔助程式庫, 四種 ABI 全部內建), 一個 APK 適用所有裝置

******

### 使用方式

******

1. 安裝 AutoJs6 6.8.0 (5308) 或更新版本
2. 安裝本外掛程式 APK (無需開啟, 外掛程式沒有啟動器進入點)
3. 在 AutoJs6 的外掛程式中心確認 Compose UI 已被識別並處於啟用狀態
4. 在指令碼中直接使用 `compose` 全域物件 (轉譯能力將隨 1.0.0 版本交付)

******

### 快速開始

******

以下範例展示目標 API 形態 (以藍圖附錄 A 為準, 轉譯能力交付前尚不能執行):

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

完整的 API 說明 (元件目錄, Modifier 操作, 工作階段物件, 錯誤碼) 見 AutoJs6 文件的 compose 模組章節.

******

### 相容性

******

外掛程式的執行需求與限制:

- AutoJs6 版本: 6.8.0 (5308) 或更新; 低於該版本的宿主會在外掛程式中心提示不相容
- Android 版本: 7.0 (API 24) 或更新
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內建全部四種, 無需依架構選擇安裝套件)
- Compose 版本: 由外掛程式自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 執行階段
- 契約版本: 1; 宿主與外掛程式透過契約版本協商, 不相符時拒絕載入並給出明確錯誤

******

### 常見問題

******

- 為什麼安裝後找不到外掛程式圖示? 外掛程式沒有獨立介面, 也不會在啟動器顯示, 請在 AutoJs6 的外掛程式中心查看
- 為什麼指令碼裡的 `compose` 還不能用? 目前為 P0 開發預覽, 轉譯器與指令碼 API 將在後續里程碑交付
- 是否需要解除安裝其它介面外掛程式? 不需要, Compose UI 與現有 `ui` 模組及其它外掛程式互不影響
- 外掛程式更新後指令碼需要修改嗎? 契約版本不變時無需修改; 契約升級會在更新日誌中明確標註

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
minimum host build: 5308 (6.8.0)
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

#### v1.0.0

_2026/10/02_

- `提示` P1 開發預覽: 宿主與外掛的 V1 契約已凍結. 正式轉譯器與 compose 指令碼 API 仍在開發中, 專用測試宿主可執行原型計數器
- `提示` 需要 AutoJs6 6.8.0 (5308) 或更新版本 (準確的最低版本號待宿主端變更落地後回填)
- `新增` 外掛程式存放庫骨架: 平台版本外掛程式建置鏈, Jetpack Compose BOM 2026.09.00 相依, Wake Activity 啟用協定與 INFO 服務 (類別 compose-ui)
- `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌, 由 JSON 來源檔案統一產生
- `新增` P0 開發預覽: 專用測試宿主可轉譯 Column / Text / Button 計數器. compose 指令碼 API 尚未交付, 安裝外掛後仍不能透過指令碼轉譯介面
- `相依` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
- `相依` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `相依` 附加 compose-ui-api.aar V1 (MPL 2.0, 雜湊鎖定), 共用相依性與宿主對齊

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
