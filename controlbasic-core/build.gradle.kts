plugins {
    `java-library`
}

group = "com.controlbasic"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    // JDK 8 javac has no --release flag; source/targetCompatibility cover it there.
    if (JavaVersion.current().isJava9Compatible) {
        options.release.set(8)
    }
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:-options")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to "Control Basic core",
            "Implementation-Version" to version
        )
    }
}
