import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.rt

plugins {
    id("com.tridium.niagara-module")
    id("com.tridium.niagara-signing")
    id("com.tridium.niagara-annotation-processors")
    id("com.tridium.convention.niagara-home-repositories")
}

description = "Control Basic runtime: wire-sheet block and interpreter"

moduleManifest {
    moduleName.set("controlBasic")
    runtimeProfile.set(rt)
}

dependencies {
    nre(":nre")
    api(":baja")
}

tasks.named<JavaCompile>("compileJava") {
    // The interpreter has no Niagara dependencies. Compile it into this jar
    // so the station does not need a second module on its classpath.
    source(project.fileTree("../controlbasic-core/src/main/java") {
        include("**/*.java")
    })
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("module.palette")
    from("module-include.xml")
    from("src/controlBasic.lexicon")
}
