# Copilot Instructions — Android (Jetpack Compose + MVVM + Firebase)

> 本ファイルは Copilot に、本プロジェクト（WeddingTimeline Android 版）の方針・規約・出力形式を伝えるためのガイドです。  
> iOS 版とは **DTO/Domain 契約を共通** にしつつ、Android 実装に最適化した記述を行います。

## Project Overview
- App: WeddingTimeline (Android)
- Tech: Kotlin / Jetpack Compose / Coroutines & Flow / Navigation Compose / Paging 3
- Min SDK:（プロジェクト設定に準拠、未確定時は 24+ を想定）
- Backend: Firebase Firestore / Cloud Storage
- Architecture: MVVM + Repository + DTO→Domain 変換
- Use cases: X/Twitter 風のタイムライン、Room 単位の公開範囲、タグ切替（挙式/披露宴）

## Generation Ground Rules
- **コンパイル可能な最小単位**で出力（1ファイルに収まるサンプルを優先。疑似 API を勝手に生やさない）。
- **Coroutines/Flow 優先**：`suspend`, `viewModelScope`, `StateFlow` を用いる。
- **UI state は単一の `UiState` データクラス**で集約。UI からは Domain のみ参照（DTO を渡さない）。
- **FireStore モデルは DTO として定義** → `toDomain()` / `toDto()` で相互変換。
- **画像/動画**：Coil / Media3（ExoPlayer）を前提。読み込みはキャンセル可能・キャッシュ有効。
- **Navigation Compose** を使用。大規模一覧は **Paging 3** を推奨。
- **再合成対策**：`key` 指定、`@Immutable/@Stable`、`remember`, `derivedStateOf` を適所で。
- **依存は最小限**（標準/Google 推奨に限定。重い外部ライブラリを安易に追加しない）。
- **コメント/Doc**：KDoc で要点を記載。メソッドには簡潔な使用例があると望ましい。
- **a11y/i18n**：`contentDescription`/talkback 対応、`strings.xml` を使用。

## Folder Conventions
- `android/app/`（アプリ）
- `android/core-model/`（Domain/DTO）
- `android/core-data/`（Repository/FireStore/Storage）
- `android/core-ui/`（UI 基盤/Design System）
- `android/feature-timeline/`（タイムライン機能）
- `android/feature-compose/`（投稿画面など）
- `docs/`, `.github/`, `scripts/`（共通）

## Firestore Contract（共通契約）
- Collections:
  - `rooms/{roomId}/posts/{postId}`
  - `rooms/{roomId}/members/{userId}`
  - `rooms/{roomId}/userLikes/{userId}/posts/{postId}`
  - `roomSecrets/{roomId}`
- Post DTO 共通フィールド：
  - `id: String`, `roomId: String`, `authorId: String`, `text: String`
  - `media: List<MediaDto>`
  - `tag: "ceremony" | "reception"`
  - `createdAt: Timestamp`
  - `likeCount: Int`

### Kotlin DTO / Domain（サンプル）
```kotlin
import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable
import androidx.compose.runtime.Immutable

data class MediaDto(
    val url: String = "",
    val type: String = "image" // image|video
)

@Immutable
data class Media(
    val url: String,
    val type: Type
) {
    enum class Type { image, video }
}

fun MediaDto.toDomain() = Media(
    url = url,
    type = runCatching { Media.Type.valueOf(type) }.getOrDefault(Media.Type.image)
)

fun Media.toDto() = MediaDto(
    url = url,
    type = type.name
)

data class TimeLinePostDto(
    val id: String = "",
    val roomId: String = "",
    val authorId: String = "",
    val text: String = "",
    val media: List<MediaDto> = emptyList(),
    val tag: String = "ceremony",
    val createdAt: Timestamp = Timestamp.now(),
    val likeCount: Int = 0
)

@JvmInline value class PostId(val value: String)

@Immutable
@Serializable
data class TimeLinePost(
    val id: PostId,
    val roomId: String,
    val authorId: String,
    val text: String,
    val media: List<Media>,
    val tag: Tag,
    val createdAtEpochMs: Long,
    val likeCount: Int
) {
    enum class Tag { ceremony, reception }
}

fun TimeLinePostDto.toDomain(): TimeLinePost = TimeLinePost(
    id = PostId(id),
    roomId = roomId,
    authorId = authorId,
    text = text,
    media = media.map { it.toDomain() },
    tag = runCatching { TimeLinePost.Tag.valueOf(tag) }.getOrDefault(TimeLinePost.Tag.ceremony),
    createdAtEpochMs = createdAt.toDate().time,
    likeCount = likeCount
)

fun TimeLinePost.toDto(): TimeLinePostDto = TimeLinePostDto(
    id = id.value,
    roomId = roomId,
    authorId = authorId,
    text = text,
    media = media.map { it.toDto() },
    tag = tag.name,
    createdAt = Timestamp(createdAtEpochMs / 1000, 0),
    likeCount = likeCount
)