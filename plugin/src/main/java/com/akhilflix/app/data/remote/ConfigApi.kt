package com.akhilflix.app.data.remote

import com.akhilflix.app.data.model.AppConfig
import com.akhilflix.app.data.model.CourseCatalog
import com.akhilflix.app.data.model.PluginInfo
import com.akhilflix.app.data.model.RepoJson
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Plain Retrofit calls. Every URL comes from the config or Constants,
 * so the backend can move without an app update.
 */
interface ConfigApi {
    @GET
    suspend fun fetchConfig(@Url url: String): AppConfig

    @GET
    suspend fun fetchCatalog(@Url url: String): CourseCatalog

    /** A CloudStream repo manifest (repo.json). */
    @GET
    suspend fun fetchRepoJson(@Url url: String): RepoJson

    /** A CloudStream plugin list (plugins.json) — a JSON array. */
    @GET
    suspend fun fetchPlugins(@Url url: String): List<PluginInfo>
}

object ApiFactory {
    // Base URL is never really used — every call passes a full @Url.
    private const val DUMMY_BASE = "https://raw.githubusercontent.com/"

    val api: ConfigApi by lazy {
        Retrofit.Builder()
            .baseUrl(DUMMY_BASE)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ConfigApi::class.java)
    }
}
