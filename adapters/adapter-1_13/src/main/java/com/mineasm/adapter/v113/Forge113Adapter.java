package com.mineasm.adapter.v113;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.13.2 adapter.
 *
 * The 1.13 generation is a watershed for the Forge ecosystem:
 *   - First generation to introduce META-INF/mods.toml (TOML) metadata;
 *   - Launchwrapper replaced by ModLauncher
 *     (net.minecraftforge.fml.loading.FMLLoader / ModLauncher pipeline);
 *   - Runtime: Java 8.
 */
public class Forge113Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = new LinkedHashSet<String>(
            Arrays.asList("1.13", "1.13.1", "1.13.2"));

    @Override
    public String id() {
        return "forge-1.13";
    }

    @Override
    public String mcFamily() {
        return "1.13.x";
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
        return 8;
    }

    @Override
    public MetadataType expectedMetadata() {
        return MetadataType.MODS_TOML;
    }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.13");
        context.setJvmProperty("mineasm.launch.pipeline", "modlauncher");
        context.setJvmProperty("mineasm.fml.entrypoint", "net.minecraftforge.fml.loading.FMLLoader");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
