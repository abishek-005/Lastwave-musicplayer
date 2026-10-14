pluginManagement {
    buildscript {
        repositories {
            google()
            mavenCentral()
            maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
        }
        dependencies {
            classpath("com.android.tools:r8:9.4.14")
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "LastWave"
include(":app")
include(":audio:decent-usb-audio-driver")
