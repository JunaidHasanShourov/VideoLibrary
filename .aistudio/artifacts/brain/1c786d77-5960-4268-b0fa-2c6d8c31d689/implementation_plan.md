# Implementation Plan: Comprehensive Audio, Video, and Subtitle Controls

## Overview
Enhance the Video Player in Video Library with full-featured, dedicated controls for **Audio**, **Video**, and **Subtitles** while preserving all existing playback capabilities, seekbar responsiveness, gestures, and UI architecture.

---

## 1. Video Quality & Display Features
- **Resolution / Quality Picker**:
  - Dynamically enumerate video track formats across adaptive streams (HLS, DASH, SmoothStreaming) and multi-bitrate progressive files.
  - Display resolution (e.g. 1080p Full HD, 720p HD, 480p SD, 360p), bitrate (Mbps / kbps), and frame rate (fps).
  - Provide an **Auto (Adaptive)** option to let ExoPlayer adjust quality dynamically based on network bandwidth, as well as locked manual resolution overrides via `TrackSelectionOverride`.
- **Aspect Ratio & Resize Modes**:
  - Support standard display modes via `AspectRatioFrameLayout`:
    - **Fit to Screen (Original Ratio)**: `RESIZE_MODE_FIT` (default)
    - **Crop / Zoom to Fill**: `RESIZE_MODE_ZOOM`
    - **Stretch**: `RESIZE_MODE_FILL`
    - **16:9 Wide**: Fixed 16:9 container aspect ratio
    - **4:3 Standard**: Fixed 4:3 container aspect ratio
  - Quick-switch toggle button or selector in the video options sheet with instant on-screen feedback.

---

## 2. Audio Track Selection, Delay Sync & Volume Boost
- **Audio Track Selector**:
  - Enumerate all audio tracks across container formats (MP4, MKV, WebM, TS, etc.).
  - Display clear metadata: Codec (Dolby Atmos, AC-3, E-AC-3, DTS, TrueHD, FLAC, AAC, Opus, MP3, etc.), Channels (5.1 Surround, 7.1, Stereo, Mono), Sample Rate (e.g. 48 kHz), and Language.
  - Retain **Auto (Default)** reset option and error fallback.
- **Audio Delay / Sync Adjustment**:
  - Audio timing offset slider / stepper in increments of ±50ms (from -2000ms to +2000ms).
  - Synchronizes audio stream timestamps for videos with lipsync issues.
- **Volume Boost (Software Amplifier)**:
  - Software gain amplification up to 200% for quiet recordings, while keeping system media volume controls intact.

---

## 3. Subtitle Tracks, Delay Sync & Styling
- **Subtitle Track Picker**:
  - Support both embedded tracks (VTT, SRT, ASS/SSA, TTML, etc.) and external attached subtitle files.
  - One-tap toggle for **Subtitles Off** or selection of specific language/track labels.
- **Subtitle Delay / Timing Sync**:
  - Real-time delay adjustment stepper (-5000ms to +5000ms in ±50ms increments) to align desynced subtitle captions with dialogue.
- **Subtitle Size & Styling**:
  - Text size options: Small (14sp), Normal (18sp), Large (24sp), Extra Large (30sp).
  - Configurable through `CaptionStyleCompat` on `PlayerView` (white high-contrast text with semi-transparent drop shadow and background scrim).

---

## 4. UI/UX Architecture & Player Integration
- **Clean Modal Bottom Sheets**:
  - Modern Material 3 bottom sheets for **Audio Settings**, **Video Settings**, and **Subtitle Settings**, replacing simple dropdown menus with accessible, touch-friendly sheets.
  - Dedicated HUD buttons on the top bar: Video Settings (`VideoSettings` icon), Audio Tracks & Sync (`Audiotrack` icon), Subtitle Tracks & Styling (`ClosedCaption` icon).
- **Non-Intrusive & Isolated**:
  - Maintain the existing seekbar dragging state (`isDraggingSlider`), playback controls, gestures, and database progress saving without touching any outside screens or features.

---

## 5. Verification & Testing
- Unit and Robolectric tests for track formatting, audio delay calculations, aspect ratio switching, and subtitle configuration.
- Compilation check via `compile_applet` and verification of smooth playback across formats.
