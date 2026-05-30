package com.vynce.vynceclient.pages

import com.vynce.vynceclient.models.Album
import com.vynce.vynceclient.models.Artist
import com.vynce.vynceclient.models.BrowseEndpoint
import com.vynce.vynceclient.models.PlaylistPanelVideoRenderer
import com.vynce.vynceclient.models.SongItem
import com.vynce.vynceclient.models.WatchEndpoint
import com.vynce.vynceclient.models.oddElements
import com.vynce.vynceclient.models.splitBySeparator
import com.vynce.vynceclient.utils.parseTime

data class NextResult(
    val title: String? = null,
    val items: List<SongItem>,
    val currentIndex: Int? = null,
    val lyricsEndpoint: BrowseEndpoint? = null,
    val relatedEndpoint: BrowseEndpoint? = null,
    val continuation: String?,
    val endpoint: WatchEndpoint, // current or continuation next endpoint
)

object NextPage {
    fun fromPlaylistPanelVideoRenderer(renderer: PlaylistPanelVideoRenderer): SongItem? {
        val longByLineRuns = renderer.longBylineText?.runs?.splitBySeparator() ?: return null
        return SongItem(
            id = renderer.videoId ?: return null,
            title = renderer.title?.runs?.firstOrNull()?.text ?: return null,
            artists = longByLineRuns.firstOrNull()?.oddElements()?.map {
                Artist(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId
                )
            } ?: return null,
            album = longByLineRuns.getOrNull(1)?.firstOrNull()?.takeIf {
                it.navigationEndpoint?.browseEndpoint != null
            }?.let {
                Album(
                    name = it.text,
                    id = it.navigationEndpoint?.browseEndpoint?.browseId!!
                )
            },
            duration = renderer.lengthText?.runs?.firstOrNull()?.text?.parseTime() ?: return null,
            thumbnail = renderer.thumbnail.thumbnails.lastOrNull()?.url ?: return null,
            explicit = renderer.badges?.find {
                it.musicInlineBadgeRenderer?.icon?.iconType == "MUSIC_EXPLICIT_BADGE"
            } != null
        )
    }
}












