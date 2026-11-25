package com.mineasm.adapter.v17;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.7.x adapter (1.7.2 / 1.7.10).
 *
 * 1.7 generation characteristics:
 *   - Metadata: mcmod.info (JSON array)
 *   - Launch mechanism: Launchwrapper Tweaker system (cpw.mods.fml.common.launcher.FMLTweaker)
 *   - Runtime: Java 6/7/8
 *   - Certificate issues are common; fml.ignoreInvalidMinecraftCertificates must be enabled
 */
public class Forge17Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = new LinkedHashSet<String>(
            Arrays.asList("1.7.2", "1.7.10"));

    @Override
    public String id() {
        return "forge-1.7";
    }

    @Override
    public String mcFamily() {
        return "1.7.x";
    }

    @Override
    public Set<String> supportedMcVersions() {
        return VERSIONS;
    }

    @Override
    public ModPlatform platform() {
        return ModPlatform.FORGE;
    }

    @Override
    public int requiredJavaMajor() {
        return 6;
    }

    @Override
    public MetadataType expectedMetadata() {
        return MetadataType.MCMOD_INFO;
    }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.7");
        context.setJvmProperty("mineasm.tweakClass", "cpw.mods.fml.common.launcher.FMLTweaker");
        context.setJvmProperty("fml.ignoreInvalidMinecraftCertificates", "true");
        context.setJvmProperty("fml.ignorePatchDiscrepancies", "true");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
