# 05: テキスト・PDF の書き出し（F04）

- 機能仕様: `docs/functional-spec/F04-export.md`
- 関連: `docs/usage-scope.md` U1・U8、`docs/detailed-design/02-cas-brief.md`・`03-cas-other-products.md`
- ATAK の API: SDK の javadoc（`docs/references.md` の S1）

## 1. 範囲

- 対象の書式は 4 つ: CAS Brief（9-line・Remarks・Restrictions）、ルーティング、チェックイン、ゲームプラン。
- 書き出す中身は、画面の結果の欄に出した文字（各 `*Result.text()`）と同じもの。書き出しのために書式を作り直さない。
- 形式はテキスト（UTF-8）と PDF。
- 書き出したファイルは端末の中に置く。ほかのアプリに渡すのは、使う人が Android の共有の画面で選んだときだけ（U8）。
  JTAK からネットワーク・航空機（の役）へ自動で送ることはしない。
- RED（兵器の範囲）は書き出さない（地図に描くだけ。`04-weapon-effects.md`）。

## 2. core（`jp.jtak.core.export`）

ATAK に依存しない。JUnit で試験する。

### 2.1 `ExportKind`（enum）

| 値 | ファイル名に使う語 |
|---|---|
| `CAS_BRIEF` | `cas-brief` |
| `ROUTING` | `routing` |
| `CHECK_IN` | `check-in` |
| `GAME_PLAN` | `game-plan` |

- `String slug()` で上の語を返す。

### 2.2 `ExportFileName`

- `static String of(ExportKind kind, LocalDateTime time, String extension)`
- 形: `jtak-<slug>-<yyyyMMdd>-<HHmmss>.<extension>`。例: `jtak-cas-brief-20261109-153012.txt`
- 英数字・`-`・`.` だけを使う（どの端末・アプリでも扱えるように）。
- `extension` は `txt` か `pdf`。それ以外・null・空は `IllegalArgumentException`。`kind`・`time` が null も `IllegalArgumentException`。
- 時刻は端末の現地時刻（呼ぶ側が `LocalDateTime.now()` を渡す）。

### 2.3 `ExportText`

- `static List<String> bodyLines(String resultText)`
  - `resultText` を `\n` で行に分け（`\r\n` も `\n` として扱う）、先頭と末尾の U1 の行（`TrainingNotice.text()` と同じ行）を 1 行ずつ除いた本文の行を返す。
  - 先頭・末尾に U1 がなければ、その側は除かない。
  - 末尾の空の行は除く。
- `static String fileContent(String resultText)`
  - `TrainingNotice.text()` + `\n` + 本文の行（`\n` でつなぐ）+ `\n` + `TrainingNotice.text()` + `\n` を返す。
  - 入力に U1 があってもなくても、**先頭と末尾にちょうど 1 行ずつ** U1 が入る（重ならない・抜けない）。
  - 本文の行の並びは画面と同じ。
- `resultText` が null か、本文の行が 1 行もないときは `IllegalArgumentException`（書くものがない）。
- 文字コードは UTF-8。BOM は付けない。改行は `\n`。

### 2.4 JUnit の試験（`ExportTextTest`・`ExportFileNameTest`）

1. 4 つの書式それぞれの formatter の結果を `fileContent` に通すと、先頭の行と最後の行が `TrainingNotice.text()` で、U1 の行は 2 行だけ。
2. 本文の行（先頭・末尾の U1 を除いた行）が、`result.text()` の同じ部分と同じ並び・同じ文字。
3. U1 のない文字を渡しても、先頭と末尾に U1 が付く。
4. `\r\n` の入力でも、出力の改行は `\n` だけ。出力は `\n` で終わる。
5. null・空・U1 だけの入力は `IllegalArgumentException`。
6. `ExportFileName.of` が 4 つの種類で決まった形になる（`jtak-check-in-20261109-153012.txt` など）。`pdf` も同じ。
7. 拡張子が `txt`・`pdf` 以外・null のときと、`kind`・`time` が null のときは `IllegalArgumentException`。

## 3. app（`jp.jtak.plugin.export`）

### 3.1 保存先

- `FileSystemUtils.getItem("export/jtak")`（端末では、ふつう `atak/export/jtak/`）。`FileSystemUtils.EXPORT_DIRECTORY` を使って組み立てる。
- なければ `mkdirs()` で作る。作れなかったら書き出しを止めて、画面に失敗を出す（4.3）。
- この場所は、ATAK が書き出しに使うディレクトリの下にまとめるための**仮の決定**。実機で共有できるかを確かめて、だめなら見直す（6 章）。

### 3.2 `ExportWriter`

- `File writeText(ExportKind kind, String resultText, LocalDateTime time)`
  - `ExportText.fileContent` の文字を UTF-8 で書き、そのファイルを返す。
- 同じ名前のファイルがあれば上書きする（名前に秒まで入るので、ふつうは起きない）。
- 失敗したら `IOException` を投げる（呼ぶ側が画面に出す）。

### 3.3 `PdfExporter`

`android.graphics.pdf.PdfDocument` で作る。

| 項目 | 値 |
|---|---|
| 紙 | A4 縦（595 × 842 pt） |
| 余白 | 上下左右 40 pt |
| 文字 | `Typeface.MONOSPACE`、10 pt、黒 |
| 行の間 | 文字の大きさの 1.4 倍 |
| U1 | **各ページ**の一番上と一番下に `TrainingNotice.text()` を 1 行ずつ。本文と同じ文字の大きさで、太字 |
| 本文 | `ExportText.bodyLines(resultText)` の行を、上から順に描く |
| 長い行 | 余白の内側に収まらない行は、`Paint.measureText` で測って折り返す。折り返した続きは、元の行と同じ並びの中に入れる |
| ページ | 本文がページに入りきらなければ、次のページに続ける。どのページにも上と下に U1 |

- `File writePdf(ExportKind kind, String resultText, LocalDateTime time)`。ファイル名は `ExportFileName.of(kind, time, "pdf")`。
- 書いたあと `PdfDocument.close()` を必ず呼ぶ（失敗したときも）。
- 日本語の字形（「訓練用」など）は `MONOSPACE` で出ることを実機で確かめた（6 章）。

### 3.4 `ExportShare`

- `void share(MapView mapView, File file, String mimeType, String fallbackText)`
- Context は 2 種類を使い分ける（レビューの結論で書き足した）:
  - **プラグインの Context**（コンストラクタで受け取る）: 文字列の資源（共有の画面の題名・Toast の文）を読むときだけに使う。
  - **ATAK の Context**（`mapView.getContext()`）: provider の Uri を作るときと、共有の画面を開くときに使う。
- Intent:
  - `Intent.ACTION_SEND`、型は `text/plain`（テキスト）か `application/pdf`（PDF）
  - `EXTRA_STREAM` は `FileProviderHelper.fromFile(mapView.getContext(), file)`（**ATAK の Context**）
  - `FileProviderHelper.setReadAccess(intent)` で読む権限を渡す
  - 同じ Uri を `intent.setClipData(ClipData.newRawUri(file.getName(), uri))` でも入れ、`intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)` を付ける（6 章の 1）。
    `setReadAccess` は API 24 以上で `FLAG_GRANT_READ_URI_PERMISSION` だけを付ける（SDK の main.jar を `javap -c` で見て確かめた。javadoc には書いていない）。
    それでも、ここで明示する。`setReadAccess` の呼び出しは残す。
  - `Intent.createChooser(intent, <題名>)` で包み、`FLAG_ACTIVITY_NEW_TASK` を付けて `mapView.getContext().startActivity` で開く
  - 包んだ chooser の Intent にも、同じ ClipData（`intent.getClipData()`）と `FLAG_GRANT_READ_URI_PERMISSION` を付ける。
    共有の画面（intentresolver）は chooser の Intent の権限で下見を読むため。
    `createChooser` は target の ClipData と grant の flag を chooser に写す（AOSP で確かめた）が、それに頼らず明示する。同じ ClipData と flag を重ねても害はない。
    変更前は target に ClipData がなかったため、chooser にも ClipData が上がらなかった（6 章の 1 の警告の原因）。
  - ClipData と flag を付けるのは Uri が取れたときだけ。Uri が null で `EXTRA_TEXT` を使うとき（4.3）は付けない。
  - 共有の画面を開く操作・送り先を選ぶ操作・保存先は変えない。
- 共有の画面を開くのは、使う人が書き出しのボタンを押したときだけ。JTAK は送り先を選ばない（U8）。
- Uri が null のとき（ATAK の provider がその場所を渡せないとき）は 4.3 のとおりにする。

## 4. 画面

### 4.1 ボタン

- 4 つの書式の欄（`cas_section`・`routing_section`・`checkin_section`・`gameplan_section`）それぞれで、結果の欄（`cas_result`・`routing_result`・`checkin_result`・`gameplan_result`）のすぐ下に、横に並べて 2 つのボタンを置く。

| ID | 文字（strings.xml） |
|---|---|
| `cas_export_text`・`routing_export_text`・`checkin_export_text`・`gameplan_export_text` | `export_text_button`: 「テキストを書き出す」 |
| `cas_export_pdf`・`routing_export_pdf`・`checkin_export_pdf`・`gameplan_export_pdf` | `export_pdf_button`: 「PDF を書き出す」 |

- ボタンは、その書式を作れたときだけ押せる（`setEnabled(true)`）。初めの状態と、作れなかったとき（結果の欄を空にしたとき。例: CAS Brief の Restrictions が食い違って結果を消したとき）は押せない。
- 書き出すのは、最後に作れた結果の `text()`（各パネルが持っておく）。結果の欄に表示された文字を読み直さない。
- 入力を変えたあと、作り直す前に書き出したときは、最後に作れた結果を書き出す（作り直さない）。

### 4.2 押したときの流れ

1. ボタンを押す。
2. `ExportWriter.writeText` か `PdfExporter.writePdf` でファイルを書く。
3. 保存先のパスを短く出す（`Toast`。例: 「書き出しました: atak/export/jtak/jtak-cas-brief-20261109-153012.pdf」）。
4. `ExportShare.share` で共有の画面を開く。使う人が送り先を選ぶか、閉じる。

- 書き出しはメインスレッドで行ってよい（書式は数十行で、ファイルは小さい）。

### 4.3 失敗したとき

| 起きたこと | 動作 |
|---|---|
| ディレクトリを作れない・書けない（`IOException`） | ファイルは残さない（途中まで書いたものは消す）。`Toast` で「書き出せませんでした」と理由を出す。共有の画面は開かない |
| テキストの共有で Uri が null | ファイルは保存したまま。同じ中身（`ExportText.fileContent`）を `EXTRA_TEXT` に入れて、`text/plain` で共有の画面を開く |
| PDF の共有で Uri が null | ファイルは保存したまま。`Toast` で保存先のパスを出し、「共有できませんでした。ファイルは保存先にあります」と知らせる |
| 共有の画面を開けない（`ActivityNotFoundException` など） | ファイルは保存したまま。`Toast` で保存先のパスを出す |

- どの場合も、ATAK を落とさない（例外はつかまえてログに出す）。

## 5. 訓練用途（usage-scope）

- U1: テキストは先頭と末尾、PDF は各ページの上と下に `TrainingNotice.text()` を入れる。外す設定・オプションは作らない。
- U8: 共有の画面を開くのは使う人がボタンを押したときだけ。送り先は使う人が選ぶ。JTAK は自動で送らない。
- 書き出す中身は画面の結果と同じなので、書式の項目・出典（U4）はこれまでのものから増えない。

## 6. 実機で確かめた結果（受入。2026-11-09）

実機（ATAK-CIV 5.8.0.9）に main（5677c57）の APK を入れ、利用者が CAS Brief をテキストと PDF に書き出した。`adb` は利用者の許可を得て使った。

1. **ATAK の provider（`com.atakmap.app.civ.provider`）は `atak/export/jtak/` のファイルを渡せる**。
   - ファイルは `atak/export/jtak/jtak-cas-brief-<日時>.txt`・`.pdf` の名前で保存された（3.1・2.2）。
   - 共有の Uri は `content://com.atakmap.app.civ.provider/external_files/emulated/0/atak/export/jtak/<ファイル>` になった（null にならなかった）。
   - 共有の画面で送り先を選ぶと、送り先がファイルを受け取った（PDF が `Download/` に同じ大きさ 23112 バイトで保存された）。保存先は変えなくてよい。
   - ただし、共有の画面（Android の intentresolver）が**送るファイルの下見（プレビュー）を読めない**。logcat に `Permission Denial: opening provider androidx.core.content.FileProvider ... that is not exported` と、`ChooserPreview: ... call Intent#setClipData() to ensure that the sharesheet is given permission.` が出る。
     送ることはできるので受入は妨げない。直し方（`Intent#setClipData` で Uri を渡し、読む権限を共有の画面にも渡す）は別の作業で行う（3.4 に書き足した。作業の範囲は 8 章）。
2. **PDF の日本語は字形どおりに出る**（3.3 の `MONOSPACE` のままでよい）。
   - 取り出した PDF を画像にして見たところ、ページの上と下の「TRAINING USE ONLY / 訓練用」と、「（未入力）」が正しい字で出ていた。
   - テキストのファイルも、先頭と末尾に「TRAINING USE ONLY / 訓練用」の行があり、日本語は化けていなかった（U1）。

## 7. 作業の範囲

- 作る: core の `jp.jtak.core.export`（2 章）と試験、app の `jp.jtak.plugin.export`（3 章）。
- 直す: 4 つのパネル（`CasBriefPanel`・`RoutingPanel`・`CheckInPanel`・`GamePlanPanel`）、`main_layout.xml`、`strings.xml`。
- `CheckInPanel`・`GamePlanPanel` は、共有（3.4）に使う `MapView` を持っていない。`CasBriefPanel`・`RoutingPanel` と同じく、コンストラクタの最初の引数で `MapView` を受け取る形にする（`(MapView mapView, Context context, View content)`）。
  そのため `JtakDropDownReceiver` の 2 つのパネルを作る行だけを直し、`mapView` を渡す。`MapView.getMapView()` は使わない（ほかのパネルと渡し方をそろえる）。
- 完了の条件: `bash software/atak-plugin/build.sh assembleDebug :core:test` が通る。

## 8. 共有の画面の下見を直す作業の範囲

- 直す: `software/atak-plugin/app/src/main/java/jp/jtak/plugin/export/ExportShare.java` の `share` だけ（3.4 の ClipData を付ける行）。
- 変えない: 保存先（3.1）、ファイル名（2.2）、書き出す中身と「TRAINING USE ONLY / 訓練用」の行（U1）、共有の画面を開くきっかけ（U8）、Uri が null のときの流れ（4.3）、ほかのクラス。
- 共有の画面に渡す中身はファイルの Uri だけ。読んだ中身をログに出さない。
- 完了の条件: `bash software/atak-plugin/build.sh assembleDebug :core:test :app:lintDebug -x :app:takdevLint` が通る。
- 実機での確かめは、利用者に確かめてから行う。確かめること:
  - テキストと PDF を書き出し、共有の画面に下見が出る。
  - logcat に `Permission Denial: opening provider` と `ChooserPreview: ... call Intent#setClipData()` が出ない。
  - 送り先がファイルを受け取れる（6 章の 1 と同じ）。
