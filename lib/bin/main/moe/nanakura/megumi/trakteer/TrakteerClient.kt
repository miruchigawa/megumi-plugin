package moe.nanakura.megumi.trakteer

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class TrakteerClient(private val apiKey: String) {
    private val client =
            HttpClient(OkHttp) {
                install(ContentNegotiation) {
                    json(
                            Json {
                                ignoreUnknownKeys = true
                                prettyPrint = true
                                isLenient = true
                            }
                    )
                }
                defaultRequest {
                    url("https://api.trakteer.id/v1/public/")
                    header("Accept", "application/json")
                    header("X-Requested-With", "XMLHttpRequest")
                    header("key", apiKey)
                }
            }

    suspend fun getSupports(
            limit: Int = 5,
            page: Int = 1,
            include: String? = null
    ): TrakteerResponse<TrakteerSupportsResult> {
        return client
                .get("supports") {
                    parameter("limit", limit)
                    parameter("page", page)
                    if (include != null) {
                        parameter("include", include)
                    }
                }
                .body()
    }

    suspend fun getBalance(): TrakteerResponse<String> {
        return client.get("current-balance").body()
    }

    fun close() {
        client.close()
    }
}
