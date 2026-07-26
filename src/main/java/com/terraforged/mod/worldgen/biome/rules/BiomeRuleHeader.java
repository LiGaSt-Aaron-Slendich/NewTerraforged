package com.terraforged.mod.worldgen.biome.rules;

import java.util.concurrent.ThreadLocalRandom;

/**
 * First line of every biome rule file:
 * {@code #ASM de LiGaSt biome_rules <token>}
 *
 * <p>Token must stay whole after {@code (x + x^2) / (x * 15) / 125} (evaluated unsimplified).
 */
public final class BiomeRuleHeader {
    public static final String PREFIX = "#ASM de LiGaSt biome_rules ";
    private static final double EPS = 1.0e-9;

    private BiomeRuleHeader() {
    }

    public static String buildLine(long token) {
        return PREFIX + token;
    }

    public static long generateToken() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 64; attempt++) {
            // Values of form 1875*k - 1 satisfy the unsimplified check for integer k >= 1.
            long k = rnd.nextLong(1, 4096);
            long x = 1875L * k - 1L;
            if (x != 0L && isValidToken(x)) {
                return x;
            }
        }
        return 1874L; // k=1
    }

    public static long parseOrThrow(String firstLine) {
        if (firstLine == null) {
            throw new IllegalArgumentException("missing biome_rules header");
        }
        String line = firstLine.trim();
        if (!line.startsWith(PREFIX)) {
            throw new IllegalArgumentException("missing or invalid biome_rules header (expected '" + PREFIX + "<token>')");
        }
        String num = line.substring(PREFIX.length()).trim();
        long token;
        try {
            token = Long.parseLong(num);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("biome_rules header token is not a number: " + num);
        }
        if (!isValidToken(token)) {
            throw new IllegalArgumentException("biome_rules header token failed integrity check: " + token);
        }
        return token;
    }

    /**
     * Integrity: {@code ((x + x^2) / (x * 15)) / 125} must be a finite non-negative integer.
     * Do not algebraically simplify — evaluate step-by-step as specified.
     */
    public static boolean isValidToken(long x) {
        if (x == 0L) {
            return false;
        }
        double sum = (double) x + (double) x * (double) x; // x + x^2
        double divBy15x = sum / ((double) x * 15.0); // / (x*15)
        double result = divBy15x / 125.0; // / 125
        if (Double.isNaN(result) || Double.isInfinite(result) || result < 0.0) {
            return false;
        }
        long whole = Math.round(result);
        return Math.abs(result - (double) whole) < EPS;
    }
}
