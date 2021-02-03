/*
 * Copyright (C) 2019 Bosch Software Innovations GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.clients.clearlydefined

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

import java.io.File
import java.net.URI

import okhttp3.OkHttpClient
import okhttp3.ResponseBody

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

const val HARVEST_CREATED = "Created"

/**
 * Interface for the ClearlyDefined REST API, based on code generated by https://app.quicktype.io/ from
 * https://github.com/clearlydefined/service/tree/master/schemas.
 */
interface ClearlyDefinedService {
    companion object {
        /**
         * The mapper for JSON serialization used by this service.
         */
        val JSON_MAPPER = JsonMapper().registerKotlinModule()

        /**
         * Create a ClearlyDefined service instance for communicating with the given [server], optionally using a
         * pre-built OkHttp [client].
         */
        fun create(server: Server, client: OkHttpClient? = null): ClearlyDefinedService =
            create(server.url, client)

        /**
         * Create a ClearlyDefined service instance for communicating with a server running at the given [url],
         * optionally using a pre-built OkHttp [client].
         */
        fun create(url: String, client: OkHttpClient? = null): ClearlyDefinedService {
            val retrofit = Retrofit.Builder()
                .apply { if (client != null) client(client) }
                .baseUrl(url)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create(JSON_MAPPER))
                .build()

            return retrofit.create(ClearlyDefinedService::class.java)
        }
    }

    /**
     * See https://github.com/clearlydefined/service/blob/661934a/schemas/swagger.yaml#L8-L14.
     */
    enum class Server(val url: String) {
        /**
         * This creates PRs against https://github.com/clearlydefined/curated-data.
         */
        PRODUCTION("https://api.clearlydefined.io"),

        /**
         * This creates PRs against https://github.com/clearlydefined/curated-data-dev.
         */
        DEVELOPMENT("https://dev-api.clearlydefined.io"),

        LOCALHOST("http://localhost:4000")
    }

    /**
     * The return type for https://api.clearlydefined.io/api-docs/#/definitions/post_definitions.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Defined(
        val coordinates: Coordinates,
        val described: Described,
        val licensed: Licensed,
        val files: List<FileEntry>? = null,
        val scores: FinalScore,

        @JsonProperty("_id")
        val id: String? = null,

        @JsonProperty("_meta")
        val meta: Meta
    ) {
        /**
         * Return the harvest status of a described component, also see
         * https://github.com/clearlydefined/website/blob/de42d2c/src/components/Navigation/Ui/HarvestIndicator.js#L8.
         */
        @JsonIgnore
        fun getHarvestStatus() =
            when {
                described.tools == null -> HarvestStatus.NOT_HARVESTED
                described.tools.size > 2 -> HarvestStatus.HARVESTED
                else -> HarvestStatus.PARTIALLY_HARVESTED
            }
    }

    /**
     * See https://github.com/clearlydefined/service/blob/b339cb7/schemas/definition-1.0.json#L80-L89.
     */
    data class FinalScore(
        val effective: Int,
        val tool: Int
    )

    /**
     * See https://github.com/clearlydefined/service/blob/b339cb7/schemas/definition-1.0.json#L48-L61.
     */
    data class Meta(
        val schemaVersion: String,
        val updated: String
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/curation-1.0.json#L7-L17.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Curation(
        val described: Described? = null,
        val licensed: Licensed? = null,
        val files: List<FileEntry>? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L145-L179.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Described(
        val score: DescribedScore? = null,
        val toolScore: DescribedScore? = null,
        val facets: Facets? = null,
        val sourceLocation: SourceLocation? = null,
        val urls: URLs? = null,
        val projectWebsite: URI? = null,
        val issueTracker: URI? = null,
        val releaseDate: String? = null,
        val hashes: Hashes? = null,
        val files: Int? = null,
        val tools: List<String>? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L180-L190.
     */
    data class DescribedScore(
        val total: Int,
        val date: Int,
        val source: Int
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L264-L275.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Facets(
        val core: Facet? = null,
        val data: Facet? = null,
        val dev: Facet? = null,
        val doc: Facet? = null,
        val examples: Facet? = null,
        val tests: Facet? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L276-L286.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Facet(
        val files: Int? = null,
        val attribution: Attribution? = null,
        val discovered: Discovered? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L287-L301.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Attribution(
        val parties: List<String>? = null,
        val unknown: Int? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L305-L319.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Discovered(
        val expressions: List<String>? = null,
        val unknown: Int? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L211-L235.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class SourceLocation(
        // The following properties match those of Coordinates, except that the revision is mandatory here.
        val type: ComponentType,
        val provider: Provider,
        val namespace: String? = null,
        val name: String,
        val revision: String,

        val path: String? = null,
        val url: String? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L236-L253.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class URLs(
        val registry: URI? = null,
        val version: URI? = null,
        val download: URI? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L135-L144.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Hashes(
        val md5: String? = null,
        val sha1: String? = null,
        val sha256: String? = null,
        val gitSha: String? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L90-L134.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class FileEntry(
        val path: File,
        val license: String? = null,
        val attributions: List<String>? = null,
        val facets: Facets? = null,
        val hashes: Hashes? = null,
        val token: String? = null,
        val natures: Set<Nature>? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L254-L263.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Licensed(
        val score: LicensedScore? = null,
        val toolScore: LicensedScore? = null,
        val declared: String? = null,
        val facets: Facets? = null
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/definition-1.0.json#L191-L204.
     */
    data class LicensedScore(
        val total: Int,
        val declared: Int,
        val discovered: Int,
        val consistency: Int,
        val spdx: Int,
        val texts: Int
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4e210d7/schemas/swagger.yaml#L84-L101.
     */
    data class ContributionPatch(
        val contributionInfo: ContributionInfo,
        val patches: List<Patch>
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4e210d7/schemas/swagger.yaml#L87-L97.
     */
    data class ContributionInfo(
        val type: ContributionType,

        /**
         * Short (100 char) description. This will also be used as the PR title.
         */
        val summary: String,

        /**
         * Describe here the problem(s) being addressed.
         */
        val details: String,

        /**
         * What does this PR do to address the issue? Include references to docs where the new data was found and, for
         * example, links to public conversations with the affected project team.
         */
        val resolution: String,

        /**
         * Remove contributed definitions from the list.
         */
        val removedDefinitions: Boolean
    )

    /**
     * See https://github.com/clearlydefined/service/blob/b339cb7/schemas/curations-1.0.json#L8-L15.
     */
    data class Patch(
        val coordinates: Coordinates,
        val revisions: Map<String, Curation>
    )

    /**
     * See https://github.com/clearlydefined/service/blob/b339cb7/schemas/curations-1.0.json#L64-L83 and
     * https://docs.clearlydefined.io/using-data#a-note-on-definition-coordinates.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class Coordinates(
        /**
         * The type of the component. For example, npm, git, nuget, maven, etc. This talks about the shape of the
         * component.
         */
        val type: ComponentType,

        /**
         * Where the component can be found. Examples include npmjs, mavencentral, github, nuget, etc.
         */
        val provider: Provider,

        /**
         * Many component systems have namespaces: GitHub orgs, NPM namespace, Maven group id, etc. This segment must be
         * supplied. If your component does not have a namespace, use '-' (ASCII hyphen).
         */
        val namespace: String? = null,

        /**
         * The name of the component. Given the mentioned [namespace] segment, this is just the simple name.
         */
        val name: String,

        /**
         * Components typically have some differentiator like a version or commit id. Use that here. If this segment is
         * omitted, the latest revision is used (if that makes sense for the provider).
         */
        val revision: String? = null
    ) {
        companion object {
            @JsonCreator
            @JvmStatic
            fun fromString(value: String): Coordinates {
                val parts = value.split('/', limit = 5)
                return Coordinates(
                    type = ComponentType.fromString(parts[0]),
                    provider = Provider.fromString(parts[1]),
                    namespace = parts[2].takeUnless { it == "-" },
                    name = parts[3],
                    revision = parts.getOrNull(4)
                )
            }
        }

        override fun toString() = listOfNotNull(type, provider, namespace ?: "-", name, revision).joinToString("/")
    }

    /**
     * See https://github.com/clearlydefined/service/blob/53acc01/routes/curations.js#L86-L89.
     */
    data class ContributionSummary(
        val prNumber: Int,
        val url: String
    )

    /**
     * See https://github.com/clearlydefined/service/blob/4917725/schemas/harvest-1.0.json#L12-L22.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class HarvestRequest(
        val tool: String? = null,
        val coordinates: String,
        val policy: String? = null
    )

    /**
     * Return a batch of definitions for the components given as [coordinates], see
     * https://api.clearlydefined.io/api-docs/#/definitions/post_definitions.
     */
    @POST("definitions")
    suspend fun getDefinitions(@Body coordinates: Collection<String>): Map<String, Defined>

    /**
     * Search for existing definitions based on the [pattern] string provided, see
     * https://api.clearlydefined.io/api-docs/#/definitions/get_definitions. This function represents the part of
     * the definitions endpoint that allows searching for package coordinates based on a pattern. The pattern string
     * should contain the parts of the coordinates (typically namespace, name, and version) relevant for the search.
     * Result is a list with the ClearlyDefined URIs to all the definitions that are matched by the pattern.
     */
    @GET("definitions")
    fun searchDefinitions(@Query("pattern") pattern: String): Call<List<String>>

    /**
     * Get the curation for the component described by [type], [provider], [namespace], [name] and [revision], see
     * https://api.clearlydefined.io/api-docs/#/curations/get_curations__type___provider___namespace___name___revision_.
     */
    @GET("curations/{type}/{provider}/{namespace}/{name}/{revision}")
    suspend fun getCuration(
        @Path("type") type: ComponentType,
        @Path("provider") provider: Provider,
        @Path("namespace") namespace: String,
        @Path("name") name: String,
        @Path("revision") revision: String
    ): Curation

    /**
     * Upload curation [patch] data, see https://api.clearlydefined.io/api-docs/#/curations/patch_curations.
     */
    @PATCH("curations")
    suspend fun putCuration(@Body patch: ContributionPatch): ContributionSummary

    /**
     * [Request][request] the given components to be harvested, see
     * https://api.clearlydefined.io/api-docs/#/harvest/post_harvest.
     */
    @POST("harvest")
    fun harvest(@Body request: Collection<HarvestRequest>): Call<String>

    /**
     * Get information about the harvest tools that have produced data for the component described by [type],
     * [provider], [namespace], [name], and [revision], see
     * https://api.clearlydefined.io/api-docs/#/harvest/get_harvest__type___provider___namespace___name___revision_.
     * This can be used to quickly find out whether results of a specific tool are already available.
     */
    @GET("harvest/{type}/{provider}/{namespace}/{name}/{revision}?form=list")
    fun harvestTools(
        @Path("type") type: ComponentType,
        @Path("provider") provider: Provider,
        @Path("namespace") namespace: String,
        @Path("name") name: String,
        @Path("revision") revision: String
    ): Call<List<String>>

    /**
     * Get the harvested data for the component described by [type], [provider], [namespace], [name], and [revision]
     * that was produced by [tool] with version [toolVersion], see
     * https://api.clearlydefined.io/api-docs/#/harvest/get_harvest__type___provider___namespace___name___revision___tool___toolVersion_
     */
    @GET("harvest/{type}/{provider}/{namespace}/{name}/{revision}/{tool}/{toolVersion}?form=streamed")
    fun harvestToolData(
        @Path("type") type: ComponentType,
        @Path("provider") provider: Provider,
        @Path("namespace") namespace: String,
        @Path("name") name: String,
        @Path("revision") revision: String,
        @Path("tool") tool: String,
        @Path("toolVersion") toolVersion: String
    ): Call<ResponseBody>
}
