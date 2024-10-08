import org.gradle.kotlin.dsl.support.listFilesOrdered

plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

group = "app.revanced.bilibili"

dependencies {
    implementation(files("libs/revanced-patcher-19.3.1.1.jar"))
//    implementation(libs.revanced.patcher)
    implementation(libs.smali)
    // Used in JsonGenerator.
    implementation(libs.gson)
}

tasks.jar {
    archiveBaseName = "${rootProject.name}-${project.name}"
    exclude("app/revanced/generator")

    manifest {
        attributes["Name"] = "BiliRoamingX Patches"
        attributes["Description"] = "Patches for BiliRoamingX."
        attributes["Version"] = version
        attributes["Timestamp"] = System.currentTimeMillis().toString()
        attributes["Source"] = "git@github.com:BiliRoamingX/BiliRoamingX.git"
        attributes["Author"] = "Kofua"
        attributes["License"] = "GNU General Public License v3.0"
    }
}

tasks.register("buildDexJar") {
    description = "Build and add a DEX to the JAR file"
    group = "build"

    dependsOn(tasks.build)

    doLast {
//        val d8 = File(System.getenv("ANDROID_HOME")).resolve("build-tools")
//            .listFilesOrdered().last().resolve("d8").absolutePath
        val d8 = "C:\\Users\\efojug\\AppData\\Local\\Android\\Sdk\\build-tools\\35.0.0\\d8.bat"

        val patchesJar = configurations.archives.get().allArtifacts.files.files.first().absolutePath
        val workingDirectory = layout.buildDirectory.dir("libs").get().asFile

        exec {
            workingDir = workingDirectory
            commandLine = listOf(d8, "--release", patchesJar)
        }

        exec {
            workingDir = workingDirectory
            commandLine = listOf("powershell", "-Command", "Compress-Archive -Path classes.dex -DestinationPath $patchesJar -Update")
//            commandLine = listOf("zip", "-u", patchesJar, "classes.dex")
        }
    }
}

tasks.register<JavaExec>("generatePatchesFiles") {
    description = "Generate patches files"

    dependsOn(tasks.build)

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.revanced.generator.Main")
}

tasks.publish {
    dependsOn("buildDexJar")
    dependsOn("generatePatchesFiles")
}

tasks.register("dist") {
    group = "build"
    dependsOn("buildDexJar")
    dependsOn("generatePatchesFiles")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/BiliRoamingX/BiliRoamingX")
            credentials {
                username = project.findProperty("gpr.user") as String?
                    ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String?
                    ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("revanced-patches-publication") {
            from(components["java"])

            pom {
                name = "BiliRoamingX Patches"
                description = "Patches for BiliRoamingX."

                licenses {
                    license {
                        name = "GNU General Public License v3.0"
                        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
                    }
                }
                developers {
                    developer {
                        id = "zjns"
                        name = "Kofua"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/BiliRoamingX/BiliRoamingX.git"
                    developerConnection = "scm:git:git@github.com:BiliRoamingX/BiliRoamingX.git"
                    url = "https://github.com/BiliRoamingX/BiliRoamingX"
                }
            }
        }
    }
}

signing {
    useGpgCmd()

    sign(publishing.publications["revanced-patches-publication"])
}
