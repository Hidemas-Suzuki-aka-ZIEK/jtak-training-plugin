# 詳細設計 03: ルーティング・チェックイン・ゲームプラン・Restrictions の生成

- 日付: 2026-11-08
- 機能仕様: `docs/functional-spec/F02-cas-products.md`（「ルーティング」「チェックイン」「ゲームプラン」「Restrictions」の表）
- 出典: `docs/references.md` S4（JP 3-09.3 2014 年版の図 V-3・V-4・V-9、V-14–V-20・V-31–V-33、図 III-7 と III-42–III-47）
- 関連: `docs/usage-scope.md` U1・U4・U8、`docs/detailed-design/02-cas-brief.md`（文字の決まり・CasBriefFormatter・画面）、ADR 0003

## 1. 範囲

| する | しない（ほかのタスク） |
|---|---|
| core: ルーティング・チェックイン・ゲームプランの文字の並び、Restrictions の型と 9-line の後ろへの足し方、JUnit の試験 | 9-line・Remarks の中身と自動の値（`02-cas-brief.md`） |
| app: ルーティング・チェックイン・ゲームプランの画面、9-line の画面に Restrictions の欄を足す、ルーティングの場所を地図の点から入れる | テキスト・PDF の書き出し（F04）、5 行目の下書き（`02-cas-brief.md` 2.3.1） |
| | Situation Update、複数の航空機・編隊での攻撃の分け方（F02「この表に入れないもの」） |

- 航空機（の役）やネットワークへは送らない（U8）。出すのは画面だけ。
- 出力のすべての先頭と末尾の行に `TrainingNotice.text()` を入れる（U1）。外す設定は作らない。
- 書式の項目・並び・言い方は F02 の表（出典 S4）だけから取る。出典にない決め方は 2.3 に設計の決定として書く（U4）。
- ゲームプランは CAS Brief とは別の文字の並びにする（利用者の決定 2026-11-08「記載は CAS Brief とは分ける」）。

## 2. core（`jp.jtak.core.cas`）

### 2.1 共通

- 結果の型 `CasProductResult`（final）: `String text` と `List<String> missingItems`（変えられないリスト。必須なのに空の項目の名前を、出す順に入れる）。
- 文字の決まりは `02-cas-brief.md` 2.5 と同じ。値の前後の空白を取る。改行は空白 1 つにする。全角の空白（U+3000）も空白として扱う。空白だけの値は空とみなす。
- 必須の項目が空のときは `（未入力）` と出し、`missingItems` に入れる。必須でない項目は、空なら行ごと出さない。
- `format` の引数が null なら `IllegalArgumentException`。入力の型は不変で、builder で作る。builder の setter は null を受けてよい（空と同じ）。
- 呼び出し符号の付け方: 航空機の呼び出し符号（`callsign`）が空でなければ、呼びかけの行の頭に `{callsign}, ` を付ける。空なら付けない（文の頭の文字は大文字にする。下の各節の例を参照）。

### 2.2 書式ごとの型と文字の並び

#### 2.2.1 ルーティング `RoutingFormatter.format(Routing)`

`Routing` の欄: `callsign`、`instruction`（enum `RoutingInstruction`）、`place`、`otherInstructions`、`onlyAircraftOnStation`（boolean）、`otherAos`、`threats`、`weatherTerrain`、`statusCalls`、`closing`（enum `RoutingClosing`）、`holdReason`。

| `RoutingInstruction` | 出す文（出典 図 V-3） |
|---|---|
| `PROCEED_REPORT_ESTABLISHED` | `Proceed {place} and report established.` |
| `MAINTAIN` | `Maintain {place}.` |
| `REPORT_PASSING` | `Report passing {place}.` |

```
TRAINING USE ONLY / 訓練用
ROUTING (JP 3-09.3 2014 Fig. V-3)
Razor 53, maintain Chevy-Dodge 14-15.
Other instructions: {otherInstructions}
You are the only aircraft on station.        ← onlyAircraftOnStation のとき
Other AOS: {otherAos}                         ← otherAos が空でないとき
Immediate threats: {threats}
Significant weather/terrain: {weatherTerrain}
Status calls: {statusCalls}
Send your check-in.                           ← closing = SEND
TRAINING USE ONLY / 訓練用
```

- 1 行目の指示は必須。`instruction` か `place` が空なら、その行を `{callsign, }（未入力）` とし、`missingItems` に `Routing/Holding` を入れる。
- 呼び出し符号を付けたときは、指示の文の頭を小文字にする（`Razor 53, maintain …`）。付けないときは大文字のまま（`Maintain …`）。
- `onlyAircraftOnStation` と `otherAos`（空でない）を両方入れたら、`build()` が `IllegalArgumentException` を投げる。両方なければ行を出さない。
- 結び（`RoutingClosing`）: `SEND` → `Send your check-in.`、`HOLD` → `Hold your check-in.`（`holdReason` が空でなければ `Hold your check-in, {holdReason}.`）、`NONE` か null → 行を出さない。`holdReason` は `HOLD` のときだけ使う。

#### 2.2.2 チェックイン `CheckInFormatter.format(CheckIn)`

JTAC が受けた内容を記録して見直すためのもの（F02）。`CheckIn` の欄: `jtacCallsign`、`aircraftCallsign`、`mode`（enum `CheckInMode`）、`missionNumber`、`aircraft`、`positionAltitude`、`ordnance`、`playtime`、`capabilities`、`abortCode`、`abortCodeNone`（boolean）。

```
TRAINING USE ONLY / 訓練用
CHECK-IN (JP 3-09.3 2014 Fig. V-4)
Hawg 11, this is Razor 53
1 Mission number: {missionNumber}
2 Number and type of aircraft: {aircraft}
3 Position and altitude: {positionAltitude}
4 Ordnance: {ordnance}
5 Playtime: {playtime}
6 Capabilities: {capabilities}
7 Abort code: {abortCode} (readback)
TRAINING USE ONLY / 訓練用
```

- 呼びかけの行: `{jtacCallsign}, this is {aircraftCallsign}`。どちらかが空なら空の側を `（未入力）` にし、`missingItems` に `JTAC callsign` / `Aircraft callsign` を入れる。
- 7 の Abort code: `abortCodeNone` のときは `7 Abort code: none (standard ABORT, ABORT, ABORT)` と出す（`(readback)` は付けない）。`abortCode` と `abortCodeNone` を両方入れたら `build()` が `IllegalArgumentException`。
- `CheckInMode`:
  - `FULL`（既定）: 1〜7 をすべて出す。空の項目は `（未入力）` で、`missingItems` に `1 Mission number` のように番号つきの名前で入れる。
  - `AS_FRAGGED`: 呼びかけの次に `As fragged.` を出し、1〜7 は入れたものだけを出す。空でも `missingItems` に入れない。
  - `WITH_EXCEPTION`: 呼びかけの次に `As fragged, with exception:` を出し、1〜7 は入れたものだけを出す。空でも `missingItems` に入れない。
- 呼びかけの行の呼び出し符号は、どのモードでも必須。

#### 2.2.3 ゲームプラン `GamePlanFormatter.format(GamePlan)`

CAS Brief とは別の書式。`GamePlan` の欄: `callsign`、`typeOfControl`（enum `TypeOfControl`: `TYPE_1`・`TYPE_2`・`TYPE_3`）、`methodOfAttack`（enum `MethodOfAttack`: `BOT`・`BOC`）、`intent`、`collateralConcerns`、`ordnance`、`desiredEffects`、`fuzing`、`interval`、`line6Digits`。

```
TRAINING USE ONLY / 訓練用
GAME PLAN (JP 3-09.3 2014 Fig. V-9)
Razor 53, advise when ready for game plan.
Razor 53, Type 2, bomb on coordinate, {追加の情報}; advise when ready for 9-line.
TRAINING USE ONLY / 訓練用
```

- 本文の行は出典 V-20 の例（「Deuce 21, Type 1, bomb on target; advise when ready for 9-line.」）の形にする。
- `TypeOfControl` → `Type 1` / `Type 2` / `Type 3`。`MethodOfAttack` → `BOT` は `bomb on target`、`BOC` は `bomb on coordinate`。
- どちらも必須。空なら `（未入力）` を出し、`missingItems` に `Type of control` / `Method of attack` を入れる。
- 追加の情報は、入れたものだけを F02 の順（intent → collateral damage concerns → ordnance → desired effects → fuzing → interval → line 6 digits）に `, ` でつなぐ。ラベルを付ける: `intent {…}`、`CDE concerns {…}`、`ordnance {…}`、`effects {…}`、`fuzing {…}`、`interval {…}`、`line 6 {…} digits`。何もなければ `, {追加の情報}` を出さない。
- 呼び出し符号が空なら、1 行目は `Advise when ready for game plan.`、本文は `Type 2, bomb on coordinate; advise when ready for 9-line.` とする。
- 図 III-7 の Type 1〜3 と BOT・BOC の説明は、出力の文に入れない（画面の説明だけ。3 章）。

#### 2.2.4 Restrictions `CasRestrictions` と `CasBriefFormatter.format(NineLine, CasRemarks, CasRestrictions)`

`CasRestrictions` の欄（この順で出す）: `dangerClose`（boolean）と `dangerCloseInitials`、`acas`、`fahs`、`separation`、`rwOutOfBp`、`pla`、`tot`（enum `TotType`: `TIME`・`STANDBY`・`AFTER_CORRELATION`・`PUSH_WHEN_READY`）と `totTime`。

9-line の文字の並び（`02-cas-brief.md` 2.5）の `[Remarks]` の後ろ、最後の注意の行の前に入れる。

```
[Restrictions]
Danger close, initials {dangerCloseInitials} (readback)
ACAs: {acas} (readback)
FAHs: {fahs} (readback)
Separation: {separation} (readback)
RW approval out of BP: {rwOutOfBp} (readback)
PLA: {pla} (readback)
TOT {totTime} (readback)
```

- 入れたものだけを出す。どれも入れていなければ `[Restrictions]` の節ごと出さない。
- Danger close: `dangerClose` が false なら出さない。true でイニシャルが空なら `Danger close, initials （未入力） (readback)` とし、`missingItems` に `Danger close initials` を入れる。イニシャルだけ入れて `dangerClose` が false なら `build()` が `IllegalArgumentException`。
- TOT/TTT は最後に出す（2.3）。`TIME` → `TOT {totTime}`（`totTime` が空なら `TOT （未入力）`、`missingItems` に `TOT`）、`STANDBY` → `Standby TOT`、`AFTER_CORRELATION` → `TOT will be issued after correlation`、`PUSH_WHEN_READY` → `Push when ready`、null → 出さない。4 つとも行の末尾に ` (readback)` を付ける（2.3 の決定 10。例 `Standby TOT (readback)`）。`totTime` は `TIME` のときだけ使い、ほかで入れたら `build()` が `IllegalArgumentException`。
- `missingItems` は `CasBriefResult` の `missingLines` とは別に、`CasBriefResult` に `missingRestrictions`（変えられないリスト）を足して返す。2 つの引数の `format` では空のリスト。
- 2 つの引数の `format(NineLine, CasRemarks)` は、空の `CasRestrictions` を渡したのと同じにする。今の出力は 1 文字も変えない（C1〜C23 がそのまま通る）。

### 2.3 設計の決定（出典に決まった形がないもの。U4）

| # | 決めたこと | 理由 |
|---|---|---|
| 1 | Restrictions の並びは本文（V-32）の番号に従い、TOT/TTT を最後にする | 本文 6.a が「TOT should be last」とする。図 V-9 は 4 項目だけで、本文の番号では TOT/TTT が PLA の前にあるため |
| 2 | 「Immediate」は TOT の選ぶ欄に入れない | 本文に決まった言い方がない。時刻の欄に書けば出せる |
| 3 | 各書式の見出しの行（`ROUTING (JP 3-09.3 2014 Fig. V-3)` など） | 02 の `CAS 9-LINE (JP 3-09.3 2014 Fig. V-9)` にそろえ、出典の図が分かるようにする |
| 4 | チェックインの 3 つのモード（全部・as fragged・with exception） | 本文 V-15 が「as fragged」「with exception」で短くしてよいとする。何を出すかの細かい形は出典にない |
| 5 | 呼び出し符号を付ける・付けないの決まりと、文の頭の大文字・小文字 | 図 V-3・V-20 の例はすべて呼び出し符号で始まる。空のときの形は出典にない |
| 6 | ゲームプランの文は V-20 の例の形にし、「control」の語を付けない（`Type 2`） | V-20 の例「Deuce 21, Type 1, bomb on target」に合わせる |
| 7 | ゲームプランの追加の情報のラベル（`intent`・`CDE concerns` など） | 本文 V-20 は項目を挙げるだけで、読み上げの言い方を決めていない |
| 8 | Other AOS は「ほかの機はいない」を選ぶ欄と、手入力の欄に分け、両方は入れられない | 本文 V-14 の「いなければ、そう言う」を、利用者が選んだときだけ出すため（F02） |
| 9 | Abort code の「none (standard ABORT, ABORT, ABORT)」は、利用者が選んだときだけ出す | 本文 V-17 は言われなければ標準とする。受けていないのに出すと記録として誤るため |
| 10 | 各 Restrictions の行と、チェックインの Abort code に `(readback)` を付ける | 図 V-9「Lines 4, 6, and any restrictions are mandatory readback.」と V-17（受けたコードは復唱する） |

### 2.4 試験（JUnit。`core` の `src/test`）

| # | 入力 | 期待 |
|---|---|---|
| D1 | ルーティング: 呼び出し符号 `Razor 53`、`MAINTAIN`、場所 `Chevy-Dodge 14-15`、結び `SEND` | 2.2.1 の例のとおりの行。先頭と末尾が `TrainingNotice.text()`。`missingItems` は空 |
| D2 | ルーティング: 呼び出し符号なし、`PROCEED_REPORT_ESTABLISHED`、場所あり | `Proceed {place} and report established.`（大文字で始まる） |
| D3 | ルーティング: 指示の種類が空 | 指示の行が `（未入力）`、`missingItems` が `[Routing/Holding]` |
| D4 | ルーティング: `onlyAircraftOnStation` と `otherAos` を両方 | `build()` が `IllegalArgumentException` |
| D5 | ルーティング: 結び `HOLD`・理由 `attack in progress` / `HOLD`・理由なし / `NONE` / `SEND` で理由あり | `Hold your check-in, attack in progress.` / `Hold your check-in.` / 結びの行なし / `Send your check-in.`（理由は出さない） |
| D6 | ルーティング: 脅威・天気・status calls が空と全角の空白だけ | その行を出さない |
| D7 | チェックイン `FULL`: 7 項目をすべて入れる | 2.2.2 の例のとおり。7 に `(readback)` |
| D8 | チェックイン `FULL`: 3 と 5 が空 | 3 と 5 が `（未入力）`、`missingItems` が `[3 Position and altitude, 5 Playtime]` |
| D9 | チェックイン `AS_FRAGGED`: 4 だけ入れる / `WITH_EXCEPTION`: 4 だけ入れる | `As fragged.` と 4 の行だけ / `As fragged, with exception:` と 4 の行だけ。どちらも `missingItems` は空 |
| D10 | チェックイン: `abortCodeNone` / `abortCode` と `abortCodeNone` を両方 | `7 Abort code: none (standard ABORT, ABORT, ABORT)` / `IllegalArgumentException` |
| D11 | チェックイン: 航空機の呼び出し符号が空 | 呼びかけの行が `Hawg 11, this is （未入力）`、`missingItems` に `Aircraft callsign` |
| D12 | ゲームプラン: `Razor 53`、`TYPE_2`、`BOC`、追加の情報なし | `Razor 53, Type 2, bomb on coordinate; advise when ready for 9-line.` |
| D13 | ゲームプラン: `TYPE_1`、`BOT`、intent と fuzing を入れる | 追加の情報が F02 の順に `, ` でつながる |
| D14 | ゲームプラン: 種類と方法が空、呼び出し符号なし | `（未入力）` が 2 つ、`missingItems` が `[Type of control, Method of attack]`、1 行目が `Advise when ready for game plan.` |
| D15 | Restrictions: 全項目を入れ、`tot = TIME`・`totTime = 1430` | `[Restrictions]` が `[Remarks]` の後ろ、最後の注意の行の前。2.2.4 の順で、TOT の行が最後。各行に `(readback)` |
| D16 | Restrictions: `tot` を `STANDBY`・`AFTER_CORRELATION`・`PUSH_WHEN_READY` にする | それぞれ 2.2.4 の文 |
| D17 | Restrictions: `TIME` で `totTime` が空 / `STANDBY` で `totTime` あり | `TOT （未入力）` と `missingRestrictions` に `TOT` / `IllegalArgumentException` |
| D18 | Restrictions: `dangerClose` でイニシャルが空 / イニシャルだけ入れる | `missingRestrictions` に `Danger close initials` / `IllegalArgumentException` |
| D19 | 3 つの引数の `format` に空の `CasRestrictions` | 2 つの引数の `format` と同じ文字（`[Restrictions]` なし）。`missingRestrictions` は空 |
| D20 | 各 `format` に null | `IllegalArgumentException` |
| D21 | 各書式の値に改行・前後の空白・全角の空白 | 02 の 2.5 と同じに整える |
| D22 | 既存の C1〜C23 | 変えずに通る |

## 3. app（`jp.jtak.plugin.cas`）

- `JtakDropDownReceiver` の今のボタン（「LRF」「9-line」）の横に「Routing」「Check-in」「Game plan」を足す。各画面は CasBriefPanel と同じ作り（入力の欄・読み上げ用の表示・上の注意の行が見えたまま。下の行はなくした）にする。
- 9-line の画面（`CasBriefPanel`）に Restrictions の欄を足し、3 つの引数の `format` を呼ぶ。
- 欄の名前は F02 の英語の名前にし、短い日本語の説明を添える（02 の 3.1 と同じ。U5 を守る）。
- 選ぶ欄（指示の種類・結び・チェックインのモード・Type・BOT/BOC・TOT の種類）は、「選ばない」を既定にする。
- ルーティングの場所は、手入力のほか「地図の点から入れる」で `MapPointLister` の一覧から選べる。選んだ点の名前（callsign）を `place` に入れる。一覧の外し方（02 の 3.2）は変えない。
- ゲームプランの Type・BOT/BOC の欄の説明に、図 III-7 の要点を出典の言い方から短く訳して出す（読み上げの文には入れない）。説明の文は次のとおり（S4 の図 III-7、PDF 114 ページの表を訳したもの）。
  - Type 1: 「統制する側が、攻撃の最終段階で目標と攻撃機を目で捉え、攻撃ごとに統制する」
  - Type 2: 「統制する側が、目視以外の手段で危険を減らし、攻撃ごとに統制する」
  - Type 3: 「統制する側が、目視以外の手段で危険を減らし、同じ交戦の中で複数の攻撃を認める」
  - BOT: 「機が、目標または狙う点を自分で捉える」
  - BOC: 「機が、CAS Brief で渡された座標に対して攻撃する」
  - 欄の上に共通の注記「Type と BOT/BOC は別々に選ぶ（どの組み合わせでもよい）。訓練用の要約」を出す（S4 の III-47 の b）。
- 必須の欄が空のとき（`missingItems`・`missingRestrictions`）は、02 と同じく画面に知らせる。
- 出すのは画面だけ。書き出しは F04（`05-export.md`）。送る API は呼ばない（U8）。新しい ATAK の API を使うときは、先に SDK の javadoc で確かめる。

## 4. 確かめ方

- `bash software/atak-plugin/build.sh assembleDebug :core:test` が通り、試験の失敗が 0 件。D1〜D22 が入っている。
- 実機の確かめ（画面で各書式が読めること）は、利用者の許可を得て行う（U6）。

## 5. 未決

- 実機での見え方（ボタンの数が増えたときの並び、Restrictions の欄で画面が長くなること）は、実機で確かめてから直す。
