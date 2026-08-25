import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.lwjgl3)
    runtimeOnly(variantOf(libs.gdx.platform.natives.desktop) {
        classifier("natives-desktop")
    })

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(kotlin("test"))
    testRuntimeOnly(libs.junit.platform.launcher)
}

application {
    mainClass.set("com.asciicity.desktop.DesktopLauncherKt")
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir.parentFile.resolve("assets")
}

tasks.register<JavaExec>("gameRun") {
    group = "application"
    dependsOn(tasks.classes)
    mainClass.set(application.mainClass)
    classpath = sourceSets.main.get().runtimeClasspath
    workingDir = projectDir.parentFile.resolve("assets")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}
