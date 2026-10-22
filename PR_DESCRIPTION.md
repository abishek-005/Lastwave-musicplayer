# PR Title
`feat(player): add local audio file playback and library discovery`

---

## Summary
LastWave provides an exceptional experience for YouTube Music streaming and downloading, but previously had no capability to discover or play existing audio files stored on device storage or SD cards.

This pull request introduces **Local Songs**, enabling users to scan, browse, and play their personal offline audio library (MP3, FLAC, M4A, AAC, WAV, OGG, OPUS) directly within LastWave. Local tracks integrate seamlessly into the native player with full waveform visuals, audio effect DSPs, queue management, notification transport controls, and lockscreen playback—all without any network requests or YouTube resolver dependencies.

---

## How It Works

1. **Discovery & Permissions**:
   - Queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` on `Dispatchers.IO` with `IS_MUSIC != 0`.
   - Requests granular runtime permissions: `READ_MEDIA_AUDIO` on Android 13+ (API 33+) and `READ_EXTERNAL_STORAGE` on Android 12 and below.
   - Includes a Storage Access Framework (SAF) folder picker (`OpenDocumentTree`) fallback for custom directories or restricted storage trees.
   - Resolves album art efficiently using content provider URIs (`content://media/external/audio/albumart/<albumId>`).

2. **Playback Architecture**:
   - Extended `PlayableTrack` with `isLocal: Boolean = false`.
   - Added direct stream resolution in `MusicPlayer.kt` via `resolveLocalTrackDirectly()`, routing `content://` and `file://` URIs directly to ExoPlayer without contacting remote stream resolvers, quality upgraders, or YouTube APIs.
   - Radio auto-queue generation and remote stream upgrading are safely bypassed when a local song is playing.
   - `MediaItem` extras serialize `isLocal` to preserve track identity across background service and notification cycles.

3. **User Interface**:
   - Built with Material 3 Expressive and liquid glass styling matching LastWave's design language.
   - Screen organized into **Songs**, **Artists**, and **Albums** tabs with fast searching and multiple sort orders (Title, Artist, Date Added, Duration).
   - Prominent entry points added:
     - Top bar Folder action icon (`Icons.Filled.Folder`) on Home and Feed screens.
     - "Local Songs" navigation card and empty-state action in the Library / Playlists tab.
     - "Device & Offline Audio" section in Settings.
   - Context menus on local tracks selectively hide remote-only actions (*Download*, *Start Mix*, *Don't recommend again*).

4. **Privacy & Scrobbler**:
   - `YtMusicHistorySyncManager` explicitly ignores local tracks so playing local phone files never pollutes remote YouTube Music history.

---

## Known Limitations

- **Lyrics**: Embedded ID3/Vorbis comment synced lyrics in local files are not parsed yet; remote LRCLIB queries are only attempted if artist and title are identified.
- **YouTube Radio**: Automatic YouTube continuous radio generation is disabled when playing local files to prevent streaming over cellular data when listening offline.
- **Downloads / Recommendations**: Context menu items for downloading, algorithmic radios, and recommendation exclusions are hidden for local tracks.

---

## What Was Tested

- [x] **Local MediaStore Scan**: Successfully indexed local audio files (MP3, FLAC, M4A, WAV).
- [x] **Direct Audio Playback**: Verified seamless audio playback through ExoPlayer and native DSP engine.
- [x] **Background & Lockscreen**: Verified lockscreen controls, notification transport buttons, and continuous background playback when app is minimized.
- [x] **Context Menus**: Verified remote actions (Download, Start Mix) are omitted for local files.
- [x] **Device tested**: `[Oppo A3x 5G / Android 14]` *(tested on physical device)*

---

## Non-Regression Checklist

- [x] **Streaming**: Remote YouTube Music search and playback are completely untouched.
- [x] **Downloads**: Offline downloaded track playback and download managers remain unaffected.
- [x] **Offline Playback**: Existing download cache paths and offline listeners operate identically.
- [x] **Scrobbler**: Scrobbler continues to process track metadata without alteration.
- [x] **Database**: Zero Room schema changes or migrations introduced (remains database version 13).
- [x] **Secrets & URLs**: No hardcoded secrets, test URLs, or credentials added.

---

## Screenshots / Screen Recording

<!-- Add screenshots or screen recordings of the Local Songs screen and playback here -->
