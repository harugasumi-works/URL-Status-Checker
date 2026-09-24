plugins {
    `java-library`
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("tools.jackson.core:jackson-databind:3.2.2")
    implementation("tools.jackson.dataformat:jackson-dataformat-csv:3.2.2")
}

application {
    mainClass.set("statuscheck.ui.Main")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
    jvmArgs("--enable-preview")
}

tasks.register<Test>("integrationTest") {
    description = "Runs the tests tagged 'integration' (makes real HTTPS requests)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("integration")
    }
    jvmArgs("--enable-preview")
    shouldRunAfter(tasks.named("test"))
}

tasks.named<JavaExec>("run") {
    jvmArgs("--enable-preview")
}

javafx {
	version = "26"
    modules("javafx.graphics", "javafx.controls", "javafx.fxml")
}