import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    api(libs.gdx)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
    testImplementation(libs.gdx.backend.headless)
    testRuntimeOnly(variantOf(libs.gdx.platform.natives.desktop) {
        classifier("natives-desktop")
    })
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir.parentFile.resolve("assets")
}

val validateContent by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Validates ASCII City registries, dialogue, objects, and the selected fixed map."
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.asciicity.content.ContentValidationCliKt")
    val selectedMap = providers.gradleProperty("contentMap").orElse("city-alpha")
    val assetsDirectory = projectDir.parentFile.resolve("assets")
    args(assetsDirectory.absolutePath, selectedMap.get())
    inputs.dir(assetsDirectory.resolve("data"))
    inputs.dir(assetsDirectory.resolve("maps"))
}

tasks.check {
    dependsOn(validateContent)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
