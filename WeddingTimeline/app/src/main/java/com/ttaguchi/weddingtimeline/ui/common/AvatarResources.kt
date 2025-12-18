package com.ttaguchi.weddingtimeline.ui.common

import com.ttaguchi.weddingtimeline.R

/**
 * Map avatar icon names to drawable resources.
 */
fun avatarResId(name: String?): Int? {
    return when (name) {
        "oomimigitsune" -> R.drawable.oomimigitsune
        "lesser_panda" -> R.drawable.lesser_panda
        "bear" -> R.drawable.bear
        "todo" -> R.drawable.todo
        "musasabi" -> R.drawable.musasabi
        "rakko" -> R.drawable.rakko
        else -> null
    }
}

fun resolveAvatarResId(name: String?): Int = avatarResId(name) ?: R.drawable.lesser_panda
