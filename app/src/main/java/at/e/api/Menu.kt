package at.e.api

data class Menu(
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val restaurant: Restaurant,
) {
    data class Category(val name: String, val menu: Menu)

    data class Item(val name: String, val category: Category)
}
