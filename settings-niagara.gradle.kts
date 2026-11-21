/*
 * Niagara module build. Run with:
 *   ./gradlew -c settings-niagara.gradle.kts jar
 *
 * Layout follows ci-richard-mcelhinney/nhaystack settings.gradle.kts
 * (Niagara Gradle plugin 7.6.x, which Tridium documents as building 4.10+).
 */

import java.util.Properties

pluginManagement {
    val loaded = Properties()
    val localFile = file("local.properties")
    if (localFile.exists()) {
        localFile.inputStream().use { loaded.load(it) }
    }
    val gradleFile = file("gradle.properties")
    val gradleProps = Properties()
    if (gradleFile.exists()) {
        gradleFile.inputStream().use { gradleProps.load(it) }
    }
    fun prop(name: String): String? {
        val fromLocal = loaded.getProperty(name)
        if (!fromLocal.isNullOrBlank()) {
            if (System.getProperty(name) == null) {
                System.setProperty(name, fromLocal)
            }
            return fromLocal
        }
        System.getProperty(name)?.let { if (it.isNotBlank()) return it }
        System.getenv(name)?.let { if (it.isNotBlank()) return it }
        gradleProps.getProperty(name)?.let { if (it.isNotBlank()) return it }
        return null
    }

    val niagaraHome = prop("niagara_home")
        ?: System.getenv("NIAGARA_HOME")
        ?: System.getenv("niagara_home")
    val pluginHome = prop("gradlePluginHome")
        ?: System.getenv("GRADLE_PLUGIN_HOME")
        ?: niagaraHome?.let { "$it/etc/m2/repository" }

    if (pluginHome.isNullOrBlank()) {
        throw GradleException(
            "Set niagara_home (or NIAGARA_HOME) to a Niagara 4.10+ install before building the module. " +
                "See README.md. Interpreter tests do not need this: ./gradlew test"
        )
    }

    val pluginVersion = prop("gradlePluginVersion") ?: "7.6.22"
    val settingsVersion = prop("settingsPluginVersion") ?: "7.6.3"

    repositories {
        maven { url = uri(pluginHome) }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        id("com.tridium.niagara") version pluginVersion
        id("com.tridium.vendor") version pluginVersion
        id("com.tridium.niagara-module") version pluginVersion
        id("com.tridium.niagara-signing") version pluginVersion
        id("com.tridium.convention.niagara-home-repositories") version pluginVersion
        id("com.tridium.niagara-annotation-processors") version pluginVersion
        id("com.tridium.settings.multi-project") version settingsVersion
    }
}

rootProject.name = "controlBasic"
rootProject.buildFileName = "niagara.gradle.kts"

include("controlBasic-rt")
include("controlBasic-wb")
project(":controlBasic-rt").buildFileName = "controlBasic-rt.gradle.kts"
project(":controlBasic-wb").buildFileName = "controlBasic-wb.gradle.kts"
