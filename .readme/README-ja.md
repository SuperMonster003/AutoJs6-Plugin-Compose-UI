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

Compose UI は AutoJs6 の UI 描画プラグインです. スクリプトはホストが提供する `compose` / `$compose` API で UI を宣言し, プラグインがホストプロセス内で Jetpack Compose と Material 3 により描画します. 現在のプレビューは `"ui";` モードの Activity 内容と非 UI スクリプトのフローティングウィンドウの両方に対応します.

プラグインは独立した画面を持たず, ランチャーにも表示されません. ホストは INFO サービスでプラグインを検出し, バージョンと互換性情報を読み取った上で, 契約 (`org.autojs.plugin.compose.api`) に従ってホストプロセス内にレンダラーを読み込みます. UI ツリー, 状態, イベントはスクリプト側で記述され, レンダラーは Compose のコンポジションにパッチを適用し, ユーザーイベントをスクリプトへ返すだけです.

******

### 現在の状態

******

1.0.0 ローカル開発プレビュー: 対応する AutoJs6 ホストビルドと, インストールして有効化したプラグインが必要です. UI ページ, フローティングウィンドウ, 5 サンプル, API リファレンス, TypeScript 型宣言をローカル連携向けに提供します. 検証済みの互換性と性能の範囲はロードマップに記録しています. 公式インデックスへの登録と正式リリースは行っていません. アイコンは仮の図案で, メンテナーの正式な画像を待っています.

******

### 機能

******

現在の開発プレビューの主な機能:

- 宣言的 UI: `compose.state` + `compose.mount(render)` が状態変化に応じて自動で再描画し, 長期保持できるノードハンドル (`compose.Text({...})` など) でプロパティや子ノードを直接変更できます
- Material 3 コア: 29 個のノードファクトリーでレイアウト, テキスト, アイコン, 画像, ボタン, 入力, 選択, 遅延リスト, ダイアログ, 進捗を提供; Snackbar はセッションのコマンドであり compose.Snackbar ファクトリーではありません
- チェーン式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` は操作順序を保持し, スコープ限定の操作はホスト側で検証されます
- 2 種類の表示先: `"ui";` スクリプトの Activity 内容は `compose.mount` または呼び出し可能な `compose` / `$compose`, 非 UI スクリプトを含むフローティング表示は `compose.floaty` の raw またはサイズ変更可能なウィンドウを使用
- 同一プロセス内での描画: ホスト内で UI 更新を適用し, イベントをスクリプトの所有スレッドへキューで渡します; ワーカーは compose.post で更新を要求します
- 1 個の APK に arm64-v8a / armeabi-v7a / x86_64 / x86 を同梱し, プラグイン独自のネイティブコードはありません; AndroidX graphics-path 補助ライブラリを含み, Android, ホスト, プラグインの互換条件が適用されます
- ネイティブのテキスト編集で選択範囲と IME の変換状態を保持し, フォーカスと明示的編集に対応, 新しい入力を上書きする遅延編集を拒否; スイッチとスライダーの状態はスクリプトが制御
- 統合時の保護: プラグインが未導入または非互換なら可用性確認は利用不可を返し, エラーは `ComposeError` で報告. セッション終了やスクリプト停止で所有ウィンドウとコールバックを解放
- カウンター, フォーム検証, 安定したキーを持つ 1000 項目のリスト, 非 UI フローティング HUD, テーマの実行可能な 5 サンプルを前提条件と一覧付きで同梱し, 対応ホストの Compose UI サンプル分類にも同期

******

### 使い方

******

1. compose スクリプト API を含む対応ローカル AutoJs6 ビルドをインストールします (最低 6.8.0 / 5316)
2. このプラグインの APK をインストールします (開く必要はありません. プラグインにはランチャーエントリがありません)
3. AutoJs6 のプラグインセンターで Compose UI が認識され, 有効になっていることを確認します
4. スクリプトで `compose` または `$compose` を使用します. Activity 内容は `compose.mount`, フローティング表示はホストに重ね合わせ表示権限を付与してから `compose.floaty` を使用します

******

### クイックスタート

******

以下のカウンターとフローティング HUD は対応するローカルプレビューホストで実行できます. HUD の実行前にホストへ他のアプリの上に表示する権限を付与してください:

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
let worker = null;
let status = compose.Text({ text: '準備中...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, '閉じる'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `進捗 ${i}%`;
        });
    }
});
```

実行可能な 5 スクリプトを assets/examples/index.json に一覧化し, 対応ホストの Compose UI サンプル分類にも同期しています. 各冒頭にモードと権限を記載しています. ノード, 修飾チェーン, テーマ, セッション, ウィンドウの詳細は同時提供のローカル API 文書と TypeScript/エディター宣言を参照してください; オンラインサイトにはローカル変更が未反映の場合があります.

******

### 互換性

******

プラグインの動作要件と制限:

- AutoJs6 の対応最小バージョン: 6.8.0 (5316) 以降. それより古いホストではプラグインセンターに非互換と表示されます
- Android のバージョン: 7.0 (API 24) 以降
- プロセッサアーキテクチャ: arm64-v8a / armeabi-v7a / x86_64 / x86 (単一の APK に 4 つすべてを内蔵, アーキテクチャ別の選択は不要)
- Compose のバージョン: プラグインに同梱 (BOM 2026.09.00). ホストの Compose ランタイムには依存しません
- 契約バージョン: 1. ホストとプラグインは契約バージョンを照合し, 不一致の場合は明確なエラーを出して読み込みを拒否します
- パッケージ化アプリも互換性のある Compose UI プラグインの別途インストールが必要で, 有効化/認可はそのアプリに属します; 互換判定は内蔵 AutoJs6 ランタイムを使い, アプリ自身の versionCode は使いません

******

### よくある質問

******

- インストール後にプラグインのアイコンが見つからないのはなぜですか? プラグインには独立した UI もランチャーエントリもありません. AutoJs6 のプラグインセンターで確認してください
- `compose` が見つからないのはなぜですか? グローバルオブジェクトは対応するローカルホストビルドが提供します. プラグイン APK のインストールだけでは追加されません
- 他の UI プラグインをアンインストールする必要はありますか? ありません. Compose UI は既存の `ui` モジュールや他のプラグインに影響しません
- プラグイン変更時はどうなりますか? 更新, アンインストール, 無効化で既存セッションを閉じて対応エラーを通知します; 互換性があり有効なプラグインでは再マウントできます
- フローティング表示に必要な条件は? ホストに重ね合わせ表示権限を付与し, 文字入力前に `window.requestFocus()` を呼び出します. HyperOS でウィンドウが見えない場合はデスクトップに戻ってください. 権限不足は PERMISSION_REQUIRED を返し, 許可画面を自動表示しません
- 任意の Compose 関数や JSX/TSX を利用できますか? できません. 文書化されたノードファクトリーとコマンドを使います; Kotlin のコンパイルや任意の Composable 関数の公開は行いません
- 回転で状態を失いますか? 現在のホストは通常の向き変更を処理し, スクリプトエンジンを維持します. 実際の Activity 再生成や破棄はエンジンとセッションを閉じ, 業務状態を自動復元しません
- セレクターでどう検索しますか? testTag はパッケージ接頭辞なしの ID として公開されます. id/testTag と desc/contentDescription は別の情報です; Button の文字が子ノードの場合は parent() をたどってクリック可能な祖先を探します

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
minimum host build: 5316 (6.8.0)
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

_2026/10/03_

- `ヒント` 1.0.0 ローカル開発プレビュー: 対応する AutoJs6 ホストビルドと, インストールして有効化したプラグインが必要です. UI ページ, フローティングウィンドウ, 5 サンプル, API リファレンス, TypeScript 型宣言をローカル連携向けに提供します. 検証済みの互換性と性能の範囲はロードマップに記録しています. 公式インデックスへの登録と正式リリースは行っていません. アイコンは仮の図案で, メンテナーの正式な画像を待っています
- `ヒント` AutoJs6 6.8.0 (5316) 以降が必要です
- `ヒント` パッケージ化アプリも互換性のある Compose UI プラグインの別途インストールが必要で, 有効化/認可はそのアプリに属します; 互換判定は内蔵 AutoJs6 ランタイムを使い, アプリ自身の versionCode は使いません
- `ヒント` ライト/ダークの 2 種類の mipmap を維持; 現在の図案は仮で, メンテナー提供の正式な白黒画像で置き換える予定
- `新機能` 呼び出し可能な compose / $compose, 保持できるノードハンドル, リアクティブな state/render/ref, 一括更新, キューへの投稿, テーマ制御
- `新機能` Material 3 コア: 29 個のノードファクトリーでレイアウト, テキスト, アイコン, 画像, ボタン, 入力, 選択, 遅延リスト, ダイアログ, 進捗を提供; Snackbar はセッションのコマンドであり compose.Snackbar ファクトリーではありません
- `新機能` UI スクリプトで Activity 内容をマウントし, compose.floaty は非 UI スクリプトにも raw / サイズ変更可能なウィンドウ, ピクセル位置と寸法, タッチ/フォーカス制御と所有リソースの解放を提供
- `新機能` 全 20 種類の Modifier 操作で宣言順序を保持し, レイアウトスコープの検証, スクロール, アクセシビリティラベルに対応
- `新機能` Material 3 テーマはシード色, ライトとダークモード, Android 12+ のシステム動的色, フォント, 文字サイズ倍率に対応
- `新機能` ネイティブのテキスト編集で選択範囲と IME の変換状態を保持し, フォーカスと明示的編集に対応, 新しい入力を上書きする遅延編集を拒否; スイッチとスライダーの状態はスクリプトが制御
- `新機能` コアアイコンと ImageWrapper/Bitmap, ローカルファイル, ホスト drawable 画像に対応; 呼び出し側が所有する画像をレンダラーが自動で recycle することはありません
- `新機能` カウンター, フォーム検証, 安定したキーを持つ 1000 項目のリスト, 非 UI フローティング HUD, テーマの実行可能な 5 サンプルを前提条件と一覧付きで同梱し, 対応ホストの Compose UI サンプル分類にも同期
- `新機能` 対応 API リファレンスと TypeScript 型宣言, 10 言語の README, プラグインセンター説明, 変更履歴
- `新機能` プラグインセンターでの検出とホスト版, 契約, 認可の検査; 独立画面やランチャー入口はありません
- `修正` Compose UI はレンダリングコールバック中のページやフローティングウィンドウの再マウントを拒否し, 現在のページを維持; プラグイン更新後のページ再マウントにも対応
- `改善` 可用性確認と ComposeError でプラグインの未導入, 無効, 未認証, 非互換, 権限不足, 終了済みセッションを統一報告. ネイティブウィンドウ接続前のキャンセルも解放処理で対応; プラグインの更新, アンインストール, 無効化時は動作中のセッションを閉じて対応するエラーを通知
- `依存関係` common-plugin-api.aar バージョン 6.8.0 (5307) を追加 (MPL 2.0, ハッシュ固定)
- `依存関係` Jetpack Compose BOM 2026.09.00 を追加 (Apache 2.0)
- `依存関係` AutoJs6 6.8.0 (5316) に対応する compose-ui-api.aar V1 を追加 (MPL 2.0, ハッシュ固定), 共有依存関係をホストと整合
- `依存関係` BOM 2026.09.00 で管理される Compose UI Test を追加 (Apache 2.0, テスト専用)
- `依存関係` JaCoCo バージョン 0.8.14 を追加 (任意のテストカバレッジ専用, リリースパッケージには含まれません)

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
