package com.vynce.music.repository.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object PreferenceConstants {
    // Authentication
    val INNER_TUBE_COOKIE = stringPreferencesKey("inner_tube_cookie")
    val VISITOR_DATA = stringPreferencesKey("visitor_data")
    val ACCOUNT_NAME = stringPreferencesKey("account_name")
    val ACCOUNT_EMAIL = stringPreferencesKey("account_email")
    val ACCOUNT_CHANNEL_HANDLE = stringPreferencesKey("account_channel_handle")
    val USE_LOGIN_FOR_BROWSE = booleanPreferencesKey("use_login_for_browse")

    // Audio / Playback
    val AUDIO_QUALITY = stringPreferencesKey("audio_quality")
    val NORMALIZE_VOLUME = booleanPreferencesKey("normalize_volume")
    val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
    val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
    val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val AUDIO_OUTPUT = stringPreferencesKey("audio_output")
    val BUFFER_SIZE = stringPreferencesKey("buffer_size")
    val REPEAT_MODE = intPreferencesKey("repeat_mode")
    val SHUFFLE_MODE = booleanPreferencesKey("shuffle_mode")
    val PLAYER_VOLUME = floatPreferencesKey("player_volume")
    val AUDIO_OFFLOAD = booleanPreferencesKey("audio_offload")
    val AUDIO_TRACK_PLAYBACK_PARAMS = booleanPreferencesKey("audio_track_playback_params")
    val EXTERNAL_PLAYER_ENABLED = booleanPreferencesKey("external_player_enabled")

    // Downloads
    val DOWNLOAD_QUALITY = stringPreferencesKey("download_quality")
    val WIFI_ONLY_DOWNLOADS = booleanPreferencesKey("wifi_only_downloads")
    val AUTO_DOWNLOAD_ON_LIKE = booleanPreferencesKey("auto_download_on_like")

    // Appearance
    val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
    val DARK_MODE = stringPreferencesKey("dark_mode")
    val PURE_BLACK = booleanPreferencesKey("pure_black")
    val SHOW_LYRICS = booleanPreferencesKey("show_lyrics")
    val LYRICS_TEXT_SIZE = floatPreferencesKey("lyrics_text_size")
    val SHOW_LYRICS_LOCKSCREEN = booleanPreferencesKey("show_lyrics_lockscreen")
    val LYRICS_FOLDER = stringPreferencesKey("lyrics_folder")
    
    // Lyrics Providers
    val LYRICS_PROVIDER_ORDER = stringPreferencesKey("lyrics_provider_order")
    val ENABLE_LYRICS_PLUS = booleanPreferencesKey("enable_lyrics_plus")

    // Privacy
    val ENABLE_HISTORY = booleanPreferencesKey("enable_history")
    val PAUSE_LISTEN_HISTORY = booleanPreferencesKey("pause_listen_history")
    val PAUSE_SEARCH_HISTORY = booleanPreferencesKey("pause_search_history")

    // General
    val LANGUAGE = stringPreferencesKey("language")
    val CONTENT_REGION = stringPreferencesKey("content_region")
    val PROXY_ENABLED = booleanPreferencesKey("proxy_enabled")
    val RESTRICTED_MODE = booleanPreferencesKey("restricted_mode")
    val BATTERY_OPTIMIZATION = booleanPreferencesKey("battery_optimization")
    
    // Quick Picks
    val QUICK_PICKS = stringPreferencesKey("quick_picks")

    // Home Cache
    val HOME_PAGE_CACHE = stringPreferencesKey("home_page_cache")

    // Sync
    val LAST_FULL_SYNC = longPreferencesKey("last_full_sync")
    const val SYNC_COOLDOWN = 30 * 60L // 30 minutes in seconds

    // Sort & Filter
    val SONG_SORT_TYPE = stringPreferencesKey("song_sort_type")
    val SONG_SORT_DESCENDING = booleanPreferencesKey("song_sort_descending")
    val PLAYLIST_SORT_TYPE = stringPreferencesKey("playlist_sort_type")
    val PLAYLIST_SORT_DESCENDING = booleanPreferencesKey("playlist_sort_descending")
    val ARTIST_VIEW_TYPE = stringPreferencesKey("artist_view_type")
    val ALBUM_VIEW_TYPE = stringPreferencesKey("album_view_type")
}

enum class AudioQuality {
    AUTO,
    LOW,
    HIGH,
}

enum class LibraryFilter {
    LIKED_SONGS,
    PLAYLISTS,
    LOCAL_SONGS,
    LIKED_ALBUMS,
    BOOKMARKED_ARTISTS,
    PODCASTS,
    ALL_SONGS,
    HISTORY
}

enum class HistorySource {
    LOCAL, REMOTE
}

enum class QuickPicks {
    QUICK_PICKS,
    LAST_LISTEN,
}














