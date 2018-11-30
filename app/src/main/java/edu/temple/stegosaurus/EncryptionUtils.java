package edu.temple.stegosaurus;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import java.util.Random;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author jesse
 */
public class EncryptionUtils {
    private static final String ALGORITHM_SCHEME = "AES/CBC/NoPadding";
    private static final int    IV_LENGTH =        16;

    /**
     * Converts bytes into a hex string.
     * @param hash Array of bytes to convert.
     * @return String containing hex values for the bytes.
     */
    private static String bytesToHex(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Converts a hex string into bytes.
     * @param hex String of hex data
     * @return Array of bytes
     */
    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i+1), 16));
        }
        return data;
    }

    /**
     * Generates a random 256-bit IV for AES encryption.
     * @return 16 random bytes in an array
     */
    private static byte[] generateIV() {
        byte[] iv = new byte[IV_LENGTH];
        new Random().nextBytes(iv);

        return iv;
    }

    /**
     * Generates client and server keys from a user password.
     * @param password User-given password string.
     * @return Array of two 256-bit hex strings, first is client key, second is server key.
     */
    public static String[] generateKeys(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] byteHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            String prelimHash = bytesToHex(byteHash);

            String serverKeyInput = password + prelimHash.substring(0, 32);
            String clientKeyInput = password + prelimHash.substring(32);

            byte[] serverKeyBytes = digest.digest(serverKeyInput.getBytes(StandardCharsets.UTF_8));
            byte[] clientKeyBytes = digest.digest(clientKeyInput.getBytes(StandardCharsets.UTF_8));

            String serverKey = bytesToHex(serverKeyBytes);
            String clientKey = bytesToHex(clientKeyBytes);

            return new String[] {clientKey, serverKey};

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Encrypts a file Using AES/CBC. Generates a random IV and prepends it to the encrypted file.
     * @param key 256-bit hex string key (64 hexidecimal digits long)
     * @param inputFile Plaintext file to encrypt
     * @param outputFile Placeholder where encrypted file will be written
     * @throws Exception
     */
    public static void encrypt(String key, File inputFile, File outputFile)
            throws Exception {
        if (key.length() != 64 || !key.matches("\\p{XDigit}+")) {
            throw new Exception("Expecting a 256-bit hex string as the key (length 64).");
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(hexToBytes(key), "AES");
            byte[] ivBytes = generateIV();
            IvParameterSpec iv = new IvParameterSpec(ivBytes);
            Cipher cipher = Cipher.getInstance(ALGORITHM_SCHEME);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);

            FileInputStream inputStream = new FileInputStream(inputFile);

            // Zero-byte padding
            int padLength = 16 - (int) (inputFile.length() % 16);

            System.out.println("input file length: " + inputFile.length());
            System.out.println("pad length: " + padLength);

            byte[] inputBytes = new byte[(int) inputFile.length() + padLength];

            System.out.println("input bytes length: " + inputBytes.length);

            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(ivBytes);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();

        } catch (Exception e) {
            throw new Exception("Error encrypting file", e);
        }
    }

    /**
     * Decrypts a file using AES/CBC. Takes first 32 bytes of input file and uses it as IV.
     * @param key 256-bit hex string key (64 hexidecimal digits long)
     * @param inputFile Encrypted file (with IV as the first 32 bytes)
     * @param outputFile Placeholder for decrypted file to be written.
     * @throws Exception
     */
    public static void decrypt(String key, File inputFile, File outputFile)
            throws Exception {
        if (key.length() != 64 || !key.matches("\\p{XDigit}+")) {
            throw new Exception("Expecting a 256-bit hex string as the key (length 64).");
        }
        try {
            SecretKeySpec secretKey = new SecretKeySpec(hexToBytes(key), "AES");
            FileInputStream inputStream = new FileInputStream(inputFile);

            // Get IV bytes from beginning of encrypted file
            byte[] ivBytes = new byte[IV_LENGTH];
            inputStream.read(ivBytes);
            IvParameterSpec iv = new IvParameterSpec(ivBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM_SCHEME);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

            byte[] inputBytes = new byte[(int) inputFile.length() - IV_LENGTH];
            inputStream.read(inputBytes);

            byte[] outputBytes = cipher.doFinal(inputBytes);

            FileOutputStream outputStream = new FileOutputStream(outputFile);
            outputStream.write(outputBytes);

            inputStream.close();
            outputStream.close();
        } catch (Exception e) {
            throw new Exception("Error decrypting file", e);
        }
    }
}