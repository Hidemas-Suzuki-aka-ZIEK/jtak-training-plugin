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
takdev はビルドのたびに `local.properties` を書き直して補助のキーを追加しますが、このファイルは Git に含まれません。
ローカルの debug と release は、追加されたキーを使わず、`out/` の下のデバッグ用の鍵で署名します。

## 3. オフラインキャッシュを用意してビルドする

ADR 0002 に従い、Gradle のキャッシュを JTAK の `out/` に**コピー**して使います。
コピー元のキャッシュは変更しません。コピー中はビルドを実行しないでください。
以下の `<キャッシュ>` は、Gradle 8.14.3 と AGP 8.13.0 の依存が入った `.gradle-home` の場所に置き換えます。
その場所が無い場合やキャッシュが不足する場合は、ネットワークから取得せず、キャッシュを用意してから実行してください。

```bash
mkdir -p out/atak-plugin/.gradle-home
cp -a <キャッシュ>/. out/atak-plugin/.gradle-home/
bash software/atak-plugin/build.sh assembleCivDebug assembleCivRelease :core:test
```

キャッシュには Gradle 8.14.3 の `all` 配布物と AGP 8.13.0 の依存が必要です。
core の試験には、この Gradle 配布物の `lib/junit-4.13.2.jar` と `lib/hamcrest-core-1.3.jar` を使います。
ネットワークから取得しません。キャッシュが足りなければビルドは失敗します。

`build.sh` は `--offline --no-daemon -Pandroid.builder.sdkDownload=false` を指定し（AGP が足りない Android SDK の部品を取りに行かないようにします）、渡された引数をそのまま Gradle に渡します。
`JAVA_HOME`（JDK 17）の指定は必須です。`JTAK_GRADLE` で Gradle 8.14.3 の実行ファイルの場所を変更できます。
`GRADLE_USER_HOME` と `ANDROID_USER_HOME`、ローカルの署名鍵は `out/atak-plugin/` の下です。
既定の Gradle の場所は、上記キャッシュ内の `wrapper/dists/gradle-8.14.3-all/10utluxaxniiv4wxiphsi49nj/gradle-8.14.3/bin/gradle` です。

flavor は `civ` だけです。`assembleDebug` でも `assembleCivDebug` を実行できます。
APK は `software/atak-plugin/app/build/outputs/apk/civ/debug/` と `software/atak-plugin/app/build/outputs/apk/civ/release/`、JUnit の結果は
`software/atak-plugin/core/build/reports/tests/test/index.html` にできます。
ATAK の `main.jar` は `compileOnly` なので APK に含めません。
SDK のサンプル秘密鍵は `out/sdk/` に展開するだけで、ローカルの APK の署名にはローカル生成の鍵を使います。

release は R8 で名前を難読化し、`atakplugin.JTAK` に再配置します。
SDK のひな形に合わせ、`-dontshrink` と `-dontoptimize` を残しているため、未使用クラスの削除と最適化はしません。
`plugin.xml` から読み込む `IPlugin` 実装のクラス名は保持します。リソースの削除も有効にしていません。
ローカルの SDK には ATAK の難読化対応表（mapping）がないため、`-applymapping` は TPP の経路だけに適用します。

実機へのインストールと ATAK での読み込みは、リポジトリのトップの `README.md` の「インストール」に書いています。

## 4. TPP の経路

tak.gov の Third Party Pipeline（TPP）は、提出したソースをビルドし、TAK の鍵で署名します。
この経路では `local.properties` を用意せず、パイプラインから `takrepo.url` を渡します。
ルートと app の `isDevKitEnabled()` は同じ判定を使います。プロパティは SDK のひな形と同じく、Gradle のプロジェクトプロパティ、次に `local.properties` の順で読みます。
`-P` で渡した値を優先し、環境変数を使う場合は Gradle 標準の `ORG_GRADLE_PROJECT_` の形式でプロジェクトプロパティとして渡します。

`takrepo.url` があるときは、その Maven リポジトリから `com.atakmap.gradle:atak-gradle-takdev:3.+` を解決します。
認証情報は `takrepo.user` と `takrepo.password` で渡します。指定がなければ、ひな形と同じ `invalid` を使います。
ATAK の API と mapping、ビルド用の keystore は takdev が解決します。署名設定はひな形と同じ `app/build/android_keystore` を参照し、提出するソースに鍵を含めません。
debug の `matchingFallbacks` は `sdk`、release は `odk` です。プラグイン API は `com.atakmap.app@5.8.0.CIV` のままです。

`takrepo.url` がなければ、2・3 章のローカル jar とオフラインキャッシュを使います。
ローカルでは `installRequiredNdk` を無効にしています。TPP では takdev の動作をそのまま使います。
JTAK は NDK を使わず、`ndkVersion` も指定していません。SDK 5.8.0.9 に付く takdev 3.6.0 は、NDK の指定がなければ自動取得を飛ばします。

TPP は Gradle 8.14.3 の wrapper を使い、`assembleCivRelease` を実行します。
`build.sh` はローカル専用です。TPP の Maven の経路でのビルドは、2026-11-11 の提出で通りました（リリース v0.1.0 の APK）。

## 5. 提出用の zip を作る

変更をコミットしたあと、リポジトリのルートで実行します。

```bash
mkdir -p out/tpp
git archive --format=zip --prefix=JTAK/ -o out/tpp/JTAK-tpp.zip HEAD:software/atak-plugin -- . \
  ':(exclude)core/src/test' ':(exclude)build.sh' ':(exclude)README.md' \
  ':(exclude)local.properties.example' ':(exclude).gitignore'
unzip -l out/tpp/JTAK-tpp.zip
```

zip の直下には `JTAK/` が 1 つあり、その中に `gradlew`・`gradlew.bat`・`gradle/wrapper/` とビルドのソースが入ります。
追跡しているファイルだけから作るため、コミットしていない変更は入りません。git の履歴（`.git`）も入りません。
入れるのはリリースのビルドに使うものだけです（2026-11-11 の決定）。
試験のコード（`core/src/test`）と、ローカル専用の `build.sh`・この README・`local.properties.example`・`.gitignore` は外します。
SDK の jar・keystore・`local.properties`・`out/`・`build/` を含まないことを一覧で確認してください。`gradle-wrapper.jar` は wrapper の実行に必要なので含めます。

提出すると TPP の規約に同意した扱いになります。出す前に、zip の中身と規約を確かめてください。
TRAINING USE ONLY / 訓練用の表示は、TPP で作る APK でも残します。
