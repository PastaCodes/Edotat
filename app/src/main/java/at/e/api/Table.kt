package at.e.api

import kotlin.uuid.Uuid

data class Table(val code: String, val uuid: Uuid = Uuid.random())
