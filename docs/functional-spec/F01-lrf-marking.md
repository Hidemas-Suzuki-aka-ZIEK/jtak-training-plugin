# F01: LRF の測距点の自動マーキング

- 要件: R1・R5（`docs/requirements.md`）
- 関連: SDK の javadoc（`docs/references.md` の S1）、`docs/interfaces/README.md`「1. LRF の入力」

## 目的

LRF（レーザー測距儀）で測った距離・方位・俯仰と自分の位置から目標の点を求め、ATAK の地図に自動で置く。
置いた点は F02 の書式（目標の位置・標高）と F03 の範囲の中心に使う。

## 入力の方式

| 方式 | 受け口 | 値 |
|---|---|---|
| A. ATAK 標準の LRF 連携 | `LocalRangeFinderInput.getInstance().registerAction(RangeFinderAction2)`。ATAK が受けた UDP 17211・ATAK 自身の Bluetooth の LRF のデータがここに来る | 距離 m、磁方位 °（NaN のことがある）、俯仰 °（上が正） |
| B. BLE 直結 | JTAK が BLE で LRF につなぎ、機種ごとのパーサで読む（機種は未定 `docs/usage-scope.md` Q2） | 機種による。パーサが A と同じ形にそろえる |
| C. 手入力 | ドロップダウンの入力欄（距離・方位・俯仰、方位は磁方位か真方位かを選ぶ） | A と同じ形 |

3 つの方式は、`core` の同じ形の値（測距の値: 距離 m・方位 °と基準（磁/真）・俯仰 °・時刻・入力の方式）にそろえてから処理する。

## 処理

1. 自分の位置（ATAK の自分の点の緯度・経度・標高）を取る。取れないときは、点を置かずに画面に理由を出す。
2. 方位が磁方位なら、`app` が `android.hardware.GeomagneticField` で偏差を求め、`core` が真方位に直す。
3. `core` が、自分の位置・距離・真方位・俯仰から目標の緯度・経度・標高を求める（計算の式と精度は詳細設計で決め、試験を付ける）。
4. `app` が地図に点を置く。点の説明に「TRAINING USE ONLY / 訓練用」、入力の方式、測距の値、時刻を入れる。
5. 方位が NaN のときは点を置かず、画面に出す（距離だけの値は捨てない。記録に残すかは詳細設計で決める）。

## 利用者向けの注意

- ATAK 標準の連携（方式 A）で来る方位は、磁方位とみなす。LRF を真方位で出す設定にしていると、偏差の分だけ点がずれる。LRF は磁方位で出す設定にして使う（2026-11-08 のレビュー）。
- 測った距離が 20 km を超えるときは点を置かず、画面に理由を出す（2026-11-08 利用者の決定）。
- 受け取った機器の名前は、点の説明に出さない（`docs/usage-scope.md` Q7）。
- 置いた点は、利用者が ATAK の機能で手で送れる。JTAK が自分から送ることはない（`docs/usage-scope.md` Q6・U8）。

## ATAK 標準の動作との関係

- `registerAction` で置き換えている間は、ATAK の標準の LRF のマーカーの代わりに JTAK が点を置く。
- プラグインを止める・外すときは、ATAK の標準の動作に戻す（戻し方は `registerAction((RangeFinderAction2) null)`。SDK（`docs/references.md` の S1）の main.jar で確かめた）。
- 使う人が JTAK の自動マーキングを切れる設定を持つ（切ると ATAK の標準の動作に戻る）。

## 受入基準

- 手入力と、ATAK 標準の連携（UDP 17211 に `1,uid,timestamp,d,a,e` を送る試験）で、地図に点が置かれる。
- `core` の目標の計算に JUnit の試験がある（距離 0・方位 0/90/180/270・俯仰の正負・NaN）。
- 点の説明に「TRAINING USE ONLY / 訓練用」が入っている。
