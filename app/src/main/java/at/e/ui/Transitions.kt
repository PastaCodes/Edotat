package at.e.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import at.e.Navigation
import at.e.lib.Direction
import kotlinx.coroutines.flow.StateFlow

object Transitions {
    val EnterRightToLeft = slideInHorizontally { it } + fadeIn()
    val ExitRightToLeft = slideOutHorizontally { -it } + fadeOut()
    val EnterLeftToRight = slideInHorizontally { -it } + fadeIn()
    val ExitLeftToRight = slideOutHorizontally { it } + fadeOut()

    inline fun <reified T : Navigation.Destination> NavGraphBuilder.slidingComposable(
        forcedDirection: StateFlow<Direction.Horizontal?>,
        noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
    ) {
        composable<T>(
            enterTransition = {
                when (forcedDirection.value) {
                    null, Direction.RightToLeft -> EnterRightToLeft
                    Direction.LeftToRight -> EnterLeftToRight
                }
            },
            exitTransition = {
                when (forcedDirection.value) {
                    null, Direction.RightToLeft -> ExitRightToLeft
                    Direction.LeftToRight -> ExitLeftToRight
                }
            },
            popEnterTransition = {
                when (forcedDirection.value) {
                    null, Direction.LeftToRight -> EnterLeftToRight
                    Direction.RightToLeft -> EnterRightToLeft
                }
            },
            popExitTransition = { ExitLeftToRight },
            content = content,
        )
    }
}
