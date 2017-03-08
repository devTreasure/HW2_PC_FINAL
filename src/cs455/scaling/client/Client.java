package cs455.scaling.client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;

public class Client {

	private LinkedList<String> hashList = new LinkedList<>();
	private int messagesSent = 0;
	private int messagesReceived = 0;
	private int capacity8K = 8192;
	private ByteBuffer buffer = ByteBuffer.allocate(capacity8K);
	private Selector selector;

	public static void main(String[] args) throws Exception {
		if (args.length < 3) {
			System.out.println("Invalid Arguments:");
			System.out.println("Please provide server-host server-port message-rate as arguments");
			System.exit(0);
		}

		String serverHostName = args[0];
		int serverPortNumber = Integer.parseInt(args[1]);
		int messageRate = Integer.parseInt(args[2]);

		Client client = new Client();
		client.start(serverHostName, serverPortNumber, messageRate);
	}

	public void start(String serverHostName, int serverPortNumber, int messageRate) throws Exception {
		SocketChannel channel;

		selector = Selector.open();

		// SocketChannel channel = SocketChannel.open(new
		// InetSocketAddress(serverHostName, serverPortNumber));
		channel = SocketChannel.open();

		channel.configureBlocking(false);

		channel.register(selector, SelectionKey.OP_CONNECT);

		channel.connect(new InetSocketAddress(serverHostName, serverPortNumber));

		boolean loop = true;

		while (loop) {

			selector.select(1000);

			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			// System.out.println("set of keys " + keys.toString());
			// System.out.println("size of keys " +
			// selector.selectedKeys().size() );
			while (keys.hasNext()) {

				SelectionKey key = keys.next();
				// System.out.println("next key " + key);
				keys.remove();

				if (!key.isValid())
					continue;

				if (key.isConnectable()) {
					System.out.println("I am connected to the server");
					connect(key);
				}
				if (key.isWritable()) {
					write(key);
				}
				if (key.isReadable()) {
					read(key);
				}
			}

			// System.out.println("Client started...");

			// System.out.println(remove);

			Thread.sleep(1000 / messageRate);

		}

		channel.close();
	}

	private void read(SelectionKey key) throws IOException, InterruptedException {
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer readBuffer = ByteBuffer.allocate(capacity8K);
		readBuffer.clear();
		int length;
		try {
			length = channel.read(readBuffer);
		} catch (IOException e) {
			System.out.println("Reading problem, closing connection");
			key.cancel();
			channel.close();
			return;
		}
		if (length == -1) {
			System.out.println("Nothing was read from server");
			channel.close();
			key.cancel();
			return;
		}
		readBuffer.flip();
		byte[] buff = new byte[capacity8K];
		readBuffer.get(buff, 0, length);
		String response = new String(buff);
		boolean remove = hashList.remove(response);
		messagesReceived++;
		PrintClientTarfficSummary(0, 0);
		System.out.println("Server said: " + response);
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private void write(SelectionKey key) throws Exception {
		SocketChannel channel = (SocketChannel) key.channel();
		// channel.write(ByteBuffer.wrap(message.getBytes()));

		// create pay-load for each request
		byte[] payload = new byte[capacity8K];
		new Random().nextBytes(payload);
		// generate hash

		String hash = SHA1FromBytes(payload);
		hashList.add(hash);

		// String reponse = sendRequest(channel, payload);

		System.out.println(buffer);
		buffer.clear();
		buffer.put(payload);
		System.out.println(buffer);
		buffer.flip();
		System.out.println(buffer);
		while (buffer.hasRemaining()) {
			channel.write(buffer);
		}
		messagesSent++;
		PrintClientTarfficSummary(0, 0);
		System.out.println("Sent data.");
		buffer.flip();
		buffer.clear();

		// channel.write(ByteBuffer.wrap(messageBytes));
		// lets get ready to read.
		key.interestOps(SelectionKey.OP_READ);
	}

	private void PrintClientTarfficSummary(int messageSent, int messageReceved) throws InterruptedException {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		System.out.println(
				"[ " + timestamp + " ]" + " MessgeSent: " + messagesSent + " MessgeReceived: " + messagesReceived);

	}

	private void connect(SelectionKey key) throws IOException {
		SocketChannel channel = (SocketChannel) key.channel();
		if (channel.isConnectionPending()) {
			channel.finishConnect();
		}
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_WRITE);
	}

	/*
	 * private String sendRequest(SocketChannel channel, byte[] payload) throws
	 * Exception {
	 * 
	 * 
	 * 
	 * 
	 * channel.read(buffer); messagesReceived++;
	 * 
	 * buffer.flip(); System.out.println(buffer);
	 * System.out.println(buffer.hasRemaining());
	 * 
	 * StringBuffer responseBuffer = new StringBuffer(); while
	 * (buffer.hasRemaining()) { responseBuffer.append((char) buffer.get()); }
	 * System.out.println(responseBuffer.toString()); return
	 * responseBuffer.toString(); }
	 */

	public static String SHA1FromBytes(byte[] data) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA1");
		byte[] hash = digest.digest(data);
		BigInteger hashInt = new BigInteger(1, hash);
		return hashInt.toString(16);
	}
}
