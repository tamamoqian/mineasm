package com.mineasm.adapter.v116;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.16.x adapter (1.16.1 ~ 1.16.5, ModLauncher pipeline, Java 8).
 */
public class Forge116Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = new LinkedHashSet<String>(
            Arrays.asList("1.16", "1.16.1", "1.16.2", "1.16.3", "1.16.4", "1.16.5"));

    @Override
    public String id() {
        return "forge-1.16";
    }

    @Override
    public String mcFamily() {
        return "1.16.x";
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
        context.setJvmProperty("mineasm.adapter.family", "1.16");
        context.setJvmProperty("mineasm.launch.pipeline", "modlauncher");
        context.setJvmProperty("mineasm.fml.entrypoint", "net.minecraftforge.fml.loading.FMLLoader");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
