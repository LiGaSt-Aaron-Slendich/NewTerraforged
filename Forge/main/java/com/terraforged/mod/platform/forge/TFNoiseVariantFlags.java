package com.terraforged.mod.platform.forge;

import com.terraforged.mod.TerraForged;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import net.minecraftforge.fml.loading.FMLPaths;

/** Opaque internal noise-variant flag store (default off). */
public final class TFNoiseVariantFlags {
    public static TFNoiseVariantFlags INSTANCE = new TFNoiseVariantFlags();

    private static final byte[] MAGIC = new byte[]{'N', 'T', 'F', 'N', 'V', 'F', '1'};
    private static final byte[] LEGACY_MAGIC = new byte[]{'N', 'T', 'F', 'E', 'G', 'F', '1'};
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;
    private static final String SEED = "ntf/nv/v1/noise-variant/do-not-edit";
    private static final String LEGACY_SEED =
            "ntf/egf/v1/" + "ArchipelagoIsExperimental/" + "do-not-edit";

    public boolean archipelago = false;
    public boolean scatteredArchipelago = false;
    public boolean islands = false;
    /** Coastal Little Ice Age overlay (bays / rocky shores). Experimental until released. */
    public boolean coastalLia = false;
    /**
     * Ocean landscape islands: corridor seafloor + deep volcanoes.
     * When on, replaces the legacy IslandFeatureOverlay path.
     */
    public boolean oceanLandscape = false;
    /**
     * Guaranteed continent placement (N±1 landmasses in the 640k window).
     * Experimental — Customize knobs are Feature Blocked until this EGF is on.
     */
    public boolean guaranteedContinents = false;

    private TFNoiseVariantFlags() {
    }

    public static void load() {
        INSTANCE = new TFNoiseVariantFlags();
        INSTANCE.readEncrypted();
        INSTANCE.scrubLegacy();
        TerraForged.LOG.debug("[TFConfig] nv store loaded");
    }

    public static boolean archipelagoEnabled() {
        return INSTANCE != null && INSTANCE.archipelago;
    }

    public static boolean scatteredArchipelagoEnabled() {
        return INSTANCE != null && INSTANCE.scatteredArchipelago;
    }

    public static boolean islandsEnabled() {
        return INSTANCE != null && INSTANCE.islands;
    }

    public static boolean coastalLiaEnabled() {
        return INSTANCE != null && INSTANCE.coastalLia;
    }

    public static boolean oceanLandscapeEnabled() {
        return INSTANCE != null && INSTANCE.oceanLandscape;
    }

    public static boolean guaranteedContinentsEnabled() {
        return INSTANCE != null && INSTANCE.guaranteedContinents;
    }

    public static void setArchipelago(boolean value) {
        ensure();
        INSTANCE.archipelago = value;
        INSTANCE.writeEncrypted();
    }

    public static void setScatteredArchipelago(boolean value) {
        ensure();
        INSTANCE.scatteredArchipelago = value;
        INSTANCE.writeEncrypted();
    }

    public static void setIslands(boolean value) {
        ensure();
        INSTANCE.islands = value;
        INSTANCE.writeEncrypted();
    }

    public static void setCoastalLia(boolean value) {
        ensure();
        INSTANCE.coastalLia = value;
        INSTANCE.writeEncrypted();
    }

    public static void setOceanLandscape(boolean value) {
        ensure();
        INSTANCE.oceanLandscape = value;
        INSTANCE.writeEncrypted();
    }

    public static void setGuaranteedContinents(boolean value) {
        ensure();
        INSTANCE.guaranteedContinents = value;
        INSTANCE.writeEncrypted();
    }

    private static void ensure() {
        if (INSTANCE == null) {
            INSTANCE = new TFNoiseVariantFlags();
        }
    }

    private static Path storePath() {
        return FMLPaths.CONFIGDIR.get().resolve("NewTerraForged").resolve(".internal").resolve("nvf.bin");
    }

    private static Path legacyStorePath() {
        return FMLPaths.CONFIGDIR.get().resolve("NewTerraForged").resolve(".internal").resolve("egf.dat");
    }

    private void readEncrypted() {
        Path path = storePath();
        if (Files.isRegularFile(path)) {
            if (readBlob(path, MAGIC, SEED)) {
                return;
            }
        }
        Path legacy = legacyStorePath();
        if (Files.isRegularFile(legacy) && readBlob(legacy, LEGACY_MAGIC, LEGACY_SEED)) {
            this.writeEncrypted();
        }
    }

    private boolean readBlob(Path path, byte[] magic, String seed) {
        try {
            byte[] all = Files.readAllBytes(path);
            if (all.length < magic.length + IV_LEN + 16) {
                return false;
            }
            for (int i = 0; i < magic.length; i++) {
                if (all[i] != magic[i]) {
                    return false;
                }
            }
            byte[] iv = Arrays.copyOfRange(all, magic.length, magic.length + IV_LEN);
            byte[] cipher = Arrays.copyOfRange(all, magic.length + IV_LEN, all.length);
            byte[] plain = decrypt(iv, cipher, seed);
            if (plain == null || plain.length < 4) {
                return false;
            }
            int flags = ByteBuffer.wrap(plain).getInt();
            this.archipelago = (flags & 1) != 0;
            this.scatteredArchipelago = (flags & 2) != 0;
            this.islands = (flags & 4) != 0;
            this.coastalLia = (flags & 8) != 0;
            this.oceanLandscape = (flags & 16) != 0;
            this.guaranteedContinents = (flags & 32) != 0;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void writeEncrypted() {
        try {
            Path path = storePath();
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            int flags = 0;
            if (this.archipelago) {
                flags |= 1;
            }
            if (this.scatteredArchipelago) {
                flags |= 2;
            }
            if (this.islands) {
                flags |= 4;
            }
            if (this.coastalLia) {
                flags |= 8;
            }
            if (this.oceanLandscape) {
                flags |= 16;
            }
            if (this.guaranteedContinents) {
                flags |= 32;
            }
            byte[] plain = ByteBuffer.allocate(4).putInt(flags).array();
            byte[] iv = new byte[IV_LEN];
            System.arraycopy(sha256(plain, SEED.getBytes(StandardCharsets.UTF_8)), 0, iv, 0, IV_LEN);
            byte[] cipher = encrypt(iv, plain, SEED);
            if (cipher == null) {
                return;
            }
            byte[] out = new byte[MAGIC.length + IV_LEN + cipher.length];
            System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
            System.arraycopy(iv, 0, out, MAGIC.length, IV_LEN);
            System.arraycopy(cipher, 0, out, MAGIC.length + IV_LEN, cipher.length);
            Files.write(path, out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.setAttribute(path, "dos:hidden", true);
                if (path.getParent() != null) {
                    Files.setAttribute(path.getParent(), "dos:hidden", true);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            TerraForged.LOG.error("[TFConfig] Failed to persist nv store", e);
        }
    }

    private void scrubLegacy() {
        try {
            Files.deleteIfExists(legacyStorePath());
            Path legacyToml = FMLPaths.CONFIGDIR.get().resolve("NewTerraForged")
                    .resolve("Critical Options").resolve("experimental-generation.toml");
            Files.deleteIfExists(legacyToml);
        } catch (Exception ignored) {
        }
    }

    private static byte[] keyBytes(String seed) {
        return Arrays.copyOf(sha256(seed.getBytes(StandardCharsets.UTF_8)), 16);
    }

    private static byte[] sha256(byte[]... parts) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (byte[] p : parts) {
                md.update(p);
            }
            return md.digest();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] encrypt(byte[] iv, byte[] plain, String seed) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes(seed), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(plain);
        } catch (Exception e) {
            TerraForged.LOG.error("[TFConfig] nv encrypt failed", e);
            return null;
        }
    }

    private static byte[] decrypt(byte[] iv, byte[] cipher, String seed) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes(seed), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(cipher);
        } catch (Exception e) {
            return null;
        }
    }
}
