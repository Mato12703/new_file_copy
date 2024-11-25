package sk.upjs.kopr.file_copy.client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.Callable;

import sk.upjs.kopr.file_copy.FileRequest;

public class FileReceiveTask implements Callable<String>{
	private static final int BUFFER_SIZE = 16384;
	private MyFileWriter myFileWriter;
	private long offset;
	private long length; // length of data to be received
	private InetAddress inetAddress;
	private int serverPort;
	private FileCopyController fileCopyController;
	private long dataRead = 0L;
	private String data = new String();

	
	public FileReceiveTask(File fileToSave, long fileSize, long offset, long length, InetAddress inetAddress, int serverPort,FileCopyController fileCopyController) throws IOException {
		this.offset = offset;
		this.length = length;
		this.inetAddress = inetAddress;
		this.serverPort = serverPort;
		this.fileCopyController = fileCopyController;
		myFileWriter = MyFileWriter.getInstance(fileToSave, fileSize);
	}

	@Override
	public String call() throws Exception {
		try(Socket socket = new Socket(inetAddress, serverPort)) {
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			oos.writeUTF("file");
			oos.flush();
			FileRequest fileRequest = new FileRequest(offset, length);
			oos.writeObject(fileRequest);
			oos.flush();
			long fileOffset = offset;
			while(true) {
				if (Thread.currentThread().isInterrupted()) {
					oos.close();
					ois.close();
					myFileWriter.close();

					if (dataRead < length) {

						data += Long.toString(offset + dataRead) + " " + Long.toString(length - dataRead);

					}
					return data;
				}

				byte[] bytes = ois.readNBytes(BUFFER_SIZE);
				if (bytes.length > 0) {
					myFileWriter.write(fileOffset, bytes, 0, bytes.length);
					dataRead = dataRead + bytes.length;
					fileCopyController.addData(bytes.length);

				}
				if (bytes.length < BUFFER_SIZE) {
					oos.close();
					ois.close();


					fileCopyController.cdl.countDown();
					if(fileCopyController.cdl.getCount()==0){
						myFileWriter.close();
					}
					break;

				}
				fileOffset += bytes.length;
				if ((fileOffset / BUFFER_SIZE) % 1000 == 0) {
					System.out.println( "Task is being executed by thread: " + Thread.currentThread().getName() + " "  + fileOffset);
				}
			}
		} catch (EOFException | SocketException e) {

		System.out.println("Task nema spojenie so serverom (napr. server bol vypnuty).");

		} catch (IOException e) {

		}
		if (dataRead < length) {

			data += Long.toString(offset + dataRead) + " " + Long.toString(length - dataRead);

		}
		return data;
	}

}
