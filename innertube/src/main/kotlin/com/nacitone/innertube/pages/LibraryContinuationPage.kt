package com.nacitone.innertube.pages

import com.nacitone.innertube.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)
