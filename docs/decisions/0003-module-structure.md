# 0003: モジュール構成（ATAK に依存しない core と ATAK のプラグインの app）

- 状態: 採用（2026-11-08。パッケージ名・表示名は同日に利用者が確定）
- 日付: 2026-11-08
- 関連: `docs/basic-design/README.md`、ADR 0002

## 背景

JTAK の中身の多くは、ATAK の API がなくても書ける計算と文字の組み立て（座標と角度、書式の生成、兵器の表）である。
これらを ATAK の API と分けておけば、実機や ATAK なしで JUnit で試験できる。

## 決定

`software/atak-plugin/` を 2 つの Gradle のモジュールに分ける。

| モジュール | 種類 | パッケージ（仮） | 中身 | 依存してよいもの |
|---|---|---|---|---|
| `core` | `java-library`（Java 17 のソースで、Android でも動く範囲の API だけを使う） | `jp.jtak.core` | 座標と角度の計算（自分の位置と測距から目標の点、磁方位と真方位）、書式の生成（ルーティング・チェックイン・ゲームプラン・CAS Brief・Restrictions・9-line）、兵器の表の読み込みと検証、テキストの書き出し、TRAINING USE ONLY の表示の文言 | JDK の標準ライブラリだけ（必要になったら ADR で足す） |
| `app` | Android のアプリ（ATAK のプラグイン） | `jp.jtak.plugin` | ATAK のライフサイクルとツールバー・ドロップダウンの UI、地図へのマーカーと範囲の描画、LRF の受け口（`RangeFinderAction2`・BLE・手入力）、PDF の書き出し | `core`、ATAK の `main.jar`（compileOnly）、Android の SDK |

- `app` は `core` に依存する。`core` は `app` にも ATAK にも依存しない。
- 試験: `core` は JUnit で試験する（`bash software/atak-plugin/build.sh :core:test`）。`app` の動作は実機で確かめる。
- 「TRAINING USE ONLY / 訓練用」の文言は `core` の 1 か所で持ち、テキスト・PDF・画面・マーカーの説明がそこから使う
  （外す設定を作らない。`docs/usage-scope.md`）。

## 変えないこと

- `core` に ATAK・Android の型を入れない（入れると JUnit で試験できなくなる）。

## 名前（2026-11-08 利用者の決定）

- パッケージ名は `jp.jtak.*`（`jp.jtak.plugin`・`jp.jtak.core`）で確定。表示名は「JTAK」（`docs/usage-scope.md` Q1）。
- ATAK のプラグイン一覧の説明には訓練用の表示を残す（「JTAK — TRAINING USE ONLY / 訓練用」）。画面の TRAINING USE ONLY / 訓練用 はドロップダウンなどの各画面に出す（U1）。
