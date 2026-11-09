# JTAK（訓練用）のビルド

TRAINING USE ONLY / 訓練用。ATAK-CIV 5.8.0.9 用のプラグインです。
`app` はプラグインのライフサイクル、ツールバーのボタン、ドロップダウンを持ちます。
ドロップダウンは、上に訓練表示を出し、その下に機能ごとのパネル（ルーティング・チェックイン・ゲームプラン・CAS Brief・兵器の加害範囲など）を並べます。
LRF の受け口（`lrf/`）と書き出し（`export/`）も `app` にあります。
`core` は Android・ATAK に依存しない Java 17 のライブラリで、書式の生成（`cas/`）・LRF の値の扱い（`lrf/`）・兵器の表（`weapons/`）・書き出し（`export/`）・取り込み（`importer/`）・訓練表示の文言を受け持ちます。

アイコンは `software/atak-plugin/app/src/main/res/drawable/ic_jtak.xml` の JTAK のアイコンです。
ツールバーとプラグインの一覧は同じ `@drawable/ic_jtak` を参照します。
図柄を差し替えるときは、この 1 ファイルを直します。

## 1. SDK を展開する

リポジトリのルートで実行します。`unzip` が必要です。

```bash
bash tools/sdk/setup-sdk.sh
```

SDK の zip の場所は `ATAK_SDK_ZIP=/path/to/ATAK-CIV-5.8.0.9-SDK.zip` で指定します（指定しないときは `$HOME/sdk/atak-civ-5.8.0.9/` の下を見ます）。
展開先は `out/sdk/ATAK-CIV-5.8.0.9-SDK/`、展開した javadoc は `out/sdk/javadoc/` です。
繰り返し実行できます。SDK の jar・keystore・テンプレート・javadoc は配布・コミットしません。

## 2. local.properties を用意する

```bash
cp software/atak-plugin/local.properties.example software/atak-plugin/local.properties
```

`sdk.dir` は Android SDK、`jtak.takdev.jar` と `jtak.sdk.jar` は展開した jar の絶対パスに直します。
Android platform 36 と build-tools 35.0.0、JDK 17 が必要です（`JAVA_HOME` で指定します）。
`local.properties` はコミットしません。
takdev はビルドのたびに `local.properties` を書き直して補助のキーを追加しますが、このファイルは Git に含まれず、追加されたキーは署名に使いません。

## 3. オフラインキャッシュを用意してビルドする

ADR 0002 に従い、Gradle のキャッシュを JTAK の `out/` に**コピー**して使います。
コピー元のキャッシュは変更しません。コピー中はビルドを実行しないでください。
以下の `<キャッシュ>` は、Gradle 8.14.3 と AGP 8.13.0 の依存が入った `.gradle-home` の場所に置き換えます。
その場所が無い場合やキャッシュが不足する場合は、ネットワークから取得せず、キャッシュを用意してから実行してください。

```bash
mkdir -p out/atak-plugin/.gradle-home
cp -a <キャッシュ>/. out/atak-plugin/.gradle-home/
bash software/atak-plugin/build.sh assembleDebug :core:test
```

キャッシュには Gradle 8.14.3 の `all` 配布物と AGP 8.13.0 の依存が必要です。
core の試験には、この Gradle 配布物の `lib/junit-4.13.2.jar` と `lib/hamcrest-core-1.3.jar` を使います。
ネットワークから取得しません。キャッシュが足りなければビルドは失敗します。

`build.sh` は `--offline --no-daemon` を指定し、渡された引数をそのまま Gradle に渡します。
`JAVA_HOME`（JDK 17）の指定は必須です。`JTAK_GRADLE` で Gradle 8.14.3 の実行ファイルの場所を変更できます。
`GRADLE_USER_HOME` と `ANDROID_USER_HOME`、デバッグ用署名鍵は `out/atak-plugin/` の下です。
既定の Gradle の場所は、上記キャッシュ内の `wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/bin/gradle` です。

APK は `software/atak-plugin/app/build/outputs/apk/debug/`、JUnit の結果は
`software/atak-plugin/core/build/reports/tests/test/index.html` にできます。
ATAK の `main.jar` は `compileOnly` なので APK に含めません。
SDK のサンプル秘密鍵は `out/sdk/` に展開するだけで、デバッグ APK の署名にはローカル生成の鍵を使います。

実機へのインストールやプラグインの読み込み確認はこの作業に含みません。
端末を操作する前に、利用者への確認が必要です。
