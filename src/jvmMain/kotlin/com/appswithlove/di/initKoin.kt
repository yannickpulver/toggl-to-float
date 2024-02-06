package com.appswithlove.di

import com.appswithlove.atlassian.AtlassianRepository
import com.appswithlove.floaat.FloatRepo
import com.appswithlove.store.DataStore
import com.appswithlove.toggl.TogglRepo
import com.appswithlove.ui.MainViewModel
import com.appswithlove.ui.feature.atlassian.AtlassianViewModel
import com.appswithlove.ui.feature.update.GithubRepo
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
                }
            }

            single { DataStore() }
            single { FloatRepo(get()) }
            single { TogglRepo(get()) }
            single { GithubRepo() }
            single { AtlassianRepository(get()) }
            factory { MainViewModel(get(), get(), get(), get()) }
            factory { AtlassianViewModel(get(), get(), get()) }
        },
    )
}
