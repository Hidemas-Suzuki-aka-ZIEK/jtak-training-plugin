# 詳細設計 02: 9-line と Remarks（CAS Brief）の生成

- 日付: 2026-11-08
- 機能仕様: `docs/functional-spec/F02-cas-products.md`（「9-line」「Remarks」の表）
- 5 行目の下書き: 2.3.1
- 根拠: SDK の javadoc（`docs/references.md` の S1）
- 出典: `docs/references.md` S4（JP 3-09.3 2014 年版の図 V-9 と V-25〜V-31）、S2（Vincenty の式）・S3（試験の値の例）、S6（MIL-STD-2525E w/Change 1。5 行目の下書きの言葉）・S7（MIL-STD-2525C。CoT の type の文字と名前の対応）
- 関連: `docs/usage-scope.md` U1・U4・U8、`docs/detailed-design/01-lrf-marking.md`（Geodesy・偏差の取り方）、ADR 0003

## 1. 範囲

| する | しない（ほかのタスク） |
|---|---|
| core: 方位と距離の逆解法、2・3・4・8 行目の自動の値の計算、9-line と Remarks の文字の並び、JUnit の試験 | ルーティング・チェックイン・ゲームプラン・Restrictions（`03-cas-other-products.md`） |
| app: 地図の点の選び方、6 行目の MGRS・4 行目の MSL・偏差を ATAK と Android から取る、手入力の欄、読み上げ用の表示 | テキスト・PDF の書き出し（F04） |
| | 目標が複数のとき・GEOREF・動く目標の書き方、緯度経度の自動の出力（手入力の欄で書ける） |

- 航空機（の役）やネットワークへは送らない（U8）。出すのは画面だけ。
- 出力のすべてに `TrainingNotice.text()` を入れる（U1）。外す設定は作らない。
- Restrictions は Remarks の後ろに `[Restrictions]` の節として足す。足し方（`format` の 3 つの引数の形）は `03-cas-other-products.md` 2.2 で決める。ゲームプランは CAS Brief とは別の文字の並びにする（利用者の決定 2026-11-08）。

## 2. core（`jp.jtak.core.cas`。ATAK と Android に依存しない純 Java）

### 2.1 方位と距離の逆解法（`jp.jtak.core.lrf.Geodesy` に足す）

- `public static double[] inverse(double lat1Deg, double lon1Deg, double lat2Deg, double lon2Deg)`。
  戻り値は `{距離(m), 始点での前方位(度。真北から時計回り、[0, 360))}`。
- Vincenty（1975）の逆解法（出典 S2）。楕円体は WGS84 で、`direct` と同じ定数を使う。
- 反復は λ の変化が 1e-12 rad 未満になるまで。上限は 200 回で、超えたら `IllegalStateException`。
- 2 点が同じなら `{0, NaN}`。対蹠点の近くは対象外と javadoc に書く。
- `direct` の動きは変えない（01 の試験はそのまま通ること）。

### 2.2 型

| 型 | 中身 |
|---|---|
| `enum AttackPointType` | `IP`（3 行目を NM で出す）・`BP`（3 行目を m で出す） |
| `enum Cardinal` | `NORTH("North")`・`NORTHEAST("Northeast")`・`EAST("East")`・`SOUTHEAST("Southeast")`・`SOUTH("South")`・`SOUTHWEST("Southwest")`・`WEST("West")`・`NORTHWEST("Northwest")`。`static Cardinal fromTrueBearing(double deg)`、`String word()` |
| `final class NineLine` | 9 行の値（文字列。空か null は未入力）と、2 行目に付ける `offset`（文字列。空か null なら付けない）。ビルダーで作り、不変 |
| `final class CasRemarks` | Remarks の 5 項目（F02 の Remarks の表の順。`ltlPtl`・`ordnance`・`threat`・`additionalRemarks`・`additionalCalls`）。空か null は出さない。ビルダーで作り、不変 |
| `final class CasBriefResult` | `text`（2.5 の文字の並び）・`missingLines`（未入力の行の番号の昇順の、変えられないリスト） |
| `final class MgrsText` | `static String clean(String s)`: MGRS の文字列から U+200E（LEFT-TO-RIGHT MARK）と U+200F（RIGHT-TO-LEFT MARK）をすべて取り除き、前後の空白（全角空白を含む）を取る。null は null、空になったら null（5.1） |

位置は `jp.jtak.core.lrf.GeoPosition` を使う。

### 2.3 自動の値の計算（`final class NineLineCalculator`。すべて static で文字列を返す。出せないときは null）

| メソッド | 計算 | 例 |
|---|---|---|
| `headingFromBearing(double trueBearingDeg, double declinationDeg)` | 磁方位 = 真方位 − 偏差（偏差は東が正。01 の 3.3 の「真 = 磁 + 偏差」と同じ向き）。[0, 360) に直し、整数に四捨五入する。0 と 360 は「360」と書き、ほかは 3 桁にゼロで埋める | 真 0.2・偏差 0 → `360`、真 5・偏差 −7 → `012` |
| `heading(GeoPosition attackPoint, GeoPosition target, double declinationDeg)` | `Geodesy.inverse` の前方位で `headingFromBearing` を呼ぶ | — |
| `distanceText(double meters, AttackPointType type)` | `IP`: m ÷ 1852 を小数 1 桁に四捨五入して `N.N NM`。`BP`: 100 m 単位に四捨五入して `N meters` | 54972.271・IP → `29.7 NM`、1249・BP → `1200 meters`、1250・BP → `1300 meters` |
| `distance(GeoPosition attackPoint, GeoPosition target, AttackPointType type)` | `Geodesy.inverse` の距離で `distanceText` を呼ぶ | — |
| `elevationFeet(double targetMslMeters)` | m ÷ 0.3048 を整数に四捨五入して `N feet` | 381.0 → `1250 feet` |
| `friendlies(GeoPosition target, List<GeoPosition> friendlies)` | いちばん近い味方を選び、目標から見た真方位の方角（`Cardinal.word()`）と、整数の m に四捨五入した距離で `South 300` の形にする | — |

- 四捨五入は `Math.round` と同じ（0.5 は上へ）。数字の書式は `Locale.ROOT`（小数点は `.`）。
- 入力が null・NaN・無限大・範囲外（緯度は ±90、経度は ±180 を超える）のとき、2 点が同じで方位が出ないとき、味方のリストが空のときは null を返す（例外にしない）。偏差が NaN のときも `heading` は null。リストの中の null や範囲外の点は飛ばす。
- `Cardinal.fromTrueBearing` の区切り: 北 = [337.5, 360) と [0, 22.5)、北東 = [22.5, 67.5)、以下 45° ごと。入力は [0, 360) に直してから使う。NaN なら null。
- 8 行目の方角は真北を基準にする。出典は真か磁かを書いていない（V-29–V-30）。利用者が「そのままでよい」と確かめた（2026-11-08）。
- 2 行目で 0 を「360」と書く（出典は「3 桁で言う」だけ）。01 の方位の表示（000.0）とは別の決まり。利用者が「そのままでよい」と確かめた（2026-11-08）。
- 8 行目の距離を何 m 単位で丸めるかは出典にない。整数の m にする（利用者が 2026-11-08 に確かめた）。
- `friendlies`: 味方のリストに目標と同じ点（`Geodesy.inverse` の距離が 0 で方位が NaN）が 1 つでもあれば、ほかの味方があっても null を返す。方角が決まらない点を飛ばして遠い味方を出すと、いちばん近い味方が、気づかないうちに 8 行目から消えるため（レビューの指摘。利用者が 2026-11-08 に確かめた）。null の 8 行目は未入力の行に出る。
- `Geodesy.inverse` は 200 回で収束しないとき（対蹠点の近く）`IllegalStateException` を投げる。`NineLineCalculator` はこれを捕まえて null を返す（ATAK を落とさない）。

### 2.3.1 5 行目の下書き（`final class TargetDescriptionDraft`、`static String fromCotType(String cotType)`）

目標に選んだ点の種類（CoT の type）から、5 行目（Target description）の下書きの言葉を返す。利用者の決定（2026-11-08。`docs/usage-scope.md` Q8）: 下書きを入れるのは敵の点だけ。言葉は公開の MIL-STD-2525 の名前から JTAK が作った表で決める。数・姿勢・色などは利用者が書き足す。

- 言葉は MIL-STD-2525E w/Change 1（S6）の陸上の部隊の表 TABLE A-XXIII と装備の表 TABLE A-XXIX の名前。
- E の符号は数字なので、CoT の type の文字（`a-h-` の後ろを 1 字ずつ `-` で区切ったもの）との対応は、MIL-STD-2525C（S7）の TABLE A-III（SIDC の表）で取る。type の `a-h-` の後ろの文字は、2525C の SIDC の 3 字目（次元）と 5 字目からの機能の符号の文字に当たる（例: `a-h-G-U-C-I` = `S*G*UCI---` = INFANTRY）。ATAK の `Icon2525cTypeResolver.mil2525cFromCotType` も同じ対応で 2525C の符号を作る。
- 2525C の名前は対応を確かめるためだけに使い、下書きの言葉にはしない。記号の絵は取り込まない。
- 下の表のページは、PDF のページ／本文に印刷されたページ。

陸上の部隊（E Change 1 の TABLE A-XXIII）

| CoT の type の頭 | 2525C の機能の符号 | 2525C の名前（PDF／印刷） | 下書きの言葉（E Change 1。PDF／印刷） |
|---|---|---|---|
| `a-h-G-U-C` | UC | COMBAT（65／59） | `Combat`（93／76） |
| `a-h-G-U-C-I` | UCI | INFANTRY（68／62） | `Infantry`（93／76） |
| `a-h-G-U-C-A` | UCA | ARMOR（66／60） | `Armor/Mechanized`（92／75。綴りは Appendix E の TABLE E-III、220／203 で確かめた。`docs/references.md` S6） |
| `a-h-G-U-C-A-A` | UCAA | ANTIARMOR（66／60） | `Antitank/Antiarmor`（92／75） |
| `a-h-G-U-C-R` | UCR | RECONNAISSANCE（71／65） | `Reconnaissance/Cavalry/Scout`（93／76） |
| `a-h-G-U-C-D` | UCD | AIR DEFENSE（65／59） | `Air Defense`（93／76） |
| `a-h-G-U-C-F` | UCF | FIELD ARTILLERY（69／63） | `Field Artillery`（93／76） |
| `a-h-G-U-C-F-M` | UCFM | MORTAR（70／64） | `Mortar`（94／77） |
| `a-h-G-U-C-E` | UCE | ENGINEER（68／62） | `Engineer`（94／77） |

陸上の装備（E Change 1 の TABLE A-XXIX）

| CoT の type の頭 | 2525C の機能の符号 | 2525C の名前（PDF／印刷） | 下書きの言葉（E Change 1。PDF／印刷） |
|---|---|---|---|
| `a-h-G-E-W` | EW | WEAPON（80／74） | `Weapon/Weapon System`（101／84） |
| `a-h-G-E-W-R-R` | EWRR | RIFLE（82／76） | `Rifle`（101／84） |
| `a-h-G-E-W-R-L` | EWRL | LIGHT MACHINE GUN（82／76） | `Machine Gun`（101／84） |
| `a-h-G-E-W-R-H` | EWRH | HEAVY MACHINE GUN（82／76） | `Machine Gun`（101／84） |
| `a-h-G-E-W-A` | EWA | AIR DEFENSE GUN（83／77） | `Air Defense Gun`（101／84） |
| `a-h-G-E-W-G` | EWG | ANTITANK GUN（82／76） | `Antitank Gun`（101／84） |
| `a-h-G-E-W-H` | EWH | HOWITZER（82／76） | `Howitzer`（102／85） |
| `a-h-G-E-W-M` | EWM | MISSILE LAUNCHER（80／74） | `Missile Launcher`（102／85） |
| `a-h-G-E-W-M-T` | EWMT | MISSILE LAUNCHER ANTITANK (AT)（81／75） | `Antitank Missile Launcher`（102／85） |
| `a-h-G-E-W-O` | EWO | MORTAR（82／76） | `Mortar`（102／85） |
| `a-h-G-E-W-X` | EWX | MULTIPLE ROCKET LAUNCHER（81／75） | `Multiple Rocket Launcher`（103／86） |
| `a-h-G-E-V` | EV | GROUND VEHICLE（83／77） | `Vehicle`（103／86） |
| `a-h-G-E-V-A` | EVA | ARMORED VEHICLE（83／77） | `Armored`（103／86） |
| `a-h-G-E-V-A-A` | EVAA | ARMORED PERSONNEL CARRIER（83／77） | `Armored Personnel Carrier`（103／86） |
| `a-h-G-E-V-A-T` | EVAT | TANK（83／77） | `Tank`（103／86） |
| `a-h-G-E-V-E` | EVE | ENGINEER VEHICLE（84／78） | `Engineer Vehicle and Equipment`（103／86） |
| `a-h-G-E-V-U` | EVU | UTILITY VEHICLE（84／78） | `Utility Vehicle`（104／87） |
| `a-h-G-E-V-C` | EVC | CIVILIAN VEHICLE（84／78） | `Civilian Vehicle`（104／87） |

決め方（出典にない決め方は、設計の決定。2026-11-08）:

- 引数の前後の空白を取ってから、`-` で区切った字の並びで比べる。表の頭の字の並びが、type の字の並びの先頭と字ごとにすべて同じ行のうち、いちばん長い行の言葉を返す（例: `a-h-G-U-C-I-d` は `a-h-G-U-C-I` に当たり `Infantry`、`a-h-G-U-C-F-M` は `Mortar`）。字の途中では当てない（`a-h-G-U-C-IX` は `a-h-G-U-C-I` に当たらず、`a-h-G-U-C` の `Combat`）。
- 前後から取る空白は `Character.isWhitespace` の範囲で、全角空白（U+3000）も含む（`MgrsText.clean`・C21 と同じ）。
- 空の段（末尾の `-`、続いた `--`）も 1 つの段として比べる。空の段は表のどの字とも同じにならないので、そこから先は当たらない（例: `a-h-G-U-C-I-` は `Infantry`、`a-h-G-U-C--I` は `Combat`）。普通の CoT の type には出ない形で、出ても下書きを利用者が直せるので、例外にはしない（2026-11-08 のレビュー）。
- 大文字と小文字は区別する（CoT の type の字は大文字と小文字で意味が違う）。
- 次のときは null を返す（下書きを入れない）: 引数が null・空・空白だけ、`a-h-` で始まらない（味方・中立・不明など）、陸上（`G`）でない（航空・水上・SOF など）、表のどの行にも当たらない（`a-h-G` や `a-h-G-U` だけの点、表にない種類）。
- 2525C に SNIPER の行はない。SDK の `a-h-G-U-C-I-d` は上の決め方で `Infantry` になる。SOF は 2525C で陸上ではない次元（`F`）なので表に入れない。
- 返す言葉は表の言葉そのまま（`TrainingNotice` は付けない。5 行目は `CasBriefFormatter.format` の先頭と末尾の行で U1 を守る）。

### 2.4 手入力と自動の値

- core は欄の値を受け取って文字にするだけ。自動の値は app が 2.3 で計算して欄に入れ、使う人が直してから「作る」を押す（F02「共通の動作」）。
- どの値が自動か手入力かは、core では区別しない。

### 2.5 文字の並び（`final class CasBriefFormatter`、`static CasBriefResult format(NineLine lines, CasRemarks remarks)`）

改行は `\n`。`{n}` は各行の値。未入力の行の値は `（未入力）` にし、`missingLines` に番号を入れる。

```
TRAINING USE ONLY / 訓練用
CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84
[1-3]
1 IP/BP: {1}
2 Heading: {2}
3 Distance: {3}
[4-6]
4 Target elevation: {4} (readback)
5 Target description: {5}
6 Target location: {6} (readback)
[7-9]
7 Type mark / Terminal guidance: {7}
8 Location of friendlies: {8}
9 Egress: {9}
[Remarks]
LTL/PTL: …
Ordnance / Effects: …
Threat / SEAD: …
Additional remarks: …
Additional calls: …
TRAINING USE ONLY / 訓練用
```

- 先頭と末尾の行は `TrainingNotice.text()`（U1）。
- 行の頭の番号と項目名は読む人の手引き。出典では番号は読まない（F02 の図の注記）。`(readback)` は必ず復唱させる行の印（F02 の図の注記）。
- 2 行目: `offset` があり、2 行目が入っていれば、`{2}` を `306, Offset left` のように `{2}, Offset {offset}` にする。2 行目が未入力なら offset は付けない。
- 1〜3 行目がすべて未入力なら、`[1-3]` から 3 行目までの 4 行を出さず、`{4}` の前に `Elevation ` を付け、1〜3 行目は未入力の行に数えない（出典 V-27 の、1〜3 行目を省いたとき）。1〜3 行目の一部だけが入っているときは、空の行を未入力にする。
- 9 行目: 値が `Egress`（大文字と小文字を区別しない）で始まらなければ、前に `Egress ` を付ける。
- 4〜9 行目は、空なら未入力に数える。
- Remarks: 空でない項目だけを上の順で出す。1 つもなければ `[Remarks]` の行も出さない。
- 値の前後の空白は取る。値の中の改行は空白 1 つにする（行の並びが崩れないように）。全角空白（U+3000）も空白として扱う（前後から取り、全角空白だけの欄は空に数え、9 行目の `Egress` の判定の前にも取る）。
- `lines` か `remarks` が null なら `IllegalArgumentException`。
- `03-cas-other-products.md` の作業で `format(NineLine, CasRemarks, CasRestrictions)` を足す。`[Restrictions]` の節は `[Remarks]` の後ろ、末尾の `TrainingNotice.text()` の前に入る。中身は `03-cas-other-products.md` 2.2.4。2 つの引数の `format` は、空の Restrictions を渡したのと同じで、上の並びは変わらない。

### 2.6 試験（JUnit。`core/src/test/java/jp/jtak/core/cas/`。`inverse` は `core/src/test/java/jp/jtak/core/lrf/`）

| # | 入力 | 期待 | 許容 |
|---|---|---|---|
| C1 | `inverse`: 出典 S3 の Flinders Peak → Buninyong（座標は 01 の 3.5 の T2） | 距離 54972.271 m、前方位 306°52′05.37″ | 1e-3 m、1e-5° |
| C2 | `inverse`: 赤道の (0,0) → (0.0090436947696644, 0) と (0, 0.008983152841195215)（01 の T1 の値） | 1000 m・0°、1000 m・90° | 1e-6 m、1e-6° |
| C3 | `inverse`: 同じ点 | `{0, NaN}` | — |
| C4 | `headingFromBearing`: 真 0.2・偏差 0／真 359.6・偏差 0／真 5・偏差 −7／真 90・偏差 NaN | `360`／`360`／`012`／null | — |
| C5 | `heading`: 同じ点 | null | — |
| C6 | `distanceText`: 54972.271・IP／1249・BP／1250・BP／NaN | `29.7 NM`／`1200 meters`／`1300 meters`／null | — |
| C7 | `distance`: C1 の 2 点・IP | `29.7 NM` | — |
| C8 | `elevationFeet`: 381.0／0／NaN | `1250 feet`／`0 feet`／null | — |
| C9 | `friendlies`: 赤道の目標 (0,0) と、`Geodesy.direct` で作った真南 300 m・真東 500 m の 2 点／空のリスト | `South 300`／null | — |
| C10 | `Cardinal.fromTrueBearing`: 0、22.4、22.5、67.5、180、337.5、360、−45 | N、N、NE、E、S、N、N、NW | — |
| C11 | `format`: 全部の行と Remarks 1 つ | 先頭と末尾が `TrainingNotice.text()`、2.5 の並び、`missingLines` は空 | — |
| C12 | `format`: 5 行目と 8 行目が空 | `（未入力）` が 2 つ、`missingLines` = [5, 8] | — |
| C13 | `format`: 1〜3 行目が空 | `[1-3]` の 4 行がなく、4 行目が `Elevation ` で始まる、`missingLines` に 1〜3 がない | — |
| C14 | `format`: 9 行目が `North to Moon`／`egress North` | `Egress North to Moon`／`egress North`（重ねない） | — |
| C15 | `format`: offset あり・2 行目 `306`／offset あり・2 行目なし（1 行目はある） | `306, Offset left`／`（未入力）` だけ | — |
| C16 | `format`: Remarks がすべて空 | `[Remarks]` の行がない | — |
| C17 | `format`: 値に改行と前後の空白 | 空白 1 つにまとまり、前後の空白がない | — |
| C18 | `format`: null の引数 | `IllegalArgumentException` | — |
| C19 | `inverse`: 対蹠点 (0,0) → (0,180)／`heading`・`distance`: 同じ 2 点 | `IllegalStateException`／どちらも null | — |
| C20 | `friendlies`: 目標 (0,0) と、目標と同じ点・真南 300 m の点の 2 つ | null | — |
| C21 | `format`: 9 行目が `　Egress North`（先頭が全角空白）／5 行目が全角空白だけ | `Egress North`／5 行目が `（未入力）` で `missingLines` に 5 | — |
| C22 | `MgrsText.clean`: 空白の前に U+200E か U+200F が入った `54S UD 08816 72272`（繰り返し入ったもの・前後に空白や全角空白があるものを含む） | `54S UD 08816 72272` | — |
| C23 | `MgrsText.clean`: null／空・空白だけ／見えない文字と空白だけ／見えない文字のない文字列 | null／null／null／そのまま | — |
| C24 | `TargetDescriptionDraft.fromCotType`: 2.3.1 の表の 27 行の type の頭それぞれ | 表の言葉 | — |
| C25 | `fromCotType`: `a-h-G-U-C-I-d`／`a-h-G-E-V-A-T-x`／` a-h-G-U-C-I `（前後に空白）／`　a-h-G-U-C-I　`（前後に全角空白）／`a-h-G-U-C-I-`（末尾が `-`）／`a-h-G-U-C--I`（空の段） | `Infantry`／`Tank`／`Infantry`／`Infantry`／`Infantry`／`Combat` | — |
| C26 | `fromCotType`: `a-h-G-U-C-IX`／`a-h-G-E-W-R` | `Combat`／`Weapon/Weapon System`（字の途中で当てない・表にない字は短い行へ戻る） | — |
| C27 | `fromCotType`: `a-f-G-U-C-I`（味方）／`a-n-G-U-C-I`（中立）／`a-u-G-U-C-I`（不明）／`a-h-A-M-F`（航空）／`a-h-G`／`a-h-G-U`／`a-h-G-I`（表にない） | すべて null | — |
| C28 | `fromCotType`: null／空／空白だけ／`A-H-G-U-C-I`（大文字） | すべて null | — |

- 値が合わないときは、式や期待値を勝手に直さず、根拠を確かめる。

## 3. app（`jp.jtak.plugin.cas` パッケージ）

### 3.1 画面

- 今のドロップダウン（`JtakDropDownReceiver`）の上に「LRF」「9-line」の 2 つのボタンを置き、押した方の部分を出す。上の `TrainingNotice.text()` はどちらでも出たままにする（下の表示はなくした）。LRF の部分（01）の動きは変えない。
- 9-line の部分:
  1. 地図の点の選択（3.2）: IP/BP の点・目標の点・味方の点の 3 つ。IP か BP かの選択（既定は IP）。一覧を読み直すボタン。
  2. 「地図の点から入れる」ボタン: 3.3 の値と 2.3 の計算で 1・2・3・4・6・8 行目の欄を埋める。取れない値の欄は空にする（手で入れた値も消える。目標を選び直したとき、前の点の値が残って別の点の書式に出ないようにするため。レビューの指摘）。1 行目には IP/BP の点の名前（callsign）を入れる。
     5 行目の欄も同じボタンで埋める: 目標の点の type（3.3）を `TargetDescriptionDraft.fromCotType`（2.3.1）に渡し、言葉が返れば欄に入れ、null なら欄を空にする（味方・中立・不明の点や表にない種類の点を選んだとき、前の目標の下書きが残らないように。ほかの行と同じ扱い）。欄は直せるままにし、利用者が数・姿勢などを書き足す。目標の点を選んでいないときは、ほかの行と同じく欄を空にする。
  3. 9 行の欄・Offset の欄・Remarks の 5 つの欄（すべて直せる）。
  4. 「作る」ボタン: 欄の値で `CasBriefFormatter.format` を呼び、結果を選んでコピーできる文字の表示に出す。`missingLines` が空でなければ、`未入力の行: 5, 8` のように表示の上に出す。
- 欄の名前は F02 の表の項目名（英語）で、日本語の短い説明を添えてよい。実在の製品・組織の名前は入れない（U5）。

### 3.2 地図の点の一覧

- 地図の上の点のマーカー（JTAK が置いた点と ATAK のマーカー）を一覧にし、名前（callsign）で選ぶ。自分の位置のマーカーは味方の候補に入れてよい。
- 同じ名前の点は、ラベルに座標を添えて区別する。同じ名前かどうかは大文字と小文字を区別せずに数える。一覧を読み直したあとの選び直しは、ラベルではなく点の UID で行う（レビューの指摘）。
- ATAK が中で使う点（`MetaMapPoint` で、UID が `TARGET_POINT` のもの）は一覧から外す。`MetaMapPoint` を全部は外さず、外す点をクラスと UID で名指しする（5.1）。
- 測距線（Range & Bearing）の端点も一覧から外す（利用者の決定 2026-11-08）。外すのは `com.atakmap.android.toolbars` の `RangeAndBearingEndpoint`（`MetaMapPoint` の子）と `DynamicRangeAndBearingEndpoint`（`Marker` の子）の点。クラスで見分ける（`instanceof`）。
- 自分の位置のマーカー（UID が `MapView.getDeviceUid()` と同じ点）は、`getTitle()` が UID を返すので、`MapView.getDeviceCallsign()` を名前にする（5.1）。
- 一覧の取り方（`MapView` のグループをたどる API）は SDK の javadoc（`out/sdk/` に展開したもの）で確かめてから使う。javadoc にない API は使わない。

### 3.3 ATAK と Android から取る値

| 値 | 取り方 | 確かめた場所 |
|---|---|---|
| 位置（緯度・経度・高さ） | マーカーの `GeoPoint`。高さは `isAltitudeValid()` で、高さの参照（`getAltitudeReference()`）が HAE のときだけ使い（AGL の高さは HAE の欄に入れない。レビューの指摘）、`GeoPosition` の `haeMeters` に入れる（ないときは NaN） | 01 の 4.2 |
| 4 行目の MSL | `com.atakmap.coremap.maps.conversion.EGM96.getMSL(GeoPoint)`（m）。高さがないか、結果が NaN なら欄を空にする | SDK の main.jar（`docs/references.md` の S1。javap。単位と NaN の扱いは確かめて追記する） |
| 2 行目の偏差 | IP/BP の点で `new GeomagneticField(lat, lon, 高さ(m。不明は 0), System.currentTimeMillis()).getDeclination()` | 01 の 4.2 と同じ方法 |
| 5 行目の下書きに使う点の種類 | 目標の点の `com.atakmap.android.maps.MapItem.getType()`（CoT の type の `String`）。そのまま 2.3.1 に渡す（null も渡してよい。null が返る） | SDK の javadoc（`MapItem`。2026-11-08 に確かめた） |
| 6 行目の MGRS | `com.atakmap.coremap.conversions.CoordinateFormatUtilities.formatToString(GeoPoint, CoordinateFormat.MGRS)` の結果を `MgrsText.clean`（2.2）に通す（空白の前に U+200E が入るため）。`formatToString` が `IllegalArgumentException` を投げたとき（MGRS に変えられない点）は欄を空にする（ほかの例外は受けない） | SDK の javap・javadoc（`docs/references.md` の S1）・5.1 |

- 方位・距離・単位・方角の計算は app で書かず、core の 2.3 を呼ぶ。

## 4. 確かめ方

- `bash software/atak-plugin/build.sh assembleDebug :core:test` が通る（01 の試験を含めて失敗 0）。
- 実機での確認の `adb install` は、利用者に確認してから行う（U6）。

## 5. 未決

- 8 行目の方角の基準（真北）、2 行目で 0 を「360」と書くこと、8 行目の距離を整数の m にすること、味方はいちばん近い 1 つだけを使うこと、目標と同じ点の味方があれば 8 行目を null にすること（2.3）は、2026-11-08 に利用者が「そのままでよい」と確かめた。未決ではなくなった。
- 味方の点は 1 つだけ選ぶ画面にしている（core は複数を受け取れる）。この形で利用者が確かめた（上と同じ日）。複数を選べるようにするかは、使ってから決める。
- 実機で確かめること（実機の操作は利用者に確認してから。結果は 5.1）: `MapItem.getTitle()` がマーカーで callsign を返すか、一覧に自分の位置・測距線の端点・図形の頂点・ルートの点が混じるか（混じったので、3.2 に除外の条件を決めた）と一覧の量、6 行目の MGRS の文字列の形、偏差の値が妥当か。同じ名前の点のラベルに UID の一部を足すかは、UID の形を見てから決める。
- 点に高さがないとき、地形の標高のデータから 4 行目を出すかは未決。今は手入力にする。
- 5 行目の下書きは 2.3.1・3.1 に決めた。出典の版（MIL-STD-2525E w/Change 1）と、対応を取る 2525C は、どちらも公開資料（Distribution A）と確かめた（`docs/usage-scope.md` Q8、`docs/references.md` S6・S7）。下書きを入れても、空の 5 行目と全角の空白だけの 5 行目の扱い（C12・C21）は変えない。表に入れる種類を増やすかは、使ってから決める。
- 5 行目の下書きと、測距線の端点を一覧から外したことは、2026-11-08 に実機で確かめた（5.3）。

### 5.1 実機の確認の結果（2026-11-08、実機・ATAK-CIV 5.8.0.9、852f94c）

利用者の承認で JTAK の APK を `adb install` し、画面を操作して確かめた。

| 確かめたこと | 結果 | 扱い |
|---|---|---|
| `getTitle()` がマーカーの名前を返すか | 利用者が置いたマーカーでは、地図の上のラベル（例 `IP1 1`。ATAK が名前の後ろに番号を付ける）を返す | このまま使う |
| 自分の位置のマーカー | 名前（callsign、例 `GUMBO`）でなく UID（`ANDROID-` で始まる文字）が出る | 直す。UID が `MapView.getDeviceUid()` と同じ点は `getDeviceCallsign()` を名前にする |
| 一覧に混じるもの | `TARGET_POINT` という点が出る。ATAK が中で使う点（地図の上に見えるマーカーではない） | 直す。ATAK が中で使う点を一覧から外す。外し方は SDK の javadoc で確かめて決める |
| 一覧の量 | 地図に 5 つのマーカー（IP 2・目標 1・味方 1・自分）を置いて 6 件（上の `TARGET_POINT` を含む） | 外したあとは 5 件になるはず |
| 測距線の端点・図形の頂点・ルートの点 | 地図に置いていないので未確認 | 置いて確かめるのは次の実機の確認で行う |
| 同じ名前の点のラベル | `IP1 1 (34.9640, 138.8533)` と `ip1 1 (34.9561, 138.8655)` のように、座標で区別できた | UID の一部は足さない |
| 6 行目の MGRS の形 | `54S UD 08816 72272`（区域と帯・100 km の区画・東と北の 5 桁ずつ、1 m 単位）。ただし、空白の前に見えない文字 U+200E（LEFT-TO-RIGHT MARK）が入っている | 直す。U+200E・U+200F を取り除く。コピーや書き出し（F04）に見えない文字が混じらないようにするため |
| 2 行目の偏差 | IP1 1 から目標まで、真方位 75.7°（MGRS を緯度・経度に直して計算）に対し、画面は 084。偏差は約 −8.3°（西に 8°）で、伊豆の偏差（西に 7〜8°）と合う | このまま使う |
| 3 行目の距離 | 計算で 4,935 m（2.66 NM）、画面は `2.7 NM` | 合っている |
| 4 行目の標高 | `965 feet`（点の高さから EGM96 で MSL にしたもの） | 地図の標高と照らし合わせていない |
| 8 行目 | `East 318`（FR1 1 を味方に選んだとき） | 味方の点の座標を控えていないので、計算では照らし合わせていない |
| 「作る」を押したときの表示 | ボタンの下に本文（先頭と末尾に TRAINING USE ONLY / 訓練用）が出る。利用者の画面でも出た | このまま使う |

### 5.2 5.1 の直しの実機の確認（2026-11-08、実機・ATAK-CIV 5.8.0.9、ab4b748）

利用者の承認で直した APK を `adb install -r` し、利用者がプラグインの読み込みを承認した（ATAK の再起動は要らなかった）。画面を操作し、画面の部品の書き出し（`uiautomator dump`）で文字を確かめた。

| 確かめたこと | 結果 | 扱い |
|---|---|---|
| `TARGET_POINT` | 一覧から消えた。一覧は `FR1 1`・`GUMBO`・`IP1 1`・`TG1 1` の 4 件（このときの地図のマーカーは IP 1・目標 1・味方 1・自分） | 直った |
| 自分の位置のマーカーの名前 | `GUMBO`（callsign）が出た | 直った |
| 6 行目の MGRS | `54S UD 08816 72272`。画面の部品の書き出しのどれにも U+200E（バイト列 e2 80 8e）がなかった（同じ方法で、入れた文字は見つかることを確かめた） | 直った |
| 「地図の点から入れる」 | IP `IP1 1`（IP）・目標 `TG1 1`・味方 `FR1 1` で「地図の点から入れました」と出た。1 行目 `IP1 1`、2 行目 `084`、3 行目 `2.7 NM`、4 行目 `965 feet`、6 行目 `54S UD 08816 72272`、8 行目 `East 318`（5.1 と同じ値） | このまま使う |
| 「作る」 | 本文の先頭と末尾に TRAINING USE ONLY / 訓練用。6 行目は `54S UD 08816 72272 (readback)`、手で入れる 5・7・9 行目は `（未入力）` | このまま使う |
| 測距線の端点（`RangeAndBearingEndpoint`）を一覧に出すか | 今回は地図に測距線を置いていないので見ていない | 利用者が「一覧から外す」と決めた（2026-11-08）。外す（3.2）。実機で測距線を置いて確かめるのは、次の実機の確認で行う |

### 5.3 測距線の端点の除外と 5 行目の下書きの実機の確認（2026-11-08、実機・ATAK-CIV 5.8.0.9、cf98198）

利用者の承認で、PR #9 をマージしたあとの main（cf98198）の APK を `adb install -r` した。画面の操作と確認は利用者が行った。ATAK 本体は触っていない。

| 確かめたこと | 結果 | 扱い |
|---|---|---|
| 測距線（Range & Bearing）の端点を「地図の点から入れる」の一覧に出すか | 利用者が測距線を引いて一覧を開いた。端点は出なかった | 直った（3.2） |
| 利用者が置いた点と自分の位置が、一覧に残るか | 残った | このまま使う |
| 敵の点を目標にして「地図の点から入れる」を押したときの 5 行目 | 種類からの下書きが入った | このまま使う（2.3.1・3.1） |
| 敵でない点を目標にしたときの 5 行目 | 空になった | このまま使う |
