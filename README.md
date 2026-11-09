# JTAK

**TRAINING USE ONLY / 訓練用** — ミルシム・タクティカルトレーニング専用。実際の作戦・射撃・航空機の統制には使わない（`docs/usage-scope.md`）。

ATAK-CIV 5.8.0.9 のプラグイン。JTAC 役の訓練を助ける。

- LRF で測った点を地図に自動でマーキングする（ATAK 標準の LRF 連携・手入力。BLE 直結はまだ使えない）
- ルーティング・チェックイン・ゲームプラン・CAS Brief・Restrictions・9-line を自動で作る
- 兵器の加害範囲（編集できる表の訓練用サンプル値）を地図に描く
- 画面表示（読み上げ用）とテキスト・PDF の書き出し

## ビルド

前提: Linux、JDK 17、Android SDK（platform 36・build-tools 35.0.0）、
ATAK-CIV 5.8.0.9 の SDK の zip（利用者が入手したもの。リポジトリには入れない）。
`build.sh` を動かす前に、環境変数 `JAVA_HOME` に JDK 17 のディレクトリを入れておく（入っていなければ止まる）。

```bash
bash tools/sdk/setup-sdk.sh                                  # SDK を out/sdk/ に展開する
bash software/atak-plugin/build.sh assembleDebug :core:test  # APK を作り、core の試験を走らせる
```

SDK の jar・keystore はリポジトリに入れない（再配布になるため）。詳細は ADR 0002（`docs/decisions/0002-build-environment.md`）。

## ディレクトリ

| 場所 | 中身 |
|---|---|
| `docs/` | 要件・用途の範囲・機能仕様・設計・インタフェース・ADR |
| `docs/manuals/` | 取扱説明と、JP 3-09.3（2014 年版）の非公式の日本語訳（訓練用。`docs/manuals/README.md`） |
| `software/atak-plugin/` | プラグイン本体（`app`・`core`） |
| `tools/sdk/` | SDK の展開スクリプト |

## ライセンス

MIT（`LICENSE`。ADR 0004 `docs/decisions/0004-license.md`）。pull request も MIT の条件で受け付ける。
ATAK の SDK はこのライセンスの対象外で、リポジトリにも入れない。用途は訓練だけとする（`docs/usage-scope.md`）。
