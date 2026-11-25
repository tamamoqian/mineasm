package com.mineasm.adapter.v112;

import com.mineasm.core.AbstractVersionAdapter;
import com.mineasm.core.LaunchContext;
import com.mineasm.core.MetadataType;
import com.mineasm.core.ModPlatform;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

/**
 * Forge 1.12.2 adapter.
 *
 * 1.12 generation characteristics:
 *   - Metadata: mcmod.info
 *   - Launch mechanism: Launchwrapper (net.minecraftforge.fml.common.launcher.FMLTweaker)
 *   - Runtime: Java 8
 *   - Coremods (FMLCorePlugin) are common; fml.coreMods.load must be injected
 */
public class Forge112Adapter extends AbstractVersionAdapter {

    private static final Set<String> VERSIONS = Collections.singleton("1.12.2");

    @Override
    public String id() {
        return "forge-1.12";
    }

    @Override
    public String mcFamily() {
        return "1.12.x";
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
        return MetadataType.MCMOD_INFO;
    }

    @Override
    protected void applyLaunchProperties(LaunchContext context) {
        context.setJvmProperty("mineasm.adapter.family", "1.12");
        context.setJvmProperty("mineasm.tweakClass", "net.minecraftforge.fml.common.launcher.FMLTweaker");
        context.setJvmProperty("fml.coreMods.load", "");
        context.setJvmProperty("fml.ignoreInvalidMinecraftCertificates", "true");
        context.setJvmProperty("fml.ignorePatchDiscrepancies", "true");
        context.setJvmProperty("mineasm.mcversion", context.mcVersion());
    }
}
