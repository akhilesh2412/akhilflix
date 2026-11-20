package com.akhilflix.app.util

/**
 * Single place for every backend URL.
 * To point the app at a different backend, change ONLY these constants
 * (or better: change the JSON hosted at CONFIG_URL — no app update needed).
 */
object Constants {
    /** Server-driven UI config. Edit this JSON on GitHub to change the whole app. */
    const val CONFIG_URL =
        "https://raw.githubusercontent.com/Akhilesh2412/akhilflix/main/akhilflix.json"

    /** Programming courses catalog (live, 132 courses). */
    const val PROGRAMMING_CATALOG_URL =
        "https://raw.githubusercontent.com/Akhilesh2412/programming-courses/main/youtube_courses.json"

    /** Dhruv Rathee catalog (live, 25 playlists). */
    const val DHRUV_CATALOG_URL =
        "https://raw.githubusercontent.com/Akhilesh2412/dhruv-rathee-courses/main/youtube_courses.json"

    const val CONFIG_CACHE_FILE = "akhilflix_config.json"
    const val YOUTUBE_THUMB = "https://i.ytimg.com/vi/%s/hqdefault.jpg"
    const val YOUTUBE_WATCH = "https://www.youtube.com/watch?v=%s"

    fun thumbFor(videoId: String): String = YOUTUBE_THUMB.format(videoId)
    fun watchUrlFor(videoId: String): String = YOUTUBE_WATCH.format(videoId)
}
