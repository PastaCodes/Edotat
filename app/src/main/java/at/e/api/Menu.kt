package at.e.api

data class Menu(
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val restaurant: Restaurant,
)
