// ixoras world converter: a standalone Java jar that moves a world saved under the old namespace
// (horsegenetics) to the new one (ixoras_horses), into a NEW folder. No third-party library and no
// Minecraft: the JDK reads zlib and gzip, and the NBT/region reader is in this module.
//
// NOT YET LISTED in settings.gradle.kts (a deliberate hold: another change was in flight when this was
// written). To turn it on, add this one line to settings.gradle.kts, then:
//     include(":converter")
//     ./gradlew :converter:jar            -> converter/build/libs/ixoras-converter.jar
//     java -jar converter/build/libs/ixoras-converter.jar --self-test
// WRITTEN, NEVER RUN. See tools/rename/README.txt for the order.
plugins {
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.jar {
    archiveBaseName.set("ixoras-converter")
    archiveVersion.set("")
    manifest {
        attributes["Main-Class"] = "ixoras.converter.Main"
    }
}
