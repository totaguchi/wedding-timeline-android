# Contract: Posts (Rooms → Posts)

## Collections
- `rooms/{roomId}/posts/{postId}`
- `rooms/{roomId}/members/{userId}`
- `rooms/{roomId}/userLikes/{userId}/posts/{postId}`
- `roomSecrets/{roomId}`

## Post DTO (Firestore)
- `id: String`
- `roomId: String`
- `authorId: String`
- `text: String`
- `media: List<MediaDto>` with `MediaDto { url: String, type: "image"|"video" }`
- `tag: "ceremony"|"reception"`
- `createdAt: Timestamp` (Firestore)
- `likeCount: Int`

## Domain Model (App)
- 時刻は **epochMs: Long** に正規化
- `tag` は enum で厳格化（`ceremony|reception`）

## Queries
- Latest: order by `createdAt` DESC, `limit N`
- Optional filter: `createdAt > :after`

## Index (例)
- `rooms/{roomId}/posts` on `createdAt DESC`

## Mapping Rules
- `Timestamp` ↔︎ `epochMs` の変換は **toDomain()/toDto()** のみで行う
- DTO を UI に渡さない。必ず Domain 経由