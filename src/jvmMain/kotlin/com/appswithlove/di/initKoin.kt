package com.appswithlove.di

import com.appswithlove.atlassian.AtlassianRepository
import com.appswithlove.floaat.FloatRepo
import com.appswithlove.json
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglRepo
import com.appswithlove.ui.MainViewModel
import com.appswithlove.ui.feature.atlassian.AtlassianViewModel
import com.appswithlove.ui.feature.update.GithubRepo
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.dsl.module

fun KoinApplication.initKoin(): KoinApplication {
    return modules(
        module {
            single {
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            }

            single {
                HttpClient(CIO) {
                    install(ContentNegotiation) {
                        json(get())
                    }
                }
            }

            single { DataStore() }
            single { FloatRepo(get()) }
            single { TogglRepo(get()) }
            single { GithubRepo() }
            single { AtlassianRepository(get(), get(), get()) }
            factory { MainViewModel(get(), get(), get(), get()) }
            factory { AtlassianViewModel(get(), get(), get()) }
        },
    )
}
