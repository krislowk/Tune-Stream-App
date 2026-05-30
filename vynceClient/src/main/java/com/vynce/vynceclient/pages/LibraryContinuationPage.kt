package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.YTItem

data class LibraryContinuationPage(
    val items: List<YTItem>,
    val continuation: String?,
)












