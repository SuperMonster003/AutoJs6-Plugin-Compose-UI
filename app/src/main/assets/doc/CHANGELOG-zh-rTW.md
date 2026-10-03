******

### 版本歷史

******

# v1.0.0

###### 2026/10/03

* `提示` P1 開發預覽: V1 宿主載入器與工作階段已可在專用測試宿主中執行 Column / Text / Button 計數器. 完整轉譯器與 compose 指令碼 API 仍在開發中
* `提示` 需要 AutoJs6 6.8.0 (5316) 或更新版本
* `新增` 外掛程式存放庫骨架: 平台版本外掛程式建置鏈, Jetpack Compose BOM 2026.09.00 相依, Wake Activity 啟用協定與 INFO 服務 (類別 compose-ui)
* `新增` 10 種語言的 README, 外掛程式中心說明與更新日誌, 由 JSON 來源檔案統一產生
* `新增` 預覽計數器支援增量更新與關閉後的回呼清理, 更新被拒絕時保留上一次有效介面
* `相依` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 雜湊鎖定)
* `相依` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `相依` 附加與 AutoJs6 6.8.0 (5316) 對齊的 compose-ui-api.aar V1 (MPL 2.0, 雜湊鎖定), 共用相依性與宿主對齊
