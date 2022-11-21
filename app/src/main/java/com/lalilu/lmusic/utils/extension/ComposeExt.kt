package com.lalilu.lmusic.utils.extension

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.*
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.blankj.utilcode.util.TimeUtils
import com.lalilu.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

fun NavController.canPopUp(): Boolean {
    return previousBackStackEntry != null
}

fun NavController.popUpElse(elseDo: () -> Unit) {
    if (canPopUp()) this.navigateUp() else elseDo()
}

/**
 * 递归清除返回栈
 */
fun NavController.clearBackStack() {
    if (popBackStack()) clearBackStack()
}

/**
 * @param to    目标导航位置
 *
 * 指定导航起点位置和目标位置
 */
fun NavController.navigateSingleTop(
    to: String,
) {
    navigate(to) {
        popUpTo(graph.findStartDestination().id) {
            inclusive = false
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun rememberStatusBarHeight(): Float {
    val density = LocalDensity.current
    val statusBarHeightDp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return remember(statusBarHeightDp, density) {
        density.run { statusBarHeightDp.toPx() }
    }
}

@Composable
fun rememberScreenHeight(): Dp {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp + WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding() + WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()
    return remember {
        screenHeightDp
    }
}

@Composable
fun rememberScreenHeightInPx(): Int {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp.dp + WindowInsets.statusBars.asPaddingValues()
        .calculateTopPadding() + WindowInsets.navigationBars.asPaddingValues()
        .calculateBottomPadding()
    return remember(screenHeightDp, configuration) {
        (screenHeightDp.value * configuration.densityDpi / 160f).roundToInt()
    }
}

@OptIn(ExperimentalMaterialApi::class)
fun SwipeProgress<ModalBottomSheetValue>.watchForOffset(
    betweenFirst: ModalBottomSheetValue, betweenSecond: ModalBottomSheetValue, elseValue: Float = 1f
): Float = when {
    from == betweenFirst && to == betweenSecond -> 1f - (fraction * 3)
    from == betweenSecond && to == betweenFirst -> fraction * 3
    else -> elseValue
}.coerceIn(0f, 1f)

/**
 * 根据屏幕的长宽类型来判断设备是否平板
 * 依据是：平板没有一条边会是Compact的
 */
@Composable
fun WindowSizeClass.rememberIsPad(): State<Boolean> {
    return remember(widthSizeClass, heightSizeClass) {
        derivedStateOf {
            widthSizeClass != WindowWidthSizeClass.Compact && heightSizeClass != WindowHeightSizeClass.Compact
        }
    }
}

@Composable
fun dayNightTextColor(alpha: Float = 1f): Color {
    val color = contentColorFor(backgroundColor = MaterialTheme.colors.background)
    return remember(color) { color.copy(alpha = alpha) }
}

@Composable
fun dayNightTextColorFilter(alpha: Float = 1f): ColorFilter {
    val color = contentColorFor(backgroundColor = MaterialTheme.colors.background)
    return remember(color) { color.copy(alpha = alpha).toColorFilter() }
}

fun Color.toColorFilter(): ColorFilter {
    return ColorFilter.tint(color = this)
}

@Composable
fun durationMsToString(duration: Long): String {
    return remember(duration) { TimeUtils.millis2String(duration, "mm:ss") }
}

@DrawableRes
@Composable
fun mimeTypeToIcon(mimeType: String): Int {
    return remember(mimeType) {
        val strings = mimeType.split("/").toTypedArray()
        when (strings[strings.size - 1].uppercase()) {
            "FLAC" -> R.drawable.ic_flac_line
            "MPEG", "MP3" -> R.drawable.ic_mp3_line
            "MP4" -> R.drawable.ic_mp4_line
            "APE" -> R.drawable.ic_ape_line
            "DSD", "DSF" -> R.drawable.ic_dsd_line
            "WAV", "X-WAV" -> R.drawable.ic_wav_line
            else -> R.drawable.ic_mp3_line
        }
    }
}

@Composable
fun <T> buildScrollToItemAction(
    target: T,
    getIndex: (T) -> Int,
    state: LazyGridState = rememberLazyGridState(),
    scope: CoroutineScope = rememberCoroutineScope()
): () -> Unit {
    // 获取当前可见元素的平均高度
    fun getHeightAverage() = state.layoutInfo.visibleItemsInfo.average { it.size.height }

    // 获取精确的位移量（只能对可见元素获取）
    fun getTargetOffset(index: Int) =
        state.layoutInfo.visibleItemsInfo.find { it.index == index }?.offset?.y

    // 获取粗略的位移量（通过对可见元素的高度求平均再通过index的差，计算出粗略值）
    fun getRoughTargetOffset(index: Int) =
        getHeightAverage() * (index - state.firstVisibleItemIndex - 1)

    return {
        scope.launch {
            val index = getIndex(target)
            if (index >= 0) {
                // 若获取不到精确的位移量，则计算粗略位移量并开始scroll
                if (getTargetOffset(index) == null) {
                    state.animateScrollBy(
                        getRoughTargetOffset(index), SpringSpec(stiffness = Spring.StiffnessVeryLow)
                    )
                }

                // 若可以获取到精确的位移量，则直接滚动到目标歌曲位置
                getTargetOffset(index)?.let {
                    state.animateScrollBy(
                        it.toFloat(), SpringSpec(stiffness = Spring.StiffnessVeryLow)
                    )
                }
            }
        }
    }
}