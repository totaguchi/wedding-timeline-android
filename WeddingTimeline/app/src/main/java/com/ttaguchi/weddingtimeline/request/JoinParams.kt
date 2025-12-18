package com.ttaguchi.weddingtimeline.request

/**
 * Parameters for joining a room.
 */
data class JoinParams(
    val roomId: String,
    val roomKey: String,
    val username: String,
    val selectedIcon: String,
)

/**
 * Errors that can occur during room join process.
 */
sealed class JoinError : Exception() {
    data object NotSignedIn : JoinError() {
        private fun readResolve(): Any = NotSignedIn
        override val message: String = "サインインに失敗しました。"
    }

    data object InvalidKey : JoinError() {
        private fun readResolve(): Any = InvalidKey
        override val message: String = "roomKey が正しくありません。"
    }

    data object UsernameTaken : JoinError() {
        private fun readResolve(): Any = UsernameTaken
        override val message: String = "このユーザー名は既に使われています。"
    }

    data object Banned : JoinError() {
        private fun readResolve(): Any = Banned
        override val message: String = "このルームへの参加は禁止されています。"
    }

    data object IconNotSelected : JoinError() {
        private fun readResolve(): Any = IconNotSelected
        override val message: String = "アイコンを指定してください。"
    }

    data object Unknown : JoinError() {
        private fun readResolve(): Any = Unknown
        override val message: String = "不明なエラーが発生しました。"
    }

    data class Message(override val message: String) : JoinError()
}
