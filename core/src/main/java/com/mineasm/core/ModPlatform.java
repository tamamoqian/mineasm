package com.mineasm.core;

/** Mod platform type. */
public enum ModPlatform {
    FORGE("Forge"),
    NEOFORGE("NeoForge"),
    FABRIC("Fabric"),
    UNKNOWN("Unknown");

    private final String displayName;

    ModPlatform(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
