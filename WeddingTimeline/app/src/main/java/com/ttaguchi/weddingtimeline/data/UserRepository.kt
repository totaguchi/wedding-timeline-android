package com.ttaguchi.weddingtimeline.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.ttaguchi.weddingtimeline.domain.model.AppUser
import com.ttaguchi.weddingtimeline.domain.model.AppUserDto
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for fetching and mutating user documents.
 */
class UserRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {

    private fun userRef(uid: String): DocumentReference =
        db.collection("users").document(uid)

    /**
     * Fetch user data from Firestore.
     */
    suspend fun fetchUser(uid: String, source: Source = Source.DEFAULT): AppUser? {
        return try {
            val snap = userRef(uid).get(source).await()
            toAppUser(snap)
        } catch (e: Exception) {
            println("[UserRepository] fetchUser failed uid=$uid: $e")
            null
        }
    }

    /**
     * Listen to user data changes in real-time.
     */
    fun listenUser(uid: String): Flow<AppUser?> = callbackFlow {
        val listener = userRef(uid).addSnapshotListener { snap, err ->
            if (err != null) {
                println("[UserRepository] listenUser error: $err")
                trySend(null)
                return@addSnapshotListener
            }
            trySend(toAppUser(snap))
        }
        awaitClose { listener.remove() }
    }

    /**
     * Create a new user document.
     */
    suspend fun createUser(
        uid: String,
        name: String,
        icon: String,
    ) {
        val nameSan = name.trim()
        val iconSan = icon.trim()

        require(nameSan.isNotEmpty()) { "名前を入力してください" }
        require(iconSan.isNotEmpty()) { "アイコンを選択してください" }

        val payload = mapOf(
            "name" to nameSan,
            "icon" to iconSan,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        userRef(uid).set(payload).await()
    }

    /**
     * Update user name.
     */
    suspend fun updateUserName(uid: String, name: String) {
        val nameSan = name.trim()
        require(nameSan.isNotEmpty()) { "名前を入力してください" }

        userRef(uid).update("name", nameSan).await()
    }

    /**
     * Update user icon.
     */
    suspend fun updateUserIcon(uid: String, icon: String) {
        val iconSan = icon.trim()
        require(iconSan.isNotEmpty()) { "アイコンを選択してください" }

        userRef(uid).update("icon", iconSan).await()
    }

    /**
     * Delete user document.
     */
    suspend fun deleteUser(uid: String) {
        userRef(uid).delete().await()
    }

    /**
     * Check if user document exists.
     */
    suspend fun userExists(uid: String): Boolean {
        return try {
            val snap = userRef(uid).get(Source.CACHE).await()
            if (snap.exists()) return true

            val serverSnap = userRef(uid).get(Source.SERVER).await()
            serverSnap.exists()
        } catch (e: Exception) {
            println("[UserRepository] userExists check failed: $e")
            false
        }
    }

    private fun toAppUser(snap: DocumentSnapshot?): AppUser? {
        if (snap == null || !snap.exists()) return null

        return try {
            val dto = snap.toObject(AppUserDto::class.java) ?: return null
            AppUser(
                id = snap.id,
                name = dto.name,
                icon = dto.icon,
                createdAt = dto.createdAt,
            )
        } catch (e: Exception) {
            println("[UserRepository] toAppUser decode failed id=${snap.id}: $e")
            null
        }
    }
}