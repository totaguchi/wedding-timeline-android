package com.ttaguchi.weddingtimeline.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.ttaguchi.weddingtimeline.data.RoomRepository
import com.ttaguchi.weddingtimeline.data.UserRepository
import com.ttaguchi.weddingtimeline.domain.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing user session state.
 * Persists roomId locally so the timeline can be initialized after login.
 */
class SessionViewModel(
    application: Application,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val userRepository: UserRepository = UserRepository(),
    private val roomRepository: RoomRepository = RoomRepository(),
) : AndroidViewModel(application) {

    // Required for default AndroidViewModelFactory (used by viewModels()).
    constructor(application: Application) : this(
        application,
        FirebaseAuth.getInstance(),
        UserRepository(),
        RoomRepository(),
    )

    private val prefs by lazy {
        application.getSharedPreferences("session_prefs", Context.MODE_PRIVATE)
    }

    private val _session = MutableStateFlow(Session.empty)
    val session: StateFlow<Session> = _session.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                loadUserSession(currentUser.uid)
            } else {
                _session.value = Session.empty
            }
        }
    }

    private fun loadUserSession(uid: String) {
        viewModelScope.launch {
            val user = userRepository.fetchUser(uid)
            val roomId = loadRoomIdFromPreferences()
            val member = if (roomId != null) {
                roomRepository.fetchMember(roomId, uid)
            } else {
                null
            }
            _session.value = Session(
                uid = uid,
                user = user,
                roomId = roomId,
                member = member,
            )
        }
    }

    /**
     * Call this after a successful join to persist and expose the room.
     */
    fun onJoinedRoom(roomId: String) {
        val uid = auth.currentUser?.uid ?: return
        saveRoomId(roomId)
        viewModelScope.launch {
            val user = userRepository.fetchUser(uid)
            val member = roomRepository.fetchMember(roomId, uid)
            _session.value = Session(
                uid = uid,
                user = user,
                roomId = roomId,
                member = member,
            )
        }
    }

    /**
     * Sign out the current user.
     */
    fun signOut() {
        auth.signOut()
        clearRoomId()
        _session.value = Session.empty
    }

    /**
     * Refresh session data.
     */
    fun refresh() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadUserSession(currentUser.uid)
        }
    }

    private fun saveRoomId(roomId: String) {
        prefs.edit().putString(KEY_ROOM_ID, roomId).apply()
    }

    private fun clearRoomId() {
        prefs.edit().remove(KEY_ROOM_ID).apply()
    }

    private fun loadRoomIdFromPreferences(): String? {
        return prefs.getString(KEY_ROOM_ID, null)
    }

    companion object {
        private const val KEY_ROOM_ID = "room_id"
    }
}
