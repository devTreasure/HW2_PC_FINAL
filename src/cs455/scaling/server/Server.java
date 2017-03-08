package cs455.scaling.server;

public class Server {

	private MyThreadPool threadPool;
	private int IP_port=0;
	
	public Server(int port_number,int poolsize) {
		this.threadPool = new MyThreadPool(poolsize);
		this.IP_port=port_number;
	}

	private void start() throws Exception {
		ServerThread serverThread = new ServerThread(IP_port,threadPool);
		Thread server = new Thread(serverThread);
		server.start();
	}
	

	public static void main(String[] args) throws Exception {
		
		if (args.length < 2) {
			System.out.println("Invalid Arguments:");
			System.out.println("Please provide  server-port thread-pool-size");
			System.exit(0);
		}

		
		int serverPortNumber = Integer.parseInt(args[0]);
		
		int threadsize = Integer.parseInt(args[1]);
		Server server = new Server(serverPortNumber,threadsize);
		server.start();
	}
}
