package echonest.sociogram.connectus;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

import java.security.PublicKey;

public class EncryptionUtils {
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey();
    }

    public static String encryptAES(String message, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encryptedMessage = cipher.doFinal(message.getBytes());
        return Base64.encodeToString(encryptedMessage, Base64.DEFAULT);
    }

    public static String encryptAESKeyWithRSA(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.encodeToString(encryptedKey, Base64.DEFAULT);
    }


}
