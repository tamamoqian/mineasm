package com.mineasm.core;

import java.io.File;

/**
 * Mod candidate information produced by Jar scanning.
 */
public class ModCandidate {
    private final File jarFile;
    private final ModPlatform platform;
    private final String modId;
    private final String modVersion;
    private final String declaredMcVersion;
    private final MetadataType metadataType;

    public ModCandidate(File jarFile, ModPlatform platform, String modId,
                        String modVersion, String declaredMcVersion, MetadataType metadataType) {
        this.jarFile = jarFile;
        this.platform = platform;
        this.modId = modId;
        this.modVersion = modVersion;
        this.declaredMcVersion = declaredMcVersion;
        this.metadataType = metadataType;
    }

    public File jarFile() {
        return jarFile;
    }

    public ModPlatform platform() {
        return platform;
    }

    public String modId() {
        return modId;
    }

    public String modVersion() {
        return modVersion;
    }

    /** MC version declared by the mod metadata; may be null. */
    public String declaredMcVersion() {
        return declaredMcVersion;
    }

    public MetadataType metadataType() {
        return metadataType;
    }

    @Override
    public String toString() {
        return modId + " " + modVersion + " [" + platform.displayName() + "] -> " + jarFile.getName();
    }
}
