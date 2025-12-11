package com.ttaguchi.weddingtimeline.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QueryDocumentSnapshot
import com.ttaguchi.weddingtimeline.model.Media
import com.ttaguchi.weddingtimeline.model.MediaDto
import com.ttaguchi.weddingtimeline.model.PostTag
import com.ttaguchi.weddingtimeline.model.TimeLinePost
import com.ttaguchi.weddingtimeline.model.TimeLinePostDto
import com.ttaguchi.weddingtimeline.model.toDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching and mutating post documents.
 */
class PostRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val mapper: PostMapper = PostMapper(),
) {

    private fun postRef(roomId: String, postId: String): DocumentReference =
        db.collection("rooms").document(roomId).collection("posts").document(postId)

    private fun postsCollection(roomId: String) =
        db.collection("rooms").document(roomId).collection("posts")

    private fun likeRef(roomId: String, postId: String, uid: String): DocumentReference =
        db.collection("rooms").document(roomId)
            .collection("posts").document(postId)
            .collection("likes").document(uid)

    private fun userLikeRef(roomId: String, uid: String, postId: String): DocumentReference =
        db.collection("rooms").document(roomId)
            .collection("userLikes").document(uid)
            .collection("posts").document(postId)

    /**
     * Create a new post document. Media assets are assumed to be already uploaded.
     */
    suspend fun createPost(
        roomId: String,
        postId: String,
        content: String,
        authorId: String,
        authorName: String,
        userIcon: String,
        tag: PostTag,
        media: List<Media>,
    ) {
        val roomIdSan = roomId.trim()
        val contentSan = content.trim()

        // Validation
        require(roomIdSan.isNotEmpty()) { "ルーム情報が取得できませんでした" }
        require(tag == PostTag.CEREMONY || tag == PostTag.RECEPTION) {
            "タグは挙式/披露宴のみ選択可能です"
        }

        // Convert Media to payload (matching Swift's structure)
        val mediaPayload = media.map { mediaItem ->
            val item = mutableMapOf<String, Any?>(
                "id" to mediaItem.id,
                "type" to mediaItem.type.rawValue,
                "mediaUrl" to mediaItem.url,  // Use "mediaUrl" to match Swift
                "width" to mediaItem.width,
                "height" to mediaItem.height,
            )
            if (mediaItem.duration != null) {
                item["duration"] = mediaItem.duration
            }
            if (mediaItem.storagePath != null) {
                item["storagePath"] = mediaItem.storagePath
            }
            item.filterValues { it != null }
        }

        println("[PostRepository] Creating post with ${mediaPayload.size} media items")
        mediaPayload.forEachIndexed { index, item ->
            println("[PostRepository]   Media[$index]: $item")
        }

        // Note: roomId is NOT stored in the document, only in the path (Swift compatible)
        val payload = mapOf(
            "content" to contentSan,
            "authorId" to authorId,
            "authorName" to authorName,
            "userIcon" to userIcon,
            "tag" to tag.rawValue,
            "createdAt" to FieldValue.serverTimestamp(),
            "media" to mediaPayload,
            "likeCount" to 0,
            "replyCount" to 0,
            "retweetCount" to 0,
        )
        
        val docRef = postsCollection(roomIdSan).document(postId)
        docRef.set(payload).await()
        println("[PostRepository] Created post: roomId=$roomIdSan, postId=$postId")
    }

    /**
     * Fetch posts with pagination.
     */
    suspend fun fetchPosts(
        roomId: String,
        limit: Long = 50,
        startAfter: DocumentSnapshot? = null,
    ): FetchPostsResult {
        println("[PostRepository] fetchPosts: roomId=$roomId, limit=$limit")
        
        val uid = auth.currentUser?.uid
        println("[PostRepository] Current user uid=$uid")

        val collectionPath = "rooms/$roomId/posts"
        println("[PostRepository] Querying collection: $collectionPath")

        var query: Query = postsCollection(roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)

        startAfter?.let {
            query = query.startAfter(it)
        }

        try {
            val snapshot = query.get().await()
            println("[PostRepository] Query completed: size=${snapshot.size()}, isEmpty=${snapshot.isEmpty}")
            
            if (snapshot.isEmpty) {
                println("[PostRepository] No documents found in $collectionPath")
                return FetchPostsResult(posts = emptyList(), lastSnapshot = null)
            }

            val docs = snapshot.documents.mapNotNull { it as? QueryDocumentSnapshot }
            println("[PostRepository] Converted ${docs.size} documents")

            // ===== isLiked を一括収集（ユーザー/ルーム専用のミラー index から 1 クエリ） =====
            val likedSet = if (uid != null) {
                try {
                    val likedSnap = db.collection("rooms").document(roomId)
                        .collection("userLikes").document(uid)
                        .collection("posts")
                        .get()
                        .await()
                    likedSnap.documents.map { it.id }.toSet()
                } catch (e: Exception) {
                    println("[PostRepository] userLikes fetch failed: ${e.message}")
                    emptySet()
                }
            } else {
                emptySet()
            }

            val posts = mapper.makePosts(docs, likedSet, roomId)
            println("[PostRepository] Mapped ${posts.size} posts successfully")
            
            posts.forEachIndexed { index, post ->
                println("[PostRepository] Post $index: id=${post.id}, content=${post.content.take(50)}, isLiked=${post.isLiked}")
            }

            val lastSnap = docs.lastOrNull()

            return FetchPostsResult(posts = posts, lastSnapshot = lastSnap)
        } catch (e: Exception) {
            println("[PostRepository] Error in fetchPosts: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Listen to latest posts with real-time updates.
     */
    fun listenLatestWithIsLiked(roomId: String, limit: Long = 20): Flow<PostsWithLikes> = callbackFlow {
        println("[PostRepository] Starting listener for roomId=$roomId")
        
        val uid = auth.currentUser?.uid
        if (uid == null) {
            // 未ログインの場合はいいね情報なしで購読
            val registration = postsCollection(roomId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        println("[PostRepository] Listen error: ${error.message}")
                        close(error)
                        return@addSnapshotListener
                    }
                    val docs = snapshot?.documents?.mapNotNull { it as? QueryDocumentSnapshot } ?: emptyList()
                    val posts = mapper.makePosts(docs, emptySet(), roomId)
                    trySend(PostsWithLikes(posts))
                }

            awaitClose { registration.remove() }
            return@callbackFlow
        }
        
        // 直近のポスト群 & 自分の liked セットを維持
        var latestDocs: List<QueryDocumentSnapshot> = emptyList()
        var likedSet: Set<String> = emptySet()

        // posts listener
        val postsRegistration = postsCollection(roomId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("[PostRepository] Posts listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                latestDocs = snapshot?.documents?.mapNotNull { it as? QueryDocumentSnapshot } ?: emptyList()
                val posts = mapper.makePosts(latestDocs, likedSet, roomId)
                println("[PostRepository] Posts updated: ${posts.size} posts")
                trySend(PostsWithLikes(posts))
            }

        // likes listener（ユーザー/ルーム専用のミラー index）
        val likesRegistration = db.collection("rooms").document(roomId)
            .collection("userLikes").document(uid)
            .collection("posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    println("[PostRepository] Likes listen error: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                likedSet = snapshot?.documents?.map { it.id }?.toSet() ?: emptySet()
                val posts = mapper.makePosts(latestDocs, likedSet, roomId)
                println("[PostRepository] Likes updated: ${likedSet.size} liked posts")
                trySend(PostsWithLikes(posts))
            }

        awaitClose { 
            println("[PostRepository] Listener closed")
            postsRegistration.remove()
            likesRegistration.remove()
        }
    }

    /**
     * Toggle like on a post.
     */
    suspend fun toggleLike(roomId: String, postId: String, uid: String, newIsLiked: Boolean): Int {
        val roomIdSan = roomId.trim()
        val likeRef = likeRef(roomIdSan, postId, uid)
        val mirrorRef = userLikeRef(roomIdSan, uid, postId)
        val postRef = postRef(roomIdSan, postId)

        return db.runTransaction { transaction ->
            // Post の取得
            val postSnap = transaction.get(postRef)
            if (!postSnap.exists()) {
                throw IllegalStateException("Post does not exist")
            }

            var likeCount = (postSnap.getLong("likeCount") ?: 0L).toInt()

            // likes サブコレクション（存在チェック）
            val likeSnap = try {
                transaction.get(likeRef)
            } catch (e: Exception) {
                null
            }
            val exists = likeSnap?.exists() == true

            if (newIsLiked) {
                if (!exists) {
                    transaction.set(likeRef, mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "userId" to uid,
                        "roomId" to roomIdSan,
                        "postId" to postId
                    ))
                    transaction.set(mirrorRef, mapOf(
                        "createdAt" to FieldValue.serverTimestamp(),
                        "roomId" to roomIdSan
                    ))
                    likeCount += 1
                    transaction.update(postRef, "likeCount", likeCount)
                }
            } else {
                if (exists) {
                    transaction.delete(likeRef)
                    transaction.delete(mirrorRef)
                    likeCount = maxOf(0, likeCount - 1)
                    transaction.update(postRef, "likeCount", likeCount)
                }
            }
            
            likeCount
        }.await()
    }

    /**
     * Generate a new post ID.
     */
    fun generatePostId(roomId: String): String {
        val roomIdSan = roomId.trim()
        return postsCollection(roomIdSan).document().id
    }
}

/**
 * Mapping helper between Firestore snapshot and models.
 */
class PostMapper {
    fun toDomain(doc: DocumentSnapshot, roomId: String, isLiked: Boolean = false): TimeLinePost? {
        return try {
            // 生データを確認
            val data = doc.data
            println("[PostMapper] Raw document data for ${doc.id}:")
            println("[PostMapper]   media field: ${data?.get("media")}")
            println("[PostMapper]   media type: ${data?.get("media")?.javaClass?.name}")
            
            val dto = doc.toObject(TimeLinePostDto::class.java)
            if (dto == null) {
                println("[PostMapper] Failed to convert document to DTO: id=${doc.id}")
                return null
            }
            
            println("[PostMapper] DTO media list size: ${dto.media.size}")
            dto.media.forEachIndexed { index, mediaDto ->
                println("[PostMapper]   Media[$index]: id=${mediaDto.id}, url=${mediaDto.url}, type=${mediaDto.type}")
            }
            
            // Set the document ID if not already set by Firestore
            if (dto.id == null) {
                dto.id = doc.id
            }
            
            val result = dto.toDomain(roomId, isLiked)
            if (result == null) {
                println("[PostMapper] Failed to map DTO to domain: id=${doc.id}")
            } else {
                println("[PostMapper] Domain model media list size: ${result.media.size}")
            }
            result
        } catch (e: Exception) {
            println("[PostMapper] Error mapping document: id=${doc.id}, error=${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun makePosts(docs: List<QueryDocumentSnapshot>, likedSet: Set<String>, roomId: String): List<TimeLinePost> {
        println("[PostMapper] makePosts: processing ${docs.size} documents")
        
        val posts = docs.mapNotNull { doc ->
            toDomain(doc, roomId, isLiked = likedSet.contains(doc.id))
        }
        
        println("[PostMapper] Successfully mapped ${posts.size} out of ${docs.size} documents")
        return posts
    }
}

data class FetchPostsResult(
    val posts: List<TimeLinePost>,
    val lastSnapshot: DocumentSnapshot?,
)

data class PostsWithLikes(
    val posts: List<TimeLinePost>,
)