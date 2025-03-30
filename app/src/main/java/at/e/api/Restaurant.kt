package at.e.api

import kotlin.uuid.Uuid

data class Restaurant(
    val name: String,
    val address: Address,
    val location: Location,
    val uuid: Uuid = Uuid.random()
)
