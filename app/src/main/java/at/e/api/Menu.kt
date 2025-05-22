package at.e.api

import at.e.lib.Money

data class Menu(
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val restaurant: Restaurant,
) {
    data class Category(val name: String, val menu: Menu)

    data class Item(val name: String, val description: String?, val price: Money.Amount, val category: Category)
}
