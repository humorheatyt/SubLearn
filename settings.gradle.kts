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
}

rootProject.name = "SubLearn"
include(":app")
include(":core:domain", ":core:subtitles", ":core:platform", ":core:design")
include(":feature:home", ":feature:player", ":feature:learning", ":feature:settings")
