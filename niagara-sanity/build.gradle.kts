plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets.main {
    java.setSrcDirs(listOf(
        "../niagara-stubs/src",
        "../controlbasic-core/src/main/java",
        "../controlBasic-rt/src",
        "../controlBasic-wb/src"
    ))
}

tasks.withType<JavaCompile>().configureEach {
    // JDK 8 javac has no --release flag; source/targetCompatibility cover it there.
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(8)
    }
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

tasks.jar {
    enabled = false
}
