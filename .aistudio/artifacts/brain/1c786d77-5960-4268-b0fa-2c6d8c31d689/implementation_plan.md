# Implementation Plan: Video Player Audio Track Fixes, Cover Artwork Resolution & In-Built Video Browser

Fix the multiple audio track selection issue in the video player, resolve directory artwork/cover extraction and card rendering, and add an in-built web browser beside the home search bar to browse the web, search, detect media streams, and import videos directly into the library without altering other existing features.

---

## 1. Problem Analysis & Root Causes

### 1.1 Audio Track Switching & Auto Selection
- **Symptoms:** When playing videos with multiple audio tracks, automatic track selection fails or selecting an audio track causes errors/breakage.
- **Root Cause:**
  - `VideoPlayerScreen.kt` currently only offers specific track overrides without an "Auto (Default)" option to clear manual track overrides.
  - Setting track overrides with `TrackSelectionOverride` does not first clear previous audio overrides (`clearOverridesOfType(C.TRACK_TYPE_AUDIO)`) or explicitly re-enable the audio track type (`setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)`).
  - Outdated or mismatched track group references from prior track change events can cause ExoPlayer to throw an exception if not handled defensively.
  - Track selection changes lack safe try-catch error guards to prevent breaking player state.

### 1.2 Directory Artwork / Cover Fetching & Display
- **Symptoms:** Art and cover images are not fetching from scanned directories or setting as covers on video cards.
- **Root Cause:**
  - `MetadataAssociator.kt` only matches image files if their base filename strictly contains one of a small set of hardcoded keywords (`poster`, `cover`, `thumb`, etc.) or matches the video base name. In many directory listings, images may have custom names, URL encoding (`%20`), or be the only image file in that directory.
  - `WebCrawler.kt` did not extract `<img>` tags, Open Graph / Twitter image metadata, or thumbnail links (`<a><img ...></a>`) into discovered artworks for association.
  - Coil image loader in `VideoLibraryApp.kt` was using standard OkHttpClient without the browser `User-Agent` and headers, causing 403 Forbidden responses when loading remote images from servers that require standard headers.
  - Fallback logic: If a directory contains videos and any image, that image should be associated with the videos in that directory.

### 1.3 In-Built Browser Next to Home Search Bar
- **Requirements:**
  - Add a browser icon button beside the home search bar.
  - Open an in-built browser screen with:
    - Clean start page featuring a search/URL address bar and popular site shortcuts.
    - Web navigation via Android `WebView` supporting JavaScript, DOM storage, and back/forward history.
    - Automatic floating bottom banner whenever video streams (e.g. `.mp4`, `.m3u8`, `.webm`, `<video>` sources) are detected on the active webpage.
    - Manual "Extract Media" / "Fetch Videos" action in the browser toolbar with deep crawler capabilities.
    - Preview list of detected videos with title, duration/format, and one-tap "Add to Library" capability.
  - Constraint: Strictly preserve all existing functionality without regressions.

---

## 2. Proposed Changes

### Component 1: Video Player Multiple Audio Track Selection
- **File:** `app/src/main/java/com/example/ui/player/VideoPlayerScreen.kt`
  - Add an **"Auto (Default)"** option to the audio tracks dropdown menu that clears all audio overrides:
    ```kotlin
    p.trackSelectionParameters = p.trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        .build()
    ```
  - When switching to a specific audio track:
    - Safely wrap track selection in a try-catch block.
    - Clear existing audio overrides first, then apply the `TrackSelectionOverride(trackItem.group.mediaTrackGroup, trackItem.trackIndex)`, and ensure `setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)`.
    - Check if any override is active so that "Auto" displays an active checkmark when no manual override is selected.
  - Add proper test tags: `testTag("audio_track_auto")`, `testTag("audio_track_item_${index}")`.

### Component 2: Cover / Artwork Directory Association & Loading
- **File:** `app/src/main/java/com/example/crawler/MetadataAssociator.kt`
  - Enhance directory path resolution to handle URL encoding/decoding and path normalization.
  - Expand artwork keyword matching to include `fanart`, `banner`, `preview`, `screenshot`, `art`, `image`, `pic`.
  - Add fallback directory matching: If a directory contains videos and any artwork file (even if custom-named), match it as the cover for the directory's videos.
  - Ensure parent directory cover matching works gracefully for nested media structures (e.g., `Movie Folder/CD1/video.mp4` picking up `Movie Folder/cover.jpg`).
- **File:** `app/src/main/java/com/example/crawler/WebCrawler.kt`
  - Extract webpage images from `<img>` tags, `meta[property="og:image"]`, `meta[name="twitter:image"]`, and `<a><img src="...">` links.
  - Tag detected page images as `MediaType.ARTWORK` so `MetadataAssociator` can link them to discovered videos.
- **File:** `app/src/main/java/com/example/VideoLibraryApp.kt`
  - Implement Coil's `ImageLoaderFactory` to configure Coil with `NetworkClient.okHttpClient` (equipped with `NetworkClient.DEFAULT_USER_AGENT` and redirect handling) so image loading across all cards and detail screens succeeds without HTTP 403 or header rejection.

### Component 3: In-Built Web Browser with Video Stream Sniffing & One-Tap Import
- **New File:** `app/src/main/java/com/example/ui/browser/BrowserScreen.kt`
  - Integrated `WebView` with enabled JavaScript, DOM storage, and custom WebViewClient.
  - **URL / Search Bar:** Auto-detects whether the user entered a valid URL (e.g., `https://...` or `domain.com`) or search keywords (redirecting to DuckDuckGo/Google search).
  - **Start Page:** Clean start view with search prompt and quick links/shortcuts (e.g., Archive.org, Open Media Directories, Public Domain Movies, Big Buck Bunny Test Streams, Wikimedia Commons).
  - **Video Sniffer:** Intercepts media resource requests (`onLoadResource`, `shouldInterceptRequest`) and DOM video tags (`<video>`, `<source>`, `.m3u8`, `.mp4`, `.webm`, `.mov`, `.mkv`) to automatically populate detected media items.
  - **Dual Import UX (per user choice):**
    - **Automatic floating banner:** Pops up at the bottom when videos are detected on the current webpage, showing count and quick "View / Add" button.
    - **Manual "Fetch Videos" button:** Located in the browser top bar to thoroughly scan the current page DOM and links using `WebCrawler` and display all found videos in a bottom sheet.
  - **Bottom Sheet / Import Dialog:** Displays found video titles, thumbnail, source URL, format, and an "Add to Library" button that writes directly to `VideoRepository` with duplicate detection.
- **File:** `app/src/main/java/com/example/ui/home/HomeScreen.kt`
  - Add a dedicated Browser icon button in the `TopAppBar` actions, immediately adjacent to the search button:
    ```kotlin
    IconButton(
        onClick = onNavigateToBrowser,
        modifier = Modifier.testTag("home_browser_btn")
    ) {
        Icon(Icons.Default.Language, contentDescription = "Open Browser")
    }
    ```
- **File:** `app/src/main/java/com/example/ui/navigation/NavRoute.kt` & `AppNavHost.kt`
  - Add `NavRoute.BROWSER = "browser"` route.
  - Register `composable(NavRoute.BROWSER)` linking to `BrowserScreen`.

---

## 3. Verification & Testing Plan

1. **Compilation & Syntax:**
   - Run `compile_applet` to verify compilation across all touched and new Kotlin files.
2. **Audio Track Selection Unit & Logic Verification:**
   - Verify `TrackSelectionOverride` behavior with unit tests or Robolectric to ensure switching between Audio Track 1, Audio Track 2, and Auto works without exception.
3. **Artwork Association Verification:**
   - Test `MetadataAssociator.associate()` with various folder structures (same folder, parent folder, custom-named images, URL-encoded filenames).
4. **Browser & UI Verification:**
   - Run existing unit test suite `gradle :app:testDebugUnitTest` to guarantee zero regressions.
