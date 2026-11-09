# 詳細設計

- 関連: `docs/basic-design/README.md`、`docs/functional-spec/`

各機能に着手するときに、ここに詳細設計を書き、実装の根拠にする。

## 進め方

1. 機能仕様（F01〜F04）と基本設計をもとに、`NN-<名前>.md`（例: `01-lrf-input.md`）を書く。
2. 書く中身: クラスとその役目、データの形、計算の式と精度、誤りのときの動作、試験の一覧。
3. ATAK の API を使うところは、javadoc で確かめてから使う。
4. 決まっていないことは「未決」として書き、利用者に確認する（訓練用途に関わるものは `docs/usage-scope.md` の要確認事項にも書く）。

## 一覧

| ファイル | 機能 | 状態 |
|---|---|---|
| `01-lrf-marking.md` | F01 LRF の測距点の自動マーキング（ATAK 標準の連携・手入力。BLE 直結は除く） | 完了（GPS がないときの実機の確認を含む） |
| `02-cas-brief.md` | F02 9-line と Remarks（CAS Brief）の生成 | 完了 |
| `03-cas-other-products.md` | F02 ルーティング・チェックイン・ゲームプラン | 完了 |
| `04-weapon-effects.md` | F03 兵器の範囲の表示（編集できる表） | 実装中 |
| `05-export.md` | F04 テキスト・PDF の書き出し | 完了（共有の画面の下見の直しは 8 章） |
| `06-scenario-import.md` | F02 ファイルから読み込む（CAS 系の書式） | 設計中 |
| `07-english-ui.md` | 全機能 英語の画面 | 実装中 |
