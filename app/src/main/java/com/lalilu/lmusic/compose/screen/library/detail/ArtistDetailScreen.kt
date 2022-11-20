package com.lalilu.lmusic.compose.screen.library.detail

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.lalilu.lmedia.entity.LArtist
import com.lalilu.lmedia.entity.LSong
import com.lalilu.lmusic.compose.component.SmartContainer
import com.lalilu.lmusic.compose.component.card.SongCard
import com.lalilu.lmusic.compose.screen.ScreenActions
import com.lalilu.lmusic.utils.extension.LocalWindowSize
import com.lalilu.lmusic.viewmodel.LocalPlayingVM
import com.lalilu.lmusic.viewmodel.PlayingViewModel

@Composable
fun ArtistDetailScreen(
    artist: LArtist,
    playingVM: PlayingViewModel = LocalPlayingVM.current
) {
    val songs = artist.songs
    val windowSize = LocalWindowSize.current
    val navToSongAction = ScreenActions.navToSongById(hapticType = HapticFeedbackType.LongPress)

    val onSongSelected: (LSong) -> Unit = { song ->
        playingVM.playSongWithPlaylist(songs, song)
    }

    SmartContainer.LazyVerticalGrid(
        columns = GridCells.Fixed(if (windowSize.widthSizeClass == WindowWidthSizeClass.Expanded) 2 else 1),
    ) {
//        item {
//            NavigatorHeaderWithButtons(
//                title = artist.name,
//                subTitle = "${artist.getSongCount()} 首歌曲"
//            ) {
//                LazyListSortToggleButton(sortByState = sortByState) {
//                    sortByState = next(sortByState)
//                }
//                SortToggleButton(sortDesc = sortDesc) {
//                    sortDesc = !sortDesc
//                }
//            }
//        }

        itemsIndexed(items = songs) { index, item ->
            SongCard(
                song = { item },
                lyricRepository = playingVM.lyricRepository,
                onClick = { onSongSelected(item) },
                onLongClick = { navToSongAction(item.id) }
            )
//            SongCard(
//                modifier = Modifier.animateItemPlacement(),
//                index = index,
//                getSong = { item },
//                onItemClick = onSongSelected,
//                onItemLongClick = { navToSongAction(it.id) }
//            )
        }
    }
}

@Composable
fun EmptyArtistDetailScreen() {

}
