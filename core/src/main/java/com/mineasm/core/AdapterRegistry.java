package com.mineasm.core;

import java.util.ServiceLoader;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Adapter registry: collects all version adapters from the classpath via
 * ServiceLoader and provides selection by MC version / platform.
 */
public final class AdapterRegistry {

    private final List<VersionAdapter> adapters = new ArrayList<VersionAdapter>();

    public AdapterRegistry() {
        for (VersionAdapter adapter : ServiceLoader.load(VersionAdapter.class)) {
            adapters.add(adapter);
        }
    }

    public List<VersionAdapter> all() {
        return new ArrayList<VersionAdapter>(adapters);
    }

    /** Looks up an adapter by exact MC version. */
    public VersionAdapter resolve(String mcVersion) {
        for (VersionAdapter a : adapters) {
            if (a.supportedMcVersions().contains(mcVersion)) {
                return a;
            }
        }
        return null;
    }

    /** Filters adapters by platform (registration order preserved). */
    public List<VersionAdapter> byPlatform(ModPlatform platform) {
        List<VersionAdapter> out = new ArrayList<VersionAdapter>();
        for (VersionAdapter a : adapters) {
            if (a.platform() == platform) {
                out.add(a);
            }
        }
        return out;
    }

    /** Union of all version families (for compatibility reports). */
    public Set<String> allSupportedVersions() {
        Set<String> out = new LinkedHashSet<String>();
        for (VersionAdapter a : adapters) {
            out.addAll(a.supportedMcVersions());
        }
        return out;
    }

    /**
     * Validates whether a mod candidate can be served by some adapter;
     * returns null when no adapter covers it.
     */
    public VersionAdapter match(ModCandidate candidate, String targetMcVersion) {
        // Prefer exact version match + platform agreement
        for (VersionAdapter a : adapters) {
            if (a.platform() == candidate.platform()
                    && a.supportedMcVersions().contains(targetMcVersion)
                    && a.accepts(candidate)) {
                return a;
            }
        }
        // Allow in-family fallback (e.g. a 1.7.2 mod entering a 1.7.10 modpack)
        for (VersionAdapter a : adapters) {
            if (a.platform() == candidate.platform() && a.accepts(candidate)) {
                return a;
            }
        }
        return null;
    }
}
