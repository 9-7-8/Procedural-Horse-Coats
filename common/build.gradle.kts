// Deliberately a plain java-library module with NO NeoForge/Minecraft dependency.
// This is what makes it portable: if this build script ever needs a Minecraft or
// NeoForge import to compile, something has leaked across the boundary and belongs
// in a version module instead.

plugins {
    id("java-library")
}

java {
    toolchain {
        // Match whatever the *lowest* Minecraft version you plan to support needs.
        // 1.12.2 mods traditionally target Java 8; NeoForge 26.1.2 requires Java 25.
        // Since common has no MC dependency, target the lowest common denominator
        // so both version modules can consume it without toolchain conflicts.
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}
