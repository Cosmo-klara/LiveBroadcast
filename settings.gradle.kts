pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
/*
    令强迫症暴怒的不稳定标记，我直接就是一个 Suppress，参考：
    https://stackoverflow.com/questions/63282922/buildfeatures-is-unstable-because-its-signature-references-unstable-marked-w
*/
@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Live Broadcast"
include(":app")
include(":server")
include(":webm")
