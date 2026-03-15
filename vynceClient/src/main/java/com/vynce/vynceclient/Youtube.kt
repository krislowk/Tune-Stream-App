package com.vynce.vynceclient

import com.vynce.vynceclient.models.YtClient.Companion.WEB_REMIX
import com.vynce.vynceclient.models.getContinuation
import com.vynce.vynceclient.pages.HomePage
import com.vynce.vynceclient.pages.SearchPage
import com.vynce.vynceclient.pages.SearchResult

object Youtube {
    val innerTube by lazy { Innertube() }

    suspend fun home(browseId: String = "FEmusic_home", params: String? = null, setLogin: Boolean = false): Result<HomePage> = runCatching {
        var response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params, setLogin = setLogin)
        val sectionListRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer

        var continuation = sectionListRenderer?.continuations?.getContinuation()

        val filters = sectionListRenderer?.header?.chipCloudRenderer?.chips?.mapNotNull { chip ->
            chip.chipCloudChipRenderer.let { renderer ->
                HomePage.Filter(
                    title = renderer.text?.runs?.firstOrNull()?.text ?: return@mapNotNull null,
                    endpoint = renderer.navigationEndpoint.browseEndpoint ?: return@mapNotNull null,
                    isSelected = renderer.isSelected
                )
            }
        }.orEmpty()

        val sections = sectionListRenderer?.contents!!
            .mapNotNull { it.musicCarouselShelfRenderer }
            .mapNotNull {
                HomePage.Section.fromMusicCarouselShelfRenderer(it)
            }.toMutableList()

        while (continuation != null) {
            response = innerTube.browse(WEB_REMIX, continuation = continuation, setLogin = setLogin)
            continuation = response.continuationContents?.sectionListContinuation?.continuations?.getContinuation()
            sections += response.continuationContents?.sectionListContinuation?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }.orEmpty()
        }
        HomePage(sections, filters)
    }

    suspend fun search(query: String, filter: SearchFilter = SearchFilter("")): Result<SearchResult> = runCatching {
        val response = innerTube.search(WEB_REMIX, query, filter.value)
        SearchResult(
            items = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.contents?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }.orEmpty(),
            continuation = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents?.lastOrNull()
                ?.musicShelfRenderer?.continuations?.getContinuation()
        )
    }

    @JvmInline
    value class SearchFilter(val value: String) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
        }
    }

    val MAX_GET_QUEUE_SIZE = 1000

    private val VISITOR_DATA_PREFIX = "Cgt"

    val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}