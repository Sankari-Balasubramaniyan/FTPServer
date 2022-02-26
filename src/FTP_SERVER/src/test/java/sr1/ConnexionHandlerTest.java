package sr1;

import org.junit.Before;
import org.junit.Test;
import sr1.server.ConnexionHandler;
import sr1.utils.Users;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ConnexionHandlerTest {
    ServerSocket ss;
    public List<Users> connectedUsers = new ArrayList<>();
    Socket clientSocket;
    Users client1;
    private ConnexionHandler ch;
    private static boolean run = true;
    public ConnexionHandlerTest() throws IOException {
        ss = new ServerSocket(8080, 5, InetAddress.getByName("127.0.0.1"));
    }
    /*@Before
    public void launchServer(){
        try{
            while (run){
                clientSocket = ss.accept();
                run = false;
            }
            ch = new ConnexionHandler(clientSocket);
            client1 = new Users("client1", "12345", clientSocket.getPort());
            connectedUsers.add(client1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Test
    public void isLoggedTrue(){
        assertEquals(client1, ch.isLogged(clientSocket));
    }*/
}
