******

### 版本历史

******

# v1.0.0

###### 2026/10/02

* `提示` P1 开发预览: V1 宿主装载器与会话已可在专用测试宿主中运行 Column / Text / Button 计数器. 完整渲染器与 compose 脚本 API 仍在开发中
* `提示` 需要 AutoJs6 6.8.0 (5308) 或更高版本 (准确的最低版本号待宿主侧改动落地后回填)
* `新增` 插件仓库骨架: 平台版本插件构建链, Jetpack Compose BOM 2026.09.00 依赖, Wake Activity 激活协议与 INFO 服务 (类别 compose-ui)
* `新增` 10 种语言的 README, 插件中心说明与更新日志, 由 JSON 源文件统一生成
* `新增` 预览计数器支持增量更新与关闭后的回调清理, 更新被拒绝时保留上一次有效界面
* `依赖` 附加 common-plugin-api.aar 版本 6.8.0 (5307) (MPL 2.0, 哈希锁定)
* `依赖` 附加 Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `依赖` 附加 compose-ui-api.aar V1 (MPL 2.0, 摘要锁定), 共享依赖与宿主对齐
