package echonest.sociogram.connectus;

import android.util.Base64;

import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DecryptionUtils {
    public static String decryptAES(String encryptedMessage, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, aesKey);
        byte[] decryptedMessage = cipher.doFinal(Base64.decode(encryptedMessage, Base64.DEFAULT));
        return new String(decryptedMessage);
    }

    public static SecretKey decryptAESKeyWithRSA(String encryptedAESKey, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedKeyBytes = cipher.doFinal(Base64.decode(encryptedAESKey, Base64.DEFAULT));
        return new SecretKeySpec(decryptedKeyBytes, "AES");
    }


}
