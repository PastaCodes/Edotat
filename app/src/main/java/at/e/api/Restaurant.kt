package at.e.api

import at.e.lib.Money
import kotlinx.datetime.TimeZone
import kotlin.uuid.Uuid

data class Restaurant(
    val name: String,
    val address: Address,
    val location: Location,
    val timeZone: TimeZone,
    val currency: Money.Currency,
    val uuid: Uuid = Uuid.random(),
)
