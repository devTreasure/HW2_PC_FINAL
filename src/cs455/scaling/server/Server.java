package cs455.scaling.server;

public class Server {

	private MyThreadPool threadPool;
	
	public Server() {
		this.threadPool = new MyThreadPool(10);
	}

	private void start() throws Exception {
		ServerThread serverThread = new ServerThread(threadPool);
		Thread server = new Thread(serverThread);
		server.start();
	}
	

	public static void main(String[] args) throws Exception {
		Server server = new Server();
		server.start();
	}
}
