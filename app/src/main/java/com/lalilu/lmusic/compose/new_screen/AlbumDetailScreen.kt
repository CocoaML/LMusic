package com.lalilu.lmusic.compose.new_screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lalilu.lmusic.compose.component.card.AlbumCoverCard
import com.lalilu.lmusic.compose.component.navigate.NavigatorHeader
import com.lalilu.lmusic.compose.new_screen.destinations.SongDetailScreenDestination
import com.lalilu.lmusic.utils.extension.dayNightTextColor
import com.lalilu.lmusic.viewmodel.LMediaViewModel
import com.lalilu.lmusic.viewmodel.PlayingViewModel
import com.lalilu.lmusic.viewmodel.SongsViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.androidx.compose.get

@OptIn(ExperimentalMaterialApi::class)
@Destination
@Composable
fun AlbumDetailScreen(
    albumId: String,
    playingVM: PlayingViewModel = get(),
    mediaVM: LMediaViewModel = get(),
    songsVM: SongsViewModel = get(),
    navigator: DestinationsNavigator
) {
    val album = mediaVM.requireAlbum(albumId) ?: run {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "[Error]加载失败 #$albumId")
        }
        return
    }

    val sortFor = remember { "ArtistDetail" }

    LaunchedEffect(album) {
        songsVM.updateBySongs(
            songs = album.songs,
            sortFor = sortFor
        )
    }

    val songsState by songsVM.songsState

    SortPanelWrapper(
        sortFor = sortFor,
        supportGroupRules = { songsVM.supportGroupRules },
        supportSortRules = { songsVM.supportSortRules },
        supportOrderRules = { songsVM.supportOrderRules }
    ) { showSortPanel ->
        SongListWrapper(
            songsState = songsState,
            hasLyricState = { playingVM.requireHasLyricState(item = it) },
            onLongClickItem = { navigator.navigate(SongDetailScreenDestination(mediaId = it.id)) },
            onClickItem = { playingVM.playSongWithPlaylist(songsState.values.flatten(), it) }
        ) {
            item {
                AlbumCoverCard(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(10.dp),
                    elevation = 2.dp,
                    imageData = { album },
                    onClick = { }
                )
            }

            item {
                NavigatorHeader(
                    title = album.name,
                    subTitle = "共 ${songsState.values.flatten().size} 首歌曲"
                ) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = dayNightTextColor(0.05f),
                        onClick = { showSortPanel.value = !showSortPanel.value }
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 15.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.subtitle2,
                            color = dayNightTextColor(0.7f),
                            text = "排序"
                        )
                    }
                }
            }
        }
    }
}