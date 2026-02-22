plugins {
    `java-library`
    `maven-publish`
}

allprojects {
    group = "com.dpk.helper"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // Skip applying java-library to the BOM project (it uses java-platform)
    if (name != "helper-bom") {
        apply(plugin = "java-library")
        apply(plugin = "maven-publish")

        java {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.compilerArgs.addAll(listOf("-parameters"))
        }

        dependencies {
            testImplementation(platform(rootProject.libs.junit.bom))
            testImplementation(rootProject.libs.junit.jupiter)
            testImplementation(rootProject.libs.assertj.core)
        }

        tasks.test {
            useJUnitPlatform()
        }

        publishing {
            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                }
            }
        }
    }
}
