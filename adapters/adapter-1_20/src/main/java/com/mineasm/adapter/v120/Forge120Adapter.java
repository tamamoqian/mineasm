package com.mineasm.adapter.v120;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Collections;
import java.util.Set;

/**
 * Forge 1.20.1 adapter (Java 17).
 */
public class Forge120Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = Collections.singleton("1.20.1");

    @Override
    public String id() {
        return "forge-1.20";
    }

    @Override
    public String mcFamily() {
        return "1.20.x";
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
        return 17;
    }

    @Override
    public MetadataType expectedMetadata() {
        return MetadataType.MODS_TOML;
    }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.20");
        context.setJvmProperty("mineasm.platform", "forge");
        context.setJvmProperty("mineasm.launch.pipeline", "modlauncher");
        context.setJvmProperty("mineasm.fml.entrypoint", "net.minecraftforge.fml.loading.FMLLoader");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
