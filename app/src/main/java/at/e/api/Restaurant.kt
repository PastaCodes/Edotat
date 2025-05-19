package at.e.api

import kotlinx.datetime.TimeZone
import kotlin.uuid.Uuid

data class Restaurant(
    val name: String,
    val address: Address,
    val location: Location,
    val timeZone: TimeZone,
    val uuid: Uuid = Uuid.random(),
)
