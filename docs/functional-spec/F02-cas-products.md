# F02: CAS 系の書式の生成

- 要件: R2・R5（`docs/requirements.md`）
- 関連: `docs/usage-scope.md` U1・U4・Q4、`docs/references.md`、F01・F04

## 目的

訓練の中で JTAC の役の人が読み上げる書式を、地図の上の点（F01 の目標・味方・IP/BP など）と入力から組み立てる。

## 対象の書式

| 書式 | 使う場面（訓練の中） |
|---|---|
| ルーティング | 航空機（の役）に、経路・待機の指示と、ほかの機・脅威を伝える |
| チェックイン | 航空機（の役）からの到着の報告を受けて記録する |
| ゲームプラン | 統制の種類（Type 1〜3）と攻撃の方法（BOT・BOC）を、9-line の前に伝える |
| CAS Brief | 攻撃の指示の本体（9-line を含む） |
| Restrictions | 制限（Danger close・空域・攻撃の方向・TOT など）を、CAS Brief の Remarks の後ろで伝える |
| 9-line | 目標の情報を決まった並びで伝える |

## 項目の決め方

- **各書式の項目・並び・用語は、公開資料で確かめてから決める**（`docs/usage-scope.md` U4）。
  出典の資料は利用者と決め（Q4）、`docs/references.md` に書いてから、この節に項目の表を書く。
- 出典が決まるまで、この機能の実装に入らない（2026-11-08 に決まった。下の表）。
- 項目ごとに、値の出どころ（地図の点から自動・F01 の計算・手入力・既定値）を表にする。
- 出典は Q4 で決めた JP 3-09.3 の 2014 年版（`docs/references.md` S4）。下の表のページは PDF の通し番号と本文のページ番号（V-25 など）。

### 9-line（出典 S4 の図 V-9「Game Plan and CAS 9-Line Briefing」、PDF 242 ページ・V-25）

図の注記（V-25）: 行の番号は読み上げない。単位は、事前に伝えていなければ標準のもの。4 行目・6 行目と Restrictions は必ず復唱させる（JTAC はほかの行の復唱も求めてよい）。
読み上げは 3 行ずつ 3 回に分けて送る（PDF 243 ページ・V-26）。

| 行 | 項目（出典の名前） | 単位・書き方（出典） | 値の出どころ | 出典のページ |
|---|---|---|---|---|
| 1 | IP/BP | IP か BP の名前。急ぎの BP（Hasty BP）なら中心の位置 | 手入力（名前）。地図の点を選ぶと、その点の名前を入れる | PDF 243・V-26 |
| 2 | Heading | IP/BP（BP は中心）から目標への磁方位。3 桁で言う。制限があるときだけ「Offset left/right」を付ける | 自動（IP/BP の点から目標の点への方位を、IP/BP の点の偏差で磁方位に直す）。Offset は手入力 | PDF 243–244・V-26–V-27 |
| 3 | Distance | IP からは NM（0.1 まで）、BP からは m（100 m 単位） | 自動（IP/BP の点から目標の点までの距離）。IP か BP かは使う人が選ぶ | PDF 244・V-27 |
| 4 | Target elevation | feet MSL。数字を 1 つずつ読み、最後に「feet」。MSL 以外のときは基準を言う。1〜3 行目を省いたときは「Elevation」から始める | 自動（目標の点の高さを MSL に直し、feet にする）。点に高さがなければ手入力 | PDF 244・V-27 |
| 5 | Target description | 具体的で平易な言葉で | 半自動: 敵の点（type が `a-h-` で始まる点）を目標に選ぶと、点の種類から下書きを入れ、利用者が直す・書き足す。言葉は MIL-STD-2525E w/Change 1 の名前（`docs/references.md` S6・S7）。表にない種類と、敵でない点は空にする。`docs/usage-scope.md` Q8、`docs/detailed-design/02-cas-brief.md` 2.3.1 | PDF 244・V-27 |
| 6 | Target location | 格子座標（100,000 m の区画の記号を含む）・緯度経度（形式を言う）・既知の点からのずれ。測地系を示す。東距と北距の間で区切って読む | 自動（目標の点の MGRS）。ほかの形は手入力 | PDF 244–246・V-27–V-29 |
| 7 | Type Mark / Terminal Guidance | 印の種類（煙・レーザー・IR など）とレーザーのコード。終末誘導なら、照射する側の呼び出し符号・「laser」・コード | 手入力 | PDF 246・V-29 |
| 8 | Location of Friendlies | 目標から見た方角（N・NE・E・SE・S・SW・W・NW）と、いちばん近い味方までの m（例「South 300」） | 自動（目標の点と、選んだ味方の点から）。直すときは手入力 | PDF 246–247・V-29–V-30 |
| 9 | Egress | 「Egress」から始める。方角か経由の点。高度を言うのが望ましい（例「Egress North to Moon, block 15-17」） | 手入力 | PDF 247・V-30 |

### Remarks（出典 S4 の図 V-9 と PDF 247–248・V-30–V-31）

前に伝えていない項目で、当てはまるものだけを読む。並びは図 V-9 のとおり。

| 順 | 項目（出典の名前） | 値の出どころ |
|---|---|---|
| 1 | LTL/PTL | 手入力 |
| 2 | Desired type/number of ordnance or weapons effects | 手入力 |
| 3 | Surface-to-air threat, location, and type of SEAD | 手入力 |
| 4 | Additional remarks（GTL・天気・危険・味方の印など） | 手入力 |
| 5 | Additional calls requested | 手入力 |

本文（V-30–V-31）の推奨の並びは、図と少し違う（Additional fires を挙げ、Additional calls を Additional remarks の前に置く）。JTAK は図の並びにそろえ、Additional fires は 4 の欄に書いてもらう。

### ルーティング（出典 S4 の図 V-3 と PDF 231–232・V-14–V-15）

チェックインの直後に、JTAC が航空機（の役）に次の順で伝える（V-14）。少なくとも「maintain」の指示を出す。

| 順 | 項目（出典の名前） | 書き方（出典） | 値の出どころ | 出典のページ |
|---|---|---|---|---|
| 0 | 呼びかけ | 航空機の呼び出し符号（図 V-3 の例はすべて呼び出し符号で始まる。例「Razor 53, …」） | 手入力（空なら付けない） | PDF 231・V-14 |
| 1 | Aircraft Routing/Holding Instructions | 図 V-3 の 3 つの言い方（「Proceed xxxx and report established」「Maintain xxxx」「Report passing xxxx」）。例「Razor 53, maintain Chevy-Dodge 14-15」 | 指示の種類は選ぶ。場所・高度は手入力（地図の点を選ぶと、その点の名前を入れる）。ほかの指示は手入力 | PDF 231・V-14 |
| 2 | Other AOS | ほかの航空機の呼び出し符号・高度・周波数。いなければ、そう言う（例「you are the only aircraft on station」） | 手入力。「ほかの機はいない」は利用者が選んだときだけ出す | PDF 231・V-14 |
| 3 | Immediate threats | 脅威と場所 | 手入力 | PDF 232・V-15 |
| 4 | Significant weather/terrain | 天気・地形 | 手入力 | PDF 232・V-15 |
| 5 | Status calls（求めるとき） | 経路の途中で報告を求める | 手入力 | PDF 232・V-15 |
| 6 | 結び | 「send your check-in」か「hold your check-in」（理由を付けてよい。例「attack in progress」） | 選ぶ（言わない、も選べる）。理由は手入力 | PDF 232–233・V-15–V-16 |

### チェックイン（出典 S4 の図 V-4、PDF 233・V-16）

航空機（の役）が JTAC に送る書式。JTAK は**受けた内容を記録する欄**として出す（JTAC が読み上げる書式ではない）。並びは図 V-4 のとおり（MNPOPCA。V-16–V-17）。
「as fragged」「with exception」で短くしてよい（V-15）。

| 順 | 項目（出典の名前） | 書き方（出典） | 値の出どころ | 出典のページ |
|---|---|---|---|---|
| 0 | 呼びかけ | 「(JTAC c/s), this is (Aircraft c/s)」 | 手入力（JTAC と航空機の呼び出し符号） | PDF 233・V-16 |
| 1 | Mission Number | ATO の任務番号 | 手入力 | PDF 233・V-16 |
| 2 | Number and type of aircraft | 例「2 by F/A-18C」 | 手入力 | PDF 233・V-16 |
| 3 | Position and altitude | 位置と高度 | 手入力 | PDF 233・V-16 |
| 4 | Ordnance | 兵装・レーザーのコード・信管 | 手入力 | PDF 233・V-16 |
| 5 | Playtime or Time on Station | 残りの時間 | 手入力 | PDF 233・V-16 |
| 6 | Capabilities | FAC(A)・センサー・Link-16・VDL・SITREP・地図の版や GRG・UAS の通信断の手順 | 手入力（1 つの欄。中の項目は入力の欄の説明に出す） | PDF 233–234・V-16–V-17 |
| 7 | Abort code | 受けたコード。受けたら JTAC は復唱する。言われなければ「ABORT, ABORT, ABORT」が標準 | 手入力。「コードなし（標準）」は利用者が選んだときだけ出す | PDF 233–234・V-16–V-17 |

### ゲームプラン（Types of Control・Methods of Attack）（出典 S4 の図 V-9、PDF 237・V-20、図 III-7 と PDF 114・III-47）

9-line の前に、CAS Brief とは別のまとまりとして伝える（利用者の決定 2026-11-08「記載は CAS Brief とは分ける」）。
本文（V-20）: 少なくとも統制の種類（Type of control）と攻撃の方法（Method of attack）を入れる。9-line と同じことは繰り返さない。

| 順 | 項目（出典の名前） | 書き方（出典） | 値の出どころ | 出典のページ |
|---|---|---|---|---|
| 0 | 呼びかけ | 「____, advise when ready for game plan.」 | 手入力（航空機の呼び出し符号） | PDF 242・V-25 |
| 1 | Type of control | 「Type 1」「Type 2」「Type 3」 | 選ぶ（必須） | PDF 114・III-47、PDF 242・V-25 |
| 2 | Method of attack | 「bomb on target」（BOT）か「bomb on coordinate」（BOC） | 選ぶ（必須） | PDF 114・III-47、PDF 237・V-20 |
| 3 | Additional information | ground commander's intent・collateral damage concerns・ordnance requested・desired effects・fuzing・interval・6 行目の桁数 | 手入力（それぞれ空でよい） | PDF 237・V-20 |
| 4 | 結び | 「Advise when ready for 9-line.」 | 固定 | PDF 242・V-25 |

図 III-7 の Type 1〜3 と BOT・BOC の説明は、選ぶ欄の説明として画面に出す（読み上げの文には入れない）。

### Restrictions（出典 S4 の図 V-9 と PDF 248–250・V-31–V-33）

当てはまるものだけを伝え、伝えたものはすべて復唱させる（V-31）。CAS Brief の Remarks の後ろに置く。
並びは本文（V-32）の番号に従う。図 V-9 には FAH・ACA・Danger close・TOT/TTT の 4 つだけがこの順で載る。

| 順 | 項目（出典の名前） | 書き方（出典） | 値の出どころ | 出典のページ |
|---|---|---|---|---|
| 1 | Danger close | 当てはまるときだけ。指揮官のイニシャルを付ける | 手入力 | PDF 249・V-32 |
| 2 | ACAs | 正式・略式の空域の調整 | 手入力 | PDF 249・V-32 |
| 3 | FAHs | 方角（sub-cardinal）か磁方位 | 手入力 | PDF 249・V-32 |
| 4 | Lateral and/or altitude separation | 横・高度の間隔 | 手入力 | PDF 249・V-32 |
| 5 | Approval out of the BP for RW | 回転翼機が BP を出る許可 | 手入力 | PDF 249・V-32 |
| 6 | PLA | PLA（Post Launch Abort）の点か方向・距離と、行う条件 | 手入力 | PDF 250・V-33 |
| 7 | TOT/TTT | 時刻。まだ決めていなければ「Standby TOT」か「TOT will be issued after correlation」。急がないときは「Push when ready」 | 時刻は手入力。ほかの 3 つは選ぶ | PDF 249・V-32 |

本文では PLA が 7 番目で TOT/TTT が 6 番目だが、同じ本文（6.a）が「TOT should be last」としているので、JTAK は TOT/TTT を最後にする（JTAK の決定。`docs/detailed-design/03-cas-other-products.md`）。
「Immediate」は本文に決まった言い方がないので、選ぶ欄に入れない（時刻の欄に書けば出せる）。

### この表に入れないもの

- Situation Update（V-17 から）と、複数の航空機・編隊での攻撃の分け方（V-21 の combined・sectored など）は、まだ扱わない。
- 目標が複数のとき（4・6・8 行目だけを続けて送る）・GEOREF などを使う 6 行目・動く目標の書き方は、まだ扱わない（手入力の欄に書けば出せる）。
- 自動の値の細かい決まり（四捨五入・北の書き方など）は `docs/detailed-design/02-cas-brief.md` 2.3。

## 共通の動作

- 地図の点を選ぶと、位置（MGRS・緯度経度）・標高・方位・距離を項目に入れる（計算は `core`）。
- 自動で入れた値も、使う人が直せる。
- 書式は `core` で文字の並びとして作り、F04 で画面・テキスト・PDF に出す。
- どの書式にも、先頭と末尾に「TRAINING USE ONLY / 訓練用」を入れる（U1）。

## ファイルから読み込む

訓練の前に用意したシナリオのテキストを読み込み、入力の欄を埋める。手入力はこれまでどおり使え、読み込んだ値もあとで直せる。
ファイルの形は `docs/interfaces/README.md` 3 章、作り方は `docs/detailed-design/06-scenario-import.md`。

- 対象: ルーティング・チェックイン・ゲームプラン・CAS Brief（9-line・Remarks・Restrictions）の 4 つの画面。
- ファイルの形: F04 で書き出すテキストと同じ。1 つのファイルに 1 つの書式。ひな形とデモは `docs/interfaces/scenario-samples/`。
- 各画面に「ファイルから読み込む」ボタンを置く。端末の中の好きな場所の `.txt` を選べる（選ぶ画面は `atak/export/jtak/` から始まる）。
- 題名の行が、その画面の書式と一致しないファイルは読み込まず、理由を画面に出す（例: チェックインの画面でルーティングのファイルを選んだとき）。
- 欄にすでに値があるときは、欄を消して読み込む前に確かめる。取りやめたら欄は変えない。
- 読み込むときは、まずその画面の欄をすべて空（既定）に戻し、ファイルに書いてある項目を入れる（2026-11-09 利用者の決定）。
  `（未入力）` の項目・読めない項目・ファイルにない項目は空のまま。前の場面の値は残らない。
- 飛ばした項目があったときは、9-line の「未入力の行」と同じように画面で知らせる。
- 読めないファイル（大きすぎる・文字が UTF-8 でない・中身が空）のときは、落ちずに理由を画面に出す。
- `TRAINING USE ONLY / 訓練用` の行は、ファイルにあってもなくてもよい。あれば読み飛ばし、欄には入れない。生成する書式には、これまでどおり先頭と末尾に入る（U1）。

## 受入基準

- 出典が `docs/references.md` に書かれ、項目の表がこの文書にある。
- 各書式の生成に JUnit の試験がある（項目が欠けたとき・自動の値と手入力の値）。
- 生成した文字に「TRAINING USE ONLY / 訓練用」が入っている。
