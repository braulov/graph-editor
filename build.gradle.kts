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
    implementation("org.openjfx:javafx-controls:21")
    implementation("org.openjfx:javafx-web:21")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.openjfx:javafx-controls:21")
    testImplementation("org.openjfx:javafx-web:21")
    testImplementation("org.testfx:testfx-core:4.0.16-alpha")
    testImplementation("org.testfx:testfx-junit5:4.0.16-alpha")

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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
    afterSuite(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
        if (desc.parent == null) { // Проверяем, что это корневой suite
            println("Test results: ${result.resultType} " +
                    "(${result.testCount} tests, " +
                    "${result.successfulTestCount} passed, " +
                    "${result.failedTestCount} failed, " +
                    "${result.skippedTestCount} skipped)")
        }
    }))
}

