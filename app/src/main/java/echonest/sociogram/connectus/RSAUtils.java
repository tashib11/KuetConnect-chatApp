package echonest.sociogram.connectus;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import android.util.Base64;

public class RSAUtils {
    public static KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        return keyPairGenerator.generateKeyPair();
    }

    public static String keyToString(Key key) {
        return Base64.encodeToString(key.getEncoded(), Base64.DEFAULT);
    }

    public static PublicKey stringToPublicKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.decode(keyStr, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    }

    public static PrivateKey stringToPrivateKey(String keyStr) throws Exception {
        byte[] keyBytes = Base64.decode(keyStr, Base64.DEFAULT);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}

