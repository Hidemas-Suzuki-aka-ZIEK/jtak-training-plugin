# 基本設計

- 関連: ADR 0002（ビルド環境）、ADR 0003（モジュール構成）、`docs/functional-spec/`、`docs/interfaces/README.md`

## 1. 全体の形

```
 LRF（ATAK 標準の連携 / BLE / 手入力）
        │  測距の値（docs/interfaces 1.1）
        ▼
 ┌──────────── app（jp.jtak.plugin。ATAK のプラグイン）────────────┐
 │ LRF の受け口 ─┐                                                  │
 │ 偏差（GeomagneticField）                                          │
 │               ▼                                                  │
 │        ┌──── core（jp.jtak.core。純 Java）────┐                  │
 │        │ 目標の計算・方位の補正                │                  │
 │        │ 書式の生成（F02）                     │                  │
 │        │ 兵器の表の読み込みと検証・円の判定    │                  │
 │        │ テキストの書き出し・TRAINING の文言   │                  │
 │        └──────────────────────────────────────┘                  │
 │               ▼                                                  │
 │ 地図の点・円の描画 / ドロップダウンの画面 / PDF の書き出し        │
 └──────────────────────────────────────────────────────────────────┘
        ▼
 ATAK の地図・端末の中のファイル（ネットワークへの自動の送信はしない）
```

## 2. app の部品（予定）

| 部品 | 役目 | ATAK・Android の API |
|---|---|---|
| ライフサイクル | プラグインの読み込みと片付け、ツールバーのボタン | PluginTemplateLegacy の形 |
| ドロップダウン | 画面（入力・書式の表示・書き出しのボタン）。上に小さく TRAINING USE ONLY | ATAK の DropDownReceiver |
| LRF の受け口 | `RangeFinderAction2` の実装・BLE・手入力を 1.1 の形にそろえる | `com.atakmap.android.lrf`（F01） |
| 地図の描画 | 目標の点・兵器の円 | ATAK の地図の項目の API（javadoc で確かめる） |
| PDF | 書式の PDF | `android.graphics.pdf.PdfDocument`（F04） |

## 3. core の部品（予定）

| 部品 | 役目 | 試験 |
|---|---|---|
| 測距と目標の計算 | 自分の位置と測距の値から目標の点、磁方位から真方位 | JUnit（F01） |
| 座標の表し方 | 緯度経度・MGRS の変換と表示 | JUnit |
| 書式 | 各書式の項目と文字の組み立て | JUnit（F02） |
| 兵器の表 | `weapons.json` の読み込み・検証・円の中かの判定 | JUnit（F03） |
| 出力の文言 | 「TRAINING USE ONLY / 訓練用」などの文言を 1 か所で持つ | JUnit（F04） |

## 4. 方針

- ATAK の API は、SDK の javadoc で確かめてから使う。
- ATAK の標準の動作を置き換えるもの（LRF の動作）は、止めたときに元に戻す。
