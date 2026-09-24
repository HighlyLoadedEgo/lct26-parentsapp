pluginManagement {
    resolutionStrategy {
        eachPlugin {
            // Both Android plugins ship in AGP; reuse it without a separate library marker.
            if (requested.id.id == "com.android.library") {
                requested.version?.let { useModule("com.android.tools.build:gradle:$it") }
            }
        }
    }
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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LCT Parents App"
include(":app")
include(":core:report")
include(":feature:questions")
include(":feature:quests")
include(":feature:report")
include(":feature:scanner")
include(":feature:pin")
