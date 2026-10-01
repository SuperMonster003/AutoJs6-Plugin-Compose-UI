<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 スクリプトに Jetpack Compose と Material 3 による UI 描画をもたらすプラグイン</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 言語

******

このドキュメントは以下の言語で提供されています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### はじめに

******

Compose UI は AutoJs6 の UI 描画プラグインです. スクリプトはホスト組み込みの `compose` グローバルオブジェクトで UI を宣言し, プラグインがホストプロセス内で Jetpack Compose と Material 3 により描画します. `"ui";` モードの Activity コンテンツとフローティングウィンドウに統一された宣言的 UI を提供します.

プラグインは独立した画面を持たず, ランチャーにも表示されません. ホストは INFO サービスでプラグインを検出し, バージョンと互換性情報を読み取った上で, 契約 (`org.autojs.plugin.compose.api`) に従ってホストプロセス内にレンダラーを読み込みます. UI ツリー, 状態, イベントはスクリプト側で記述され, レンダラーは Compose のコンポジションにパッチを適用し, ユーザーイベントをスクリプトへ返すだけです.

******

### 現在の状態

******

現在のバージョンは P0 開発プレビューです. リポジトリにはビルド可能なプラグインの骨格, INFO サービス, Wake Activity による有効化プロトコルが含まれていますが, レンダラーとスクリプト API は未提供のため, インストールしてもスクリプトは `compose` で UI を描画できません. 今後のマイルストーンと進捗はロードマップを参照してください.

******

### 機能

******

プラグインが提供を予定している主要な機能:

- 宣言的 UI: `compose.state` + `compose.mount(render)` が状態変化に応じて自動で再描画し, 長期保持できるノードハンドル (`compose.Text({...})` など) でプロパティや子ノードを直接変更できます
- Material 3 コンポーネントのコアセット: レイアウト (Column / Row / Box / LazyColumn など), テキスト, ボタン, テキストフィールド, スイッチ, スライダー, プログレスインジケーター, カード, ダイアログ
- チェーン式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` は操作順序を保持し, スコープ限定の操作はホスト側で検証されます
- 2 つの表示面: `"ui";` スクリプトの Activity コンテンツ (`compose.mount`) と任意のスクリプトのフローティングウィンドウ (`compose.floaty`)
- プロセス内描画: レンダラーはホストプロセス内で動作し, プロセス間の UI ブリッジがないためイベントと状態更新の遅延が小さい
- 純粋なバイトコード APK: ネイティブライブラリなし, ABI 区分なし, 単一パッケージで全デバイスに対応

******

### 使い方

******

1. AutoJs6 6.8.0 (5308) 以降をインストールします
2. このプラグインの APK をインストールします (開く必要はありません. プラグインにはランチャーエントリがありません)
3. AutoJs6 のプラグインセンターで Compose UI が認識され, 有効になっていることを確認します
4. スクリプトで `compose` グローバルオブジェクトをそのまま使用します (描画機能はバージョン 1.0.0 で提供されます)

******

### クイックスタート

******

以下の例は目標とする API の形を示します (ロードマップ付録 A に基づき, 描画機能の提供前は実行できません):

```js
"ui";

// カウンター (宣言的 render 層)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `${count.value} 回クリックしました`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, '1 を加える'),
]));
```

```js
// フローティング HUD (ノードハンドル層)
let status = compose.Text({ text: '準備中...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, '閉じる'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `進捗 ${i}%` }));
    }
});
```

完全な API リファレンス (コンポーネントカタログ, Modifier 操作, セッションオブジェクト, エラーコード) は AutoJs6 ドキュメントの compose モジュールの章にあります.

******

### 互換性

******

プラグインの動作要件と制限:

- AutoJs6 のバージョン: 6.8.0 (5308) 以降. それより古いホストではプラグインセンターに非互換と表示されます
- Android のバージョン: 7.0 (API 24) 以降
- プロセッサアーキテクチャ: 制限なし (純粋なバイトコード APK, ネイティブライブラリなし)
- Compose のバージョン: プラグインに同梱 (BOM 2026.09.00). ホストの Compose ランタイムには依存しません
- 契約バージョン: 1. ホストとプラグインは契約バージョンを照合し, 不一致の場合は明確なエラーを出して読み込みを拒否します

******

### よくある質問

******

- インストール後にプラグインのアイコンが見つからないのはなぜですか? プラグインには独立した UI もランチャーエントリもありません. AutoJs6 のプラグインセンターで確認してください
- スクリプトの `compose` がまだ使えないのはなぜですか? 現在は P0 開発プレビューで, レンダラーとスクリプト API は今後のマイルストーンで提供されます
- 他の UI プラグインをアンインストールする必要はありますか? ありません. Compose UI は既存の `ui` モジュールや他のプラグインに影響しません
- プラグインの更新後にスクリプトの修正は必要ですか? 契約バージョンが変わらない限り不要です. 契約の更新は更新履歴に明記されます

******

### 権限とセキュリティ

******

プラグインは Android の実行時権限を一切要求せず, ネットワーク, ストレージ, センサーにもアクセスしません.

- コンポーネントの保護: Wake Activity と INFO サービスはいずれも `org.autojs.permission.PLUGIN` 署名権限で保護され, AutoJs6 ホストのみがアクセスできます
- バックグラウンド動作なし: 常駐サービス, ブロードキャストレシーバー, 定期タスクを持たず, ホストに読み込まれていない間はリソースを消費しません
- データ境界: スクリプトデータやユーザーファイルを読み書きせず, UI の状態はホストプロセスのメモリ内にのみ存在します
- バックアップポリシー: アプリのバックアップとデバイス移行は無効化されており, プラグイン自体は移行が必要なデータを保持しません

ホストはレンダラーを読み込む際も自身のスクリプト権限モデルを維持し, プラグインがスクリプトから到達できるシステム機能を広げることはありません.

******

### プラグインインターフェース

******

ホストに公開される識別子:

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

ホストは `org.autojs.plugin.INFO` でプラグインを検出し, `requiresHostVersion` などの機能情報を読み取ります. レンダラーファクトリーのクラス名は `org.autojs.plugin.compose.RENDERER_FACTORY` メタデータで宣言され, ホストはプラグイン APK のパスからクラスローダー (親はホスト) を作成し, ホストプロセス内でインスタンス化します.

******

### ロードマップ

******

マイルストーン, 設計上の決定, 受け入れ基準は 1 つのロードマップにまとめて記録されています:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.0.0

_2026/10/02_

- `ヒント` P0 開発プレビュー: リポジトリの骨格はビルドでき, ホストに認識されますが, レンダラーとスクリプト API はまだ提供されていません
- `ヒント` AutoJs6 6.8.0 (5308) 以降が必要です (正確な最小ビルド番号はホスト側の変更が取り込まれた後に補完されます)
- `新機能` プラグインリポジトリの骨格: プラットフォームバージョンプラグインのビルドチェーン, Jetpack Compose BOM 2026.09.00 の依存関係, Wake Activity による有効化プロトコル, INFO サービス (カテゴリ compose-ui)
- `新機能` JSON ソースから生成される 10 言語の README, プラグインセンターの説明, 更新履歴
- `依存関係` common-plugin-api.aar バージョン 6.8.0 (5307) を追加 (MPL 2.0, ハッシュ固定)
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (Apache 2.0)

##### さらに詳しいリリース履歴は次を参照してください

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

******

### ビルド

******

クローン後は Gradle Wrapper でそのままビルドできます. 必要な Android Gradle Plugin と Kotlin のバージョンは, プラットフォームバージョンプラグインが現在の IDE 環境に合わせて自動的に選択します.

デバッグ APK をビルドする:

```powershell
.\gradlew.bat :app:assembleDebug
```

JVM 単体テストを実行し, 端末上の契約テストをパッケージする:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

リリース APK をビルドする (`sign.properties` と署名鍵が必要):

```powershell
.\gradlew.bat :app:assembleRelease
```

署名を検証し, ダイジェスト付きのリリースファイルを生成する:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

多言語ドキュメントがソースと一致していることを検証する:

```powershell
py .python\generate_markdown.py --check
```

ビルドには JDK 21 以降が必要です. `.readme` または `.changelog` 配下のソースを編集した後は, `py .python\generate_markdown.py` を実行してすべてのドキュメントを再生成してください.

******

### ドキュメントの配置

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

README, プラグインセンターの説明, 更新履歴はすべて `.readme` と `.changelog` 配下の JSON ソースから生成されます. 生成された Markdown ファイルを直接編集しないでください.

******

### ライセンス

******

本プロジェクトは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE) の下で公開されています. サードパーティコンポーネントのライセンス情報は [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md) を参照してください.

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- compose モジュールのドキュメント: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
