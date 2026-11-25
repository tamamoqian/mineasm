package com.mineasm.core;

/**
 * Version adapter base class: implements the generation-agnostic
 * accepts / prepareLaunch logic. Per-generation adapters only declare their
 * version set, platform, metadata type and launch properties.
 */
public abstract class AbstractVersionAdapter implements VersionAdapter {

    @Override
    public boolean accepts(ModCandidate candidate) {
        if (candidate == null) {
            return false;
        }
        if (candidate.platform() != platform()) {
            return false;
        }
        if (expectedMetadata() == MetadataType.MCMOD_INFO
                && candidate.metadataType() != MetadataType.MCMOD_INFO) {
            return false;
        }
        if (expectedMetadata() == MetadataType.MODS_TOML
                && candidate.metadataType() != MetadataType.MODS_TOML
                && candidate.metadataType() != MetadataType.NEOFORGE_MODS_TOML) {
            return false;
        }
        if (expectedMetadata() == MetadataType.NEOFORGE_MODS_TOML
                && candidate.metadataType() != MetadataType.NEOFORGE_MODS_TOML) {
            return false;
        }
        return true;
    }

    @Override
    public void prepareLaunch(LaunchContext context) throws Exception {
        // 1) Check the Java major version (compatibility verification can simulate
        //    the target runtime via mineasm.simulated.java.major)
        String effective = System.getProperty("mineasm.simulated.java.major",
                System.getProperty("java.version", "8"));
        int javaMajor = parseJavaMajor(effective);
        if (javaMajor < requiredJavaMajor()) {
            throw new IllegalStateException(id() + " requires Java " + requiredJavaMajor()
                    + "+; current runtime is Java " + javaMajor);
        }
        // 2) Inject generation-specific launch properties
        applyLaunchProperties(context);
        // 3) Hook the native loader pipeline (when available)
        context.bootstrapNativePipeline();
    }

    /** Each generation injects its own JVM properties and FML/NeoForge loading arguments. */
    protected abstract void applyLaunchProperties(LaunchContext context);

    static int parseJavaMajor(String version) {
        try {
            if (version.startsWith("1.")) {
                return Integer.parseInt(version.substring(2, version.indexOf('.', 2)));
            }
            int dot = version.indexOf('.');
            if (dot > 0) {
                return Integer.parseInt(version.substring(0, dot));
            }
            return Integer.parseInt(version);
        } catch (Exception e) {
            return 8;
        }
    }
}
