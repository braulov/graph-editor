plugins {
    kotlin("jvm") version "2.1.10"
    id("org.openjfx.javafxplugin") version "0.0.13"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.1") // HTML-генерация
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.graphstream:gs-core:2.0")
    implementation("org.graphstream:gs-ui-javafx:2.0")
    implementation("com.google.code.gson:gson:2.10.1")
}
javafx {
    version = "21"
    modules = listOf("javafx.controls", "javafx.web")
}
application {
    mainClass.set("MainAppKt") // Указываем точку входа (имя файла без .kt)
}
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "21" // Целевая версия JVM
}

tasks.withType<JavaCompile> {
    options.release.set(21) // Целевая версия Java
}
