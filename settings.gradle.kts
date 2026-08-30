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
