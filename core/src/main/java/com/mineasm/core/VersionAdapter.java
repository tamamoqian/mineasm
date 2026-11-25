package com.mineasm.core;

/**
 * Version adapter contract: one implementation per Forge / NeoForge MC version
 * generation, registered via META-INF/services (ServiceLoader).
 */
public interface VersionAdapter {

    /** Adapter identifier, e.g. "forge-1.7", "neoforge-1.21". */
    String id();

    /** MC version family, e.g. "1.7.x", "1.21.x". */
    String mcFamily();

    /** The exact set of MC versions this adapter can serve. */
    java.util.Set<String> supportedMcVersions();

    /** Target platform. */
    ModPlatform platform();

    /** Minimum Java major version required to launch mods of this generation. */
    int requiredJavaMajor();

    /** The mod metadata format expected by this generation. */
    MetadataType expectedMetadata();

    /**
     * Whether the adapter accepts this mod candidate (version family match,
     * platform match, metadata compatibility).
     */
    boolean accepts(ModCandidate candidate);

    /**
     * Pre-launch preparation: injects JVM properties, assembles the classpath,
     * hooks the native pipeline.
     */
    void prepareLaunch(LaunchContext context) throws Exception;
}
