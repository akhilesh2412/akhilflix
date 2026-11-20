package com.akhilflix.app.data.model

import com.google.gson.annotations.SerializedName

// ---------------------------------------------------------------------------
// Server-driven UI config (akhilflix.json) — the whole app is defined here.
// Edit the JSON on the backend and the app changes with NO APK update.
// ---------------------------------------------------------------------------

data class AppConfig(
    @SerializedName("appName") val appName: String = "AkhilFlix",
    @SerializedName("theme") val theme: ThemeConfig = ThemeConfig(),
    @SerializedName("logoUrl") val logoUrl: String = "",
    @SerializedName("tabs") val tabs: List<TabConfig> = defaultTabs(),
    @SerializedName("homeSections") val homeSections: List<SectionConfig> = emptyList(),
    @SerializedName("sources") val sources: Map<String, String> = emptyMap(),
    // Action buttons shown on Home (type: play | detail | open_url | open_youtube | share)
    @SerializedName("homeButtons") val homeButtons: List<AppAction> = emptyList(),
    @SerializedName("notes") val notes: String = ""
) {
    companion object {
        fun defaultTabs() = listOf(
            TabConfig("home", "Home", "home", "home"),
            TabConfig("courses", "Courses", "school", "courses"),
            TabConfig("search", "Search", "search", "search"),
            TabConfig("mylist", "My List", "bookmark", "mylist")
        )
    }
}

data class ThemeConfig(
    @SerializedName("primary") val primary: String = "#E50914",
    @SerializedName("background") val background: String = "#141414",
    @SerializedName("surface") val surface: String = "#1F1F1F",
    @SerializedName("text") val text: String = "#FFFFFF",
    @SerializedName("muted") val muted: String = "#B3B3B3"
)

data class TabConfig(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("icon") val icon: String, // home | school | search | bookmark | play
    @SerializedName("screen") val screen: String // home | courses | search | mylist
)

data class SectionConfig(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    // banner_carousel | horizontal_row | grid
    @SerializedName("type") val type: String = "horizontal_row",
    // key into AppConfig.sources, e.g. "programming" or "dhruv"
    @SerializedName("source") val source: String = "programming",
    // category inside that catalog, e.g. "Web Development", "Python"
    @SerializedName("category") val category: String = "",
    @SerializedName("limit") val limit: Int = 20,
    // What tapping a course in this section does: "detail" (default) or "play"
    @SerializedName("clickAction") val clickAction: String = "detail"
)

/**
 * One backend-driven action. `type` picks the behaviour, `target` is what
 * it acts on. See ActionHandler for the full list of supported types.
 */
data class AppAction(
    // play | detail | open_url | open_youtube | share
    @SerializedName("type") val type: String,
    @SerializedName("label") val label: String = "",
    // video id | course id | full URL | text to share (depends on type)
    @SerializedName("target") val target: String = ""
)

// ---------------------------------------------------------------------------
// Course catalogs (youtube_courses.json) — same schema as the
// programming-courses and dhruv-rathee-courses repos.
// ---------------------------------------------------------------------------

data class CourseCatalog(
    @SerializedName("courses") val courses: List<Course> = emptyList()
)

data class Course(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("channel") val channel: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("thumbnail") val thumbnail: String = "",
    @SerializedName("videoCount") val videoCount: Int = 0,
    @SerializedName("videos") val videos: List<Video> = emptyList()
)

data class Video(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    // duration in seconds
    @SerializedName("duration") val duration: Int = 0,
    @SerializedName("thumbnail") val thumbnail: String = "",
    // Direct stream URL (HLS .m3u8 / DASH .mpd / MP4 / MKV / WebM).
    // When set, the video plays in the advanced ExoPlayer.
    // When blank, the video plays via the YouTube embed (id = YouTube ID).
    @SerializedName("streamUrl") val streamUrl: String = "",
    // Optional subtitle file (.vtt or .srt)
    @SerializedName("subtitleUrl") val subtitleUrl: String = ""
) {
    /** "12:34" style label. */
    fun durationLabel(): String {
        val m = duration / 60
        val s = duration % 60
        return if (duration >= 3600) {
            "%d:%02d:%02d".format(duration / 3600, m % 60, s)
        } else {
            "%d:%02d".format(m, s)
        }
    }
}
