package at.e.lib

sealed interface Direction<T : Direction<T>> {
    sealed interface Horizontal : Direction<Horizontal>

    val inverse: T

    data object LeftToRight : Horizontal {
        override val inverse = RightToLeft
    }
    data object RightToLeft : Horizontal {
        override val inverse = LeftToRight
    }
}
