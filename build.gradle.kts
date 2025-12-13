import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.internal.os.OperatingSystem
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Zip

plugins {
    base
    java
    application                                    // for "run" task
    id("edu.sc.seis.launch4j") version "4.0.0"     // for generating Windows .exe
    id("io.github.goooler.shadow") version "8.1.8" // only needed to make linux build
}

val appWorkingDir = layout.buildDirectory.dir("app")
val appVersion = version.toString()  // from gradle.properties

// copy resources to build directory
tasks.register<Copy>("copyResources") {
    inputs.property("version", appVersion)

    from("$projectDir/src/main/resources")
    into(appWorkingDir)

    filesMatching("**/*.txt") {
        // replace @APP_VERSION@ in resource files with string taken from gradle.properties
        filter<ReplaceTokens>("tokens" to mapOf("APP_VERSION" to appVersion))
    }
}

// set JDK version (from gradle.properties)
val jdkVersion: JavaLanguageVersion = JavaLanguageVersion.of((project.property("jdkVersion") as String).toInt())
java {
    toolchain {
        languageVersion.set(jdkVersion)
    }
}
val javaHome: File = javaToolchains.launcherFor {
    languageVersion.set(jdkVersion)
}.get().metadata.installationPath.asFile

// creates folder "jre"
tasks.register<Exec>("createRuntime") {
    println("Using Java Home: $javaHome")

    val jlink = file(
        "${javaHome}/bin/" +
                if (OperatingSystem.current().isWindows) "jlink.exe" else "jlink"
    )
    val outputDir = layout.buildDirectory.dir("jre").get().asFile

    inputs.file(jlink)
    outputs.dir(outputDir)

    doFirst {
        outputDir.deleteRecursively()
    }

    /* Note: The set of modules below has been determined with jdeps as follows:
    1. Build the project: ./gradlew build
    2. Remove build/libs/particle-life-app.jar
    3. Run jdeps --print-module-deps --ignore-missing-deps --module-path build/libs build/libs/particle-life-app-all.jar
    The output lists all required modules in the last line. */
    commandLine(
        jlink,
        "--no-header-files",
        "--no-man-pages",
        "--strip-debug",
        "--module-path", "$javaHome/jmods",
        "--compress", "zip-0", // no compression here -> will be compressed in final zip
        "--add-modules",
        "java.base,java.desktop,java.scripting,java.sql,jdk.unsupported",
        "--output", outputDir
    )
}

tasks.register<Copy>("copyJre") {
    dependsOn("createRuntime")
    from(layout.buildDirectory.dir("jre"))
    into(appWorkingDir.map { it.dir("jre") })
}

launch4j {
    icon = "$projectDir/favicon.ico"
    mainClassName = "com.particle_life.app.Main"
    dontWrapJar = true
    requires64Bit = true
    bundledJrePath = "jre"
}

tasks.register<Copy>("copyLaunch4j") {
    dependsOn("createExe")
    from(layout.buildDirectory.dir("launch4j"))
    into(appWorkingDir)
}

tasks.register("assembleApp") {
    dependsOn("copyResources", "copyJre", "copyLaunch4j")
}

tasks.register<Zip>("zipApp") {
    dependsOn("assembleApp")
    from(appWorkingDir)
    destinationDirectory.set(layout.buildDirectory.dir("zipApp"))
    archiveFileName.set("particle-life-app.zip")
}

application {
    mainClass.set("com.particle_life.app.Main") // required for "run" task
    if (OperatingSystem.current().isMacOsX) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}

// when running app with "./gradlew run",
// resources must be copied, as we don't include them in the JAR
tasks.named<JavaExec>("run") {
    dependsOn("copyResources")
    workingDir(appWorkingDir)
}

group = "com.particle.life.app"

// exclude resources from particle-life-app-<version>.jar
// (we store them outside, next to the executable instead)
tasks.processResources {
    exclude("**/*")
}

val imGuiVersion = "1.86.2"

// set imGuiNatives
val imGuiNatives: String = when {
    OperatingSystem.current().isLinux -> {
        val osArch = System.getProperty("os.arch")
        if (osArch.startsWith("arm") || osArch.startsWith("aarch64")) {
            "linux-" +
                    if (osArch.contains("64") || osArch.startsWith("armv8")) "arm64" else "arm32"
        } else {
            "linux"
        }
    }

    OperatingSystem.current().isMacOsX -> "macos"

    OperatingSystem.current().isWindows -> {
        val osArch = System.getProperty("os.arch")
        if (osArch.contains("64")) {
            "windows" + if (osArch.startsWith("aarch64")) "-arm64" else ""
        } else {
            "windows-x86"
        }
    }

    else -> error("Unsupported OS for imGui natives")
}

val lwjglVersion = "3.3.6"

// set lwjglNatives
// code generated with https://www.lwjgl.org/customize (Linux x64, macOS x64, macOS arm64, Windows x64)
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            "natives-linux"

        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"

        arrayOf("Windows").any { name.startsWith(it) } ->
            "natives-windows"

        else ->
            throw Error("Unrecognized or unsupported platform. Please set \"lwjglNatives\" manually")
    }
}

repositories {
    mavenCentral()
    mavenLocal()

    // JitPack allows to easily add dependencies on GitHub repos
    // (e.g. implementation 'com.github.User:Repo:Tag')
    maven(url = "https://jitpack.io")  // this line should be at the end of the repositories
}

dependencies {
    // Particle Life Backend
    implementation("com.github.tom-mohr:particle-life:v0.5.2")

    // YAML Parser "YamlBeans"
    implementation("com.esotericsoftware.yamlbeans:yamlbeans:1.17")

    // TOML Parser
    implementation("com.moandjiezana.toml:toml4j:0.7.2")

    // Apache Commons Text (for Levenshtein distance)
    implementation("org.apache.commons:commons-text:1.12.0")

    // Linear Algebra Library "JOML"
    implementation("org.joml:joml:1.10.1")

    // GUI Library "Dear ImGui" from https://github.com/SpaiR/imgui-java
    implementation("io.github.spair:imgui-java-binding:$imGuiVersion")
    implementation("io.github.spair:imgui-java-natives-$imGuiNatives:$imGuiVersion")

    // Lightweight Java Game Library "LWJGL" (code from https://www.lwjgl.org/customize)

    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
}
