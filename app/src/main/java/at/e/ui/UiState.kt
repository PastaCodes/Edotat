package at.e.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

sealed interface UiState<in T> {
    companion object {
        @Composable
        fun <T> remember() =
            remember { mutableStateOf<UiState<T>>(Loading) }
    }

    data object Loading : UiState<Any?>
    data class Data<T>(val data: T) : UiState<T>
}
