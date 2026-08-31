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

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // Gradle 9 no longer puts the JUnit Platform launcher on the test runtime
    // classpath automatically - without this the test JVM fails to start with
    // "Failed to load JUnit Platform". Version comes from the BOM above.
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// Render sample coats through the real overlay pipeline (composer + gradient +
// white template) to build/coat-samples/ so the result can be eyeballed
// without launching the game. Dev tooling only.
tasks.register<JavaExec>("bakeCoatSamples") {
    group = "horsegenetics"
    description = "Render sample coats through CoatTextureComposer to build/coat-samples/"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.coat.pattern.CoatSampleTool")
    args(layout.buildDirectory.dir("coat-samples").get().asFile.absolutePath)
}
