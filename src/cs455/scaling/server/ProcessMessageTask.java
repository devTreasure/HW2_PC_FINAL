package cs455.scaling.server;

import java.math.BigInteger;
import java.security.MessageDigest;

public class ProcessMessageTask implements Runnable {

	private MessageEvent event;

	public ProcessMessageTask(MessageEvent event) {
		this.event = event;
	}

	@Override
	public void run() {
		String response = "ERROR";
		try {
			response = SHA1FromBytes(event.getData());
		} catch (Exception e) {
			e.printStackTrace();
		}
		event.getServerThread().send(event.getSocket(), response);
	}
	
	public static String SHA1FromBytes(byte[] data) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		return hashInt.toString(16);
	}

}
