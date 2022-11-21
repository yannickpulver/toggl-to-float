import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "com.appswithlove"
version = "1.0.2"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation(compose.desktop.currentOs)
                implementation("io.ktor:ktor-client-core:2.1.0")
                implementation("io.ktor:ktor-client-cio:2.1.0")
                implementation("ca.gosyer:compose-material-dialogs-datetime:0.8.0")
            }
        }
        val jvmTest by getting

    }
}

compose.desktop {
    application {
        mainClass = "com.appswithlove.MainKt"
        javaHome = System.getenv("JDK_15")
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Exe)
            packageName = "Toggl2Float"
            packageVersion = "1.0.1"
            macOS {
                iconFile.set(project.file("icon.icns"))
                bundleID = "com.appswithlove.toggl2float"
            }
            windows {
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
