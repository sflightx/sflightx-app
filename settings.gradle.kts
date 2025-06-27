@file:Suppress("DEPRECATION", "UnstableApiUsage")


include(":EnhancedDatabasae")


include(":ImageCrop")


pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://jitpack.io")
        google()
        mavenCentral()
        gradlePluginPortal()
        //noinspection JcenterRepositoryObsolete
        jcenter()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SFlightX"
include(":app")
