package at.e.api.faux

import at.e.api.Menu
import at.e.api.Order
import at.e.api.Suborder
import at.e.lib.replaceOne

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
    val items: MutableList<Order.Entry> = mutableListOf(),
) {
    // fun getItemQuantity(item: Menu.Item) = this.items[item] ?: 0

    fun incrementItemQuantity(item: Menu.Item): Int {
        val existingEntry = this.items.find { it.item == item }
        if (existingEntry != null) {
            val newEntry = Order.Entry(item, existingEntry.quantity + 1)
            this.items.replaceOne(existingEntry, newEntry)
            return newEntry.quantity
        } else {
            this.items.add(Order.Entry(item, 1))
            return 1
        }
    }

    fun decrementItemQuantity(item: Menu.Item): Int {
        val existingEntry = this.items.find { it.item == item }!!
        if (existingEntry.quantity == 1) {
            this.items.remove(existingEntry)
            return 0
        } else {
            val newEntry = Order.Entry(item, existingEntry.quantity - 1)
            this.items.replaceOne(existingEntry, newEntry)
            return newEntry.quantity
        }
    }
}
