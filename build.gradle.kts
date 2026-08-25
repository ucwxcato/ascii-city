plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "com.asciicity"
    version = "0.1.0-SNAPSHOT"
}

tasks.register("gameRun") {
    group = "application"
    description = "Runs the standalone ASCII City game."
    dependsOn(":desktop:gameRun")
}
