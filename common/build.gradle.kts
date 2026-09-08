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
    // The exhaustive tests scale as 3^genes: CoatTextureIdTest walks every
    // genotype (177 147 at 11 genes) and CoatTextureComposerTest bakes 2^genes
    // coats. Gradle's 512 MB default stopped being enough at 11 genes.
    maxHeapSize = "2g"
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

// Bake the gene-spec parity fixtures the creator checks itself against. The
// creator previews genes with a JavaScript twin of SpecPainter; this writes what
// the real Java engine produces so wiki/gene-creator/tools/check-parity.mjs can
// prove the two still agree. Dev tooling only.
tasks.register<JavaExec>("bakeSpecFixtures") {
    group = "horsegenetics"
    description = "Write wiki/gene-creator/fixtures/expected.json from the real spec engine"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.genetics.spec.SpecFixtureTool")
    args(rootProject.layout.projectDirectory.file("wiki/gene-creator/fixtures/expected.json").asFile.absolutePath)
}

// Regenerate the gene creator's two generated files - the inlined coat textures
// and the worked examples - from the mod's own assets and example genes.
tasks.register<JavaExec>("bakeCreatorAssets") {
    group = "horsegenetics"
    description = "Regenerate wiki/gene-creator/assets/textures.js and js/examples.js"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.genetics.spec.CreatorAssetTool")
    args(rootProject.layout.projectDirectory.dir("wiki/gene-creator").asFile.absolutePath)
}

// The wiki's page for each data-driven gene, plus the generated gene lists on
// the sidebar and the landing page. See GeneWikiTool for why these pages in
// particular are not hand-written.
tasks.register<JavaExec>("bakeGeneWikiPages") {
    group = "horsegenetics"
    description = "Write wiki/gene-*.html and the generated spans of pages.js and index.html"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.genetics.spec.GeneWikiTool")
    args(rootProject.layout.projectDirectory.dir("wiki").asFile.absolutePath)
}

// One side-on snapshot per data-driven gene, on a standard bay, for the wiki's
// gene index. Also the fastest way to look at every gene at once - see the class.
tasks.register<JavaExec>("bakeGeneIcons") {
    group = "horsegenetics"
    description = "Write wiki/assets/gene-icons/*.png - one bay horse per data-driven gene"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.coat.pattern.GeneIconTool")
    args(rootProject.layout.projectDirectory.dir("wiki/assets/gene-icons").asFile.absolutePath)
}

// Concatenate the shipped gene files into the single array the browser fetches.
// The wiki tools cannot walk a classpath index; see GeneFileTool.
tasks.register<JavaExec>("bakeGeneBundle") {
    group = "horsegenetics"
    description = "Write wiki/horse-designer/assets/genes.json from horsegenetics/genes/"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.genetics.spec.GeneFileTool")
    args(rootProject.layout.projectDirectory.file("wiki/horse-designer/assets/genes.json").asFile.absolutePath)
}

// Write every registered breed out as a JSON file plus the classpath index the
// loader reads. This is how the built-in breeds became data files, and how a
// Java-defined breed is exported for someone to edit. Dev tooling only.
tasks.register<JavaExec>("bakeBreedFiles") {
    group = "horsegenetics"
    description = "Write common/src/main/resources/horsegenetics/breeds/ from the registered breeds"
    dependsOn(tasks.named("classes"))
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.example.horsegenetics.common.breed.spec.BreedFileTool")
    args(layout.projectDirectory.dir("src/main/resources/horsegenetics/breeds").asFile.absolutePath,
            // ...and the browser's single-file copy of the same content, which
            // the wiki tools fetch because TeaVM cannot read a classpath index.
            rootProject.layout.projectDirectory.file("wiki/horse-designer/assets/breeds.json").asFile.absolutePath)
}
