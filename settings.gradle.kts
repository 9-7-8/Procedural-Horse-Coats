rootProject.name = "horse-genetics"

include(":common")
include(":neoforge-26.1.2")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.neoforged.net/releases")
        mavenCentral()
    }
}

// Auto-provisions JDK 25 if it isn't already installed - carried over from
// the MDK's own settings.gradle, which uses the equivalent Groovy syntax:
//   plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' }
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
