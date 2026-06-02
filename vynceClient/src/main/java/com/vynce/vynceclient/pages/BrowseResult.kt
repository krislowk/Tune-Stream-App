package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.models.filterExplicit

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
    val continuation: String? = null,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
        val continuation: String? = null,
    )

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(
                items = items.mapNotNull {
                    it.copy(
                        items = it.items
                            .filterExplicit()
                            .ifEmpty { return@mapNotNull null }
                    )
                }
            )
        } else this
}















