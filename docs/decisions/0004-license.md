# 0004: ライセンス（MIT）

- 状態: 採用（2026-11-09 利用者の決定）
- 日付: 2026-11-09
- 関連: `LICENSE`、`docs/usage-scope.md` の Q14、ADR 0002（ATAK の SDK を再配布しない）

## 背景

リポジトリを公開し、市販の LRF への対応などを pull request で受けることにした（2026-11-09 利用者）。
それまでリポジトリの直下に LICENSE がなく、pull request を受けるときの条件も決まっていなかった（Q14 の (3)）。
利用者の答えは「都合が悪くなければ何でも」だった。

## 決定

- リポジトリの直下に `LICENSE` を置き、MIT ライセンスにする。著作権者の表示は「JTAK contributors」とする。
- pull request は MIT の条件で受ける（送った人は、その変更を MIT で出すことに同意したものとする）。
- 公開するものは、過去のコミットを含まないリリースブランチ（親のない 1 コミット）にする（Q14 の追記）。

## 理由

- 短く、条件が分かりやすい。外から pull request を受けやすい。
- ATAK-CIV は GPLv3 だが、JTAK のプラグインは ATAK の `main.jar` を compileOnly で使うだけで、ATAK のソースや SDK の jar を含まない（ADR 0002）。
  MIT は GPLv3 と両立するので、ATAK に読み込ませて使うことの妨げにならない。

## 変えないこと

- ATAK の SDK（jar・keystore・テンプレート）はリポジトリに入れない。LICENSE は SDK にはかからない。
- 「TRAINING USE ONLY / 訓練用」の表示と、`docs/usage-scope.md`（訓練用途のガードレール）は変えない。MIT の条件とは別に、このリポジトリの用途は訓練だけとする。
- `docs/manuals/jp3-09-3/` の JP 3-09.3 の非公式の訳は、原文の著作権の扱いが MIT と別である（米国の政府の著作物）。訳を公開することは Q14 で利用者が決めた。
