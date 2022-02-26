# **FTP SERVER**
##### _**SUBMITTED BY**_ - **Sankari Balasubramaniyan**

## **OBJECTIVE**

**Created an FTP server using JAVA programming language adhering to the TCP/IP protocol, which cna be connected with the FTP client and execute the requested commands.**

**Commands performed are :**
- Connect to the server with FTP client.
- Connect passively to exchange data/directory/files.
- List the items in directory.
- Change directory, move back to parent directory and list current directory.
- Remove or create a directory.

## **ENVIRONMENT AND EXECUTION**

**The project is built using maven, hence maven is a prerequisite to run the program.** 

The project also runs without any parameters, the address, port number and user details are pre defined in the program.
At present three different users can connect to this server - {user1 : 12345, user2 : 12345, user3 : 12345}.

**By default, ADDRESS : 127.0.0.1 and PORT : 8080.**

**Execution examples**

- mvn package
- java -jar target/FTP_SERVER-1.0-SNAPSHOT.jar

## **PROGRAM STRUCTURE**

The project has the following classes and methods

**Main Class** : Launches the main function, calls FTPServer class to enable/accept client connection.

**FTPServer Class** : Accepts a new client connection, start a new thread to execute client commands by creating an instance and calling ConnexionHandler class.

**ConnexionHandler Class** : Using command socket that was accepted in FTPServer class, executes all the commands requested by FTP client.

**FTPServerDTP Class** : Accepts data socket to transfer files between Server and Client, which will be executed in a separate thread in ConnexionHandler class.

**User Class** : Implements getters and setters to maintain Username, Password, Port, Working directory details and encrypt password using SHA-1 algorithm.

## **CODE SNIPPETS**

#### **PASSIVE MODE**

```java
/**
	 * <p>This function allow to change the mode, from active mode to passive mode.
	 * In that mode, the FTPServer generate a new random port and user the same ip to
	 * </p>
	 */
	private void connectPassively() throws IOException {
		Random ran = new Random();
		int port1 = ran.nextInt(240) + 10;
		int port2 = ran.nextInt(256);
		String ip = "127.0.0.1";
		int new_port = port1 * 256 + port2;
		dataServer = new FTPServerDTP(ip, new_port);
		Thread thread = new Thread(dataServer);
		thread.start();
		reply("227 Enter passive mode (127,0,0,1" + "," + port1 + "," + port2 + ")");
	}
```

Ftp client tries to connect passively to this server, for which the server responds back with the address and port it is listening to. Calls FTPServerDTP class to accept the client data connection and creates a new thread to execute data commands (transfer files).

#### **CWD CHANGING DIRECTORY** 

```java
/**
	 * <p>
	 * This function allow to change the current working directory to a new working
	 * one, given by the client through the command under the form CWD directName.
	 * if directName is a directory, the current directory represented by the static
	 * Path's object variable ConnexionHandler.cWorkingDir is updating.
	 * </p>
	 * 
	 * @param line         <i>the command send by the client</i>
	 * @param clientSocket <i>the socket client</i>
	 */
	private void askForChangingWorkDirectory(String line, Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
		if (user != null) {
			List<String> cmdInfos = Arrays.asList(line.split(" "));
			if (cmdInfos.size() == 2) {

				Path newWorkDirName = user.getcWorkingDir().resolve(cmdInfos.get(1));
				user.setcWorkingDir(newWorkDirName.toAbsolutePath());
				if (Files.isDirectory(user.getcWorkingDir())) {
					reply("250 Directory changed successfully !");
					System.out.println("250 success");
				} else {
					reply("550 Change directory failed");
					System.out.println("250 success");
				}
			} else {
				reply("Bad command");
			}
		} else {
			closeConnexion(clientSocket, "No logIn");
		}
	}
```
Get the directory to be changed to, from the ftp client. Obtain the absolute path of the directory, then set that as the current working directory.

#### **COMMAND SOCKET**

```java
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
                pool.execute(new ConnexionHandler(this.serverSocket.accept()));
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
```

Accept a new connection from the Ftp client (connecting via default address and port) to establish a command socket. Create a new thread to execute commands via the socket of that particular user independently. 

## **KEY DEVELOPMENT POINTS**

- The program complies with common project guidelines.
- The program works with FTP client like Filezilla.
- The program establishes both command and data pipeline for three different users to execute client requests.
- User password is encrypted using SHA-1 encryption algorithm.

## **DIFFICULTIES**

- Difficulty in implementing Rename command, since the ftp client has only one command while ftp server has two RNFR and RNTO commands.
- Implemented Get and Put commands to download and upload file but could not test it.

#### _The doc folder contains a documention and video presentation._




