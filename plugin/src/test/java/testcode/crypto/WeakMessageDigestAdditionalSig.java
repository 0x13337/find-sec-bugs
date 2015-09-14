package testcode.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;

public class WeakMessageDigestAdditionalSig {

    public static void weakDigestMoreSig() throws NoSuchProviderException, NoSuchAlgorithmException {
        MessageDigest.getInstance("SHA1", "SUN");
        MessageDigest.getInstance("MD5", "SUN");
        MessageDigest.getInstance("MD4", "SUN");
        MessageDigest.getInstance("MD2", "SUN");
        MessageDigest.getInstance("MD5");
        MessageDigest.getInstance("MD4");
        MessageDigest.getInstance("MD2");
        MessageDigest.getInstance("SHA1", new DummyProvider());
        MessageDigest.getInstance("MD5", new DummyProvider());
        MessageDigest.getInstance("MD4", new DummyProvider());
        MessageDigest.getInstance("MD2", new DummyProvider());
        MessageDigest.getInstance("sha-384","SUN"); //OK!
        MessageDigest.getInstance("SHA-512", "SUN"); //OK!
    }

    static class DummyProvider extends Provider {

        protected DummyProvider() {
            super("dummy", 1.0, "");
        }
    }
}
