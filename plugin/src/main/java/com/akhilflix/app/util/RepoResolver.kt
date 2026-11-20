package com.akhilflix.app.util

/**
 * Turns whatever GitHub link the user pastes into the catalog URL(s)
 * of a CloudStream repo (its youtube_courses.json, same schema as
 * programming-courses / dhruv-rathee-courses).
 *
 * Accepted inputs:
 *  - https://github.com/<owner>/<repo>            (repo page)
 *  - https://raw.githubusercontent.com/<owner>/<repo>/main/youtube_courses.json
 *  - https://raw.githubusercontent.com/<owner>/<repo>/builds/repo.json
 *    (a CloudStream install URL — owner/repo are read off it)
 *  - any other https://...json link (tried as-is)
 *
 * For a repo page the main branch is tried first, then master.
 */
object RepoResolver {

    data class Resolved(
        val id: String,
        val name: String,
        val catalogCandidates: List<String>
    )

    private val githubPageRegex =
        Regex("""^https?://github\.com/([^/]+)/([^/]+?)(?:\.git)?/?(?:[?#].*)?$""")
    private val rawRegex =
        Regex("""^https?://raw\.githubusercontent\.com/([^/]+)/([^/]+)/([^/]+)/(.+)$""")

    fun resolve(input: String): Resolved? {
        val text = input.trim()
        if (text.isEmpty()) return null

        // Direct raw link to a youtube_courses.json file.
        rawRegex.matchEntire(text)?.let { m ->
            val (owner, repo, _, path) = m.destructured
            if (path.endsWith("youtube_courses.json")) {
                return Resolved(idOf(owner, repo), repo, listOf(text))
            }
            // Any other raw link (e.g. builds/repo.json): the owner/repo
            // still tell us where the catalog lives.
            return Resolved(
                idOf(owner, repo),
                repo,
                catalogCandidates(owner, repo)
            )
        }

        // github.com/<owner>/<repo> page link.
        githubPageRegex.matchEntire(text)?.let { m ->
            val (owner, repo) = m.destructured
            return Resolved(idOf(owner, repo), repo, catalogCandidates(owner, repo))
        }

        // Anything else ending in .json: try it as a catalog URL as-is.
        if (text.startsWith("http") && text.endsWith(".json")) {
            val slug = text.substringAfterLast('/').removeSuffix(".json")
            val name = slug.ifBlank { "custom-repo" }
            return Resolved("custom-${kotlin.math.abs(text.hashCode())}", name, listOf(text))
        }

        return null
    }

    private fun catalogCandidates(owner: String, repo: String) = listOf(
        "https://raw.githubusercontent.com/$owner/$repo/main/youtube_courses.json",
        "https://raw.githubusercontent.com/$owner/$repo/master/youtube_courses.json"
    )

    private fun idOf(owner: String, repo: String): String {
        val clean = { s: String ->
            s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
        return "${clean(owner)}-${clean(repo)}"
    }
}
