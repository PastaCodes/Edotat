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

object Transitions {
    val InForward = slideInHorizontally { it } + fadeIn()
    val OutForward = slideOutHorizontally { -it } + fadeOut()
    val InBackward = slideInHorizontally { -it } + fadeIn()
    val OutBackward = slideOutHorizontally { it } + fadeOut()
}

inline fun <reified T : Any> NavGraphBuilder.slidingComposable(
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) {
    composable<T>(
        enterTransition = { Transitions.InForward },
        exitTransition = { Transitions.OutForward },
        popEnterTransition = { Transitions.InBackward },
        popExitTransition = { Transitions.OutBackward },
        content = content,
    )
}
