import java.security.SecureRandom;

public class Keys {
    byte[] generatedKey;
    final int KEY_LENGTH = 12;
    static final byte[] CHARS = {'A', 'B', 'C', 'D', 'E',
            'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O',
            'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9'};

    static SecureRandom rnd = new SecureRandom();

    public Keys() {
        generatedKey = new byte[KEY_LENGTH];
    }

    public byte[] genNewKey() {
        byte[] key = new byte[KEY_LENGTH];
        for(int i = 0; i < key.length; i++) {
            key[i] = CHARS[rnd.nextInt(CHARS.length)];
        }
        return key;
    }
}
