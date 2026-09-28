package me.imtoggle.midichat.core

import me.imtoggle.midichat.config.ModConfig
import net.minecraft.client.Minecraft

val mc
    get() = Minecraft.getInstance()

val config
    get() = ModConfig.CONFIG.instance()