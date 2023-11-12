import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0"
    idea
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.tfowl.netlogtools"
version = "1.0-SNAPSHOT"

application {
    mainClass = "com.tfowl.netlogtools.cli.Main"
}

repositories {
    mavenCentral()
}

//val generatedLogTypeDir = "${project.buildDir}/log-types/kotlin"
//
//val generateLogTypes = tasks.register("generateLogTypes") {
//    outputs.dir(generatedLogTypeDir)
//
//    doLast {
//        fun generateNetLogEnumType(packageName: String, name: String, constants: List<String>): String {
//            return buildString {
//                appendLine("package $packageName")
//                appendLine("data class $name(override val label: String): NetLogEnum<$name> {")
//                appendLine("    override fun toString(): String = label")
//                appendLine("    companion object : NetLogEnumFactory<$name> {")
//                constants.forEach { constant ->
//                    appendLine("        val $constant = $name(\"$constant\")")
//                }
//                appendLine("        private val cache = mutableMapOf(")
//                constants.forEach { constant ->
//                    appendLine("            \"$constant\" to $constant,")
//                }
//                appendLine("        )")
//                appendLine("    override fun lookup(label: String): $name? = cache[label]")
//                appendLine("    override fun create(label: String): $name = lookup(label) ?: $name(label).also { cache += label to it }")
//                appendLine("    override fun values(): List<$name> = cache.values.toList()")
//                appendLine("    }")
//                appendLine("}")
//            }
//        }
//
//        fun fetchEnumConstantsFromHeader(client: HttpClient, sourceUrl: String, definingMacro: String): List<String> {
//            val source = client.send(
//                HttpRequest.newBuilder(URI.create(sourceUrl)).GET().build(), BodyHandlers.ofString()
//            ).body()
//
//            return source.lines()
//                .filter { it.startsWith("$definingMacro(") }
//                .map { it.removeSurrounding("$definingMacro(", ")") }
//        }
//
//        fun writeKotlinSourceFile(path: Path, @Language("kotlin") source: String) {
//            println("Writing ${path.absolute()}")
//            path.parent.createDirectories()
//            Files.writeString(path, source, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
//        }
//
//        fun createNetLogEnumFromHeader(
//            client: HttpClient, sourceUrl: String, macro: String, packageName: String, name: String, path: Path,
//        ) {
//            val constants = fetchEnumConstantsFromHeader(client, sourceUrl, macro)
//            val kotlinSource = generateNetLogEnumType(packageName, name, constants)
//            writeKotlinSourceFile(path, kotlinSource)
//        }
//
//        val client = HttpClient.newHttpClient()
//
//        val packageName = "com.tfowl.netlogtools.netlog"
//
//        val root = Path.of(generatedLogTypeDir, packageName.replace('.', '/'))
//
//        // Base NetLogEnum with some convenience functions on the Factory (companion object)
//        writeKotlinSourceFile(
//            root.resolve("NetLogEnum.kt"), """
//package com.tfowl.netlogtools.netlog
//interface NetLogEnum<E : NetLogEnum<E>> {
//    val label: String
//}
//
//interface NetLogEnumFactory<E : NetLogEnum<E>> {
//    fun lookup(label: String): E?
//    fun create(label: String): E
//    fun values(): List<E>
//}"""
//        )
//
//        writeKotlinSourceFile(
//            root.resolve("Phase.kt"),
//            generateNetLogEnumType(packageName, "Phase", listOf("PHASE_BEGIN", "PHASE_NONE", "PHASE_END"))
//        )
//
//        createNetLogEnumFromHeader(
//            client,
//            "https://raw.githubusercontent.com/chromium/chromium/main/net/log/net_log_source_type_list.h",
//            "SOURCE_TYPE",
//            packageName,
//            "SourceType",
//            root.resolve("SourceType.kt")
//        )
//
//        createNetLogEnumFromHeader(
//            client,
//            "https://raw.githubusercontent.com/chromium/chromium/main/net/log/net_log_event_type_list.h",
//            "EVENT_TYPE",
//            packageName,
//            "EventType",
//            root.resolve("EventType.kt")
//        )
//    }
//}

tasks.withType<KotlinCompile>().all {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
//    dependsOn(generateLogTypes)
}

tasks.withType<JavaCompile>().all {
//    dependsOn(generateLogTypes)
}

sourceSets {
    main {
//        kotlin.srcDir(generatedLogTypeDir)
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
//        generatedSourceDirs.add(file(generatedLogTypeDir))
    }
}

tasks.named("clean") {
//    delete(generatedLogTypeDir)
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-okio:1.6.0")

    implementation("org.hildan.har:har-parser:0.6.0")


    implementation(platform("io.ktor:ktor-bom:2.3.6"))
    implementation("io.ktor:ktor-http")
    implementation("io.ktor:ktor-utils")
    implementation("io.ktor:ktor-network")
    implementation("com.squareup.okio:okio:3.5.0")

    implementation("com.github.ajalt.clikt:clikt:4.2.0")

    implementation("org.apache.logging.log4j:log4j-core:2.20.0")
    runtimeOnly("org.apache.logging.log4j:log4j-slf4j18-impl:2.18.0")
}
