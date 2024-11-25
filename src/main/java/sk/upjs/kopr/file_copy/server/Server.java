package sk.upjs.kopr.file_copy.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

	public static final int SERVER_PORT = 5000;
	public static final File FILE_TO_SHARE = new File("C:\\Users\\marti\\Downloads\\Afja05_redukcia_optim_B.wmv");
	
	public static void main(String[] args) throws IOException {
		ExecutorService executor = Executors.newCachedThreadPool();
		if (! FILE_TO_SHARE.exists() || ! FILE_TO_SHARE.isFile()) {
			throw new FileNotFoundException("No such file: " + FILE_TO_SHARE);
		}
		RandomAccessFile raf = new RandomAccessFile(FILE_TO_SHARE, "r");
		raf.close();
		try (ServerSocket ss = new ServerSocket(SERVER_PORT)) {
			System.out.println("Sharing file " + FILE_TO_SHARE + " with size " + (FILE_TO_SHARE.length()/1_000_000) + " MB");
			System.out.println("Server is running on port " + SERVER_PORT + " ...");

			while(true) {
				Socket socket = ss.accept();
				FileSendTask fileSendTask = new FileSendTask(FILE_TO_SHARE, socket);
				executor.submit(fileSendTask);
			}
		}
		
	}

}
