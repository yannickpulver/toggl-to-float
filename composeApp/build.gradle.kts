import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.buildKonfig)
}

group = "com.appswithlove"
version = rootProject.file("VERSION").readText().trim()

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm("desktop")

    jvmToolchain(17)
    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.ktor.core)
            implementation(libs.ktor.cio)
            implementation(libs.kotlinx.datetime)

            implementation("com.bybutter.compose:compose-jetbrains-expui-theme:2.2.0")
            implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            implementation("io.ktor:ktor-client-content-negotiation:2.1.0")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.1.0")
            implementation("ca.gosyer:compose-material-dialogs-datetime:0.8.0")
            implementation("ca.gosyer:accompanist-flowlayout:0.25.2")

        }

    }
}

compose.desktop {
    application {
        mainClass = "com.appswithlove.MainKt"
        nativeDistributions {
            modules("jdk.unsupported")
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe)
            packageName = "Toggl2Float"
            packageVersion = rootProject.file("VERSION").readText().trim()
            macOS {
                iconFile.set(project.file("icon.icns"))
                entitlementsFile.set(project.file("entitlements.plist"))
                bundleID = "com.appswithlove.toggl2float"
                signing {
                    val id = System.getenv("APPSTORE_IDENTITY")
                    sign.set(id != null)
                    identity.set(id)
                }
            }
            windows {
                iconFile.set(project.file("icon.ico"))
            }
        }
        buildTypes.release.proguard {
            configurationFiles.from(project.file("compose-desktop.pro"))
            obfuscate.set(false)
        }
    }
}

buildConfig {
    packageName("com.appswithlove")
    buildConfigField("APP_VERSION", provider { "${project.version}" })
}
