# 詳細設計 01: LRF の測距点の自動マーキング

- 日付: 2026-11-08
- 機能仕様: `docs/functional-spec/F01-lrf-marking.md`（要件 R1・R5）
- 対象: ATAK 標準の連携・手入力。BLE 直結は対象外
- 根拠: SDK の javadoc と main.jar（`docs/references.md` の S1）
- 出典: `docs/references.md` S2（Vincenty の式）・S3（試験の値の例）
- 関連: ADR 0003（core と app の分け方）、`docs/usage-scope.md` U1・U8・Q6

## 1. 範囲

| 入れる | 入れない |
|---|---|
| core: 測距の値の形、目標の計算（偏差の補正を含む）、点の説明の文、画面に出す文、JUnit の試験 | BLE 直結のパーサ |
| app: `RangeFinderAction2` の受け口、自分の位置と偏差を取る処理、マーカーを置く処理、手入力、自動マーキングの入切、止めるときに ATAK の標準の動作に戻す処理 | 測距の記録の保存・一覧（必要になったら別のタスク） |

## 2. 入力の経路（main.jar で確かめた動き）

```
ATAK 自身の Bluetooth の LRF（LRFCotManager・GenericLRFCallback）─┐
UDP 17211（LocalRangeFinderInput.run。全アドレスで受ける）───────┤
                                                              ▼
                     LocalRangeFinderInput.process(String "1,uid,時刻,d,a,e[,name]")
                       ├ d の欄が COMPASS / RANGE / MAIN で始まる → ATAK が LRF の誤りとしてトーストを出す（JTAK には来ない）
                       ├ registerAction で登録した動作あり → RangeFinderAction2.onRangeFinderInfo(uid, name, d, a, e) … JTAK
                       └ 登録なし（null）              → ATAK の標準の動作（測った点に ATAK のマーカー）
```

- name は 7 番目の欄。無いときは `"PLRF"`。
- a の欄が `NaN` のときは `Double.parseDouble` で `Double.NaN` になって JTAK に来る。
- **標準の動作への戻し方**: `registerAction((RangeFinderAction2) null)`。
  - `registerAction` はフィールドに入れるだけで、`process` は null なら標準の動作になる。
  - `RangeFinderAction2` は非推奨の `RangeFinderAction` を継承する。キャストを書いて、どちらの多重定義を呼ぶかを明らかにする。
- 受け口の呼ばれるスレッドは UDP の受信のスレッドか Bluetooth のスレッドで、UI のスレッドではない。

## 3. core（`jp.jtak.core.lrf`。ATAK と Android に依存しない純 Java）

### 3.1 型

| 型 | 中身 |
|---|---|
| `enum AzimuthReference` | `MAGNETIC`・`TRUE` |
| `enum InputSource` | `ATAK_LRF`（表示「ATAK の LRF 連携」）・`BLE`（「BLE 直結」）・`MANUAL`（「手入力」）。表示の文字列を持つ |
| `final class GeoPosition` | `latitudeDeg`・`longitudeDeg`・`haeMeters`（WGS84 の楕円体高。不明は `Double.NaN`）。`hasAltitude()` |
| `final class RangeReading` | `distanceMeters`・`azimuthDeg`・`AzimuthReference`・`elevationDeg`（上が正）・`epochMillis`・`InputSource`・`deviceName`（null 可。表示しない。`docs/usage-scope.md` Q7）。方位の基準か入力の方式（source）が null なら `IllegalArgumentException` |
| `enum TargetStatus` | `OK`・`NO_OBSERVER`・`INVALID_OBSERVER`・`INVALID_DISTANCE`・`DISTANCE_OUT_OF_RANGE`・`INVALID_ELEVATION`・`AZIMUTH_UNAVAILABLE`・`INVALID_DECLINATION` |
| `final class TargetSolution` | `status`・`target`（`OK` のときだけ。ほかは null）・`trueAzimuthDeg`・`declinationDeg`（真方位の入力のときは NaN）・`horizontalDistanceMeters`・`heightDifferenceMeters` |

型はすべて不変（final のフィールド、setter なし）。

### 3.2 `Geodesy.direct(latDeg, lonDeg, azimuthDeg, distanceMeters)`

- Vincenty（1975）の順解法（出典 S2）。楕円体は WGS84（a = 6378137 m、1/f = 298.257223563）。
- 反復は σ の変化が 1e-12 rad 未満になるまで。上限は 200 回で、超えたら `IllegalStateException`。
- 距離 0 は入力と同じ点を返す。
- 経度は (-180, 180] にそろえる。
- 使う範囲は LRF の距離（数十 km まで）。対蹠点の近く（約 20,000 km）は対象外と javadoc に書く。

### 3.3 `TargetCalculator.solve(GeoPosition observer, RangeReading reading, double declinationDeg)`

0. reading が null なら `IllegalArgumentException`（呼び出し側の誤り。状態では返さない）。
1. 入力を次の順で確かめ、だめなら `target` が null の結果を返す。
   1. observer が null なら `NO_OBSERVER`。
   2. 緯度・経度が有限でないか範囲外（緯度は ±90、経度は ±180）なら `INVALID_OBSERVER`。
   3. 距離が有限でないか負なら `INVALID_DISTANCE`。
   4. 距離が上限 `MAX_DISTANCE_METERS` = 20,000 m を超えるなら `DISTANCE_OUT_OF_RANGE`（20,000 m ちょうどは置く。2026-11-08 利用者の決定）。
   5. 俯仰が有限でないか ±90 の外なら `INVALID_ELEVATION`。
   6. 方位が有限でない（NaN を含む）なら `AZIMUTH_UNAVAILABLE`。有限なら [0, 360) に正規化してから使う（370 → 10）。
   7. 磁方位で、偏差が有限でないなら `INVALID_DECLINATION`。真方位のときは偏差を見ない。
2. 真方位 = 磁方位 + 偏差。偏差は東が正で、Android の `GeomagneticField.getDeclination()` と同じ向き。値は [0, 360) にそろえる。
3. 水平距離 = d·cos e、高低差 = d·sin e（e はラジアンに直す）。
4. 目標の緯度・経度 = `Geodesy.direct(観測者, 真方位, 水平距離)`。
5. 目標の楕円体高 = 観測者の楕円体高 + 高低差。観測者の高さが不明なら NaN。
6. 地球の曲率と大気の屈折は入れない。
   - 高さの誤差のめやすは 水平距離² / (2R)。1 km で約 0.08 m、5 km で約 2 m、10 km で約 8 m、上限の 20 km で約 31 m。
   - 訓練用の精度として受け入れ、javadoc にこの限界を書く。

### 3.4 文（すべて先頭に `TrainingNotice.text()`。U1）

数は `Locale.ROOT` で書く。方位は小数 1 桁に丸めてから [0, 360) に直して書く（359.96 は `000.0`。`360.0` とは書かない）。

- `LrfRemarks.format(RangeReading, TargetSolution)`: マーカーの説明（remarks）。行の順は次のとおり。
  1. `TRAINING USE ONLY / 訓練用`
  2. `JTAK LRF 測距点`
  3. `入力: <InputSource の表示>`。受け取った機器の名前（deviceName）は付けない（`docs/usage-scope.md` Q7）
  4. `距離: 523.0 m`
  5. 方位
     - 磁方位の入力: `方位: 磁 090.0° / 偏差 +7.5° / 真 097.5°`
     - 真方位の入力: `方位: 真 097.5°`
  6. `俯仰: +5.0°`
  7. `水平距離: 521.0 m / 高低差: +45.6 m`
  8. `目標: 35.681200, 139.767100 / 標高(HAE): 85.6 m`。高さが不明なら `標高(HAE): 不明`
  9. `時刻: 2026-11-08T02:30:00Z`（UTC、`Instant` の ISO-8601）
  10. `TRAINING USE ONLY / 訓練用`
- `LrfMessages.forStatus(TargetStatus, RangeReading)`: `OK` 以外のときに画面に出す文。例:
  - `AZIMUTH_UNAVAILABLE`: `TRAINING USE ONLY / 訓練用 — 方位が取れなかったため点を置きません（距離 523.0 m）`
  - `NO_OBSERVER`: `… — 自分の位置が取れないため点を置きません`
  - `DISTANCE_OUT_OF_RANGE`: `… — 距離が上限 20 km を超えたため点を置きません（距離 25000.0 m）`
  - ほかの状態にも 1 文ずつ用意する。
  - 距離だけの値は捨てない。この文とログに残す（F01 の 5 の決定）。記録は持たない。

### 3.5 試験（JUnit。`core/src/test/java/jp/jtak/core/lrf/`）

| # | 入力 | 期待 | 許容 |
|---|---|---|---|
| T1 | 赤道 (0,0)、真方位 0°・90°・180°・270°、1000 m、俯仰 0 | (0.0090436947696644, 0)・(0, 0.008983152841195215)・(-0.0090436947696644, 0)・(0, -0.008983152841195215) | 1e-9° |
| T2 | 出典 S3 の例: Flinders Peak (-37°57′03.72030″, 144°25′29.52440″)、方位 306°52′05.37″、54972.271 m | Buninyong (-37°39′10.15610″, 143°55′35.38390″) | 1e-6° |
| T3 | 距離 0、俯仰 +10、観測者の高さ 100 m | 観測者と同じ緯度・経度、高さ 100 m | 1e-12 |
| T4 | 1000 m、俯仰 +30 と −30、観測者 100 m | 水平 866.0254 m、高さ 600 m と −400 m | 1e-6 m |
| T5 | 磁 350°・偏差 +15° と、磁 10°・偏差 −15° | 真 5° と 355° | 1e-9° |
| T6 | 真方位の入力、偏差 NaN | `OK`（偏差を見ない） | — |
| T7 | 方位 NaN | `AZIMUTH_UNAVAILABLE`、target null | — |
| T8 | observer null／緯度 91／経度 181／緯度 NaN／距離 −1／距離 NaN／俯仰 91／俯仰 +∞／磁方位で偏差 NaN | それぞれ `NO_OBSERVER`／`INVALID_OBSERVER`×3／`INVALID_DISTANCE`×2／`INVALID_ELEVATION`×2／`INVALID_DECLINATION` | — |
| T8b | 真方位 370°／磁方位 370° の remarks／方位 +∞ | 真方位 10°／`方位: 磁 010.0°` を含む／`AZIMUTH_UNAVAILABLE` | 1e-9° |
| T9 | 観測者の高さ NaN | `OK`、目標の高さ NaN | — |
| T10 | 俯仰 ±90 | 水平 ≈ 0、目標の緯度・経度 ≈ 観測者 | 1e-9° |
| T11 | `LrfRemarks.format` | 先頭と末尾の行が `TrainingNotice.text()`、入力の方式の表示、高さ不明で「不明」 | — |
| T12 | `LrfMessages.forStatus` のすべての状態 | 先頭が `TrainingNotice.text()` | — |
| T13 | 方位の基準が null の `RangeReading` | `IllegalArgumentException` | — |
| T14 | 入力の方式（source）が null の `RangeReading`／`solve(observer, null, 0)` | どちらも `IllegalArgumentException` | — |
| T15 | 距離 20,000 m／20,000.001 m | `OK`／`DISTANCE_OUT_OF_RANGE`（2026-11-08 の決定） | — |
| T16 | deviceName に名前を入れた `RangeReading` の remarks と `forStatus` の文 | 名前がどの行にも出ない（Q7） | — |
| T17 | 磁方位 359.96°・偏差 0 の remarks | 方位の行が `方位: 磁 000.0° / 偏差 +0.0° / 真 000.0°`。`360.0` を含まない（3.4） | — |

T1・T2 の値について:
- T1 の期待値は、赤道での子午線曲率半径 M = a(1−e²) と a から出したもの。順解法の式とは別の手計算による確認。
- T2 の出典 S3 の値は GRS80 の楕円体で出したもの。WGS84 との差は 1e-8° 程度で、許容より小さい。
- S3 には終点での方位 127°10′25.07″ も載っている。これは**逆方位**（終点から始点の向き）で、Vincenty の終点の前方位とは 180° 違う。試験で終点の方位を比べるときは 180° を足して比べる（比べなくてもよい）。
- 値が合わないときは、式や期待値を勝手に直さず、根拠を確かめる。

## 4. app（`jp.jtak.plugin` の下に `lrf` パッケージ）

### 4.1 クラス

| クラス | 役目 |
|---|---|
| `JtakLrfAction implements RangeFinderAction2` | `onRangeFinderInfo(uidPrefix, name, d, a, e)` から `RangeReading`（MAGNETIC、ATAK_LRF、deviceName = name、時刻は受けた時刻）を作り、UI のスレッドに渡して（`MapView.post`）`LrfMarkingController.handle` を呼ぶ |
| `LrfMarkingController` | 自分の位置と偏差を取る → core で計算する → `OK` ならマーカーを置き、そうでなければ文をトーストで出してログに残す |
| `LrfAutoMarkingSwitch` | 自動マーキングの入切（SharedPreferences）と、`registerAction` の登録・解除 |
| ドロップダウン（`JtakDropDownReceiver` と layout） | 自動マーキングの入切のスイッチと、手入力のフォーム |

### 4.2 自分の位置と偏差

- 自分の位置は `MapView.getMapView().getSelfMarker()` の点。
  - マーカーが null か点が `isValid()` でなければ、observer を null にする（core が `NO_OBSERVER` を返す）。
  - 高さは `isAltitudeValid()` のときだけ使う。HAE かどうかは javadoc で確かめる。
- 偏差は `new GeomagneticField(lat, lon, 高さ(m。不明は 0), System.currentTimeMillis()).getDeclination()`。
- GPS がつかめていないときの自分の点（2026-11-08 に実機で確かめた。5.1）:
  - ATAK は、GPS が切れても自分の点を最後に GPS で取れた位置に残す。利用者が自分の位置を手で置くこともできる。
  - このとき点は `isValid()` のままなので、JTAK はその位置から計算して点を置く。0,0 にはならなかった。
  - 2026-11-08 の利用者の決定で、この動きのままにする（ATAK の標準の LRF と同じ）。コードは変えない。
  - `NO_OBSERVER` になるのは、自分の点に位置がまったくないとき（マーカーが null か、点が `isValid()` でないとき）だけ。
  - 画面の左下に NO GPS と出ていても点が置かれるので、利用者は自分の点の位置が正しいかを見てから測る。手で置いた位置の消し方は確かめていない。

### 4.3 マーカー

- `PlacePointTool.MarkerCreator(GeoPoint)`。高さが NaN なら `GeoPoint.UNKNOWN` にする。
- 設定:
  - `setType("a-u-G")`（不明の地上の点）
  - `setCallsign("JTAK LRF HHmmss")`（端末の時刻）
  - `setMetaString("remarks", LrfRemarks.format(...))`
  - `showCotDetails(false)`
  - `placePoint()`
- **JTAK からは送信しない（U8）**:
  - JTAK は CoT を送る API を呼ばない。
  - 置いた点は、利用者が ATAK の機能で手で送れる（`docs/usage-scope.md` Q6。2026-11-08 利用者の決定）。そのため `setNeverPersist(true)` は設定しない。送った点にも説明の TRAINING の行がそのまま付く。
  - `MarkerCreator` の既定は `archive = true` で、`placePoint()` の最後に `Marker.persist` を呼ぶ（main.jar の javap）。
    そのため点は端末に保存され、ATAK を再起動しても残る（2026-11-08 に実機で確かめた。5.1）。端末の中に残るだけで送信ではない（U8 に反しない）。
    訓練のあとに点を消すかは利用者の運用で、JTAK は消さない。
  - 点が自動で送られないことは、2026-11-08 に実機で利用者が確かめた（5.1）。TAK サーバー・ほかの端末につないだ状態では未確認。
- 説明の欄のキー `remarks` が ATAK の詳細の画面に出るかは、実機で確かめる。

### 4.4 自動マーキングの入切と、止めるとき

- 設定のキーは `jtak.lrf.auto_marking`（boolean、既定 true）。入れ物は ATAK の既定の SharedPreferences。
- 入のときは `registerAction(jtakLrfAction)`、切のときは `registerAction((RangeFinderAction2) null)` で ATAK の標準の動作に戻す。
- `JtakMapComponent.onCreate` で設定を読み、入なら登録する。
- `onDestroyImpl` では、JTAK が登録していたなら `registerAction((RangeFinderAction2) null)` で戻す。
- 既知の限界: ATAK には「いま登録されている動作」を読む API がない。ほかのプラグインが JTAK の後に登録していると、JTAK を止めたときにその登録も消える。

### 4.5 手入力

- フォームの欄:
  - 距離（m）
  - 方位（°）
  - 方位の基準（磁／真。既定は磁）
  - 俯仰（°。既定 0）
  - 「点を置く」ボタン
- 入力の方式は `MANUAL`。自動マーキングの入切に関係なく使える。
- 数として読めない欄があれば、`TrainingNotice.text()` を先頭に付けた文をトーストで出す。
- ドロップダウンの上にある `TrainingNotice` の表示は残す（下の表示はなくした）。

## 5. 確かめ方

1. `software/atak-plugin/build.sh assembleDebug :core:test` が成功し、T1〜T12 と、そのあとに足した試験が通る。
2. 実機での確認。`adb` を使う操作は、**行う前に利用者に確認する**（U6）。
   - `adb install -r` で JTAK を入れ直す。
   - 手入力で点が置かれ、説明に TRAINING の行があること。
   - `adb shell "echo '1,JTAKTEST,0,500,90,5' | timeout 3 nc -u 127.0.0.1 17211"` で点が置かれること（実機の nc は `-w1` だと終わらないので、`timeout 3` で止める）。
   - a の欄が `NaN` のときは、点が置かれず文が出ること。
   - 自動マーキングを切ると、同じ送信で ATAK の標準のマーカーになること。
   - 4.2・4.3 の未確認の点（GPS がないときの自分の点、`remarks` の表示、再起動したあとに点が残るか）を確かめ、結果をこの文書に追記する。結果は 5.1（GPS がないときの自分の点も確かめた）。

### 5.1 実機での結果（2026-11-08。実機・ATAK 5.8.0.9）

利用者の承認を得て、`adb install -r` で入れ直し、UDP 17211 に 2 つの行を送った。
確かめ方は logcat と、利用者が画面で見た結果。

| 項目 | 結果 | 根拠 |
|---|---|---|
| `registerAction` で JTAK に渡るか（U-1: 起動の時点で `getInstance()` が null でない） | 確かめた | logcat で、ATAK の受け口が `receive: 1,JTAKTEST,0,500,90,5` を `jp.jtak.plugin.lrf.JtakLrfAction` に渡していた |
| `1,JTAKTEST,0,500,90,5` で点が置かれるか | 確かめた | 利用者が画面で確認。自分の位置から東に約 500 m に点があった（GPS は取れていた） |
| 説明（remarks）の中身 | 確かめた | 利用者が画面で確認。先頭と末尾が `TRAINING USE ONLY / 訓練用`、3 行目が `入力: ATAK の LRF 連携`。`JTAKTEST`（機器名）はどこにも出ない（Q7） |
| 方位が NaN のとき、点を置かず文を出すか | 確かめた | 13:30 に `1,jtak-nan-test,0,500,NaN,0` を UDP 17211 に送った。画面の写真でトースト「TRAINING USE ONLY / 訓練用 — 方位が取れなかったため点を置きません（…」が出た（全文は logcat の `JtakLrf` の行で「（距離 500.0 m）」まで）。地図の点は増えていない（利用者の指示で操作。利用者も画面で見た） |
| 手で送れるか・自動で送られないか（Q6・U8） | 確かめた | 利用者が画面で確認。点の操作に ATAK の普通の点と同じ「送る」があり、ほかの端末・サーバーに自動で届いた様子はなかった |
| 手入力で点が置かれるか | 確かめた | 300 m・真方位 45°・俯仰 0 で「JTAK LRF 132631」が置かれた。点は自分の点から東に約 216 m・北に約 208 m（約 300 m・約 46°）。説明は先頭と末尾に TRAINING の行、`入力: 手入力`、`方位: 真 045.0°` を表示した（利用者の許可を得て操作） |
| 自動マーキングを切ると ATAK の標準のマーカーになるか | 確かめた | 利用者がスイッチを手で切り、ATAK を立ち上げ直したあとも切れたままだった（設定が残る）。切った状態で `1,jtak-off-test,0,400,90,0` を送ると、ATAK の標準の点「GUMBO.PLRF」（54S UE 10397 20049）と赤い線「90°M 400 m」が出た。logcat に JTAK に渡した行（`overriding default behavior ... JtakLrfAction`）はなかった。スイッチを入れ直して `1,jtak-on-test,0,350,60,0` を送ると、logcat で JtakLrfAction に渡り（13:35:46）、「JTAK LRF 133546」（54S UE 10279 20211）が置かれた。自分の点（10000 20000）から東に 279 m・北に 211 m で、約 350 m・真方位 約 53°（磁方位 60° から偏差を引いた値）（利用者の許可を得て操作） |
| 再起動したあとに点が残るか（`archive = true` の既定値） | 確かめた | 利用者が ATAK を立ち上げ直した（プロセスの起動は 13:32:05）。そのあとの地図に「JTAK LRF 132631」「JTAK LRF 125841」が残っていた（画面の写真） |
| GPS が取れていないときの自分の点（U-2） | 確かめた（点が置かれる。利用者の決定でこのままにする。4.2） | 22:57 に端末の位置情報が切れていることを `is-location-enabled` = false で確かめた。23:02 と 23:04 の画面の左下は NO GPS で、自分の点は 54S UE 30000 40000 のまま残っていた。23:00:43 と 23:04:44 に `1,JTAKTEST,0,500,90,5`・`1,JTAKTEST2,0,500,90,5` を UDP 17211 に送ると、logcat で JtakLrfAction に渡り、`JtakLrf` の警告は出なかった。2 回目は「JTAK LRF 230444」が自分の点の東寄りに置かれたことを、利用者と画面の写真で確かめた。手入力は利用者の画面では見えなかったが、トーストも警告も出ていないので、画面の外に置かれたと考えられる（未確認）。 |

logcat には、ATAK 自身の `deleting the rb because the spi was deleted: ANDROID-…PLRF` という行も出ていた。ATAK の標準の LRF の線を消す動きで、JTAK のコードからは出していない。

## 6. 未決

| 項目 | 状態 |
|---|---|
| 置いた点を利用者が ATAK の機能で手で送れるようにするか（`setNeverPersist`） | 決定（2026-11-08 利用者「手で送れるようにする」。`docs/usage-scope.md` Q6。4.3） |
| 受け取った機器の名前を説明に出すか | 決定（2026-11-08 利用者「出さない」。`docs/usage-scope.md` Q7。3.4） |
| 距離の上限 | 決定（2026-11-08 利用者「20 km」。3.3） |
