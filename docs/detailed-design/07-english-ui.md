# 07: 英語の画面（全機能）

- 機能仕様: `docs/functional-spec/README.md` の「共通の決まり」（言語）。F01〜F04 のすべての画面にかかわる
- 利用者の決定: 2026-11-09
- 関連: `docs/usage-scope.md` U1〜U3・「画面の言語」、`01-lrf-marking.md`・`04-weapon-effects.md`・`05-export.md`・`06-scenario-import.md`
- 別の作業で扱う: 英語の取扱説明、コードのコメントの日英併記

## 1. 範囲

### 1.1 言語の決め方

- 端末の言語が日本語（ja）なら日本語、それ以外なら英語にする。
- 英語の文字は `res/values/`（既定）、日本語の文字は `res/values-ja/` に置く。
- 言語はプラグインの中で切り替えない。言語を選ぶ設定は作らない。

### 1.2 英語にするもの

- 画面の文字: ボタン・見出し・欄の名前・選択肢・Toast・確認の文・誤りの文。
- LRF の点の説明（remarks）と、点を置いたとき・置かなかったときの文（`LrfMessages`）。
- 既定の兵器の表の名前（行のラベルと輪の名前）。
- 兵器の表の入力の誤りの文（`WeaponEntryForm`・`WeaponTableCodec`・`WeaponJson`）。
- 書き出しに失敗したときの誤りの文（3.4）。

点の説明と既定の表は、**作ったときの言語**で作る。前に作った点と、保存してある表は書き換えない
（日本語で作った点は、英語の端末で見ても日本語のまま）。

### 1.3 変えないもの（どちらの言語でも同じ）

- 「TRAINING USE ONLY / 訓練用」の表示（`TrainingNotice` の 2 つの文）と、兵器の訓練用サンプル値の文（`TrainingNotice.weaponSampleText()`。U2・U3）。
  どちらも、いまと同じく日本語と英語の両方の言葉で出る。
- 表のファイルの注意書き（`WeaponTableCodec.FILE_NOTICE`）。
- CAS 系の書式（CAS Brief・ルーティング・チェックイン・ゲームプラン・Restrictions）の中身と、テキスト・PDF に書き出す中身。
  未入力の印「（未入力）」（`CasText.MISSING`）も変えない。
- JP 3-09.3 の英語の用語と、`(JP 3-09.3 2014 Fig. …)` の題名。未入力の項目の名前（core がもう英語で返している）。
- プログラムの誤り（起こらないはずのもの）: core の CAS の `IllegalArgumentException`、`LrfMessages` の「未対応の状態: 」、`Geodesy` の `IllegalStateException`、
  書き出しの `ExportText`・`ExportFileName` の `IllegalArgumentException`（いまの呼び出しでは起こらない）。
- `Log.*` の文、コードのコメント（別の作業で扱う）。
- `app_name`（`JTAK`）と `app_desc`（`JTAK — TRAINING USE ONLY / 訓練用`）。
- 表のファイルの形・キーの名前・id（`docs/interfaces/README.md`）。言語で変えない。

### 1.4 ファイルからの読み込み

`06-scenario-import.md` の読み込みの画面と誤りの文も、この設計の決まりに従う。
画面の文字は values と values-ja に分けて置き、core の誤りの文は `Lang` を受け取って作る（2.1）。
読み込むファイルの形（ラベルの名前など）は、言語で変えない。

## 2. core

### 2.1 Lang

`jp.jtak.core.Lang` を足す。

```java
public enum Lang { JA, EN }
```

- 言語で文が変わる core のメソッドは、**`Lang` を省けない引数**として受け取る。日本語を既定にした重ね書き（overload）は作らない。
  渡し忘れをコンパイルで見つけるため。
- core は端末の言語を読まない（`Locale.getDefault()` を使わない）。言語は app が決めて渡す（3.1）。
- **`Lang` に null は渡せない**（2026-11-09 利用者の決定）。`Lang` を受け取る core の public のメソッドとコンストラクタは、最初に `Objects.requireNonNull(lang, "lang")` を呼び、null なら `NullPointerException` を投げる。
  黙って英語にすると、渡し忘れに気づけないため。ほかの引数の検査より先に行う。

### 2.2 Lang を足すメソッド

| いまのメソッド | 直したあと |
|---|---|
| `InputSource.label()` | `InputSource.label(Lang)`。コンストラクタで持つ名前を JA と EN の 2 つにする |
| `LrfMessages.forStatus(TargetStatus, RangeReading)` | `forStatus(TargetStatus, RangeReading, Lang)` |
| `LrfRemarks.format(RangeReading, TargetSolution)` | `format(RangeReading, TargetSolution, Lang)`。入力の名前は `reading.source().label(lang)` |
| `DefaultWeaponTable.table()` | `table(Lang)` |
| `WeaponEntryForm.toEntry(String id, String label, String[] ringNames, String[] ringRadii, String note)` | 最後に `Lang` を足す |
| `WeaponTableCodec.parse(byte[])`・`validate(WeaponTable)` | `parse(byte[], Lang)`・`validate(WeaponTable, Lang)` |
| `WeaponJson.parse(byte[])`（package-private） | `parse(byte[], Lang)` |

`WeaponTableCodec.write(WeaponTable)` と `FILE_NOTICE` は変えない。

### 2.3 LRF の文

`LrfMessages.forStatus` は、いまと同じく `TrainingNotice.text() + " — " + 本文` の形にする。距離の数の書き方（`Locale.ROOT`、小数 1 桁）は変えない。

| 状態 | JA（いまのまま） | EN |
|---|---|---|
| 距離の部分 | `距離 %.1f m` | `distance %.1f m` |
| OK | `点を置きました（<距離>）` | `Point placed (<distance>)` |
| NO_OBSERVER | `自分の位置が取れないため点を置きません（<距離>）` | `Point not placed: own position unavailable (<distance>)` |
| INVALID_OBSERVER | `自分の位置が正しくないため点を置きません（<距離>）` | `Point not placed: own position invalid (<distance>)` |
| INVALID_DISTANCE | `距離が正しくないため点を置きません（<距離>）` | `Point not placed: distance invalid (<distance>)` |
| DISTANCE_OUT_OF_RANGE | `距離が上限 20 km を超えたため点を置きません（<距離>）` | `Point not placed: distance exceeds the 20 km limit (<distance>)` |
| INVALID_ELEVATION | `俯仰が正しくないため点を置きません（<距離>）` | `Point not placed: elevation angle invalid (<distance>)` |
| AZIMUTH_UNAVAILABLE | `方位が取れなかったため点を置きません（<距離>）` | `Point not placed: azimuth unavailable (<distance>)` |
| INVALID_DECLINATION | `磁気偏差が取れないため点を置きません（<距離>）` | `Point not placed: magnetic declination unavailable (<distance>)` |

`InputSource.label(Lang)`:

| 値 | JA（いまのまま） | EN |
|---|---|---|
| ATAK_LRF | `ATAK の LRF 連携` | `ATAK LRF link` |
| BLE | `BLE 直結` | `BLE direct` |
| MANUAL | `手入力` | `Manual entry` |

`LrfRemarks.format` の行（先頭と末尾の `TrainingNotice.text()`、数の書き方、行の順は変えない）:

| 行 | JA（いまのまま） | EN |
|---|---|---|
| 見出し | `JTAK LRF 測距点` | `JTAK LRF range point` |
| 入力 | `入力: <名前>` | `Input: <name>` |
| 距離 | `距離: %.1f m` | `Distance: %.1f m` |
| 方位（磁） | `方位: 磁 %05.1f° / 偏差 %+.1f° / 真 %05.1f°` | `Azimuth: magnetic %05.1f° / declination %+.1f° / true %05.1f°` |
| 方位（真） | `方位: 真 %05.1f°` | `Azimuth: true %05.1f°` |
| 俯仰 | `俯仰: %+.1f°` | `Elevation angle: %+.1f°` |
| 水平距離 | `水平距離: %.1f m / 高低差: %+.1f m` | `Horizontal distance: %.1f m / Height difference: %+.1f m` |
| 目標 | `目標: %.6f, %.6f / 標高(HAE): %s` | `Target: %.6f, %.6f / Altitude (HAE): %s` |
| 標高が分からないとき | `不明` | `unknown` |
| 時刻 | `時刻: <Instant>` | `Time: <Instant>` |

### 2.4 兵器の表の入力の誤り（WeaponEntryForm）

文の中の数（N・MIN・MAX・n）は、いまの文と同じ値を入れる。JA はいまの文のまま変えない。

| JA（いまのまま） | EN |
|---|---|
| `ラベル: 1 から N 文字にしてください` | `Label: use 1 to N characters` |
| `メモ: N 文字までにしてください` | `Note: use up to N characters` |
| `輪 n: 名前と距離の両方を入れてください` | `Ring n: enter both the name and the distance` |
| `輪 n: 名前は N 文字までにしてください` | `Ring n: the name must be up to N characters` |
| `輪 n: 距離は MIN から MAX までの整数（m）にしてください` | `Ring n: the distance must be a whole number from MIN to MAX (m)` |
| `輪 n: 距離は前の輪より大きくしてください` | `Ring n: the distance must be larger than the previous ring` |
| `輪を 1 つ以上入れてください` | `Enter at least one ring` |

### 2.5 表のファイルの誤り（WeaponTableCodec）

- 誤りの文は「場所: わけ」の形のまま。`Errors.add(path, reason)` は、`path` が空ならわけだけ、空でなければ `path + ": " + reason` にする。
- 場所（`path`）は `entries[0].rings[1].radiusM` のような形で、`child` が `path + "." + key` でつなぐ。**場所の書き方は言語で変えない**（キーの名前はファイルと同じ英語）。
- JSON として読めないとき（`JsonException`）は、いまと同じく Codec で受け取り（41 行目のあたり）、2.6 の文を誤りに入れる。

わけの文:

| JA（いまのまま） | EN |
|---|---|
| `全体はオブジェクトにしてください` | `the whole file must be an object` |
| `ありません` | `missing` |
| `1 にしてください` | `must be 1` |
| `空でない文字列にしてください` | `must be a non-empty string` |
| `true にしてください` | `must be true` |
| `配列にしてください` | `must be an array` |
| `1 行から N 行までにしてください（いまは n 行）` | `must have 1 to N rows (now n)` |
| `知らないキーです` | `unknown key` |
| `オブジェクトにしてください` | `must be an object` |
| `文字列にしてください` | `must be a string` |
| `空にできません` | `must not be empty` |
| `N 文字までにしてください` | `must be up to N characters` |
| `英小文字・数字・- だけにしてください` | `use only lowercase letters, digits and -` |
| `ほかの行と同じ id です（<id>）` | `same id as another row (<id>)` |
| `1 個から N 個までにしてください（いまは n 個）` | `must have 1 to N items (now n)` |
| `MIN から MAX までの整数にしてください` | `must be an integer from MIN to MAX` |
| `前の輪より大きい値にしてください` | `must be larger than the previous ring` |
| `N 文字までにしてください（いまは n 文字）` | `must be up to N characters (now n)` |

### 2.6 JSON の読み取りの誤り（WeaponJson）

- 誤りの文は `error(at, reason)` が `位置 + ": " + わけ` の形で作る。
- 位置: JA は `<行>行<列>列`（いまのまま。95 行目のあたり）、EN は `line <行>, column <列>`。
- 256 KiB を超えたとき（60 行目のあたり）と、UTF-8 として読めないとき（73 行目のあたり）も、同じ位置の書き方を使う。
  - JA: `1行1列: 入力が 256 KiB（262144 バイト）を超えています`（いまのまま）
  - EN: `line 1, column 1: input exceeds 256 KiB (262144 bytes)`

わけの文（`<c>`・`<key>`・`<\e>`・N はいまと同じ中身を入れる）:

| JA（いまのまま） | EN |
|---|---|
| `正しくない UTF-8 のバイト列です` | `invalid UTF-8 byte sequence` |
| `値の後に余分な文字があります` | `extra characters after the value` |
| `値がありません（入力が途中で終わっています）` | `missing value (the input ends early)` |
| `値として読めない文字です（<c>）` | `unexpected character (<c>)` |
| `値として読めない文字です` | `unexpected character` |
| `入れ子が深すぎます（N 段まで）` | `nesting too deep (up to N levels)` |
| `キーは " で囲んだ文字列にしてください` | `a key must be a string in double quotes` |
| `同じキーがあります（<key>）` | `duplicate key (<key>)` |
| `キーの後に : が必要です` | `':' expected after the key` |
| `オブジェクトが閉じていません` | `object not closed` |
| `, か } が必要です` | `',' or '}' expected` |
| `配列が閉じていません` | `array not closed` |
| `, か ] が必要です` | `',' or ']' expected` |
| `文字列が閉じていません` | `string not closed` |
| `文字列の中に、エスケープしていない制御文字があります` | `unescaped control character in a string` |
| `使えないエスケープです（<\e>）` | `invalid escape (<\e>)` |
| `\u の後に 16 進数 4 桁が必要です` | `4 hex digits expected after \u` |
| `組になっていないサロゲートです` | `unpaired surrogate` |
| `数の書き方が正しくありません` | `invalid number format` |
| `小数点の後に数字が必要です` | `digit expected after the decimal point` |
| `指数の後に数字が必要です` | `digit expected after the exponent` |
| `数として読めません` | `cannot read as a number` |

### 2.7 既定の表（DefaultWeaponTable）

- id と値は言語で変えない: `sample-small`（100 / 300 m）、`sample-medium`（250 / 600 m）、`sample-large`（500 / 1000 m）。
- 名前:

| | JA（いまのまま） | EN |
|---|---|---|
| 行のラベル | `サンプル 小`・`サンプル 中`・`サンプル 大` | `Sample S`・`Sample M`・`Sample L` |
| 輪の名前 | `内側`・`外側` | `Inner`・`Outer` |

- メモ（`NOTE`）は、どちらの言語でも同じ 1 つの文にし、両方の言葉で書く（U3）:
  `訓練用サンプル値。実在の兵器の値ではない / TRAINING SAMPLE VALUE — NOT A REAL WEAPON`（`MAX_NOTE_LENGTH` の 200 文字に収まる）。
  - いまのメモ（`訓練用サンプル値。実在の兵器の値ではない`）と変わるのはここだけ。
    `WeaponTableCodecTest` の 111 行目のあたりで確かめている値も、この文に直す。
  - 保存してある表のメモは書き換えない（1.2）。

### 2.8 Locale.ROOT を付ける

数や 16 進を書く `String.format` が、端末の言語で書き方を変えないようにする。

- `RoutingInstruction.sentence` の `String.format(pattern, place)` → `String.format(Locale.ROOT, pattern, place)`
- `WeaponJson` の `String.format("\\u%04x", (int) c)`（416 行目のあたり） → `String.format(Locale.ROOT, "\\u%04x", (int) c)`

ほかの `String.format` は、もう `Locale.ROOT` を付けている。

### 2.9 試験（JUnit）

- いまある試験は、言語を受け取るメソッドに `Lang.JA` を渡し、確かめる値は変えない（2.7 のメモだけ直す）。
- 英語の試験を足す:
  - `LrfCoreTest`: `forStatus` の EN（OK と、置かなかったときの 7 つ）、`format` の EN（磁方位と真方位、標高が分からないとき）、`InputSource.label(Lang.EN)`。
    EN でも先頭と末尾に `TrainingNotice.text()` が入ること。
  - `WeaponEntryFormTest`: 2.4 の EN の文（少なくともラベル・輪の距離・輪が 1 つもないとき）。
  - `WeaponTableCodecTest`: `parse(…, Lang.EN)` で、2.5 の EN の文と、場所の書き方が JA と同じこと。
    JSON の読み取りの誤り（位置の `line L, column C`、256 KiB を超えたとき、UTF-8 として読めないとき）。
  - `DefaultWeaponTable.table(Lang.EN)`: ラベルと輪の名前が EN、id・値・メモが JA と同じこと。
- 書き出す中身（`ExportText`）と CAS の書式の試験は変えない（言語を受け取らない）。

## 3. app

### 3.1 言語を決める（JtakLang）

- `res/values/strings.xml` に `<string name="jtak_lang">en</string>`、`res/values-ja/strings.xml` に `<string name="jtak_lang">ja</string>` を置く。
  `jtak_lang` は values と values-ja の両方に置くキーなので、どちらにも `translatable="false"` を付けない（付けると Android lint の Untranslatable の警告になる。レビューの推奨）。
- `jp.jtak.plugin.JtakLang.of(Context pluginContext)` が `jtak_lang` を読み、`ja` なら `Lang.JA`、それ以外は `Lang.EN` を返す。
  core に渡す `Lang` は、すべてここから取る（読むのはこの 1 か所だけ）。
- `Locale.getDefault()` で決めない。わけ: Android がリソース（values と values-ja）を選ぶ言語と、`Locale.getDefault()` が返す言語は、
  端末の言語の設定（優先する言語が複数ある、アプリごとの言語など）によってずれることがある。
  リソースから決めれば、画面の文字と core の文の言語がいつもそろう。

### 3.2 Lang の渡し方

- `LrfMarkingController(MapView, Lang)`: `JtakMapComponent` が作るときに `JtakLang.of(pluginContext)` を渡す（21 行目のあたり）。
  `forStatus` と `format` に渡す（40・70 行目のあたり）。
- `WeaponTableFile(File, Lang)`: `WeaponPanel` が作るときに渡す（75 行目のあたり）。
  既定の表（95・107・111 行目のあたり）は `DefaultWeaponTable.table(lang)`、読み込み（103 行目のあたり）は `parse(…, lang)`。
- `WeaponPanel` は `Lang` をフィールドに持つ（67 行目のあたりで決める）。
  `toEntry`（446 行目のあたり）、既定の表に戻す（489 行目のあたり）、`validate`（502 行目のあたり）に渡す。
- `WeaponPanel.reasonOf` は R.string を返しているので変えない。

### 3.3 文字のリソース

- 英語だけの値（JP 3-09.3 の用語の欄など。下の一覧）は `values/` にだけ置く。`values-ja/` には置かない（日本語の端末でも英語のまま出る）。
  `mode_*`、`cas_remarks_title`・`cas_ltl_ptl`・`cas_ordnance`・`cas_threat`・`cas_additional_*`・`cas_line7`・`cas_line9`、
  `routing_instr_*`・`routing_other_instructions`・`routing_only_aircraft`・`routing_threats`・`routing_weather_terrain`・`routing_status_calls`・`routing_closing_send`・`routing_closing_hold`、
  `checkin_jtac_callsign`・`checkin_aircraft_callsign`・`checkin_mode_as_fragged`・`checkin_mode_with_exception`・`checkin_1`〜`checkin_7` の行、
  `gameplan_callsign`・`gameplan_type`・`gameplan_type_1`〜`3`・`gameplan_method`・`gameplan_intent`・`gameplan_collateral`・`gameplan_ordnance`・`gameplan_effects`・`gameplan_fuzing`・`gameplan_interval`、
  `restrictions_title`・`restrictions_danger_close`・`restrictions_acas`・`restrictions_fahs`・`restrictions_separation`・`restrictions_rw_out_of_bp`・`restrictions_pla`・`restrictions_tot_standby`・`restrictions_tot_after_correlation`・`restrictions_tot_push`、
  `app_name`・`app_desc`、`weapons_unit_m`。
  英語だけと決めて `values/` にだけ置くキーには、`translatable="false"` を付ける。
  わけ: 付けないと Android lint の MissingTranslation のエラーになり、本当の翻訳の漏れと見分けられない（レビューの推奨）。
  確かめ方: `bash software/atak-plugin/build.sh :app:lintDebug -x :app:takdevLint` で、MissingTranslation と Untranslatable が 0 件。
  （`-x :app:takdevLint` は、前からある takdevLint の「Obfuscation disabled」で先に止まるのを避けるため。）
- 日本語を含むキーは、いまの値をそのまま `values-ja/` に移し、`values/` には下の表の英語を置く。
  書式の引数（`%1$s` など）は JA と EN で同じものを、同じ数だけ使う。
- `jtak_lang` と 3.4 の `export_reason_*` を足す。

#### LRF

| キー | EN |
|---|---|
| lrf_title | `LRF range point auto-marking` |
| lrf_auto_switch | `Auto-marking (ATAK LRF link)` |
| lrf_manual_title | `Manual entry` |
| lrf_distance_hint | `Distance (m)` |
| lrf_azimuth_hint | `Azimuth (°)` |
| lrf_elevation_hint | `Elevation angle (°, up is positive)` |
| lrf_ref_magnetic | `Magnetic` |
| lrf_ref_true | `True` |
| lrf_place_button | `Place point` |
| lrf_input_error | `Point not placed: a field is not a number` |

#### CAS Brief（9-line）

| キー | EN |
|---|---|
| cas_title | `CAS 9-line and Remarks (JP 3-09.3 2014 Fig. V-9)` |
| cas_point_ip | `IP/BP point` |
| cas_point_target | `Target point` |
| cas_point_friendly | `Friendly point` |
| cas_type_ip | `IP (distance in NM)` |
| cas_type_bp | `BP (distance in m)` |
| cas_none | `(none)` |
| choice_none | `(none)` |
| cas_refresh_button | `Reload point list` |
| cas_fill_button | `Fill from map points` |
| cas_make_button | `Make` |
| cas_fill_missing | `Lines not filled: %1$s` |
| cas_fill_done | `Filled from map points` |
| cas_points_count | `Map points: %1$d` |
| cas_missing_lines | `Lines not entered: %1$s` |
| cas_line1 | `1 IP/BP (point name)` |
| cas_line2 | `2 Heading (magnetic, 3 digits)` |
| cas_line3 | `3 Distance (IP: NM, BP: meters)` |
| cas_line4 | `4 Target elevation (feet)` |
| cas_line5 | `5 Target description` |
| cas_line6 | `6 Target location (MGRS)` |
| cas_line8 | `8 Location of friendlies (direction and distance)` |
| cas_offset | `Offset (left / right)` |
| missing_items | `Items not entered: %1$s` |

#### Routing

| キー | EN |
|---|---|
| routing_title | `Routing (JP 3-09.3 2014 Fig. V-3)` |
| routing_conflict | `"You are the only aircraft on station" and "Other AOS" cannot both be entered. Leave one empty.` |
| routing_fill_done | `Put the map point name in the place field` |
| routing_fill_none | `Select a point` |
| routing_point | `Point for the place` |
| routing_callsign | `Aircraft callsign` |
| routing_instruction | `Routing / Holding instruction` |
| routing_place | `Place (point name, altitude, etc.)` |
| routing_other_aos | `Other AOS (other aircraft)` |
| routing_closing | `Closing instruction` |
| routing_hold_reason | `Hold reason (optional, no period at the end)` |

#### Check-in

| キー | EN |
|---|---|
| checkin_title | `Check-in (JP 3-09.3 2014 Fig. V-4)` |
| checkin_conflict | `An abort code and "No abort code" cannot both be entered. Leave one empty.` |
| checkin_mode | `Format` |
| checkin_mode_full | `Full (all items)` |
| checkin_abort_code_none | `No abort code (ABORT, ABORT, ABORT)` |

#### Game plan

| キー | EN |
|---|---|
| gameplan_title | `Game plan (JP 3-09.3 2014 Fig. V-9)` |
| gameplan_note | `Type and BOT/BOC are chosen separately (any combination). Training summary.` |
| gameplan_type_1_desc | `Type 1: the controller visually acquires the target and the attacking aircraft in the terminal phase and controls each attack` |
| gameplan_type_2_desc | `Type 2: the controller reduces risk by means other than visual and controls each attack` |
| gameplan_type_3_desc | `Type 3: the controller reduces risk by means other than visual and clears multiple attacks within one engagement` |
| gameplan_method_bot | `BOT (bomb on target)` |
| gameplan_method_boc | `BOC (bomb on coordinate)` |
| gameplan_bot_desc | `BOT: the aircraft acquires the target or aim point itself` |
| gameplan_boc_desc | `BOC: the aircraft attacks the coordinates given in the CAS Brief` |
| gameplan_line6_digits | `Line 6 digits` |

#### Restrictions

| キー | EN |
|---|---|
| restrictions_initials | `Danger close: commander's initials` |
| restrictions_tot | `TOT format` |
| restrictions_tot_time | `TOT time` |
| restrictions_tot_time_option | `TOT (enter a time)` |
| restrictions_conflict | `The Restrictions entries conflict (initials only with Danger close; a TOT time only when "TOT format" is a time)` |
| restrictions_missing | `Restrictions not entered: %1$s` |

#### RED（兵器の範囲）

英語の名前にも `TRAINING` を入れる（U1・U3）。

| キー | EN |
|---|---|
| weapons_title | `Risk-estimate distance (RED) (TRAINING)` |
| weapons_table_default | `Table: default (no file)` |
| weapons_table_file | `Table: file (%1$d rows)` |
| weapons_table_invalid | `The table file is invalid, so the default table is used` |
| weapons_more_errors | `%1$d more` |
| weapons_target | `Target point` |
| weapons_refresh | `Refresh point list` |
| weapons_row | `Weapon row` |
| weapons_draw | `Draw` |
| weapons_erase | `Erase` |
| weapons_no_target | `Select a target point` |
| weapons_no_row | `The table has no rows` |
| weapons_friendly_title | `Friendly points inside the RED circles:` |
| weapons_friendly_none | `Friendly points inside the RED circles: none` |
| weapons_friendly_hit | `%1$s — %2$s (%3$d m)` |
| weapons_unknown | `Points with unknown distance: %1$d` |
| weapons_erased | `Circles erased: %1$d` |
| weapons_edit_title | `Edit table` |
| weapons_edit_label | `Label` |
| weapons_ring1_name | `Ring 1 name` |
| weapons_ring1_radius | `Ring 1 radius (m)` |
| weapons_ring2_name | `Ring 2 name (leave empty if not used)` |
| weapons_ring2_radius | `Ring 2 radius (m)` |
| weapons_ring3_name | `Ring 3 name (leave empty if not used)` |
| weapons_ring3_radius | `Ring 3 radius (m)` |
| weapons_edit_note | `Note` |
| weapons_save_row | `Save row` |
| weapons_add_row | `Add row` |
| weapons_delete_row | `Delete row` |
| weapons_reset_table | `Reset to default table` |
| weapons_reload | `Reload` |
| weapons_new_row_label | `New row` |
| weapons_new_ring_name | `Inner` |
| weapons_saved | `Saved` |
| weapons_save_failed | `Could not save the table: %1$s` |
| weapons_errors_title | `Fix the entries:` |
| weapons_delete_confirm | `Delete %1$s` |
| weapons_reset_confirm | `Reset the table to the 3 default rows. The current rows will be removed.` |
| weapons_ok | `OK` |
| weapons_cancel | `Cancel` |
| weapons_circle_title | `TRAINING %1$s %2$s %3$d m` |
| weapons_circle_target | `Target: %1$s` |
| weapons_group_name | `JTAK TRAINING RED` |
| weapons_draw_failed | `Could not draw the circles: %1$s` |
| weapons_erase_failed | `Could not erase the circles: %1$s` |
| weapons_refresh_failed | `Could not get the point list: %1$s` |
| weapons_kept_as | `The previous file is kept as %1$s` |
| weapons_reason_read_failed | `Could not read the table file: %1$s` |
| weapons_reason_too_large | `The file is too large (limit %1$s bytes)` |
| weapons_reason_mkdir_failed | `Cannot create the folder: %1$s` |
| weapons_reason_tmp_write_failed | `Cannot write the temporary file: %1$s` |
| weapons_reason_rename_invalid_failed | `Cannot rename the invalid file: %1$s` |
| weapons_reason_rename_failed | `Cannot rename the file: %1$s` |

円の説明（remarks）は、いまと同じく `WeaponCircles.remarks()` が `TrainingNotice.text()` と `weaponSampleText()` を入れる（どちらも両方の言葉）。

#### 書き出し

| キー | EN |
|---|---|
| export_text_button | `Export text` |
| export_pdf_button | `Export PDF` |
| export_share_title | `Share the exported file` |
| export_saved | `Exported: %1$s` |
| export_failed | `Could not export: %1$s` |
| export_share_failed | `Could not share. The file is in the save folder: %1$s` |

### 3.4 書き出しの誤り（ExportException）

app の Java に直接書いた日本語の文は、書き出しの例外の 4 つだけ（`Log.*` を除く）。これをリソースから出す。

- `jp.jtak.plugin.export.ExportException extends IOException` を足す。
  - わけを enum `Reason { TEXT_WRITE, DIR_CREATE, DIR_OPEN, PDF_WRITE }` で持ち、細かい中身を文字列 `detail` で持つ（いまの文の `: ` の後ろの部分）。
  - 元の例外があれば cause に入れる。
- 置き換える 4 か所:

| 場所 | いま | Reason | detail |
|---|---|---|---|
| `ExportWriter` 24 行目のあたり | `テキストを書けませんでした: ` + `e.getMessage()` | TEXT_WRITE | `e.getMessage()` |
| `ExportWriter` 33 行目のあたり | `保存先を作れませんでした: ` + `directory` | DIR_CREATE | `directory` |
| `ExportWriter` 37 行目のあたり | `保存先を開けませんでした: ` + `e.getMessage()` | DIR_OPEN | `e.getMessage()` |
| `PdfExporter` 69 行目のあたり | `PDF を書けませんでした: ` + `e.getMessage()` | PDF_WRITE | `e.getMessage()` |

- `ExportWriter` は、いまと同じく `Context` を使わない。
- 4 つのパネル（`CasBriefPanel`・`CheckInPanel`・`GamePlanPanel`・`RoutingPanel` の書き出しの失敗を出すところ）は、
  `ExportException` なら `export_reason_*` に `detail` を入れた文を作り、それを `export_failed` の `%1$s` に入れる。
  ほかの `IOException` は、いまと同じく `e.getMessage()` を入れる。

| キー | JA（values-ja） | EN（values） |
|---|---|---|
| export_reason_text_write | `テキストを書けませんでした: %1$s` | `Could not write the text: %1$s` |
| export_reason_dir_create | `保存先を作れませんでした: %1$s` | `Could not create the save folder: %1$s` |
| export_reason_dir_open | `保存先を開けませんでした: %1$s` | `Could not open the save folder: %1$s` |
| export_reason_pdf_write | `PDF を書けませんでした: %1$s` | `Could not write the PDF: %1$s` |

JA の文は、いまの例外の文と同じになる（日本語の端末では、いまと同じ文が出る）。

注意: `Reason` を文にする `exportReason` は、4 つのパネルに同じ形で 1 つずつある。
`Reason` を足すときは、4 つすべてと `export_reason_*`（values と values-ja）を同じ変更で足す（レビューの指摘）。

## 4. 画面

- 画面の並び・ボタン・欄は変えない。文字だけが端末の言語で変わる。
- 英語の文字は日本語より長くなることがある。ボタンや欄の幅が足りずに切れるときは、報告する（レイアウトを大きく変えない）。
- 「TRAINING USE ONLY / 訓練用」の帯と、RED の訓練用サンプル値の文は、どちらの言語でも両方の言葉で出る。

## 5. 訓練用途

- `docs/usage-scope.md` U1〜U3 の表示は、どちらの言語でも両方の言葉のまま出す。英語の端末で日本語の表示が消えることはない。
- 英語の名前（RED の題・円の名前・グループの名前）にも `TRAINING` を入れる（3.3）。
- 既定の表のメモを、両方の言葉の 1 つの文にする（2.7）。
- 英語の文に、実在の製品・組織の名前、実在の兵器の名前を入れない。
- 言語の決まりは `docs/usage-scope.md` の「画面の言語」に書く。

## 6. 実機

- 実機で英語と日本語の画面を見るときは、`adb install` の前に利用者に確かめる。
- 端末の言語を変えるのは利用者が行う。開発の作業では端末の設定を変えない。ATAK 本体には触らない。
- 2026-11-09 に、利用者が実機（ATAK-CIV 5.8.0.9）で次の 3 点を確かめた（受入基準 #1）。
  - 英語の文が、ボタンや欄で切れずに出る。
  - プラグインの `Context` は、端末の言語に合わせて `values`（英語）と `values-ja`（日本語）を選ぶ。
  - 言語を変えたときにいつ反映されるかも確かめた。

## 7. 作業の範囲

作る:

- core: `jp.jtak.core.Lang`
- app: `jp.jtak.plugin.JtakLang`、`jp.jtak.plugin.export.ExportException`、`res/values-ja/strings.xml`

直す:

- core: `InputSource`・`LrfMessages`・`LrfRemarks`・`DefaultWeaponTable`・`WeaponEntryForm`・`WeaponTableCodec`・`WeaponJson`・`RoutingInstruction`（2 章）
- core の試験: `LrfCoreTest`・`WeaponEntryFormTest`・`WeaponTableCodecTest`（2.9）
- app: `res/values/strings.xml`（英語にする。3.3）、`JtakMapComponent`・`LrfMarkingController`・`WeaponTableFile`・`WeaponPanel`（3.2）、
  `ExportWriter`・`PdfExporter` と 4 つのパネル（3.4）
- 06-scenario-import.md の実装も、1.4 の決まりに従う（06 の作業で行う）

完了の条件:

- `bash software/atak-plugin/build.sh assembleDebug :core:test` が通る。
- app の Java に、画面に出る日本語の文字が残っていない（`Log.*`・コメントを除く）。
- `values/strings.xml` に日本語の文字が残っていない（`app_desc` の「訓練用」を除く）。
- `values-ja/strings.xml` のキーが、3.3 で日本語を含むとしたキーと `jtak_lang`・`export_reason_*` にそろっている。
