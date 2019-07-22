@file:Suppress("UNUSED_VARIABLE")

import com.jfrog.bintray.gradle.BintrayExtension
import com.jfrog.bintray.gradle.BintrayPlugin
import io.gitlab.arturbosch.detekt.DetektPlugin
import io.gitlab.arturbosch.detekt.detekt
import kr.motd.gradle.sphinx.gradle.SphinxTask
import org.jetbrains.kotlin.backend.common.onlyIf
import java.util.*

plugins {
    `maven-publish`
    kotlin("multiplatform") version "1.3.41"
    id("org.ajoberstar.reckon") version "0.11.0"
    id("com.github.ben-manes.versions") version "0.21.0"
    id("io.gitlab.arturbosch.detekt") version "1.0.0-RC16" apply false
    id("com.jfrog.bintray") version "1.8.4" apply false
    id("kr.motd.sphinx") version "2.5.0"
}

reckon {
    scopeFromProp()
    stageFromProp("alpha", "beta", "rc", "final")
}

allprojects {
    group = "com.github.jcornaz.kwik"

    repositories {
        mavenCentral()
        jcenter()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
    apply<DetektPlugin>()
    apply<BintrayPlugin>()
    apply<MavenPublishPlugin>()

    kotlin {
        jvm()

        sourceSets {
            commonMain {
                dependencies {
                    api(kotlin("stdlib-common"))
                }
            }

            commonTest {
                dependencies {
                    api(kotlin("test-common"))
                    api(kotlin("test-annotations-common"))
                }
            }

            val jvmMain by existing {
                dependencies {
                    implementation(kotlin("stdlib"))
                }
            }

            val jvmTest by existing {
                dependencies {
                    implementation(kotlin("test"))
                    implementation(kotlin("test-junit"))
                }
            }
        }
    }

    detekt {
        input = files(
            "src/commonMain/kotlin",
            "src/jvmMain/kotlin"
        )
        buildUponDefaultConfig = true
        config = files("$rootDir/detekt-config.yml")
    }

    publishing {
        publications.withType<MavenPublication>().apply {
            val metadata by getting {
                artifactId = "kwik-${project.name}-common"
            }

            val jvm by getting {
                artifactId = "kwik-${project.name}-jvm"
            }
        }
    }

    configure<BintrayExtension> {
        user = System.getenv("BINTRAY_USER")
        key = System.getenv("BINTRAY_KEY")
        publish = true

        override = true

        with(pkg) {
            userOrg = "kwik"
            name = "kwik"
            repo = when {
                '+' in project.version.toString() -> "dev"
                '-' in project.version.toString() -> "preview"
                else -> "stable"
            }

            setLicenses("Apache-2.0")

            vcsUrl = "https://github.com/jcornaz/kwik"
            githubRepo = "jcornaz/kwik"

            with(version) {
                name = project.version.toString()
                released = Date().toString()
                if ('+' !in project.version.toString()) {
                    vcsTag = project.version.toString()
                }
            }
        }

        setPublications("metadata", "jvm")
    }

    tasks.named("bintrayUpload") {
        dependsOn("check")
    }
}

tasks {
    register("version") {
        doLast {
            println(project.version)
        }
    }

    val sphinx by existing {
        inputs.file("$rootDir/CHANGELOG.rst")
        inputs.file("$rootDir/README.rst")
    }

    withType<SphinxTask> {
        setSourceDirectory("$rootDir/docs")
    }
}
