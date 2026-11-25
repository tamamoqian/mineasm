package com.mineasm.adapter.v117;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Forge 1.17.1 adapter.
 *
 * 1.17 is the first generation requiring Java 16+; loading facilities beyond
 * ModLauncher such as ImmediateWindowHandler / ForgeAuthLib appear here.
 */
public class Forge117Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = new LinkedHashSet<String>(
            Arrays.asList("1.17", "1.17.1"));

    @Override
    public String id() {
        return "forge-1.17";
    }

    @Override
    public String mcFamily() {
        return "1.17.x";
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
        return 16;
    }

    @Override
    public MetadataType expectedMetadata() {
        return MetadataType.MODS_TOML;
    }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.17");
        context.setJvmProperty("mineasm.launch.pipeline", "modlauncher");
        context.setJvmProperty("mineasm.fml.entrypoint", "net.minecraftforge.fml.loading.FMLLoader");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
