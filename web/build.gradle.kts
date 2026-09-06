// SPIKE (2026-09-06): can TeaVM compile common/ to JavaScript?
//
// The question this module exists to answer is narrow: the horse designer is
// hosted on GitHub Pages, which is static, so there is no server to run the
// real coat pipeline on - and hand-porting genes to JavaScript is the thing we
// are trying to stop doing. TeaVM compiles JVM *bytecode*, so it is not a
// rewrite: it is common/ itself, running in the browser.
//
// common/ is an unusually good candidate - Java 17, zero third-party
// dependencies, no reflection anywhere in the coat pipeline, and
// CoatTextureComposer.compose takes int[] and returns int[], so nothing has to
// cross the boundary but numbers.
plugins {
    id("java")
    id("org.teavm") version "0.15.0"
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        // TeaVM itself needs 17+; common/ targets 17.
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    implementation(project(":common"))
}

teavm {
    // The WebAssembly-GC backend. Worth testing against JS because the coat
    // pipeline's hot loop is BodyNoise, whose hash is 64-bit integer maths -
    // native in wasm, emulated with pairs of 32-bit ints in JavaScript.
    wasmGC {
        mainClass.set("com.example.horsegenetics.web.CoatSpike")
        obfuscated.set(true)
        sourceMap.set(false)
    }

    js {
        mainClass.set("com.example.horsegenetics.web.CoatSpike")
        // What ships: minified, no source map. Flip both while debugging.
        obfuscated.set(true)
        sourceMap.set(false)
        optimization.set(org.teavm.gradle.api.OptimizationLevel.AGGRESSIVE)
    }
}
