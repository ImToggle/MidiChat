package me.imtoggle.midichat.config

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler
import dev.isxander.yacl3.config.v2.api.SerialEntry
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.ResourceLocation

class ModConfig {

    companion object {
        val CONFIG = ConfigClassHandler.createBuilder(ModConfig::class.java)
            .id(ResourceLocation.fromNamespaceAndPath("midichat", "config"))
            .serializer { config -> GsonConfigSerializerBuilder.create(config)
                .setPath(FabricLoader.getInstance().configDir.resolve("midichat.json"))
                .build()
            }
            .build()
    }

    @SerialEntry var instrument = "harp"

    @SerialEntry var sendVolume = 100

    @SerialEntry var localVolume = 100

}