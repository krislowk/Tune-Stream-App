package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.Album
import com.vynce.vynceclient.models.AlbumItem
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.ArtistItem
import com.vynce.vynceclient.models.MusicResponsiveListItemRenderer
import com.vynce.vynceclient.models.MusicTwoRowItemRenderer
import com.vynce.vynceclient.models.PlaylistItem
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.YTItem
import com.vynce.vynceclient.models.clean
import com.vynce.vynceclient.models.oddElements
import com.vynce.vynceclient.models.splitBySeparator
import com.vynce.vynceclient.utils.parseTime

data class RelatedPage(
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val playlists: List<PlaylistItem>,
) {
    companion object {
        fun fromMusicResponsiveListItemRenderer(renderer: MusicResponsiveListItemRenderer): SongItem? {
            val secondaryLine = renderer.flexColumns.getOrNull(1)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()
                ?: return null
            val thirdLine = renderer.flexColumns.getOrNull(2)
                ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.splitBySeparator()
                ?: emptyList()
            val listRun = (secondaryLine + thirdLine).clean()
            
            return SongItem(
                id = renderer.playlistItemData?.videoId ?: return null,
                title = renderer.flexColumns.firstOrNull()
                    ?.musicResponsiveListItemFlexColumnRenderer?.text?.runs?.firstOrNull()
                    ?.text ?: return null,
                artists = listRun.getOrNull(0)?.oddElements()?.map {
                    Artist(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId
                    )
                } ?: return null,
                album = listRun.getOrNull(1)?.firstOrNull()?.takeIf { it.navigationEndpoint?.browseEndpoint != null }?.let {
                    Album(
                        name = it.text,
                        id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                    )
                },
                duration = secondaryLine.lastOrNull()?.firstOrNull()?.text?.parseTime(),
                thumbnail = renderer.thumbnail?.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                explicit = renderer.badges?.find {
                    it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                } != null
            )
        }

        fun fromMusicTwoRowItemRenderer(renderer: MusicTwoRowItemRenderer): YTItem? {
            return when {
                renderer.isAlbum -> AlbumItem(
                    browseId = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                    playlistId = renderer.thumbnailOverlay?.musicItemThumbnailOverlayRenderer
                        ?.content?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchPlaylistEndpoint?.playlistId ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    artists = null,
                    year = renderer.subtitle?.runs?.lastOrNull()?.text?.toIntOrNull(),
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    explicit = renderer.subtitleBadges?.find {
                        it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
                    } != null
                )
                renderer.isPlaylist -> PlaylistItem(
                    id = renderer.navigationEndpoint.browseEndpoint?.browseId?.removePrefix("VL") ?: return null,
                    title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                    author = renderer.subtitle?.runs?.getOrNull(2)?.let {
                        Artist(
                            name = it.text,
                            id = it.navigationEndpoint?.browseEndpoint?.browseId
                        )
                    },
                    songCountText = renderer.subtitle?.runs?.getOrNull(4)?.text,
                    thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                    playEndpoint = renderer.thumbnailOverlay
                        ?.musicItemThumbnailOverlayRenderer?.content
                        ?.musicPlayButtonRenderer?.playNavigationEndpoint
                        ?.watchPlaylistEndpoint ?: return null,
                    shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    radioEndpoint = renderer.menu.menuRenderer.items.find {
                        it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                    }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null
                )
                renderer.isArtist -> {
                    ArtistItem(
                        id = renderer.navigationEndpoint.browseEndpoint?.browseId ?: return null,
                        title = renderer.title.runs?.firstOrNull()?.text ?: return null,
                        thumbnail = renderer.thumbnailRenderer.musicThumbnailRenderer?.getThumbnailUrl() ?: return null,
                        shuffleEndpoint = renderer.menu?.menuRenderer?.items?.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MUSIC_SHUFFLE"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                        radioEndpoint = renderer.menu.menuRenderer.items.find {
                            it.menuNavigationItemRenderer?.icon?.iconType == "MIX"
                        }?.menuNavigationItemRenderer?.navigationEndpoint?.watchPlaylistEndpoint ?: return null,
                    )
                }
                else -> null
            }
        }
    }
}












