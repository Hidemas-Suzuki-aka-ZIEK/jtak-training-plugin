# JP 3-09.3（2014 年版）の日本語訳

> **TRAINING USE ONLY / 訓練用**
>
> 非公式の訳。原文は Joint Publication 3-09.3, *Close Air Support*, 25 November 2014（`docs/references.md` S4）。
> ミルシム・タクティカルトレーニングで、JTAC の役の用語と手順を学ぶためのもの。実際の作戦・実弾の射撃・実在の航空機の統制には使わない。
> 訳の中の数値（距離・時間など）は原文のまま訳したもので、実際の安全距離として使わない。

- 日付: 2026-11-09
- 決まり: `docs/usage-scope.md` Q10

## 1. ファイルの一覧

行は、原文の PDF から取り出した文字（作業用。コミットしない）の行番号。訳の作業の範囲を示すためのもの。

| ファイル | 原文の中身 | PDF のページ | 状態 |
|---|---|---|---|
| `00-front-matter.md` | 表紙・前書き・目次 | 1〜11 | 済 |
| `01-executive-summary-01.md` | Executive Summary（前半） | 12〜21 | 済 |
| `01-executive-summary-02.md` | Executive Summary（後半） | 22〜25 | 済 |
| `ch1.md` | 第 I 章 Overview and Fundamentals | 26〜37 | 済 |
| `ch2-01.md` | 第 II 章 Command and Control（1） | 38〜47 | 済 |
| `ch2-02.md` | 第 II 章（2） | 48〜57 | 済 |
| `ch2-03.md` | 第 II 章（3） | 58〜67 | 済 |
| `ch3-01.md`〜`ch3-11.md` | 第 III 章 Planning and Requesting（11 本） | 68〜199 | 済 |
| `ch4.md` | 第 IV 章 Preparation | 200〜217 | 済 |
| `ch5-01.md`〜`ch5-03.md` | 第 V 章 Execution（1）〜（3） | 218〜256 | 済 |
| `ch5-04.md`・`ch5-05.md` | 第 V 章（4）（5） | 257〜285 | 済 |
| `app-a.md` | 付録 A | 286〜291 | 済 |
| `app-b.md` | 付録 B | 292〜301 | 済 |
| `app-c.md` | 付録 C | 302〜303 | 済 |
| `app-d.md` | 付録 D | 304〜311 | 済 |
| `app-e-01.md`・`app-e-02.md` | 付録 E Examples of Close Air Support Missions | 312〜337 | 済 |
| `app-f.md`・`app-g.md`・`app-h.md` | 付録 F〜H | 338〜345 | 済 |
| `glossary.md` | Glossary（略語と用語の定義） | 346〜361 | 済 |

各ファイルの行の範囲（作業用）:

| ファイル | 行 | ファイル | 行 |
|---|---|---|---|
| 00-front-matter | 1〜349 | ch3-09 | 7783〜8359 |
| 01-executive-summary-01 | 350〜904 | ch3-10 | 8360〜8909 |
| 01-executive-summary-02 | 905〜1117 | ch3-11 | 8910〜9172 |
| ch1 | 1118〜1583 | ch4 | 9173〜9829 |
| ch2-01 | 1584〜2181 | ch5-01 | 9830〜10429 |
| ch2-02 | 2182〜2701 | ch5-02 | 10430〜10994 |
| ch2-03 | 2702〜3196 | ch5-03 | 10995〜11594 |
| ch3-01 | 3197〜3763 | ch5-04 | 11595〜12185 |
| ch3-02 | 3764〜4343 | ch5-05 | 12186〜12799 |
| ch3-03 | 4344〜4902 | app-a | 12800〜13018 |
| ch3-04 | 4903〜5499 | app-b | 13019〜13297 |
| ch3-05 | 5500〜6069 | app-c | 13298〜13353 |
| ch3-06 | 6070〜6631 | app-d | 13354〜13684 |
| ch3-07 | 6632〜7192 | app-e-01 | 13685〜14267 |
| ch3-08 | 7193〜7782 | app-e-02 | 14268〜14466 |
| app-f | 14467〜14499 | app-g | 14500〜14550 |
| app-h | 14551〜14601 | glossary | 14602〜15276 |

## 2. 訳の決まり

### 2.1 ファイルの形

1. 先頭に次の 4 行を置く（`<範囲>` はそのファイルの中身）。

   ```
   # JP 3-09.3（2014 年版）<範囲> の日本語訳

   > **TRAINING USE ONLY / 訓練用** — 非公式の訳。原文は JP 3-09.3, Close Air Support, 25 November 2014（docs/references.md S4）。
   > 訓練で用語と手順を学ぶためのもので、実際の作戦・射撃・航空機の統制には使わない。訳の中の数値を実際の安全距離として使わない。
   ```

2. 原文のページが変わるところごとに、決まった形の印を 1 行で入れる。PDF のページと、印刷のページ（各ページの先頭近くの `V-14 JP 3-09.3` や `IV-1` の部分）を書く。

   ```
   <!-- PDF p.231 / V-14 -->
   ```

   印刷のページが読み取れないページは `<!-- PDF p.231 / - -->` と書く。白紙のページ（Intentionally Blank）も印を入れ、「（このページは白紙）」と書く。
   **印の数は、範囲に含まれるページの数と同じにする**（抜けを数で確かめるため）。
3. 原文の章・節・項の番号（`1.`、`a.`、`(1)`、`(a)`）は、そのまま残す。見出しは「日本語（英語の原題）」の形にする（例: `## 2. CAS の実行（Execution）`）。
4. 図表（Figure・Table）は、文字が読める限り表か箇条書きに訳し、「図 V-9（PDF p.245）」のように番号とページを書く。取り出した文字が乱れていて読めないときは、勝手に組み立てずに「図 V-9 の文字は取り出しが乱れているため、原文の PDF p.245 を見ること」と書く。
5. 欄外の囲み（引用・事例の囲みなど）も訳し、`>` の引用ブロックにする。ページの上下の繰り返しの見出し（`Chapter V`、`JP 3-09.3` だけの行）は訳さない。

### 2.2 訳し方

1. **要約しない。段落を飛ばさない。** 原文のすべての文を訳す。
2. 平易な日本語の常体（だ・である）で書く。直訳で分かりにくいときは、意味を変えない範囲で語順を変えてよい。
3. **数値・単位は原文のまま**（NM・feet・meters・seconds などは単位も原文どおり、または「m」「ft」のように書いても数は変えない）。足したり、換算したりしない。
4. 取り出した文字の分断（`follo wing`、`recomm endations`）は黙って直す。
5. 略語は原文のまま残す。初めて出るところだけ「日本語（英略語）」にする（例: 近接航空支援（CAS））。2 回目からは略語だけでよい。
6. 無線の決まった言い方（brevity。`CLEARED HOT`・`CONTACT`・`ABORT`・`IN`・`SPOT`・`CAPTURED` など）と、書式の項目名（9-line の各行、check-in の項目など）は英語のまま残し、初めて出るところで括弧に短い説明を付ける。
7. 3 章の対訳表にある語は、その訳を使う。表にない語は、`glossary.md` の訳を使う。どちらにもない語で、訳が決まらないものは、原文の語を括弧で残す。
8. 原文の注（`See JP 3-09.2…` などの参照）も訳す。文書名は英語のまま残す。

### 2.3 安全に関わるところ

- RED（Risk-Estimate Distance）と Danger Close は、原文にある定義と参照だけを訳す。原文にない距離の表や数値を足さない。
- 原文の数値は、原文のまま訳す。注記や解説を足さない（「実際には〜」のような説明も足さない）。

## 3. 主な用語の対訳

JTAK の画面（`software/atak-plugin/app/src/main/res/values/strings.xml`）と設計書（`docs/detailed-design/02-cas-brief.md`・`03-cas-other-products.md`）と同じ語を使う。
書式の項目名は画面と同じく英語のまま残す。

| 原文 | 訳 |
|---|---|
| close air support (CAS) | 近接航空支援（CAS） |
| joint terminal attack controller (JTAC) | 統合末端攻撃統制官（JTAC） |
| forward air controller (airborne) (FAC(A)) | 空中の前線航空統制官（FAC(A)） |
| terminal attack control | 末端攻撃統制 |
| Type 1 / Type 2 / Type 3 control | Type 1 / Type 2 / Type 3 の統制 |
| bomb on target (BOT) / bomb on coordinate (BOC) | BOT（bomb on target）/ BOC（bomb on coordinate）。説明は画面と同じ（BOT: 機が目標または狙う点を自分で捉える。BOC: 機が CAS brief で渡された座標に対して攻撃する） |
| CAS brief（9-line） | CAS brief（9-line） |
| game plan | game plan（ゲームプラン） |
| check-in / check-out | check-in / check-out |
| routing | routing（ルーティング） |
| restrictions | restrictions（制限事項） |
| remarks | remarks（備考） |
| initial point (IP) | IP（initial point。進入の始点） |
| battle position (BP) | BP（battle position。回転翼機の攻撃の位置） |
| holding point | 待機点 |
| egress | 離脱（egress） |
| target / target location / target elevation | 目標 / 目標の位置 / 目標の標高 |
| target description | 目標の説明 |
| mark / type mark | 標示 / 標示の種類（type mark） |
| terminal guidance | 終末誘導（terminal guidance） |
| friendlies / friendly forces | 味方 / 味方の部隊 |
| laser target line (LTL) / pointer-target line (PTL) | LTL / PTL（初出で原語を添える） |
| time on target (TOT) / time to target (TTT) | TOT（time on target）/ TTT（time to target） |
| danger close | Danger Close（初出で「味方に近い攻撃」と説明する） |
| risk-estimate distance (RED) | 危険見積距離（RED） |
| collateral damage estimate (CDE) | 付随的損害の見積もり（CDE） |
| suppression of enemy air defenses (SEAD) | 敵防空の制圧（SEAD） |
| airspace coordination area (ACA) | 空域調整区域（ACA） |
| fire support coordination measure (FSCM) | 火力支援調整手段（FSCM） |
| final attack heading (FAH) | 最終攻撃方位（FAH） |
| aircraft on station (AOS) | 到着している機（AOS） |
| ground commander | 地上部隊の指揮官 |
| supported commander | 被支援指揮官 |
| commander's intent | 指揮官の意図 |
| rules of engagement (ROE) | 交戦規定（ROE） |
| clearance / cleared hot | 攻撃の許可 / CLEARED HOT |
| aircrew | 搭乗員 |
| fixed-wing (FW) / rotary-wing (RW) | 固定翼機（FW）/ 回転翼機（RW） |
| magnetic / true (heading, bearing) | 磁方位 / 真方位 |
| laser range finder (LRF) | レーザー測距器（LRF） |
| deconflict / deconfliction | 干渉しないように調整する（deconflict）/ 干渉の回避（deconfliction） |
| correlation | 照合（correlation） |
| air picture | 航空状況（air picture） |
| mission command | 任務指揮 |

略語の全体は `glossary.md` にある。
