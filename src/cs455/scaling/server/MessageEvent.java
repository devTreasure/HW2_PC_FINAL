package cs455.scaling.server;

import java.nio.channels.SocketChannel;

public class MessageEvent {

	private ServerThread server;
	private SocketChannel socket;
	private byte[] data;

	public MessageEvent(ServerThread serverThread, SocketChannel socket, byte[] dataCopy) {
		super();
		this.server = serverThread;
		this.socket = socket;
		this.data = dataCopy;
	}

	public ServerThread getServerThread() {
		return server;
	}

	public void setServerThread(ServerThread serverThread) {
		this.server = serverThread;
	}

	public SocketChannel getSocket() {
		return socket;
	}

	public byte[] getData() {
		return data;
	}

}
