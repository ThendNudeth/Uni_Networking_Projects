import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;

public class DHEncryption {

    private PublicKey publicKey;
    KeyAgreement keyAgreement;
    byte[] sharedsecret;
    String ALGO = "AES";

    public DHEncryption() {
        makeKeyExchangeParams();
    }

    private void makeKeyExchangeParams() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("EC");
            kpg.initialize(128);
            KeyPair kp = kpg.generateKeyPair();
            publicKey = kp.getPublic();
            keyAgreement = KeyAgreement.getInstance("ECDH");
            keyAgreement.init(kp.getPrivate());

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public byte[] encryptMessage(byte[] toSend) {
        try {
            Key key = genKey();
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.ENCRYPT_MODE, key);
            byte[] encVal = c.doFinal(toSend);
            return encVal;
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return toSend;
    }

    public byte[] decryptMessage(byte[] encryptedMessage) {
        try {
            Key key = genKey();
            Cipher c = Cipher.getInstance(ALGO);
            c.init(Cipher.DECRYPT_MODE, key);
            byte[] decValue = c.doFinal(encryptedMessage);
            return decValue;
        } catch (BadPaddingException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return encryptedMessage;
    }

    protected Key genKey() {
        return new SecretKeySpec(sharedsecret, ALGO);
    }

    public void setReceiverPublicKey(PublicKey publickey) {
        try {
            keyAgreement.doPhase(publickey, true);
            sharedsecret = keyAgreement.generateSecret();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
