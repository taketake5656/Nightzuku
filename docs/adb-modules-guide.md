# ADB モジュール ガイド

**日本語** | [English](adb-modules-guide.en.md)

このガイドはモジュールの作者とテスター向けです。API の正確な仕様については
[ADB モジュール API リファレンス](adb-modules-api.md) を参照してください。

## このシステムについて

ADB モジュールは、Nightzuku のプライベートストレージにインストールされ、
動作中の Nightzuku サーバーを通じて実行される ZIP パッケージです。

- ADB で起動した Nightzuku: スクリプトは ADB シェルの権限で実行されます。
- root で起動した Nightzuku: スクリプトは root 権限で実行されます。
- セーフモード: 手動のアクションのみ実行できます。
- フルアクセス: より強力なモジュールの動作を許可します。
- バックグラウンドアクション: `service.sh` を実行するには有効にする必要があります。

これは見た目だけのスタブではなく、実際に動作するモジュールランナーです。ZIP のインストール、
メタデータの解析、モジュールファイルの保存、Nightzuku を通じたシェルスクリプトの実行、
ローカル WebUI の表示、有効状態の管理、モジュールの削除、最終実行ログの書き込みを行います。

Magisk/KSU のようなシステムレスオーバーレイの実装ではありません。マウントフックはなく、
`/data/adb/modules` との互換性も保証しません。また、常駐デーモンの監視機能も現時点ではありません。

## 最小構成のモジュール

次の構成を作成します:

```text
my-module/
├── module.prop
├── action.sh
└── webui/
    └── index.html
```

`module.prop`:

```properties
id=my-module
name=My Module
version=1.0
versionCode=1
author=Author
description=Short module description
```

`action.sh`:

```sh
#!/system/bin/sh
echo "module=$SHIZUKU_MODULE_ID"
echo "mode=$SHIZUKU_MODULE_MODE"
id
```

パッケージ化します:

```sh
cd my-module
zip -r ../my-module.zip .
```

「ADB モジュール」画面から `my-module.zip` をインストールします。

## 任意のファイル

バナー:

```text
banner.png
```

WebUI:

```text
webui/index.html
```

バックグラウンド／サービスフック:

```text
service.sh
```

独自のパスは `module.prop` で指定できます:

```properties
banner=assets/banner.webp
webui=webui
usesShellBridge=true
action=scripts/action.sh
```

`window.Shizuku` を必要とする WebUI ページでは、`usesShellBridge=true` の指定が必須です。

## スクリプトの実行環境

スクリプトはモジュールのディレクトリで実行されます。次の変数を使用できます:

```sh
MODDIR=/data/user/0/kerneldroid.nightzuku/files/adb_modules/<id>
ASH_STANDALONE=1
SHIZUKU_MODULE_ID=<id>
SHIZUKU_MODULE_MODE=safe|custom|full
SHIZUKU_MODULE_TRUSTED=0|1
SHIZUKU_MODULE_BACKGROUND=0|1
```

Magisk/KSU のパスをハードコードしないでください。`$MODDIR` を使用してください。

## アクションとサービス

ユーザーが操作して実行するコマンドには `action.sh` を使用します。

バックグラウンドでのセットアップには `service.sh` を使用します。次の条件をすべて満たすと実行されます:
- モジュールが有効になっている。
- アクセスモードがフルアクセス、またはサービスを有効にしたカスタムである。
- 設定でバックグラウンドアクションが有効になっている。
- Nightzuku の binder が利用可能である。

マネージャーは、binder セッションごとに 1 回、有効なサービスを自動実行します。

## 完全信頼

完全信頼（Full Trust）はモジュールごとの特別扱いの設定です。モジュールのカードを長押しして切り替えます。信頼されたモジュールは、基本的な安全上の制限（タイムアウト、出力の上限）は守りつつ、全体のポリシーによる制限を受けずに動作します。

## 安全上の制限

- ZIP 内のパストラバーサルは拒否されます。
- 最大エントリ数: `2048`。
- 展開後の最大サイズ: `200 MB`。
- スクリプトのタイムアウト: `120 秒`。
- 保持される出力: stdout／stderr それぞれ最後の `64 KB`。

タイムアウト時の終了コードは `124` です。

## ログ

ログはインストールされたモジュールのディレクトリ内に書き込まれます:

```text
logs/action-last.log
logs/service-last.log
```

各ログには、モジュール ID、スクリプト名、終了コード、アクセスモード、stdout、stderr が含まれます。

## テスト用モジュール

リポジトリには次のファイルが含まれています:

```text
test-modules/adb-test-module.zip
```

インストール、有効化／無効化、アクション、サービス、WebUI、バナー表示の動作確認に使用してください。
