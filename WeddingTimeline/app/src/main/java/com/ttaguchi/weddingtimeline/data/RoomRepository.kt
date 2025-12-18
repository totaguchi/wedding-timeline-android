package com.ttaguchi.weddingtimeline.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.io.IOException
import com.ttaguchi.weddingtimeline.domain.model.AppUser
import com.ttaguchi.weddingtimeline.domain.model.AppUserDto
import com.ttaguchi.weddingtimeline.domain.model.RoomMember
import com.ttaguchi.weddingtimeline.domain.model.RoomMemberDto
import com.ttaguchi.weddingtimeline.request.JoinError
import com.ttaguchi.weddingtimeline.request.JoinParams
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching and mutating room and room member documents.
 * NOTE: username is display name and allows duplicates. No uniqueness lock is used.
 */
class RoomRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    private fun roomRef(roomId: String): DocumentReference =
        db.collection("rooms").document(roomId)

    private fun memberRef(roomId: String, uid: String): DocumentReference =
        roomRef(roomId).collection("members").document(uid)

    private fun userRef(uid: String): DocumentReference =
        db.collection("users").document(uid)

    /**
     * Sign in anonymously if not already signed in.
     * @return uid of the current user
     */
    suspend fun signInAnonymouslyIfNeeded(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }

        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: throw JoinError.NotSignedIn
    }

    /**
     * Join a room with JoinParams.
     */
    suspend fun joinRoom(params: JoinParams) {
        joinRoom(
            roomId = params.roomId,
            roomKey = params.roomKey,
            username = params.username,
            userIcon = params.selectedIcon,
        )
    }

    /**
     * Join a room with roomId, roomKey, username, and icon.
     */
    suspend fun joinRoom(
        roomId: String,
        roomKey: String,
        username: String,
        userIcon: String,
    ) {
        val roomIdSan = roomId.trim()
        val roomKeySan = roomKey.trim()
        val userNameSan = username.trim()

        // Validation
        if (roomIdSan.isEmpty()) {
            throw JoinError.Message("ルームIDを入力してください。")
        }
        if (roomKeySan.isEmpty()) {
            throw JoinError.Message("入室キーを入力してください。")
        }
        if (userNameSan.isEmpty()) {
            throw JoinError.Message("ユーザー名を入力してください。")
        }
        if (userIcon.isEmpty()) {
            throw JoinError.IconNotSelected
        }

        // Ensure user is signed in
        val uid = try {
            signInAnonymouslyIfNeeded()
        } catch (e: Exception) {
            throw mapJoinError(e)
        }

        // Check if user is already a member
        val existed = try {
            isUserAlreadyInRoom(roomIdSan, uid)
        } catch (e: Exception) {
            throw mapJoinError(e)
        }

        val roomRef = roomRef(roomIdSan)
        val memberRef = memberRef(roomIdSan, uid)
        val usernameLower = userNameSan.lowercase()

        // Check if user is banned
        if (existed) {
            try {
                val memberSnap = memberRef.get(Source.SERVER).await()
                val isBanned = memberSnap.getBoolean("isBanned") ?: false
                if (isBanned) {
                    throw JoinError.Banned
                }
            } catch (e: JoinError.Banned) {
                throw e
            } catch (e: Exception) {
                println("[RoomRepository] ban check failed: ${e.message}")
            }
        }

        // NOTE: Do not read /roomSecrets on client.
        // We write providedKey on first join and Firestore rules validate it server-side.
        try {
            db.runTransaction { transaction ->
                if (existed) {
                    // Update existing member: only allowed fields per Firestore rules
                    val updates = mapOf(
                        "username" to userNameSan,
                        "userIcon" to userIcon,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    transaction.set(memberRef, updates, com.google.firebase.firestore.SetOptions.merge())
                } else {
                    // Create new member: include providedKey for validation
                    val memberData = mapOf(
                        "username" to userNameSan,
                        "usernameLower" to usernameLower,
                        "role" to "member",
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "lastSignedInAt" to FieldValue.serverTimestamp(),
                        "isBanned" to false,
                        "mutedUntil" to null,
                        // Firestore rules validate roomKey via this field.
                        "providedKey" to roomKeySan,
                        "userIcon" to userIcon,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                    transaction.set(memberRef, memberData)
                }
                null
            }.await()
        } catch (e: Exception) {
            println("[RoomRepository] joinRoom transaction failed: ${e.message}")
            throw mapJoinError(e)
        }

        // NOTE: With current Firestore rules, members/{uid} update is restricted to
        // username/userIcon/updatedAt only, so we cannot delete providedKey after create.
    }

    private fun mapJoinError(error: Exception): JoinError {
        val firebaseError = error as? FirebaseFirestoreException
        if (firebaseError != null) {
            val msg = when (firebaseError.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "入室に失敗しました。ルームIDまたは入室キーが正しいか確認してください。"
                FirebaseFirestoreException.Code.NOT_FOUND ->
                    "指定のルームが見つかりませんでした。ルームIDをご確認ください。"
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
                    "タイムアウトしました。通信環境を確認して、もう一度お試しください。"
                FirebaseFirestoreException.Code.UNAVAILABLE ->
                    "サーバーに接続できませんでした。しばらくしてから再度お試しください。"
                FirebaseFirestoreException.Code.CANCELLED ->
                    "操作がキャンセルされました。再度お試しください。"
                else -> "入室に失敗しました。しばらくしてから再度お試しください。"
            }
            return JoinError.Message(msg)
        }

        val authError = error as? FirebaseAuthException
        if (authError != null) {
            val msg = when (authError.errorCode) {
                "ERROR_NETWORK_REQUEST_FAILED" ->
                    "ネットワークエラーのためサインインできませんでした。通信状況を確認して再度お試しください。"
                "ERROR_TOO_MANY_REQUESTS" ->
                    "リクエストが集中しています。時間をおいて再度お試しください。"
                else -> "サインインに失敗しました。もう一度お試しください。"
            }
            return JoinError.Message(msg)
        }

        if (error is IOException) {
            return JoinError.Message("ネットワークに接続できません。通信環境を確認して再度お試しください。")
        }

        return JoinError.Message("入室に失敗しました。しばらくしてから再度お試しください。")
    }

    /**
     * Change username within a room.
     */
    suspend fun changeUsername(roomId: String, newUsername: String) {
        val uid = auth.currentUser?.uid ?: throw JoinError.NotSignedIn

        val roomIdSan = roomId.trim()
        val usernameSan = newUsername.trim()

        if (usernameSan.isEmpty()) {
            throw IllegalArgumentException("ユーザー名を入力してください")
        }

        val memberRef = memberRef(roomIdSan, uid)

        try {
            db.runTransaction { transaction ->
                // Username allows duplicates, no lock operation needed
                val updates = mapOf(
                    "username" to usernameSan,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
                transaction.set(memberRef, updates, com.google.firebase.firestore.SetOptions.merge())
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            println("[RoomRepository] changeUsername failed: ${e.message}")
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                throw JoinError.UsernameTaken
            }
            throw JoinError.Unknown
        }
    }

    /**
     * Leave a room (delete member document).
     */
    suspend fun leaveRoom(roomId: String) {
        val roomIdSan = roomId.trim()
        val uid = auth.currentUser?.uid ?: throw JoinError.NotSignedIn
        val memberRef = memberRef(roomIdSan, uid)

        db.runTransaction { transaction ->
            val memberSnap = transaction.get(memberRef)
            if (memberSnap.exists()) {
                transaction.delete(memberRef)
            }
            null
        }.await()
    }

    suspend fun deleteMyAccount(roomId: String) {
        val uid = auth.currentUser?.uid ?: throw JoinError.NotSignedIn
        val roomIdSan = roomId.trim()
        val roomRef = roomRef(roomIdSan)

        try {
            // 1) Delete /rooms/{roomId}/userLikes/{uid}/posts/*
            val userLikesPosts = roomRef
                .collection("userLikes")
                .document(uid)
                .collection("posts")
            batchDelete(userLikesPosts.orderBy(FieldPath.documentId()))

            // 2) Delete collectionGroup("likes") where userId == uid && roomId == roomId
            val likesGroup = db.collectionGroup("likes")
                .whereEqualTo("userId", uid)
                .whereEqualTo("roomId", roomIdSan)
            batchDelete(likesGroup)

            // 3) Delete authored posts and their likes subcollections
            var last: DocumentSnapshot? = null
            while (true) {
                var query = roomRef.collection("posts")
                    .whereEqualTo("authorId", uid)
                    .orderBy(FieldPath.documentId())
                    .limit(200)
                if (last != null) {
                    query = query.startAfter(last)
                }
                val snap = query.get(Source.SERVER).await()
                if (snap.documents.isEmpty()) break

                val batch = db.batch()
                for (doc in snap.documents) {
                    val postRef = doc.reference
                    // Delete likes subcollection
                    val likes = postRef.collection("likes").orderBy(FieldPath.documentId())
                    batchDelete(likes)
                    // Delete post itself
                    batch.delete(postRef)
                }
                batch.commit().await()
                last = snap.documents.lastOrNull()
            }

            // 4) Delete members/{uid}
            memberRef(roomIdSan, uid).delete().await()

            println("[RoomRepository] deleteMyAccount completed for uid=$uid in roomId=$roomIdSan")
        } catch (e: Exception) {
            println("[RoomRepository] deleteMyAccount failed: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Batch delete query results with pagination.
     */
    private suspend fun batchDelete(query: Query, pageSize: Int = 300) {
        var last: DocumentSnapshot? = null
        while (true) {
            var q = query.orderBy(FieldPath.documentId()).limit(pageSize.toLong())
            if (last != null) {
                q = q.startAfter(last)
            }
            val snap = q.get(Source.SERVER).await()
            if (snap.documents.isEmpty()) break

            val batch = db.batch()
            for (doc in snap.documents) {
                batch.delete(doc.reference)
            }
            batch.commit().await()
            last = snap.documents.lastOrNull()
        }
    }

    /**
     * Check if user is already a member of the room.
     */
    suspend fun isUserAlreadyInRoom(roomId: String, uid: String): Boolean {
        val roomIdSan = roomId.trim()
        val memberRef = memberRef(roomIdSan, uid)

        return try {
            val snapshot = memberRef.get(Source.SERVER).await()
            snapshot.exists()
        } catch (e: Exception) {
            println("[RoomRepository] isUserAlreadyInRoom failed: ${e.message}")
            throw e
        }
    }

    /**
     * Fetch room member profile (Swift: fetchRoomUser).
     * Returns minimal set for UI: username and userIcon.
     */
    suspend fun fetchRoomUser(roomId: String, uid: String): Pair<String, String?>? {
        val roomIdSan = roomId.trim()
        val ref = memberRef(roomIdSan, uid)

        return try {
            val snap = ref.get(Source.SERVER).await()
            if (!snap.exists()) return null

            val username = snap.getString("username")
            val userIcon = snap.getString("userIcon")

            if (username.isNullOrEmpty()) return null
            Pair(username, userIcon)
        } catch (e: Exception) {
            println("[RoomRepository] fetchRoomUser failed: ${e.message}")
            throw e
        }
    }

    /**
     * Fetch room member (full data).
     */
    suspend fun fetchRoomMember(roomId: String, uid: String): RoomMember {
        val roomIdSan = roomId.trim()
        val ref = memberRef(roomIdSan, uid)

        return try {
            val snap = ref.get(Source.SERVER).await()
            toRoomMember(snap) ?: throw IllegalStateException("Member not found")
        } catch (e: Exception) {
            println("[RoomRepository] fetchRoomMember failed: ${e.message}")
            throw e
        }
    }

    /**
     * Fetch user from /users/{uid}.
     */
    suspend fun fetchUser(uid: String): AppUser? {
        return try {
            val snap = userRef(uid).get(Source.SERVER).await()
            if (!snap.exists()) return null
            snap.toObject(AppUserDto::class.java)?.toDomain()
        } catch (e: Exception) {
            println("[RoomRepository] fetchUser failed for uid=$uid: ${e.message}")
            null
        }
    }

    /**
     * Fetch user's roomId from /users/{uid}.
     * Returns null if user doesn't have a roomId or document doesn't exist.
     */
    suspend fun fetchUserRoomId(uid: String): String? {
        return try {
            val snap = userRef(uid).get(Source.SERVER).await()
            snap.getString("roomId")
        } catch (e: Exception) {
            println("[RoomRepository] fetchUserRoomId failed for uid=$uid: ${e.message}")
            null
        }
    }

    /**
     * Fetch member data with source option.
     */
    suspend fun fetchMember(roomId: String, uid: String, source: Source = Source.DEFAULT): RoomMember? {
        return try {
            val snap = memberRef(roomId, uid).get(source).await()
            toRoomMember(snap)
        } catch (e: Exception) {
            println("[RoomRepository] fetchMember failed roomId=$roomId uid=$uid: ${e.message}")
            null
        }
    }

    /**
     * Listen to room member data changes in real-time.
     */
    fun listenMember(roomId: String, uid: String): Flow<RoomMember?> = callbackFlow {
        val listener = memberRef(roomId, uid).addSnapshotListener { snap, err ->
            if (err != null) {
                println("[RoomRepository] listenMember error: ${err.message}")
                trySend(null)
                return@addSnapshotListener
            }
            trySend(toRoomMember(snap))
        }
        awaitClose { listener.remove() }
    }

    /**
     * Check if user is a member of the room.
     */
    suspend fun isMember(roomId: String, uid: String): Boolean {
        return try {
            val cacheSnap = memberRef(roomId, uid).get(Source.CACHE).await()
            if (cacheSnap.exists()) return true

            val serverSnap = memberRef(roomId, uid).get(Source.SERVER).await()
            serverSnap.exists()
        } catch (e: Exception) {
            println("[RoomRepository] isMember check failed: ${e.message}")
            false
        }
    }

    /**
     * Check if room exists.
     */
    suspend fun roomExists(roomId: String): Boolean {
        return try {
            val snap = roomRef(roomId).get(Source.SERVER).await()
            snap.exists()
        } catch (e: Exception) {
            println("[RoomRepository] roomExists check failed: ${e.message}")
            false
        }
    }

    /**
     * Fetch all members of a room.
     */
    suspend fun fetchAllMembers(roomId: String): List<RoomMember> {
        return try {
            val snap = roomRef(roomId).collection("members").get(Source.SERVER).await()
            snap.documents.mapNotNull { toRoomMember(it) }
        } catch (e: Exception) {
            println("[RoomRepository] fetchAllMembers failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Listen to all members of a room in real-time.
     */
    fun listenAllMembers(roomId: String): Flow<List<RoomMember>> = callbackFlow {
        val listener = roomRef(roomId).collection("members")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    println("[RoomRepository] listenAllMembers error: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val members = snap?.documents?.mapNotNull { toRoomMember(it) }.orEmpty()
                trySend(members)
            }
        awaitClose { listener.remove() }
    }

    private fun toRoomMember(snap: DocumentSnapshot?): RoomMember? {
        if (snap == null || !snap.exists()) return null

        return try {
            val dto = snap.toObject(RoomMemberDto::class.java) ?: return null
            dto.toDomain()
        } catch (e: Exception) {
            println("[RoomRepository] toRoomMember decode failed id=${snap.id}: ${e.message}")
            null
        }
    }
}
