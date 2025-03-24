package at.e.backend

import at.e.backend.faux.FauxBackendInterface

val backendInterface: BackendInterface =
    FauxBackendInterface // Development mode only

interface BackendInterface {
    fun getRestaurants(query: String): List<Restaurant>
}
