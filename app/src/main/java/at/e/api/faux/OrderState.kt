package at.e.api.faux

import at.e.api.Menu
import at.e.api.Order
import at.e.api.Suborder

data class OrderState(
    val order: Order,
    var activeSuborder: SuborderState? = null,
    val suborderHistory: MutableList<SuborderState> = mutableListOf(),
) {
    fun toHistory(): OrderHistory {
        assert(this.activeSuborder == null)
        return OrderHistory(this.order, this.suborderHistory)
    }
}

data class SuborderState(
    val suborder: Suborder,
    val items: MutableMap<Menu.Item, Int> = mutableMapOf(),
) {
    fun getItemQuantity(item: Menu.Item) = this.items[item] ?: 0

    fun incrementItemQuantity(item: Menu.Item): Int {
        val prevQuantity = this.getItemQuantity(item)
        return (prevQuantity + 1).also { this.items[item] = it }
    }

    fun decrementItemQuantity(item: Menu.Item): Int {
        val prevQuantity = this.getItemQuantity(item)
        val newQuantity = prevQuantity - 1
        if (newQuantity < 1) {
            this.items.remove(item)
            return 0
        }
        this.items[item] = newQuantity
        return newQuantity
    }
}
