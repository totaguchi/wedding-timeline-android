# AGENTS.md — WeddingTimeline (iOS + Android, SwiftUI/Compose, MVVM, Firebase)

本ドキュメントはエージェント型開発のための役割定義・ルール・ハンドオフ手順を規定します。  
対象: iOS (SwiftUI), Android (Jetpack Compose), Backend(Firebase Firestore/Storage)

---

## 0) 全体ポリシー（共通）

- **ターゲット**
  - iOS: iOS 17+（最小 16 可）, Swift 5.9+, Swift Concurrency, Observation(@Observable)
  - Android: Kotlin（Coroutines/Flow）, Jetpack Compose, MinSDK はプロジェクト要件に合わせる
- **アーキテクチャ**: MVVM + Repository + DTO→Domain 変換。UI は Domain のみを参照（DTO を渡さない）
- **データストア**: Firebase Firestore（強整合性前提の UI は避ける）、Cloud Storage（画像/動画）
- **非同期**: iOS=async/await、Android=Coroutines（suspend/Flow）。UI 更新はメインスレッド厳守
- **パフォーマンス**: タイムラインは大規模件数（~1000）想定。差分更新・キャンセル可能ローディング・不要な再計測/再合成の抑制
- **依存**: 最小主義（SPM/Gradle）。重厚な外部ライブラリの無闇な追加禁止
- **命名**: `TimeLinePostDTO`, `TimeLinePost`, `PostRepository`, `TimeLineViewModel`
- **i18n/a11y**: ローカライズ前提。アクセシビリティ属性（ラベル/コンテント説明）付与
- **セキュリティ**: 機密は環境変数/CI Secrets で管理。API キー直コミット禁止

---

## 1) エージェント一覧（役割と成果物）

### A1. iOS SwiftUI エージェント（主担当/iOS）
**責務**: 画面/コンポーネント実装、Observation 適用、`task(id:)` 設計、アニメ最適化  
**成果物**: コンパイル可能な Swift ファイル + `#Preview`、軽量パフォーマンス改善 PR

### A2. Android Compose エージェント（主担当/Android）
**責務**: Compose 画面/コンポーネント、ViewModel（StateFlow）、Navigation、Paging  
**成果物**: コンパイル可能な Kotlin ファイル、Preview アノテーション付与、Media/画像ローディング最適化

### A3. Firebase/データ層 エージェント（共通）
**責務**: Firestore コレクション/インデックス設計、DTO/Domain、Repository、Cloud Storage パス  
**成果物**: DTO/Domain/Repository 実装（両OS）、最小限のルール案とサンプルクエリ

### A4. パフォーマンス/計測 エージェント（共通）
**責務**: レンダリング/メモリ/スレッドのボトルネック分析、検証手順、しきい値（Apdex 等）  
**成果物**: 計測手順、チェックリスト、改善差分の提案

### A5. QA/テスト エージェント（共通）
**責務**: ユニット/統合/スナップショット（可能なら）テスト、CI での並列化とログ収集  
**成果物**: iOS XCTest、Android JUnit/Compose UI Test、テストデータ/Fixtures

### A6. DevInfra/CI エージェント（共通）
**責務**: Lint/Format、CI（GitHub Actions 等）での build/test/artifact、署名/Secrets 連携  
**成果物**: `.github/workflows/**`, フォーマッタ/リンタ設定、`Makefile` or `scripts/**`

### A7. Docs/UX ライター エージェント（共通）
**責務**: README、ADR、開発ガイドライン、スクショ/GIF 導線  
**成果物**: `README.md`, `docs/adr/**`, コンポーネント使用例

---

## 2) ディレクトリとルーティング規則

- **iOS**: `ios/App/`, `ios/Features/**`, `ios/Shared/Models/`, `ios/Shared/Services/`, `ios/Shared/Components/`
- **Android**: `android/app/`, `android/core-model/`, `android/core-data/`, `android/core-ui/`, `android/feature-timeline/`, `android/feature-compose/`
- **共通ドキュメント**: `docs/**`, `backend/firestore/**`, `.github/**`, `scripts/**`

**担当ルール**  
- `ios/**` ⇒ iOS エージェント  
- `android/**` ⇒ Android エージェント  
- `backend/**`, `Shared/**`（モデル/リポジトリ仕様） ⇒ Firebase/データ層  
- `Tests/**`, `UITests/**`, `android/**/androidTest/**` ⇒ QA  
- `.github/**`, `scripts/**` ⇒ DevInfra  
- `docs/**`, `README.md` ⇒ Docs

曖昧な場合の相談順: iOS → Android → データ層 → Perf → QA → Docs

---

## 3) ハンドオフ・プロトコル

1. `docs/handoffs/YYYYMMDD-<topic>.md` を作成（課題/前提/入出力/制約/完了条件/未決事項）
2. PR に `Handoff:` セクションと担当エージェントを記載
3. 受領側は `Open Questions` を追記し最小実装を返却
4. 最終結合は各プラットフォーム主担当が実施、QA がテスト確定

---

## 4) コーディング規約（要点）

**共通**
- DTO は Firestore 生データ、Domain はアプリ内部で使用
- `init(dto:)`（iOS）/ 拡張 `toDomain()`（Android）で相互変換
- エラーは型安全に集約（`AppError`/`sealed class`）。ユーザ向け文言は別責務に分離

**iOS**
- Observation（`@Observable`）を最優先。UI 更新は `@MainActor`
- `task(id:)` を使い `onAppear` に重処理を置かない
- `GeometryReader` は必要最小限、画像は非同期+キャッシュ

**Android**
- ViewModel は `StateFlow`/`MutableStateFlow` + `collectAsStateWithLifecycle`
- 画像は **Coil**、動画は **Media3(ExoPlayer)** を推奨
- Navigation Compose、Paging 3、WorkManager（アップロード/リトライ）
- 安定性: `@Immutable`/`@Stable` の付与・`LazyColumn(items, key=...)` のキー必須
- 再合成抑制: `remember`, `derivedStateOf`, `snapshotFlow` を適所で使用

---

## 5) Firestore/モデル 仕様（共通契約）

**コレクション（例）**
- `rooms/{roomId}/posts/{postId}`
- `rooms/{roomId}/members/{userId}`
- `rooms/{roomId}/userLikes/{userId}/posts/{postId}`
- `roomSecrets/{roomId}`

**Post DTO（共通フィールド）**
- `id: String`
- `roomId: String`
- `authorId: String`
- `text: String`
- `media: [MediaDTO] / List<MediaDto>`
- `tag: "ceremony" | "reception"`
- `createdAt: Timestamp`
- `likeCount: Int`

**iOS: Swift DTO/Domain**
```swift
import FirebaseFirestore
import FirebaseFirestoreSwift

public struct TimeLinePostDTO: Codable, Sendable, Identifiable {
    public var id: String
    public var roomId: String
    public var authorId: String
    public var text: String
    public var media: [MediaDTO]
    public var tag: String
    public var createdAt: Timestamp
    public var likeCount: Int
}

public struct TimeLinePost: Sendable, Identifiable {
    public let id: String
    public let roomId: String
    public let authorId: String
    public let text: String
    public let media: [Media]
    public let tag: Tag
    public let createdAt: Date
    public var likeCount: Int

    public enum Tag: String, CaseIterable { case ceremony, reception }

    public init(dto: TimeLinePostDTO) {
        id = dto.id; roomId = dto.roomId; authorId = dto.authorId
        text = dto.text; media = dto.media.map(Media.init)
        tag = Tag(rawValue: dto.tag) ?? .ceremony
        createdAt = dto.createdAt.dateValue()
        likeCount = dto.likeCount
    }
}