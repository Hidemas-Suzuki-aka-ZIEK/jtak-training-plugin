# JTAK 取扱説明 — LRF の測距点（F01）と CAS の書式（F02）

> **TRAINING USE ONLY / 訓練用**
>
> JTAK はミルシム・タクティカルトレーニングで JTAC の役を練習するためのプラグイン。
> 実際の作戦・実弾の射撃・実在の航空機の統制には使わない。画面・地図の点・書式の先頭と末尾には、いつも「TRAINING USE ONLY / 訓練用」が出る。この表示を消す設定はない。

- 日付: 2026-11-09
- 対象: ATAK-CIV 5.8.0.9 に読み込んだ JTAK

画面の名前（ボタン・欄）は、プラグインの画面に出る文字のまま「」で書く。

## 1. 画面の開き方と切り替え

1. ATAK のツールバーから JTAK を開く。右側に JTAK の画面が出る。
2. 画面の上に「LRF」「9-line」「Routing」「Check-in」「Game plan」「RED」のボタンがある。押すと、その画面に切り替わる。
3. 画面のいちばん上の「TRAINING USE ONLY / 訓練用」の行は、どの画面でも出たままになる。

## 2. LRF の測距点を地図に置く（F01）

レーザー測距器（LRF）で測った距離と方位から、目標の点を地図に置く。

### 2.1 入力の経路

| 経路 | 使い方 | 状態 |
|---|---|---|
| ATAK の LRF 連携 | ATAK が対応している Bluetooth の LRF をつなぐ。または、UDP の 17211 番に `1,uid,時刻,距離,方位,俯仰[,名前]` の形の行を送る | 使える |
| 手入力 | 「LRF」の画面の「手入力」の欄に打ち込む（2.4） | 使える |
| BLE 直結 | LRF を JTAK に直接つなぐ | まだ使えない（機種が決まってから作る） |

### 2.2 自動マーキングのスイッチ

- 「LRF」の画面の「自動マーキング（ATAK の LRF 連携）」で入と切を切り替える。最初は入になっている。
- 選んだ状態は、ATAK を立ち上げ直しても残る。
- 入のとき: ATAK の LRF 連携で届いた測定は JTAK が受け取り、点を置く。
- 切のとき: ATAK の標準の動き（自分の位置からの赤い線と点）に戻る。手入力は切のときも使える。
- ATAK の LRF 連携の行の距離の欄が `COMPASS`・`RANGE`・`MAIN` で始まるときは、測定の誤りとして ATAK が知らせを出す。JTAK は点を置かない。

### 2.3 置かれる点

- 自分の位置から、測った距離と方位の先に点を置く。
  - 方位が磁方位のときは、端末で計算した偏差を足して真方位にしてから置く。
  - 地球の丸みは計算に入れていない。
- 点の種類は「不明（unknown）」の地上の点。名前は「JTAK LRF HHmmss」（HHmmss は端末の時刻）。
- 点の説明（remarks）には、次の順で書く。測った機器の名前は書かない。
  1. TRAINING USE ONLY / 訓練用
  2. JTAK LRF 測距点
  3. 入力: ATAK の LRF 連携 / BLE 直結 / 手入力 のどれか
  4. 距離
  5. 方位（磁方位の入力なら「方位: 磁 090.0° / 偏差 +7.5° / 真 097.5°」の形。真方位の入力なら「方位: 真 …」）
  6. 俯仰
  7. 水平距離 / 高低差
  8. 目標の緯度・経度 / 標高（HAE。分からないときは「不明」）
  9. 時刻（UTC）
  10. TRAINING USE ONLY / 訓練用
- 点は端末に保存され、ATAK を立ち上げ直しても残る。JTAK は点を消さない。要らなくなった点は ATAK の操作で消す。
- JTAK は点をほかの端末へ送らない。送りたいときは、ATAK の「送る」で手で送る。送った点の説明にも TRAINING の行が付いている。

### 2.4 手入力

1. 「LRF」の画面の「手入力」の欄に、次を入れる。
   - 「距離（m）」
   - 「方位（°）」と、その基準（「磁方位」か「真方位」。最初は磁方位）
   - 「俯仰（°。上が正）」（最初は 0）
2. 「点を置く」を押す。
3. 数として読めない欄があると、「数として読めない欄があるため点を置きません」と、TRAINING の行を先頭にした知らせが出る。

### 2.5 点が置かれないとき

次の場合は点を置かず、画面に知らせを出す。知らせはどれも「TRAINING USE ONLY / 訓練用 — 」で始まり、最後の括弧に測った距離が入る。

| 場合 | 画面の知らせ（例） |
|---|---|
| 距離が 20 km を超える（20,000 m ちょうどは置く） | 「TRAINING USE ONLY / 訓練用 — 距離が上限 20 km を超えたため点を置きません（距離 25000.0 m）」 |
| 方位が取れなかった | 「TRAINING USE ONLY / 訓練用 — 方位が取れなかったため点を置きません（距離 500.0 m）」 |
| 自分の位置が取れない | 「TRAINING USE ONLY / 訓練用 — 自分の位置が取れないため点を置きません（距離 500.0 m）」 |

### 2.6 GPS がないときの注意

GPS が取れないときは、地図の上の自分の点の最後の位置から点を置く。画面に NO GPS と出ていても点は置かれるので、測る前に自分の点の位置が合っているかを見る。

## 3. CAS の書式を作る（F02）

「9-line」「Routing」「Check-in」「Game plan」の画面で、地図の点と入力から、読み上げ用の書式を作る。共通の動きは次のとおり。

- 地図の点を選んで「地図の点から入れる」を押すと、計算できる欄に値が入る。
- 自動で入った値も、手で直せる。
- 「作る」を押すと、結果が画面に出る。結果の文字は選んでコピーできる。
- 結果の先頭と末尾には、TRAINING の行が入る。
- JTAK が書式をほかの端末へ自分で送ることはない。画面に出すほか、テキスト・PDF のファイルに書き出せる（F04。使い方は `F04-export.md`）。送るときは、書き出したあとに開く共有の画面で、使う人が送り先を選ぶ。
- 欄の名前は書式の項目名のまま英語で出し、短い日本語の説明を添えている。
- 選ぶ欄の最初の値は「（選ばない）」。

### 3.1 地図の点の選び方

- 一覧には、地図の点が名前（callsign）で出る。自分の点も名前で出る。
- 同じ名前の点が 2 つ以上あるときは、座標を添えて区別する（例: `IP1 1 (34.9640, 138.8533)`）。大文字と小文字の違いだけの名前も、同じ名前として数える。
- ATAK が中で使う点（`TARGET_POINT`）と、測距線（Range & Bearing）の端点は一覧に出ない。
- 点を足したり消したりしたあとは、「点の一覧を読み直す」を押す。一覧の上に「地図の点: N 件」と出る。

### 3.2 9-line（CAS brief）

画面の題は「CAS 9-line と Remarks（JP 3-09.3 2014 Fig. V-9）」。

1. 3 つの点を選ぶ。
   - 「IP/BP の点」と、その種類（「IP（距離は NM）」か「BP（距離は m）」。最初は IP）
   - 「目標の点」
   - 「味方の点」
2. 「地図の点から入れる」を押す。1・2・3・4・5・6・8 行目に値が入り、「地図の点から入れました」と出る。値が入らなかった行は「入らなかった行: …」と出る。
   - 取れない値の欄は空になる。手で入れていた値も消えるので、手で入れる欄は、このボタンのあとに入れる。
3. 残りの欄を手で入れ、自動で入った値も必要なら直す。
4. 「作る」を押す。

自動で入る値:

| 行 | 中身 | 例 |
|---|---|---|
| 1 IP/BP | IP/BP の点の名前 | `IP1` |
| 2 Heading | IP/BP から目標への磁方位。整数の 3 桁で、0 は 360 と書く | `084` |
| 3 Distance | IP なら NM で小数 1 桁。BP なら 100 m 単位 | `29.7 NM`・`1200 meters` |
| 4 Target elevation | 目標の点の標高（MSL）を feet で書く。点に高さがないときは空 | `1250 feet` |
| 5 Target description | 目標の点が敵の点なら、その種類から下書きを入れる。敵でない点や、表にない種類なら空 | — |
| 6 Target location | 目標の点の MGRS | `54S UD 08816 72272` |
| 8 Location of friendlies | 目標から見た、いちばん近い味方 1 つの方角（真北基準）と距離（整数の m）。目標と同じ点の味方があるときは空 | `East 318` |

手で入れる欄:

- 7 行目「7 Type mark / Terminal guidance」と 9 行目「9 Egress」。9 行目の頭に `Egress` がなければ、結果に足して出す。
- 「Offset（left / right）」。2 行目に添えて「306, Offset left」の形で出る。
- 「Remarks」の 5 つの欄: 「LTL/PTL」「Ordnance / Effects」「Threat / SEAD」「Additional remarks」「Additional calls」。入れた項目だけが結果に出る。
- 「Restrictions」の欄（3.6）。

結果の並び:

- 先頭に TRAINING の行、次に `CAS 9-LINE (JP 3-09.3 2014 Fig. V-9) / Datum: WGS 84`。
- 節は [1-3]・[4-6]・[7-9]・[Remarks]・[Restrictions] の順。末尾も TRAINING の行。
- 4 行目と 6 行目には、読み返しを求める印 `(readback)` が付く。
- 入っていない行は `（未入力）` と出て、画面に「未入力の行: 5, 8」の形で知らせる。
- 1〜3 行目がすべて空のときは [1-3] を出さず、4 行目を `Elevation …` の形で出す。

### 3.3 Routing

画面の題は「Routing（JP 3-09.3 2014 Fig. V-3）」。

- 「Aircraft callsign（呼び出し名）」「Routing / Holding の指示」（「Proceed … and report established」「Maintain …」「Report passing …」から選ぶ）と「場所（点の名前・高度など）」を入れる。
- 場所は手で入れるか、「場所にする点」で地図の点を選んで「地図の点から入れる」を押す。点の名前が入り、「地図の点の名前を場所に入れました」と出る。点を選んでいないときは「点を選んでください」と出る。
- そのほかの欄: 「Other instructions」「You are the only aircraft on station」「Other AOS（ほかの機）」「Immediate threats」「Significant weather/terrain」「Status calls」。
- 「結びの指示」で「Send your check-in」か「Hold your check-in」を選ぶ。Hold のときは「Hold の理由」を入れてもよい（文末のピリオドは要らない）。
- 「You are the only aircraft on station」と「Other AOS」は同時には入れられない。両方を入れると、どちらかを空にするよう知らせが出る。

### 3.4 Check-in

画面の題は「Check-in（JP 3-09.3 2014 Fig. V-4）」。

- 「JTAC callsign」と「Aircraft callsign」を入れる。
- 「言い方」で「全項目を言う」「As fragged」「As fragged, with exception」のどれかを選ぶ。
- 項目は「1 Mission number」「2 Number and type of aircraft」「3 Position and altitude」「4 Ordnance」「5 Playtime」「6 Capabilities」「7 Abort code」。
- 「Abort code なし（ABORT, ABORT, ABORT）」と「7 Abort code」は同時には入れられない。

### 3.5 Game plan

画面の題は「Game plan（JP 3-09.3 2014 Fig. V-9）」。

- 「Aircraft callsign」を入れる。
- 「Type of control」で Type 1〜3 を選ぶ。画面に出る説明（訓練用の要約）:
  - Type 1: 統制する側が、攻撃の最終段階で目標と攻撃機を目で捉え、攻撃ごとに統制する
  - Type 2: 統制する側が、目視以外の手段で危険を減らし、攻撃ごとに統制する
  - Type 3: 統制する側が、目視以外の手段で危険を減らし、同じ交戦の中で複数の攻撃を認める
- 「Method of attack」で BOT か BOC を選ぶ。
  - BOT: 機が、目標または狙う点を自分で捉える
  - BOC: 機が、CAS Brief で渡された座標に対して攻撃する
- Type と BOT/BOC は別々に選ぶ。どの組み合わせでもよい。
- そのほかの欄: 「Attack intent」「CDE concerns」「Ordnance」「Desired effects」「Fuzing」「Interval」「Line 6 の桁数」。

### 3.6 Restrictions

「9-line」の画面にある。

- 欄: 「Danger close」「Danger close の指揮官のイニシャル」「ACAs」「FAHs」「Separation」「RW approval out of BP」「PLA」「TOT の言い方」「TOT の時刻」。
- 「TOT の言い方」は「TOT（時刻を入れる）」「Standby TOT」「TOT will be issued after correlation」「Push when ready」から選ぶ。
- イニシャルは「Danger close」を付けたときだけ、「TOT の時刻」は「TOT の言い方」が時刻のときだけ入れられる。食い違うと知らせが出る。
- 入れるべき欄が空なら「未入力の Restrictions: …」と出る。

### 3.7 必須の欄が空のとき

Routing・Check-in・Game plan では、必須の欄が空のまま「作る」を押すと、「未入力の項目: …」と画面に出る。

### 3.8 扱わないもの

次のものは専用の欄がない。必要なら、手入力の欄（Remarks・Other instructions など）に書く。

- Situation Update
- 複数の機による攻撃、複数の目標
- GEOREF の座標
- 動く目標

## 4. 実機で確かめたことと、まだのこと

確かめたこと:

- LRF の測距点の置き方（2 章）は、実機の ATAK で確かめた。
- 9-line の自動の値は、2026-11-08 に実機で次の値になった: 2 行目 `084`、3 行目 `2.7 NM`、4 行目 `965 feet`、6 行目 `54S UD 08816 72272`、8 行目 `East 318`。「地図の点から入れました」も出た。

まだ確かめていないこと:

- 4 行目の標高を、地図の標高と照らし合わせること。8 行目の方角と距離を、別の計算で照らし合わせること。
- Routing・Check-in・Game plan・Restrictions の画面の実機での見え方（ボタンの並び・画面の長さ）。
- LRF の点を TAK サーバーやほかの端末へ送ったときの見え方。
- GPS がないときの手入力で、点が画面の外に置かれたかどうか。
- 手で置いた自分の位置の消し方。

## 5. 出典

- 機能仕様: `docs/functional-spec/F01-lrf-marking.md`・`F02-cas-products.md`
- 詳細設計: `docs/detailed-design/01-lrf-marking.md`・`02-cas-brief.md`・`03-cas-other-products.md`
- 画面の文言: `software/atak-plugin/app/src/main/res/values/strings.xml`、LRF の知らせは `software/atak-plugin/core/src/main/java/jp/jtak/core/lrf/LrfMessages.java`
- 書式の元: JP 3-09.3, *Close Air Support*, 25 November 2014（`docs/references.md` S4）。日本語訳は `docs/manuals/jp3-09-3/`。
