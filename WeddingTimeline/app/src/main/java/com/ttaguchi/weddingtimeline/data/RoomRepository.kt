package com.ttaguchi.weddingtimeline.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.ttaguchi.weddingtimeline.model.RoomMember
import com.ttaguchi.weddingtimeline.model.RoomMemberDto
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

    private fun secretRef(roomId: String): DocumentReference =
        db.collection("roomSecrets").document(roomId)

    private fun memberRef(roomId: String, uid: String): DocumentReference =
        roomRef(roomId).collection("members").document(uid)

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
        if (userIcon.isEmpty()) {
            throw JoinError.IconNotSelected
        }

        // Ensure user is signed in
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
        val uid = auth.currentUser?.uid ?: throw JoinError.NotSignedIn

        // Check if user is already a member
        val existed = isUserAlreadyInRoom(roomIdSan, uid)

        val roomRef = roomRef(roomIdSan)
        val secretRef = secretRef(roomIdSan)
        val memberRef = memberRef(roomIdSan, uid)
        val usernameLower = userNameSan.lowercase()

        // Validate roomKey only for new members
        if (!existed) {
            try {
                val secretSnap = secretRef.get(Source.SERVER).await()
                if (!secretSnap.exists()) {
                    throw IllegalArgumentException("このルームは存在しません")
                }
                val storedKey = secretSnap.getString("roomKey")
                if (storedKey != roomKeySan) {
                    throw JoinError.InvalidKey
                }
            } catch (e: JoinError.InvalidKey) {
                throw e
            } catch (e: Exception) {
                println("[RoomRepository] roomKey validation failed: ${e.message}")
                throw JoinError.Unknown
            }
        }

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

        // Run transaction
        try {
            db.runTransaction { transaction ->
                if (existed) {
                    // Update existing member: username, icon, lastSignedInAt
                    val updates = mapOf(
                        "username" to userNameSan,
                        "usernameLower" to usernameLower,
                        "lastSignedInAt" to FieldValue.serverTimestamp(),
                        "userIcon" to userIcon,
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
                        "providedKey" to roomKeySan,
                        "userIcon" to userIcon,
                    )
                    transaction.set(memberRef, memberData)
                }
                null
            }.await()
        } catch (e: Exception) {
            println("[RoomRepository] joinRoom transaction failed: ${e.message}")
            throw JoinError.Unknown
        }

        // Remove providedKey after creation
        if (!existed) {
            memberRef.update("providedKey", FieldValue.delete()).await()
        }
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
        val newLower = usernameSan.lowercase()

        try {
            db.runTransaction { transaction ->
                // Username allows duplicates, no lock operation needed
                val updates = mapOf(
                    "username" to usernameSan,
                    "usernameLower" to newLower,
                )
                transaction.set(memberRef, updates, com.google.firebase.firestore.SetOptions.merge())
                null
            }.await()
        } catch (e: FirebaseFirestoreException) {
            println("[RoomRepository] changeUsername failed: ${e.message}")
            // If permission denied (code 7), it might be uniqueness constraint
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

    /**
     * Check if user is already a member of the room.
     */
    suspend fun isUserAlreadyInRoom(roomId: String, uid: String): Boolean {
        val roomIdSan = roomId.trim()
        val memberRef = memberRef(roomIdSan, uid)

        return try {
            // Use server source to avoid stale cache
            val snapshot = memberRef.get(Source.SERVER).await()
            snapshot.exists()
        } catch (e: Exception) {
            println("[RoomRepository] isUserAlreadyInRoom failed: ${e.message}")
            throw e
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
     * Fetch room member data (single shot, server-first).
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
     * Fetch member data.
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
            // Try cache first
            val cacheSnap = memberRef(roomId, uid).get(Source.CACHE).await()
            if (cacheSnap.exists()) return true

            // Fallback to server
            val serverSnap = memberRef(roomId, uid).get(Source.SERVER).await()
            serverSnap.exists()
        } catch (e: Exception) {
            println("[RoomRepository] isMember check failed: ${e.message}")
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