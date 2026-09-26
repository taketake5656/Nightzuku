# Nightzuku コネクタ API

**日本語** | [English](nightzuku-connectors.en.md)

Nightzuku コネクタは、「アクティベーター」と呼ばれるサードパーティ製アプリが、Nightzuku サーバーを端末上で起動するためのコマンドを安全に取得できるようにする実験的な機能です（**試験運用機能**で利用できます）。

## Nightzuku コネクタの目的

愛好家によって、PC や完全な root 権限なしで一時的に昇格した権限を得られる、ローカル権限昇格（LPE）の脆弱性（例: Dirty Pipe、FOTA の脆弱性、mktimer）が見つかることがあります。Nightzuku コネクタは、こうした「アクティベーター」（通常は 1MB 程度の小さな APK）が Nightzuku 内部の起動コマンドを取得し、それぞれの脆弱性を利用して端末上で直接 Nightzuku サーバーを起動するための、標準化されたインターフェースを提供します。

## 前提条件

安全のため、この機能は**既定では無効**です。
利用するには、Nightzuku の設定 →**試験運用機能**を開き、**Nightzuku コネクタ**を有効にして、安全に関する警告に同意する必要があります。

## 使い方

Nightzuku コネクタが有効な場合、Nightzuku は次の URI で、外部に公開されたローカルの `ContentProvider` を提供します:

```
content://kerneldroid.nightzuku.connector
```

### プロバイダへの問い合わせ

この URI に問い合わせると、`command` という名前の列を 1 つだけ持ち、行も 1 行だけの `Cursor` を取得できます。

#### Android Java/Kotlin の例:

```kotlin
val uri = Uri.parse("content://kerneldroid.nightzuku.connector")
contentResolver.query(uri, null, null, null, null)?.use { cursor ->
    if (cursor.moveToFirst()) {
        val commandIndex = cursor.getColumnIndex("command")
        if (commandIndex != -1) {
            val nightzukuCommand = cursor.getString(commandIndex)
            // 取得したコマンドを、ローカルの脆弱性を利用するペイロードで実行する
            Log.d("Activator", "Got command: $nightzukuCommand")
        }
    }
}
```

#### シェルスクリプトの例:

```bash
OUTPUT=$(content query --uri content://kerneldroid.nightzuku.connector)
if [[ $OUTPUT == *"command="* ]]; then
    CMD=$(echo "$OUTPUT" | grep -o 'command=.*' | cut -d= -f2-)
    # 昇格した権限でコマンドを実行する
    eval "$CMD"
else
    echo "Nightzuku Connectors is not enabled or Nightzuku is not installed."
fi
```

### 戻り値

- **Nightzuku コネクタ**が**有効**な場合、プロバイダは Nightzuku 内部のサーバーを起動するために必要な、完全なシェルコマンドの文字列を返します。
- **Nightzuku コネクタ**が**無効**な場合（またはユーザーが警告に同意していない場合）、プロバイダはクライアントの問い合わせ方法に応じて、`null` または空の結果セットを返します。
