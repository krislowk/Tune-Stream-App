package com.vynce.vynceclient

import com.vynce.vynceclient.models.AccountInfo
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.GridRenderer
import com.vynce.vynceclient.models.MusicCarouselShelfRenderer
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SearchSuggestions
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.WatchEndpoint
import com.vynce.vynceclient.models.WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig.Companion.MUSIC_VIDEO_TYPE_ATV
import com.vynce.vynceclient.models.YtClient.Companion.ANDROID_MUSIC
import com.vynce.vynceclient.models.YtClient.Companion.IOS
import com.vynce.vynceclient.models.YtClient.Companion.TVHTML5
import com.vynce.vynceclient.models.YtClient.Companion.WEB
import com.vynce.vynceclient.models.YtClient.Companion.WEB_REMIX
import com.vynce.vynceclient.models.YtLocale
import com.vynce.vynceclient.models.getContinuation
import com.vynce.vynceclient.models.oddElements
import com.vynce.vynceclient.models.response.PlayerResponse
import com.vynce.vynceclient.pages.AlbumPage
import com.vynce.vynceclient.pages.ArtistItemsContinuationPage
import com.vynce.vynceclient.pages.ArtistItemsPage
import com.vynce.vynceclient.pages.ArtistPage
import com.vynce.vynceclient.pages.BrowseResult
import com.vynce.vynceclient.pages.ExplorePage
import com.vynce.vynceclient.pages.HomePage
import com.vynce.vynceclient.pages.MoodAndGenres
import com.vynce.vynceclient.pages.NewReleaseAlbumPage
import com.vynce.vynceclient.pages.NextPage
import com.vynce.vynceclient.pages.NextResult
import com.vynce.vynceclient.pages.PlaylistContinuationPage
import com.vynce.vynceclient.pages.PlaylistPage
import com.vynce.vynceclient.pages.RelatedPage
import com.vynce.vynceclient.pages.SearchPage
import com.vynce.vynceclient.pages.SearchResult
import com.vynce.vynceclient.pages.SearchSuggestionPage
import com.vynce.vynceclient.pages.SearchSummary
import com.vynce.vynceclient.pages.SearchSummaryPage
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.net.Proxy

object Youtube {
    val innerTube by lazy { Innertube() }

    var locale: YtLocale
        get() = innerTube.locale
        set(value) {
            innerTube.locale = value
        }
    var visitorData: String
        get() = innerTube.visitorData
        set(value) {
            innerTube.visitorData = value
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
    var useLoginForBrowse: Boolean
        get() = innerTube.useLoginForBrowse
        set(value) {
            innerTube.useLoginForBrowse = value
        }

    suspend fun searchSuggestions(query: String): Result<SearchSuggestions> = runCatching {
        val response =
            innerTube.getSearchSuggestions(WEB_REMIX, query)
        SearchSuggestions(
            queries = response.contents?.getOrNull(0)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull { content ->
                content.searchSuggestionRenderer?.suggestion?.runs?.joinToString(separator = "") { it.text }
            }.orEmpty(),
            recommendedItems = response.contents?.getOrNull(1)?.searchSuggestionsSectionRenderer?.contents?.mapNotNull {
                it.musicResponsiveListItemRenderer?.let { renderer ->
                    SearchSuggestionPage.fromMusicResponsiveListItemRenderer(renderer)
                }
            }.orEmpty()
        )
    }

    suspend fun searchSummary(query: String): Result<SearchSummaryPage> = runCatching {
        val response = innerTube.search(WEB_REMIX, query)
        SearchSummaryPage(
            summaries = response.contents?.tabbedSearchResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { it ->
                if (it.musicCardShelfRenderer != null)
                    SearchSummary(
                        title = it.musicCardShelfRenderer.header.musicCardShelfHeaderBasicRenderer.title.runs?.firstOrNull()?.text
                            ?: return@mapNotNull null,
                        items = listOfNotNull(SearchSummaryPage.fromMusicCardShelfRenderer(it.musicCardShelfRenderer))
                            .plus(
                                it.musicCardShelfRenderer.contents
                                    ?.mapNotNull { it.musicResponsiveListItemRenderer }
                                    ?.mapNotNull(SearchSummaryPage.Companion::fromMusicResponsiveListItemRenderer)
                                    .orEmpty()
                            )
                            .distinctBy { it.id }
                            .ifEmpty { null } ?: return@mapNotNull null
                    )
                else
                    SearchSummary(
                        title = it.musicShelfRenderer?.title?.runs?.firstOrNull()?.text
                            ?: return@mapNotNull null,
                        items = it.musicShelfRenderer.contents
                            ?.mapNotNull {
                                SearchSummaryPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                            }
                            ?.distinctBy { it.id }
                            ?.ifEmpty { null } ?: return@mapNotNull null
                    )
            }!!
        )
    }

    suspend fun search(query: String, filter: SearchFilter): Result<SearchResult> = runCatching {
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

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        val response =
            innerTube.search(WEB_REMIX, continuation = continuation)
        SearchResult(
            items = response.continuationContents?.musicShelfContinuation?.contents
                ?.mapNotNull {
                    SearchPage.toYTItem(it.musicResponsiveListItemRenderer)
                }!!,
            continuation = response.continuationContents.musicShelfContinuation.continuations?.getContinuation()
        )
    }

    suspend fun album(browseId: String, withSongs: Boolean = true): Result<AlbumPage> =
        runCatching {
            val response = innerTube.browse(WEB_REMIX, browseId)
            val playlistId =
                response.microformat?.microformatDataRenderer?.urlCanonical?.substringAfterLast('=')!!
            AlbumPage(
                album = AlbumItem(
                    browseId = browseId,
                    playlistId = playlistId,
                    title = response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                    artists = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.straplineTextOne?.runs?.oddElements()
                        ?.map {
                            Artist(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }!!,
                    year = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = response.contents.twoColumnBrowseResultsRenderer.tabs.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicResponsiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url!!,
                ),
                songs = if (withSongs) albumSongs(playlistId).getOrThrow() else emptyList(),
                otherVersions = response.contents.twoColumnBrowseResultsRenderer.secondaryContents?.sectionListRenderer?.contents?.getOrNull(
                    1
                )?.musicCarouselShelfRenderer?.contents
                    ?.mapNotNull { it.musicTwoRowItemRenderer }
                    ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
                    .orEmpty()
            )
        }

    suspend fun albumSongs(playlistId: String): Result<List<SongItem>> = runCatching {
        var response = innerTube.browse(WEB_REMIX, "VL$playlistId")
        val songs = response.contents?.twoColumnBrowseResultsRenderer
            ?.secondaryContents?.sectionListRenderer
            ?.contents?.firstOrNull()
            ?.musicPlaylistShelfRenderer?.contents
            ?.mapNotNull {
                AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
            }!!
            .toMutableList()
        var continuation =
            response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .contents.firstOrNull()?.musicPlaylistShelfRenderer?.continuations?.getContinuation()
        while (continuation != null) {
            response = innerTube.browse(
                client = WEB_REMIX,
                continuation = continuation,
            )
            songs += response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
            }.orEmpty()
            continuation =
                response.continuationContents?.musicPlaylistShelfContinuation?.continuations?.getContinuation()
        }
        songs
    }

    suspend fun artist(browseId: String): Result<ArtistPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId)
        ArtistPage(
            artist = ArtistItem(
                id = browseId,
                title = response.header?.musicImmersiveHeaderRenderer?.title?.runs?.firstOrNull()?.text
                    ?: response.header?.musicVisualHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                thumbnail = response.header.musicImmersiveHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl()
                    ?: response.header.musicVisualHeaderRenderer?.foregroundThumbnail?.musicThumbnailRenderer?.getThumbnailUrl()!!,
                shuffleEndpoint = response.header.musicImmersiveHeaderRenderer?.playButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint,
                radioEndpoint = response.header.musicImmersiveHeaderRenderer?.startRadioButton?.buttonRenderer?.navigationEndpoint?.watchEndpoint
            ),
            sections = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                ?.tabRenderer?.content?.sectionListRenderer?.contents
                ?.mapNotNull(ArtistPage::fromSectionListRendererContent)!!,
            description = response.header.musicImmersiveHeaderRenderer?.description?.runs?.firstOrNull()?.text
        )
    }

    suspend fun artistItems(endpoint: BrowseEndpoint): Result<ArtistItemsPage> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params)
        val gridRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
            ?.gridRenderer
        if (gridRenderer != null) {
            ArtistItemsPage(
                title = gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text.orEmpty(),
                items = gridRenderer.items.mapNotNull {
                    it.musicTwoRowItemRenderer?.let { renderer ->
                        ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                    }
                },
                continuation = gridRenderer.continuations?.getContinuation()
            )
        } else {
            ArtistItemsPage(
                title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text!!,
                items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                    ?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    }!!,
                continuation = response.contents.singleColumnBrowseResultsRenderer.tabs.firstOrNull()
                    ?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
                    ?.musicPlaylistShelfRenderer?.continuations?.getContinuation()
            )
        }
    }

    suspend fun artistItemsContinuation(continuation: String): Result<ArtistItemsContinuationPage> =
        runCatching {
            val response =
                innerTube.browse(WEB_REMIX, continuation = continuation)
            val gridContinuation = response.continuationContents?.gridContinuation
            if (gridContinuation != null) {
                ArtistItemsContinuationPage(
                    items = gridContinuation.items.mapNotNull {
                        it.musicTwoRowItemRenderer?.let { renderer ->
                            ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer)
                        }
                    },
                    continuation = gridContinuation.continuations?.getContinuation()
                )
            } else {
                ArtistItemsContinuationPage(
                    items = response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                        ArtistItemsPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                    }!!,
                    continuation = response.continuationContents.musicPlaylistShelfContinuation.continuations?.getContinuation()
                )
            }
        }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "VL$playlistId",
            setLogin = true
        )
        val base =
            response.contents?.twoColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()
        val header = base?.musicResponsiveHeaderRenderer
            ?: base?.musicEditablePlaylistDetailHeaderRenderer?.header?.musicResponsiveHeaderRenderer
        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = header?.title?.runs?.firstOrNull()?.text!!,
                author = header.straplineTextOne?.runs?.firstOrNull()?.let {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                },
                songCountText = header.secondSubtitle?.runs?.firstOrNull()?.text,
                thumbnail = header.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.lastOrNull()?.url!!,
                playEndpoint = null,
                shuffleEndpoint = header.buttons?.lastOrNull()?.menuRenderer?.items?.firstOrNull()?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!,
                radioEndpoint = header.buttons.lastOrNull()?.menuRenderer?.items!!.find {
                    it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint!!
            ),
            songs = response.contents?.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
                ?.firstOrNull()?.musicPlaylistShelfRenderer?.contents?.mapNotNull {
                    PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
                }!!,
            songsContinuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .contents.firstOrNull()
                ?.musicPlaylistShelfRenderer?.continuations?.getContinuation(),
            continuation = response.contents.twoColumnBrowseResultsRenderer.secondaryContents.sectionListRenderer
                .continuations?.getContinuation()
        )
    }

    suspend fun playlistContinuation(continuation: String) = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            continuation = continuation,
            setLogin = true
        )
        PlaylistContinuationPage(
            songs = response.continuationContents?.musicPlaylistShelfContinuation?.contents?.mapNotNull {
                PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
            }!!,
            continuation = response.continuationContents.musicPlaylistShelfContinuation.continuations?.getContinuation()
        )
    }

    suspend fun home(browseId: String = "FEmusic_home", params: String? = null): Result<HomePage> = runCatching {
        var response = innerTube.browse(WEB_REMIX, browseId = browseId, params = params, setLogin = useLoginForBrowse)
        val sectionListRenderer = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()
            ?.tabRenderer?.content?.sectionListRenderer

        val continuation = sectionListRenderer?.continuations?.getContinuation()

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

        if (continuation != null) {
            response = innerTube.browse(WEB_REMIX, continuation = continuation, setLogin = useLoginForBrowse)
            sections += response.continuationContents?.sectionListContinuation?.contents
                ?.mapNotNull { it.musicCarouselShelfRenderer }
                ?.mapNotNull {
                    HomePage.Section.fromMusicCarouselShelfRenderer(it)
                }.orEmpty()
        }
        HomePage(sections, filters)
    }

    suspend fun explore(): Result<ExplorePage> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, browseId = "FEmusic_explore")
        ExplorePage(
            newReleaseAlbums = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_new_releases_albums"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicTwoRowItemRenderer }
                ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer).orEmpty(),
            moodAndGenres = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.find {
                it.musicCarouselShelfRenderer?.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint?.browseId == "FEmusic_moods_and_genres"
            }?.musicCarouselShelfRenderer?.contents
                ?.mapNotNull { it.musicNavigationButtonRenderer }
                ?.mapNotNull(MoodAndGenres.Companion::fromMusicNavigationButtonRenderer)
                .orEmpty()
        )
    }

    suspend fun newReleaseAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_new_releases_albums")
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items
            ?.mapNotNull { it.musicTwoRowItemRenderer }
            ?.mapNotNull(NewReleaseAlbumPage::fromMusicTwoRowItemRenderer)
            .orEmpty()
    }

    suspend fun moodAndGenres(): Result<List<MoodAndGenres>> = runCatching {
        val response = innerTube.browse(WEB_REMIX, browseId = "FEmusic_moods_and_genres")
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents!!
            .mapNotNull(MoodAndGenres.Companion::fromSectionListRendererContent)
    }

    suspend fun browse(browseId: String, params: String?): Result<BrowseResult> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, browseId = browseId, params = params)
        BrowseResult(
            title = response.header?.musicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
            items = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.mapNotNull { content ->
                when {
                    content.gridRenderer != null -> {
                        BrowseResult.Item(
                            title = content.gridRenderer.header?.gridHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.gridRenderer.items
                                .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    content.musicCarouselShelfRenderer != null -> {
                        BrowseResult.Item(
                            title = content.musicCarouselShelfRenderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text,
                            items = content.musicCarouselShelfRenderer.contents
                                .mapNotNull(MusicCarouselShelfRenderer.Content::musicTwoRowItemRenderer)
                                .mapNotNull(RelatedPage.Companion::fromMusicTwoRowItemRenderer)
                        )
                    }

                    else -> null
                }
            }.orEmpty()
        )
    }

    suspend fun likedPlaylists(): Result<List<PlaylistItem>> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_liked_playlists",
            setLogin = true
        )
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items!!
            .drop(1) // the first item is "create new playlist"
            .mapNotNull(GridRenderer.Item::musicTwoRowItemRenderer)
            .mapNotNull {
                ArtistItemsPage.fromMusicTwoRowItemRenderer(it) as? PlaylistItem
            }
    }

    suspend fun librarySongs(): Result<List<SongItem>> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_library_corpus_track_items",
            setLogin = true
        )
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.musicShelfRenderer?.contents?.mapNotNull {
            AlbumPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer)
        }!!
    }

    suspend fun libraryArtists(): Result<List<ArtistItem>> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_library_corpus_artists",
            setLogin = true
        )
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items?.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer) as? ArtistItem
            }
        }!!
    }

    suspend fun libraryAlbums(): Result<List<AlbumItem>> = runCatching {
        val response = innerTube.browse(
            client = WEB_REMIX,
            browseId = "FEmusic_library_corpus_albums",
            setLogin = true
        )
        response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents?.firstOrNull()?.gridRenderer?.items?.mapNotNull {
            it.musicTwoRowItemRenderer?.let { renderer ->
                ArtistItemsPage.fromMusicTwoRowItemRenderer(renderer) as? AlbumItem
            }
        }!!
    }

    suspend fun player(videoId: String, playlistId: String? = null): Result<PlayerResponse> =
        runCatching {
            var playerResponse: PlayerResponse
            if (this.cookie != null) { // if logged in: try ANDROID_MUSIC client first because IOS client does not play age restricted songs
                playerResponse =
                    innerTube.player(ANDROID_MUSIC, videoId, playlistId)
                if (playerResponse.playabilityStatus.status == "OK") {
                    return@runCatching playerResponse
                }
            }
            playerResponse = innerTube.player(IOS, videoId, playlistId)
            if (playerResponse.playabilityStatus.status == "OK") {
                return@runCatching playerResponse
            }
            val safePlayerResponse =
                innerTube.player(TVHTML5, videoId, playlistId)
            if (safePlayerResponse.playabilityStatus.status != "OK") {
                return@runCatching playerResponse
            }
            // Use our simpler stream resolver
            val stream: String? = YtStream.getVideoStream(videoId)

            if (stream != null) {
                // If we got a stream from NewPipe, we can use it to populate/override the streaming data
                // This is especially useful if InnerTube's response is throttled or missing URLs
                return@runCatching safePlayerResponse.copy(
                    streamingData = safePlayerResponse.streamingData?.copy(
                        adaptiveFormats = safePlayerResponse.streamingData.adaptiveFormats.map { adaptiveFormat ->
                            if (adaptiveFormat.isAudio) {
                                adaptiveFormat.copy(url = stream)
                            } else {
                                adaptiveFormat
                            }
                        }
                    )
                )
            }

            return@runCatching safePlayerResponse
        }

    suspend fun next(endpoint: WatchEndpoint, continuation: String? = null): Result<NextResult> =
        runCatching {
            val response = innerTube.next(
                WEB_REMIX,
                endpoint.videoId,
                endpoint.playlistId,
                endpoint.playlistSetVideoId,
                endpoint.index,
                endpoint.params,
                continuation
            )
            val title =
                response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.header?.musicQueueHeaderRenderer?.subtitle?.runs?.firstOrNull()?.text
            val playlistPanelRenderer = response.continuationContents?.playlistPanelContinuation
                ?: response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs[0].tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer!!

            val items = playlistPanelRenderer.contents.mapNotNull { content ->
                content.playlistPanelVideoRenderer
                    ?.let(NextPage::fromPlaylistPanelVideoRenderer)
                    ?.let { it to content.playlistPanelVideoRenderer.selected }
            }
            val songs = items.map { it.first }
            val currentIndex = items.indexOfFirst { it.second }.takeIf { it != -1 }

            // load automix items
            playlistPanelRenderer.contents.lastOrNull()?.automixPreviewVideoRenderer?.content?.automixPlaylistVideoRenderer?.navigationEndpoint?.watchPlaylistEndpoint?.let { watchPlaylistEndpoint ->
                return@runCatching next(watchPlaylistEndpoint).getOrThrow().let { result ->
                    result.copy(
                        title = title,
                        items = songs + result.items,
                        lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(
                            1
                        )?.tabRenderer?.endpoint?.browseEndpoint,
                        relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(
                            2
                        )?.tabRenderer?.endpoint?.browseEndpoint,
                        currentIndex = currentIndex,
                        endpoint = watchPlaylistEndpoint
                    )
                }
            }
            NextResult(
                title = title,
                items = songs,
                currentIndex = currentIndex,
                lyricsEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(
                    1
                )?.tabRenderer?.endpoint?.browseEndpoint,
                relatedEndpoint = response.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer.watchNextTabbedResultsRenderer.tabs.getOrNull(
                    2
                )?.tabRenderer?.endpoint?.browseEndpoint,
                continuation = playlistPanelRenderer.continuations?.getContinuation(),
                endpoint = endpoint
            )
        }

    suspend fun lyrics(endpoint: BrowseEndpoint): Result<String?> = runCatching {
        val response =
            innerTube.browse(WEB_REMIX, endpoint.browseId, endpoint.params)
        response.contents?.sectionListRenderer?.contents?.firstOrNull()?.musicDescriptionShelfRenderer?.description?.runs?.firstOrNull()?.text
    }

    suspend fun related(endpoint: BrowseEndpoint): Result<RelatedPage> = runCatching {
        val response = innerTube.browse(WEB_REMIX, endpoint.browseId)
        val songs = mutableListOf<SongItem>()
        val albums = mutableListOf<AlbumItem>()
        val artists = mutableListOf<ArtistItem>()
        val playlists = mutableListOf<PlaylistItem>()
        response.contents?.sectionListRenderer?.contents?.forEach { sectionContent ->
            sectionContent.musicCarouselShelfRenderer?.contents?.forEach { content ->
                when (val item =
                    content.musicResponsiveListItemRenderer?.let(RelatedPage.Companion::fromMusicResponsiveListItemRenderer)
                        ?: content.musicTwoRowItemRenderer?.let(RelatedPage.Companion::fromMusicTwoRowItemRenderer)) {
                    is SongItem -> if (content.musicResponsiveListItemRenderer?.overlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchEndpoint?.watchEndpointMusicSupportedConfigs
                            ?.watchEndpointMusicConfig?.musicVideoType == MUSIC_VIDEO_TYPE_ATV
                    ) songs.add(item)

                    is AlbumItem -> albums.add(item)
                    is ArtistItem -> artists.add(item)
                    is PlaylistItem -> playlists.add(item)
                    null -> {}
                }
            }
        }
        RelatedPage(songs, albums, artists, playlists)
    }

    suspend fun queue(
        videoIds: List<String>? = null,
        playlistId: String? = null
    ): Result<List<SongItem>> = runCatching {
        if (videoIds != null) {
            assert(videoIds.size <= MAX_GET_QUEUE_SIZE) // Max video limit
        }
        innerTube.getQueue(WEB_REMIX, videoIds, playlistId).queueDatas
            .mapNotNull {
                it.content.playlistPanelVideoRenderer?.let { renderer ->
                    NextPage.fromPlaylistPanelVideoRenderer(renderer)
                }
            }
    }

    suspend fun transcript(videoId: String): Result<String> = runCatching {
        val response = innerTube.getTranscript(WEB, videoId)
        response.actions?.firstOrNull()?.updateEngagementPanelAction?.content?.transcriptRenderer?.body?.transcriptBodyRenderer?.cueGroups?.joinToString(
            separator = "\n"
        ) { group ->
            val time = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.startOffsetMs
            val text = group.transcriptCueGroupRenderer.cues[0].transcriptCueRenderer.cue.simpleText
                .trim('♪')
                .trim(' ')
            "[%02d:%02d.%03d]$text".format(time / 60000, (time / 1000) % 60, time % 1000)
        }!!
    }

    suspend fun visitorData(): Result<String> = runCatching {
        Json.parseToJsonElement(innerTube.getSwJsData().bodyAsText().substring(5))
            .jsonArray[0]
            .jsonArray[2]
            .jsonArray.first { (it as? JsonPrimitive)?.content?.startsWith(VISITOR_DATA_PREFIX) == true }
            .jsonPrimitive.content
    }

    suspend fun accountInfo(): Result<AccountInfo> = runCatching {
        val response = innerTube.accountMenu(WEB_REMIX)
        val action = response.actions.firstOrNull() ?: throw Exception("Could not find account info in response")
        val header = action.openPopupAction.popup.multiPageMenuRenderer.header ?: throw Exception("Not logged in")

        header.activeAccountHeaderRenderer.toAccountInfo()
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

    const val MAX_GET_QUEUE_SIZE = 1000

    private const val VISITOR_DATA_PREFIX = "Cgt"

    const val DEFAULT_VISITOR_DATA = "CgtsZG1ySnZiQWtSbyiMjuGSBg%3D%3D"
}