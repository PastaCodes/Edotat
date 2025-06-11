package at.e.lib

fun Number.euros() = Money.Amount((this.toDouble() * 100).toInt(), Money.Currency.EUR)

object Money {
    enum class Currency {
        USD {
            override fun toString(smallestUnitAmount: Int): String {
                return "\$ ${toDigits(smallestUnitAmount)}"
            }

            override fun toDigits(smallestUnitAmount: Int): String {
                val cents = smallestUnitAmount % 100
                val wholes = smallestUnitAmount / 100
                return "$wholes.${cents.toString().padStart(2, '0')}"
            }

            override val code = "USD"
        },
        EUR {
            override fun toString(smallestUnitAmount: Int): String {
                val cents = smallestUnitAmount % 100
                val wholes = smallestUnitAmount / 100
                return "$wholes,${cents.toString().padStart(2, '0')} €"
            }

            override fun toDigits(smallestUnitAmount: Int): String {
                val cents = smallestUnitAmount % 100
                val wholes = smallestUnitAmount / 100
                return "$wholes.${cents.toString().padStart(2, '0')}"
            }

            override val code = "EUR"
        },
        ;
        abstract fun toString(smallestUnitAmount: Int): String
        abstract fun toDigits(smallestUnitAmount: Int): String // No symbols and force . as separator
        abstract val code: String
    }

    data class Amount(val smallestUnitAmount: Int, val currency: Currency) {
        override fun toString() = this.currency.toString(this.smallestUnitAmount)
        fun toDigits() = this.currency.toDigits(this.smallestUnitAmount)
    }
}

operator fun Money.Amount.times(quantity: Int) =
    Money.Amount(this.smallestUnitAmount * quantity, this.currency)

inline fun <T> Iterable<T>.sumOf(
    currency: Money.Currency,
    selector: (T) -> Money.Amount,
): Money.Amount {
    var sum = 0
    for (element in this) {
        val amount = selector(element)
        assert(amount.currency == currency)
        sum += amount.smallestUnitAmount
    }
    return Money.Amount(sum, currency)
}
