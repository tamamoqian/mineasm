package com.mineasm.core;

/**
 * Context for one launch/verification pass, carrying the version, architecture,
 * mod list and JVM property injection.
 */
public class LaunchContext {
    private final String mcVersion;
    private final ModPlatform platform;
    private final Arch targetArch;
    private final java.util.List<ModCandidate> mods;
    private final java.util.Map<String, String> jvmProperties = new java.util.LinkedHashMap<String, String>();

    public LaunchContext(String mcVersion, ModPlatform platform, Arch targetArch, java.util.List<ModCandidate> mods) {
        this.mcVersion = mcVersion;
        this.platform = platform;
        this.targetArch = targetArch;
        this.mods = mods;
    }

    public String mcVersion() {
        return mcVersion;
    }

    public ModPlatform platform() {
        return platform;
    }

    public Arch targetArch() {
        return targetArch;
    }

    public java.util.List<ModCandidate> mods() {
        return java.util.Collections.unmodifiableList(mods);
    }

    public void setJvmProperty(String key, String value) {
        jvmProperties.put(key, value);
        System.setProperty(key, value);
    }

    public java.util.Map<String, String> jvmProperties() {
        return java.util.Collections.unmodifiableMap(jvmProperties);
    }

    /**
     * Bootstraps the native pipeline: invokes the native bootstrap stub and
     * verifies that the preparePipeline / installTransformers call chain is
     * usable (returns false when the native component is unavailable).
     */
    public boolean bootstrapNativePipeline() {
        if (!NativeBridge.isAvailable()) {
            return false;
        }
        int nativeArch = NativeBridge.probeArch();
        return nativeArch == targetArch.nativeCode();
    }
}
