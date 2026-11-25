package com.mineasm.core;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Jar mod scanner: recognizes Forge / NeoForge / Fabric metadata and extracts
 * basic information.
 *
 * Supported metadata formats (covering 1.7.x ~ 1.21.x):
 *   mcmod.info                  -> Forge 1.7 ~ 1.12
 *   META-INF/mods.toml          -> Forge 1.13 ~ 1.20, NeoForge 1.20.1 ~ 1.20.2
 *   META-INF/neoforge.mods.toml -> NeoForge 1.20.4+ / 1.21+
 *   fabric.mod.json             -> Fabric (recognized only; Forge adapters reject it)
 */
public final class ModScanner {

    private static final Pattern MC_MOD_INFO_ID = Pattern.compile("\"modid\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MC_MOD_INFO_VER = Pattern.compile("\"version\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern MC_MOD_INFO_MCVER = Pattern.compile("\"mcversion\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TOML_STRING = Pattern.compile("([A-Za-z0-9_.-]+)\\s*=\\s*\"([^\"]*)\"");
    private static final Pattern TOML_SECTION = Pattern.compile("\\[\\[?\\s*([A-Za-z0-9_.-]+)\\s*\\]\\]?");

    private ModScanner() {
    }

    /**
     * Scans a single Jar file; returns a candidate with platform=UNKNOWN (or
     * null for non-Jar files) when nothing can be recognized.
     */
    public static ModCandidate scan(File jar) {
        ZipFile zip = null;
        try {
            zip = new ZipFile(jar);
            ModCandidate candidate = scanZip(zip, jar);
            return candidate;
        } catch (Exception e) {
            System.err.println("[mineasm] Cannot read " + jar.getName() + ": " + e.getMessage());
            return null;
        } finally {
            if (zip != null) {
                try {
                    zip.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static ModCandidate scanZip(ZipFile zip, File jar) {
        // Priority: neoforge.mods.toml > mods.toml > mcmod.info > fabric.mod.json
        ZipEntry neo = zip.getEntry(MetadataType.NEOFORGE_MODS_TOML.entryPath());
        if (neo != null) {
            return fromToml(zip, neo, jar, ModPlatform.NEOFORGE, MetadataType.NEOFORGE_MODS_TOML);
        }
        ZipEntry forge = zip.getEntry(MetadataType.MODS_TOML.entryPath());
        if (forge != null) {
            return fromToml(zip, forge, jar, ModPlatform.FORGE, MetadataType.MODS_TOML);
        }
        ZipEntry legacy = zip.getEntry(MetadataType.MCMOD_INFO.entryPath());
        if (legacy != null) {
            return fromMcModInfo(zip, legacy, jar);
        }
        ZipEntry fabric = zip.getEntry(MetadataType.FABRIC_JSON.entryPath());
        if (fabric != null) {
            return new ModCandidate(jar, ModPlatform.FABRIC, jar.getName(), "unknown", null, MetadataType.FABRIC_JSON);
        }
        // Bare Jar (no metadata at all): recorded as platform UNKNOWN; adapters decide
        // whether to reject it.
        return new ModCandidate(jar, ModPlatform.UNKNOWN, stripExt(jar.getName()), "unknown", null, MetadataType.NONE);
    }

    private static ModCandidate fromToml(ZipFile zip, ZipEntry entry, File jar,
                                         ModPlatform platform, MetadataType type) {
        String text = readEntry(zip, entry);
        String modId = null;
        String modVer = null;
        String mcVer = null;
        boolean inModsSection = false;
        for (String rawLine : text.split("\r?\n")) {
            String line = rawLine.trim();
            Matcher section = TOML_SECTION.matcher(line);
            if (section.matches()) {
                String sec = section.group(1);
                inModsSection = sec.startsWith("mods");
                continue;
            }
            Matcher kv = TOML_STRING.matcher(line);
            if (kv.matches()) {
                String key = kv.group(1);
                String val = kv.group(2);
                if ("modId".equals(key) && (modId == null || inModsSection)) {
                    modId = val;
                } else if ("version".equals(key) && (modVer == null || inModsSection)) {
                    modVer = val;
                } else if ("mcversion".equals(key) && mcVer == null) {
                    mcVer = val;
                } else if ("loaderVersion".equals(key) && mcVer == null) {
                    // The forge loader version only roughly indicates the MC generation;
                    // used for display only.
                }
            }
        }
        return new ModCandidate(jar, platform,
                modId != null ? modId : stripExt(jar.getName()),
                modVer != null ? modVer : "unknown", mcVer, type);
    }

    private static ModCandidate fromMcModInfo(ZipFile zip, ZipEntry entry, File jar) {
        String text = readEntry(zip, entry);
        return new ModCandidate(jar, ModPlatform.FORGE,
                firstMatch(MC_MOD_INFO_ID, text, stripExt(jar.getName())),
                firstMatch(MC_MOD_INFO_VER, text, "unknown"),
                firstMatch(MC_MOD_INFO_MCVER, text, null),
                MetadataType.MCMOD_INFO);
    }

    private static String firstMatch(Pattern p, String text, String fallback) {
        Matcher m = p.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return fallback;
    }

    private static String readEntry(ZipFile zip, ZipEntry entry) {
        try {
            java.io.InputStream in = zip.getInputStream(entry);
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            return new String(out.toByteArray(), "UTF-8");
        } catch (Exception e) {
            return "";
        }
    }

    private static String stripExt(String name) {
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
