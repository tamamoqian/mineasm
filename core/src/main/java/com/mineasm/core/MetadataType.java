package com.mineasm.core;

/**
 * Mod metadata formats.
 * 1.7.x ~ 1.12.x use mcmod.info; 1.13.x ~ 1.20.x Forge use mods.toml;
 * NeoForge 1.20.4+ / 1.21.x use neoforge.mods.toml.
 */
public enum MetadataType {
    MCMOD_INFO("mcmod.info"),
    MODS_TOML("META-INF/mods.toml"),
    NEOFORGE_MODS_TOML("META-INF/neoforge.mods.toml"),
    FABRIC_JSON("fabric.mod.json"),
    NONE("(none)");

    private final String entryPath;

    MetadataType(String entryPath) {
        this.entryPath = entryPath;
    }

    public String entryPath() {
        return entryPath;
    }
}
