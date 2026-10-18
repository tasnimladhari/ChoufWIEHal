package com.choufwie.hal.util;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;

/**
 * - Passwords: PBKDF2-HMAC-SHA256 (120k iterations) with a per-user random salt.
 * - Check-in text: AES-256-GCM, key generated inside the Android Keystore (never leaves the device's secure hardware).
 */
public final class Security {
    private Security() {}

    private static final String ALIAS = "hal_checkin_key";
    private static final int ITER = 120_000;

    // ---------- passwords ----------
    public static String newSalt() {
        byte[] s = new byte[16];
        new SecureRandom().nextBytes(s);
        return Base64.encodeToString(s, Base64.NO_WRAP);
    }

    public static String hash(String pw, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw.toCharArray(), Base64.decode(salt, Base64.NO_WRAP), ITER, 256);
            byte[] h = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.encodeToString(h, Base64.NO_WRAP);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    public static boolean verify(String pw, String salt, String expected) {
        if (salt == null || expected == null) return false;
        return MessageDigest.isEqual(hash(pw, salt).getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }

    // ---------- encryption at rest ----------
    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (ks.containsAlias(ALIAS)) return (SecretKey) ks.getKey(ALIAS, null);
        KeyGenerator kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        kg.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return kg.generateKey();
    }

    /** Returns Base64(iv || ciphertext). */
    public static String encrypt(String plain) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, key());
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) { throw new IllegalStateException("Encryption failed", e); }
    }

    /** Returns "" if the data can't be decrypted (e.g. key lost after reinstall). */
    public static String decrypt(String enc) {
        try {
            if (enc == null || enc.isEmpty()) return "";
            byte[] all = Base64.decode(enc, Base64.NO_WRAP);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, all, 0, 12));
            return new String(c.doFinal(all, 12, all.length - 12), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
}
