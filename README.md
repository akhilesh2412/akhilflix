# AkhilFlix — the whole app runs from ONE GitHub repo

AkhilFlix is a Netflix-style Android app for your own course catalogs
(programming courses + Dhruv Rathee). It works **exactly like a CloudStream
repo**: the installed app is a thin host shell, and the **entire app code**
comes from a GitHub repo as a plugin. Push new code to the repo and the app
updates itself — **no APK update, no Play Store**.

## How it works (CloudStream model)

```
GitHub repo `akhilflix`
  main branch:  host app source + plugin source + this README
  builds branch (made by CI):  repo.json + akhilflix-plugin.dex
```

1. You push code (to the `plugin/` module) to `main`.
2. GitHub Actions builds the plugin to a DEX (`:plugin:buildPluginDex`)
   and publishes `repo.json` + `akhilflix-plugin.dex` to the `builds` branch.
3. The host app on the phone fetches `repo.json`, downloads the DEX when
   the version changed, loads it with `DexClassLoader`, and runs it.
4. The plugin DEX is cached on the phone, so the app also opens offline
   with the last downloaded version.

On top of that, the plugin itself is server-driven: `akhilflix.json`
controls screens, tabs, sections, theme, buttons and click behaviour
(see below). So there are two update layers, both without an APK update:
**code** via the repo plugin, **content/UI** via the JSON.

## Content

- **Programming courses** — 132 courses, 5,889 YouTube videos
  (`Akhilesh2412/programming-courses`)
- **Dhruv Rathee** — 25 playlists, 1,309 YouTube videos
  (`Akhilesh2412/dhruv-rathee-courses`)

Videos are public YouTube videos from your own curated catalogs.

## Setup (first time)

1. Create a GitHub repo named **`akhilflix`** (public) and push this whole
   project to `main`.
2. Wait for the Actions run **"Build AkhilFlix Plugin"** to finish. It
   creates the `builds` branch with `repo.json` + `akhilflix-plugin.dex`.
   Check it opened:
   `https://raw.githubusercontent.com/Akhilesh2412/akhilflix/builds/repo.json`
3. Build the host APK in Android Studio (`:app:assembleRelease`) or with
   `./gradlew :app:assembleRelease`, install it on the phone.
4. Open the app. It downloads the plugin from the repo and the full app
   appears. (First launch needs internet + a finished CI build.)

## Update the app WITHOUT an APK update

- **Code/feature change:** edit anything in `plugin/`, commit + push to
  `main`. CI rebuilds the DEX, the app downloads it on next launch.
- **Content/UI change:** edit `akhilflix.json` (see below). No rebuild at
  all — the plugin picks it up on next launch / pull-to-refresh.

## The honest limits

- **Not for the Play Store.** Downloading and running code from outside
  the Play Store violates Play policy. AkhilFlix is sideload-only, exactly
  like CloudStream. Distribute the host APK directly (GitHub release,
  Telegram, your site).
- **The host shell itself** (downloading + loading the plugin) only changes
  with an APK update. Everything else — every screen, the player, all
  behaviour — lives in the plugin.
- **First launch needs the repo.** Until CI has published the first DEX,
  the host shows a loading/error screen with a retry button.
- **The plugin DEX must not contain library classes** (Compose, Retrofit,
  ExoPlayer…). The `:plugin:buildPluginDex` task passes them to d8 as
  `--lib` so the plugin resolves them from the host at runtime. That is why
  the host app also depends on the same libraries — if you add a new
  library to the plugin, add it to `app/build.gradle.kts` too.

## Server-driven UI (`akhilflix.json`)

The plugin fetches its UI config from:

```
https://raw.githubusercontent.com/Akhilesh2412/akhilflix/main/akhilflix.json
```

(change it in `plugin/.../util/Constants.kt` → `CONFIG_URL`)

Load order: **backend → local file cache → bundled
`plugin/src/main/assets/akhilflix.json`**. Pull-to-refresh on Home reloads
the backend config.

Each home section fetches its catalog from `sources` in the config and
filters by `category`. Catalogs are your existing live files:

- `programming` → `programming-courses/main/youtube_courses.json`
- `dhruv` → `dhruv-rathee-courses/main/youtube_courses.json`

### Update content/UI without any rebuild

**Add a section** — append to `homeSections`:
```json
{ "id": "devops", "title": "DevOps & Tools", "type": "horizontal_row",
  "source": "programming", "category": "DevOps & Tools", "limit": 12 }
```

**Add a tab** — append to `tabs` (screen must be one of
`home`, `courses`, `search`, `mylist`).

**Change the theme** — edit `theme` hex colors (`primary`, `background`,
`surface`, `text`, `muted`).

**Add a content source** — add a key to `sources` with a catalog URL in
the same `youtube_courses.json` schema, then point a section at it.

**Change a section's layout** — set `type` to `banner_carousel`,
`horizontal_row`, or `grid`. Unknown types render as a horizontal row.

### Click behaviour — also backend-driven

**Per-section click action** — `clickAction` on a section:
- `"detail"` (default) — tapping a course opens its detail screen.
- `"play"` — tapping a course plays its first video directly.

**Home action buttons** — `homeButtons` in the config adds buttons on Home:
```json
"homeButtons": [
  { "type": "open_url", "label": "Join Telegram",
    "target": "https://t.me/yourchannel" },
  { "type": "play", "label": "Play Featured", "target": "<video id>" }
]
```
Action types: `play` (target = video id), `detail` (target = course id),
`open_url` (target = full URL), `open_youtube` (target = video id, @handle
or URL), `share` (target = text to share).

**Limit:** the JSON can only pick actions the plugin code already knows
how to do. A *brand-new* action type needs its code added to `plugin/`
(which is still just a repo push — no APK update).

## Player — plays everything (PSPlayer-style)

One player screen routes by content type:

- **Direct stream link** (`streamUrl` on a video) → advanced **ExoPlayer**
  (media3). Supports **HLS (.m3u8), DASH (.mpd), MP4, MKV, WebM** — the
  format is auto-detected from the URL. The built-in controller gives
  play/pause, seek, playback speed, quality selection and subtitle
  selection. Subtitles: set `subtitleUrl` (.vtt or .srt) on the video.
  There is also an "Open in external player" button (VLC / MX Player).
- **YouTube video** (no `streamUrl`, just a YouTube `id`) → in-app YouTube
  IFrame embed, with an "Open in YouTube app" fallback.

To make a video play in ExoPlayer, add `streamUrl` (and optionally
`subtitleUrl`) to it in the catalog JSON:

```json
{ "id": "abc123", "title": "Lecture 1", "duration": 3600,
  "thumbnail": "https://...",
  "streamUrl": "https://example.com/video.m3u8",
  "subtitleUrl": "https://example.com/subs-en.vtt" }
```

## Config schema (`akhilflix.json`)

| Field | Type | Meaning |
|---|---|---|
| `appName` | string | Title shown on Home |
| `theme.primary` | hex | Accent color (buttons, highlights) |
| `theme.background` / `surface` / `text` / `muted` | hex | App colors |
| `logoUrl` | string | Reserved (logo image URL) |
| `tabs[]` | array | Bottom navigation. `id`, `title`, `icon` (`home`/`school`/`search`/`bookmark`/`play`), `screen` (`home`/`courses`/`search`/`mylist`) |
| `homeSections[]` | array | Home sections in order. `id` (unique), `title`, `type` (`banner_carousel`/`horizontal_row`/`grid`), `source` (key in `sources`), `category` (must match catalog category exactly), `limit` (max courses), `clickAction` (`detail` or `play`) |
| `homeButtons[]` | array | Action buttons on Home. Each: `type` (`play`/`detail`/`open_url`/`open_youtube`/`share`), `label`, `target` (video id / course id / URL / text) |
| `sources{}` | map | Source key → catalog JSON URL (`youtube_courses.json` schema) |
| `notes` | string | Human note, ignored by the app |

Catalog categories (must match exactly):
- Programming: Web Development, DSA & Algorithms, Python,
  Programming Languages, DevOps & Tools, Mobile Development,
  AI & Machine Learning, Databases, Computer Science, System Design,
  Cyber Security
- Dhruv Rathee: Current Affairs, History & Culture, Science & Space,
  Finance & Business, Education, Travel & Vlogs

## Build commands

```bash
./gradlew :app:assembleRelease     # host APK (install this once)
./gradlew :plugin:buildPluginDex   # plugin DEX -> plugin/build/plugin/akhilflix-plugin.dex
```

> The project was written without a local Android SDK, so do a first Gradle
> sync in Android Studio and fix any version warnings it reports. The DEX
> build needs the Android SDK (d8) — CI has it; locally, Android Studio
> provides it.

## Project layout

```
settings.gradle.kts          app + plugin-api + plugin
app/                         HOST SHELL (thin, installed APK)
  src/main/assets/akhilflix.json   Bundled fallback UI config (the plugin
                                   reads it via the host Context)
  src/main/java/com/akhilflix/app/
    MainActivity.kt          Loading / Ready(plugin) / Error screen
    PluginLoader.kt          repo.json fetch, DEX download, DexClassLoader
    PluginRepo.kt            repo.json model
    AkhilFlixApp.kt          Application
plugin-api/                  Shared contract (host + plugin both depend on it)
  src/main/java/com/akhilflix/api/AkhilFlixPlugin.kt
plugin/                      THE APP (compiled to DEX, hosted on GitHub)
  src/main/java/com/akhilflix/app/
    AkhilFlixPluginImpl.kt   Plugin entry point (implements the interface)
    AkhilFlixRoot.kt         Root composable: theme + navigation
    data/                    models, Retrofit API, repository
    ui/                      ActionHandler, theme, components, screens, viewmodel
    util/Constants.kt        CONFIG_URL + catalog URLs
  build.gradle.kts           Library + buildPluginDex (d8) task
.github/workflows/build-plugin.yml   CI: build DEX, publish repo.json + DEX to builds
akhilflix.json               Backend UI config — lives on main, edit any time
```

## Notes

- My List is stored locally on the device (DataStore), not on the backend.
- If the backend config fails to load, the plugin uses the cached/bundled
  config — it never shows a blank screen because of a network error.
- To make a section show ALL courses in a catalog, set `"category": ""`.
- ExoPlayer handles HLS, DASH, MP4, MKV and WebM; anything it cannot play
  can be handed to an external player from the player screen.
- The host caches the plugin DEX + version (SharedPreferences). A new
  version in repo.json triggers a re-download on next launch.
