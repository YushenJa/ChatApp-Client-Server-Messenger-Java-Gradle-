package de.hsrm.mi.utils;

import java.util.Base64;        // Kodierung und Dekodierung von Daten im Textformat

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtil {
    private static final String KEY = "1234567890123456";

    // Nachricht mit dem AES-Algorithmus verschlüsseln
    public static String encrypt(String message) throws Exception {
        SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes()));
    }

    // verschlüsselte Nachricht entschlüsseln
    public static String decrypt(String encrypted) throws Exception {
        SecretKeySpec key = new SecretKeySpec(KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encrypted)));
    }
}