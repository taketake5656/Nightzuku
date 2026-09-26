# Nightzuku ADB モジュール API

**日本語** | [English](adb-modules-api.en.md)

ADB モジュールは、Nightzuku アプリのプライベートストレージにインストールされ、現在動作中の Nightzuku サーバーを通じて実行される ZIP パッケージです。Nightzuku が ADB で起動されている場合、モジュールのスクリプトは ADB シェルの権限で実行されます。Nightzuku が root で起動されている場合は、root 権限で実行されます。

これは root のオーバーレイシステムではありません。アクション、WebUI、サービスフック、そして制御された ADB/root シェルアクセスのための、Nightzuku を基盤としたモジュールランナーです。

## パッケージ形式

モジュールは、ZIP のルートに `module.prop` を含む `.zip` ファイルです。

```text
module.zip
├── module.prop
├── banner.png
├── action.sh
├── service.sh
└── webui/
    └── index.html
```

すべてのパスは相対パスである必要があります。絶対パスや `..` によるトラバーサルは、インストール時に拒否されます。

## module.prop

必須フィールド:

```properties
id=my-module
name=My Module
version=1.0
versionCode=1
author=Author
description=Short description
```

任意フィールド:

```properties
banner=banner.png
webui=webui
action=action.sh
```

ルール:

- `id` は `[A-Za-z][A-Za-z0-9._-]{1,63}` に一致する必要があります。
- `banner` には `.png`、`.jpg`、`.jpeg`、`.webp` を指定できます。
- `banner` を省略した場合、Nightzuku は `banner.png`、`banner.jpg`、`banner.jpeg`、`banner.webp` の順に確認します。
- `webui` を省略した場合、Nightzuku は `webroot`、`webui`、`web` の順に確認します。
- WebUI は `<webui>/index.html` が存在する場合にのみ利用できます。
- `action` の既定値は `action.sh` です。
- `service.sh` は自動的に検出されます。

## インストール時の動作

インストールの流れ:

1. ユーザーが Android のファイル選択画面でモジュールの ZIP を選択します。
2. Nightzuku が ZIP をキャッシュにコピーします。
3. Nightzuku が `module.prop` を検証します。
4. Nightzuku がステージング用ディレクトリに展開します。
5. Nightzuku が安全でないパスを拒否します。
6. Nightzuku が `.sh` ファイルに実行権限を付けます。
7. Nightzuku が同じ `id` の既存モジュールを置き換えます。
8. Nightzuku がモジュールをアプリのプライベートストレージに保存します。

安全上の制限:

- ZIP の最大エントリ数: `2048`。
- 展開後の最大サイズ: `200 MB`。
- メモリ／ログに保持されるスクリプト出力: ストリームごとに最後の `64 KB`。
- スクリプトのタイムアウト: `120 秒`。

## 実行環境

スクリプトは Nightzuku サーバーのプロセス生成機能を通じて実行されます。実行されるコマンドは次のとおりです:

```sh
sh /path/to/module/action.sh
```

または:

```sh
sh /path/to/module/service.sh
```

作業ディレクトリはモジュールのディレクトリです。

環境変数:

```sh
MODDIR=/data/user/0/kerneldroid.nightzuku/files/adb_modules/<id>
ASH_STANDALONE=1
SHIZUKU_MODULE_ID=<id>
SHIZUKU_MODULE_MODE=safe|full
SHIZUKU_MODULE_BACKGROUND=0|1
```

モジュール内のファイルには、すべて `MODDIR` を使用してください。`/data/adb/modules` のような root 向けのパスを前提にしないでください。

## アクション

`action.sh` は、モジュールのカードからユーザーが手動で実行するアクションです。

アクションの結果:
- stdout／stderr はダイアログに表示されます。
- 最後の出力はモジュールディレクトリ内の `logs/action-last.log` に書き込まれます。
- タイムアウト（120 秒）時は終了コード `124` を返します。

最小構成の `action.sh`:

```sh
#!/system/bin/sh
echo "module=$SHIZUKU_MODULE_ID"
id
```

## サービス

`service.sh` はバックグラウンド用のフックです。

実行ポリシー:
- **セーフモード**: ブロックされます。
- **フルアクセスモード**: 「バックグラウンドアクションを許可」が有効な場合に許可されます。
- サービススクリプトは Nightzuku の binder セッションごとに 1 回実行されます。
- 最後の出力は `logs/service-last.log` に書き込まれます。
- タイムアウト（120 秒）時は終了コード `124` を返します。

## WebUI

WebUI は `webui/index.html` から読み込まれます。

現在の WebView のポリシー:
- JavaScript、DOM ストレージ、ローカルファイルへのアクセスが有効です。
- ネットワークアクセスは、カスタム／フルアクセスモードで有効にしない限りブロックされます。
- ポリシーで許可されている場合、有効なモジュールのローカル WebUI に `window.Shizuku` が公開されます。

### JavaScript からシェルへのブリッジ

`window.Shizuku` オブジェクトを使うと、WebUI からシェルを操作できます。

#### モジュール情報

```javascript
const info = JSON.parse(window.Shizuku.getModuleInfo());
console.log(info.id);         // 例: "my-module"
console.log(info.enabled);    // true
```

#### シェルの実行

```javascript
const result = JSON.parse(window.Shizuku.exec("id"));
if (result.ok) {
    console.log(result.stdout);
}
```

詳細なオプション付きの実行:

```javascript
const result = JSON.parse(window.Shizuku.execWithOptions("pwd", JSON.stringify({
  timeoutSeconds: 30,
  cwd: "webui"
})));
```

ルール:
- `stdin` は 64 KB までです。
- stdout／stderr はストリームごとに最後の 64 KB を返します。
- `cwd` はモジュールのディレクトリ内である必要があります。

### 完全信頼

完全信頼（Full Trust）はモジュールごとの特別扱いの設定です。モジュールのカードを長押しして切り替えます。

信頼されたモジュールは:
- アクション／サービス／バックグラウンド／WebUI に関する全体の制限を受けません。
- ReCommand の確認ダイアログが表示されません。
- `download()` と WebView のインターネットアクセスを同時に使用できます。

### WebUI アセットローダー

`window.Shizuku.download(url, relativeWebPath)` は、HTTPS のアセットをモジュールの WebUI ルートにダウンロードします。
- 最大ファイルサイズ: 20 MB。
- `index.html` は上書きできません。

## 有効化、無効化、削除

無効にすると、モジュールのディレクトリに `disable` ファイルが作成され、アクションとサービスがブロックされます。削除すると、モジュールのディレクトリ全体が削除されます。

## テスト用モジュール

リポジトリにはテスト用モジュールが含まれています:

```text
test-modules/adb-test-module.zip
```

含まれるファイル:

- `module.prop`
- `banner.png`
- `action.sh`
- `service.sh`
- `webui/index.html`

アクションを実行すると、現在の UID、SDK バージョン、モジュール ID、モジュールのモードが出力されます。

## 現在の対応範囲

実装済み:

- ZIP のインストール。
- モジュールのメタデータ解析。
- パストラバーサルの防止。
- サイズとエントリ数の制限。
- 有効化／無効化／削除。
- バナーの表示。
- WebUI の表示。
- WebUI での HTTPS アセットの読み込み。
- WebUI からモジュールの WebUI ルートへの HTTPS ファイルダウンロード。
- 手動での `action.sh` 実行。
- ポリシーで制御される `service.sh`。
- Nightzuku の binder セッションごとに 1 回のサービス実行。
- 最後のアクション／サービスのログ。
- タイムアウト／stdin／cwd／env を指定できる、JavaScript からシェルへの直接ブリッジ。

未実装:

- システムレスなファイルシステムオーバーレイ。
- Magisk/KSU のマウントの仕組み。
- 常駐サービスの監視。

これらは別の機能であり、現在の ADB モジュール API で利用できるかのように扱うべきではありません。
