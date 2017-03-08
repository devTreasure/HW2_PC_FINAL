package cs455.scaling.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ServerThread implements Runnable {

	private ServerSocketChannel serverChannel;
	private Selector selector;
	private int capacity8K = 8192;
	private ByteBuffer readBuffer = ByteBuffer.allocate(capacity8K);
	private MyThreadPool threadPool;
	private int totalConnections = 0;
	private int messagesSent = 0;
	private int messagesReceived = 0;

	public ServerThread(MyThreadPool threadPool) throws Exception {
		this.threadPool = threadPool;
		selector = SelectorProvider.provider().openSelector();

		// Create a new non-blocking server socket channel
		this.serverChannel = ServerSocketChannel.open();
		serverChannel.configureBlocking(false);

		// Bind the server socket to the specified address and port
		InetSocketAddress isa = new InetSocketAddress(9898);
		serverChannel.socket().bind(isa);
		System.out.println("Server Started on port:" + isa.getPort());

		// Register the server socket channel, indicating an interest in
		// accepting new connections
		serverChannel.register(selector, SelectionKey.OP_ACCEPT);
	}

	@Override
	public void run() {
		while (true) {
			try {

				// Process any pending changes
				synchronized (this.changeRequests) {
					Iterator changes = this.changeRequests.iterator();
					while (changes.hasNext()) {
						ChangeRequest change = (ChangeRequest) changes.next();
						switch (change.type) {
						case ChangeRequest.CHANGEOPS:
							SelectionKey key = change.socket.keyFor(this.selector);
							key.interestOps(change.ops);
						}
					}
					this.changeRequests.clear();
				}

				// Wait for an event one of the registered channels
				this.selector.select();

				// Iterate over the set of keys for which events are available
				Iterator selectedKeys = this.selector.selectedKeys().iterator();
				while (selectedKeys.hasNext()) {
					SelectionKey key = (SelectionKey) selectedKeys.next();
					selectedKeys.remove();

					if (!key.isValid()) {
						continue;
					}

					// Check what event is available and deal with it
					if (key.isAcceptable()) {
						this.accept(key);
					} else if (key.isReadable()) {
						this.read(key);
					} else if (key.isWritable()) {
						this.write(key);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void accept(SelectionKey key) throws IOException, InterruptedException {
		// For an accept to be pending the channel must be a server socket
		// channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);
		totalConnections++;
		PrintServerTarfficSummary(0, 0);
		// Register the new SocketChannel with our Selector, indicating
		// we'd like to be notified when there's data waiting to be read
		socketChannel.register(this.selector, SelectionKey.OP_READ);
	}

	private void read(SelectionKey key) throws IOException, InterruptedException {
		// ystem.out.println("Read Request...");
		SocketChannel socketChannel = (SocketChannel) key.channel();

		// Clear out our read buffer so it's ready for new data
		this.readBuffer.clear();

		// Attempt to read off the channel
		int numRead;
		try {
			numRead = socketChannel.read(this.readBuffer);
		} catch (IOException e) {
			// The remote forcibly closed the connection, cancel
			// the selection key and close the channel.
			key.cancel();
			socketChannel.close();
			return;
		}

		if (numRead == -1) {
			// Remote entity shut the socket down cleanly. Do the
			// same from our end and cancel the channel.
			key.channel().close();
			key.cancel();
			return;
		}
		messagesReceived++;
		PrintServerTarfficSummary(0, 0);
		// Hand the data off to our worker thread
		byte[] dataCopy = new byte[numRead];
		System.arraycopy(readBuffer.array(), 0, dataCopy, 0, numRead);
		String input = new String(dataCopy);
		// System.out.println(input);
		MessageEvent event = new MessageEvent(this, socketChannel, dataCopy);
		ProcessMessageTask worker = new ProcessMessageTask(event);
		threadPool.execute(worker);

		// new ProcessMessageTask(event)
		// this.worker.processData(this, socketChannel, this.readBuffer.array(),
		// numRead);
	}

	private void PrintServerTarfficSummary(int messageSent, int messageReceved) throws InterruptedException {
		Timestamp timestamp = new Timestamp(System.currentTimeMillis());
		System.out.println("[ " + timestamp + " ]" + "  Current Server Throughput: " + (messagesSent + messageReceved)
				+ "/s " + " Total connections: " + totalConnections);

	}

	// A list of ChangeRequest instances
	private List changeRequests = new LinkedList();

	// Maps a SocketChannel to a list of ByteBuffer instances
	private Map pendingData = new HashMap();

	public void send(SocketChannel socket, String response) {
		synchronized (this.changeRequests) {
			// Indicate we want the interest ops set changed
			this.changeRequests.add(new ChangeRequest(socket, ChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

			// And queue the data we want written
			synchronized (this.pendingData) {
				List queue = (List) this.pendingData.get(socket);
				if (queue == null) {
					queue = new ArrayList();
					this.pendingData.put(socket, queue);
				}
				queue.add(response);
			}
		}

		// Finally, wake up our selecting thread so it can make the required
		// changes
		this.selector.wakeup();
	}

	private void write(SelectionKey key) throws IOException, InterruptedException {
		SocketChannel socketChannel = (SocketChannel) key.channel();

		synchronized (this.pendingData) {
			List queue = (List) this.pendingData.get(socketChannel);

			// Write until there's not more data ...
			while (!queue.isEmpty()) {
				ByteBuffer buf = ByteBuffer.wrap(((String) queue.get(0)).getBytes());
				socketChannel.write(buf);
				messagesSent++;
				PrintServerTarfficSummary(0, 0);
				queue.remove(0);
			}

			if (queue.isEmpty()) {
				// We wrote away all data, so we're no longer interested
				// in writing on this socket. Switch back to waiting for
				// data.
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}
}
