/*
 * Root build for the Niagara module parts. Not used by `./gradlew test`.
 * Template: ci-richard-mcelhinney/nhaystack build.gradle.kts
 */

plugins {
    id("com.tridium.niagara")
    id("com.tridium.vendor")
    id("com.tridium.niagara-signing")
    id("com.tridium.convention.niagara-home-repositories")
    id("com.tridium.niagara-module") apply false
}

vendor {
    defaultVendor("bew1769")
    defaultModuleVersion("1.0.0")
}

subprojects {
    repositories {
        mavenCentral()
    }
}

signingServices {
    signingProfileFactory {
        allowDefaultProfile.set(false)
    }
}

val skipSigning = providers.gradleProperty("signing.skip").orElse("false").get() == "true"

niagaraSigning {
    val certAlias = providers.gradleProperty("certAlias")
        .orElse(providers.systemProperty("certAlias"))
        .orElse(providers.environmentVariable("NIAGARA_SIGNING_ALIAS"))
        .orElse(providers.environmentVariable("CERT_ALIAS"))
        .orElse("controlbasic")
        .get()
    aliases.set(listOf(certAlias))

    if (!skipSigning) {
        val profile = providers.gradleProperty("niagara.signing.profile")
            .orElse(providers.systemProperty("niagara.signing.profile"))
            .orElse(providers.environmentVariable("NIAGARA_SIGNING_PROFILE"))
            .orElse(providers.environmentVariable("SIGNING_PROFILE"))
            .orNull

        if (profile.isNullOrBlank() || !file(profile).isFile) {
            throw GradleException(
                "Module signing profile is missing. Run scripts/generate-dev-cert.sh (or .bat), " +
                    "then set niagara.signing.profile in local.properties or NIAGARA_SIGNING_PROFILE. " +
                    "See docs/SIGNING.md. Refusing to fall back to an unsigned or throwaway certificate. " +
                    "Pass -Psigning.skip=true only to run :createProfile before a profile exists."
            )
        }
        signingProfileFile.set(file(profile))
    }
}
