package at.e.api

import kotlinx.datetime.LocalDateTime

data class Order(val menu: Menu, val table: Table, val account: Account) {
    data class Entry(val item: Menu.Item, val quantity: Int)
}

data class Suborder(val order: Order, val started: LocalDateTime, var sent: LocalDateTime? = null) {
    override fun equals(other: Any?): Boolean {
        return other === this
    }

    override fun hashCode() = javaClass.hashCode()
}
