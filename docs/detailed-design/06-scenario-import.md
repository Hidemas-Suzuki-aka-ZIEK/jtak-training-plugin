# 詳細設計 06: CAS 系の書式をファイルから読み込む

- 日付: 2026-11-09
- 機能仕様: `docs/functional-spec/F02-cas-products.md`（「ファイルから読み込む」）
- 出典: `docs/references.md` S4（書式の項目は 03・02 と同じ。この設計で新しく取り込む外部資料はない）
- 関連: `docs/interfaces/README.md` 3 章（ファイルの形）、`docs/usage-scope.md` U1・Q11・Q13、
  `docs/detailed-design/02-cas-brief.md`・`03-cas-other-products.md`（書式の文字の並び）、`05-export.md`（書き出し。F04）、
  SDK の javadoc（`docs/references.md` の S1。ファイルを選ぶ画面）、
  `07-english-ui.md` 1.4・2.1・3.1・3.3（画面と誤りの文の言語）

## 1. 範囲

| する | しない（ほかのタスク） |
|---|---|
| core: テキストのファイルを読み、書式の種類と項目の値に分ける（パーサ）。JUnit の試験 | 書き出し（F04）。読み込みは書き出しのテキストを読むだけで、書き出しの形は変えない |
| app: 4 つの画面（ルーティング・チェックイン・ゲームプラン・CAS Brief）に「ファイルから読み込む」のボタン、ファイルを選ぶ画面、入力欄を消す前の確認、入力欄への反映、飛ばした項目の表示 | 地図の点（9-line の点のスピナー・ルーティングの場所の点）を読み込みで作る・選ぶこと |
| | 1 つのファイルに複数の書式を書くこと |

- 読み込むのは、利用者が端末に置いたテキストだけ。ネットワークからは取らない（usage-scope U8 と同じ考え）。
- 読み込んだ値は入力欄に入れるだけ。出力（画面・書き出し）は今までどおり formatter が作るので、`TRAINING USE ONLY / 訓練用` は必ず入る（U1）。
  ファイルにこの行がなくても、あっても、出力は変わらない（usage-scope Q11）。
- 読み込みのあとも、入力欄はすべて手で直せる（受入基準 #2）。

## 2. core（`jp.jtak.core.importer`）

ATAK に依存しない純 Java（ADR 0003）。`jp.jtak.core.cas` の型を変えずに使う。

### 2.1 入口と型

```java
public final class ScenarioImporter {
    public static final int MAX_BYTES = 262_144;          // 256 KiB（interfaces 3.1）
    public static ScenarioImportResult parse(byte[] data, ScenarioKind expected, Lang lang);
    public static ScenarioImportResult read(InputStream in, ScenarioKind expected, Lang lang) throws IOException;
}
```

- `lang`（`jp.jtak.core.Lang`）は、誤りの文と `skippedItems` の文の言語（07 の 1.4・2.1）。省けない引数にし、日本語を既定にした overload は作らない。
  最初に `Objects.requireNonNull(lang, "lang")` を呼び、null なら `NullPointerException` を投げる（ほかの引数の検査より先。07 の 2.1）。
- `data`・`in`・`expected` のどれかが null なら `IllegalArgumentException`。それ以外の入力では例外を投げず、結果の `error` で知らせる（受入基準 #4）。
  `read` だけは、ストリームから読めないときに `IOException` を投げる。
- `read` は `in` から `MAX_BYTES + 1` バイトまで読む。`MAX_BYTES` を超えたら `TOO_LARGE` を返す（それ以上は読まない）。
  超えなければ、読んだバイト列で `parse` と同じ結果を返す。`in` は閉じない（開いた側が閉じる）。
  app はファイルの大きさを先に調べず、ファイルを開いて `read` に渡すだけにする（3.1）。

#### 2.1.1 importer の中の部品（2026-11-09 利用者の決定 4）

読み込みは `jp.jtak.core.importer` の中の部品だけで行う。どの部品もパッケージの中だけで使う（名前は仮。変えてよい）。

| 部品 | すること |
|---|---|
| `ScenarioLines` | バイト列を UTF-8 で読む（`REPORT`）、BOM を除く、LF・CRLF で行に分け CR を除く、前後の空白（U+3000 を含む）を除く、空行と U1 の行を飛ばす（行の番号は残す）、値を整える（空なら null）。2.3 の 1〜3・6 |
| `ImportLog` | `skippedItems` を集める（2.6 の並べ方） |
| `RoutingParser`・`CheckInParser`・`GamePlanParser`・`CasBriefParser` | 行の並びから DTO と飛ばした項目を作る。2.4 |
| `ScenarioImporter` | 上の部品を順につなぐだけ: 大きさ → 行 → 題名 → 書式の照合 → パーサ → `NOTHING_READ` の判定 |

- `jp.jtak.core.cas.CasText` はパッケージの中だけのまま使わない（`cas` の API は変えない）。値の整え方は `ScenarioLines` が持つ。
- 書き出し（`cas` の formatter）と読み込み（`importer`）をつなぐのは、interfaces 3 章のテキストの形だけにする。
  ラベルの文字（`Other AOS: ` など）は両方に書くことになるので、2.8 の D9（往復）で食い違いがないかを確かめる。
- `jp.jtak.core.cas` の enum（モード・TOT の種類など）は、DTO の型としてそのまま使う。
- `ScenarioKind`（enum）: `ROUTING`・`CHECK_IN`・`GAME_PLAN`・`CAS_BRIEF`。どの画面のボタンから読むかを app が渡す。
- `ScenarioImportResult`（final・不変）:

| 欄 | 型 | 意味 |
|---|---|---|
| `kind` | `ScenarioKind` | 題名の行から決めた書式。題名が読めなければ null |
| `routing`・`checkIn`・`gamePlan`・`casBrief` | 下の DTO | `kind` に合う 1 つだけが入り、ほかは null。`error` があればすべて null |
| `skippedItems` | `List<String>`（変えられない） | 飛ばした項目。ファイルの中の順。2.6 |
| `error` | `ScenarioImportError` | 読み込まないときの理由。読めたときは null |

- `ScenarioImportError`: `code`（enum。2.2）、`message`（`lang` の言語の文。2.2。画面にそのまま出す）、`actualKind`（`WRONG_KIND` のときの、ファイルの書式）。

### 2.2 読み込まないとき（エラー）

文は core の表に持つ（app で組み立てない）。`lang` が `JA` なら日本語、`EN` なら英語の文にする（07 の 1.4）。`{…}` は値を入れる。

| `code` | いつ | 画面に出す文（JA） | 画面に出す文（EN） |
|---|---|---|---|
| `TOO_LARGE` | 256 KiB を超える | `ファイルが大きすぎます（256 KiB まで）。` | `The file is too large (up to 256 KiB).` |
| `NOT_UTF8` | UTF-8 として読めない（`CharsetDecoder` を `REPORT` にして判定） | `UTF-8 のテキストとして読めません。` | `Cannot read the file as UTF-8 text.` |
| `EMPTY` | U1 の行と空行を除くと、何も書いていない | `ファイルに何も書かれていません。` | `The file is empty.` |
| `UNKNOWN_TITLE` | 題名の行が interfaces 3.1 の 4 つのどれとも一致しない | `題名の行が JTAK の書式ではありません。1 行目（TRAINING USE ONLY の行を除く）に書式の題名を書いてください。` | `The title line is not a JTAK format. Write the format title on the first line (excluding the TRAINING USE ONLY line).` |
| `WRONG_KIND` | 題名は読めたが、ボタンを押した画面と書式が違う | `このファイルは {ファイルの書式} の書式です。{ファイルの書式} の画面で読み込んでください。` | `This file is in the {ファイルの書式} format. Load it on the {ファイルの書式} screen.` |
| `NOTHING_READ` | 題名は合っているが、読めた項目が 1 つもない（すべて `（未入力）`・読めない） | `読み込める項目がありませんでした。` | `No items could be read.` |

- `{ファイルの書式}` は画面の名前。JA は `ルーティング`・`チェックイン`・`ゲームプラン`・`CAS Brief`、EN は `Routing`・`Check-in`・`Game plan`・`CAS Brief`
  （EN は app の `mode_routing`・`mode_checkin`・`mode_gameplan` と同じ文字）。
- 英語の文は、設計の決定（2026-11-09。07 の 1.4「06 の画面と誤りの文も 07 に従う」による）。
- `NOTHING_READ` は設計の決定（2.7 の 1）。ひな形をそのまま読んだときに、入力欄が空になるだけで「成功」に見えるのを防ぐ。
  エラーのときは入力欄を消さない（3.2 の 1）。

### 2.3 読み方の共通の決まり

1. 先頭の BOM（U+FEFF）を除く。改行は CRLF・LF のどちらも 1 行の終わりとする（CR だけは改行とみなさない。値の中の CR は取り除く）。
2. 各行の前後の空白（全角の空白 U+3000 を含む）を除いてから読む。
3. 空行と、`TRAINING USE ONLY / 訓練用` に完全に一致する行は飛ばす（知らせない。usage-scope Q11）。
4. 空でない最初の行が題名。interfaces 3.1 の表と完全に一致するかで書式を決める（大文字と小文字も区別する。受入基準 #10）。
5. 題名のあとの行は、行の頭のラベル（`Other AOS: `・`4 Target elevation: ` など。2.4）で項目を決める。行の順番は確かめない
   （書き出しの順でなくても読む）。ラベルは大文字と小文字を区別する。
   `:` で終わるラベル（`Other AOS:`・`4 Target elevation:` など）は、`:` のあとが空白か行末のときだけ合う（`Other AOS:X` はどのラベルにも合わず、8 で知らせる。2.7 の 11）。
6. 値は `ScenarioLines` で整える（改行は 1 行の中にはないので、前後の空白を除くだけ。2.1.1）。値が空なら、その項目は飛ばして知らせる。
7. 値が `（未入力）` の項目は飛ばして知らせる。
8. どのラベルにも合わない行は飛ばし、`{N} 行目` として知らせる（N はファイルの中の行の番号。1 から数え、U1 の行・空行も数に入れる）。
9. 同じ項目が 2 回以上あれば、最初の行を使い、あとの行を `{項目}（重複）` として知らせる。
10. ファイルにない項目（行ごとない）は、DTO の欄を null にする。app は読み込む前に入力欄をすべて空にする（3.2）ので、
    その欄は空（既定）のままになる（2026-11-09 利用者の決定 2。前の決め方を改めた）。ただし、書式で必須の項目（formatter が
    `missingItems` に入れる項目）がファイルにないときは、`{項目}（なし）` として知らせる。必須でない項目がないことは知らせない
    （書き出しは空の項目の行を出さないので、ないのがふつうのため）。

### 2.4 書式ごとの行と項目

DTO（`RoutingImport`・`CheckInImport`・`GamePlanImport`・`CasBriefImport`）は不変で、欄はすべて参照の型（`String`・enum・`Boolean`）。
**null は「ファイルになかった・飛ばした」で、その入力欄は空（既定）のまま**であることを表す（3.2 の 3 で先に消すため）。`jp.jtak.core.cas` の builder は使わない
（builder は null を空として扱い、組み合わせによっては例外を投げるため）。

#### 2.4.1 ルーティング（`RoutingImport`）

| ファイルの行 | DTO の欄 | 知らせる名前 |
|---|---|---|
| 本文の行（下の式） | `callsign`・`instruction`・`place` | `Routing/Holding` |
| `Other instructions: {値}` | `otherInstructions` | `Other instructions` |
| `You are the only aircraft on station.` | `onlyAircraftOnStation = TRUE` | `Only aircraft on station` |
| `Other AOS: {値}` | `otherAos` | `Other AOS` |
| `Immediate threats: {値}` | `threats` | `Immediate threats` |
| `Significant weather/terrain: {値}` | `weatherTerrain` | `Significant weather/terrain` |
| `Status calls: {値}` | `statusCalls` | `Status calls` |
| `Send your check-in.` | `closing = SEND` | `Closing` |
| `Hold your check-in.` | `closing = HOLD`、`holdReason` は null | `Closing` |
| `Hold your check-in, {理由}.` | `closing = HOLD`、`holdReason = {理由}` | `Closing` |

- 本文の行は、題名のすぐあとの行に限らず、次の式に合う最初の行とする（`(?:` は前を読むだけの組）:
  `^(?:(.+?), )?(proceed|Proceed) (.+) and report established\.$`、`^(?:(.+?), )?(maintain|Maintain) (.+)\.$`、
  `^(?:(.+?), )?(report passing|Report passing) (.+)\.$`。1 つめの組が `callsign`、3 つめの組が `place`。
  `callsign` がない行（`Proceed …`）は `callsign` を null（空のまま）にする。
- 本文の行が `{callsign}, （未入力）` か `（未入力）` のときは、`callsign` だけを読み（あれば）、`Routing/Holding（未入力）` を知らせる。
- `Other instructions:` などで、本文の式にも合ってしまう行（例: `Other instructions: maintain FL200.`）は、ラベルの行として読む（ラベルを先に調べる）。
- 結びの行がないときは `closing` を null（既定のまま）にする（2.3 の 10 の必須に入らない）。

#### 2.4.2 チェックイン（`CheckInImport`）

| ファイルの行 | DTO の欄 | 知らせる名前 |
|---|---|---|
| `{JTAC}, this is {航空機}` | `jtacCallsign`・`aircraftCallsign`（片方が `（未入力）` なら、その片方だけ飛ばす） | `JTAC callsign`・`Aircraft callsign` |
| `As fragged.` | `mode = AS_FRAGGED` | `Mode` |
| `As fragged, with exception:` | `mode = WITH_EXCEPTION` | `Mode` |
| `1 Mission number: {値}` 〜 `6 Capabilities: {値}` | `missionNumber`・`aircraft`・`positionAltitude`・`ordnance`・`playtime`・`capabilities` | `1 Mission number` など、ラベルの `:` の前 |
| `7 Abort code: {コード} (readback)` | `abortCode = {コード}`、`abortCodeNone = FALSE` | `7 Abort code` |
| `7 Abort code: none (standard ABORT, ABORT, ABORT)` | `abortCodeNone = TRUE`、`abortCode` は null | `7 Abort code` |

- 呼びかけの行は `^(.+), this is (.+)$`。
- モードの行がないときは `mode = FULL` にする（interfaces 3.2「どちらもなければ全項目の形」）。これは 2.3 の 10 の例外。
- `7 Abort code: {コード}` の末尾の ` (readback)` はなくてもよい（手で書いたファイルを読めるようにする）。
- 必須（2.3 の 10）は、`CheckInFormatter` が `missingItems` に入れる項目と同じにする。`JTAC callsign`・`Aircraft callsign` は、どのモードでも必須。
  `1 Mission number`〜`7 Abort code` は、モードが FULL のとき（モードの行がないときを含む）だけ必須。`As fragged`・`with exception` のときは、ないことを知らせない（2026-11-09 の決定。2.7 の 7）。

#### 2.4.3 ゲームプラン（`GamePlanImport`）

| ファイルの行 | DTO の欄 | 知らせる名前 |
|---|---|---|
| `[{callsign}, ]advise when ready for game plan.`（頭は大小どちらでも） | `callsign` | `Callsign` |
| 本文の行（下） | `typeOfControl`・`methodOfAttack`・`intent`・`collateralConcerns`・`ordnance`・`desiredEffects`・`fuzing`・`interval`・`line6Digits` | `Type of control`・`Method of attack`・`Intent`・`CDE concerns`・`Ordnance`・`Effects`・`Fuzing`・`Interval`・`Line 6 digits` |

値の中に `, ` や `; ` があっても読めるように、区切りではなくラベルの文字で分ける（2026-11-09 利用者の決定 1）。

- 1 行目: 末尾の `advise when ready for game plan.`（頭は大小どちらでも）を除き、残りの末尾の `, ` を除く。残りがコールサイン
  （`, ` を含んでもよい）。残りが空ならコールサインはない。
- 本文の行は `; advise when ready for 9-line.` で終わる最初の行。この終わりは行の末尾のものだけを除く（値の中の `; ` は値に残す）。残りを次の順に分ける:
  1. `^(.*), line 6 (.+) digits$` に合えば（`.*` は長いほうを取るので、最後の `, line 6 ` で分ける）、2 つめの組を `line6Digits` にし、1 つめの組を残りとする。
  2. 残りの中で、ラベル `, intent `・`, CDE concerns `・`, ordnance `・`, effects `・`, fuzing `・`, interval ` をこの順に探す。
     それぞれ、前に見つけたラベルのあとから探し、ないラベルは飛ばす。見つけたラベルから次に見つけたラベル（なければ末尾）までが、そのラベルの値。
  3. 最初に見つけたラベルの前（ラベルが 1 つもなければ残りのすべて）を頭とし、`, ` で区切る。
     - 最後の 2 つが、順に `typeOfControl`（`Type 1|Type 2|Type 3`）と `methodOfAttack`（`bomb on target|bomb on coordinate`）。
       `（未入力）` なら飛ばして `（未入力）` を知らせる。ほかの値なら読めない項目として飛ばす（`Type of control（読めない）`）。
     - その前の部分を `, ` でつなぎ直したものがコールサイン（`, ` を含んでもよい）。前の部分がなければ、本文の行にコールサインはない。
     - 頭を区切って 2 つに満たないときは、`Type of control（読めない）`・`Method of attack（読めない）` を知らせる。
- コールサインが 1 行目と本文の行の両方にあって違うときは、1 行目を使い、`Callsign（2 か所で違う）` を知らせる。
- 値に書けないのは、ラベルの文字（`, intent `〜`, interval `・`, line 6 `）だけ。`, ` と `; ` は書いてよい（interfaces 3.2）。

#### 2.4.4 CAS Brief（`CasBriefImport`）

- 段の見出しの行（`[1-3]`・`[4-6]`・`[7-9]`・`[Remarks]`・`[Restrictions]`）は飛ばす（知らせない）。項目はラベルで決めるので、見出しの位置は問わない。
- 末尾の ` (readback)` は、あれば除いて値にする。なくてもよい。

| ファイルの行 | DTO の欄 | 知らせる名前 |
|---|---|---|
| `1 IP/BP: {値}` | `line1` | `9-line 1` |
| `2 Heading: {値}[, Offset {値}]` | `line2`・`offset`（最後の `, Offset ` で分ける） | `9-line 2`・`Offset` |
| `3 Distance: {値}` | `line3` | `9-line 3` |
| `4 Target elevation: {値}` | `line4` | `9-line 4` |
| `5 Target description: {値}` | `line5` | `9-line 5` |
| `6 Target location: {値}` | `line6` | `9-line 6` |
| `7 Type mark / Terminal guidance: {値}` | `line7` | `9-line 7` |
| `8 Location of friendlies: {値}` | `line8` | `9-line 8` |
| `9 Egress: {値}` | `line9`（頭の `Egress ` を 1 つだけ除く。大文字と小文字を区別する） | `9-line 9` |
| `LTL/PTL: `・`Ordnance / Effects: `・`Threat / SEAD: `・`Additional remarks: `・`Additional calls: ` | `ltlPtl`・`remarksOrdnance`・`threat`・`additionalRemarks`・`additionalCalls` | ラベルの `:` の前 |
| `Danger close, initials {頭文字}` | `dangerClose = TRUE`・`dangerCloseInitials` | `Danger close` |
| `Danger close, initials （未入力）` | `dangerClose = TRUE`、`dangerCloseInitials` は null | `Danger close`（`Danger close（未入力）` と知らせる） |
| `ACAs: `・`FAHs: `・`Separation: `・`RW approval out of BP: `・`PLA: ` | `acas`・`fahs`・`separation`・`rwOutOfBp`・`pla` | ラベルの `:` の前 |
| `TOT {時刻}` | `tot = TIME`・`totTime` | `TOT` |
| `Standby TOT` | `tot = STANDBY` | `TOT` |
| `TOT will be issued after correlation` | `tot = AFTER_CORRELATION` | `TOT` |
| `Push when ready` | `tot = PUSH_WHEN_READY` | `TOT` |

- 4 行目: `1`〜`3` の行が 1 つもないときだけ、値の頭の `Elevation ` を除く（書き出しが `[1-3]` を出さないときに足す言葉。02 の 2.5）。
  `1`〜`3` のどれかがあれば、`Elevation ` も値として残す。
- 9 行目: 書き出しは、値が `Egress` で始まらないとき（大文字と小文字を区別しない）だけ、値の前に `Egress ` を足す（02 の 2.5・C14）。
  読むときは、頭の `Egress `（大文字と小文字を区別する）を 1 つだけ除く。入力欄が `west` でも `Egress west` でも、書き出しは
  `Egress west` になり、読み込むと入力欄は `west` になる。もう一度書き出すと同じ `Egress west` になる（値は変わるが、出力は変わらない）。
  頭が小文字の `egress …` は除かないので、値も出力もそのまま戻る。
- `TOT` の行が 2 種類以上あれば、2.3 の 9 と同じく最初の行を使う。`TOT {時刻}` の `{時刻}` が `（未入力）` なら `TOT` ごと飛ばす。
- 必須（2.3 の 10）は、9-line の 4〜9 行目と、`1`〜`3` のどれかがあるときの 1〜3 行目（02 の 2.5。formatter が `missingLines` に入れる行と同じ）。

### 2.5 ファイルの中で両立しない組み合わせ

- ルーティング: `You are the only aircraft on station.` と `Other AOS:` の両方があれば、両方を飛ばし、`Only aircraft on station（組み合わせ）`・
  `Other AOS（組み合わせ）` を知らせる（03 の 2.2.1 で両方はなりたたないため）。
- チェックイン: `7 Abort code` の 2 つの形の両方があれば、2.3 の 9（重複）として最初の行を使う。
- ほかに、組み合わせで読まない項目はない。
- 組み合わせで飛ばした項目は、「読めた項目」に数えない。ほかに読めた項目がなければ、2.2 の `NOTHING_READ` にする（2.7 の 10）。

### 2.6 飛ばした項目の知らせ方

- `skippedItems` の 1 つの要素は、`lang` の言語で作る（07 の 1.4）。
  - JA: `{知らせる名前}（{理由}）` か `{N} 行目`。理由は `未入力`・`読めない`・`重複`・`組み合わせ`・`なし`・`2 か所で違う`。
  - EN: `{知らせる名前} ({理由})` か `line {N}`。理由は上と同じ順に `not entered`・`unreadable`・`duplicate`・`conflict`・`missing`・`differs in 2 places`。
  - `{知らせる名前}`（2.4 の表）は、どちらの言語でも同じ英語のラベル（`Routing/Holding`・`9-line 6` など）にする。
  - この設計書のほかの節の例（`Routing/Holding（未入力）`・`{N} 行目` など）は JA の形で書いている。
- 並びはファイルの中で見つけた順。`（なし）` は最後に、2.4 の表の順で足す。
- 画面での出し方は 3.3。

### 2.7 設計の決定（出典にない決め方）

1. 題名だけ合っていて何も読めないファイルはエラーにする（`NOTHING_READ`）。ひな形をそのまま読んで、入力欄を消しただけで成功に見えないようにするため。エラーのときは入力欄を消さない（3.2 の 1）。
2. 行の順番は確かめない。手で書いたファイルや、行を入れ替えたファイルも読めるようにする（interfaces 3.1 は「順番は書き出しと同じにする」と書く側の決まりにしている）。
3. 同じ項目は最初の行を使う。
4. ルーティングのコールサインは、`Proceed …` のように頭が大文字の行では null にする。読み込みの前に入力欄を消すので、コールサインの欄は空になる（3.2 の 3）。
5. 2.4 の `(readback)`・`Egress `・`Elevation ` の扱い。どれも、書き出し → 読み込み → 書き出しで、出力が最初の書き出しと同じになるようにする（2.8 の D9）。
   入力欄の値は、9 行目の頭の `Egress ` だけ元と違うことがある（2.4.4）。
6. （2026-11-09 削除）app の欄の反映で、片方を入れたらもう片方を消す組の決まり。読み込みの前に入力欄をすべて消すので要らなくなった（利用者の決定 2）。ファイルの中で両方を書いたときの扱いは 2.5 のまま。
7. （2026-11-09）チェックインの必須の項目は、モードで変わる（2.4.2）。`CheckInFormatter` が `（未入力）` を出すのと同じ範囲だけ `（なし）` を知らせ、`As fragged` のファイルで 1〜7 行目がないことを知らせない。
8. （2026-11-09）`Danger close, initials （未入力）` は、Danger close のチェックは入れ、頭文字の欄は空にして、`Danger close（未入力）` と知らせる（2.4.4）。
   formatter はチェックがあって頭文字が空のとき、この行を出して頭文字を `missingItems` に入れるので、書き出し → 読み込み → 書き出しで同じ出力になる。
9. （2026-11-09）飛ばした項目の行は、「未入力の行」の下に足して出す（3.3）。
10. （2026-11-09 のレビュー）2.5 の組み合わせで飛ばした項目は「読めた」に数えない。`You are the only aircraft on station.` と
    `Other AOS:` の 2 行だけのファイルは、入力欄に何も入らないので、1 と同じ理由で `NOTHING_READ` にする。
11. （2026-11-09 のレビュー）`:` で終わるラベルは、`:` のあとが空白か行末のときだけ合う（2.3 の 5）。formatter は `: ` の形でしか書かないので、
    往復には関わらない。`Other AOS:X` のような手書きの行を、ラベルの一部が値に混じった形で読まないため。
12. （2026-11-09 のレビュー）エラーの `message` は、「未入力の行」の欄と Toast の両方に出す（3.2 の 1）。

### 2.8 試験（JUnit。`core/src/test/java/jp/jtak/core/importer/`）

| ID | 内容 | 期待 |
|---|---|---|
| D1 | 4 つのデモのファイル（`docs/interfaces/scenario-samples/demo/`）を読む | それぞれ `error` が null、項目がファイルどおり、`skippedItems` が空。期待する値は、試験の中でデモのファイルの該当の行から取り出す（試験のコードに実在の兵器の名前を書かない。usage-scope Q13） |
| D2 | 4 つのひな形のファイルを読む | ルーティング・ゲームプラン・CAS Brief・チェックインとも `NOTHING_READ`（チェックインは mode = FULL だけでは「読めた」に数えない） |
| D3 | 一部が `（未入力）` のファイル。`Danger close, initials （未入力）` の行を含む形 | その項目だけ null、`skippedItems` に `（未入力）` の名前、ほかは読める。Danger close の行は、チェックは TRUE、頭文字は null、`Danger close（未入力）`（2.7 の 8） |
| D4 | 壊れた UTF-8、256 KiB を 1 バイト超える、空、U1 の行だけ | `NOT_UTF8`・`TOO_LARGE`・`EMPTY`・`EMPTY` |
| D5 | 題名が 1 文字違う・大文字と小文字が違う | `UNKNOWN_TITLE` |
| D6 | チェックインの画面でルーティングのファイル | `WRONG_KIND`、`actualKind = ROUTING` |
| D7 | CRLF・BOM、U1 の行あり・なし | どれも D1 と同じ結果 |
| D8 | 知らない行・同じ項目の 2 行・only aircraft と Other AOS の両方。題名と only aircraft と Other AOS の 2 行だけのルーティングのファイル | `{N} 行目`・`（重複）`・`（組み合わせ）`、ほかは読める。2 行だけのファイルは `NOTHING_READ`（2.5、2.7 の 10） |
| D9 | 書き出しとの往復: 試験の中で model を builder で作り、各 formatter の `text` を読み、読んだ値で model を作り直して formatter にかける | 作り直した model の `text` が元の `text` と同じ。読んだ値は、9 行目の頭の `Egress ` を除いて model と同じ（コールサインあり・なし、CAS Brief の `[1-3]` なしの形、`Egress ` で始まる 9 行目、TOT の 4 種類、Danger close、Abort code の 2 つの形、チェックインの 3 つのモード、チェックインの FULL で一部の項目が空（`（未入力）` で書き出す）の形、ゲームプランの値に `, ` と `; ` を含む形、`, ` を含むゲームプランのコールサイン）。作り直すとき、null の項目は空（既定）として扱う。読んだ値と model は、文の一致とは別に欄ごとに比べる。試験の値に実在の兵器の名前を使わない（`sample A` などにする。usage-scope Q13） |
| D10 | 行の順番を入れ替えたファイル | D1 と同じ結果 |
| D11 | ゲームプランのコールサインが 1 行目と本文の行で違う | 1 行目のコールサイン、`Callsign（2 か所で違う）` |
| D12 | 必須の行がないファイル（ルーティングの本文の行、CAS Brief の 6 行目） | `Routing/Holding（なし）`・`9-line 6（なし）`、ほかは読める |
| D13 | ゲームプランの値に `, ` と `; ` を含むファイル（例: intent `destroy, then reattack`、ordnance `sample A; sample B`）、`, ` を含むコールサイン、書いてもよい部分をいくつか省いた形 | 値・コールサインがファイルどおりに読め、省いた部分は null（2.4.3） |
| D14 | `read(InputStream)`: ちょうど `MAX_BYTES` バイトの流れ、`MAX_BYTES + 1` バイトの流れ、`in` が null | 1 つ目は `parse` と同じ結果、2 つ目は `TOO_LARGE`、3 つ目は `IllegalArgumentException`。どれも流れを閉じない |
| D15 | ゲームプランの本文の行で、ラベルの前の部分が `, ` で 2 つに分けられない、Type・攻撃の方法が決まった値でない | Type of control・Method of attack を `（読めない）` で知らせる |
| D16 | `ScenarioLines` の単体の試験: BOM、CRLF、CR だけの改行、全角の空白（U+3000）、行の番号、`:` で終わるラベルのあとが空白・行末・それ以外の文字 | 2.3 の決まりどおりに整え、行の番号はファイルの行の番号と同じ。ラベルは、`:` のあとが空白か行末のときだけ合う（2.3 の 5） |
| D17 | 言語: 2.2 の 6 つの誤りと 2.6 の 6 つの理由・`{N} 行目` を、`lang` を `JA`・`EN` にして作る。`lang` が null（`data`・`in` も null のときを含む） | 文が 2.2・2.6 の表と同じ（EN の `WRONG_KIND` は書式の名前も英語）。`lang` が null なら `NullPointerException`（`data`・`in` が null でも IAE より先） |

- D1〜D16 は `Lang.JA` で書く（期待する文は JA の形）。

- D1・D2 は、リポジトリのファイルを試験から読む（`docs/interfaces/scenario-samples/` へのパスは Gradle の `test` の作業ディレクトリから決める。
  読めない構成なら、同じ中身を試験の資源にコピーし、2 つが同じかを試験で比べる）。

## 3. app（`jp.jtak.plugin.cas`）

### 3.1 ボタンとファイルを選ぶ画面

- 4 つの画面（`RoutingPanel`・`CheckInPanel`・`GamePlanPanel`・`CasBriefPanel`）の上のほうに「ファイルから読み込む」のボタンを置く（共通の部分は `PanelKit`）。
- 押したら ATAK の `ImportFileBrowserDialog` を開く（SDK の javadoc。`docs/references.md` の S1）:
  ```java
  new ImportFileBrowserDialog(context)
      .setTitle(pluginContext.getString(R.string.import_dialog_title))   // 3.5
      .setExtensionTypes("txt")
      .setStartDirectory(startDir)          // 下
      .setOnDismissListener(listener)
      .show();
  ```
  - `startDir` は `FileSystemUtils.getItem("export/jtak")`。なければ ATAK の作業のフォルダ（`FileSystemUtils.getRoot()`）。場所は問わない（決定 b）。
  - `onFileSelected(null)`・`onDialogClosed()` のときは何もしない。
  - `setUseProvider` は呼ばない（既定のまま）。実機で `atak/` の中と外（`Download/` など）の両方から読んで確かめる（4 章の 2、利用者の決定 3）。
    外のファイルが読めなければ、`setUseProvider(true)` を試し、決定 b「場所は問わない」の扱いを利用者に確かめる。
- ファイルを選んだら、別のスレッドで `FileInputStream` を開き、`ScenarioImporter.read` を呼び、流れを閉じて、結果を UI のスレッドに戻す
  （大きさの確かめと読み方は core の `read` に任せる。app は先に `File.length()` を見ない。2.1.1、利用者の決定 4）。
  別のスレッドは、既存の画面に仕組みがあればそれを使う。なければ 1 本のスレッドの `ExecutorService` を画面ごとに持ち、画面を閉じたら止める。
  `read` の `lang` には `JtakLang.of(pluginContext)` を渡す（07 の 3.1）。
  開けない・読めない（`IOException`）ときは `ファイルを読めませんでした。`（3.5 の `import_read_failed`）を出し、入力欄は変えない。

### 3.2 反映の順番

1. `error` があれば、その `message` を「未入力の行」の欄と Toast に出して終わる（入力欄は変えない。「未入力の行」の欄の文は `message` に置き換わる。受入基準 #4・#10。2.7 の 12）。
2. 入力欄に 1 つでも値があれば（文字の欄が空でない、チェックがついている、選ぶ欄が既定でない）、`AlertDialog` で確かめる:
   「入力中の内容を消して、ファイルの値を入れます。ファイルにない項目は空（既定）になります。」、ボタンは「消して読み込む」「やめる」。
   「やめる」なら何も変えない（受入基準 #8）。
3. その画面の入力欄をすべて、画面を開いたときの状態に戻す（利用者の決定 2）: 文字の欄は空、チェックは外す、選ぶ欄は既定、
   チェックインのモードは画面を開いたときの「なし」。読み込みでは、4 でファイルのモード（行がなければ FULL。2.4.2）が必ず入るので、
   読み込んだあとに「なし」のまま残ることはない（2026-11-09 の決定。前の版の「全項目（FULL）に戻す」を改めた）。
4. DTO の null でない欄を、入力欄に入れる（受入基準 #9）。null の欄は 3 で戻した状態のまま。
5. 入れたあと、今までどおり formatter で出力を作り直す（出力の U1 の行は formatter が入れる）。
6. `skippedItems` が空でなければ、3.3 で出す。

- 3 の「すべて」には、9-line の点のスピナーと、ルーティングの地図の点は入れない（消さない。2 の確かめでも数えない）。
- CAS Brief の画面: 9-line の点のスピナー（IP/BP・目標・味方）は変えない。値は 1〜9 行目の文字の欄に入れる。
  点を選ぶとその行の文字の欄が書き変わる作りなら、読み込みのあとに点を選び直したときだけ書き変わる（読み込みでスピナーを動かさない）。
  作りがこの決まりと合わないときは、推測で実装せず根拠を確かめる。
- ルーティングの画面: 場所を地図の点から入れる欄も、文字の欄に `place` を入れるだけにする。

### 3.3 飛ばした項目の出し方

- 02 の 3.1 の 4 の「未入力の行」と同じ場所・同じ見た目で、`読み込まなかった項目: {skippedItems を , でつなぐ}`（3.5 の `import_skipped_items`）を出す。
  区切りは「未入力の行」と同じ `PanelKit.join`（`, `。言語によらない）を使う（2026-11-09 の決定。前の版の「、」はやめた）。
  次に出力を作り直す（「作る」を押す）か、別のファイルを読むまで出したままにする。
- 「未入力の行」が出ているときは、その下に続けて出す（同じ欄の中で改行して足すか、すぐ下の行に置く）。出力を作り直したら（「作る」を押したら）、
  「未入力の行」は作り直した内容になり、飛ばした項目の行は消す（2026-11-09 の決定。2.7 の 9）。
- 何も飛ばさなかったときは、`ファイルを読み込みました。`（3.5 の `import_done`）を短く出す（Toast でよい）。

### 3.4 守ること

- 出力のすべてに `TRAINING USE ONLY / 訓練用` が入ることは変えない（読み込みの仕組みでこの行を作ったり消したりしない。U1）。
- 読んだファイルの中身をログに出さない（場所の名前・コールサインが入るため）。出すのはエラーの `code` だけ。

### 3.5 画面の文（values と values-ja）

07 の 3.3 に従う。下の文字列のリソースを `values/strings.xml`（英語）と `values-ja/strings.xml`（日本語）の両方に置く。
キーの名前は仮（変えてよい）。書式の引数（`%1$s`）は JA と EN で同じにする。英語の文は 2026-11-09 に決めた。

| キー（仮） | どこで | values（EN） | values-ja（JA） |
|---|---|---|---|
| `import_button` | 4 つの画面のボタン（3.1） | `Load from file` | `ファイルから読み込む` |
| `import_dialog_title` | ファイルを選ぶ画面の題名（3.1） | `File to load (.txt)` | `読み込むファイル（.txt）` |
| `import_read_failed` | `IOException` のとき（3.1） | `Could not read the file.` | `ファイルを読めませんでした。` |
| `import_confirm_message` | 消す前の確かめ（3.2 の 2） | `Clear the current entries and load the file values. Items not in the file will be empty (default).` | `入力中の内容を消して、ファイルの値を入れます。ファイルにない項目は空（既定）になります。` |
| `import_confirm_ok` | 同じ（ボタン） | `Clear and load` | `消して読み込む` |
| `import_confirm_cancel` | 同じ（ボタン） | `Cancel` | `やめる` |
| `import_skipped_items` | 飛ばした項目（3.3） | `Items not loaded: %1$s` | `読み込まなかった項目: %1$s` |
| `import_done` | 何も飛ばさなかったとき（3.3） | `File loaded.` | `ファイルを読み込みました。` |

- 誤りの文（2.2）と `skippedItems` の中身（2.6）は core が `lang` で作るので、リソースには置かない。
- lint で MissingTranslation と Untranslatable が 0 件になること（07 の 3.3）。

## 4. 確かめ方

1. `JAVA_HOME=<JDK 17 の場所> bash software/atak-plugin/build.sh assembleDebug :core:cleanTest :core:test :app:lintDebug -x :app:takdevLint` が通る。
   2.8 の D1〜D17 が通る。lint の MissingTranslation・Untranslatable が 0 件（3.5）。
2. 実機の確認（`adb install` など）は、利用者に確かめてから行う（usage-scope U6）:
   - 4 つの画面で、デモのファイルを `atak/export/jtak/` に置いて読み、出力がデモのファイルと同じになる（U1 の行を含む）。
   - 同じデモのファイルを `atak/` の外（`Download/` など）に置いて読み、同じ結果になる（利用者の決定 3）。読めなければ 3.1 の手順で確かめる。
   - 「消して読み込む」: CAS Brief で Restrictions を入れたあと、Restrictions のないファイルを読むと、Restrictions の欄が空になる。
   - 確かめの「やめる」、題名が違うファイル、ひな形のファイル（`NOTHING_READ`。どれも入力欄は変わらない）。
3. 実機の確認の結果（2026-11-09、実機・ATAK-CIV 5.8.0.9、PR #25 の APK）:
   - 入れ方: ワークツリーごとにデバッグ用の署名の鍵が違い、`adb install -r` が `INSTALL_FAILED_UPDATE_INCOMPATIBLE` で失敗した。
     利用者に確かめてから、プラグイン（`jp.jtak.plugin`）だけを消して入れ直した。ATAK 本体は消していない。プラグインの設定は初めの状態に戻った。
   - 置いたファイル: `atak/export/jtak/` にデモの 4 つとひな形の 4 つ、`Download/` に `jtak-demo-1-routing.txt`。
   - 利用者が確かめ、おかしな点はなかった:
     1. ファイルを選ぶ画面が `atak/export/jtak` から開き、`Download/` のファイルも選んで読み込める（利用者の決定 3）。
     2. 入力欄に値があると消してよいかを確かめるダイアログが出る。Toast が出る。飛ばした項目が「未入力の行」の下に並ぶ。
     3. 「作る」を押すと、飛ばした項目の行が消える（3.3）。
   - 実機では確かめを頼まなかったもの: 確かめの「やめる」、題名が違うファイル、「消して読み込む」で Restrictions の欄が空になること。
     題名が違うファイルは core の試験（2.8）で確かめた。「やめる」と Restrictions の欄は app の動きで、
     2026-11-09 のレビューでコードを読んだだけで、動かしてはいない。気になるときは次に実機へ入れたときに確かめる。
   - 次に実機へ入れるときも、別のワークツリーで作った APK は鍵が違うので、入れ直しが要る（プラグインの設定は消える）。

## 5. 決まったこと（2026-11-09 利用者の決定）

設計の段階で「未決」にしていた 4 つを、利用者が次のとおり決めた。

1. ゲームプランの値に `, ` と `; ` を書いてよい。パーサはラベル（`, intent ` など）を手がかりに読む。値に書けないのはラベルの文字列だけ（2.4.3、interfaces 3.2）。
2. 新しく読み込むときは、入力欄を必ず空にしてから入れる。ファイルにない項目は空（既定）になり、前の場面の値は残らない（2.3 の 10、3.2）。
3. 実機で、`atak/` の中と外の両方から読み込む試験をする（3.1、4 章の 2）。
4. 読み方の部品は `importer` に持ち、ファイルの読み込みもすべて `importer` から行う（`ScenarioImporter.read`）。`CasText` は `public` にしない（2.1.1）。
   書式を作る側（formatter）と読む側（importer）は、テキストの形だけでつながる。同じラベルを 2 か所に書くことになるので、D9 の往復の試験で食い違いを見つける。
