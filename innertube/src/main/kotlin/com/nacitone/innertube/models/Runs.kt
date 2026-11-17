package com.nacitone.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Runs(
    val runs: List<Run>?,
)

@Serializable
data class Run(
    val text: String,
    val navigationEndpoint: NavigationEndpoint?,
)

fun List<Run>.splitBySeparator(): List<List<Run>> {
    val res = mutableListOf<List<Run>>()
    var tmp = mutableListOf<Run>()
    forEach { run ->
        if (run.text.trim() == "•") {
            res.add(tmp)
            tmp = mutableListOf()
        } else {
            tmp.add(run)
        }
    }
    res.add(tmp)
    return res
}


object ArtistConjunctions {
    var conjunctions: List<String> = listOf("and")
}

fun List<List<Run>>.clean(): List<List<Run>> {
    val firstGroup = getOrNull(0) ?: return this
    val hasArtistSignals = firstGroup.any { it.navigationEndpoint != null } ||
        firstGroup.any { it.text.contains(" & ") } ||
        ArtistConjunctions.conjunctions.any { conj ->
            firstGroup.any { it.text.trim().equals(conj, ignoreCase = true) }
        }
    return if (hasArtistSignals) this else drop(1)
}

fun List<Run>.oddElements() =
    filterIndexed { index, _ ->
        index % 2 == 0
    }
