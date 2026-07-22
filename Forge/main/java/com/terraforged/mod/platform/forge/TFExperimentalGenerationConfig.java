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

/**
 * Experimental Generation Features — persisted in an opaque encrypted blob, not a user-facing TOML.
 * Default off; toggle only via the secret EGF console.
 */
public final class TFExperimentalGenerationConfig {
    public static TFExperimentalGenerationConfig INSTANCE = new TFExperimentalGenerationConfig();

    private static final byte[] MAGIC = new byte[]{'N', 'T', 'F', 'E', 'G', 'F', '1'};
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LEN = 12;
    /** Obfuscated material — not a user password; just keeps the file non-plaintext. */
    private static final String SEED =
            "ntf/egf/v1/" + "ArchipelagoIsExperimental/" + "do-not-edit";

    public boolean archipelago = false;
    public boolean scatteredArchipelago = false;

    private TFExperimentalGenerationConfig() {
    }

    public static void load() {
        INSTANCE = new TFExperimentalGenerationConfig();
        INSTANCE.readEncrypted();
        INSTANCE.scrubLegacyPlaintext();
        // Do not log flag values — avoids leaking experimental state into logs.
        TerraForged.LOG.debug("[TFConfig] EGF store loaded");
    }

    public static boolean archipelagoEnabled() {
        return INSTANCE != null && INSTANCE.archipelago;
    }

    public static boolean scatteredArchipelagoEnabled() {
        return INSTANCE != null && INSTANCE.scatteredArchipelago;
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

    private static void ensure() {
        if (INSTANCE == null) {
            INSTANCE = new TFExperimentalGenerationConfig();
        }
    }

    private static Path storePath() {
        // Nested under config but not a readable settings file.
        return FMLPaths.CONFIGDIR.get().resolve("NewTerraForged").resolve(".internal").resolve("egf.dat");
    }

    private void readEncrypted() {
        Path path = storePath();
        if (!Files.isRegularFile(path)) {
            return;
        }
        try {
            byte[] all = Files.readAllBytes(path);
            if (all.length < MAGIC.length + IV_LEN + 16) {
                return;
            }
            for (int i = 0; i < MAGIC.length; i++) {
                if (all[i] != MAGIC[i]) {
                    return;
                }
            }
            byte[] iv = Arrays.copyOfRange(all, MAGIC.length, MAGIC.length + IV_LEN);
            byte[] cipher = Arrays.copyOfRange(all, MAGIC.length + IV_LEN, all.length);
            byte[] plain = decrypt(iv, cipher);
            if (plain == null || plain.length < 4) {
                return;
            }
            int flags = ByteBuffer.wrap(plain).getInt();
            this.archipelago = (flags & 1) != 0;
            this.scatteredArchipelago = (flags & 2) != 0;
        } catch (Exception e) {
            TerraForged.LOG.warn("[TFConfig] EGF store unreadable — using defaults");
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
            byte[] plain = ByteBuffer.allocate(4).putInt(flags).array();
            byte[] iv = new byte[IV_LEN];
            // Deterministic IV from content+seed so file is stable; secrecy is opacity not forward secrecy.
            System.arraycopy(sha256(plain, SEED.getBytes(StandardCharsets.UTF_8)), 0, iv, 0, IV_LEN);
            byte[] cipher = encrypt(iv, plain);
            if (cipher == null) {
                return;
            }
            byte[] out = new byte[MAGIC.length + IV_LEN + cipher.length];
            System.arraycopy(MAGIC, 0, out, 0, MAGIC.length);
            System.arraycopy(iv, 0, out, MAGIC.length, IV_LEN);
            System.arraycopy(cipher, 0, out, MAGIC.length + IV_LEN, cipher.length);
            Files.write(path, out, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                // Best-effort hide on Windows; ignored elsewhere.
                Files.setAttribute(path, "dos:hidden", true);
                if (path.getParent() != null) {
                    Files.setAttribute(path.getParent(), "dos:hidden", true);
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            TerraForged.LOG.error("[TFConfig] Failed to persist EGF store", e);
        }
    }

    /** Remove the old open TOML if a previous build created it. */
    private void scrubLegacyPlaintext() {
        try {
            Path legacy = FMLPaths.CONFIGDIR.get().resolve("NewTerraForged")
                    .resolve("Critical Options").resolve("experimental-generation.toml");
            if (Files.isRegularFile(legacy)) {
                // One-shot migrate plaintext → encrypted, then delete.
                try {
                    String text = Files.readString(legacy);
                    if (text.contains("archipelago") && text.toLowerCase().contains("true")) {
                        // crude migrate: if either true in old file, keep after encrypted load only if store empty
                        if (!Files.isRegularFile(storePath())) {
                            this.archipelago = text.contains("archipelago = true")
                                    || text.contains("archipelago=true");
                            this.scatteredArchipelago = text.contains("scattered_archipelago = true")
                                    || text.contains("scattered_archipelago=true");
                            this.writeEncrypted();
                        }
                    }
                } catch (Exception ignored) {
                }
                Files.deleteIfExists(legacy);
            }
        } catch (Exception ignored) {
        }
    }

    private static byte[] keyBytes() {
        return Arrays.copyOf(sha256(SEED.getBytes(StandardCharsets.UTF_8)), 16);
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

    private static byte[] encrypt(byte[] iv, byte[] plain) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(plain);
        } catch (Exception e) {
            TerraForged.LOG.error("[TFConfig] EGF encrypt failed", e);
            return null;
        }
    }

    private static byte[] decrypt(byte[] iv, byte[] cipher) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes(), "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return c.doFinal(cipher);
        } catch (Exception e) {
            return null;
        }
    }
}
