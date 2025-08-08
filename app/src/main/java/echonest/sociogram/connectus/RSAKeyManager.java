package echonest.sociogram.connectus;
import android.util.Log;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public class RSAKeyManager {
    private String userId;
    private FirebaseFirestore db;

    public RSAKeyManager(String userId) {
        this.userId = userId;
        this.db = FirebaseFirestore.getInstance();

        try {
            KeyStoreUtils.generateAndStoreRSAKeyPairIfNeeded();
            uploadPublicKeyToFirestore(); // public key from keystore
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadPublicKeyToFirestore() {
        try {
            PublicKey publicKey = KeyStoreUtils.getStoredPublicKey();
            String encodedPublicKey = Base64.getEncoder().encodeToString(publicKey.getEncoded());

            db.collection("users").document(userId)
                    .update("publicKey", encodedPublicKey)
                    .addOnSuccessListener(aVoid -> Log.d("RSAKey", "Public key uploaded"))
                    .addOnFailureListener(e -> Log.e("RSAKey", "Failed to upload public key", e));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            return KeyStoreUtils.getStoredPrivateKey();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
