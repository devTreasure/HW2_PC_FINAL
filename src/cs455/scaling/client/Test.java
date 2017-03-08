package cs455.scaling.client;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;

public class Test {
	public static void main(String[] args) throws Exception {
		// 1024 * 8 = 8192
		for (int i = 0; i < 5; i++) {
			byte[] payload = new byte[8192];
			new Random().nextBytes(payload);
			String str = new String(payload);
			System.out.println(str.length());
			System.out.println(SHA1FromBytes(payload));
		}
	}

	public static String SHA1FromBytes(byte[] data) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		return hashInt.toString(16);
	}
}
