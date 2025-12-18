package com.ttaguchi.weddingtimeline.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.RoomRepository
import com.ttaguchi.weddingtimeline.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val repository: RoomRepository = RoomRepository()
) : ViewModel() {

    companion object {
        private const val PREFS_NAME = "session_prefs"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_ROOM_ID = "lastRoomId"
    }

    private val _session = MutableStateFlow(Session.empty)
    val session: StateFlow<Session> = _session.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * アプリ起動時に呼び出して、Auth とキャッシュ、会員状態を整合させる
     * Swift の bootstrapOnLaunch() に相当
     */
    fun bootstrapOnLaunch(context: Context) {
        viewModelScope.launch {
            try {
                // 1) Auth を確保（匿名でサインイン）
                val uid = ensureSignedInUID()
                
                // 2) SharedPreferences から前回のログイン状態を読み込む
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val wasLoggedIn = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
                val lastRoomId = prefs.getString(KEY_ROOM_ID, null)
                
                println("[SessionViewModel] Bootstrap: uid=$uid, wasLoggedIn=$wasLoggedIn, lastRoomId=$lastRoomId")
                
                if (wasLoggedIn && !lastRoomId.isNullOrEmpty()) {
                    // 3) 前回ログイン済みの場合、会員状態を検証
                    validateCurrentMembership(uid, lastRoomId)
                } else {
                    // ログアウト状態
                    _session.value = Session.empty
                    _isInitialized.value = true
                }
            } catch (e: Exception) {
                println("[SessionViewModel] Bootstrap error: ${e.message}")
                e.printStackTrace()
                _session.value = Session.empty
                _isInitialized.value = true
            }
        }
    }

    /**
     * Firestore の members に自分の doc が存在するかを検証
     * Swift の validateCurrentMembership() に相当
     */
    private suspend fun validateCurrentMembership(uid: String, roomId: String) {
        try {
            // 会員情報を取得
            val isInRoom = repository.isUserAlreadyInRoom(roomId, uid)
            
            if (isInRoom) {
                // 会員ならセッション情報を最新化
                val member = repository.fetchRoomMember(roomId, uid)
                val user = try {
                    repository.fetchUser(uid)
                } catch (e: Exception) {
                    null
                }
                
                _session.value = Session(
                    uid = uid,
                    user = user,
                    roomId = roomId,
                    member = member
                )
                println("[SessionViewModel] Validation success: member found")
            } else {
                // 会員でない（退室済みなど）
                println("[SessionViewModel] Validation: member not found → ログアウト状態へ")
                _session.value = Session.empty
            }
        } catch (e: Exception) {
            println("[SessionViewModel] Validation error: ${e.message}")
            // 通信失敗などは致命ではないので、キャッシュは温存
            // ただし初期化は完了とする
        } finally {
            _isInitialized.value = true
        }
    }

    /**
     * Auth にサインイン済みであることを確保（なければ匿名サインイン）
     */
    private suspend fun ensureSignedInUID(): String {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            return currentUser.uid
        }
        
        // 匿名サインイン
        return auth.signInAnonymously().await().user?.uid
            ?: throw IllegalStateException("Failed to sign in anonymously")
    }

    /**
     * ルーム入室成功時に呼び出す
     * Swift の signIn() に相当
     */
    fun onJoinedRoom(context: Context, roomId: String) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid ?: return@launch
                println("[SessionViewModel] onJoinedRoom: roomId=$roomId, userId=$uid")

                // SharedPreferences に保存
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .putString(KEY_ROOM_ID, roomId)
                    .apply()

                // 会員情報を取得してセッションを更新
                val member = repository.fetchRoomMember(roomId, uid)
                val user = try {
                    repository.fetchUser(uid)
                } catch (e: Exception) {
                    null
                }

                _session.value = Session(
                    uid = uid,
                    user = user,
                    roomId = roomId,
                    member = member
                )
            } catch (e: Exception) {
                println("[SessionViewModel] onJoinedRoom error: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * サインアウト
     * Swift の signOut() に相当
     */
    fun signOut(context: Context) {
        viewModelScope.launch {
            try {
                val roomId = _session.value.roomId
                
                // 1) Firestore から退室
                if (roomId != null) {
                    try {
                        repository.leaveRoom(roomId)
                    } catch (e: Exception) {
                        println("[SessionViewModel] leaveRoom error: ${e.message}")
                    }
                }

                // 2) Firebase Auth からサインアウト
                try {
                    auth.signOut()
                } catch (e: Exception) {
                    println("[SessionViewModel] Auth signOut error: ${e.message}")
                }

                // 3) ローカル状態をクリア
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean(KEY_IS_LOGGED_IN, false)
                    .putString(KEY_ROOM_ID, null)
                    .apply()

                _session.value = Session.empty
                _isInitialized.value = false
            } catch (e: Exception) {
                println("[SessionViewModel] signOut error: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
