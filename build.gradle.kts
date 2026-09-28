plugins {
    id("dev.kikugie.loom-back-compat")
    id("me.modmuss50.mod-publish-plugin") version "2.1.1"
    kotlin("jvm")
}

fun getProp(prop: String): String = sc.properties[prop] as String

version = "${property("mod.version")}+${getProp("mod.mc_version")}"
base.archivesName = property("mod.id") as String

val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

val compatibleVersions: List<String> = sc.properties.rawOrNull("mod", "mc_releases")
    ?.asList().orEmpty().map { it.toString() }

repositories {
    /**
     * Restricts dependency search of the given [groups] to the [maven URL][url],
     * improving the setup speed.
     */
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    mavenCentral()
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.isxander.dev/releases")
    maven("https://maven.maxhenkel.de/repository/public")
}

dependencies {
    minecraft("com.mojang:minecraft:${sc.current.version}")
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:1.13.12+kotlin.2.4.0")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${getProp("deps.fabric_api")}")

    modImplementation("com.terraformersmc:modmenu:${getProp("deps.modmenu")}")
    modImplementation("dev.isxander:yet-another-config-lib:${getProp("deps.yacl")}")
    modImplementation("de.maxhenkel.voicechat:voicechat-api:${getProp("deps.vc_api")}")
    modImplementation("de.maxhenkel.voicechat:voicechat-api:${getProp("deps.vc_api")}:fabric-stub")
    runtimeOnly("maven.modrinth:simple-voice-chat:fabric-${getProp("deps.vc_mod")}")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1")
    }

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
    runConfigs.remove(runConfigs["server"])
}

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value = getProp(property)
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("fabric.mod.json") { expand(props) }
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        description = "Builds mod jars and copies results to `build/libs/{mod version}/`"

        inputs.property("version", project.property("mod.version"))
        from(loomx.modJar.flatMap { it.archiveFile }, loomx.modSourcesJar.flatMap { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
    }

    publishMods {
        file = loomx.modJar.get().archiveFile
        changelog = project.rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided."
        type = STABLE
        modLoaders.add("fabric")

        modrinth {
            projectId = property("publish.modrinth").toString()
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            environment = CLIENT_ONLY
            minecraftVersions.addAll(compatibleVersions)
            requires("fabric-language-kotlin")
            requires("modmenu")
            requires("simple-voice-chat")
            requires("yacl")
        }
    }
}