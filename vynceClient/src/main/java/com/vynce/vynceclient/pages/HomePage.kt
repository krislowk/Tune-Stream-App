package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.Album
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.EpisodeItem
import com.vynce.vynceclient.models.MusicCarouselShelfRenderer
import com.vynce.vynceclient.models.MusicMultiRowListItemRenderer
import com.vynce.vynceclient.models.MusicResponsiveListItemRenderer
import com.vynce.vynceclient.models.MusicShelfRenderer
import com.vynce.vynceclient.models.MusicTwoRowItemRenderer
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.PodcastItem
import com.vynce.vynceclient.models.SectionListRenderer
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.models.filterExplicit
import com.vynce.vynceclient.models.filterVideoSongs
import com.vynce.vynceclient.models.oddElements
import com.vynce.vynceclient.models.splitBySeparator
import com.vynce.vynceclient.utils.parseTime
import kotlinx.serialization.Serializable

@Serializable
data class HomePage(
    val chips: List<Chip>?,
    val sections: List<Section>,
    val continuation: String? = null,
) {
    val filters: List<Chip>
        get() = chips ?: emptyList()

    @Serializable
    data class Chip(
        val title: String,
        val endpoint: BrowseEndpoint?,
        val deselectEndPoint: BrowseEndpoint?,
        val isSelected: Boolean = false,
    ) {
        companion object {
            fun fromChipCloudChipRenderer(renderer: SectionListRenderer.Header.ChipCloudRenderer.Chip): Chip? {
                return Chip(
                    title = renderer.chipCloudChipRenderer.text?.runs?.firstOrNull()?.text ?: return null,
                    endpoint = renderer.chipCloudChipRenderer.navigationEndpoint?.browseEndpoint,
                    deselectEndPoint = renderer.chipCloudChipRenderer.onDeselectedCommand?.browseEndpoint,
                    isSelected = renderer.chipCloudChipRenderer.isSelected
                )
            }
        }
    }

    @Serializable
    data class Section(
        val title: String?,
        val label: String?,
        val thumbnail: String?,
        val endpoint: BrowseEndpoint?,
        val items: List<YTItem>,
    ) {
        companion object {
            fun fromMusicCarouselShelfRenderer(renderer: MusicCarouselShelfRenderer): Section {
                val title = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.title?.runs?.firstOrNull()?.text
                val twoRowCount = renderer.contents.count { it.musicTwoRowItemRenderer != null }
                val multiRowCount = renderer.contents.count { it.musicMultiRowListItemRenderer != null }
                val responsiveCount = renderer.contents.count { it.musicResponsiveListItemRenderer != null }

                val items = mutableListOf<YTItem>()

                // Parse musicTwoRowItemRenderer items (songs, albums, playlists, artists, podcasts)
                renderer.contents.mapNotNull { it.musicTwoRowItemRenderer }
                    .mapNotNull { fromMusicTwoRowItemRenderer(it) }
                    .let { items.addAll(it) }

                // Parse musicMultiRowListItemRenderer items (podcast episodes)
                renderer.contents.mapNotNull { it.musicMultiRowListItemRenderer }
                    .mapNotNull { fromMusicMultiRowListItemRenderer(it) }
                    .let { items.addAll(it) }

                // Parse musicResponsiveListItemRenderer items (quick picks songs)
                renderer.contents.mapNotNull { it.musicResponsiveListItemRenderer }
                    .mapNotNull { fromMusicResponsiveListItemRenderer(it) }
                    .let { items.addAll(it) }

                val podcastCount = items.count { it is PodcastItem }
                val episodeCount = items.count { it is EpisodeItem }
                val songCount = items.count { it is SongItem }


                return Section(
                    title = title,
                    label = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.strapline?.runs?.firstOrNull()?.text,
                    thumbnail = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl(),
                    endpoint = renderer.header?.musicCarouselShelfBasicHeaderRenderer?.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = items
                )
            }

            fun fromMusicShelfRenderer(renderer: MusicShelfRenderer): Section {
                val title = renderer.title?.runs?.firstOrNull()?.text
                val items = renderer.contents?.mapNotNull { content ->
                    content.musicResponsiveListItemRenderer?.let { fromMusicResponsiveListItemRenderer(it) }
                } ?: emptyList()

                return Section(
                    title = title,
                    label = null,
                    thumbnail = null,
                    endpoint = renderer.bottomEndpoint?.browseEndpoint ?: renderer.moreContentButton?.buttonRenderer?.navigationEndpoint?.browseEndpoint,
                    items = items
                )
            }

            private fun fromMusicMultiRowListItemRenderer(renderer: MusicMultiRowListItemRenderer): EpisodeItem? {
                val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator()
                val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

                return EpisodeItem(
                    id = renderer.onTap?.watchEndpoint?.videoId ?: return null,
                    title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
                    author = null,
                    podcast = null,
                    duration = subtitleRuns?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                    publishDateText = subtitleRuns?.firstOrNull()?.firstOrNull()?.text,
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = false,
                    endpoint = renderer.onTap.watchEndpoint,
                    libraryAddToken = libraryTokens.addToken,
                    libraryRemoveToken = libraryTokens.removeToken,
                )
            }

            private fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
                // Quick picks uses musicResponsiveListItemRenderer for songs
                if (!renderer.isSong) return null

                val videoId = renderer.videoId ?: return null

                val secondaryLine = renderer.flexColumns
                    .getOrNull(1)
                    ?.musicResponsiveListItemFlexColumnRenderer
                    ?.text
                    ?.runs
                    ?.splitBySeparator()

                return SongItem(
                    id = videoId,
                    title = renderer.flexColumns
                        .firstOrNull()
                        ?.musicResponsiveListItemFlexColumnRenderer
                        ?.text
                        ?.runs
                        ?.firstOrNull()
                        ?.text ?: return null,
                    artists = secondaryLine?.getOrNull(0)?.oddElements()?.map {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    } ?: emptyList(),
                    album = secondaryLine?.getOrNull(1)?.firstOrNull()
                        ?.takeIf { it.navigationEndpoint?.browseEndpoint != null }
                        ?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                            )
                        },
                    duration = secondaryLine?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                    thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = renderer.badges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null,
                    isEpisode = renderer.isEpisode
                )
            }

            private fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
                // Debug logging for type detection
                val title = renderer.title.runs?.firstOrNull()?.text ?: "unknown"
                val pageType = renderer.navigationEndpoint.browseEndpoint
                    ?.browseEndpointContextSupportedConfigs
                    ?.browseEndpointContextMusicConfig
                    ?.pageType
                val hasWatchEndpoint = renderer.navigationEndpoint.watchEndpoint != null

                // Debug for episodes
                if (renderer.isEpisode) {
                    val overlayVideoId = renderer.thumbnailOverlay
                        ?.musicItemThumbnailOverlayRenderer?.content
                        ?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchEndpoint?.videoId
                    val browseId = renderer.navigationEndpoint.browseEndpoint?.browseId
                }

                return when {
                    renderer.isSong -> {
                        val subtitleRuns = renderer.subtitle?.runs?.oddElements()
                        val artists = subtitleRuns?.filter { run ->
                            run.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("UC") == true || (run.navigationEndpoint?.browseEndpoint != null && !run.navigationEndpoint.browseEndpoint.browseId.startsWith("MPREb_"))
                        }?.map { run ->
                            Artist(
                                name = run.text,
                                id = run.navigationEndpoint?.browseEndpoint?.browseId
                            )
                        }?.ifEmpty {
                            subtitleRuns.firstOrNull()?.let { run ->
                                listOf(Artist(name = run.text, id = null))
                            } ?: emptyList()
                        } ?: emptyList()

                        val album = subtitleRuns?.firstOrNull {
                            it.navigationEndpoint?.browseEndpoint?.browseId?.startsWith("MPREb_") == true
                        }?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                            )
                        }

                        SongItem(
                            id = renderer.navigationEndpoint.watchEndpoint?.videoId
                                ?: renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content?.musicPlayButtonRenderer?.playNavigationEndpoint?.watchEndpoint?.videoId
                                ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = artists,
                            album = album,
                            duration = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl()
                                ?: return null,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true
                        )
                    }
                    renderer.isAlbum -> {
                        AlbumItem(
                            browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint?.playlistId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            artists = renderer.subtitle?.runs?.oddElements()?.drop(1)?.map {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            },
                            year = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            explicit = renderer.subtitleBadges?.find {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } != null
                        )
                    }

                    renderer.isPlaylist -> {
                        PlaylistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = Artist(
                                name = renderer.subtitle?.runs?.firstOrNull()?.text ?: return null,
                                id = null
                            ),
                            songCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                            radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint
                        )
                    }

                    renderer.isArtist || renderer.isUserChannel -> {
                        ArtistItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.lastOrNull()?.text ?: return null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                            radioEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                            isProfile = renderer.isUserChannel
                        )
                    }

                    renderer.isPodcast -> {
                        PodcastItem(
                            id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                            title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                            author = renderer.subtitle?.runs?.firstOrNull()?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            },
                            episodeCountText = null,
                            thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl(),
                            playEndpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchPlaylistEndpoint,
                            shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                                it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                            }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint,
                        )
                    }

                    renderer.isEpisode -> {
                        val videoId = renderer.thumbnailOverlay
                            ?.musicItemThumbnailOverlayRenderer?.content
                            ?.musicPlayButtonRenderer?.playNavigationEndpoint
                            ?.watchEndpoint?.videoId ?: ""
                        val titleText = renderer.title.runs?.firstOrNull()?.text?: ""
                        val thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: ""
                        val subtitleRuns = renderer.subtitle?.runs?.splitBySeparator()
                        val libraryTokens = PageHelper.extractLibraryTokensFromMenuItems(renderer.menu?.menuRenderer?.items)

                        // Find podcast link in subtitle (has isPodcastEndpoint)
                        val podcastRun = renderer.subtitle?.runs?.find {
                            it.navigationEndpoint?.browseEndpoint?.isPodcastEndpoint == true
                        }
                        val podcastAlbum = podcastRun?.let {
                            Album(
                                name = it.text,
                                id = it.navigationEndpoint?.browseEndpoint?.browseId ?: return@let null
                            )
                        }

                        EpisodeItem(
                            id = videoId,
                            title = titleText,
                            author = subtitleRuns?.firstOrNull()?.firstOrNull()?.let {
                                Artist(
                                    name = it.text,
                                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                                )
                            },
                            podcast = podcastAlbum,
                            duration = subtitleRuns?.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                            publishDateText = subtitleRuns?.getOrNull(1)?.firstOrNull()?.text,
                            thumbnail = thumbnail,
                            explicit = renderer.subtitleBadges?.any {
                                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                            } == true,
                            endpoint = renderer.thumbnailOverlay
                                ?.musicItemThumbnailOverlayRenderer?.content
                                ?.musicPlayButtonRenderer?.playNavigationEndpoint
                                ?.watchEndpoint,
                            libraryAddToken = libraryTokens.addToken,
                            libraryRemoveToken = libraryTokens.removeToken,
                        )
                    }

                    else -> null
                }
            }
        }
    }

    fun filterExplicit(enabled: Boolean = true) =
        if (enabled) {
            copy(sections = sections.map {
                it.copy(items = it.items.filterExplicit())
            })
        } else this

    fun filterVideoSongs(disableVideos: Boolean = false) =
        if (disableVideos) {
            copy(sections = sections.map { section ->
                section.copy(items = section.items.filterVideoSongs(true))
            })
        } else this
}















