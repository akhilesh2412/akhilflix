package com.akhilflix.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akhilflix.app.data.model.PluginInfo
import com.akhilflix.app.util.PluginRunner
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A stream link resolved from a plugin, ready for the ExoPlayer. */
data class PendingStream(
    val title: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val subtitleUrl: String? = null
)

data class PluginUiState(
    val loading: Boolean = false,
    val pluginName: String = "",
    val sections: List<HomePageList> = emptyList(),
    val error: String? = null,
    val query: String = "",
    val searching: Boolean = false,
    val searchResults: List<SearchResponse> = emptyList(),
    val detail: LoadResponse? = null,
    val detailLoading: Boolean = false,
    val pendingStream: PendingStream? = null,
    val streamLoading: Boolean = false,
    val streamError: String? = null
)

/**
 * Drives one running CloudStream plugin: its home page (categories),
 * search, detail and stream links. The plugin itself is loaded by
 * PluginRunner; this ViewModel only calls the provider's MainAPI.
 */
class PluginViewModel(private val context: Context) : ViewModel() {

    private val _uiState = MutableStateFlow(PluginUiState())
    val uiState: StateFlow<PluginUiState> = _uiState.asStateFlow()

    private var api: MainAPI? = null

    /** Download + load the plugin, then show its home page. */
    fun openPlugin(plugin: PluginInfo) {
        _uiState.value = PluginUiState(loading = true, pluginName = plugin.displayName())
        viewModelScope.launch {
            val result = PluginRunner.loadProviders(context.applicationContext, plugin)
            result.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    loading = false,
                    error = e.message ?: "Plugin load nahi hua"
                )
            }.onSuccess { providers ->
                val provider = providers.firstOrNull()
                if (provider == null) {
                    _uiState.value = _uiState.value.copy(
                        loading = false,
                        error = "Plugin load hua par koi provider nahi mila"
                    )
                    return@launch
                }
                api = provider
                loadHome()
            }
        }
    }

    /** One section per MainPageData entry, like CloudStream's home page. */
    private fun loadHome() {
        val provider = api ?: return
        viewModelScope.launch {
            val sections = withContext(Dispatchers.IO) {
                val out = mutableListOf<HomePageList>()
                for (entry in provider.mainPage) {
                    runCatching {
                        provider.getMainPage(
                            1,
                            MainPageRequest(entry.name, entry.data, entry.horizontalImages)
                        )
                    }.onSuccess { resp -> resp?.items?.let { out += it } }
                }
                out
            }
            _uiState.value = _uiState.value.copy(
                loading = false,
                sections = sections,
                error = if (sections.isEmpty()) {
                    "Is plugin ka home page khaali hai — upar search karke dekho"
                } else {
                    null
                }
            )
        }
    }

    fun search(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        val provider = api ?: return
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(searchResults = emptyList(), searching = false)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(searching = true)
            val results = withContext(Dispatchers.IO) {
                runCatching { provider.search(query) }.getOrNull() ?: emptyList()
            }
            _uiState.value = _uiState.value.copy(searching = false, searchResults = results)
        }
    }

    /** Open an item's detail page (movie / series / live). */
    fun openItem(item: SearchResponse) {
        val provider = api ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(detailLoading = true, detail = null)
            val loaded = withContext(Dispatchers.IO) {
                runCatching { provider.load(item.url) }.getOrNull()
            }
            _uiState.value = _uiState.value.copy(
                detailLoading = false,
                detail = loaded,
                error = if (loaded == null) "Detail load nahi hua" else null
            )
        }
    }

    fun clearDetail() {
        _uiState.value = _uiState.value.copy(detail = null)
    }

    /** Resolve a movie / episode into a playable stream link. */
    fun playData(title: String, data: String) {
        val provider = api ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                streamLoading = true,
                streamError = null,
                pendingStream = null
            )
            val (links, subtitles) = withContext(Dispatchers.IO) {
                val foundLinks = mutableListOf<ExtractorLink>()
                val foundSubs = mutableListOf<SubtitleFile>()
                runCatching {
                    provider.loadLinks(
                        data,
                        false,
                        { sub -> foundSubs += sub },
                        { link -> foundLinks += link }
                    )
                }
                foundLinks to foundSubs
            }
            val best = pickBestLink(links)
            if (best == null) {
                _uiState.value = _uiState.value.copy(
                    streamLoading = false,
                    streamError = "Koi stream link nahi mila"
                )
                return@launch
            }
            val headers = best.headers.toMutableMap()
            if (best.referer.isNotBlank()) headers["Referer"] = best.referer
            _uiState.value = _uiState.value.copy(
                streamLoading = false,
                pendingStream = PendingStream(
                    title = title,
                    url = best.url,
                    headers = headers,
                    subtitleUrl = subtitles.firstOrNull()?.url
                )
            )
        }
    }

    fun clearStream() {
        _uiState.value = _uiState.value.copy(pendingStream = null, streamError = null)
    }

    /** Best playable link: highest quality first, HLS preferred over DASH over MP4. */
    private fun pickBestLink(links: List<ExtractorLink>): ExtractorLink? {
        val playable = links.filter {
            it.type != ExtractorLinkType.TORRENT && it.url.isNotBlank()
        }
        if (playable.isEmpty()) return null
        return playable.sortedWith(
            compareByDescending<ExtractorLink> { it.quality }
                .thenBy { typeRank(it.type) }
        ).firstOrNull()
    }

    private fun typeRank(type: ExtractorLinkType): Int = when (type) {
        ExtractorLinkType.M3U8 -> 0
        ExtractorLinkType.DASH -> 1
        ExtractorLinkType.VIDEO -> 2
        else -> 3
    }
}
