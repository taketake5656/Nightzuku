# NightDog

**日本語** | [English](nightdog.en.md)

NightDog は、Shizuku サーバープロセスのハングやシステムサービスの停止を監視し、異常を検知した場合はプロセスを終了させて、スターターが再起動できるようにします。

## 仕組み

1. サーバーのメインスレッドが 5 秒ごとに `NightDog.beat()` を呼び出します。
2. ウォッチドッグのスレッドが、60 秒ごとのポーリングでハートビートが途切れていないかを確認します。
3. ハートビートが 60 秒以上途切れている場合、ウォッチドッグは `Process.killProcess()` を呼び出します。
4. ウォッチドッグは、4 つのシステムサービス（`package`、`activity`、`user`、`appops`）に `linkToDeath` を登録しています。いずれかのサービスが停止した場合、NightDog は指数バックオフで再接続を試みます（最大 10 回）。
5. 応答しないものの停止通知が届いていないサービスを検出するため、ポーリングのたびに予備の `pingBinder()` チェックも行います。

## 有効化と無効化

NightDog は、マネージャーアプリの試験運用機能のスイッチで切り替えます。スイッチを操作すると、サーバーに binder トランザクション（`setNightDogEnabled`／`getNightDogEnabled`）が送られ、サーバーはそれに応じてウォッチドッグとハートビートのループを開始または停止します。

## 再起動の動作

サーバープロセスが終了すると、スターター（独立したネイティブプロセス）が終了を検知し、最大 5 回までサーバーを再起動します。

## 設定値

| パラメータ | 既定値 |
|-----------|---------|
| ハートビートの間隔 | 5 秒 |
| タイムアウト（ハートビートの途切れ） | 60 秒 |
| ポーリングの間隔 | 60 秒 |
| 再接続の最大試行回数 | 10 回 |

## 関連ファイル

- `nightdog/src/main/java/rikka/shizuku/nightdog/NightDog.kt`
- `server/src/main/java/rikka/shizuku/server/ShizukuService.java`
- `manager/src/main/java/moe/shizuku/manager/settings/LabFeaturesActivity.kt`
