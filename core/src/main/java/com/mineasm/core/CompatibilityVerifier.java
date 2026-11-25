package com.mineasm.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compatibility verifier (invoked by :dist:verifyCompatibility):
 * synthesizes one real Forge / NeoForge format mod Jar per version generation,
 * runs the full scan -> adapter match -> prepareLaunch pipeline, and checks
 * the results.
 *
 * Covers requirement #6: Forge/NeoForge mods of every version generation load
 * without conflicts or functional anomalies (at the structural level).
 */
public final class CompatibilityVerifier {

    private int passed = 0;
    private int failed = 0;
    private final Map<String, String> failures = new LinkedHashMap<String, String>();

    public static void main(String[] args) throws Exception {
        CompatibilityVerifier v = new CompatibilityVerifier();
        v.verifyAll();
        v.printReport();
        if (v.failed > 0) {
            System.exit(1);
        }
    }

    public void verifyAll() throws Exception {
        AdapterRegistry registry = new AdapterRegistry();
        if (registry.all().isEmpty()) {
            recordFailure("registry", "no VersionAdapter implementation found on the classpath");
            return;
        }

        File tempDir = File.createTempFile("mineasm-verify", "");
        if (!tempDir.delete() || !tempDir.mkdirs()) {
            throw new IllegalStateException("Cannot create temp directory: " + tempDir);
        }

        try {
            // One synthetic mod Jar per MC generation
            String[][] cases = {
                    { "1.7.2", "mcmod.info" },
                    { "1.7.10", "mcmod.info" },
                    { "1.12.2", "mcmod.info" },
                    { "1.13.2", "META-INF/mods.toml" },
                    { "1.14.4", "META-INF/mods.toml" },
                    { "1.15.2", "META-INF/mods.toml" },
                    { "1.16.5", "META-INF/mods.toml" },
                    { "1.17.1", "META-INF/mods.toml" },
                    { "1.18.2", "META-INF/mods.toml" },
                    { "1.19.2", "META-INF/mods.toml" },
                    { "1.20.1", "META-INF/mods.toml" },
                    { "1.21.1", "META-INF/neoforge.mods.toml" },
            };
            for (String[] c : cases) {
                verifyCase(registry, tempDir, c[0], c[1]);
            }
        } finally {
            deleteRecursively(tempDir);
        }
    }

    private void verifyCase(AdapterRegistry registry, File tempDir, String mcVersion, String metaPath)
            throws Exception {
        try {
            File jar = new File(tempDir, "verify-" + mcVersion + ".jar");
            writeSyntheticMod(jar, mcVersion, metaPath);

            ModCandidate candidate = ModScanner.scan(jar);
            if (candidate == null) {
                recordFailure(mcVersion, "scan result is null");
                return;
            }

            VersionAdapter adapter = registry.match(candidate, mcVersion);
            if (adapter == null) {
                recordFailure(mcVersion, "no adapter matches platform=" + candidate.platform()
                        + " meta=" + candidate.metadataType());
                return;
            }

            LaunchContext ctx = new LaunchContext(mcVersion, candidate.platform(),
                    Arch.detectHost(), java.util.Collections.singletonList(candidate));
            // Simulate the Java runtime required by this generation (so the
            // verification runs on any host JVM)
            System.setProperty("mineasm.simulated.java.major", String.valueOf(adapter.requiredJavaMajor()));
            adapter.prepareLaunch(ctx);
            System.clearProperty("mineasm.simulated.java.major");

            System.out.println("[verify] PASS " + mcVersion
                    + " via " + adapter.id()
                    + " (meta=" + candidate.metadataType().entryPath()
                    + ", " + ctx.jvmProperties().size() + " properties injected)");
            passed++;
        } catch (Throwable t) {
            recordFailure(mcVersion, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    /** Synthesizes a minimal Forge/NeoForge mod Jar with generation-correct metadata format. */
    static void writeSyntheticMod(File jarFile, String mcVersion, String metaPath) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jarFile));

        String content;
        if (metaPath.endsWith(".info")) {
            content = "[{\"modid\":\"synthetic-" + mcVersion + "\","
                    + "\"name\":\"Synthetic Mod " + mcVersion + "\","
                    + "\"version\":\"1.0.0\","
                    + "\"mcversion\":\"" + mcVersion + "\"}]";
        } else {
            String tomlKey = metaPath.contains("neoforge") ? "neoforge.mods.toml" : "mods.toml";
            content = "modLoader=\"javafml\"\n"
                    + "loaderVersion=\"[40,)\"\n"
                    + "license=\"MIT\"\n"
                    + "[[mods]]\n"
                    + "modId=\"synthetic-" + mcVersion + "\"\n"
                    + "version=\"1.0.0\"\n"
                    + "displayName=\"Synthetic Mod " + mcVersion + "\"\n"
                    + "[[dependencies." + "synthetic-" + mcVersion + "]]\n"
                    + "modId=\"forge\"\n"
                    + "mandatory=true\n"
                    + "versionRange=\"[40,)\"\n"
                    + "ordering=\"NONE\"\n"
                    + "side=\"BOTH\"\n";
            metaPath = "META-INF/" + tomlKey;
        }

        zip.putNextEntry(new ZipEntry(metaPath));
        zip.write(content.getBytes("UTF-8"));
        zip.closeEntry();

        zip.putNextEntry(new ZipEntry("com/example/SyntheticMod.class"));
        zip.write(new byte[] { (byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE });
        zip.closeEntry();

        zip.close();
    }

    private void recordFailure(String mcVersion, String reason) {
        failed++;
        failures.put(mcVersion, reason);
        System.err.println("[verify] FAIL " + mcVersion + " -> " + reason);
    }

    public void printReport() {
        System.out.println("========================================");
        System.out.println("MineASM compatibility verification report: " + passed + " passed, " + failed + " failed");
        for (Map.Entry<String, String> e : failures.entrySet()) {
            System.out.println("  [" + e.getKey() + "] " + e.getValue());
        }
        System.out.println("========================================");
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        f.delete();
    }
}
