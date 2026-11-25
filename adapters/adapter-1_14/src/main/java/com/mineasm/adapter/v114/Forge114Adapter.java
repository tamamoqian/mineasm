package com.mineasm.adapter.v114;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.14.4 adapter (ModLauncher pipeline, Java 8).
 */
public class Forge114Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = new LinkedHashSet<String>(
            Arrays.asList("1.14", "1.14.1", "1.14.2", "1.14.3", "1.14.4"));

    @Override
    public String id() {
        return "forge-1.14";
    }

    @Override
    public String mcFamily() {
        return "1.14.x";
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
        context.setJvmProperty("mineasm.adapter.family", "1.14");
        context.setJvmProperty("mineasm.launch.pipeline", "modlauncher");
        context.setJvmProperty("mineasm.fml.entrypoint", "net.minecraftforge.fml.loading.FMLLoader");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
