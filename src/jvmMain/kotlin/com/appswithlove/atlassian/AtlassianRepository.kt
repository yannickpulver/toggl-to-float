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
import java.util.Base64

class AtlassianRepository(private val dataStore: DataStore) {

    private val client = HttpClient(CIO)

    suspend fun postWorklog(
        issueId: String,
        started: String,
        timeSpentSeconds: Int,
        comment: String
    ) {
        val url = "https://${dataStore.getStore.atlassianHost}/rest/api/3/issue/$issueId/worklog"

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
              "timeSpentSeconds": $timeSpentSeconds
            }
        """.trimIndent()
        postRequest(url, data)
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
        Logger.log("Response: ${response.status.value} ${response.bodyAsText()}")
        return response
    }


}