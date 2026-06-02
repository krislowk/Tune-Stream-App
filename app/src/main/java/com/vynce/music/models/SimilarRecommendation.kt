package com.vynce.music.models

import com.vynce.music.db.entities.LocalItem
import com.vynce.vynceclient.models.YTItem

data class SimilarRecommendation(
    val title: LocalItem,
    val items: List<YTItem>,
)















