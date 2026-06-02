# Vynce: The Ultimate Modern Music Experience for Android

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blueviolet.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Vynce is a high-performance, aesthetically pleasing music streaming and management application for Android. Built from the ground up with modern Android development practices, Vynce combines a powerful playback engine with a seamless, declarative UI to deliver a top-tier music experience.

Developed with ❤️ by **GxynnZero**.

---

## 📑 Table of Contents
1. [Introduction & Vision](#-introduction--vision)
2. [Project Architecture](#-project-architecture)
3. [Deep Dive: `:app` Module](#-deep-dive-app-module)
    - [UI Layer & Jetpack Compose](#ui-layer--jetpack-compose)
    - [Playback Service (Media3)](#playback-service-media3)
    - [Data Layer (Room & Repository)](#data-layer-room--repository)
    - [Dependency Injection (Hilt)](#dependency-injection-hilt)
4. [Deep Dive: `:vynceClient` Module](#-deep-dive-vynceclient-module)
    - [InnerTube API Integration](#innertube-api-integration)
    - [Security & Deobfuscation](#security--deobfuscation)
    - [Performance Optimization](#performance-optimization)
5. [Advanced Features](#-advanced-features)
    - [Lyrics System (Gemini AI & Multi-Source)](#lyrics-system-gemini-ai--multi-source)
    - [Autoplay & Queue Management](#autoplay--queue-management)
    - [Local File Integration](#local-file-integration)
6. [Development Guide](#-development-guide)
    - [Prerequisites](#prerequisites)
    - [Building the Project](#building-the-project)
    - [Coding Standards](#coding-standards)
7. [Screenshots & UI Showcase](#-screenshots--ui-showcase)
8. [Roadmap](#-roadmap)
9. [Troubleshooting & FAQ](#-troubleshooting--faq)
10. [Contributing](#-contributing)
11. [License](#-license)

---

## 🌟 Introduction & Vision

Vynce is designed for users who want a clean, fast, and feature-rich alternative to mainstream music players. In a world where music applications are often bloated with social features or intrusive ads, Vynce focuses on what matters most: **the music**.

Our vision is to provide:
- **Zero Distractions**: A pure focus on your library and discovery.
- **High Performance**: Instant loading, smooth scrolling, and lag-free playback.
- **Modern Aesthetics**: Leveraging the latest Material 3 guidelines to create a beautiful, adaptive interface.
- **User Privacy**: No tracking, no data harvesting. Your data stays on your device.

---

## 🏗 Project Architecture

Vynce follows a modular architecture to ensure separation of concerns, better build times, and easier maintenance.

### High-Level Module Structure
- **`:app`**: The Android application module. It contains all UI code, ViewModels, Android Services, and the database implementation.
- **`:vynceClient`**: A pure Kotlin/JVM library that handles communication with external music APIs (primarily YouTube Music/InnerTube). It is independent of the Android framework, making it highly portable.

### Clean Architecture Principles
The project adheres to Clean Architecture principles:
1. **Presentation Layer**: Jetpack Compose UI and ViewModels.
2. **Domain Layer**: Implicitly defined through Repositories and Use Cases (interactors) where needed.
3. **Data Layer**: Room Database, DataStore, and the `vynceClient` network implementation.

---

## 📱 Deep Dive: `:app` Module

The `:app` module is the heart of the Vynce user experience. It is where the declarative UI meets the powerful Android media framework.

### UI Layer & Jetpack Compose
Vynce is built entirely using **Jetpack Compose**. This allows for a much more reactive and maintainable UI compared to traditional XML-based layouts.

#### Key UI Components:
- **`Navigation.kt`**: Handles the entire navigation flow using Type-Safe Navigation.
- **`BottomModal.kt` & `BottomSheet.kt`**: Custom implementations for modern, fluid modal experiences.
- **`SectionHeader.kt`**: Standardized headers for Home and Explore sections.
- **`Card.kt` & `Carousel.kt`**: Reusable components for displaying albums, artists, and playlists.
- **`SwipeableItem.kt`**: Adds intuitive swipe actions to list items (e.g., swipe to delete or add to queue).

#### The M3 Experience:
We fully embrace **Material 3**, including:
- **Dynamic Color**: The app's theme adapts to the user's wallpaper on Android 12+.
- **Predictive Back**: Support for the latest Android navigation gestures.
- **Tonal Palettes**: A cohesive color system that looks great in both light and dark modes.

### Playback Service (Media3)
The `MusicService.kt` is a sophisticated implementation of Google's **Media3 (ExoPlayer)** library.

#### Features:
- **MediaLibraryService**: Provides a robust foundation for media playback that integrates with the system.
- **LeastRecentlyUsedCacheEvictor**: Automatically manages a 512MB cache of audio streams to ensure fast repeats and reduced data usage.
- **AudioFocus Management**: Handles interruptions from calls, notifications, and other apps.
- **Gapless Playback**: Ensures that transitions between tracks are perfectly seamless.
- **Audio Effects Manager**: Handles loudness normalization and custom equalization.

#### Pre-fetching Logic:
The service doesn't just play the current song; it intelligently pre-fetches the next track in the queue, resolving its URI and starting the buffer before the current track finishes.

### Data Layer (Room & Repository)
Vynce uses a highly relational **Room Database** to provide a lightning-fast offline experience.

#### Core Database Entities:
| Entity | Description |
| :--- | :--- |
| `SongEntity` | The primary storage for song metadata. |
| `AlbumEntity` | Stores detailed album information. |
| `ArtistEntity` | Discography and metadata for artists. |
| `PlaylistEntity` | User-defined collections of tracks. |
| `History` | Chronological log of playback events. |
| `LyricsEntity` | Cached lyrics from multiple sources. |
| `SearchHistory` | Persistent storage for recent searches. |
| `StreamCache` | Metadata for optimizing network streams. |

#### Repository Pattern:
The `SongRepository` acts as the single source of truth for the UI. It orchestrates:
- **Syncing**: Aligning local state with remote updates.
- **Likes**: Managing "favorite" status across the app.
- **History**: Recording playback for the "Recently Played" section.
- **Local Scanning**: Utilizing `LocalProvider` to index audio files on the device.

### Dependency Injection (Hilt)
**Hilt** is used for dependency injection throughout the app, making the codebase modular and testable.
- `AppModule.kt`: Provides singleton instances of the Database, Network Client, and Repositories.
- `Qualifiers.kt`: Used to distinguish between different types of providers (e.g., different OkHttp configurations).

---

## 📡 Deep Dive: `:vynceClient` Module

The `:vynceClient` module is a powerful engine for interacting with the **YouTube Music InnerTube API**.

### InnerTube API Integration
Instead of relying on official (but limited) APIs, Vynce uses a custom-built client that speaks the "native language" of YouTube Music's web and mobile interfaces.

#### Core Client Features:
- **`InnerTube.kt`**: A Ktor-based client that manages sessions, cookies, and HTTP/2 connections.
- **Exponential Backoff**: Automatic retry logic for transient network failures.
- **Gzip/Deflate**: Full support for compressed responses to save data.
- **Proxy Support**: Native support for HTTP and SOCKS proxies.

#### Supported Endpoints:
- `search()`: Powerful search with result filtering (Songs, Albums, Artists, Playlists).
- `next()`: The engine behind "Up Next" suggestions and related content.
- `browse()`: Detailed fetching of Album, Artist, and Playlist pages.
- `player()`: Retrieves the actual audio stream URLs and metadata (loudness, bitrate).
- `feedback()`: Allows users to "Like" or "Dislike" tracks directly on their YouTube account.
- `edit_playlist()`: Add, remove, or reorder tracks in cloud playlists.

### Security & Deobfuscation
Streaming from YouTube requires handling complex signature transformations. Vynce includes a `CipherDeobfuscator` (within the app module) and uses extraction logic inspired by `NewPipe` to ensure that playback remains stable even as YouTube updates its platform.

### Performance Optimization
The client is tuned for speed:
- **Connection Pooling**: Reuses connections to reduce handshake overhead.
- **Custom JSON Serializer**: Uses `kotlinx-serialization` with optimized configurations for large payloads.
- **Parallel Fetching**: UI metadata (Related items, Lyrics) are fetched in parallel to reduce perceived latency.

---

## 🚀 Advanced Features

### Lyrics System (Gemini AI & Multi-Source)
Vynce features one of the most advanced lyrics systems available on Android.

#### Multi-Source Fetching:
The app attempts to fetch lyrics from a hierarchy of sources:
1. **YouTube Transcript**: Synced lyrics directly from the source.
2. **LRCLIB**: High-quality community-sourced synced lyrics.
3. **NetEase / QQ Music**: Reliable sources for international and niche content.
4. **YouTube Static Lyrics**: Fallback for plain-text lyrics.

#### Gemini AI Integration:
If no lyrics are found, Vynce can use **Google's Gemini AI** to:
- **Generate Lyrics**: Create a transcript from the song's metadata.
- **Sync Lyrics**: Use the "Generate Timed Lyrics" feature to transform plain text into a synced `.lrc` format.
- **Translate**: Live translation of lyrics into the user's preferred language using "Natural" translation modes.

### Autoplay & Queue Management
The `PlayerViewModel` includes a smart **Autoplay Engine**. As you reach the end of your queue, Vynce analyzes your current listening session and automatically fetches related "Up Next" tracks from YouTube Music, ensuring the music never stops.

#### Queue Features:
- **Reorderable Queue**: Drag and drop tracks to change the play order.
- **Persistent Queue**: Your queue is saved even if the app is closed.
- **Shuffle & Repeat Modes**: Standard and advanced (Repeat One/All/Off) modes.

### Local File Integration
Vynce isn't just a streaming app; it's a full-fledged local music player.
- **MediaStore Integration**: Automatically detects MP3, FLAC, M4A, and other common formats.
- **Unified Library**: Local and streaming songs appear together in your library and playlists seamlessly.

---

## 🛠 Development Guide

### Prerequisites
- **Android Studio Koala (2024.1.1)** or newer.
- **Kotlin 2.0.0+**
- **Java 17**
- **Android SDK 28+**

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/GxynnZero/Vynce.git
   ```
2. Open in Android Studio.
3. Create a `local.properties` file in the root:
   ```properties
   gravatar.api.key="YOUR_KEY"
   ```
4. Click **Sync Project with Gradle Files**.

### Coding Standards
We aim for high-quality, readable code:
- **Kotlin First**: No Java in new code.
- **Compose Stability**: Follow Compose stability rules to avoid unnecessary recompositions.
- **Hilt for DI**: Never manually instantiate repositories or ViewModels.
- **KDoc**: Document all public APIs and complex logic.

---

## 📸 Screenshots & UI Showcase

*(Screenshots coming soon!)*

Vynce features a beautiful, glass-morphic player interface, adaptive home screens, and a minimalist search experience.

---

## 🗺 Roadmap

- [ ] **Android Auto**: Full support for in-car entertainment.
- [ ] **Cast Support**: Stream to Chromecast and Google Home devices.
- [ ] **Advanced EQ**: 10-band equalizer with custom presets.
- [ ] **Sleep Timer**: Schedule when to stop the music.
- [ ] **Cloud Sync**: Sync your local library metadata across devices.
- [ ] **Material You Widgets**: Beautiful home screen controls.

---

## 🔍 Troubleshooting & FAQ

**Q: Why isn't some music playing?**
A: Ensure your internet connection is stable. Some tracks might be region-locked by YouTube.

**Q: How do I enable lyrics?**
A: Tap the lyrics icon in the player. If they don't appear, you can use the "Generate with Gemini" option in settings.

**Q: Can I use my own YouTube account?**
A: Yes! You can provide your cookies in the settings to access your personal playlists and recommendations.

---

## 🤝 Contributing

Contributions are welcome! Whether it's a bug fix, a new feature, or a translation, your help is appreciated.

1. **Fork** the repo.
2. Create your **Feature Branch** (`git checkout -b feature/AmazingFeature`).
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`).
4. **Push** to the branch (`git push origin feature/AmazingFeature`).
5. Open a **Pull Request**.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

**Vynce** - *Your Music, Your Way.*
Designed and developed by [GxynnZero](https://github.com/GxynnZero).
