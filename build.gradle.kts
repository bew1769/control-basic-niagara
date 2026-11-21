plugins {
    base
}

tasks.register("test") {
    dependsOn(":controlbasic-core:test")
    group = "verification"
    description = "Run the Control Basic interpreter tests."
}

tasks.register("sanity") {
    dependsOn(":niagara-sanity:compileJava")
    group = "verification"
    description = "Compile the Niagara rt/wb sources against local stubs."
}
