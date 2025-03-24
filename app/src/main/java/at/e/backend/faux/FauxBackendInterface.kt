package at.e.backend.faux

import at.e.backend.BackendInterface
import at.e.backend.Restaurant
import at.e.lib.ObjectFuzzySearch

object FauxBackendInterface : BackendInterface {
    private val TO_STRINGS: (Restaurant) -> List<String> = { restaurant ->
        listOf(
            restaurant.name,
            restaurant.address.streetAddress,
            restaurant.address.locality,
            restaurant.address.division,
            restaurant.address.country,
        )
    }
    private val WEIGHTS = listOf(5, 2, 3, 1, 1)
    private const val CUTOFF = 50
    private const val MAX_COUNT = 50

    override fun getRestaurants(query: String): List<Restaurant> =
        when {
            query.isEmpty() -> RESTAURANTS.take(MAX_COUNT)
            else -> ObjectFuzzySearch.extractSorted(
                query, RESTAURANTS, TO_STRINGS, WEIGHTS, CUTOFF, MAX_COUNT
            )
        }
}
