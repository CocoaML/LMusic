package com.lalilu.lmusic.utils

import android.support.v4.media.session.PlaybackStateCompat.*

enum class PlayMode(
    val repeatMode: Int,
    val shuffleMode: Int,
    val value: Int
) {
    ListRecycle(repeatMode = REPEAT_MODE_ALL, shuffleMode = SHUFFLE_MODE_NONE, value = 0),
    RepeatOne(repeatMode = REPEAT_MODE_ONE, shuffleMode = SHUFFLE_MODE_NONE, value = 1),
    Shuffle(repeatMode = REPEAT_MODE_ALL, shuffleMode = SHUFFLE_MODE_ALL, value = 2);

    fun next(): PlayMode {
        return when (this) {
            ListRecycle -> RepeatOne
            RepeatOne -> Shuffle
            Shuffle -> ListRecycle
        }
    }

    companion object {
        const val KEY = "PLAY_MODE"

        fun of(value: Int): PlayMode = when (value) {
            1 -> RepeatOne
            2 -> Shuffle
            else -> ListRecycle
        }

        fun of(repeatMode: Int, shuffleMode: Int): PlayMode {
            if (repeatMode == REPEAT_MODE_ONE) return RepeatOne
            if (shuffleMode == SHUFFLE_MODE_ALL) return Shuffle
            return ListRecycle
        }
    }
}