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

Compose UI 是 AutoJs6 的界面渲染插件. 腳本透過宿主內置的 `compose` 全域物件宣告界面, 插件在宿主進程內以 Jetpack Compose 與 Material 3 完成渲染, 為 `"ui";` 模式 Activity 內容與懸浮窗提供統一的宣告式界面方案.

插件不包含任何獨立界面, 也不在啟動器中顯示入口. 宿主透過 INFO 服務發現插件並讀取版本與兼容資訊, 再按契約 (`org.autojs.plugin.compose.api`) 在宿主進程內載入渲染器. 界面樹, 狀態與事件在腳本側描述, 渲染器只負責把補丁套用到 Compose 組合並把用戶事件回傳給腳本.

******

### 當前狀態

******

P1 開發預覽: 宿主與插件的 V1 契約已凍結. 正式渲染器與 compose 腳本 API 仍在開發中, 專用測試宿主可執行原型計數器.

******

### 功能特性

******

插件計劃交付的核心能力:

- 宣告式界面: `compose.state` + `compose.mount(render)` 按狀態變化自動重繪, 同時提供可長期持有的節點句柄 (`compose.Text({...})` 等) 直接修改屬性與子節點
- Material 3 組件核心集: 佈局 (Column / Row / Box / LazyColumn 等), 文字, 按鈕, 輸入框, 開關, 滑桿, 進度條, 卡片, 對話框等
- 鏈式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` 保留操作順序, 作用域操作在宿主側校驗
- 兩種承載面: `"ui";` 腳本的 Activity 內容 (`compose.mount`) 與任意腳本的懸浮窗 (`compose.floaty`)
- 進程內渲染: 渲染器在宿主進程中運行, 沒有跨進程界面橋接, 事件與狀態更新低延遲
- 單一安裝包: 不區分 ABI, 不含插件自有原生代碼 (僅隨 Compose 附帶的 AndroidX graphics-path 輔助庫, 四種 ABI 全部內置), 一個 APK 適配所有裝置

******

### 使用方式

******

1. 安裝 AutoJs6 6.8.0 (5308) 或更高版本
2. 安裝本插件 APK (無需開啟, 插件沒有啟動器入口)
3. 在 AutoJs6 的插件中心確認 Compose UI 已被識別並處於啟用狀態
4. 在腳本中直接使用 `compose` 全域物件 (渲染能力將隨 1.0.0 版本交付)

******

### 快速開始

******

以下示例展示目標 API 形態 (以路線圖附錄 A 為準, 渲染能力交付前尚不能運行):

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

完整的 API 說明 (組件目錄, Modifier 操作, 會話物件, 錯誤碼) 見 AutoJs6 文件的 compose 模組章節.

******

### 兼容性

******

插件的運行要求與限制:

- AutoJs6 版本: 6.8.0 (5308) 或更高; 低於該版本的宿主會在插件中心提示不兼容
- Android 版本: 7.0 (API 24) 或更高
- 處理器架構: arm64-v8a / armeabi-v7a / x86_64 / x86 (單一 APK 內置全部四種, 無需按架構選擇安裝包)
- Compose 版本: 由插件自帶 (BOM 2026.09.00), 不依賴宿主的 Compose 運行時
- 契約版本: 1; 宿主與插件透過契約版本協商, 不匹配時拒絕載入並給出明確錯誤

******

### 常見問題

******

- 為什麼安裝後找不到插件圖示? 插件沒有獨立界面, 也不會在啟動器顯示, 請在 AutoJs6 的插件中心查看
- 為什麼腳本裏的 `compose` 還不能用? 當前為 P0 開發預覽, 渲染器與腳本 API 將在後續里程碑交付
- 是否需要卸載其它界面插件? 不需要, Compose UI 與現有 `ui` 模組及其它插件互不影響
- 插件更新後腳本需要修改嗎? 契約版本不變時無需修改; 契約升級會在更新日誌中明確標註

******

### 權限與安全

******

插件不申請任何 Android 運行時權限, 也不存取網絡, 儲存空間或感應器.

- 組件保護: Wake Activity 與 INFO 服務均受 `org.autojs.permission.PLUGIN` 簽名權限保護, 只有 AutoJs6 宿主可以存取
- 無背景行為: 插件沒有常駐服務, 廣播接收器或定時任務, 不被宿主載入時不消耗資源
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
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5308 (6.8.0)
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

#### v1.0.0

_2026/10/02_

- `提示` P1 開發預覽: 宿主與插件的 V1 契約已凍結. 正式渲染器與 compose 腳本 API 仍在開發中, 專用測試宿主可執行原型計數器
- `提示` 需要 AutoJs6 6.8.0 (5308) 或更高版本 (準確的最低版本號待宿主側改動落地後回填)
- `新增` 插件倉庫骨架: 平台版本插件構建鏈, Jetpack Compose BOM 2026.09.00 依賴, Wake Activity 激活協議與 INFO 服務 (類別 compose-ui)
- `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 源檔案統一生成
- `新增` P0 開發預覽: 專用測試宿主可渲染 Column / Text / Button 計數器. compose 腳本 API 尚未交付, 安裝插件後仍不能透過腳本渲染界面
- `依賴` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
- `依賴` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `依賴` 附加 compose-ui-api.aar V1 (MPL 2.0, 摘要鎖定), 共用依賴與宿主對齊

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
