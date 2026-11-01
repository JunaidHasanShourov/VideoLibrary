# Production Readiness: Complete Sample & Demo Removal

Remove all sample media, demonstration presets, and mock URL buttons across the app so it launches as a clean, pristine, production-ready video library and browser player.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed and will guide the production cleanup:

- **Confirmed Decision 1**: **Purge Sample Database Entries**: Clear all preloaded Google/Blender Open Movie sample videos (`Big Buck Bunny`, `Elephants Dream`, `For Bigger Blazes`, `Tears of Steel`, etc.) from the Room database on launch so existing app data starts completely empty.
- **Confirmed Decision 2**: **Browser Shortcut Upgrade**: Replace the "Google Sample Media" shortcut tile on the browser start page with the **Internet Archive Video Library** (`https://archive.org/details/movies`).
- **Confirmed Decision 3**: **Clean Importer UI**: Remove the "Try Sample Video" chip from `AddVideoScreen` and "Try Sample URL" chip from `WebsiteImporterScreen`, retaining clean, modern placeholder text and full functional manual / crawling capabilities.
- **Confirmed Decision 4**: **Filesystem Sanity**: Clean up any misplaced or malformed cover image files in the resource directory.

---

## 1. Overview & Core Concept

- **What It Does**: Transitions the application from a development/testing showcase into a true first-version release. When users install or open the app, they receive a pristine, modern video library with zero mock content, an empty library state that gracefully guides them to import videos or use the web crawler, and a clean browser start page.
- **Target Audience**: Users seeking a self-contained, powerful personal video hub with full hardware/software codec acceleration (including AC-3, E-AC-3, DTS, Opus, and all modern container formats) without clutter from test files.
- **Key Value**: Professional, production-grade zero-state; zero mock video interference; complete user ownership of their media library.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Launch / Clean Slate Flow**:
   - User launches the app for the first time or updates to this stable release.
   - Any previous sample database entries are cleared once; the initial seeding routine is completely disabled.
   - The Library displays the polished M3 Empty State: hero illustration icon (`Icons.Default.VideoLibrary`), clear title ("No videos yet"), and dual action buttons: "Browse Web" and "Import Video".
2. **Web Crawler / Browser Flow**:
   - User opens the in-app browser.
   - The start page shows clean search input and popular video site shortcuts: YouTube, Vimeo, Internet Archive (replacing Google Sample Media), Dailymotion, Twitch, and Reddit.
   - Automatic floating media detection and manual extraction toolbars remain 100% functional for any live URL navigated by the user.
3. **Manual Import Flow**:
   - User navigates to Add Video.
   - Clean text fields for Title and Video URL without prefilled sample buttons or confusing dummy URLs.
   - Live format detection and metadata preview work seamlessly on real video URLs.

### Visual Identity & Theme
- **Empty State Posture**: Spacious, unobtrusive vertical layout utilizing Material 3 tonal elevation, rounded containers (`RoundedCornerShape(20.dp)`), and subtle accent tints.
- **Iconography**: Material Symbols filled icons (`VideoLibrary`, `Public`, `AddLink`, `FolderOpen`) for high scanability.
- **Touch Targets**: Minimum 48dp on all interactive buttons, cards, and shortcut chips.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Explicit Seed Removal vs. Just Disabling New Insertions**
  - *Chosen Approach*: Specifically delete any existing sample video records (matched by their known sample URLs or tags) and disable the `insertVideos(samples)` block in `VideoRepository.kt`.
  - *Why*: Ensures users who already opened the app during development testing don't keep seeing Big Buck Bunny and other test videos cluttering their personal library.
  - *Alternatives Considered*: Only deleting the seed code would leave old test entries in existing app storage.
- **Decision 2: Internet Archive as Replacement Shortcut**
  - *Chosen Approach*: Point to `https://archive.org/details/movies` with an Archive icon.
  - *Why*: Provides legal, public-domain, accessible video content for users who want to immediately test the player and crawler without depending on a developer test bucket.
- **Decision 3: Zero Feature Loss on Crawlers and Decoders**
  - *Chosen Approach*: Only remove sample buttons and seed data; all Media3 FFmpeg decoders (AC-3 5.1 surround 48kHz, DTS, TrueHD, Vorbis, Opus), player controls, crawler extractors, and database schemas remain fully active and intact.

---

## 4. Technical Architecture & Data Strategy

```
┌──────────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                        │
├───────────────────┬──────────────────────┬───────────────────┤
│   LibraryScreen   │  BrowserScreen       │ AddVideo / Import │
│ (M3 Empty State)  │ (Clean Shortcuts +   │ (Clean Input +    │
│                   │  Archive.org)        │  Format Sniffer)  │
└─────────┬─────────┴──────────┬───────────┴─────────┬─────────┘
          │                    │                     │
          ▼                    ▼                     ▼
┌──────────────────────────────────────────────────────────────┐
│                     Viewmodels & Logic                       │
│  - VideoViewModel (Pristine state, reactive Flow)            │
│  - BrowserViewModel (Media detection & extraction)           │
└──────────────────────────────┬───────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                  VideoRepository (Room DB)                   │
│  - Seed routine removed                                      │
│  - Existing sample URLs purged on launch                     │
│  - Real user imports preserved                               │
└──────────────────────────────────────────────────────────────┘
```

### Components to Update
1. **`VideoRepository.kt`**:
   - Remove `insertVideos(samples)` mock list.
   - Add one-time migration/purge routine to delete sample videos with `commondatastorage.googleapis.com` or `storage.googleapis.com/gtv-videos-bucket` URLs from `videoDao`.
2. **`AddVideoScreen.kt`**:
   - Remove "Try Sample Video" suggestion chip.
   - Clean up placeholder text to be standard generic hints (e.g. `https://example.com/video.mp4` -> `https://.../video.mp4`).
3. **`WebsiteImporterScreen.kt`**:
   - Remove "Try Sample URL" suggestion chip.
4. **`BrowserScreen.kt`**:
   - Replace "Google Sample Media" shortcut entry in `popularShortcuts` with "Internet Archive" (`https://archive.org/details/movies`).
5. **Project Files**:
   - Inspect and clean up any stray or malformed files in the res directory.
