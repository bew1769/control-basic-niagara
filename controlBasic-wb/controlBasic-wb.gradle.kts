import com.tridium.gradle.plugins.module.util.ModulePart.RuntimeProfile.wb

plugins {
    id("com.tridium.niagara-module")
    id("com.tridium.niagara-signing")
    id("com.tridium.niagara-annotation-processors")
    id("com.tridium.convention.niagara-home-repositories")
}

description = "Control Basic Workbench editor"

moduleManifest {
    moduleName.set("controlBasic")
    runtimeProfile.set(wb)
}

dependencies {
    nre(":nre")
    api(":baja")
    api(":bajaui-wb")
    // BTextEditor and BButton signatures reference javax.baja.gx.BImage, which lives in gx-rt.
    api(":gx-rt")
    api(":gx-wb")
    api(":workbench-wb")
    api(project(":controlBasic-rt"))
}

tasks.named<Jar>("jar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("module-include.xml")
}
