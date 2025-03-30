package at.e.api.faux.lib

import at.e.lib.times
import com.frosch2010.fuzzywuzzy_kotlin.FuzzySearch

object ObjectFuzzySearch {
    // My best effort at a scoring function that feels natural
    private fun score(query: String, string: String) =
        FuzzySearch.ratio(query, string) +
        query.length * FuzzySearch.ratio(query, string.take(query.length)) +
        1000 * string.startsWith(query, ignoreCase = true) +
        500 * query.length * string.contains(query, ignoreCase = true)

    fun <T> extractSorted(
        query: String,
        choices: Collection<T>,
        toStrings: (T) -> List<String>,
        weights: List<Int>,
        cutoff: Int, // Mainly for performance purposes
        maxCount: Int,
    ): List<T> {
        val queryWords = query.split(' ')
        return choices
            .associateWith { choice ->
                queryWords.sumOf { word ->
                    toStrings(choice).zip(weights).sumOf { (string, weight) ->
                        weight * score(word, string)
                    }
                }
            }
            .entries
            .filter { (_, score) -> score > cutoff }
            .sortedByDescending { (_, score) -> score }
            .take(maxCount)
            .map { (choice, _) -> choice }
    }
}
