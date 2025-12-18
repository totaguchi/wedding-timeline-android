package com.ttaguchi.weddingtimeline.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ttaguchi.weddingtimeline.data.RoomRepository
import com.ttaguchi.weddingtimeline.request.JoinError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for LoginScreen.
 */
class LoginViewModel(
    private val repository: RoomRepository = RoomRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Available avatar icons
    val icons = listOf(
        "oomimigitsune",
        "lesser_panda",
        "bear",
        "todo",
        "musasabi",
        "rakko"
    )

    fun updateRoomId(value: String) {
        _uiState.update { it.copy(roomId = value, errorMessage = null) }
    }

    fun updateRoomKey(value: String) {
        _uiState.update { it.copy(roomKey = value, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun selectIcon(icon: String) {
        _uiState.update { it.copy(selectedIcon = icon, errorMessage = null) }
    }

    fun join(onSuccess: (String) -> Unit) {
        // 多重実行ガード
        if (_uiState.value.isLoading) return

        val state = _uiState.value
        if (state.roomId.isBlank() ||
            state.roomKey.isBlank() ||
            state.username.isBlank() ||
            state.selectedIcon == null) {
            _uiState.update { it.copy(errorMessage = "すべての項目を入力してください") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                repository.joinRoom(
                    roomId = state.roomId,
                    roomKey = state.roomKey,
                    username = state.username,
                    userIcon = state.selectedIcon
                )
                // 成功時のみonSuccessを呼ぶ
                _uiState.update { it.copy(isLoading = false) }
                onSuccess(state.roomId)
            } catch (e: JoinError) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "入室に失敗しました"
                    )
                }
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
