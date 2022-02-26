package sr1.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FTPServerDTP implements Runnable {
	private final ServerSocket dataServerSocket;
	private Socket dataSocket;
	private BufferedReader dataIn;
	private BufferedWriter dataOut;
	protected InputStream in;

	public FTPServerDTP(String address, int port) {
		try {
			this.dataServerSocket = new ServerSocket(port, 100, InetAddress.getByName(address));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void run() {
		try {
			dataSocket = dataServerSocket.accept();
			in = dataSocket.getInputStream();
			dataIn = new BufferedReader(new InputStreamReader(in));																
			dataOut = new BufferedWriter(new OutputStreamWriter(dataSocket.getOutputStream()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String dataRead() {
		String reply;
		try {
			reply = dataIn.readLine();
		} catch (IOException e) {
			throw new RuntimeException("Can't read from Datasocket");
		}
		return reply;
	}

	public void dataWrite(String message) {
		try {
			dataOut.write(message);
			dataOut.write("\r\n");
			dataOut.flush();
		} catch (IOException e) {
			throw new RuntimeException("Can't write through Datasocket");
		}
	}

	public void close() {
		try {
			dataOut.flush();
			dataOut.close();
			dataIn.close();
			dataSocket.close();
			dataServerSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't close the socket");
		}
	}
}
