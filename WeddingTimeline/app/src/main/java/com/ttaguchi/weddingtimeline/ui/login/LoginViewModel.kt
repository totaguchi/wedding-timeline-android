package com.ttaguchi.weddingtimeline.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.RoomRepository
import com.ttaguchi.weddingtimeline.request.JoinError
import com.ttaguchi.weddingtimeline.request.JoinParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for LoginScreen.
 */
class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val roomRepository: RoomRepository = RoomRepository(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Available avatar icons
    val icons = listOf(
        "avatar_1", "avatar_2", "avatar_3", "avatar_4",
        "avatar_5", "avatar_6", "avatar_7", "avatar_8",
        "avatar_9", "avatar_10", "avatar_11", "avatar_12"
    )

    fun updateRoomId(value: String) {
        _uiState.value = _uiState.value.copy(roomId = value, errorMessage = null)
    }

    fun updateRoomKey(value: String) {
        _uiState.value = _uiState.value.copy(roomKey = value, errorMessage = null)
    }

    fun updateUsername(value: String) {
        _uiState.value = _uiState.value.copy(username = value, errorMessage = null)
    }

    fun selectIcon(icon: String) {
        _uiState.value = _uiState.value.copy(selectedIcon = icon)
    }

    fun join(onSuccess: (String) -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        // Validation
        if (state.roomId.isBlank()) {
            _uiState.value = state.copy(errorMessage = "ルームIDを入力してください")
            return
        }
        if (state.roomKey.isBlank()) {
            _uiState.value = state.copy(errorMessage = "ルームキーを入力してください")
            return
        }
        if (state.username.isBlank()) {
            _uiState.value = state.copy(errorMessage = "ユーザー名を入力してください")
            return
        }
        if (state.selectedIcon == null) {
            _uiState.value = state.copy(errorMessage = "アイコンを選択してください")
            return
        }

        _uiState.value = state.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val params = JoinParams(
                    roomId = state.roomId.trim(),
                    roomKey = state.roomKey.trim(),
                    username = state.username.trim(),
                    selectedIcon = state.selectedIcon,
                )
                
                roomRepository.joinRoom(params)

                // Success
                _uiState.value = state.copy(isLoading = false)
                onSuccess(params.roomId)

            } catch (e: JoinError) {
                val errorMsg = when (e) {
                    is JoinError.NotSignedIn -> e.message
                    is JoinError.InvalidKey -> e.message
                    is JoinError.UsernameTaken -> e.message
                    is JoinError.Banned -> e.message
                    is JoinError.IconNotSelected -> e.message
                    is JoinError.Unknown -> e.message
                }
                _uiState.value = state.copy(isLoading = false, errorMessage = errorMsg)
            } catch (e: Exception) {
                _uiState.value = state.copy(
                    isLoading = false,
                    errorMessage = "入室に失敗しました: ${e.message}"
                )
            }
        }
    }
}

/**
 * UI state for LoginScreen.
 */
data class LoginUiState(
    val roomId: String = "",
    val roomKey: String = "",
    val username: String = "",
    val selectedIcon: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
