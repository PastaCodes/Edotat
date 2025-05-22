package at.e.lib

fun Number.euros() = Money.Amount((this.toDouble() * 100).toInt(), Money.Currency.EUR)

object Money {
    enum class Currency {
        USD {
            override fun toString(smallestUnitAmount: Int): String {
                val cents = smallestUnitAmount % 100
                val wholes = smallestUnitAmount / 100
                return "\$$wholes.${cents.toString().padStart(2, '0')}"
            }
        },
        EUR {
            override fun toString(smallestUnitAmount: Int): String {
                val cents = smallestUnitAmount % 100
                val wholes = smallestUnitAmount / 100
                return "$wholes,${cents.toString().padStart(2, '0')} €"
            }
        },
        ;
        abstract fun toString(smallestUnitAmount: Int): String
    }

    data class Amount(val smallestUnitAmount: Int, val currency: Currency) {
        override fun toString() = this.currency.toString(this.smallestUnitAmount)
    }
}
