package com.ttaguchi.weddingtimeline.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.RoomRepository
import com.ttaguchi.weddingtimeline.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class SettingsUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

class SettingsViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val roomRepository: RoomRepository = RoomRepository(),
    private val userRepository: UserRepository = UserRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Sign out from the current room and Firebase Auth.
     */
    fun signOut(context: Context, roomId: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                // 1) Leave room if joined
                if (roomId != null) {
                    try {
                        roomRepository.leaveRoom(roomId)
                        println("[SettingsViewModel] Left room: $roomId")
                    } catch (e: Exception) {
                        println("[SettingsViewModel] leaveRoom error: ${e.message}")
                        // Continue with sign out even if leave room fails
                    }
                }

                // 2) Sign out from Firebase Auth
                try {
                    auth.signOut()
                    println("[SettingsViewModel] Signed out from Firebase Auth")
                } catch (e: Exception) {
                    println("[SettingsViewModel] Auth signOut error: ${e.message}")
                }

                // 3) Clear local session data
                val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("isLoggedIn", false)
                    .putString("lastRoomId", null)
                    .apply()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "ログアウトしました"
                )
                onSuccess()
            } catch (e: Exception) {
                println("[SettingsViewModel] signOut error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "ログアウトに失敗しました"
                )
            }
        }
    }

    /**
     * Delete user account and all associated data.
     */
    fun deleteAccount(context: Context, roomId: String?, onSuccess: () -> Unit) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "ログイン情報が見つかりません")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            try {
                // 1) Delete room-related data if in a room
                if (roomId != null) {
                    try {
                        roomRepository.deleteMyAccount(roomId)
                        println("[SettingsViewModel] Deleted account data from room: $roomId")
                    } catch (e: Exception) {
                        println("[SettingsViewModel] deleteMyAccount error: ${e.message}")
                        // Continue with account deletion even if room data deletion fails
                    }
                }

                // 2) Delete user document from /users/{uid}
                try {
                    userRepository.deleteUser(uid)
                    println("[SettingsViewModel] Deleted user document: $uid")
                } catch (e: Exception) {
                    println("[SettingsViewModel] deleteUser error: ${e.message}")
                }

                // 3) Delete Firebase Auth account
                try {
                    auth.currentUser?.delete()?.await()
                    println("[SettingsViewModel] Deleted Firebase Auth account")
                } catch (e: Exception) {
                    println("[SettingsViewModel] Auth delete error: ${e.message}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "アカウント削除に失敗しました"
                    )
                    return@launch
                }

                // 4) Sign out and clear local data
                auth.signOut()
                val prefs = context.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putBoolean("isLoggedIn", false)
                    .putString("lastRoomId", null)
                    .apply()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    successMessage = "アカウントを削除しました"
                )
                onSuccess()
            } catch (e: Exception) {
                println("[SettingsViewModel] deleteAccount error: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "削除に失敗しました"
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}