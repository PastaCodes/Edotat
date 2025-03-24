package at.e.lib

import com.frosch2010.fuzzywuzzy_kotlin.FuzzySearch

object ObjectFuzzySearch {
    fun <T> extractSorted(
        query: String,
        choices: Collection<T>,
        toStrings: (T) -> List<String>,
        weights: List<Int>,
        cutoff: Int,
        maxCount: Int,
    ) =
        choices
            .associateWith { choice ->
                toStrings(choice).zip(weights).sumOf { (string, weight) ->
                    weight * FuzzySearch.ratio(query, string)
                }
            }
            .entries
            .filter { (_, score) ->
                score > cutoff
            }
            .sortedByDescending { (_, score) ->
                score
            }
            .map { (choice, _) ->
                choice
            }
            .take(maxCount)
}
