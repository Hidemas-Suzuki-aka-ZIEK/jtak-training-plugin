# JP 3-09.3（2014 年版）付録 A（Appendix A） の日本語訳

> **TRAINING USE ONLY / 訓練用** — 非公式の訳。原文は JP 3-09.3, Close Air Support, 25 November 2014（docs/references.md S4）。
> 訓練で用語と手順を学ぶためのもので、実際の作戦・射撃・航空機の統制には使わない。訳の中の数値を実際の安全距離として使わない。

<!-- PDF p.286 / A-1 -->

## 付録 A　統合戦術航空攻撃要請（Joint Tactical Air Strike Request）

### 第 I 節　任務の要請（SECTION I―MISSION REQUEST）

**Line 1.**

- UNIT CALLED. 部隊の名称・呼出符号・あらかじめ割り当てた番号を示す。
- THIS IS. 要請の発信元を、部隊の名称・呼出符号・あらかじめ割り当てた番号で示す。
- REQUEST NUMBER. 事前計画の任務では、発信元の要請の通し番号を示す。即時の任務では、この番号は ASOC/DASC が割り当てる。
- SENT. 要請を送信した時刻と人を示す。

**Line 2.** （任務の区分 Mission Categories）

- PREPLANNED（事前計画）: 事前計画の要請では、優先順位（precedence。欄 A）または優先度（priority。欄 B）を記入する。
  - PRECEDENCE は、要請者が決めた重要度の高い順に、数字で書く。
  - PRIORITY は、次に示すとおりに書く（#1 は緊急、#2 は優先、#3 は通常）。
- IMMEDIATE（即時）:
  - PRIORITY. 即時の要請では、優先度（欄 C）を記入する。即時の要請は、定義上すべて precedence #1 なので、precedence の記入は要らない。事前計画（欄 B）または即時（欄 C）の優先度は、次の数字で決める（例: 戦術状況を定める）。

  **#1.** 緊急（Emergency）は #1。直ちに行動が要る目標で、ほかのすべての任務の優先度に優先する。

  **#2.** 優先（Priority）は #2。直ちに行動が要る目標で、通常の目標に優先する。

  **#3.** 通常（Routine）は #3。機会目標（targets of opportunity）。実行を急がない目標。

- RECEIVED. 要請を受け取った時刻と人を示す。

**Line 3.** TARGET IS/NUMBER OF — 攻撃する目標の種類・おおよその大きさ・機動性を述べる。おおまかな見積もりでも、目標の数（例: 戦車 10 両）または目標区域の大きさ（例: 正面 500 meter の人員）を示す必要がある。

<!-- PDF p.287 / A-2 -->

そうしないと、計画をつくる者は、必要な戦力（航空機の数と種類、弾薬の量と種類）を正確に決められない。注: 項目 M の「Centers (CP, COM)」は、C2 の拠点・指揮所などを指す。

**Line 4.** TARGET LOCATION IS. 対象の地域で定められた MGRS を使って、目標の位置を示す。

- BLOCK A. COORDINATES. 点の目標、または始点の位置を示す。
- BLOCK B. COORDINATES. A と合わせて使うと、A から B までの座標になる。
- BLOCK C. COORDINATES. A・B と合わせて使うと、経路になる。
- BLOCK D. COORDINATES. A から C と合わせて使うと、経路になるか、目標区域を表す。
- BLOCK E. TGT ELEV. 目標の標高。MSL からの feet で示す。
- BLOCK F. SHEET NO. 読んで分かるとおり。
- BLOCK G. SERIES. 読んで分かるとおり。
- BLOCK H. CHART NO. 読んで分かるとおり。
- CHECKED. 目標の情報を、誰と照らし合わせて確かめたかを示す。

**Line 5.** TARGET TIME/DATE. 航空攻撃を要請する時刻と日付を示す。

- BLOCK A. ASAP — できるだけ早く（as soon as possible）。
- BLOCK B. NLT. 目標は、示した時刻より前に、遅くともその時刻までに攻撃する。
- BLOCK C. AT. 目標を攻撃する時刻を示す。
- BLOCK D. TO. 空中待機や縦隊の上空掩護（column cover）などの支援が要る期間の終わりを示す。TO を使うときは、NLT と AT は要らない。

**Line 6.** DESIRED ORD/RESULTS. 要請者が望む航空攻撃の結果を示す。これは計画をつくる者に欠かせない情報で、要請者は慎重に考えなければならない。

- BLOCK A. ORDNANCE. 望む弾薬。
- BLOCK B. DESTROY. 読んで分かるとおり。

<!-- PDF p.288 / A-3 -->

- BLOCK C. NEUTRALIZE. 読んで分かるとおり。
- BLOCK D. HARASS/INTERDICT. 読んで分かるとおり。

**Line 7.** FINAL CONTROL. 状況の説明（briefing）をして弾薬の投下（release）を統制する、最終の統制官（例: JTAC、FAC[A]）を示す。

- BLOCK A. JTAC. 末端統制の種類を送る。
- BLOCK B. CALL SIGN. 末端統制官の呼出符号。
- BLOCK C. FREQ. 勧める TAD の周波数。
- BLOCK D. CONTROL POINT. 統制点の軍用グリッドの座標、または航法援助施設による位置（navigational aid fix）、あるいはその両方。統制点は、最終の統制官が統制を始める前に、攻撃機の飛行経路でいちばん遠い限りの点である。

**Line 8.** REMARKS. 要請のほかの欄に入れていない情報を入れられる。状況の更新（Situation Update）。JTAR を出すとき、JTAC または要請する機関は、ASOC/DASC に現在の状況の更新を伝える。状況の更新は次からなる。

(1) 状況の更新の番号（#）

(2) 目標、敵の全般の状況

(3) 脅威の活動

(4) 味方の状況

(5) 味方の位置

(6) 火砲の活動

(7) 許可を出す権限（clearance authority）

(8) 要請する弾薬

(9) Restrictions/Remarks（制限事項・備考）

(10) 局地的な SEAD の努力（制圧・EW）

(11) 危険（天候・地形・障害物）

### 第 II 節　調整（SECTION II – COORDINATION）

**Line 9.** NSFS. NSFS の調整。

<!-- PDF p.289 / A-4 -->

**Line 10.** ARTY. 火砲の調整。

**Line 11.** AIO/G-2/G-3. 航空情報幕僚（Air Intelligence Officer）、G-2、G-3、またはほかの軍種の同等の者との調整。

**Line 12.** REQUEST. 要請の承認または不承認を示す。

**Line 13.** BY. 要請を承認または不承認にした人を示す。

**Line 14.** REASON FOR DISAPPROVAL. 読んで分かるとおり。

**Line 15.** RESTRICTIVE FIRE/AIR PLAN. ACA は、味方の地上から撃つ非核の火力に対して、おおむね安全な空域を定める。ACA は、特定の区域での地上から撃つ火力の条件を、搭乗員に警告する。必要に応じて、計画番号またはコードネームを出す。

**Line 16.** IS IN EFFECT. 該当する ACA の計画が有効な期間を定める。

**Line 17.** LOCATION. ACA の中心線の始点と終点のグリッド座標。

**Line 18.** WIDTH (METERS). 中心線の両側からの ACA の幅を定める。

**Line 19.** ALTITUDE/VERTEX. ACA の高度。MSL からの feet で示す。

### 第 III 節　任務のデータ（SECTION III – MISSION DATA）

注: 要請した機関に送る任務のデータは、要請に入っていない項目だけに限ってよい。

**Line 20.** MISSION NUMBER. 読んで分かるとおり。

**Line 21.** CALL SIGN. 読んで分かるとおり。

**Line 22.** NO. AND TYPE AIRCRAFT. 読んで分かるとおり。

**Line 23.** ORDNANCE. 弾薬の種類。コード番号か実際の名称のどちらでもよい。

**Line 24.** EST/ALT TAKEOFF. 任務の機が離陸する推定の時刻、または実際の時刻。

**Line 25.** EST TOT. 推定の TOT（time on target）。

**Line 26.** CONTROL POINT (COORDS). 最終の統制官が統制を始める前に、攻撃機の飛行経路でいちばん遠い限りの点。要請で指定してあるときは、Line 7 の項目 D と同じ。

**Line 27.** INITIAL CONTACT. 編隊が最初に連絡する統制機関を示す。

<!-- PDF p.290 / A-5 -->

**Line 28.** JTAC/FAC(A)/TAC(A) CALL SIGN/FREQ. 最終の統制機関の呼出符号と周波数。

**Line 29.** AIRSPACE COORDINATION AREA. このデータは Line 15 から 19 を見ること。

**Line 30.** TGT DESCRIPTION. 読んで分かるとおり。

**Line 31.** TGT COORD/ELEV. 読んで分かるとおり。

**Line 32.** BDA REPORT (USMTF INFLTREP). この任意の欄は、任務ごとの BDA を記録するのに使う。

<!-- PDF p.291 / A-6 -->

図 A-1　国防総省（Department of Defense）様式 1972、統合戦術航空攻撃要請（Joint Tactical Air Strike Request）（PDF p.291）。原文では様式の画像だけが載っていて、取り出した文字は図の題名だけである。様式の中身は原文の PDF p.291 を見ること。

（付録 A の終わり。次は付録 B。）
