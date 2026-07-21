package com.terraforged.mod.client.gui.screen.preset;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * NewTF preset file header:
 * <pre>
 * #ASM de LiGaSt ,NewTF-default-presset 1226880
 * #creator LiGaSt
 * { ... json ... }
 * </pre>
 * Official default markers are {@code year * 128 * 5} for years
 * {@code 1917, 1991, 2004, 2014, 2022}. Validation requires
 * {@code marker % 128 == 0} and {@code (marker / 128 / 5)} equals one of those years.
 */
public final class PresetFormat {
    public static final String MOD_AUTHOR = "LiGaSt";
    public static final String ACCOUNT_ALIAS = "Apron3333";
    public static final String UNKNOWN_CREATOR = "creator unknown";
    private static final String ASM_PREFIX = "#ASM de LiGaSt ,";
    private static final String USER_TYPE = "NewTF-presset";
    private static final String DEFAULT_TYPE = "NewTF-default-presset";
    private static final String CREATOR_PREFIX = "#creator ";
    /** Human-readable official IDs (years). Stored marker = id * 128 * 5. */
    public static final Set<Long> OFFICIAL_DEFAULT_IDS = Set.of(1917L, 1991L, 2004L, 2014L, 2022L);
    private static final long MARKER_FACTOR = 128L * 5L;

    private PresetFormat() {
    }

    public record Header(boolean defaultPreset, long marker, String rawCreator, String displayAuthor) {
    }

    public static Header parseHeaderLine1(String line) {
        if (line == null || !line.startsWith(ASM_PREFIX)) {
            throw new IllegalArgumentException("This is not a NewTF world preset");
        }
        String rest = line.substring(ASM_PREFIX.length()).trim();
        int space = rest.lastIndexOf(' ');
        if (space <= 0) {
            throw new IllegalArgumentException("This is not a NewTF world preset");
        }
        String type = rest.substring(0, space).trim();
        long marker = Long.parseLong(rest.substring(space + 1).trim());
        boolean defaultPreset = DEFAULT_TYPE.equals(type);
        if (!defaultPreset && !USER_TYPE.equals(type)) {
            throw new IllegalArgumentException("This is not a NewTF world preset");
        }
        if (defaultPreset && !isOfficialDefaultMarker(marker)) {
            throw new IllegalArgumentException("This is not a NewTF world preset");
        }
        if (!defaultPreset && !isValidUserMarker(marker)) {
            throw new IllegalArgumentException("This is not a NewTF world preset");
        }
        return new Header(defaultPreset, marker, "", UNKNOWN_CREATOR);
    }

    public static Header finalizeHeader(Header partial, String creatorLine) {
        String raw = parseCreator(creatorLine);
        return new Header(partial.defaultPreset, partial.marker, raw, resolveDisplayAuthor(partial.defaultPreset, partial.marker, raw));
    }

    public static String buildHeaderLines(boolean defaultPreset, long marker) {
        String type = defaultPreset ? DEFAULT_TYPE : USER_TYPE;
        return ASM_PREFIX + type + " " + marker + System.lineSeparator()
                + CREATOR_PREFIX + resolveSaveCreator() + System.lineSeparator();
    }

    /** Marker stored in default preset files: {@code year * 128 * 5}. */
    public static long officialMarker(long yearId) {
        return yearId * MARKER_FACTOR;
    }

    public static String resolveSaveCreator() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return UNKNOWN_CREATOR;
        }
        User user = mc.getUser();
        if (user == null) {
            return UNKNOWN_CREATOR;
        }
        String name = user.getName();
        if (name == null || name.isBlank()) {
            return UNKNOWN_CREATOR;
        }
        // Account alias maps to mod author identity in saved presets.
        if (ACCOUNT_ALIAS.equalsIgnoreCase(name)) {
            return MOD_AUTHOR;
        }
        return name;
    }

    public static long generateUserMarker() {
        // Avoid colliding with official markers (year * 128 * 5).
        long marker;
        do {
            long k = java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 1_000_000);
            marker = 128L * k;
        } while (isOfficialDefaultMarker(marker));
        return marker;
    }

    private static String parseCreator(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (!trimmed.startsWith(CREATOR_PREFIX)) {
            return "";
        }
        return trimmed.substring(CREATOR_PREFIX.length()).trim();
    }

    private static String resolveDisplayAuthor(boolean defaultPreset, long marker, String rawCreator) {
        boolean claimsOfficialAuthor = MOD_AUTHOR.equalsIgnoreCase(rawCreator);
        if (defaultPreset) {
            if (!isOfficialDefaultMarker(marker)) {
                return "unknown";
            }
            if (claimsOfficialAuthor || rawCreator.isBlank()) {
                return MOD_AUTHOR;
            }
            return rawCreator;
        }
        // LiGaSt without an official default marker → spoof / unknown.
        if (claimsOfficialAuthor) {
            return "unknown";
        }
        return rawCreator.isBlank() ? UNKNOWN_CREATOR : rawCreator;
    }

    /**
     * Official defaults: marker divisible by 128 and by 5, and
     * {@code marker / 128 / 5} is one of {@link #OFFICIAL_DEFAULT_IDS}.
     */
    public static boolean isOfficialDefaultMarker(long marker) {
        if (marker <= 0L || marker % 128L != 0L) {
            return false;
        }
        if ((marker / 128L) % 5L != 0L) {
            return false;
        }
        long yearId = marker / 128L / 5L;
        return OFFICIAL_DEFAULT_IDS.contains(yearId);
    }

    private static boolean isValidUserMarker(long marker) {
        return marker > 0L && marker % 128L == 0L;
    }
}
