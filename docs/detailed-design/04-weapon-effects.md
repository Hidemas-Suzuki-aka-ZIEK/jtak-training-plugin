# 詳細設計 04: RED（危険見積距離）の表示（編集できる表）

画面での呼び方は「RED（危険見積距離）」（2026-11-09 利用者の決定。F03「呼び方」、`docs/usage-scope.md` Q12）。
この文書の「兵器の範囲」「範囲の円」は、その機能と円を指す。値は訓練用サンプル値のまま。

- 日付: 2026-11-08
- 機能仕様: `docs/functional-spec/F03-weapon-effects.md`
- 出典: なし（既定の表の値は、意味のない切りのよい数。2.4）
- 関連: `docs/usage-scope.md` U1・U2・U3・U5・U8・Q3・Q9、`docs/interfaces/README.md` 2 章（weapons.json の形）、
  SDK の javadoc（`docs/references.md` の S1。DrawingCircle・MapGroup・FileSystemUtils）、
  `docs/detailed-design/02-cas-brief.md`（MapPointLister・PanelKit・画面の作り）、ADR 0003

## 1. 範囲

| する | しない（ほかのタスク・しないこと） |
|---|---|
| core: weapons.json の読み書き（依存なしの JSON）、形と値の検査、既定の表、編集の欄から行を作る処理、味方の点が円の中にあるかの判定、JUnit の試験 | 実在の兵器の名前・諸元を既定の表に入れる（U3・Q3） |
| app: 兵器の範囲の画面（目標・行を選ぶ、描く・消す、味方の点の結果）、表の編集（行の保存・追加・削除・既定に戻す・再読み込み）、表のファイルの読み書き、円を地図に描く | 円を JTAK から送る・ネットワークへ出す（U8・Q9） |
| | 円を ATAK の再起動のあとに残す（Q9）、目標が動いたときに円を動かす |
| | テキスト・PDF の書き出し（F04） |

- 表の値はすべて「訓練用サンプル値」で、実際の安全距離・危害半径ではない（U2）。この文言は core の定数から出し、画面・円の説明に必ず入れる。外す設定は作らない（U1）。
- 既定の表は汎用の区分だけにする（U3。Q3 は未確認のため）。JTAK は行の名前の候補を出さない。利用者が編集の画面で書いた名前は、利用者の入力として扱う。
- 円は JTAK が自分から送らない。利用者が ATAK の操作で送ることはできる（Q9）。

## 2. core（`jp.jtak.core.weapons`）

### 2.1 型

すべて不変（final の欄と、変えられないリスト）。

| 型 | 欄 | 決まり |
|---|---|---|
| `WeaponRing` | `String name`、`int radiusM` | |
| `WeaponEntry` | `String id`、`String label`、`List<WeaponRing> rings`、`String note`（空文字のことがある。null にはしない） | `rings` は内側から外側の順（`radiusM` の小さい順） |
| `WeaponTable` | `List<WeaponEntry> entries` | `notice` と `trainingSampleValues` は型に持たない（2.3） |
| `WeaponTableResult` | `WeaponTable table`（検査に通らなければ null）、`List<String> errors` | `errors` は空なら成功。理由は日本語で、場所の道筋を頭に付ける（2.3） |

### 2.2 JSON の読み書き（`WeaponJson`。外からは `WeaponTableCodec` を使う）

core は依存を持たない（ADR 0003。org.json も使わない）ので、小さな JSON の読み手と書き手を作る。

**読み手**（厳しく読む。RFC 8259 の文法）:

- 値: オブジェクト・配列・文字列・数・`true`・`false`・`null`。
- 文字列: エスケープ `\" \\ \/ \b \f \n \r \t \uXXXX`。サロゲートの組は 1 文字に組み立てる。組になっていないサロゲート、エスケープしていない制御文字（U+0000〜U+001F）はエラー。
- 数: JSON の文法どおり（先頭の `0` の後に数字は続かない、`+` は付かない）。`BigDecimal` として読む。
- 同じオブジェクトの中の同じキー、値の後に続く空白以外の文字、入れ子の深さが 10 を超えるもの、入力が 256 KiB（262144 バイト）を超えるものはエラー。
- 先頭の BOM（U+FEFF）は取り除く。入力は UTF-8 のバイト列として受け取り、正しくない UTF-8 はエラーにする。
- 文法のエラーは「`{行}行{列}列: {理由}`」の 1 件だけを返す（それ以上は読まない）。

**書き手**:

- UTF-8、インデントは空白 2 つ、改行は `\n`、末尾に改行 1 つ。キーの順は `docs/interfaces/README.md` 2 章の例の順（`schemaVersion`・`notice`・`trainingSampleValues`・`entries`。行は `id`・`label`・`rings`・`note`。輪は `name`・`radiusM`）。
- 文字列は `"` `\` と制御文字だけをエスケープする（制御文字は `\n` などの短い形があればそれ、なければ `\u00XX`）。日本語などはそのまま書く。
- `notice` には常に `WeaponTableCodec.FILE_NOTICE`、`trainingSampleValues` には常に `true` を書く（2.3）。
- `note` が空の行は `note` を書かない。

`WeaponTableCodec` の入口:

- `static WeaponTableResult parse(byte[] utf8)`: 読んで検査する。
- `static String write(WeaponTable table)`: 書く。引数が null なら `IllegalArgumentException`。
- `static List<String> validate(WeaponTable table)`: 型の表を、読んだときと同じ値の決まりで検査する（保存の前に使う）。

### 2.3 形と値の検査

正本の形は `docs/interfaces/README.md` 2 章。ここに検査の細かい決まりを書く。

| 道筋 | 決まり |
|---|---|
| （全体） | オブジェクト。知らないキーはエラー |
| `schemaVersion` | 数の `1`（整数の書き方。`1.0` はエラー） |
| `notice` | 空でない文字列。**画面にも円の説明にも出さない**。画面・円に出す文言は core の定数（2.6）だけ。保存のたびに app が `FILE_NOTICE` で上書きする |
| `trainingSampleValues` | `true`（`false` や文字列はエラー） |
| `entries` | 配列で、1〜50 行 |
| `entries[i]` | オブジェクト。知らないキーはエラー |
| `entries[i].id` | 空でない文字列、64 文字まで、英小文字・数字・`-` だけ。表の中で重ならない |
| `entries[i].label` | 空でない文字列（前後の空白を除いて）、40 文字まで |
| `entries[i].rings` | 配列で、1〜3 個 |
| `entries[i].rings[j]` | オブジェクト。知らないキーはエラー |
| `entries[i].rings[j].name` | 空でない文字列（前後の空白を除いて）、20 文字まで |
| `entries[i].rings[j].radiusM` | 整数の書き方の数（小数点・指数のない形。`100.0`・`1e2` はエラー）で、1〜50000。同じ行の中で、前の輪より大きい（同じ値もエラー） |
| `entries[i].note` | なくてよい。あれば文字列で、200 文字まで |

- 文字数は Unicode の符号位置の数で数える（`String.codePointCount`）。絵文字 1 つは 1 文字。
- 文字列の値は前後の空白を取って持つ（`id` は取らずに検査する）。取るのは `String.trim` が取る文字（半角の空白・タブ・改行など）だけで、全角の空白（U+3000）は取らない（2026-11-09 利用者の決定）。
- エラーの理由は日本語で、道筋を頭に付ける。例: `entries[1].rings[0].radiusM: 1 から 50000 までの整数にしてください`、`entries[2].id: ほかの行と同じ id です（sample-small）`。
- 値のエラーは見つかった順に最大 20 件まで集める。1 件でもあれば `table` は null。

### 2.4 既定の表（`DefaultWeaponTable.table()`）

| id | label | 輪（name / radiusM） |
|---|---|---|
| `sample-small` | サンプル 小 | 内側 / 100、外側 / 300 |
| `sample-medium` | サンプル 中 | 内側 / 250、外側 / 600 |
| `sample-large` | サンプル 大 | 内側 / 500、外側 / 1000 |

- 値には出典がない。意味のない切りのよい数で、実在の兵器とも、実際の安全距離・危害半径とも関係がない（2026-11-08 利用者の決定「案A 100/300・250/600・500/1000 m」）。
- `note` は 3 行とも `訓練用サンプル値。実在の兵器の値ではない`。
- 既定の表は 2.3 の検査に通る（試験 W1）。

### 2.5 編集の欄から行を作る（`WeaponEntryForm`）

画面の編集の欄（3.3）は文字のままで core に渡し、core が行にする。

- `static FormResult toEntry(String id, String label, String[] ringNames, String[] ringRadii, String note)`:
  `ringNames` と `ringRadii` は長さ 3（内側から）。null の要素は空と同じ。
  1. 名前と距離の両方が空の枠は捨てる。片方だけが空の枠はエラー（`輪 {n}: 名前と距離の両方を入れてください`）。
  2. 距離は前後の空白を取り、半角の数字だけ（符号・小数点なし）で 1〜50000 の整数。そうでなければエラー（`輪 {n}: 距離は 1 から 50000 までの整数（m）にしてください`）。
  3. 残った枠が 1 つ以上で、距離が内側から大きくなる順であること。
  4. ラベル・輪の名前・メモの文字数は 2.3 と同じ。
  5. 前後の空白の取り方も 2.3 と同じ（`String.trim`。全角の空白は取らない。2026-11-09 利用者の決定）。
  6. 距離が数として読めた枠は、その枠に名前のエラーがあっても、次の枠の「前より大きい」の比べる相手にする（エラーを 1 回で全部出すため）。
- `FormResult` は `WeaponEntry entry`（エラーなら null）と `List<String> errors`。
- `static String nextId(WeaponTable table)`: `row-1`、`row-2`… のうち、表で使っていないいちばん小さい番号の id を返す。

### 2.6 文言（`TrainingNotice`）

- `TrainingNotice.text()`（今のまま）: `TRAINING USE ONLY / 訓練用`。
- `TrainingNotice.weaponSampleText()`（足す）: `訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA`（`docs/usage-scope.md` 3 章）。
- `WeaponTableCodec.FILE_NOTICE`: `TRAINING USE ONLY / 訓練用。訓練用サンプル値であり、実際の安全距離・危害半径ではない。` ファイルに書くだけで、画面には出さない。

### 2.7 味方の点が円の中にあるか（`FriendlyProximity`）

core は ATAK の型を知らないので、点は core の型で受け取る。

- `PointRef`（不変）: `String uid`、`String name`、`String cotType`（null は空）、`double lat`、`double lon`、`boolean self`（自分の点）。
- `static ProximityResult check(WeaponEntry entry, PointRef target, List<PointRef> points)`:
  - 味方とみなす点: `self` が true の点、または `cotType` が `a-f-` で始まる点。
  - `target` と同じ `uid` の点は除く。
  - 距離は `jp.jtak.core.lrf.Geodesy.inverse(target, 点)` の距離（m）。距離 ≤ その行のいちばん外側の輪の `radiusM` なら中にある。
  - 中にある点ごとに、点が入るいちばん内側の輪（距離 ≤ `radiusM` となる最初の輪）と距離を返す。距離の近い順に並べる。
  - `Geodesy.inverse` が `IllegalStateException` を投げた点は、中にあるとはみなさず、`unknownCount` に数える。
  - 試験のために、距離の計算を差し替えられる形（`check(..., DistanceFunction)`）も用意する。
- `ProximityResult`: `List<ProximityHit> hits`（`uid`・`name`・`ringIndex`・`ringName`・`distanceM`）、`int unknownCount`。

### 2.8 設計の決定

ドキュメント・出典で決まっていなかったことを、ここで決めた。

| 決めたこと | 理由 |
|---|---|
| 輪は 1 行に 3 つまで、距離は 1〜50000 m の整数 | 色を赤・橙・黄の 3 色にした（利用者の決定）。50 km を超える範囲は訓練の画面で扱わない |
| `notice` は検査するが画面に出さない | 利用者が書き換えられる文字で、訓練用の表示を消せないようにする（U1） |
| 境目（距離 = 半径）は中に入れる | 味方の点を見落とさない側に倒す |
| 距離を計算できない点は「中にない」と数え、件数を画面に出す | 黙って捨てない |
| 文字数は符号位置で数える | 画面の見た目の文字数に近い |

### 2.9 試験（JUnit。`core/src/test/java/jp/jtak/core/weapons/`）

| ID | 試験 |
|---|---|
| W1 | 既定の表が検査に通り、行・輪・値が 2.4 の表のとおり |
| W2 | 既定の表を書いて読むと、同じ表に戻る（書き手の出力は 2.2 の形。`notice` は `FILE_NOTICE`） |
| W3 | 同じキー、値の後の文字、深さ 11、256 KiB を超える入力、正しくない UTF-8、組になっていないサロゲートがそれぞれエラー（行と列が付く） |
| W4 | BOM 付きの入力を読める。`\u` のエスケープとサロゲートの組を正しく読む |
| W5 | 2.3 の表の各行の決まりを破った入力が、それぞれ道筋付きのエラーになる（`schemaVersion` 2、`trainingSampleValues` false、行 0・51 行、id の重なりと使えない文字、ラベル 41 文字、輪 0・4 個、名前 21 文字、`radiusM` 0・50001・`100.0`・`1e2`・同じ値・小さくなる順、`note` 201 文字、知らないキー） |
| W6 | 絵文字を含むラベル 40 符号位置は通り、41 はエラー |
| W7 | `notice` を書き換えたファイルを読んで書き直すと、`notice` が `FILE_NOTICE` になる |
| W8 | 書き手が `"`・`\`・改行・タブ・U+0001 をエスケープし、日本語はそのまま書く |
| W9 | `WeaponEntryForm.toEntry`: 空の枠を捨てる、片方だけの枠はエラー、距離の書き方のエラー、大きくなる順でないとエラー、枠が全部空でエラー |
| W10 | `nextId`: 空き番号のいちばん小さいものを返す（`row-1` と `row-3` があれば `row-2`） |
| W11 | `FriendlyProximity`: 内側の中・外側の中・外・境目ちょうど（中）・目標と同じ uid（除く）・敵の点（除く）・自分の点（`self`、type が空でも味方）・計算できない点（`unknownCount`） |
| W12 | `TrainingNotice.weaponSampleText()` が 2.6 の文言と同じ |

## 3. app（`jp.jtak.plugin.weapons`）

### 3.1 使い回すもの

- `jp.jtak.plugin.cas.MapPointLister` と `PanelKit` を `public` にし、`weapons` から使う（中身は変えない。使うメソッドも `public` にする）。core の表の行数の上限（`MAX_ENTRIES`）も、画面で 50 行の上限を見るために `public` にした。
- 目標の候補と味方の点は、どちらも `MapPointLister.list(mapView)` から取る。自分の点（`mapView.getSelfMarker()`）は `PointRef.self = true` にする。
- 円の部品（3.4 の DrawingCircle と、もし作られれば中心の点）は、目標の候補に出さない（4 章で確かめる）。
  出るときは、`MapPointLister` で uid が `jtak-wpn-` で始まる点と、meta `shapeUID` が `jtak-wpn-` で始まる点を除く。

### 3.2 画面（`WeaponPanel`。メニューのボタン `mode_weapons` で開く節 `weapons_section`）

上から順に:

1. `TrainingNotice.weaponSampleText()` を、ほかの文より目立つ形（太字）で出す。常に出し、隠す操作は作らない。
2. 表の状態の 1 行: 「表: 既定（ファイルなし）」「表: ファイル（{n} 行）」「表のファイルが正しくないので既定の表を使っています」のどれか。正しくないときは、その下に 2.3 の理由を最大 5 件と「ほか {n} 件」を出す。
3. 目標（スピナー。`MapPointLister` の点）と「点の一覧を更新」のボタン。
4. 行（スピナー。`label` を出す）。選んだ行の輪を「内側 100 m / 外側 300 m」の形で 1 行に出す。
5. 「描く」「消す」のボタン。
6. 結果の文: 描いたあと、味方の点の判定（2.7）を出す。
   - 先頭の行は `TrainingNotice.text()`。
   - 中にある点があれば「RED の円の中の味方の点:」と、1 点ごとに「{名前} — {輪の名前}（{距離} m）」。距離は整数の m（四捨五入）。
   - なければ「RED の円の中の味方の点: なし」。`unknownCount` が 1 以上なら「距離を計算できなかった点: {n}」。
7. 編集の節（3.3）。

節を開くたびに（`onShown`）、表のファイルを読み直し、点の一覧を更新する。

### 3.3 表の編集

選んだ行を、次の欄で編集する: ラベル、輪 1〜3 の名前と距離（m）、メモ。距離の欄は数字のキーボード。

| ボタン | 動き |
|---|---|
| 行を保存 | `WeaponEntryForm.toEntry` で行にし、表の中の同じ id の行と入れ替え、表全体を `validate` してからファイルに書く（3.5）。エラーなら書かずに理由を出す |
| 行を足す | `nextId` の id で、ラベル「新しい行」・輪 1 つ（「内側」100 m）の行を表の末尾に足して書き、その行を選ぶ。表が 50 行なら押せない |
| 行を消す | 確認の画面（`AlertDialog`。`mapView.getContext()`）で「{label} を消します」を出し、「実行」なら消して書く（「やめる」なら何もしない）。表が 1 行なら押せない |
| 既定の表に戻す | 確認の画面で「表を既定の 3 行に戻します。いまの行は消えます」を出し、「実行」なら既定の表をファイルに書く（ファイルは消さない。「やめる」なら何もしない） |
| 再読み込み | ファイルを読み直す（3.5）。編集中の欄は捨てる |

確認の画面のボタンは「実行」（`weapons_ok`）と「やめる」（`weapons_cancel`）。行を選んでいない（表に行がない）ときに「行を保存」「描く」を押したら、結果の欄に「表の行がありません」（`weapons_no_row`）を出す。

- 書いたあと、画面の表とスピナーを、書いた表で作り直す。
- 書き込みに失敗したら、「表を保存できませんでした」と理由を出す。ファイルの残り方は 3.5 の「書く」の手順 4 のとおり。
- 理由は、原因を表す値（enum）を strings.xml の文言に変えて出す（3.6）。前のファイルを `weapons.invalid-*.json` の名前で残したときは、その名前も出す。
- 書き込みに失敗したあとは、表の状態の欄（3.2 の 2）だけをファイルの読み直し（`WeaponTableFile.read()`）で作り直す。画面の表・行の一覧・入力欄・失敗のメッセージには触らない（入力した値と失敗の理由を消さないため。`reloadTable` は使わない）。2026-11-09 の決定（レビューの指摘）。

### 3.4 円を描く

- 「描く」は、選んだ目標の点の位置を中心に、選んだ行の輪ごとに 1 つの `DrawingCircle` を作る（輪の数は 1）。
- 「描く」を押したときに `MapPointLister.list(mapView)` を取り直し、選んでいる目標の uid で点を引き直す。見つからなければ描かずに `weapons_no_target` の文言を出す。円の中心と味方の判定（2.7）は、この取り直した一覧の位置で行う（画面を開いたあとに点が動いても、古い位置で判定しないため。2.8 の「見落とさない側に倒す」）。
- uid: `jtak-wpn-` + `UUID.randomUUID()`。
- 線の色（ARGB）: 内側から 1 つ目 `0xFFFF0000`（赤）、2 つ目 `0xFFFF8C00`（橙）、3 つ目 `0xFFFFFF00`（黄）。塗り `0x00000000`（なし）。線の太さは ATAK の既定のまま。
- 題名（`setTitle`）: `訓練用 {label} {輪の名前} {radiusM} m`。
- 説明（meta `remarks`）: 次の行を改行でつなぐ。
  ```
  TRAINING USE ONLY / 訓練用
  訓練用サンプル値 — 実際の安全距離・危害半径ではない / TRAINING SAMPLE VALUES — NOT REAL SAFETY DATA
  目標: {目標の点の名前}
  {note}（空なら行ごと出さない）
  ```
  1・2 行目は `TrainingNotice.text()` と `weaponSampleText()` から出す。
- 中心の点は JTAK からは作らない（`setCenterMarker` を呼ばない）。
- 置くグループ: `mapView.getRootGroup().findMapGroup("Drawing Objects")`。見つからなければ、ルートに `JTAK 訓練用の RED` のグループを作って使う。
- 残さない: `persist()` を呼ばない。meta `archive` を false にする。`setNeverPersist` は使わない（使うと送る操作も止まる）。
- 送らない: JTAK は送る API（CoT の送信など）を呼ばない。利用者は ATAK の円の操作で送れる（Q9）。
- 「描く」を押すたびに円を足す（前の円は消さない。2026-11-09 利用者が確かめた）。作った円は `WeaponPanel` が覚えておく。
- 「消す」は、覚えている円をすべてグループから取り除く（`removeItem`）。利用者が ATAK の操作で消した円は、取り除くときに飛ばす。
- プラグインが閉じられたとき（`JtakDropDownReceiver.disposeImpl`）も、覚えている円を取り除く。
- 目標の点が動いても円は動かさない。
- 失敗したとき: 「描く」「消す」「点の一覧を更新」（`onShown` での更新も含む）で ATAK の API が `RuntimeException` を投げたら、ATAK を落とさずにログに残し、結果の欄に「円を描けませんでした」「円を消せませんでした」「点の一覧を取れませんでした」のどれかと理由（例外のメッセージ）を出す。描く途中で失敗したら、その回に足した円だけを取り除き、前の回までの円は残す。取り除けなかった円（`removeItem` が例外を投げた、または取り除いたあともグループに入っている）は覚えておき、次の「消す」とプラグインが閉じられたときにもう一度取り除く（2026-11-09 のレビュー）。
- 「消す」とプラグインが閉じられたときは、1 つの円を取り除けなくても残りの円の取り除きを続ける。取り除けなかった円は覚えたままにし、最後に最初の例外を投げ直す。「消す」では「円を消せませんでした」と理由を出し、閉じるときはログだけに残す（2026-11-09 のレビュー）。

### 3.5 表のファイル

- 場所: `FileSystemUtils.getItem("tools/jtak/weapons.json")`。
- 読む:
  - ファイルがない: 既定の表を使う。ファイルは作らない（初めて保存したときに作る）。
  - 読めて検査に通る: その表を使う。
  - 読めない・検査に通らない: 既定の表を使い、3.2 の 2 に理由を出す。ファイルはそのまま残す。
- 書く:
  1. 親のフォルダ（`tools/jtak/`）がなければ作る（`mkdirs`）。
  2. `weapons.json.tmp` に書く。書けなければ tmp を消し、前のファイルには触れずに失敗にする。
  3. 今のファイルが検査に通らないものなら、同じフォルダに `weapons.invalid-YYYYMMDD-HHmmss.json` として名前を変えて残す。
  4. tmp を `weapons.json` に名前を変える。失敗したら tmp を消す。3 で名前を変えていれば、前のファイルは `weapons.invalid-*.json` の名前で残る（`weapons.json` はない）。失敗の表示（3.3）には、残したファイルの名前も出す。
- 書くのは `WeaponTableCodec.write` の出力だけ（`notice` と `trainingSampleValues` は core が入れる）。
- 表のファイルの読み書きは UI スレッドで行ってよい。ファイルの大きさの上限が 1 MiB で、`read()` は読めないときを例外にせず `INVALID_USING_DEFAULT` として返すため。読み込みの呼び出し（`reloadTable`）に `RuntimeException` を受け止める try は足さない。2026-11-09 の決定（レビューで未決だった点）。

### 3.6 文字

画面の文字は `res/values/strings.xml` に置く。訓練用の文言（`TrainingNotice` の 2 つ）は strings.xml に置かず、core から出す（U1。ADR 0003）。
3.4 が文字どおりに決めている文言（題名の `訓練用 `、説明の `目標: `、グループの名前 `JTAK 訓練用の RED`）と、3.3・3.4 の失敗の表示も strings.xml に置き、文言は 3.3・3.4 のとおりにする（2026-11-09 の決定）。
`WeaponTableFile` などが投げる例外のメッセージは、そのまま画面に出さない。原因を表す値（enum など）にし、`WeaponPanel` が strings.xml の文言に変えて出す。
実在の製品・組織・兵器の名前は、画面の文字・円の題名と説明・既定の表に入れない（U3・U5）。

## 4. 確かめ方

- `bash software/atak-plugin/build.sh assembleDebug :core:test` が通る。W1〜W12 が入る。
- 実機での確かめ。`adb install` などは、利用者に確認してから行う。
  1. 円を描くと、線だけの赤・橙・黄の円が目標の周りに出る。題名と説明（訓練用の 2 行を含む）が ATAK の画面に出る。
  2. 円の中心の点が地図に出ないか、出ても目標の候補に入らない（3.1）。
  3. 円に ATAK の送る操作がある。JTAK は自動で送らない（ほかの端末・サーバーに届かない）。
  4. **ATAK を立ち上げ直すと、円が消えている**（Q9 の「残さない」）。
  5. ATAK の操作で円を編集（色などを変える）してから立ち上げ直しても、円が消えている。
  6. `tools/jtak/` がない状態で保存すると、フォルダとファイルができる。ファイルを壊すと、画面に理由が出て既定の表が使われる。
  7. 味方の点を円の中と外に置き、結果の文が合う。
- 4 か 5 で円が残るときは、置くグループを `Drawing Objects` から `JTAK 訓練用の RED`（3.4。JTAK が作るグループ）に変えて、もう一度確かめる。それでも残るときは利用者に確認する。
- 実機での結果（2026-11-09。実機に `adb install`。利用者「確認した．問題ない」）: 兵器の節で表から選んで円を描ける、円が目標の周りに出る、画面と円の説明に `TRAINING USE ONLY / 訓練用` と訓練用サンプル値の文言が出る、「消す」で円が消えてほかの点は残る。上の 1 にあたる。2〜7 はまだ確かめていない。

## 5. 未決

- `FileSystemUtils.getItem` が返す実際の場所と、フォルダがないときの動き（4 章の 6）。
- DrawingCircle が自分で中心の点を作るか（4 章の 2）。
- 円の説明（remarks）が ATAK の円の画面に出るか（4 章の 1）。出ないときは、題名に `訓練用` を残したうえで、出し方を利用者に確認する。
