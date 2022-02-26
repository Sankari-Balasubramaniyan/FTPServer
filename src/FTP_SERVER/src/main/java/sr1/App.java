package sr1;

import sr1.server.FTPServer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) {
        FTPServer server = new FTPServer();
        server.start();
    }
}
