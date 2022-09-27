package com.lalilu.lmusic.viewmodel

import android.text.TextUtils
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.blankj.utilcode.util.ToastUtils
import com.lalilu.lmusic.apis.NeteaseDataSource
import com.lalilu.lmusic.apis.bean.netease.SongSearchSong
import com.lalilu.lmusic.datasource.MDataBase
import com.lalilu.lmusic.datasource.entity.MNetworkData
import com.lalilu.lmusic.datasource.entity.MNetworkDataUpdateForCoverUrl
import com.lalilu.lmusic.datasource.entity.MNetworkDataUpdateForLyric
import com.lalilu.lmusic.service.LMusicLyricManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NetworkDataViewModel @Inject constructor(
    private val neteaseDataSource: NeteaseDataSource,
    private val dataBase: MDataBase
) : ViewModel() {
    fun getNetworkDataFlowByMediaId(mediaId: String) =
        dataBase.networkDataDao().getFlowById(mediaId)
            .distinctUntilChanged()

    suspend fun getNetworkDataByMediaId(mediaId: String): MNetworkData? =
        withContext(Dispatchers.IO) {
            return@withContext dataBase.networkDataDao().getById(mediaId)
        }

    fun getSongResult(
        keyword: String,
        items: SnapshotStateList<SongSearchSong>,
        msg: MutableState<String>
    ) = viewModelScope.launch(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            items.clear()
            msg.value = "搜索中..."
        }
        flow {
            val response = neteaseDataSource.searchForSong(keyword)
            val results = response?.result?.songs ?: emptyList()
            emit(results)
        }.onEach {
            withContext(Dispatchers.Main) {
                msg.value = if (it.isEmpty()) "无结果" else ""
                items.addAll(it)
            }
        }.catch {
            withContext(Dispatchers.Main) {
                msg.value = "搜索失败"
                items.clear()
            }
        }.launchIn(this)
    }

    fun saveMatchNetworkData(
        mediaId: String,
        songId: Long?,
        title: String?,
        success: () -> Unit = {}
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (songId == null || title == null) {
            ToastUtils.showShort("未选择匹配歌曲")
            return@launch
        }
        try {
            dataBase.networkDataDao().save(
                MNetworkData(
                    mediaId = mediaId,
                    songId = songId.toString(),
                    title = title
                )
            )
            ToastUtils.showShort("保存匹配信息成功")
            withContext(Dispatchers.Main) {
                success()
            }
        } catch (e: Exception) {
            ToastUtils.showShort("保存匹配信息失败")
        }
    }

    fun saveCoverUrlIntoNetworkData(
        songId: String?,
        mediaId: String,
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (songId == null) {
            ToastUtils.showShort("未选择匹配歌曲")
            return@launch
        }
        try {
            neteaseDataSource.searchForDetail(songId)?.let {
                dataBase.networkDataDao().updateCoverUrl(
                    MNetworkDataUpdateForCoverUrl(
                        mediaId = mediaId,
                        cover = it.songs[0].al.picUrl
                    )
                )
                ToastUtils.showShort("保存封面地址成功")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ToastUtils.showShort("保存封面地址失败")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun saveLyricIntoNetworkData(
        songId: String?,
        mediaId: String,
        success: () -> Unit = {}
    ) = viewModelScope.launch(Dispatchers.IO) {
        flow {
            if (songId != null) emit(songId)
            else ToastUtils.showShort("未选择匹配歌曲")
        }.mapLatest {
            ToastUtils.showShort("开始获取歌词")
            neteaseDataSource.searchForLyric(it)
        }.mapLatest {
            val lyric = it?.mainLyric
            if (TextUtils.isEmpty(lyric)) {
                ToastUtils.showShort("选中歌曲无歌词")
                return@mapLatest null
            }
            Pair(lyric!!, it.translateLyric)
        }.onEach {
            it ?: return@onEach
            dataBase.networkDataDao().updateLyric(
                MNetworkDataUpdateForLyric(
                    mediaId = mediaId,
                    lyric = it.first,
                    tlyric = it.second
                )
            )
            ToastUtils.showShort("保存匹配歌词成功")
            LMusicLyricManager.currentLyric.requireUpdate()
            withContext(Dispatchers.Main) {
                success()
            }
        }.catch {
            ToastUtils.showShort("保存失败")
        }.launchIn(this)
    }
}