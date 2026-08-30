// This is written from the general shape of the ModDevGradle-based MDK.
// Before relying on it, diff it against the real template at:
// https://github.com/NeoForgeMDKs/MDK-26.1.2-ModDevGradle
// (I don't have network access to fetch it directly while writing this -
// the plugin id, block names, and default run configs below should be
// cross-checked against that repo rather than assumed correct verbatim.)

plugins {
    id("java-library")
    id("net.neoforged.moddev") version "2.0.+"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

neoForge {
    version = "26.1.2"

    runs {
        create("client") {
            client()
        }
        create("server") {
            server()
        }
    }

    mods {
        create("horsegenetics") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation(project(":common"))
}
