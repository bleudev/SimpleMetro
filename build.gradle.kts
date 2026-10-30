import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.serialization") version "2.4.21"
    id("net.fabricmc.fabric-loom") version "1.18.4"
    id("maven-publish")
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    // if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()
}


fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = URI.create("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.quiltmc.org/repository/release") }
        filter { includeGroupAndSubgroups("org.quiltmc") }
    }
}

dependencies {
    // To change the versions see the gradle.properties file
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    implementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    implementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")

    implementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    implementation("maven.modrinth:xaeros-world-map:${project.property("xaeros_world_map")}")
    implementation("maven.modrinth:xaeros-minimap:${project.property("xaeros_minimap")}")
    implementation(files("libs/xaerolib-fabric-26.3-1.7.17.jar"))

    implementation("maven.modrinth:modmenu:21.0.0")
    implementation("maven.modrinth:simple-voice-chat:${project.property("simple_voice_chat")}")

    implementation("org.quiltmc.parsers:gson:0.3.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", project.property("minecraft_version"))
    inputs.property("loader_version", project.property("loader_version"))
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to project.property("minecraft_version")!!,
            "loader_version" to project.property("loader_version")!!,
            "kotlin_loader_version" to project.property("kotlin_loader_version")!!
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    // ensure that the encoding is set to UTF-8, no matter what the system default is
    // this fixes some edge cases with special characters not displaying correctly
    // see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
    // If Javadoc is generated, this must be specified in that task too.
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.property("archives_base_name") as String
            from(components["java"])
        }
    }

    // See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
    repositories {
        // Add repositories to publish to here.
        // Notice: This block does NOT have the same function as the block in the top level.
        // The repositories here will be used for publishing your artifact, not for
        // retrieving dependencies.
    }
}
