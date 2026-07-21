package com.terraforged.mod.client.gui.screen.preset;

import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

/**
 * NewTF preset file header:
 * <pre>
 * #ASM de LiGaSt ,NewTF-default-presset 1917
 * #creator LiGaSt
 * { ... json ... }
 * </pre>
 */
public final class PresetFormat {
    public static final String MOD_AUTHOR = "LiGaSt";
    public static final String UNKNOWN_CREATOR = "creator unknown";
    private static final String ASM_PREFIX = "#ASM de LiGaSt ,";
    private static final String USER_TYPE = "NewTF-presset";
    private static final String DEFAULT_TYPE = "NewTF-default-presset";
    private static final String CREATOR_PREFIX = "#creator ";
    public static final Set<Long> OFFICIAL_DEFAULT_MARKERS = Set.of(1917L, 1991L, 2004L, 2014L, 2022L);

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
        if (defaultPreset && !OFFICIAL_DEFAULT_MARKERS.contains(marker)) {
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
        return name == null || name.isBlank() ? UNKNOWN_CREATOR : name;
    }

    public static long generateUserMarker() {
        long k = java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 1_000_000);
        return 128L * k;
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
            if (!OFFICIAL_DEFAULT_MARKERS.contains(marker)) {
                return "unknown";
            }
            if (claimsOfficialAuthor || rawCreator.isBlank()) {
                return MOD_AUTHOR;
            }
            return rawCreator;
        }
        if (claimsOfficialAuthor) {
            return "unknown";
        }
        return rawCreator.isBlank() ? UNKNOWN_CREATOR : rawCreator;
    }

    private static boolean isValidUserMarker(long marker) {
        return marker > 0L && marker % 128L == 0L;
    }
}
