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

1.7.0 は現在の公開リリースです (GitHub Release, 公式プラグインインデックスに登録済み). 3-Block Compose の新しい識別情報を含む AutoJs6 6.8.0 (5322) 以降のビルド (ホストのコミット f12665ffde 以降) が必要です. 公開時点で AutoJs6 6.8.0 は正式リリース前のため, このコミットを含む AutoJs6 ビルドを使用してください. AutoJs6 エディターのメニュー "3-Block Compose デザイナーで開く" はホストのコミット db7e6a1734 以降のビルドが必要で, デザイナーの TSX プロジェクトには TypeScript Engine 0.7.0 が必要で, UI (XML) プロジェクトのレイアウトプレビューにはプレビューサービスを含むビルド (ホストのコミット 306a8f7834 以降) が必要です. 本プラグインは UI ページ, フローティングウィンドウ, ビジュアルデザイナー (Compose と UI プロジェクト), 5 つのサンプル, API リファレンス, デザイナーガイドページ, TypeScript 宣言を提供します. 検証済みの互換性と性能の範囲はロードマップに記録しています. アイコンはメンテナー提供の正式な図案を使用しています. 1.7.0 以降, UI (XML) プロジェクトのインタラクティブプレビューでは AutoJs6 がスクリプト全体を実際に実行します. プレビュー契約 v2 を含む AutoJs6 ビルド (ホストのコミット 4c332c2053 以降) と Android 13 以降が必要です.

******

### 機能

******

現在の開発プレビューの主な機能:

- ビジュアルデザイナー: スクリプトファイルがそのままプロジェクトで, 実際のレンダラーが編集中の UI ツリーをそのままプレビューし, アウトライン / コンポーネントパレット / プロパティドロワーで 55 個すべてのコンポーネントを編集でき, 元に戻す / やり直すと下書き復元に対応. 書き出したスクリプトはコピーするか AutoJs6 に直接送って実行できます. キャンバスへのドロップはポインターの位置に入り, 開いたポップアップはキャンバス内に描かれ, Image は @drawable とプロジェクトの画像フォルダーの画像を表示でき, 状態パネルで state のプレビュー値を一時的に変えられ, 操作プレビューでは JavaScript プロジェクトのスクリプトを制限付きのサンドボックスで実行し (Android 8.0 以降), タップや入力がスクリプトのとおりにキャンバスを更新し, コンソールに出力とエラーを表示します. プロジェクトは JavaScript と TSX から選べ, TSX プロジェクトは TypeScript Engine 0.7.0 以降で解析し, 正規化した TSX として保存します
- AutoJs6 との連携: プロジェクトは実行のために送信, AutoJs6 のエディターで開く (双方向同期), AutoJs6 のスクリプトフォルダーへの取り込み, コピーまたは書き出しができ, デザイナーは AutoJs6 のエディターやファイルマネージャーから .js / .tsx ファイルを直接開いて同じファイルに保存できます. インポートしたプロジェクトは編集画面のメニューからインポート元へ書き戻したり, インポート元から再インポートしたりできます
- UI (XML) プロジェクト: デザイナーは従来の "ui" モードスクリプト (E4X XML レイアウト) も開いてその場で編集でき, 触れていないバイト (空白, コメント, 属性の順序, 引用符, {式}) はそのまま保たれます. キャンバスは AutoJs6 のプレビューサービスが ui モードのレイアウトインフレーターで描画し (Android 11 以降は実際のビューを埋め込み, それ未満はビットマップのスナップショット), 属性ドロワーはドキュメントサイトのタグと属性カタログに従い, パレットは 48 個のタグを列挙し, 要素に `ui.<id>.on` ハンドラーのスタブを追加でき, バインディングパネルで `{{ }}` の値を一時的に上書きできます
- UI (XML) プロジェクトのインタラクティブプレビュー (1.7.0): キャンバス上の "操作プレビュー" スイッチをオンにすると, AutoJs6 がスクリプト全体を通常の ui モード実行として実行し, キャンバスにスクリプトの ui.layout が表示されてタップやタッチを直接受け取り, イベントハンドラー, `{{ }}` バインディングとその他のモジュールはスクリプトの実際のロジックで動作し, コンソールタブに出力, エラー, 終了理由が表示されます. 編集後約 0.4 秒で再実行し, スイッチをオフにするかデザイナーを離れると停止し, ドキュメントとファイルは変わりません
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
- デザイナーの TSX プロジェクトには TypeScript Engine 0.7.0 以降 (構文木解析サービスを提供) が必要で, 実行時は AutoJs6 が TypeScript Engine でコンパイルします. 未インストールまたは古いバージョンの場合, TSX プロジェクトは読み取り専用のソースとして開き, 閲覧とコピーができます
- デザイナーの操作プレビューでスクリプトを実行するには Android 8.0 (API 26) 以降が必要です (サンドボックスの JavaScript Date が java.time を使うため). それより古い Android では JavaScript プロジェクトも TSX プロジェクトと同じ方法でプレビューされます
- UI (XML) プロジェクトのレイアウトプレビューにはプレビューサービスを含む AutoJs6 6.8.0 ビルド (ホストのコミット 306a8f7834 以降. プラグインはバージョン番号ではなくサービスの有無で判断します) が必要です. Android 11 (API 30) 以降ではキャンバスに AutoJs6 が描画した実際のビューを埋め込み, それより古いバージョンではビットマップのスナップショットを表示します. サービスがなくても UI プロジェクトは編集でき, キャンバスに理由が表示されます
- UI (XML) プロジェクトのインタラクティブプレビューには, プレビュー契約 v2 を含む AutoJs6 6.8.0 ビルド (ホストのコミット 4c332c2053 以降) と Android 13 (API 33) 以降が必要です. Android 13 - 14 ではキャンバス内の入力欄はキーボード入力を受け取れず, Android 15 以降で受け取れます. 条件を満たさない場合はスイッチが表示されず, キャンバス上の行に理由が表示され, "AutoJs6 で実行" を使えます. 契約 v1 だけの AutoJs6 ビルドでもレイアウトプレビューは利用できます

******

### よくある質問

******

- ギャラリーのサンプルはどう実行しますか? "AutoJs6 で実行" を押すとスクリプトをインストール済みの AutoJs6 に渡します. ギャラリー自体はプレビューとコードを表示するだけで, スクリプトを実行しません
- デザイナーはどんなスクリプトでも開けますか? 解釈するのはトップレベルの `compose.state` 宣言と唯一の `compose.mount` / `floaty` / `dialog` 呼び出しだけで, それ以外の文はそのまま保持します. ルートが compose のファクトリでない, マウント呼び出しが複数ある, レンダリング関数に return がないなど対応付けできない構造は, 行と列を示して開くのを拒否します. TSX プロジェクトでは `compose.X` / `compose:X` タグが編集できるノードになり, ほかのタグと `{}` 式は元のまま保持し, XML タグとスプレッド属性は拒否します
- デザイナーはスクリプトを実行しますか? 操作プレビューの中だけです. 編集中は JavaScript を Rhino パーサー, TSX を TypeScript Engine の解析サービスで解析し, プラグイン自身のレンダラーでプレビューするだけで, イベントハンドラーは実行されず, state は初期値か状態パネルで設定したプレビュー値を表示します. 操作プレビューをオンにすると, JavaScript プロジェクトのスクリプトがプラグイン内の制限付きサンドボックスで実行されます (Android 8.0 以降). 使えるのは compose, console, toast とタイマーだけで, Java / Android のクラス, ファイル, ネットワーク, ほかの AutoJs6 モジュールには触れられず, 1 回の実行が約 3 秒を超えると停止され, 結果はキャンバスに表示されるだけでプロジェクトは変わりません. TSX プロジェクト (および Android 8.0 未満のデバイス) ではスクリプトを実行せず, state にバインドされたコントロールはプレビュー値だけを変えます. 実際の実行は AutoJs6 に任せます. UI (XML) プロジェクトのキャンバスは AutoJs6 のプレビューサービスがレイアウトだけを行い (ビューを生成して描画), プロジェクトのスクリプトは実行しません. `{{ }}` の値はスクリプトのトップレベルのリテラル宣言 (サンドボックスで評価) かバインディングパネルの上書きから来ます. 1.7.0 以降, UI (XML) プロジェクトの操作プレビューは異なります. スイッチをオンにすると AutoJs6 が "AutoJs6 で実行" と同じようにスクリプト全体を実際に実行し (サンドボックスの制限はなく, 副作用は実際に発生し, 各プロジェクトで初回実行前に確認します), ui.layout の内容だけがキャンバスに表示されます. 結果はこれまでどおりプロジェクトには書き込まれません
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
- スクリプトのサンドボックス: デザイナーの操作プレビューは JavaScript プロジェクトのスクリプトをプラグインのプロセス内で Rhino インタープリターにより実行します. 使えるのは compose, console, toast とタイマーだけで, Java / Android のクラス, ファイル, ネットワーク, ほかの AutoJs6 モジュールには触れられず, 実行ごとに命令数と時間の上限があり, 結果はキャンバスにだけ使われプロジェクトには書き込まれません
- バックアップポリシー: アプリのバックアップとデバイス移行は無効化されており, プラグイン自体は移行が必要なデータを保持しません
- レイアウトプレビュー: UI (XML) プロジェクトのキャンバスはインストール済みの AutoJs6 が描画します. プラグインは XML 片とバインディング値だけを AutoJs6 のプレビューサービス (PLUGIN 署名権限で保護) に渡し, AutoJs6 は ui モードのレイアウトインフレーターでビューを生成して描き返すだけで, プロジェクトのスクリプトは実行せず, プラグインのプロジェクトファイルにも触れられません. このプレビューの実行は AutoJs6 の実行中リストに表示され, いつでも停止できます
- インタラクティブプレビュー (UI プロジェクト): スイッチをオンにすると, プラグインはスクリプトのテキストを AutoJs6 のプレビューサービスに渡し, AutoJs6 はそれを通常の ui モードスクリプトとして実行します ("AutoJs6 で実行" と同じエンジンとスクリプト権限で, 副作用は実際に発生します). キャンバスには ui.layout の内容だけが表示され, コンソール出力が返されます. 各プロジェクトで初回実行前に確認が必要で, 実行は AutoJs6 の実行中リストに表示され, スイッチをオフにするか, デザイナーを離れるか, AutoJs6 から停止すると終了します. スクリプトはプラグインのプロセスでは実行されず, プラグインがスクリプトの権限を広げることもありません

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

#### v1.7.0

_2026/10/15_

- `ヒント` UI (XML) プロジェクトのインタラクティブプレビューは, AutoJs6 がスクリプト全体を実際に実行するようになりました (プレビュー契約 v2 を含む AutoJs6 6.8.0 ビルド, ホストのコミット 4c332c2053 以降, および Android 13 以降が必要). スクリプトの副作用は AutoJs6 で実行した場合と同じで, 各プロジェクトで初めてスイッチをオンにするときに確認します. 条件を満たさない場合はキャンバス上の行に理由を表示し, "AutoJs6 で実行" を使えます
- `新機能` UI (XML) プロジェクトのインタラクティブプレビュー: キャンバス上の "操作プレビュー" スイッチをオンにすると, AutoJs6 がスクリプト全体を ui モード実行として実行し, キャンバスにスクリプトの ui.layout が表示されてタップやタッチを直接受け取り, イベントハンドラー, {{ }} バインディング, タイマーとその他のモジュールはスクリプトの実際のロジックで動作します. 編集後約 0.4 秒で再実行し, スイッチをオフにするか, デザイナーがバックグラウンドに移るか, 編集ページを閉じると停止してキャンバスはレイアウトプレビューに戻ります. ドキュメント, プロジェクトファイル, 下書きは変更されません. Android 13 - 14 ではキャンバス内の入力欄はキーボード入力を受け取れず, Android 15 以降で受け取れます
- `新機能` UI プロジェクトの "コンソール" タブは, インタラクティブプレビュー中に AutoJs6 が実行するスクリプトの出力 (レベル別に着色), 実行状態, 終了理由を表示します. スクリプトがエラーになるとキャンバス上にエラー (行番号付き) が表示され, タップでコンソールが開きます. 実行は AutoJs6 の実行中リストにプロジェクトのファイル名で表示され, そこから停止することもできます
- `修正` デザイナーで UI プロジェクトを開くと, まれに "AutoJs6 のプレビューサービスは現在利用できません (at most 2 sessions per client)" と表示される問題: レイアウトが安定する前にキャンバスのサイズが 2 回変わると, プラグインが AutoJs6 にプレビューセッションを重複して作成し, クライアントあたりのセッション上限に達していました. 現在は同時に 1 つのセッションだけを作成し, その間に変わったサイズや外観は後から反映します
- `依存関係` 契約成果物 ui-preview-api.aar を ui プレビュー契約 v2 に更新 (v1 の後にスクリプトの実行と停止のインターフェースとコンソール / エラー / 終了のコールバックを追加. v1 だけの AutoJs6 ビルドでもレイアウトプレビューは利用できます)

#### v1.6.0

_2026/10/15_

- `ヒント` デザイナーに UI (XML) プロジェクトが加わりました: 従来の "ui" モードスクリプト (E4X XML レイアウト) を開いて編集でき, キャンバスは AutoJs6 が描画します. レイアウトプレビューにはプレビューサービスを含む AutoJs6 6.8.0 ビルド (ホストのコミット 306a8f7834 以降) が必要です. Android 11 (API 30) 以降ではキャンバスに AutoJs6 が描画した実際のビューを埋め込み, それより古いバージョンではビットマップのスナップショットを表示します. サービスがなくても編集でき, キャンバスに理由が表示されます
- `新機能` デザイナーが UI (XML) プロジェクトに対応: プロジェクトの新規作成で "UI (XML)" を選ぶか, "ui" モードスクリプトをインポートするか AutoJs6 エディター / ファイルマネージャーから開くと UI プロジェクトとして読み込まれます. ホームの一覧には各プロジェクトに JS / TSX / UI の種別が表示されます. スクリプト内の XML レイアウトは位置付きの構文木として保持され, 触れていない部分 (空白, コメント, 属性の順序, 引用符, {式}) はバイト単位でそのまま保たれ, 編集した開始タグ, テキスト, 新しく挿入した要素だけが書き換えられます. スクリプト内のほかの XML リテラルもキャンバスに切り替えて編集できます
- `新機能` UI プロジェクトの編集ページ: アウトライン (折りたたみ, 上下移動, 削除, 長押しドラッグ, レイアウトが複数あるときの切り替え), 属性ドロワー (タグ, id, テキスト内容, 設定済みの属性と View クラスごとにまとめたカタログ属性, テキスト / `{{ }}` バインディング / {式} の三状態, 値なしの属性, 任意の属性名), コンポーネントパレット (ドキュメントの 48 個のタグをカテゴリー別に既定属性付きで列挙), バインディングパネル (`{{ }}` 名の評価結果と, キャンバスだけに効く一時的な上書き), 問題一覧 (ドキュメントサイトのタグと属性カタログで検証) とコンソール (AutoJs6 がレイアウトの描画中に報告した内容). 保存, 下書きの復元, ソースの同期, 実行, AutoJs6 エディターで開く, AutoJs6 のスクリプトディレクトリへのインポート, コピーとエクスポートは UI プロジェクトでも同様に使えます
- `新機能` UI プロジェクトのキャンバスは AutoJs6 が描画します (レイアウトプレビュー): プラグインは XML 片とバインディング値を AutoJs6 のプレビューサービスに渡し, AutoJs6 が ui モードのレイアウトインフレーターで実際のビューを生成してキャンバスに描き返します. プロジェクトのスクリプトは実行しません. キャンバス上の選択とドラッグは通常どおり動作し, 描画に失敗したときは原因の要素を指し示します. プレビューの実行は AutoJs6 の実行中リストに "3-Block Compose preview" として表示され, そこから停止できます
- `新機能` UI プロジェクトの属性ドロワーで選択中の要素にイベントを追加できます: ui.layout の呼び出しの後に `ui.<id>.on('イベント', ...)` ハンドラーのスタブを追記し (id のない要素には自動で id を付与), 本体はスクリプトエディターで書きます. スクリプトが登録済みのイベントは "スクリプトで処理済み" と表示されます
- `依存関係` 契約成果物 ui-preview-api.aar (AutoJs6 ui レイアウトプレビュー契約 v1) を追加. デザイナーはこれを通じて AutoJs6 に UI プロジェクトのキャンバスの描画を依頼します

#### v1.5.1

_2026/10/15_

- `修正` デザイナーのキャンバスに新しいテキストフィールドが現れたとき, プラグインが強制終了することがありました. 例えば, コンポーネントパレットからテキストフィールドを挿入したとき, 元に戻す / やり直すでテキストフィールドが再び現れたとき, 操作プレビューでスクリプトがテキストフィールドを表示したときです (1.4.0 から). こうしたテキストフィールドは正常に表示されるようになりました
- `修正` デザイナーのプロパティパネルでキー見出しの翻訳を補完

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
