package echonest.sociogram.connectus;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;

public class KeyStoreUtils {

    private static final String ALIAS = "my_rsa_key";

    public static void generateAndStoreRSAKeyPairIfNeeded() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        if (!keyStore.containsAlias(ALIAS)) {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            keyPairGenerator.initialize(
                    new KeyGenParameterSpec.Builder(ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                            .build()
            );
            keyPairGenerator.generateKeyPair();
        }
    }

    public static PublicKey getStoredPublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            return ((KeyStore.PrivateKeyEntry) entry).getCertificate().getPublicKey();
        }
        throw new Exception("No public key found");
    }

    public static PrivateKey getStoredPrivateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);

        KeyStore.Entry entry = keyStore.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.PrivateKeyEntry) {
            return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }
        throw new Exception("No private key found");
    }
}
