import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;

public class FileSrv {

	public final static int PORT = 8000; // The port on which the server
											// listens.

	private static File directory; // The directory from which files are served.

	private static final int THREAD_POOL_SIZE = 5;

	private static final int QUEUE_CAPACITY = 10;

	private static ArrayBlockingQueue<Socket> connectionQueue;

	public static void main(String[] args) {
		String dirname;
		if (args.length > 0) {
			dirname = args[0];
		} else {
			dirname = System.getProperty("directory path");
		}
		directory = new File(dirname);
		if (!directory.isDirectory()) {
			System.out.println("Error: \"" + dirname + "\" is not a directory.");
			return;
		}
		ServerSocket listener; // Listens for incoming connections.
		Socket connection; // For communication with the connecting program.
		try {

			listener = new ServerSocket(PORT);

			connectionQueue = new ArrayBlockingQueue<Socket>(QUEUE_CAPACITY);
			for (int i = 0; i < THREAD_POOL_SIZE; i++) {
				new ConnectionHandler(); // Create the thread; it starts itself.
			}

			System.out.println("Listening on port " + PORT);
			while (true) {
				// Accept next connection request and put it in the queue.
				connection = listener.accept();
				try {
					connectionQueue.put(connection); // Blocks if queue is full.
				} catch (InterruptedException e) {
				}
			}
		} catch (Exception e) {
			System.out.println("Sorry, the server has shut down.");
			System.out.println("Error:  " + e);
			return;
		}

	}

	/**
	 * Defines one of the threads in the thread pool. Each thread runs in an
	 * infinite loop in which it takes a connection from the connection queue
	 * and handles communication with that client. The thread starts itself in
	 * its constructor. The constructor also sets the thread to be a daemon
	 * thread. (A program will end if all remaining threads are daemon threads.)
	 */
	private static class ConnectionHandler extends Thread {
		ConnectionHandler() {
			setDaemon(true);
			start();
		}

		public void run() {
			while (true) {
				Socket client;
				try {
					client = connectionQueue.take();
				} catch (InterruptedException e) {
					continue; // (If interrupted, just go back to start of while
								// loop.)
				}
				String clientAddress = client.getInetAddress().toString();
				try {
					System.out.println("Connection from " + clientAddress);
					System.out.println("Handled by thread " + this);
					Date now = new Date(); // The current date and time.
					System.out.println("Connected at " + now);
					client.setSoTimeout(60000); // timeout for a client, 1
												// minute
					handleConnection(client, clientAddress);
				} catch (Exception e) {
					System.out.println("Error on connection with: " + clientAddress + ": " + e);
				}
			}
		}
	}

	/**
	 * Handles requests from the user.
	 * 
	 * @param client
	 *            client that server is handling
	 * @param clientAddress
	 *            clients address
	 */
	private final static void handleConnection(Socket client, String clientAddress) {
		try {
			System.out.println();
			BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
			OutputStream out = client.getOutputStream();
			String command = in.readLine();
			String[] commAr = command.split("\\s");

			String httpVer = "";
			if (commAr[0] == null) {
				NetUtil.writeASCII(responseCodes(400), out);
				NetUtil.writeCRLF(out);
				close(client);
				throw new IOException("No data was sent by the client.");
			}
			if (commAr.length >= 3) {
				if (commAr.length > 3) {
					NetUtil.writeASCII(responseCodes(400), out);
					NetUtil.writeCRLF(out);
					close(client);
					throw new IOException("No data was sent by the client.");
				} else if (commAr[2].equals("HTTP/1.1") || commAr[2].equals("HTTP/1.0")) {
					httpVer = commAr[2] + " ";
				} else {
					NetUtil.writeASCII(responseCodes(400), out);
					NetUtil.writeCRLF(out);
					close(client);
					throw new IOException("Not a specified HTTP/1.1 or HTTP/1.0 request by client");
				}
			}

			if (command.startsWith("GET"))
				GET(commAr[1], out, httpVer);
			else if (command.startsWith("HEAD"))
				HEAD(commAr[1], out, httpVer);
			else if (command.equals("LIST"))
				LIST(out, httpVer);
			else if (commAr[0].equals("POST") || commAr[0].equals("PUT") || commAr[0].equals("HEAD")
					|| commAr[0].equals("DELETE") || commAr[0].equals("PATCH") || commAr[0].equals("OPTIONS")) {
				if (commAr.length == 3) {
					NetUtil.writeASCII(commAr[2] + " " + responseCodes(501), out);
					NetUtil.writeCRLF(out);
					System.out.print("Client requested " + commAr[0] + ", " + commAr[2] + " " + responseCodes(501));
				} else {
					NetUtil.writeASCII(responseCodes(501), out);
					NetUtil.writeCRLF(out);
					System.out.print("Client requested " + commAr[0] + ", " + responseCodes(501));
				}

			} else {
				NetUtil.writeASCII(responseCodes(400), out);
				NetUtil.writeCRLF(out);
				throw new IOException("improper request by client.");
			}
		} catch (Exception e) {
			if (e instanceof SocketTimeoutException) {
				System.out.println("Timeout on connection with: " + clientAddress + ": " + e);
			} else {
				System.out.println("An error occurred while communicating with the client.");
				System.out.println("Error: " + e);
			}
		}

		close(client);
	}

	/**
	 * Closes client connection
	 * 
	 * @param client
	 *            client thats being handled
	 */
	private final static void close(Socket client) {
		try {
			client.close();
		} catch (Exception e) {
		}
	}

	/**
	 * Server returns list of users files in directory
	 * 
	 * @param out
	 *            Bitstream output to print to user
	 * @throws IOException
	 */
	private final static void LIST(OutputStream out, String httpVer) throws IOException {
		NetUtil.writeASCII("OK", out);
		NetUtil.writeCRLF(out);
		File[] files = directory.listFiles();
		for (File f : files) {
			if (f.isFile() && f.canRead()) {
				NetUtil.writeASCII(NetUtil.encodeURL(f.getName()), out);
				NetUtil.writeCRLF(out);
			}
		}
		System.out.println("Sent directory listing.");
	}

	/**
	 * Returns Content-Length and Content-Type and body of URI
	 * 
	 * @param command
	 *            URI String
	 * @param out
	 *            Output stream to write to user
	 * @throws IOException
	 */
	private final static void GET(String command, OutputStream out, String httpVer) throws IOException {
		if (command.equals("/")){
			command = "index.html";
		}
		String fileName = URLDecoder.decode(command, "UTF-8");
		File file = new File(directory, fileName);
		if (file.isFile() && file.canRead()) {
			printHeaders(command, out, httpVer, file);
			InputStream fileIn = new FileInputStream(file);
			NetUtil.copyStream(fileIn, out);
			fileIn.close();
			System.out.println("Sent file " + fileName);
		} else {
			NetUtil.writeASCII(responseCodes(404), out);
			NetUtil.writeCRLF(out);
			System.out.println("Unknown file requested: " + fileName);
		}
	}

	/**
	 * Returns Content-Length and Content-Type of URI
	 * 
	 * @param command
	 *            URI String
	 * @param out
	 *            Output stream to write to user
	 * @throws IOException
	 */
	private final static void HEAD(String command, OutputStream out, String httpVer) throws IOException {
		String fileName = URLDecoder.decode(command, "UTF-8");
		File file = new File(directory, fileName);
		if (file.isFile() && file.canRead()) {
			printHeaders(command, out, httpVer, file);
			System.out.println("Sent file information" + fileName);
		} else if (file.isFile() && !file.canRead()) {
			NetUtil.writeASCII(responseCodes(403), out);
			NetUtil.writeCRLF(out);
			System.out.println("Client attempted to access: " + fileName);
		} else {
			NetUtil.writeASCII(responseCodes(404), out);
			NetUtil.writeCRLF(out);
			System.out.println("Client requested a file that doesn't exist: " + fileName + ", ");
		}

	}

	/**
	 * prints headers for certain requests
	 * 
	 * @param command
	 *            name of file
	 * @param out
	 *            output stream used to write to client
	 * @param httpVer
	 *            the http version that it was requested on
	 * @param file
	 *            file
	 * @throws IOException
	 */
	private final static void printHeaders(String command, OutputStream out, String httpVer, File file)
			throws IOException {
		NetUtil.writeASCII(httpVer, out);
//		NetUtil.writeCRLF(out);
		NetUtil.writeASCII(responseCodes(200), out);
		NetUtil.writeCRLF(out);
		NetUtil.writeASCII("Content-Type: " + contentType(command), out);
		NetUtil.writeCRLF(out);
		NetUtil.writeASCII(contentLength(file), out);
		NetUtil.writeCRLF(out);
		NetUtil.writeASCII(printConnection(), out);
		NetUtil.writeCRLF(out);
		NetUtil.writeCRLF(out);
	}

	/**
	 * returns specifc response codes
	 * 
	 * @param error
	 *            numerical representation of response code
	 * @return returns reponse string
	 */
	private final static String responseCodes(int error) {

		switch (error) {
		case 200:
			return "200 OK";
		case 400:
			return "400 Bad Request";
		case 404:
			return "404 File Not Found";
		case 403:
			return "403 Forbidden";
		case 501:
			return "501 Not Implemented";
		case 500:
			return "500 Internal Server Error";
		case 505:
			return "505 HTTP Version Not Supported";
		}
		return "";
	}

	/**
	 * returns connection status
	 * 
	 * @return connection status
	 */
	private final static String printConnection() {
		return "Connection: close";
	}

	/**
	 * Returns content length
	 * 
	 * @param file
	 *            name of file being searched
	 * @return
	 */
	private final static String contentLength(File file) {
		return "Content-Length: " + file.length();
	}

	/**
	 * returns mime type of file using its extension
	 * 
	 * @param file
	 *            name of file
	 * @return mime type of file
	 */
	private final static String contentType(String file) {
		String[] sArray = file.split("\\.");
		switch (sArray[1]) {
		case "html":
			return "text/html";
		case "txt":
			return "text/plain";
		case "css":
			return "text/css";
		case "js":
			return "text/javascript";
		case "java":
			return "text/x-java";
		case "c":
			return "text/x-csrc";
		case "jpg":
			return "text/jpeg";
		case "jpeg":
			return "text/jpeg";
		case "png":
			return "text/png";
		case "gif":
			return "text/gif";
		}
		return "x-application/x-unknown";
	}
}
