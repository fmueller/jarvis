plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("flexmark", "com.vladsch.flexmark:flexmark-all:0.64.8")
        }
    }
}

rootProject.name = "jarvis"
