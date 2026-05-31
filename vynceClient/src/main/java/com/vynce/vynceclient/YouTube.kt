package com.vynce.vynceclient

import com.vynce.vynceclient.models.AccountInfo
import com.vynce.vynceclient.models.Album
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.EpisodeItem
import com.vynce.vynceclient.models.GridRenderer
import com.vynce.vynceclient.models.MusicResponsiveListItemRenderer
import com.vynce.vynceclient.models.MusicShelfRenderer
import com.vynce.vynceclient.models.MusicTwoRowItemRenderer
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.PodcastItem
import com.vynce.vynceclient.models.Run
import com.vynce.vynceclient.models.SearchSuggestions
import com.vynce.vynceclient.models.SectionListRenderer
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.WatchEndpoint
import com.vynce.vynceclient.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.models.YouTubeClient
import com.vynce.vynceclient.models.YouTubeClient.Companion.WEB
import com.vynce.vynceclient.models.YouTubeClient.Companion.WEB_REMIX
import com.vynce.vynceclient.models.YtLocale
import com.vynce.vynceclient.models.getContinuation
import com.vynce.vynceclient.models.getItems
import com.vynce.vynceclient.models.oddElements
import com.vynce.vynceclient.models.response.AccountMenuResponse
import com.vynce.vynceclient.models.response.BrowseResponse
import com.vynce.vynceclient.models.response.FeedbackResponse
import com.vynce.vynceclient.models.response.GetQueueResponse
import com.vynce.vynceclient.models.response.GetSearchSuggestionsResponse
import com.vynce.vynceclient.models.response.GetTranscriptResponse
import com.vynce.vynceclient.models.response.NextResponse
import com.vynce.vynceclient.models.response.PlayerResponse
import com.vynce.vynceclient.models.response.SearchResponse
import com.vynce.vynceclient.models.splitBySeparator
import com.vynce.vynceclient.pages.AlbumPage
import com.vynce.vynceclient.pages.ArtistItemsContinuationPage
import com.vynce.vynceclient.pages.ArtistItemsPage
import com.vynce.vynceclient.pages.ArtistPage
import com.vynce.vynceclient.pages.BrowseResult
import com.vynce.vynceclient.pages.ChartsPage
import com.vynce.vynceclient.pages.ExplorePage
import com.vynce.vynceclient.pages.HistoryPage
import com.vynce.vynceclient.pages.HomePage
import com.vynce.vynceclient.pages.LibraryContinuationPage
import com.vynce.vynceclient.pages.LibraryPage
import com.vynce.vynceclient.pages.MoodAndGenres
import com.vynce.vynceclient.pages.NewReleaseAlbumPage
import com.vynce.vynceclient.pages.NextPage
import com.vynce.vynceclient.pages.NextResult
import com.vynce.vynceclient.pages.PageHelper
import com.vynce.vynceclient.pages.PlaylistContinuationPage
import com.vynce.vynceclient.pages.PlaylistPage
import com.vynce.vynceclient.pages.PodcastPage
import com.vynce.vynceclient.pages.RelatedPage
import com.vynce.vynceclient.pages.SearchPage
import com.vynce.vynceclient.pages.SearchResult
import com.vynce.vynceclient.pages.SearchSuggestionPage
import com.vynce.vynceclient.pages.SearchSummary
import com.vynce.vynceclient.pages.SearchSummaryPage
import io.ktor.client.call.body
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.Proxy

object YouTube {
    private val innerTube = Innertube()

    private const val ENABLE_NEWPIPE_STREAM_INFO_EXTRACTOR = true

    var locale: YtLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String?
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
        }

    var dataSyncId: String?
        get() = innerTube.dataSyncId
        set(value) {
            innerTube.dataSyncId = value
        }

    var cookie: String?
        get() = innerTube.cookie
        set(value) {
            innerTube.cookie = value
        }
    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) {
            innerTube.proxy = value
        }

    var proxyAuth: String?
        get() = innerTube.proxyAuth
        set(value) {
            innerTube.proxyAuth = value
        }

    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> =
        runCatching {
            val response = innerTube.getSearchSuggestions(WEB_REMIX, query).body<GetSearchSuggestionsResponse>()
            SearchSuggestions(
                queries =
                    response.contents
                        ?.getOrNull(0)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull { content ->
                            content.searchSuggestionRenderer
                                ?.suggestion
                                ?.runs
                                ?.joinToString(separator = "") { it.text }
                        }.orEmpty(),
                recommendedItems =
                    response.contents
                        ?.getOrNull(1)
                        ?.searchSuggestionsSectionRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicResponsiveListItemRenderer?.let { renderer ->
                                SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                            }
                        }.orEmpty(),
            )
        }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query).body<SearchResponse>()
            val allSummaries = mutableListOf<SearchSummary>()

            response.contents
                ?.tabbedSearchResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { section ->
                    if (section.musicCardShelfRenderer != null) {
                        // Top result card - keep as single section
                        val items =
                            listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(section.musicCardShelfRenderer))
                                .plus(
                                    section.musicCardShelfRenderer.contents
                                        ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                        ?.mapNotNull(SearchSummaryPage.Companion::fromMusicResponsiveListItemRenderer)
                                        .orEmpty(),
                                ).distinctBy { it.id }

                        if (items.isNotEmpty()) {
                            allSummaries.add(
                                SearchSummary(
                                    title =
                                        section.musicCardShelfRenderer.header
                                            .musicCardShelfHeaderBasicRenderer.title
                                            .runs?.firstOrNull()?.text
                                            ?: YouTubeConstants.DEFAULT_TOP_RESULT,
                                    items = items,
                                ),
                            )
                        }
                    } else if (section.musicShelfRenderer != null) {
                        val items =
                            section.musicShelfRenderer.contents
                                ?.getItems()
                                ?.mapNotNull { SearchSummaryPage.fromMusicResponsiveListItemRenderer(it) }
                                ?.distinctBy { it.id }
                                ?: emptyList()

                        if (items.isEmpty()) return@forEach

                        val apiTitle =
                            section.musicShelfRenderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text

                        if (apiTitle != null) {
                            // API provided a title, use single section
                            allSummaries.add(SearchSummary(title = apiTitle, items = items))
                        } else {
                            // No title - group items by type into separate sections
                            val grouped =
                                items.groupBy { item ->
                                    when (item) {
                                        is EpisodeItem -> {
                                            "Episodes"
                                        }

                                        is PodcastItem -> {
                                            "Podcasts"
                                        }

                                        is AlbumItem -> {
                                            "Albums"
                                        }

                                        is ArtistItem -> {
                                            if (item.isProfile) "Profiles" else "Artists"
                                        }

                                        is PlaylistItem -> {
                                            "Playlists"
                                        }

                                        is SongItem -> {
                                            when {
                                                item.isEpisode -> "Episodes"
                                                item.isVideoSong -> "Videos"
                                                else -> "Songs"
                                            }
                                        }
                                    }
                                }

                            // Add each group as a separate section in a logical order
                            val sectionOrder =
                                listOf(
                                    "Songs",
                                    "Videos",
                                    "Albums",
                                    "Artists",
                                    "Playlists",
                                    "Podcasts",
                                    "Episodes",
                                    "Profiles",
                                    YouTubeConstants.DEFAULT_OTHER_RESULTS,
                                )
                            sectionOrder.forEach { sectionName ->
                                grouped[sectionName]?.let { groupItems ->
                                    if (groupItems.isNotEmpty()) {
                                        allSummaries.add(SearchSummary(title = sectionName, items = groupItems))
                                    }
                                }
                            }
                        }
                    }
                }

            // Merge sections with the same title
            val mergedSummaries =
                allSummaries
                    .groupBy { it.title }
                    .map { (title, sections) ->
                        SearchSummary(
                            title = title,
                            items = sections.flatMap { it.items }.distinctBy { it.id },
                        )
                    }
                    // Reorder to maintain logical order
                    .sortedBy { summary ->
                        when (summary.title) {
                            YouTubeConstants.DEFAULT_TOP_RESULT -> 0
                            "Songs" -> 1
                            "Videos" -> 2
                            "Albums" -> 3
                            "Artists" -> 4
                            "Playlists" -> 5
                            "Podcasts" -> 6
                            "Episodes" -> 7
                            "Profiles" -> 8
                            else -> 9
                        }
                    }

            SearchSummaryPage(summaries = mergedSummaries)
        }

    suspend fun search(
        query: String,
        filter: SearchFilter,
    ): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, query, filter.value).body<SearchResponse>()
            val shelves =
                response.contents
                    ?.tabbedSearchResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.mapNotNull { it.musicShelfRenderer }
                    .orEmpty()
            SearchResult(
                items =
                    shelves
                        .flatMap { shelf ->
                            shelf.contents?.getItems()?.mapNotNull { SearchPage.toYTItem(it) } ?: emptyList()
                        }.distinctBy { it.id },
                continuation =
                    shelves
                        .firstOrNull { it.continuations != null }
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> =
        runCatching {
            val response = innerTube.search(WEB_REMIX, continuation = continuation).body<SearchResponse>()
            val items =
                response.continuationContents
                    ?.musicShelfContinuation
                    ?.contents
                    ?.mapNotNull {
                        SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                    } ?: emptyList()
            SearchResult(
                items = items,
                continuation =
                    if (items.isEmpty()) {
                        null
                    } else {
                        response.continuationContents
                            ?.musicShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                    },
            )
        }

    suspend fun addToPlaylist(playlistId: String, videoId: String) =
        innerTube.addToPlaylist(WEB_REMIX, playlistId, videoId)

    suspend fun album(
        browseId: String,
        withSongs: Boolean = true,
    ): Result<AlbumPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()
            if (browseId.contains("FEmusic_library_privately_owned_release_detail")) {
                val playlistId =
                    response.header
                        ?.musicDetailHeaderRenderer
                        ?.menu
                        ?.menuRenderer
                        ?.topLevelButtons
                        ?.firstOrNull()
                        ?.buttonRenderer
                        ?.navigationEndpoint
                        ?.watchPlaylistEndpoint
                        ?.playlistId!!
                val albumItem =
                    AlbumItem(
                        browseId = browseId,
                        playlistId = playlistId,
                        title =
                            response.header.musicDetailHeaderRenderer.title.runs
                                ?.firstOrNull()
                                ?.text!!,
                        artists =
                            response.header.musicDetailHeaderRenderer.subtitle.runs?.filter { it.navigationEndpoint != null }?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                )
                            },
                        year =
                            response.header.musicDetailHeaderRenderer.subtitle.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            response.header.musicDetailHeaderRenderer.thumbnail.croppedSquareThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()!!
                                .url,
                        explicit = response.header.musicDetailHeaderRenderer.subtitleBadges?.any {
                            it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                        } ?: false,
                    )
                return@runCatching AlbumPage(
                    album = albumItem,
                    songs =
                        response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicShelfRenderer
                            ?.contents
                            ?.getItems()
                            ?.mapNotNull {
                                AlbumPage.getSong(it, albumItem)
                            }!!
                            .toMutableList(),
                    otherVersions = emptyList(),
                )
            } else {
                val playlistId =
                    response.microformat
                        ?.microformatDataRenderer
                        ?.urlCanonical
                        ?.substringAfterLast('=')!!
                val albumItem =
                    AlbumItem(
                        browseId = browseId,
                        playlistId = playlistId,
                        title =
                            response.contents
                                ?.twoColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text!!,
                        artists =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.straplineTextOne
                                ?.runs
                                ?.oddElements()
                                ?.map {
                                    Artist(
                                        name = it.text,
                                        id = it.navigationEndpoint?.browseEndpoint?.browseId,
                                    )
                                }!!,
                        year =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail =
                            response.contents.twoColumnBrowseResultsRenderer.tabs
                                .firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url!!,
                        explicit = response.contents.twoColumnBrowseResultsRenderer.tabs
                            .firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicResponsiveHeaderRenderer
                            ?.subtitleBadges
                            ?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } ?: false,
                    )
                return@runCatching AlbumPage(
                    album = albumItem,
                    songs =
                        if (withSongs) {
                            albumSongs(
                                playlistId,
                                albumItem,
                            ).getOrThrow()
                        } else {
                            emptyList()
                        },
                    otherVersions =
                        response.contents.twoColumnBrowseResultsRenderer.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.getOrNull(
                                1,
                            )?.musicCarouselShelfRenderer
                            ?.contents
                            ?.mapNotNull { it.musicTwoRowItemRenderer }
                            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                            .orEmpty(),
                )
            }
        }

    suspend fun albumSongs(
        playlistId: String,
        album: AlbumItem? = null,
    ): Result<List<SongItem>> =
        runCatching {
            var response = innerTube.browse(WEB_REMIX, "VL$playlistId").body<BrowseResponse>()
            val songs =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.secondaryContents
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
                    ?.musicPlaylistShelfRenderer
                    ?.contents
                    ?.getItems()
                    ?.mapNotNull {
                        AlbumPage.getSong(it, album)
                    }!!
                    .toMutableList()
            var continuation =
                response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                    .contents
                    .firstOrNull()
                    ?.musicPlaylistShelfRenderer
                    ?.contents
                    ?.getContinuation()
            val seenContinuations = mutableSetOf<String>()
            var requestCount = 0
            val maxRequests = 50 // Prevent excessive API calls

            while (continuation != null && requestCount < maxRequests) {
                // Prevent infinite loops by tracking seen continuations
                if (continuation in seenContinuations) {
                    break
                }
                seenContinuations.add(continuation)
                requestCount++

                response =
                    innerTube
                        .browse(
                            client = WEB_REMIX,
                            continuation = continuation,
                        ).body<BrowseResponse>()
                songs +=
                    response.onResponseReceivedActions
                        ?.firstOrNull()
                        ?.appendContinuationItemsAction
                        ?.continuationItems
                        ?.getItems()
                        ?.mapNotNull {
                            AlbumPage.getSong(it, album)
                        }.orEmpty()
                continuation =
                    response.continuationContents
                        ?.musicPlaylistShelfContinuation
                        ?.continuations
                        ?.getContinuation()
            }
            songs
        }

    suspend fun artist(browseId: String): Result<ArtistPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId).body<BrowseResponse>()

            fun mapRuns(runs: List<Run>?): List<Run>? =
                runs?.map { run ->
                    Run(
                        text = run.text,
                        navigationEndpoint = run.navigationEndpoint,
                    )
                }

            val descriptionRuns =
                response.contents
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
                    ?.musicDescriptionShelfRenderer
                    ?.description
                    ?.runs
                    ?.let(::mapRuns)
                    ?: response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.description
                        ?.runs
                        ?.let(::mapRuns)

            // Check subscription state from multiple locations:
            // 1. musicImmersiveHeaderRenderer.subscriptionButton (regular artists)
            // 2. musicVisualHeaderRenderer.subscriptionButton (podcast channels)
            val immersiveSubscribed =
                response.header
                    ?.musicImmersiveHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.subscribed
            val visualSubscribed =
                response.header
                    ?.musicVisualHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.subscribed
            val isSubscribed = immersiveSubscribed ?: visualSubscribed ?: false

            // Also extract channelId from visual header if not in immersive header
            val channelIdFromVisual =
                response.header
                    ?.musicVisualHeaderRenderer
                    ?.subscriptionButton
                    ?.subscribeButtonRenderer
                    ?.channelId

            ArtistPage(
                artist =
                    ArtistItem(
                        id = browseId,
                        title =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text
                                ?: response.header
                                    ?.musicHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text!!,
                        thumbnail =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.thumbnail
                                ?.musicThumbnailRenderer
                                ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicVisualHeaderRenderer
                                    ?.foregroundThumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl()
                                ?: response.header
                                    ?.musicDetailHeaderRenderer
                                    ?.thumbnail
                                    ?.musicThumbnailRenderer
                                    ?.getThumbnailUrl(),
                        channelId =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.subscriptionButton
                                ?.subscribeButtonRenderer
                                ?.channelId
                                ?: channelIdFromVisual,
                        playEndpoint =
                            response.contents
                                ?.singleColumnBrowseResultsRenderer
                                ?.tabs
                                ?.firstOrNull()
                                ?.tabRenderer
                                ?.content
                                ?.sectionListRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicShelfRenderer
                                ?.contents
                                ?.firstOrNull()
                                ?.musicResponsiveListItemRenderer
                                ?.overlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchEndpoint,
                        shuffleEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.playButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint
                                ?: response.contents
                                    ?.singleColumnBrowseResultsRenderer
                                    ?.tabs
                                    ?.firstOrNull()
                                    ?.tabRenderer
                                    ?.content
                                    ?.sectionListRenderer
                                    ?.contents
                                    ?.firstOrNull()
                                    ?.musicShelfRenderer
                                    ?.contents
                                    ?.firstOrNull()
                                    ?.musicResponsiveListItemRenderer
                                    ?.navigationEndpoint
                                    ?.watchPlaylistEndpoint,
                        radioEndpoint =
                            response.header
                                ?.musicImmersiveHeaderRenderer
                                ?.startRadioButton
                                ?.buttonRenderer
                                ?.navigationEndpoint
                                ?.watchEndpoint,
                    ),
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
                description = descriptionRuns?.joinToString(separator = "") { it.text },
                subscriberCountText =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.subscriptionButton2
                        ?.subscribeButtonRenderer
                        ?.subscriberCountWithSubscribeText
                        ?.runs
                        ?.firstOrNull()
                        ?.text
                        ?: response.header
                            ?.musicImmersiveHeaderRenderer
                            ?.subscriptionButton
                            ?.subscribeButtonRenderer
                            ?.longSubscriberCountText
                            ?.runs
                            ?.firstOrNull()
                            ?.text
                        ?: response.header
                            ?.musicImmersiveHeaderRenderer
                            ?.subscriptionButton
                            ?.subscribeButtonRenderer
                            ?.shortSubscriberCountText
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                monthlyListenerCount =
                    response.header
                        ?.musicImmersiveHeaderRenderer
                        ?.monthlyListenerCount
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                descriptionRuns = descriptionRuns,
                isSubscribed = isSubscribed,
            )
        }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            val sectionContent =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()

            val gridRenderer = sectionContent?.gridRenderer
            val musicCarouselShelfRenderer = sectionContent?.musicCarouselShelfRenderer
            val musicPlaylistShelfRenderer = sectionContent?.musicPlaylistShelfRenderer
            val musicShelfRenderer = sectionContent?.musicShelfRenderer

            when {
                gridRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            gridRenderer.header
                                ?.gridHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                .orEmpty(),
                        items =
                            gridRenderer.items.mapNotNull {
                                it.musicTwoRowItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                                }
                            },
                        continuation = gridRenderer.continuations?.getContinuation(),
                    )
                }

                musicCarouselShelfRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            musicCarouselShelfRenderer.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                .orEmpty(),
                        items =
                            musicCarouselShelfRenderer.contents.mapNotNull { content ->
                                content.musicTwoRowItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                                } ?: content.musicResponsiveListItemRenderer?.let { renderer ->
                                    ArtistItemsPage.fromMusicResponsiveListItemRenderer(renderer)
                                }
                            },
                        continuation = null,
                    )
                }

                musicShelfRenderer != null -> {
                    ArtistItemsPage(
                        title =
                            musicShelfRenderer.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: response.header
                                    ?.musicHeaderRenderer
                                    ?.title
                                    ?.runs
                                    ?.firstOrNull()
                                    ?.text
                                ?: "",
                        items =
                            musicShelfRenderer.contents?.getItems()?.mapNotNull {
                                ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                            } ?: emptyList(),
                        continuation = musicShelfRenderer.continuations?.getContinuation(),
                    )
                }

                else -> {
                    ArtistItemsPage(
                        title =
                            response.header
                                ?.musicHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text ?: "",
                        items =
                            musicPlaylistShelfRenderer?.contents?.getItems()?.mapNotNull {
                                ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                            } ?: emptyList(),
                        continuation = musicPlaylistShelfRenderer?.contents?.getContinuation(),
                    )
                }
            }
        }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()

            when {
                response.continuationContents?.gridContinuation != null -> {
                    val gridContinuation = response.continuationContents.gridContinuation
                    val items =
                        gridContinuation.items.mapNotNull {
                            it.musicTwoRowItemRenderer?.let { renderer ->
                                ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                            }
                        }
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else gridContinuation.continuations?.getContinuation(),
                    )
                }

                response.continuationContents?.musicPlaylistShelfContinuation != null -> {
                    val musicPlaylistShelfContinuation = response.continuationContents.musicPlaylistShelfContinuation
                    val items =
                        musicPlaylistShelfContinuation.contents.getItems().mapNotNull {
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                        }
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else musicPlaylistShelfContinuation.continuations?.getContinuation(),
                    )
                }

                else -> {
                    val continuationItems =
                        response.onResponseReceivedActions
                            ?.firstOrNull()
                            ?.appendContinuationItemsAction
                            ?.continuationItems
                    val items =
                        continuationItems?.getItems()?.mapNotNull {
                            ArtistItemsPage.fromMusicResponsiveListItemRenderer(it)
                        } ?: emptyList()
                    ArtistItemsContinuationPage(
                        items = items,
                        continuation = if (items.isEmpty()) null else continuationItems?.getContinuation(),
                    )
                }
            }
        }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "VL$playlistId",
                        setLogin = true,
                    ).body<BrowseResponse>()
            val base =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
            val header =
                base?.musicResponsiveHeaderRenderer
                    ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer

            val editable = base?.musicEditablePlaylistDetailHeaderRenderer != null

            val description: String? =
                header?.description?.musicDescriptionShelfRenderer?.description?.runs?.joinToString("") { it.text }
                    ?: base?.musicEditablePlaylistDetailHeaderRenderer
                        ?.header?.musicDetailHeaderRenderer
                        ?.description?.runs?.joinToString("") { it.text }
                    ?: response.header?.musicDetailHeaderRenderer
                        ?.description?.runs?.joinToString("") { it.text }

            val author: Artist? = run {
                val fromStrapline = header?.straplineTextOne?.runs
                    ?.firstOrNull()
                    ?.let { Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId) }
                if (fromStrapline != null) return@run fromStrapline

                val detailSubtitle = base?.musicEditablePlaylistDetailHeaderRenderer
                    ?.header?.musicDetailHeaderRenderer?.subtitle?.runs
                    ?: response.header?.musicDetailHeaderRenderer?.subtitle?.runs
                if (detailSubtitle != null) {
                    val segments = detailSubtitle.splitBySeparator()
                    val run = segments.getOrNull(1)?.firstOrNull()
                        ?: segments.firstOrNull()?.firstOrNull()
                    val fromDetail = run?.let {
                        Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId)
                    }
                    if (fromDetail != null) return@run fromDetail
                }

                val fromHeaderSubtitle = header?.subtitle?.runs
                    ?.firstOrNull { it.navigationEndpoint != null }
                    ?.let { Artist(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId) }
                if (fromHeaderSubtitle != null) return@run fromHeaderSubtitle

                val facepile = header?.facepile?.avatarStackViewModel
                if (facepile != null) {
                    val name = facepile.text?.content
                    val browseId = facepile.rendererContext?.commandContext?.onTap?.innertubeCommand?.browseEndpoint?.browseId
                    if (name != null) return@run Artist(name = name, id = browseId)
                }

                null
            }

            val authorAvatarUrl: String? = header?.facepile
                ?.avatarStackViewModel?.avatars?.firstOrNull()
                ?.avatarViewModel?.image?.sources?.firstOrNull()
                ?.url

            PlaylistPage(
                playlist =
                    PlaylistItem(
                        id = playlistId,
                        title =
                            header
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text!!,
                        author = author,
                        songCountText =
                            header.secondSubtitle
                                ?.runs
                                ?.findLast {
                                    it.text.any { c -> c.isDigit() } &&
                                            !it.text.contains("view", ignoreCase = true) &&
                                            !it.text.contains("hour", ignoreCase = true) &&
                                            !it.text.contains("minute", ignoreCase = true)
                                }?.text,
                        thumbnail =
                            header.thumbnail
                                ?.musicThumbnailRenderer
                                ?.thumbnail
                                ?.thumbnails
                                ?.lastOrNull()
                                ?.url!!,
                        playEndpoint = null,
                        shuffleEndpoint =
                            header.buttons
                                .lastOrNull()
                                ?.menuRenderer
                                ?.items
                                ?.firstOrNull()
                                ?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint!!,
                        radioEndpoint =
                            header.buttons
                                .getOrNull(2)
                                ?.menuRenderer
                                ?.items
                                ?.find {
                                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                                }?.menuNavigationItemRenderer
                                ?.navigationEndpoint
                                ?.watchPlaylistEndpoint,
                        isEditable = editable,
                        description = description,
                        authorAvatarUrl = authorAvatarUrl,
                    ),
                songs =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                        ?.getItems()
                        ?.mapNotNull {
                            PlaylistPage.fromMusicResponsiveListItemRenderer(it)
                        } ?: emptyList(),
                songsContinuation =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                        ?.getContinuation()
                        ?: response.contents
                            ?.twoColumnBrowseResultsRenderer
                            ?.secondaryContents
                            ?.sectionListRenderer
                            ?.contents
                            ?.firstOrNull()
                            ?.musicPlaylistShelfRenderer
                            ?.continuations
                            ?.getContinuation(),
                continuation =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    suspend fun playlistContinuation(continuation: String): Result<PlaylistContinuationPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val mainContents: List<MusicShelfRenderer.Content> =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.mapNotNull { content: SectionListRenderer.Content ->
                        content.musicPlaylistShelfRenderer?.contents
                            ?: content.musicShelfRenderer?.contents
                    }
                    ?.flatten()
                    ?: emptyList()

            val shelfContents: List<MusicShelfRenderer.Content> =
                response.continuationContents?.musicPlaylistShelfContinuation?.contents ?: emptyList()

            val musicShelfContinuationContents: List<MusicShelfRenderer.Content> =
                response.continuationContents?.musicShelfContinuation?.contents ?: emptyList()

            val appendedContents: List<MusicShelfRenderer.Content> =
                response.onResponseReceivedActions
                    ?.firstOrNull()
                    ?.appendContinuationItemsAction
                    ?.continuationItems
                    .orEmpty()

            val allContents = mainContents + shelfContents + musicShelfContinuationContents + appendedContents

            val songs =
                allContents
                    .mapNotNull { content: MusicShelfRenderer.Content -> content.musicResponsiveListItemRenderer }
                    .mapNotNull { renderer -> PlaylistPage.fromMusicResponsiveListItemRenderer(renderer) }

            val nextContinuation =
                if (songs.isEmpty()) {
                    null
                } else {
                    response.continuationContents
                        ?.sectionListContinuation
                        ?.continuations
                        ?.getContinuation()
                        ?: response.continuationContents
                            ?.musicPlaylistShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                        ?: response.continuationContents
                            ?.musicShelfContinuation
                            ?.continuations
                            ?.getContinuation()
                        ?: response.onResponseReceivedActions
                            ?.firstOrNull()
                            ?.appendContinuationItemsAction
                            ?.continuationItems
                            ?.getContinuation()
                }

            PlaylistContinuationPage(
                songs = songs,
                continuation = nextContinuation,
            )
        }


    suspend fun home(
        continuation: String? = null,
        params: String? = null,
    ): Result<HomePage> =
        runCatching {
            if (continuation != null) {
                return@runCatching homeContinuation(continuation).getOrThrow()
            }

            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_home", params = params).body<BrowseResponse>()
            val continuation =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.continuations
                    ?.getContinuation()
            val sectionListRender =
                response.contents
                    ?.singleColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
            val carousels = sectionListRender?.contents?.mapNotNull { it.musicCarouselShelfRenderer } ?: emptyList()
            val sections =
                carousels
                    .map {
                        HomePage.Section.fromMusicCarouselShelfRenderer(it)
                    }.toMutableList()
            val chips =
                sectionListRender
                    ?.header
                    ?.chipCloudRenderer
                    ?.chips
                    ?.mapNotNull { HomePage.Chip.fromChipCloudChipRenderer(it) }
            HomePage(chips, sections, continuation)
        }

    private suspend fun homeContinuation(continuation: String): Result<HomePage> =
        runCatching {
            val response =
                innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
            val continuation =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.continuations
                    ?.getContinuation()
            HomePage(
                null,
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.mapNotNull { it.musicCarouselShelfRenderer }
                    ?.map {
                        HomePage.Section.fromMusicCarouselShelfRenderer(it)
                    }.orEmpty(),
                continuation,
            )
        }

    suspend fun explore(continuation: String? = null): Result<ExplorePage> =
        runCatching {
            val response = if (continuation != null) {
                innerTube.browse(WEB_REMIX, continuation = continuation).body<BrowseResponse>()
            } else {
                innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore").body<BrowseResponse>()
            }
            
            val contents: List<SectionListRenderer.Content>
            val nextContinuation: String?

            if (continuation != null) {
                val sectionList = response.continuationContents?.sectionListContinuation
                contents = sectionList?.contents ?: emptyList()
                nextContinuation = sectionList?.continuations?.getContinuation()
            } else {
                val sectionList = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer
                contents = sectionList?.contents ?: emptyList()
                nextContinuation = sectionList?.continuations?.getContinuation()
            }

            ExplorePage(
                newReleaseAlbums =
                    contents.find {
                        it.musicCarouselShelfRenderer
                            ?.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.moreContentButton
                            ?.buttonRenderer
                            ?.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId ==
                                "FEmusic_new_releases_albums"
                    }?.musicCarouselShelfRenderer
                    ?.contents
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                    .orEmpty(),
                moodAndGenres =
                    contents.find {
                        it.musicCarouselShelfRenderer
                            ?.header
                            ?.musicCarouselShelfBasicHeaderRenderer
                            ?.moreContentButton
                            ?.buttonRenderer
                            ?.navigationEndpoint
                            ?.browseEndpoint
                            ?.browseId ==
                                "FEmusic_moods_and_genres"
                    }?.musicCarouselShelfRenderer
                    ?.contents
                    ?.mapNotNull { it.musicNavigationButtonRenderer }
                    ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                    .orEmpty(),
                continuation = nextContinuation
            )
        }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_new_releases_albums").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull()
                ?.gridRenderer
                ?.items
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                .orEmpty()
        }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres").body<BrowseResponse>()
            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents!!
                .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
        }

    suspend fun browse(
        browseId: String,
        params: String? = null,
        tabIndex: Int = 0,
    ): Result<BrowseResult> =
        runCatching {
            // Use authentication for library endpoints
            val needsLogin = browseId.startsWith("FEmusic_library") || browseId == "VLSE" || browseId == "VLRDPN" || browseId.startsWith("FEmusic_liked")
            val response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params, setLogin = needsLogin).body<BrowseResponse>()
            val tabs = response.contents?.singleColumnBrowseResultsRenderer?.tabs
            val sectionListRenderer =
                if (tabs != null && tabs.size > tabIndex) {
                    tabs[tabIndex]
                        .tabRenderer.content
                        ?.sectionListRenderer
                } else {
                    null
                }
            val sectionContents = sectionListRenderer?.contents
            BrowseResult(
                title =
                    response.header
                        ?.musicHeaderRenderer
                        ?.title
                        ?.runs
                        ?.firstOrNull()
                        ?.text,
                continuation = sectionListRenderer?.continuations?.getContinuation(),
                items =
                    sectionContents
                        ?.mapNotNull { content ->
                            when {
                                content.gridRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.gridRenderer.header
                                                ?.gridHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.gridRenderer.items
                                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                                .mapNotNull { renderer ->
                                                    // Try LibraryPage first (more lenient for library endpoints), fall back to RelatedPage
                                                    LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                                                        ?: RelatedPage.fromMusicTwoRowItemRenderer(renderer)
                                                },
                                        continuation = content.gridRenderer.continuations?.getContinuation(),
                                    )
                                }

                                content.musicCarouselShelfRenderer != null -> {
                                    val carouselItems = mutableListOf<YTItem>()
                                    for (carouselContent in content.musicCarouselShelfRenderer.contents) {
                                        val item = carouselContent.musicTwoRowItemRenderer?.let { renderer ->
                                            LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                                                ?: RelatedPage.fromMusicTwoRowItemRenderer(renderer)
                                        } ?: carouselContent.musicMultiRowListItemRenderer?.let { renderer ->
                                            PodcastPage.fromMusicMultiRowListItemRenderer(renderer)
                                        } ?: carouselContent.musicResponsiveListItemRenderer?.let { renderer ->
                                            LibraryPage.fromMusicResponsiveListItemRenderer(renderer)
                                                ?: RelatedPage.fromMusicResponsiveListItemRenderer(renderer)
                                        }
                                        if (item != null) {
                                            carouselItems.add(item)
                                        }
                                    }
                                    BrowseResult.Item(
                                        title =
                                            content.musicCarouselShelfRenderer.header
                                                ?.musicCarouselShelfBasicHeaderRenderer
                                                ?.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items = carouselItems,
                                    )
                                }

                                content.musicShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title =
                                            content.musicShelfRenderer.title
                                                ?.runs
                                                ?.firstOrNull()
                                                ?.text,
                                        items =
                                            content.musicShelfRenderer.contents
                                                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                                                ?.mapNotNull(LibraryPage.Companion::fromMusicResponsiveListItemRenderer)
                                                ?: emptyList(),
                                        continuation = content.musicShelfRenderer.continuations?.getContinuation(),
                                    )
                                }

                                content.musicPlaylistShelfRenderer != null -> {
                                    BrowseResult.Item(
                                        title = null, // MusicPlaylistShelfRenderer doesn't have a title
                                        items =
                                            content.musicPlaylistShelfRenderer.contents
                                                .getItems()
                                                .mapNotNull(LibraryPage.Companion::fromMusicResponsiveListItemRenderer),
                                    )
                                }

                                else -> {
                                    null
                                }
                            }
                        }.orEmpty(),
            )
        }

    suspend fun library(
        browseId: String,
        tabIndex: Int = 0,
    ): Result<LibraryPage> =
        browse(browseId, tabIndex = tabIndex).map { result ->
            LibraryPage(
                items = result.items.flatMap { it.items },
                continuation = result.continuation ?: result.items.firstOrNull { it.continuation != null }?.continuation,
            )
        }

    suspend fun libraryContinuation(continuation: String) =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val contents = response.continuationContents

            when {
                contents?.gridContinuation != null -> {
                    LibraryContinuationPage(
                        items =
                            contents.gridContinuation.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull { LibraryPage.fromMusicTwoRowItemRenderer(it) },
                        continuation = contents.gridContinuation.continuations?.getContinuation(),
                    )
                }

                contents?.musicShelfContinuation != null -> {
                    LibraryContinuationPage(
                        items =
                            contents.musicShelfContinuation.contents
                                ?.mapNotNull(MusicShelfRenderer.Content::musicResponsiveListItemRenderer)
                                ?.mapNotNull { LibraryPage.fromMusicResponsiveListItemRenderer(it) }
                                ?: emptyList(),
                        continuation = contents.musicShelfContinuation.continuations?.getContinuation(),
                    )
                }

                else -> {
                    LibraryContinuationPage(
                        items = emptyList(),
                        continuation = null,
                    )
                }
            }
        }

    suspend fun libraryRecentActivity(): Result<LibraryPage> =
        runCatching {
            val continuation = LibraryFilter.FILTER_RECENT_ACTIVITY.value

            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        continuation = continuation,
                        setLogin = true,
                    ).body<BrowseResponse>()

            val gridItems =
                response.continuationContents
                    ?.sectionListContinuation
                    ?.contents
                    ?.firstOrNull()
                    ?.gridRenderer
                    ?.items

            if (gridItems == null) {
                return@runCatching LibraryPage(
                    items = emptyList(),
                    continuation = null,
                )
            }

            val items =
                gridItems
                    .mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            LibraryPage.fromMusicTwoRowItemRenderer(renderer)
                        }
                    }.toMutableList()

            /*
             * We need to fetch the artist page when accessing the library because it allows to have
             * a proper playEndpoint, which is needed to correctly report the playing indicator in
             * the home page.
             *
             * Despite this, we need to use the old thumbnail because it's the proper format for a
             * square picture, which is what we need.
             */
            items.forEachIndexed { index, item ->
                if (item is ArtistItem) {
                    YouTube.artist(item.id)
                        .getOrNull()?.artist?.let { fetchedArtist ->
                        items[index] = fetchedArtist.copy(thumbnail = item.thumbnail)
                    }
                }
            }

            LibraryPage(
                items = items,
                continuation = null,
            )
        }

    suspend fun getChartsPage(continuation: String? = null): Result<ChartsPage> =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "FEmusic_charts",
                        params = "ggMGCgQIgAQ%3D",
                        continuation = continuation,
                    ).body<BrowseResponse>()

            val sections = mutableListOf<ChartsPage.ChartSection>()

            response.contents
                ?.singleColumnBrowseResultsRenderer
                ?.tabs
                ?.firstOrNull()
                ?.tabRenderer
                ?.content
                ?.sectionListRenderer
                ?.contents
                ?.forEach { content ->

                    content.musicCarouselShelfRenderer?.let { renderer ->
                        val title =
                            renderer.header
                                ?.musicCarouselShelfBasicHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: return@forEach

                        val items =
                            renderer.contents
                                .mapNotNull { item ->
                                    when {
                                        item.musicResponsiveListItemRenderer != null -> {
                                            convertToChartItem(item.musicResponsiveListItemRenderer)
                                        }

                                        item.musicTwoRowItemRenderer != null -> {
                                            convertMusicTwoRowItem(item.musicTwoRowItemRenderer)
                                        }

                                        else -> {
                                            null
                                        }
                                    }
                                }.filterNotNull()

                        if (items.isNotEmpty()) {
                            sections.add(
                                ChartsPage.ChartSection(
                                    title = title,
                                    items = items,
                                    chartType = determineChartType(title),
                                ),
                            )
                        }
                    }

                    content.gridRenderer?.let { renderer ->
                        val title =
                            renderer.header
                                ?.gridHeaderRenderer
                                ?.title
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?: return@let

                        val items =
                            renderer.items
                                .mapNotNull { item ->
                                    item.musicTwoRowItemRenderer?.let { renderer ->
                                        convertMusicTwoRowItem(renderer)
                                    }
                                }.filterNotNull()

                        if (items.isNotEmpty()) {
                            sections.add(
                                ChartsPage.ChartSection(
                                    title = title,
                                    items = items,
                                    chartType = ChartsPage.ChartType.NEW_RELEASES,
                                ),
                            )
                        }
                    }
                }

            ChartsPage(
                sections = sections,
                continuation =
                    response.continuationContents
                        ?.sectionListContinuation
                        ?.continuations
                        ?.getContinuation(),
            )
        }

    private fun determineChartType(title: String): ChartsPage.ChartType =
        when {
            title.contains("Trending", ignoreCase = true) -> ChartsPage.ChartType.TRENDING
            title.contains("Top", ignoreCase = true) -> ChartsPage.ChartType.TOP
            else -> ChartsPage.ChartType.GENRE
        }

    private fun convertToChartItem(renderer: MusicResponsiveListItemRenderer): YTItem? {
        return try {
            when {
                renderer.flexColumns.size >= 3 && renderer.playlistItemData?.videoId != null -> {
                    val firstColumn =
                        renderer.flexColumns
                            .getOrNull(0)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text ?: return null

                    val secondColumn =
                        renderer.flexColumns
                            .getOrNull(1)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text ?: return null

                    val titleRun = firstColumn.runs?.firstOrNull() ?: return null
                    val title = titleRun.text.takeIf { it.isNotBlank() } ?: return null

                    val artists =
                        secondColumn.runs?.splitBySeparator()?.getOrNull(0)?.oddElements()?.map { run ->
                            Artist(
                                name = run.text.trim(),
                                id = run.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        } ?: emptyList()

                    val thirdColumn =
                        renderer.flexColumns
                            .getOrNull(2)
                            ?.musicResponsiveListItemFlexColumnRenderer
                            ?.text

                    SongItem(
                        id = renderer.playlistItemData.videoId,
                        title = title,
                        artists = artists,
                        thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        musicVideoType = renderer.musicVideoType,
                        explicit =
                            renderer.badges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                        chartPosition =
                            thirdColumn
                                ?.runs
                                ?.firstOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        chartChange = thirdColumn?.runs?.getOrNull(1)?.text,
                    )
                }

                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            println("Error converting chart item: ${e.message}\n${Json.encodeToString(renderer)}")
            null
        }
    }

    private fun convertMusicTwoRowItem(renderer: MusicTwoRowItemRenderer): YTItem? {
        return try {
            when {
                renderer.isSong -> {
                    val subtitle = renderer.subtitle?.runs ?: return null
                    SongItem(
                        id = renderer.navigationEndpoint.watchEndpoint?.videoId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            subtitle.splitBySeparator().firstOrNull()?.oddElements()?.map {
                                Artist(
                                    name = it.text.trim(),
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            } ?: emptyList(),
                        album = subtitle.splitBySeparator().getOrNull(1)?.firstOrNull()?.let {
                            Album(name = it.text, id = it.navigationEndpoint?.browseEndpoint?.browseId ?: "")
                        },
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        musicVideoType = renderer.musicVideoType,
                        explicit =
                            renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                    )
                }

                renderer.isAlbum -> {
                    AlbumItem(
                        browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        playlistId =
                            renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer
                                ?.content
                                ?.musicPlayButtonRenderer
                                ?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint
                                ?.playlistId ?: return null,
                        title =
                            renderer.title.runs
                                ?.firstOrNull()
                                ?.text ?: return null,
                        artists =
                            renderer.subtitle?.runs?.oddElements()?.drop(1)?.mapNotNull {
                                it.navigationEndpoint?.browseEndpoint?.browseId?.let { id ->
                                    Artist(name = it.text, id = id)
                                }
                            },
                        year =
                            renderer.subtitle
                                ?.runs
                                ?.lastOrNull()
                                ?.text
                                ?.toIntOrNull(),
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        explicit =
                            renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                    )
                }

                else -> {
                    null
                }
            }
        } catch (e: Exception) {
            println("Error converting two row item: ${e.message}\n${Json.encodeToString(renderer)}")
            null
        }
    }

    suspend fun toggleSongLibrary(
        videoId: String,
        addToLibrary: Boolean,
    ): Result<Boolean> =
        runCatching {
            if (addToLibrary) {
                addSongToLibrary(videoId).getOrThrow()
            } else {
                removeSongFromLibrary(videoId).getOrThrow()
            }
        }

    suspend fun addSongToLibrary(videoId: String): Result<Boolean> =
        runCatching {
            // Get fresh song data with menu tokens using next endpoint
            val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val song =
                nextResult.items.find { it.id == videoId }
                    ?: throw Exception("Song not found in next response")

            val addToken =
                song.libraryAddToken
                    ?: throw Exception("Add to library token not available")

            feedback(listOf(addToken)).getOrThrow()
        }

    suspend fun removeSongFromLibrary(videoId: String): Result<Boolean> =
        runCatching {
            // Get fresh song data with menu tokens using next endpoint
            val nextResult = next(WatchEndpoint(videoId = videoId)).getOrThrow()
            val song =
                nextResult.items.find { it.id == videoId }
                    ?: throw Exception("Song not found in next response")

            val removeToken =
                song.libraryRemoveToken
                    ?: throw Exception("Remove from library token not available")

            feedback(listOf(removeToken)).getOrThrow()
        }

    suspend fun feedback(tokens: List<String>): Result<Boolean> =
        runCatching {
            innerTube
                .feedback(WEB_REMIX, tokens)
                .body<FeedbackResponse>()
                .feedbackResponses
                .all { it.isProcessed }
        }


    suspend fun musicHistory() =
        runCatching {
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = "FEmusic_history",
                        setLogin = true,
                    ).body<BrowseResponse>()

            HistoryPage(
                sections =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.mapNotNull {
                            it.musicShelfRenderer?.let { musicShelfRenderer ->
                                HistoryPage.fromMusicShelfRenderer(musicShelfRenderer)
                            }
                        },
            )
        }
    suspend fun player(videoId: String, playlistId: String? = null, client: YouTubeClient, signatureTimestamp: Int? = null, poToken: String? = null,) : Result<PlayerResponse> =
    runCatching {
        innerTube.player(client, videoId, playlistId, signatureTimestamp, poToken).body<PlayerResponse>()
    }

    suspend fun next(
        endpoint: WatchEndpoint,
        continuation: String? = null,
    ): Result<NextResult> =
        runCatching {
            val response =
                innerTube
                    .next(
                        WEB_REMIX,
                        endpoint.videoId,
                        endpoint.playlistId,
                        endpoint.playlistSetVideoId,
                        endpoint.index,
                        endpoint.params,
                        continuation,
                    ).body<NextResponse>()

            val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
                    ?: response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer
                        .watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer
                        ?.content?.playlistPanelRenderer!!

            val title = response.contents.singleColumnMusicWatchNextResultsRenderer
                .tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer
                .content?.musicQueueRenderer?.header?.musicQueueHeaderRenderer?.subtitle
                ?.runs?.firstOrNull()?.text

            val items = playlistPanelRenderer.contents.mapNotNull { content ->
                content.playlistPanelVideoRenderer?.let(NextPage::fromPlaylistPanelVideoRenderer)
                    ?.let { it to content.playlistPanelVideoRenderer.selected }
                }
            val songs = items.map { it.first }
            val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

            // load automix items
            playlistPanelRenderer.contents
                .lastOrNull()
                ?.automixPreviewVideoRenderer
                ?.content
                ?.automixPlaylistVideoRenderer
                ?.navigationEndpoint
                ?.watchPlaylistEndpoint
                ?.let { watchPlaylistEndpoint ->
                    return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                        result.copy(
                            title = title,
                            items = songs + result.items,
                            lyricsEndpoint =
                                response.contents.singleColumnMusicWatchNextResultsRenderer
                                    .tabbedRenderer
                                    .watchNextTabbedResultsRenderer
                                    .tabs
                                    .getOrNull(
                                        1,
                                    )?.tabRenderer
                                    ?.endpoint
                                    ?.browseEndpoint,
                            relatedEndpoint =
                                response.contents.singleColumnMusicWatchNextResultsRenderer
                                    .tabbedRenderer
                                    .watchNextTabbedResultsRenderer
                                    .tabs
                                    .getOrNull(
                                        2,
                                    )?.tabRenderer
                                    ?.endpoint
                                    ?.browseEndpoint,
                            currentIndex = currentIndex,
                            endpoint = watchPlaylistEndpoint,
                        )
                    }
                }
            NextResult(
                title = title,
                items = songs,
                currentIndex = currentIndex,
                lyricsEndpoint =
                    response.contents.singleColumnMusicWatchNextResultsRenderer
                        .tabbedRenderer
                        .watchNextTabbedResultsRenderer
                        .tabs
                        .getOrNull(
                            1,
                        )?.tabRenderer
                        ?.endpoint
                        ?.browseEndpoint,
                relatedEndpoint =
                    response.contents.singleColumnMusicWatchNextResultsRenderer
                        .tabbedRenderer
                        .watchNextTabbedResultsRenderer
                        .tabs
                        .getOrNull(
                            2,
                        )?.tabRenderer
                        ?.endpoint
                        ?.browseEndpoint,
                continuation = playlistPanelRenderer.continuations?.getContinuation(),
                endpoint = endpoint,
            )
        }
    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params).body<BrowseResponse>()
            response.contents
                ?.sectionListRenderer
                ?.contents
                ?.firstOrNull { it.musicDescriptionShelfRenderer != null }
                ?.musicDescriptionShelfRenderer
                ?.description
                ?.runs
                ?.joinToString(separator = "") { it.text }
        }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, endpoint.browseId).body<BrowseResponse>()
            val songs = mutableListOf<SongItem>()
            val albums = mutableListOf<AlbumItem>()
            val artists = mutableListOf<ArtistItem>()
            val playlists = mutableListOf<PlaylistItem>()
            response.contents?.sectionListRenderer?.contents?.forEach { sectionContent ->
                sectionContent.musicCarouselShelfRenderer?.contents?.forEach { content ->
                    when (
                        val item =
                            content.musicResponsiveListItemRenderer?.let(RelatedPage.Companion::fromMusicResponsiveListItemRenderer)
                                ?: content.musicTwoRowItemRenderer?.let(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                    ) {
                        is SongItem -> {
                            if (content.musicResponsiveListItemRenderer
                                    ?.overlay
                                    ?.musicItemThumbnailOverlayRenderer
                                    ?.content
                                    ?.musicPlayButtonRenderer
                                    ?.playNavigationEndpoint
                                    ?.watchEndpoint
                                    ?.watchEndpointMusicSupportedConfigs
                                    ?.watchEndpointMusicConfig
                                    ?.musicVideoType == MUSIC_VIDEO_TYPE_ATV
                            ) {
                                songs.add(item)
                            }
                        }

                        is AlbumItem -> {
                            albums.add(item)
                        }

                        is ArtistItem -> {
                            artists.add(item)
                        }

                        is PlaylistItem -> {
                            playlists.add(item)
                        }

                        is PodcastItem, is EpisodeItem -> {}

                        null -> {}
                    }
                }
            }
            RelatedPage(songs, albums, artists, playlists)
        }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null,
    ): Result<List<SongItem>> =
        runCatching {
            if (videoIds != null) {
                assert(videoIds.size <= MAX_GET_QUEUE_SIZE) // Max video limit
            }
            innerTube
                .getQueue(WEB_REMIX, videoIds, playlistId)
                .body<GetQueueResponse>()
                .queueDatas
                .mapNotNull {
                    it.content.playlistPanelVideoRenderer?.let { renderer ->
                        NextPage.fromPlaylistPanelVideoRenderer(renderer)
                    }
                }
        }

    suspend fun transcript(videoId: String): Result<String> =
        runCatching {
            val response = innerTube.getTranscript(WEB, videoId).body<GetTranscriptResponse>()
            response.actions
                ?.firstOrNull()
                ?.updateEngagementPanelAction
                ?.content
                ?.transcriptRenderer
                ?.body
                ?.transcriptBodyRenderer
                ?.cueGroups
                ?.joinToString(
                    separator = "\n",
                ) { group ->
                    val time =
                        group.transcriptCueGroupRenderer.cues[0]
                            .transcriptCueRenderer.startOffsetMs
                    val text =
                        group.transcriptCueGroupRenderer.cues[0]
                            .transcriptCueRenderer.cue.simpleText
                            .trim('♪')
                            .trim(' ')
                    "[%02d:%02d.%03d]$text".format(time / 60000, (time / 1000) % 60, time % 1000)
                }!!
        }

    suspend fun visitorData(): Result<String> =
        runCatching {
            Json
                .parseToJsonElement(innerTube.getSwJsData().bodyAsText().substring(5))
                .jsonArray[0]
                .jsonArray[2]
                .jsonArray
                .first {
                    (it as? JsonPrimitive)?.contentOrNull?.let { candidate ->
                        YouTube.VISITOR_DATA_REGEX.containsMatchIn(candidate)
                    } ?: false
                }.jsonPrimitive.content
        }

    suspend fun accountInfo(): Result<AccountInfo> =
        runCatching {
            innerTube
                .accountMenu(WEB_REMIX)
                .body<AccountMenuResponse>()
                .actions[0]
                .openPopupAction.popup.multiPageMenuRenderer
                .header
                ?.activeAccountHeaderRenderer?.toAccountInfo()!!
        }

    suspend fun likeVideo(
        videoId: String,
        like: Boolean,
    ) = runCatching {
        if (like) {
            innerTube.likeVideo(WEB_REMIX, videoId)
        } else {
            innerTube.unlikeVideo(WEB_REMIX, videoId)
        }
    }

    suspend fun likePlaylist(
        playlistId: String,
        like: Boolean,
    ) = runCatching {
        if (like) {
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        } else {
            innerTube.unlikePlaylist(WEB_REMIX, playlistId)
        }
    }

    suspend fun subscribeChannel(
        channelId: String,
        subscribe: Boolean,
        params: String? = null,
    ) = runCatching {
        // Default params from YouTube Music API - required for subscription to work
        val subscribeParams = params ?: "EgIIAhgA"
        if (subscribe) {
            innerTube.subscribeChannel(WEB_REMIX, channelId, subscribeParams)
        } else {
            innerTube.unsubscribeChannel(WEB_REMIX, channelId, subscribeParams)
        }
    }

    suspend fun podcast(podcastId: String): Result<PodcastPage> = podcastWithDebug(podcastId) { }

    suspend fun podcastWithDebug(
        podcastId: String,
        log: (String) -> Unit,
    ): Result<PodcastPage> =
        runCatching {
            println("Fetching podcast with ID: $podcastId")
            val response =
                innerTube
                    .browse(
                        client = WEB_REMIX,
                        browseId = podcastId,
                        setLogin = true,
                    ).body<BrowseResponse>()

            println("Response received, twoColumnBrowseResultsRenderer: ${response.contents?.twoColumnBrowseResultsRenderer != null}")
            println("singleColumnBrowseResultsRenderer: ${response.contents?.singleColumnBrowseResultsRenderer != null}")

            // Try twoColumn first (standard layout)
            var header =
                response.contents
                    ?.twoColumnBrowseResultsRenderer
                    ?.tabs
                    ?.firstOrNull()
                    ?.tabRenderer
                    ?.content
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
                    ?.musicResponsiveHeaderRenderer

            // Fallback to singleColumn layout
            if (header == null) {
                header =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicResponsiveHeaderRenderer
                println("Using singleColumn layout, header found: ${header != null}")
            }

            println("Header title: ${header?.title?.runs?.firstOrNull()?.text}")

            // Debug: Log button structure
            header?.buttons?.forEachIndexed { i, button ->
                println(
                    "[PODCAST] Button[$i]: menuRenderer=${button.menuRenderer != null}, toggleButtonRenderer=${button.toggleButtonRenderer != null}, playButtonRenderer=${button.musicPlayButtonRenderer != null}",
                )
                button.menuRenderer?.items?.forEachIndexed { j, item ->
                    println(
                        "[PODCAST] Button[$i].menuItems[$j]: toggle=${item.toggleMenuServiceItemRenderer?.defaultIcon?.iconType}, nav=${item.menuNavigationItemRenderer?.icon?.iconType}",
                    )
                    // Check for SUBSCRIBE button (like artists have)
                    if (item.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE") {
                        val channelIds =
                            item.toggleMenuServiceItemRenderer.defaultServiceEndpoint.subscribeEndpoint
                                ?.channelIds
                        println("[PODCAST] Found SUBSCRIBE button! channelIds=$channelIds")
                    }
                }
                button.toggleButtonRenderer?.let { toggle ->
                    println(
                        "[PODCAST] Button[$i].toggleButtonRenderer: defaultIcon=${toggle.defaultIcon?.iconType}, defaultToken=${toggle.defaultServiceEndpoint?.feedbackEndpoint?.feedbackToken?.take(
                            30,
                        )}, subscribeChannelIds=${toggle.defaultServiceEndpoint?.subscribeEndpoint?.channelIds}",
                    )
                }
            }

            // Extract channelId and subscription state for subscription (like artists)
            val subscribeToggle =
                header
                    ?.buttons
                    ?.flatMap { button ->
                        button.menuRenderer?.items ?: emptyList()
                    }?.find {
                        it.toggleMenuServiceItemRenderer?.defaultIcon?.iconType == "SUBSCRIBE"
                    }?.toggleMenuServiceItemRenderer
            val channelId =
                subscribeToggle
                    ?.defaultServiceEndpoint
                    ?.subscribeEndpoint
                    ?.channelIds
                    ?.firstOrNull()
            // isSelected indicates user is currently subscribed (toggle is in "toggled" state)
            val isChannelSubscribed = subscribeToggle?.isSelected == true
            println("[PODCAST] Extracted channelId for subscription: $channelId, isSubscribed: $isChannelSubscribed")

            // Extract library tokens from the header's menu buttons OR toggle buttons
            var libraryTokens =
                header
                    ?.buttons
                    ?.flatMap { button ->
                        button.menuRenderer?.items ?: emptyList()
                    }?.let { menuItems ->
                        PageHelper.extractLibraryTokensFromMenuItems(menuItems)
                    }

            // Also check for standalone toggle buttons (used by some podcasts)
            if (libraryTokens?.addToken == null && libraryTokens?.removeToken == null) {
                header?.buttons?.forEach { button ->
                    button.toggleButtonRenderer?.let { toggle ->
                        val iconType = toggle.defaultIcon?.iconType
                        if (iconType != null && PageHelper.isLibraryIcon(iconType)) {
                            val defaultToken = toggle.defaultServiceEndpoint?.feedbackEndpoint?.feedbackToken
                            val toggledToken = toggle.toggledServiceEndpoint?.feedbackEndpoint?.feedbackToken
                            libraryTokens =
                                if (PageHelper.isAddLibraryIcon(iconType)) {
                                    // BOOKMARK_BORDER: default=add, toggled=remove
                                    PageHelper.LibraryFeedbackTokens(defaultToken, toggledToken)
                                } else {
                                    // BOOKMARK: default=remove, toggled=add
                                    PageHelper.LibraryFeedbackTokens(toggledToken, defaultToken)
                                }
                            println(
                                "[PODCAST] Found toggle button with library tokens - add: ${libraryTokens.addToken != null}, remove: ${libraryTokens.removeToken != null}",
                            )
                        }
                    }
                }
            }
            println("[PODCAST] Library tokens - add: ${libraryTokens?.addToken != null}, remove: ${libraryTokens?.removeToken != null}")

            val podcastItem =
                PodcastItem(
                    id = podcastId,
                    title =
                        header
                            ?.title
                            ?.runs
                            ?.firstOrNull()
                            ?.text ?: "",
                    author =
                        header?.straplineTextOne?.runs?.firstOrNull()?.let {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId,
                            )
                        },
                    episodeCountText =
                        header
                            ?.secondSubtitle
                            ?.runs
                            ?.firstOrNull()
                            ?.text,
                    thumbnail =
                        header
                            ?.thumbnail
                            ?.musicThumbnailRenderer
                            ?.thumbnail
                            ?.thumbnails
                            ?.lastOrNull()
                            ?.url,
                    playEndpoint =
                        header
                            ?.buttons
                            ?.find {
                                it.menuRenderer
                                    ?.items
                                    ?.firstOrNull()
                                    ?.menuNavigationItemRenderer
                                    ?.icon
                                    ?.iconType == "PLAY_ARROW"
                            }?.menuRenderer
                            ?.items
                            ?.firstOrNull()
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    shuffleEndpoint =
                        header
                            ?.buttons
                            ?.find {
                                it.menuRenderer?.items?.any { item ->
                                    item.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                                } ==
                                        true
                            }?.menuRenderer
                            ?.items
                            ?.find { it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE" }
                            ?.menuNavigationItemRenderer
                            ?.navigationEndpoint
                            ?.watchPlaylistEndpoint,
                    libraryAddToken = libraryTokens?.addToken,
                    libraryRemoveToken = libraryTokens?.removeToken,
                    channelId = channelId,
                )

            // Try twoColumn for episodes
            val secondaryContents = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents
            println("secondaryContents null: ${secondaryContents == null}")
            println("secondaryContents.sectionListRenderer null: ${secondaryContents?.sectionListRenderer == null}")
            println("sectionListRenderer.contents size: ${secondaryContents?.sectionListRenderer?.contents?.size ?: 0}")

            secondaryContents?.sectionListRenderer?.contents?.forEachIndexed { index, content ->
                println(
                    "Content[$index]: musicShelfRenderer=${content.musicShelfRenderer != null}, musicPlaylistShelfRenderer=${content.musicPlaylistShelfRenderer != null}, gridRenderer=${content.gridRenderer != null}",
                )
                content.musicShelfRenderer?.let { shelf ->
                    println("musicShelfRenderer.contents size: ${shelf.contents?.size ?: 0}")
                }
                content.musicPlaylistShelfRenderer?.let { shelf ->
                    println("musicPlaylistShelfRenderer.contents size: ${shelf.contents.size}")
                }
            }

            var episodeContents =
                secondaryContents
                    ?.sectionListRenderer
                    ?.contents
                    ?.firstOrNull()
                    ?.musicShelfRenderer
                    ?.contents

            // Try musicPlaylistShelfRenderer
            if (episodeContents == null) {
                episodeContents =
                    secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicPlaylistShelfRenderer
                        ?.contents
                println("Trying musicPlaylistShelfRenderer: ${episodeContents?.size ?: 0}")
            }

            // Fallback to singleColumn
            if (episodeContents == null) {
                episodeContents =
                    response.contents
                        ?.singleColumnBrowseResultsRenderer
                        ?.tabs
                        ?.firstOrNull()
                        ?.tabRenderer
                        ?.content
                        ?.sectionListRenderer
                        ?.contents
                        ?.find { it.musicShelfRenderer != null }
                        ?.musicShelfRenderer
                        ?.contents
                println("Using singleColumn for episodes, found: ${episodeContents?.size ?: 0}")
            }

            println("Episode contents count: ${episodeContents?.size ?: 0}")

            // Get episodes from musicMultiRowListItemRenderer (used for podcasts)
            val multiRowItems = episodeContents?.mapNotNull { it.musicMultiRowListItemRenderer } ?: emptyList()
            println("multiRowItems count: ${multiRowItems.size}")

            multiRowItems.take(2).forEachIndexed { idx, renderer ->
                println("Episode[$idx] title: ${renderer.title?.runs?.firstOrNull()?.text}")
                println("Episode[$idx] subtitle: ${renderer.subtitle?.runs?.map { it.text }}")
                println("Episode[$idx] videoId: ${renderer.onTap?.watchEndpoint?.videoId}")
                println("Episode[$idx] thumbnail: ${renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()}")
            }

            val episodes =
                multiRowItems.mapNotNull { renderer ->
                    PodcastPage.fromMusicMultiRowListItemRenderer(renderer, podcastItem)
                }

            println("Parsed episodes: ${episodes.size}")

            PodcastPage(
                podcast = podcastItem,
                episodes = episodes,
                continuation =
                    response.contents
                        ?.twoColumnBrowseResultsRenderer
                        ?.secondaryContents
                        ?.sectionListRenderer
                        ?.contents
                        ?.firstOrNull()
                        ?.musicShelfRenderer
                        ?.continuations
                        ?.getContinuation()
                        ?: response.contents
                            ?.singleColumnBrowseResultsRenderer
                            ?.tabs
                            ?.firstOrNull()
                            ?.tabRenderer
                            ?.content
                            ?.sectionListRenderer
                            ?.contents
                            ?.find { it.musicShelfRenderer != null }
                            ?.musicShelfRenderer
                            ?.continuations
                            ?.getContinuation(),
                isChannelSubscribed = isChannelSubscribed,
            )
        }

    suspend fun savePodcast(
        podcastId: String,
        save: Boolean,
    ) = runCatching {
        val playlistId = podcastId.removePrefix("MPSP")
        println("[PODCAST_API] savePodcast: podcastId=$podcastId, playlistId=$playlistId, save=$save")
        if (save) {
            innerTube.likePlaylist(WEB_REMIX, playlistId)
        } else {
            innerTube.unlikePlaylist(WEB_REMIX, playlistId)
        }
    }
    suspend fun libraryPodcastChannels(): Result<LibraryPage> =
        library("FEmusic_library_non_music_audio_channels_list")
            .onFailure { e -> println("[PODCAST_API] libraryPodcastChannels FAILED: $e") }
            .onSuccess { println("[PODCAST_API] libraryPodcastChannels SUCCESS: ${it.items.size} items") }

    suspend fun libraryPodcastEpisodes(): Result<LibraryPage> =
        library("FEmusic_library_non_music_audio_list")
            .onFailure { e -> println("[PODCAST_API] libraryPodcastEpisodes FAILED: $e") }
            .onSuccess { println("[PODCAST_API] libraryPodcastEpisodes SUCCESS: ${it.items.size} items") }

    /**
     * Fetch saved podcast shows from library.
     * Uses FEmusic_library_non_music_audio_list and filters to only PodcastItem.
     */
    suspend fun savedPodcastShows(): Result<List<PodcastItem>> =
        runCatching {
            val libraryPage = libraryPodcastEpisodes().getOrThrow()
            libraryPage.items.filterIsInstance<PodcastItem>()
        }

    suspend fun addEpisodeToSavedEpisodes(episodeId: String) = runCatching {
        innerTube.addToPlaylist(WEB_REMIX, "SE", episodeId)
    }

    suspend fun removeEpisodeFromSavedEpisodes(episodeId: String, setVideoId: String) = runCatching {
        innerTube.removeFromPlaylist(WEB_REMIX, "SE", episodeId, setVideoId)
    }

    suspend fun episodesForLater() = playlist("SE")

    suspend fun removeFromPlaylist(
        playlistId: String,
        videoId: String,
        setVideoId: String,
    ) = runCatching {
        innerTube.removeFromPlaylist(WEB_REMIX, playlistId, videoId, setVideoId)
    }

    suspend fun getChannelId(browseId: String): String {
        artist(browseId).onSuccess {
            return it.artist.channelId ?: ""
        }
        return ""
    }

    @JvmInline
    value class SearchFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_SONG = SearchFilter("EgWKAQIIAWoKEAkQBRAKEAMQBA%3D%3D")
            val FILTER_VIDEO = SearchFilter("EgWKAQIQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ALBUM = SearchFilter("EgWKAQIYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_ARTIST = SearchFilter("EgWKAQIgAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_FEATURED_PLAYLIST = SearchFilter("EgeKAQQoADgBagwQDhAKEAMQBRAJEAQ%3D")
            val FILTER_COMMUNITY_PLAYLIST = SearchFilter("EgeKAQQoAEABagoQAxAEEAoQCRAF")
            val FILTER_PODCAST = SearchFilter("EgWKAQJQAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_EPISODE = SearchFilter("EgWKAQJYAWoKEAkQChAFEAMQBA%3D%3D")
            val FILTER_PROFILE = SearchFilter("EgWKAQJYAWoSEAUQCRADEAQQEBAVEAoQDhAR")
        }
    }

    @JvmInline
    value class LibraryFilter(
        val value: String,
    ) {
        companion object {
            val FILTER_RECENT_ACTIVITY = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpYnJhcnlfbGFuZGluZxoQZ2dNR0tnUUlCaEFCb0FZQg%3D%3D")
            val FILTER_RECENTLY_PLAYED = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpYnJhcnlfbGFuZGluZxoQZ2dNR0tnUUlCUkFCb0FZQg%3D%3D")
            val FILTER_PLAYLISTS_ALPHABETICAL = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpa2VkX3BsYXlsaXN0cxoQZ2dNR0tnUUlBUkFBb0FZQg%3D%3D")
            val FILTER_PLAYLISTS_RECENTLY_SAVED = LibraryFilter("4qmFsgIrEhdGRW11c2ljX2xpa2VkX3BsYXlsaXN0cxoQZ2dNR0tnUUlBQkFCb0FZQg%3D%3D")
        }
    }

    fun getNewPipeStreamUrls(videoId: String): List<Pair<Int, String>> =
        if (ENABLE_NEWPIPE_STREAM_INFO_EXTRACTOR) {
            NewPipeExtractor.newPipePlayer(videoId)
        } else {
            emptyList()
        }

    fun newPipePlayer(
        videoId: String,
        tempRes: PlayerResponse,
    ): PlayerResponse? {
        if (tempRes.playabilityStatus.status != "OK") {
            return null
        }

        val streamsList = getNewPipeStreamUrls(videoId)

        val decodedSigResponse =
            tempRes.copy(
                streamingData =
                    tempRes.streamingData?.copy(
                        formats =
                            tempRes.streamingData.formats?.map { format ->
                                format.copy(
                                    url = NewPipeUtils.getStreamUrl(format, videoId).getOrNull()
                                        ?: streamsList.find { it.first == format.itag }?.second
                                        ?: format.url,
                                )
                            },
                        adaptiveFormats =
                            tempRes.streamingData.adaptiveFormats.map { adaptiveFormat ->
                                adaptiveFormat.copy(
                                    url = NewPipeUtils.getStreamUrl(adaptiveFormat, videoId).getOrNull()
                                        ?: streamsList.find { it.first == adaptiveFormat.itag }?.second
                                        ?: adaptiveFormat.url,
                                )
                            },
                    ),
            )

        val urlList =
            (
                    decodedSigResponse.streamingData
                        ?.adaptiveFormats
                        ?.mapNotNull { it.url }
                        ?.toMutableList() ?: mutableListOf()
                    ).apply {
                    decodedSigResponse.streamingData
                        ?.formats
                        ?.mapNotNull { it.url }
                        ?.let { addAll(it) }
                }

        return if (urlList.isNotEmpty()) {
            decodedSigResponse
        } else {
            null
        }
    }


    const val MAX_GET_QUEUE_SIZE = 1000

    private val VISITOR_DATA_REGEX = Regex("^Cg[t|s]")

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}












