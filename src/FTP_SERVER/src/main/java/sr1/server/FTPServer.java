package sr1.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FTPServer implements Runnable{
    private ServerSocket serverSocket;
    private String host = "127.0.0.1";
    private final int PORT = 8080; //default port 21 force using sudo root user, so change to 8080
    private final int POOLSIZE = 4; //the thread pool size
    private final ExecutorService pool;
    protected Thread thread;
	private boolean keepRunning;
    
    public FTPServer(){
        try{
            this.serverSocket = new ServerSocket(this.PORT, 100, InetAddress.getByName(this.host));
            this.pool = Executors.newFixedThreadPool(this.POOLSIZE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void run() {
        System.out.println(">> Lancement du serveur ... ");
        while (keepRunning){
            try{
                pool.execute(new ConnexionHandler(this.serverSocket.accept()));//open socket 
            }catch (IOException ioe){
                pool.shutdownNow();
            }
        }
    }

    public void start(){
    	keepRunning = true;
        thread = new Thread(this);
        thread.start();
    }
    
    public void stop(){
    	this.keepRunning = false;
    }
}
