# インタフェース仕様

- 関連: F01（LRF）、F03（兵器の表）、SDK の javadoc（`docs/references.md` の S1）、ADR 0003、3 章の読み込みは F02 の「ファイルから読み込む」

## 1. LRF の入力

### 1.1 `core` にそろえる形（測距の値）

| 項目 | 型 | 単位・意味 |
|---|---|---|
| `distanceM` | double | 距離（m）。0 以上・20,000 以下（`docs/detailed-design/01-lrf-marking.md` 3.3） |
| `azimuthDeg` | double | 方位（°）。0〜360 の外は 0〜360 に直す。NaN のことがある |
| `azimuthRef` | enum | `MAGNETIC`（磁方位）か `TRUE`（真方位） |
| `elevationDeg` | double | 俯仰（°、上が正） |
| `timestamp` | long | 受けた時刻（ms、エポックから） |
| `source` | enum | `ATAK_LRF`（下の 1.2）・`BLE`（1.3）・`MANUAL`（1.4） |
| `deviceName` | String | 入力の機器の名前（なければ null）。画面・説明・ログには出さない（`docs/usage-scope.md` Q7） |

型の名前・クラスの形は詳細設計で決める。

### 1.2 ATAK 標準の LRF 連携

- 受け口: `LocalRangeFinderInput.getInstance().registerAction(RangeFinderAction2)`。
- `onRangeFinderInfo(String uidPrefix, String name, double distance, double azimuth, double elevation)`:
  distance は m、azimuth は**磁方位**の °（偏差の補正なし。`Double.NaN` のことがある）、elevation は °（上が正）。
  `azimuthRef = MAGNETIC`、`source = ATAK_LRF`、`deviceName = name` にそろえる。
- ATAK はこの経路に、UDP の 17211 番で受けたデータも渡す。UDP の形式（ATAK が受けるもの。JTAK は直接受けない）:

```
specversion,uid,timestamp,d,a,e
例: 1,LRF-TEST,1762560000000,850.0,123.4,2.1
```

  specversion は 1、d は距離（m）、a は磁方位（°）、e は俯仰（°）。試験で LRF の代わりに使える。

### 1.3 BLE 直結

- 機種が決まってから書く（`docs/usage-scope.md` Q2）。機種ごとのパーサが 1.1 の形にそろえる。
- 機種の通信の仕様は、公開されているもの（メーカーの公開資料）だけを使い、`docs/references.md` に書く。

### 1.4 手入力

- ドロップダウンの入力欄: 距離（m）、方位（°）と基準（磁/真）、俯仰（°、既定 0）。
- 次の値は点を置かず、理由を出す: 距離が数でない・0 より小さい・20 km を超える、方位が数でない、俯仰が −90〜90 の外。
  方位が 0〜360 の外なら 0〜360 に直して使う（370 → 10）。距離 0 は受け付ける（自分の位置に点を置く）。
  決まりは入力の方式（ATAK の LRF・BLE・手入力）によらず同じで、`core` の `TargetCalculator` が判定する（`docs/detailed-design/01-lrf-marking.md` 3.3）。

## 2. 兵器の表（`weapons.json`）

場所は ATAK の作業のフォルダの `tools/jtak/weapons.json`（`FileSystemUtils.getItem`）。利用者は JTAK の画面の中で編集する。
検査の細かい決まり・エラーの出し方・保存の手順は `docs/detailed-design/04-weapon-effects.md` 2.3・3.5 が正本。

```json
{
  "schemaVersion": 1,
  "notice": "TRAINING USE ONLY / 訓練用。訓練用サンプル値であり、実際の安全距離・危害半径ではない。",
  "trainingSampleValues": true,
  "entries": [
    {
      "id": "sample-small",
      "label": "サンプル 小",
      "rings": [
        { "name": "内側", "radiusM": 100 },
        { "name": "外側", "radiusM": 300 }
      ],
      "note": "訓練用サンプル値。実在の兵器の値ではない"
    }
  ]
}
```

| 項目 | 決まり |
|---|---|
| （全体） | UTF-8 の JSON、256 KiB まで。知らないキー・同じキーはエラー |
| `schemaVersion` | 整数の `1`。ほかの値は読まない |
| `notice` | 必須で、空でない文字列。**画面・円の説明には出さない**（出す文言は core の定数。04 の 2.6）。保存のたびに JTAK が決まった文で上書きする |
| `trainingSampleValues` | 必須で `true`。`false` か欠けていたら読まない（`docs/usage-scope.md` U2） |
| `entries` | 1〜50 行 |
| `entries[].id` | 空でない、64 文字まで、英小文字・数字・`-` だけ。表の中で重ならない |
| `entries[].label` | 画面に出す名前。空でない、40 文字まで。既定の表は汎用の区分だけ（U3・Q3） |
| `entries[].rings` | 1〜3 個。内側から外側の順 |
| `entries[].rings[].name` | 空でない、20 文字まで |
| `entries[].rings[].radiusM` | 整数の書き方（`100.0`・`1e2` は不可）で 1〜50000（m）。同じ行の中で前の輪より大きい |
| `entries[].note` | なくてよい。あれば 200 文字まで |

- 文字数は Unicode の符号位置で数える。
- 読めない・検査に通らないファイルは使わず、既定の表を使って理由を画面に出す（04 の 3.5）。
- 上の値は既定の表の 1 行で、出典のない、意味のない切りのよい数（04 の 2.4）。

## 3. 読み込むファイル（CAS 系の書式）

F02 の「ファイルから読み込む」で読むテキストのファイル。形は F04 で書き出すテキストと同じ（`docs/detailed-design/05-export.md`）。
読み方の細かい決まり・エラーの出し方は `docs/detailed-design/06-scenario-import.md` が正本。

### 3.1 ファイルの決まり

- UTF-8（BOM はあってもよい）。改行は LF・CRLF のどちらでもよい。大きさは 256 KiB まで。
- 拡張子は `.txt`。場所は問わない（選ぶ画面は `atak/export/jtak/` から始まる）。
- 1 つのファイルに 1 つの書式だけを書く。
- `TRAINING USE ONLY / 訓練用` の行はあってもなくてもよい。読むときは飛ばす。読み込んだあとの画面・書き出しには、今までどおりこの表示が入る（usage-scope U1）。
- 空でない最初の行（U1 の行を除く）が題名。下の 4 つのどれかと完全に一致しないファイルは読み込まない。

| 題名の行 | 書式 |
|---|---|
| `ROUTING (JP 3-09.3 2014 Fig. V-3)` | ルーティング |
| `CHECK-IN (JP 3-09.3 2014 Fig. V-4)` | チェックイン |
| `GAME PLAN (JP 3-09.3 2014 Fig. V-9)` | ゲームプラン |
| `CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84` | CAS Brief（9-line・Remarks・Restrictions） |

- 値が `（未入力）` の項目と、読めない項目は飛ばし、読み込んだあとに飛ばした項目を知らせる。
- 行の順番は書き出しと同じにする。下の「書いてもよい行」は、書かない（行ごと消す）こともできる。

### 3.2 書式ごとの行

**ルーティング**

1. 本文の行: `[コールサイン, ]proceed <場所> and report established.`、`[コールサイン, ]maintain <場所>.`、`[コールサイン, ]report passing <場所>.` のどれか（コールサインがないときは先頭を大文字にする）
2. 書いてもよい行（この順）: `Other instructions: `、`You are the only aircraft on station.`、`Other AOS: `、`Immediate threats: `、`Significant weather/terrain: `、`Status calls: `
   - `You are the only aircraft on station.` と `Other AOS:` は、どちらか一方だけにする
3. 結びの行（書いてもよい）: `Send your check-in.`、`Hold your check-in.`、`Hold your check-in, <理由>.`

**チェックイン**

1. `<JTAC のコールサイン>, this is <航空機のコールサイン>`
2. 書いてもよい行: `As fragged.`、`As fragged, with exception:`（どちらもなければ全項目の形）
3. `1 Mission number: `〜`6 Capabilities: `、`7 Abort code: <コード> (readback)` または `7 Abort code: none (standard ABORT, ABORT, ABORT)`

**ゲームプラン**

1. `[コールサイン, ]advise when ready for game plan.`
2. `[コールサイン, ]<Type 1|Type 2|Type 3>, <bomb on target|bomb on coordinate>` に、書いてもよい部分を `, intent `、`, CDE concerns `、`, ordnance `、`, effects `、`, fuzing `、`, interval `、`, line 6 <N> digits` の順で続け、`; advise when ready for 9-line.` で終える
   - 値の中に `, ` や `; ` を書いてよい（2026-11-09 利用者の決定）。読むときはラベルを手がかりに分ける。
     値に書けないのは、ラベルの文字列（`, intent `〜`, interval `、`, line 6 `）だけ（`docs/detailed-design/06-scenario-import.md` 2.4.3）

**CAS Brief**

1. `[1-3]` の段（書いてもよい。書かないときは 4 行目を `4 Target elevation: Elevation <値> (readback)` とする）: `1 IP/BP: `、`2 Heading: <値>[, Offset <値>]`、`3 Distance: `
2. `[4-6]` の段: `4 Target elevation: <値> (readback)`、`5 Target description: `、`6 Target location: <値> (readback)`
3. `[7-9]` の段: `7 Type mark / Terminal guidance: `、`8 Location of friendlies: `、`9 Egress: Egress <値>`
4. `[Remarks]` の段（書いてもよい。行ごとに書いてもよい）: `LTL/PTL: `、`Ordnance / Effects: `、`Threat / SEAD: `、`Additional remarks: `、`Additional calls: `
5. `[Restrictions]` の段（書いてもよい。行ごとに書いてもよい）:
   - `Danger close, initials <頭文字> (readback)`
   - `ACAs: <値> (readback)`、`FAHs: <値> (readback)`、`Separation: <値> (readback)`、`RW approval out of BP: <値> (readback)`、`PLA: <値> (readback)`
   - TOT はどれか 1 行: `TOT <時刻> (readback)`、`Standby TOT (readback)`、`TOT will be issued after correlation (readback)`、`Push when ready (readback)`

### 3.3 ひな形とデモのファイル

`docs/interfaces/scenario-samples/` に置く。どれも先頭と末尾に `TRAINING USE ONLY / 訓練用` の行がある。

| ファイル | 中身 |
|---|---|
| `template/jtak-template-routing.txt` | ルーティングのひな形 |
| `template/jtak-template-check-in.txt` | チェックインのひな形（全項目の形） |
| `template/jtak-template-game-plan.txt` | ゲームプランのひな形 |
| `template/jtak-template-cas-brief.txt` | CAS Brief のひな形（TOT と Danger close の行は 3.2 から選んで足す） |
| `demo/jtak-demo-1-routing.txt` 〜 `demo/jtak-demo-4-cas-brief.txt` | 1 つの架空の訓練の場面を、ルーティング → チェックイン → ゲームプラン → CAS Brief の順に書いたデモ |

- ひな形は、`（未入力）` を値に書き換えて使う。`（未入力）` のまま残した項目は読み込まない。
- ルーティングとゲームプランのひな形には、コールサインの場所がない。入れるときは、本文の行の頭に `<コールサイン>, ` を書き、続く文の頭を小文字にする（3.2。デモのファイルが例）。
- デモのコールサイン（OWL 21・KESTREL 11）・地名（IP PINE）・座標は架空のもの。Ordnance などには実在の兵器の名前を書いている（usage-scope Q13。デモのファイルだけ）。
