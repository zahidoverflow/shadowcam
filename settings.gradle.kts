rootProject.name = "shadowcam"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/shadowcam.versions.toml"))
        }
    }
}

include(
    ":app",
    ":core-model",
    ":core-engine",
    ":profiles",
    ":sources",
    ":logging",
    ":antidetect"
)
