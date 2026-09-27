# Android 17（API 37）互換性の比較

**日本語** | [English](android-17-compatibility.en.md)

このドキュメントでは、Android 17（Cinnamon Bun / API 37）Canary 上で動作させた場合の、オリジナルの Shizuku（v13.6.0）と、モダン化したフォーク（v13.6.0.r39 以降）の技術的な比較を示します。

## 根本的な問題

2026 年 5 月の Android 17 のアップデートで、Google は仮想デバイスへの対応とセキュリティ向上のため、非公開 API に大きな変更を加えました。具体的には、複数デバイスを区別できるよう、`IPackageManager` と `IPermissionManager` のメソッドシグネチャに `deviceId` パラメータが追加されました。

### 影響を受ける API
- `IPermissionManager.grantRuntimePermission`
- `IPermissionManager.revokeRuntimePermission`
- `IPermissionManager.checkPermission`
- `IPackageManager.getInstalledPackages`
- `IPackageManager.getPackageInfo`
- `IPackageManager.getApplicationInfo`

Yozuku はこれらの非公開 API に依存して動作するため、従来の実装は API 37 以降で `NoSuchMethodError` により失敗します。

## オリジナルの Shizuku（v13.6.0）

オリジナルの Shizuku を Android 17 で動かすと、サーバープロセスは起動するものの、権限の判定やパッケージ一覧の取得の際に致命的な失敗が発生します。

### 動作
- 権限の付与／取り消しが何も表示されずに失敗するか、サーバーがクラッシュします。
- パッケージ一覧が空で返されるか、クライアントアプリでクラッシュが発生します。
- システムサービスとのやり取りで、Logcat に `NoSuchMethodError` が頻繁に記録されます。

## Yozuku（モダン化したフォーク）

Yozuku は `Android17Compat.java` によって、高性能な動的リフレクションのフォールバックを実装しています。

### 技術的な実装
- **動的なメソッド解決:** 対象のメソッド（例: `grantRuntimePermission`）が新しい `deviceId` パラメータを必要とするかを判別し、必要な場合は `Context.DEVICE_ID_DEFAULT`（0）を渡します。
- **キャッシュ層:** 解決済みの `Method` オブジェクトやシステムサービスのプロキシを保持して、リフレクションのオーバーヘッドをなくし、ネイティブに近い性能を確保します。
- **サービスへの組み込み:** `ShizukuService` は重要なシステム API の呼び出しすべてに `Android17Compat` を使用し、API 37 以降での安定性を確保しています。

## まとめ

オリジナルの Shizuku は、仮想デバイス API への移行により Android 17 と互換性がありません。Yozuku の `Android17Compat` 層は完全な機能を取り戻し、最新の Android バージョンにおける昇格した権限でのアクセスの標準であり続けることを目指しています。
