package com.appswithlove.atlassian

import com.appswithlove.store.DataStore
import com.appswithlove.ui.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import java.util.Base64
import kotlin.math.ceil
import kotlin.math.roundToInt

class AtlassianRepository(private val dataStore: DataStore) {

    private val client = HttpClient(CIO)

    private fun roundSecondsToNearestQuarterHour(duration: Int): Int {
        val durationInMinutes = duration / 60f
        val roundedDurationInMinutes = ceil(durationInMinutes / 15) * 15
        return roundedDurationInMinutes.toInt() * 60
    }

    suspend fun postWorklog(
        issueId: String,
        started: String,
        timeSpentSeconds: Int,
        comment: String
    ) {
        val url = "https://${dataStore.getStore.atlassianHost}/rest/api/3/issue/$issueId/worklog"

        val time = if (dataStore.getStore.attlasianRoundToQuarterHour) {
            roundSecondsToNearestQuarterHour(timeSpentSeconds)
        } else {
            timeSpentSeconds
        }

        val data = """
            {
              "comment": {
                "content": [
                  {
                    "content": [
                      {
                        "text": "$comment",
                        "type": "text"
                      }
                    ],
                    "type": "paragraph"
                  }
                ],
                "type": "doc",
                "version": 1
              },
              "started": "$started",
              "timeSpentSeconds": $time
            }
        """.trimIndent()
        val response = postRequest(url, data)
        if (response.status == HttpStatusCode.Created) {
            Logger.log("Worklog added for issue $issueId")
        } else {
            Logger.err("Worklog not added: ${response.bodyAsText()}")
        }
    }

   private fun HttpRequestBuilder.basicAuthAtlassian() {
        val email: String? = dataStore.getStore.atlassianEmail
        val apiKey: String? = dataStore.getStore.atlassianApiKey
        if (email.isNullOrEmpty() || apiKey.isNullOrEmpty()) {
            Logger.err("No Atlassian credentials found")
            throw Exception("No Atlassian credentials found")
        }
        basicAuth(email, apiKey)
    }

    private suspend fun getRequest(
        url: String
    ): HttpResponse {
        val response: HttpResponse = client.get(url) {
            basicAuthAtlassian()
        }
        return response
    }

    private suspend fun postRequest(url: String, data: String): HttpResponse {
        val response: HttpResponse = client.post(url) {
            header(HttpHeaders.ContentType, "application/json")
            basicAuthAtlassian()
            setBody(data)
        }
        return response
    }


}