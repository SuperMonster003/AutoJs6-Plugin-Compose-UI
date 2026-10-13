<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>AutoJs6 スクリプトに Jetpack Compose と Material 3 による UI 描画をもたらすプラグイン</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / 言語

******

このドキュメントは以下の言語で提供されています:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- 日本語 [ja] # 現在
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### はじめに

******

3-Block Compose は AutoJs6 の UI 描画プラグインです. スクリプトはホストが提供する `compose` / `$compose` API で UI を宣言し, プラグインがホストプロセス内で Jetpack Compose と Material 3 により描画します. 現在のプレビューは `"ui";` モードの Activity 内容と非 UI スクリプトのフローティングウィンドウの両方に対応します.

プラグインにはビジュアルデザイナーとコンポーネントギャラリーが内蔵されています. ランチャーから開くとデザイナーのホームに入り, スクリプトプロジェクトを作成またはインポートして実際のレンダラー上で画面を編集でき, ギャラリーでは全コンポーネントのプレビューとサンプルスクリプトを閲覧できます. スクリプトの画面は引き続きホストプロセス内で描画されます. スクリプトの画面は引き続きホストプロセス内で描画されます. ホストは INFO サービスでプラグインを検出してバージョンと互換性情報を読み取り, 契約 (`org.autojs.plugin.compose.api`) に従って自身のプロセス内でレンダラーファクトリを生成します.

******

### 現在の状態

******

1.3.0 ローカル開発プレビュー: 3-Block Compose の新しい識別情報を含む AutoJs6 ホストビルド (ホストのコミット f12665ffde 以降) に本プラグインをインストールして有効化する必要があります. AutoJs6 エディターのメニュー "3-Block Compose デザイナーで開く" はホストのコミット db7e6a1734 以降のビルドが必要です. ローカルの付属物として UI ページ, フローティングウィンドウ, ビジュアルデザイナー, 5 つのサンプル, API リファレンス, デザイナーガイドページ, TypeScript 宣言を提供します. 検証済みの互換性と性能の範囲はロードマップに記録しています. 公式インデックスには未登録で, 公式リリースはまだありません. アイコンはメンテナー提供の正式な図案を使用しています.

******

### 機能

******

現在の開発プレビューの主な機能:

- ビジュアルデザイナー: スクリプトファイルがそのままプロジェクトで, 実際のレンダラーが編集中の UI ツリーをそのままプレビューし, アウトライン / コンポーネントパレット / プロパティドロワーで 55 個すべてのコンポーネントを編集でき, 元に戻す / やり直すと下書き復元に対応. 書き出したスクリプトはコピーするか AutoJs6 に直接送って実行できます
- AutoJs6 との連携: プロジェクトは実行のために送信, AutoJs6 のエディターで開く (双方向同期), AutoJs6 のスクリプトフォルダーへの取り込み, コピーまたは書き出しができ, デザイナーは AutoJs6 のエディターやファイルマネージャーから .js ファイルを直接開いて同じファイルに保存できます
- 宣言的 UI: `compose.state` + `compose.mount(render)` が状態変化に応じて自動で再描画し, 長期保持できるノードハンドル (`compose.Text({...})` など) でプロパティや子ノードを直接変更できます
- Material 3 コア: 29 個のノードファクトリーでレイアウト, テキスト, アイコン, 画像, ボタン, 入力, 選択, 遅延リスト, ダイアログ, 進捗を提供; Snackbar はセッションのコマンドであり compose.Snackbar ファクトリーではありません
- チェーン式 Modifier: `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` は操作順序を保持し, スコープ限定の操作はホスト側で検証されます
- 2 種類の表示先: `"ui";` スクリプトの Activity 内容は `compose.mount` または呼び出し可能な `compose` / `$compose`, 非 UI スクリプトを含むフローティング表示は `compose.floaty` の raw またはサイズ変更可能なウィンドウを使用
- 同一プロセス内での描画: ホスト内で UI 更新を適用し, イベントをスクリプトの所有スレッドへキューで渡します; ワーカーは compose.post で更新を要求します
- 1 個の APK に arm64-v8a / armeabi-v7a / x86_64 / x86 を同梱し, プラグイン独自のネイティブコードはありません; AndroidX graphics-path 補助ライブラリを含み, Android, ホスト, プラグインの互換条件が適用されます
- ネイティブのテキスト編集で選択範囲と IME の変換状態を保持し, フォーカスと明示的編集に対応, 新しい入力を上書きする遅延編集を拒否; スイッチとスライダーの状態はスクリプトが制御
- 統合時の保護: プラグインが未導入または非互換なら可用性確認は利用不可を返し, エラーは `ComposeError` で報告. セッション終了やスクリプト停止で所有ウィンドウとコールバックを解放
- カウンター, フォーム検証, 安定したキーを持つ 1000 項目のリスト, 非 UI フローティング HUD, テーマの実行可能な 5 サンプルを前提条件と一覧付きで同梱し, 対応ホストの 3-Block Compose サンプル分類にも同期
- TSX は `<compose.Column>`, `<compose:Text>`, ノードファクトリへの参照, Fragment, スロットとリアクティブなコールバックに対応. 同じツリーで Compose と従来の XML ノードを混在させることはできません
- XML `<compose>` コンテナと compose.attach は UI ページや既存のフローティングウィンドウに独立した Compose セッションを埋め込みます. compose.AndroidView は既存の Android View または同期ファクトリが返す View を表示します
- compose.dialog は更新と終了が可能なセッションを返し, UI スクリプトと通常のスクリプトでダイアログやモーダルボトムシートを表示できます
- Material 3 拡張コンポーネント: ナビゲーションとドロワー, タブ, ボトムシートとメニュー, 日付と時刻の選択, ページングとグリッド, チップ, バッジ, 分割ボタン, フローティングボタン, 検索バー, ツールチップ, プル更新
- コンポーネントギャラリー: ランチャーの入口から全 55 コンポーネントの Material 3 プレビューと実行可能なサンプルスクリプトを表示し, コピーまたは AutoJs6 へ送信できます. 設定ページの言語, ダークモード, テーマカラーは既定で AutoJs6 に追従し, 4 種類のランチャーアイコンを提供します

******

### 使い方

******

1. compose スクリプト API を含む対応ローカル AutoJs6 ビルドをインストールします (最低 6.8.0 / 5322)
2. プラグインの APK をインストールし, ランチャーから 3-Block Compose を開くとデザイナーのホームに入ります. ここでスクリプトプロジェクトを作成するか, 上部バーからコンポーネントギャラリーと設定を開けます
3. AutoJs6 のプラグインセンターで 3-Block Compose が認識され, 有効になっていることを確認します
4. スクリプトで `compose` または `$compose` を使用します. Activity 内容は `compose.mount`, フローティング表示はホストに重ね合わせ表示権限を付与してから `compose.floaty` を使用します
5. 編集画面のメニューには実行, AutoJs6 のエディターで開く, AutoJs6 のスクリプトフォルダーに取り込む, コピー, 書き出しがあります. AutoJs6 のエディターメニュー (対応する更新を含む AutoJs6 が必要) やファイルマネージャーで 3-Block Compose を選ぶと, スクリプトファイルを直接編集できます

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

実行可能な 5 スクリプトを assets/examples/index.json に一覧化し, 対応ホストの 3-Block Compose サンプル分類にも同期しています. 各冒頭にモードと権限を記載しています. ノード, 修飾チェーン, テーマ, セッション, ウィンドウの詳細は同時提供のローカル API 文書と TypeScript/エディター宣言を参照してください; オンラインサイトにはローカル変更が未反映の場合があります.

******

### 互換性

******

プラグインの動作要件と制限:

- AutoJs6 の対応最小バージョン: 6.8.0 (5322) 以降. それより古いホストではプラグインセンターに非互換と表示されます
- Android のバージョン: 7.0 (API 24) 以降
- プロセッサアーキテクチャ: arm64-v8a / armeabi-v7a / x86_64 / x86 (単一の APK に 4 つすべてを内蔵, アーキテクチャ別の選択は不要)
- Compose のバージョン: プラグインに同梱 (BOM 2026.09.00). ホストの Compose ランタイムには依存しません
- 契約バージョン: 2. ホストとプラグインは契約バージョンを照合し, 不一致の場合は明確なエラーを出して読み込みを拒否します
- パッケージ化アプリも互換性のある 3-Block Compose プラグインの別途インストールが必要で, 有効化/認可はそのアプリに属します; 互換判定は内蔵 AutoJs6 ランタイムを使い, アプリ自身の versionCode は使いません
- View ファクトリは描画前にメインスレッドで実行されます. 無効な置換では現在の内容を保持します. 同じ View の二重所有や別の親からの移動は拒否されます. 借用 View のリスナーと呼び出し側のリソース所有権は維持されます
- cancelable=false は戻る操作, 外側のクリック, 下方向のスワイプによる終了を禁止します. 明示的な終了やスクリプト終了時にはダイアログを解放し, 既存のページと他のセッションを維持します
- このビルドは 3-Block Compose 契約 V2 を使用し, AutoJs6 6.8.0 / 5322 が必要です. TSX には TypeScript Engine 0.6.7 が必要です. 新しいホストは旧 V1 レンダラーの既存コンポーネントも使用できます. 拡張コンポーネントには V2 が必要です
- compose.memo は AutoJs6 6.8.0 / 5323 が提供し, 依存値が変わらない render 内の断片を再利用します. このプラグインの更新は不要で, TSX には TypeScript Engine 0.6.8 が必要です
- 1.2.0 以降, コントラクトクラス (org.autojs.plugin.compose.api) は独立プロセス向けにプラグインへ同梱されます. AutoJs6 内ではホストが同じクラスを提供し, プラグイン側のコピーは使われません
- デザイナーはプラグイン自身のプロセスで動作し, AutoJs6 に依存しません. 実行, AutoJs6 エディターで開く, AutoJs6 のスクリプトフォルダーへの取り込みにはインストール済みの AutoJs6 が必要で, エディターメニューの "3-Block Compose デザイナーで開く" にはその入口を含む AutoJs6 ビルドが必要です

******

### よくある質問

******

- ギャラリーのサンプルはどう実行しますか? "AutoJs6 で実行" を押すとスクリプトをインストール済みの AutoJs6 に渡します. ギャラリー自体はプレビューとコードを表示するだけで, スクリプトを実行しません
- デザイナーはどんなスクリプトでも開けますか? 解釈するのはトップレベルの `compose.state` 宣言と唯一の `compose.mount` / `floaty` / `dialog` 呼び出しだけで, それ以外の文はそのまま保持します. ルートが compose のファクトリでない, マウント呼び出しが複数ある, レンダリング関数に return がないなど対応付けできない構造は, 行と列を示して開くのを拒否します
- デザイナーはスクリプトを実行しますか? いいえ. Rhino パーサーで解析し, プラグイン自身のレンダラーでプレビューするだけです. イベントは発火せず, state は初期値を表示します. 実行は AutoJs6 に任せます
- `compose` が見つからないのはなぜですか? グローバルオブジェクトは対応するローカルホストビルドが提供します. プラグイン APK のインストールだけでは追加されません
- 他の UI プラグインをアンインストールする必要はありますか? ありません. 3-Block Compose は既存の `ui` モジュールや他のプラグインに影響しません
- プラグイン変更時はどうなりますか? 更新, アンインストール, 無効化で既存セッションを閉じて対応エラーを通知します; 互換性があり有効なプラグインでは再マウントできます
- フローティング表示に必要な条件は? ホストに重ね合わせ表示権限を付与し, 文字入力前に `window.requestFocus()` を呼び出します. HyperOS でウィンドウが見えない場合はデスクトップに戻ってください. 権限不足は PERMISSION_REQUIRED を返し, 許可画面を自動表示しません
- TSX や任意の Compose 関数は使えますか? 対応するホストと TypeScript Engine で, 文書にある Compose ノードファクトリを TSX から使えます. 任意の Kotlin Composable 関数や独自の TSX コンポーネントには対応していません
- 回転で状態を失いますか? 現在のホストは通常の向き変更を処理し, スクリプトエンジンを維持します. 実際の Activity 再生成や破棄はエンジンとセッションを閉じ, 業務状態を自動復元しません
- セレクターでどう検索しますか? testTag はパッケージ接頭辞なしの ID として公開されます. id/testTag と desc/contentDescription は別の情報です; Button の文字が子ノードの場合は parent() をたどってクリック可能な祖先を探します

******

### 権限とセキュリティ

******

プラグインは Android の実行時権限を一切要求せず, ネットワーク, ストレージ, センサーにもアクセスしません.

- コンポーネントの保護: Wake Activity と INFO サービスは署名権限 `org.autojs.permission.PLUGIN` で保護され, AutoJs6 ホストだけがアクセスできます. デザイナーの編集画面は権限なしの公開コンポーネントですが, ユーザーが渡したスクリプト文書の Uri だけを受け取り, 読み書きもその Uri に限られます
- バックグラウンド動作: 常駐サービスや定期タスクはなく, 自身が更新されたときに一度だけシステムブロードキャストを受け取ってランチャーアイコンのコンポーネントを整えます. ホストに読み込まれず, デザイナーもギャラリーも開いていなければ何も消費しません
- データの境界: レンダラーはスクリプトデータやユーザーファイルを読み書きせず, UI の状態はホストプロセスのメモリーにだけ存在します. デザイナーのプロジェクトと下書きはプラグインの非公開フォルダーに置かれ, システムのファイル選択ツールでのみインポート / エクスポートされるか, ユーザーが渡した Uri を通じて単一のスクリプトファイルを読み書きします. プロジェクトフォルダーは AutoJs6 にプロジェクトを渡すときに限り, プラグイン自身のコンテンツプロバイダーで要求ごとに共有されます. インポート時に選んだファイルへのアクセス権を保持しますが (提供元が許可する場合), そのファイルを読み書きするのはユーザーが "元のファイルに書き戻す" または "元のファイルから再インポート" を選んだときだけです
- バックアップポリシー: アプリのバックアップとデバイス移行は無効化されており, プラグイン自体は移行が必要なデータを保持しません

ホストはレンダラーを読み込む際も自身のスクリプト権限モデルを維持し, プラグインがスクリプトから到達できるシステム機能を広げることはありません.

******

### プラグインインターフェース

******

ホストに公開される識別子:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

ホストは `org.autojs.plugin.INFO` でプラグインを検出し, `requiresHostVersion` などの機能情報を読み取ります. レンダラーファクトリーのクラス名は `org.autojs.plugin.compose.RENDERER_FACTORY` メタデータで宣言され, ホストはプラグイン APK のパスからクラスローダー (親はホスト) を作成し, ホストプロセス内でインスタンス化します.

******

### ロードマップ

******

マイルストーン, 設計上の決定, 受け入れ基準は 1 つのロードマップにまとめて記録されています:

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### リリース履歴

******

#### v1.3.0

_2026/10/13_

- `新機能` デザイナーで "スクリプトをインポート" から作成したプロジェクトは, 編集画面のメニューの "元のファイルに書き戻す" と "元のファイルから再インポート" を使えます. インポート時に元のファイルへのアクセス権を保持し (提供元が許可する場合), 前回の同期以降にもう一方が変更されていれば先に確認し, 元のファイルを開けなくなった場合は選び直して関連付けを戻せます. プロジェクトの保存で元のファイルへ自動的に書き戻すことはありません
- `新機能` デザイナーのキャンバスで Image の画像をプレビューできるようになりました. '@drawable/name' はインストール済みの AutoJs6 から取得し, 相対パスは編集画面のメニューの "画像フォルダー" で選んだフォルダーから探します (読み取り専用のアクセス権のみ保持). URL と絶対パスは引き続き代替表示となり, 理由は問題一覧に表示されます
- `修正` デザイナーのキャンバスで開いた状態のポップアップ系コンポーネント (AlertDialog, ModalBottomSheet, DropdownMenu, Tooltip) をカードとして表示する際, 内容に余白と間隔が付かない問題
- `改善` デザイナーのキャンバスでノードを長押ししてドラッグすると, 対象コンテナの並び方向 (縦, 横, グリッド) に沿って挿入線を表示し, 指を離した位置にノードを配置するようになりました. 以前は常にコンテナの末尾に追加されていました
- `改善` デザイナーのキャンバス上の AndroidView は, サイズ修飾 (h, fillMaxWidth など) を保った淡い色の代替領域として表示され, その中に view 式のソースを表示するようになりました
- `改善` デザイナーのプロパティドロワーの修飾チェーンは, 各項目の左にあるハンドルをドラッグして並べ替えられるようになりました. 指を離すまで挿入線が表示されます. 上へ / 下への移動ボタンも引き続き使えます

#### v1.2.0

_2026/10/13_

- `ヒント` プラグイン名を Compose UI から 3-Block Compose に変更しました: アプリケーション ID は io.github.supermonster003.autojs6.plugin.compose.ui から io.github.supermonster003.autojs6.plugin.three.block.compose になります. Android は新しいアプリとして扱い, 旧パッケージは併存できます. 設定と権限は引き継がれないため, 新パッケージをプラグインセンターで再度有効化し, 権限を付与してください
- `ヒント` 新しいプラグイン識別情報を含む AutoJs6 6.8.0 ホストビルド (ホストのコミット f12665ffde 以降) が必要です. それ以前のホストは旧パッケージ名でプラグインを検出するため, このバージョンを認識しません
- `ヒント` デザイナーの使い方はドキュメントサイトの "3-Block Compose デザイナー" ページを参照してください. AutoJs6 エディターのメニュー "3-Block Compose デザイナーで開く" にはホストのコミット db7e6a1734 以降の AutoJs6 6.8.0 ビルドが必要で, 他の送り先はインストール済みの AutoJs6 があれば使えます
- `新機能` ビジュアルデザイナー: ランチャーからデザイナーのホームを開き, 非公開ストレージ内のスクリプトプロジェクトを作成, インポート, オープンできます. 編集画面では実際のレンダラーによるプレビューキャンバス, アウトラインツリー, コンポーネントパレット, プロパティドロワーを提供し, タップで選択, 長押しドラッグで親を変更, アウトライン内の並べ替え, 元に戻す / やり直す, 下書きの自動保存と復元, 表示オプションの編集, 契約カタログに基づく検証レポート, スクリプトのコピーまたは AutoJs6 への送信に対応します. 横向きの画面やタブレットではキャンバスとパネルが左右に並びます. コンポーネントギャラリーと設定はデザイナーのホームから開くようになりました
- `新機能` デザイナーと AutoJs6 の連携: 編集画面のメニューから現在のスクリプトを AutoJs6 のエディターで開き (AutoJs6 で保存するとデザイナーが再読み込みします), AutoJs6 のスクリプトフォルダーに取り込み, または任意の場所に書き出せます. デザイナーは AutoJs6 のエディターやファイルマネージャーから .js ファイルを直接開いて同じファイルに保存でき, 書き込み権限がない場合は新しいプロジェクトとして保存します. プロジェクトファイルはプラグイン自身のコンテンツプロバイダーで要求ごとに権限を付与して共有し, 他のフォルダーは公開しません
- `改善` アプリ名, プラグイン ID (three-block-compose), パッケージ名とコンポーネント名, リリース成果物, ドキュメント, GitHub リポジトリ (AutoJs6-Plugin-Three-Block-Compose) を名称変更に合わせて更新しました. スクリプト API の compose, コントラクトのバージョン, 描画の動作は変わりません
- `改善` ランチャー, プラグインセンター, システムのアプリ情報のアイコンをメンテナー提供の正式な図案に変更しました. ライトとダークの図案は同じ形状を共有します
- `改善` 同梱サンプルとホスト側のサンプル分類を 3-Block Compose に変更しました
- `改善` コントラクト AAR compose-ui-api をプラグインに同梱して完全に保持するようにしました (以前はコンパイル時のみ参照). プラグイン自身のプロセス向けで, AutoJs6 内では引き続きホストの同一コントラクトクラスが優先されるため, 描画の動作とコントラクトのバージョンは変わりません
- `依存関係` Rhino パーサーの軽量 jar (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, AutoJs6 ホストと同じビルド) を追加しました. ビジュアルデザイナーがスクリプトを解析するためだけに使い, 実行はしません. release ビルドには参照されるパーサークラスだけが残ります

#### v1.1.0

_2026/10/08_

- `ヒント` 1.1.0 ローカル開発プレビュー: 対応する AutoJs6 ホストビルドと, インストールして有効化したプラグインが必要です. UI ページ, フローティングウィンドウ, 5 サンプル, API リファレンス, TypeScript 型宣言をローカル連携向けに提供します. 検証済みの互換性と性能の範囲はロードマップに記録しています. 公式インデックスへの登録と正式リリースは行っていません. アイコンは仮の図案で, メンテナーの正式な画像を待っています
- `ヒント` このビルドは Compose UI 契約 V2 を使用し, AutoJs6 6.8.0 / 5322 が必要です. TSX には TypeScript Engine 0.6.7 が必要です. 新しいホストは旧 V1 レンダラーの既存コンポーネントも使用できます. 拡張コンポーネントには V2 が必要です
- `ヒント` compose.memo は AutoJs6 6.8.0 / 5323 が提供し, 依存値が変わらない render 内の断片を再利用します. このプラグインの更新は不要で, TSX には TypeScript Engine 0.6.8 が必要です
- `ヒント` ギャラリーと設定ページはプラグイン自身のプロセスで動作し, ホスト内の描画方式と最低ホストバージョンは変わりません. サンプルスクリプトの実行には, このプラグインをインストールして有効にした AutoJs6 が必要です
- `新機能` TSX は `<compose.Column>`, `<compose:Text>`, ノードファクトリへの参照, Fragment, スロットとリアクティブなコールバックに対応. 同じツリーで Compose と従来の XML ノードを混在させることはできません
- `新機能` XML `<compose>` コンテナと compose.attach は UI ページや既存のフローティングウィンドウに独立した Compose セッションを埋め込みます. compose.AndroidView は既存の Android View または同期ファクトリが返す View を表示します
- `新機能` compose.dialog は更新と終了が可能なセッションを返し, UI スクリプトと通常のスクリプトでダイアログやモーダルボトムシートを表示できます
- `新機能` Material 3 拡張コンポーネント: ナビゲーションとドロワー, タブ, ボトムシートとメニュー, 日付と時刻の選択, ページングとグリッド, チップ, バッジ, 分割ボタン, フローティングボタン, 検索バー, ツールチップ, プル更新
- `新機能` コンポーネントギャラリー: ランチャーから開き, 全 55 コンポーネントの Material 3 プレビューとサンプルスクリプトを閲覧し, クリップボードにコピーまたはインストール済みの AutoJs6 に送って実行できます
- `新機能` 設定ページ: 言語, ダークモード, テーマカラーは既定で AutoJs6 に追従し, 個別に設定できます. ランチャーアイコンはアダプティブ (ライト / ダーク / 自動) と透明背景の 4 種類から選べます
- `修正` Icon Studio でランチャーアイコンの背景を変更した後にビルドできなくなる問題を修正
- `改善` Android のアプリ情報アイコンに Icon Studio と共通の図案と明暗の背景色を使用し, プラグインセンターの透明な図案と既存のランチャー設定を維持
- `改善` View ファクトリは描画前にメインスレッドで実行されます. 無効な置換では現在の内容を保持します. 同じ View の二重所有や別の親からの移動は拒否されます. 借用 View のリスナーと呼び出し側のリソース所有権は維持されます
- `改善` cancelable=false は戻る操作, 外側のクリック, 下方向のスワイプによる終了を禁止します. 明示的な終了やスクリプト終了時にはダイアログを解放し, 既存のページと他のセッションを維持します
- `改善` プラグインセンターのアイコンとシステムのアプリ情報アイコンは, 他の独立プラグインと同じライト #FAFAFA / ダーク #212121 の背景を使用します
- `依存関係` 凍結 V1 を維持し, 任意の AndroidView 相互運用拡張を持つ compose-ui-api.aar に更新
- `依存関係` 既存の V1 と AndroidView 契約を維持したまま, compose-ui-api.aar に任意のダイアログ機能を追加
- `依存関係` 既存のノードモデルと V1 コンポーネントの意味を維持し, compose-ui-api.aar に V2 カタログを追加
- `依存関係` material-color-utilities 4.1.1 (MIT) を追加. 他の独立プラグインと同じテーマカラー導出に使用します
- `依存関係` AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines などのホスト固定バージョンのコピーを追加. プラグイン自身のプロセスのギャラリーが使用し, ホスト内では引き続きホストのコピーが優先されます

##### さらに詳しいリリース履歴は次を参照してください

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-ja.md)

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

本プロジェクトは [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE) の下で公開されています. サードパーティコンポーネントのライセンス情報は [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md) を参照してください.

******

### 関連リンク

******

- AutoJs6 プロジェクト: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 ドキュメント: https://docs.autojs6.com
- compose モジュールのドキュメント: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- サードパーティ通知: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
