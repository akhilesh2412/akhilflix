package com.akhilflix.app.util

/**
 * Turns whatever GitHub / CloudStream link the user pastes into the
 * catalog URL(s) of a CloudStream repo (its youtube_courses.json, same
 * schema as programming-courses / dhruv-rathee-courses).
 *
 * A CloudStream repo URL can look like MANY things, so the parsing is
 * deliberately forgiving — owner + repo are pulled out of the link
 * wherever they appear, and everything after the repo name is ignored:
 *  - https://github.com/<owner>/<repo>            (repo page)
 *  - https://github.com/<owner>/<repo>/tree/main  (file listing)
 *  - https://github.com/<owner>/<repo>/blob/main/youtube_courses.json
 *  - https://www.github.com/<owner>/<repo>        (www. prefix)
 *  - <owner>/<repo>                               (bare, no domain)
 *  - https://raw.githubusercontent.com/<owner>/<repo>/builds/repo.json
 *    (a CloudStream install URL — the catalog lives on main/master)
 *  - https://raw.githubusercontent.com/<owner>/<repo>/main/youtube_courses.json
 *    (direct catalog link, used as-is)
 *
 * For a repo page the main branch is tried first, then master.
 *
 * TWO kinds of repo are supported, and the candidates cover both:
 *  - course repos: youtube_courses.json (main, then master)
 *  - CloudStream plugin repos: repo.json — the pasted link itself when
 *    it ends in .json, else main/repo.json, master/repo.json,
 *    builds/repo.json (CloudStream repos keep their manifest in one
 *    of these places)
 */
object RepoResolver {

    data class Resolved(
        val id: String,
        val name: String,
        val catalogCandidates: List<String>,
        val repoJsonCandidates: List<String>
    )

    // Direct raw link to a youtube_courses.json file (incl. refs/heads/... paths).
    private val rawCatalogRegex =
        Regex("""^https?://raw\.githubusercontent\.com/([^/]+)/([^/]+)/.+youtube_courses\.json$""", RegexOption.IGNORE_CASE)

    // owner/repo found ANYWHERE in a github.com / raw.githubusercontent.com link.
    // Everything after the repo name is ignored: /tree/main, /blob/..., .git,
    // trailing slash, query string, fragment.
    private val ownerRepoRegex =
        Regex("""(?:github\.com|raw\.githubusercontent\.com)/([^/\s?#]+)/([^/\s?#]+?)(?:\.git)?(?:[/\s?#].*)?$""", RegexOption.IGNORE_CASE)

    // Bare "owner/repo" with no domain at all.
    private val bareRegex = Regex("""^([^/\s]+)/([^/\s]+?)(?:\.git)?$""")

    fun resolve(input: String): Resolved? {
        val text = input.trim()
        if (text.isEmpty()) return null

        // Direct raw catalog link — use it exactly as pasted.
        rawCatalogRegex.matchEntire(text)?.let { m ->
            val (owner, repo) = m.destructured
            return Resolved(idOf(owner, repo), repo, listOf(text), emptyList())
        }

        // Any GitHub / raw link: pull owner + repo out of it.
        ownerRepoRegex.find(text)?.let { m ->
            val (owner, repo) = m.destructured
            return Resolved(
                idOf(owner, repo),
                repo,
                catalogCandidates(owner, repo),
                repoJsonCandidates(owner, repo, text)
            )
        }

        // Bare owner/repo.
        bareRegex.matchEntire(text)?.let { m ->
            val (owner, repo) = m.destructured
            return Resolved(
                idOf(owner, repo),
                repo,
                catalogCandidates(owner, repo),
                repoJsonCandidates(owner, repo, text)
            )
        }

        // Any other https://...json link: try it as a repo manifest as-is.
        if (text.startsWith("http") && text.endsWith(".json")) {
            val slug = text.substringAfterLast('/').removeSuffix(".json")
            val name = slug.ifBlank { "custom-repo" }
            return Resolved(
                "custom-${kotlin.math.abs(text.hashCode())}",
                name,
                emptyList(),
                listOf(text)
            )
        }

        return null
    }

    private fun catalogCandidates(owner: String, repo: String) = listOf(
        "https://raw.githubusercontent.com/$owner/$repo/main/youtube_courses.json",
        "https://raw.githubusercontent.com/$owner/$repo/master/youtube_courses.json"
    )

    private fun repoJsonCandidates(owner: String, repo: String, pasted: String): List<String> {
        val base = "https://raw.githubusercontent.com/$owner/$repo"
        val candidates = mutableListOf<String>()
        // The pasted link itself first when it already points at a .json file
        // (e.g. a CloudStream install link like .../builds/repo.json).
        if (pasted.trim().endsWith(".json")) candidates += pasted.trim()
        candidates += "$base/main/repo.json"
        candidates += "$base/master/repo.json"
        candidates += "$base/builds/repo.json"
        return candidates.distinct()
    }

    private fun idOf(owner: String, repo: String): String {
        val clean = { s: String ->
            s.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        }
        return "${clean(owner)}-${clean(repo)}"
    }
}
