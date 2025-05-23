package at.e.lib

import kotlinx.coroutines.flow.MutableStateFlow

sealed interface LoadingState<out T> {
    companion object {
        fun <T> flow() = MutableStateFlow<LoadingState<T>>(Loading)
    }

    data object Loading : LoadingState<Nothing> {
        override fun isData() = false
    }

    data class Data<T>(val data: T) : LoadingState<T> {
        override fun isData() = true
    }

    fun ifData(action: (T) -> Unit) {
        if (this is Data) {
            action(this.data)
        }
    }

    fun ifLoading(action: () -> Unit) {
        if (this is Loading) {
            action()
        }
    }

    fun isData(): Boolean

    fun isLoading() = !this.isData()

    val forceData
        get() = (this as Data).data

    fun <R> mapData(transform: (T) -> R) =
        when (this) {
            is Loading -> null
            is Data -> transform(this.data)
        }
}

inline fun <T> LoadingState<T>.dataOrElse(fallback: () -> T) =
    when (this) {
        is LoadingState.Loading -> fallback()
        is LoadingState.Data -> this.data
    }
