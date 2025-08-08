package echonest.sociogram.connectus;

import android.util.Log;
import java.security.KeyPair;
import javax.crypto.SecretKey;

public class RSATest {
    public static void runTest() {
        try {
            String testMessage = "Hello, RSA + AES!";

            // Step 1: Generate RSA Key Pair (Public & Private)
            KeyPair keyPair = RSAUtils.generateRSAKeyPair();

            // Step 2: Generate AES Key for Encrypting Messages
            SecretKey aesKey = EncryptionUtils.generateAESKey();

            // Step 3: Encrypt the message using AES Key
            String encryptedMessage = EncryptionUtils.encryptAES(testMessage, aesKey);

            // Step 4: Encrypt the AES Key using RSA Public Key
            String encryptedAESKey = EncryptionUtils.encryptAESKeyWithRSA(aesKey, keyPair.getPublic());

            // Simulate sending encrypted data...
            Log.d("RSA_TEST", "Original Message: " + testMessage);
            Log.d("RSA_TEST", "Encrypted Message: " + encryptedMessage);
            Log.d("RSA_TEST", "Encrypted AES Key: " + encryptedAESKey);

            // Step 5: Decrypt AES Key using RSA Private Key
            SecretKey decryptedAESKey = DecryptionUtils.decryptAESKeyWithRSA(encryptedAESKey, keyPair.getPrivate());

            // Step 6: Decrypt the Message using AES Key
            String decryptedMessage = DecryptionUtils.decryptAES(encryptedMessage, decryptedAESKey);

            Log.d("RSA_TEST", "Decrypted Message: " + decryptedMessage);

        } catch (Exception e) {
            Log.e("RSA_TEST", "Encryption/Decryption Failed", e);
        }
    }
}
