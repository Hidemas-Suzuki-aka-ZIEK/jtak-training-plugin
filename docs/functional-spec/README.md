# 機能仕様の一覧

- 関連: `docs/requirements.md`（要件）、`docs/usage-scope.md`（用途の範囲・最優先）

| ID | 機能 | 要件 | 状態 |
|---|---|---|---|
| F01 | [LRF の測距点の自動マーキング](F01-lrf-marking.md) | R1・R5 | 骨子 |
| F02 | [CAS 系の書式の生成](F02-cas-products.md) | R2・R5 | 骨子（項目は出典の確定後） |
| F03 | [RED（危険見積距離）の表示](F03-weapon-effects.md) | R3・R5 | 骨子 |
| F04 | [画面表示と書き出し](F04-export.md) | R4・R5 | 骨子 |

## 共通の決まり

- 「TRAINING USE ONLY / 訓練用」をすべての出力に入れる（`docs/usage-scope.md` U1）。
- 画面の言語: 端末の言語が日本語（ja）なら日本語、それ以外なら英語で出す（`docs/detailed-design/07-english-ui.md`）。
  - 言語で変わるのは、画面の文字、LRF の点の説明、既定の兵器の表の名前。点と表は作ったときの言語で作り、前からあるものは書き換えない。
  - CAS 系の書式と書き出しの中身、「（未入力）」、「TRAINING USE ONLY / 訓練用」の表示、訓練用サンプル値の文は、どちらの言語でも同じにする。
- 計算と文字の組み立ては `core`、ATAK の API と UI は `app` に置く（ADR 0003）。
- 各機能の詳しい設計は、着手するときに `docs/detailed-design/` に書く。
- 仕様で決まっていないことは、推測で実装しない。
