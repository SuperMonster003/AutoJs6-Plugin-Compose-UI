******

### 版本歷史

******

# v1.0.0

###### 2026/10/03

* `提示` P1 開發預覽: V1 宿主載入器與會話已可在專用測試宿主中運行 Column / Text / Button 計數器. 完整渲染器與 compose 腳本 API 仍在開發中
* `提示` 需要 AutoJs6 6.8.0 (5316) 或更高版本
* `新增` 插件倉庫骨架: 平台版本插件構建鏈, Jetpack Compose BOM 2026.09.00 依賴, Wake Activity 激活協議與 INFO 服務 (類別 compose-ui)
* `新增` 10 種語言的 README, 插件中心說明與更新日誌, 由 JSON 源檔案統一生成
* `新增` 預覽計數器支援增量更新與關閉後的回調清理, 更新被拒絕時保留上一次有效界面
* `依賴` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
* `依賴` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `依賴` 附加與 AutoJs6 6.8.0 (5316) 對齊的 compose-ui-api.aar V1 (MPL 2.0, 摘要鎖定), 共用依賴與宿主對齊
