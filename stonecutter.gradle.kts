plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1~1.21.10"

stonecutter parameters {
    swaps["mod_version"] = "\"${property("mod.version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
    dependencies["fapi"] = node.project.property("deps.fabric_api") as String

    replacements {
        string(current.parsed >= "1.21.11") {
            replace("ResourceLocation", "Identifier")
        }

        string(current.parsed > "1.21.11") {
            replace("ClientCommandManager", "ClientCommands")
        }
    }
}
