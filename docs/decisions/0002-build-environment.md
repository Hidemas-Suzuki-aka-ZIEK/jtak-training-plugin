# 0002: ビルド環境（ATAK-CIV 5.8.0.9・AGP 8.13・JDK 17・オフラインのビルド）

- 状態: 採用（2026-11-08。利用者が「docs＋ビルド骨格」の計画を承認）
- 日付: 2026-11-08
- 関連: `README.md`、`software/atak-plugin/`、`tools/sdk/setup-sdk.sh`。
  元にしたもの: 同じ SDK で ATAK のプラグインをビルドしている、既存のビルド設定

## 背景

利用者の実機（Android）には、ATAK-CIV 5.8.0.9 が入っている。手元に同じ版の SDK
（`ATAK-CIV-5.8.0.9-SDK.zip`）がある。
同じ SDK を使った既存のビルド設定で、ATAK のプラグインをネットワークを使わずにビルドできている。

SDK の jar・keystore・テンプレートは TAK の配布物で、リポジトリに入れると再配布になる。

## 決定

| 項目 | 決めたこと |
|---|---|
| 対象 | ATAK-CIV 5.8.0.9。プラグイン API の表記は `com.atakmap.app@5.8.0.CIV` |
| Gradle・AGP | Gradle 8.14.3・Android Gradle Plugin 8.13.0 |
| JDK | 17。`JAVA_HOME` で指定する |
| Android | compileSdk 36・targetSdk 36・minSdk 31・build-tools 35.0.0（手元にあるのはこれだけ） |
| オフライン | `android.builder.sdkDownload=false`。Gradle の配布物と依存は `out/` の下のキャッシュを使う（Gradle 8.14.3 と JUnit が入った `.gradle-home` を用意する。用意のしかたは `software/atak-plugin/README.md` に書く） |
| takdev | SDK の `atak-gradle-takdev.jar` を `local.properties` で場所を指定して `buildscript` の classpath に入れる。`isDevKitEnabled = { false }` |
| ATAK の API | SDK の `main.jar` を **`compileOnly`** で使う（APK に入れない） |
| SDK の展開 | `tools/sdk/setup-sdk.sh` が SDK の zip から main.jar・takdev・javadoc・keystore・`samples/PluginTemplateLegacy` を `out/sdk/` に展開する。zip の場所は引数か環境変数で変えられるようにし、既定は上の場所 |
| 入口 | `bash software/atak-plugin/build.sh <Gradle のタスク>`（`GRADLE_USER_HOME`・`ANDROID_USER_HOME` を `out/` の下にする） |
| ひな形 | `samples/PluginTemplateLegacy` をもとに `app` を作る（ADR 0003） |
| 署名 | デバッグの署名は `out/` の下の keystore を使う。keystore はコミットしない（`.gitignore`） |

## 変えないこと

- SDK の jar・keystore・テンプレートをリポジトリに入れない。`local.properties` もコミットしない（`local.properties.example` だけを置く）。
- ビルドの途中でネットワークから SDK・依存を取りに行かない。足りないものがあれば、先に用意してからビルドする。

## まだ確かめていないこと

- リリース版（開発者向けでない）の ATAK が、この署名のプラグインを読み込むか。
  2026-11-08、実機の ATAK-CIV 5.8.0.9 で試した（利用者の確認のうえで `adb install`）。この ATAK は DEVELOPER BUILD で、
  ログに「SDK skipping signature check」と出て、debug の鍵の APK を読み込んだ。署名を確かめる ATAK では未確認（`docs/usage-scope.md` Q5）。
