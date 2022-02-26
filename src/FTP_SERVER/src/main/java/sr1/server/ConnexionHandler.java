package sr1.server;

import sr1.utils.Command;
import sr1.utils.Users;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.management.RuntimeErrorException;

public class ConnexionHandler implements Runnable {
	private Socket clientSocket;
	private BufferedReader in;
	private BufferedWriter out;
	
	private int count=0;
	private File oldFileName;
	private FTPServerDTP dataServer;

	private String type = "BINARY";
	private String line;
	private static Command cmd = new Command("ncmd");
	private List<Users> connectedUsers = new ArrayList<>();

	List<Users> users = Arrays.asList(new Users("user1", "12345", 0), new Users("user2", "12345", 0),
			new Users("user3", "12345", 0));

	private String username = "";

	public ConnexionHandler(Socket clientSocket) throws IOException {
		this.clientSocket = clientSocket;
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream())); 
		out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream())); 
	}

	@Override
	public void run() {
		reply("220 FTP server");
		System.out.println("connection");
		while (!this.clientSocket.isClosed()) {
			try {
				line = readLine();
				System.out.println(line);
				
				stepCreateUser(line, this.clientSocket);
				
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		
	}

	/**
	 * <p>This function help to find a user by name.</p>
	 * @param name <i>the user's name.</i>
	 * @return <i>return the user or null.</i>
	 */
	private boolean findUserByName(String name) {
		for (Users u : users) {
			if (u.getUsername().equals(name))
				return true;
		}
		return false;
	}

	/**
	 * <p>THis function play the role of the authentication function.</p>
	 * @param line <i>A string received from the client, asking for connexion to the server.</i>
	 * @param clientSocket <i>the client socket.</i>
	 */
	public void stepCreateUser(String line, Socket clientSocket) {
		if (line.startsWith("USER")) {
		
			if (findUserByName(Arrays.asList(line.split(" ")).get(1))) {
				username = Arrays.asList(line.split(" ")).get(1);
				reply("331 username is OK");
				System.out.println("user excuted");
			} else {
				reply("430 invalid username");
			}
		} else if (line.startsWith("PASS")) {
			Users user = new Users(username, Arrays.asList(line.split(" ")).get(1), clientSocket.getPort());
			if (foundUser(user) != null) {
				connectedUsers.add(user);
				reply("230 connexion successfully");
				System.out.println("password excuted");
			} else {
				reply("430 invalid password");
			}
		} else {
			List<String> cmdInfos = Arrays.asList(line.split(" "));
			treatCmd(cmdInfos.get(0), clientSocket);
		}
	}

	/**
	 * <p>The server reply to the client by this function, it send the code corresponding to the action that it performed.</p>
	 * @param response <i>a response message.</i>
	 */
	private void reply(String response) {
		try {
			out.write(response);
			out.write("\r\n");
			out.flush();
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	}

	private String readLine() throws IOException {
		return in.readLine();
	}

	/**
	 * <p>As certain ftp's command are different to the project's requirements ones,this function does the conversion.</p>
	 * @param cmd <i>The command coming from to client.</i>
	 * @return <i>The requirement command.</i>
	 */
	private String convertCommand(String cmd) {
		String c_cmd = "";
		switch (cmd) {
		case "type":
			c_cmd = "type";
			break;
		case "SYST":
			c_cmd = "syst";
			break;
		case "passive":
			c_cmd = "PASV";
			break;
		case "ls":
			c_cmd = "lst";
			break;
		case "cd":
			c_cmd = "cwd";
			break;
		case "cdup":
			c_cmd = "cdup";
			break;
		case "mkdir":
			c_cmd = "mkd";
			break;
		case "put":
			c_cmd = "put";
			break;
		case "get":
			c_cmd = "get";
			break;
		case "rmdir":
			c_cmd = "rmd";
			break;
		case "pwd":
			c_cmd = "pwd";
			break;
		case "rename":
			if(count==0) {
				c_cmd = "RNFR";
				count=1;
			}
			else {
				c_cmd = "RNTO";
				count=0;
			}
			break;
		default:
			c_cmd = cmd;
		}
		return c_cmd;
	}

	/**
	 * <p>When a command is received from the client, this function execute the right function.</p>
	 * @param cmd <i>the client's command</i>
	 * @param clientSocket <i>the client's socket</i>
	 */
	private void treatCmd(String cmd, Socket clientSocket) {
		String mod = convertCommand(cmd);
		System.out.println(mod);
		switch (mod.toUpperCase()) {
		case "FEAT":
			System.out.println("FEATURES");
			reply("211-features:");
			reply("EPRT");
			reply("EPSV");
			reply("PASV");			
			reply("REST STREAM");
			reply("SIZE");
			reply("TVFS");
			reply("211 End");
			break;
		case "AUTH":
			reply("530 Please log in");
			break;
		case "TYPE":
			ConnexionHandler.cmd.setCmd("TYPE");
			transferType(line);
			break;
		case "SYST":
			ConnexionHandler.cmd.setCmd("SYST");
			system(line);
			break;
		case "PWD":
			ConnexionHandler.cmd.setCmd("PWD");
			askForCurrentDirectory(clientSocket);
			break;
		case "PASV":
			ConnexionHandler.cmd.setCmd("PASV");
			try {
				connectPassively();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		case "LIST":
		case "LST":
			ConnexionHandler.cmd.setCmd("LST");
			askForDirectoryContent(line, clientSocket);
			break;
		case "CWD":
			ConnexionHandler.cmd.setCmd("CWD");
			askForChangingWorkDirectory(line, clientSocket);
			break;
		case "CDUP":
			ConnexionHandler.cmd.setCmd("CDUP");
			askToLeaveWorkDirectory(clientSocket);
			break;
		case "GETT":
			ConnexionHandler.cmd.setCmd("GETT");
			//downloadTextFile(line, clientSocket, dataSocket);
		case "GET":
			ConnexionHandler.cmd.setCmd("GET");
			downloadTextFile(line);
			break;
		case "PUT":
			ConnexionHandler.cmd.setCmd("PUT");
			upload(line);
			break;
		case "RNFR":
			ConnexionHandler.cmd.setCmd("RNFR");
			direcToRename(line,clientSocket);
			break;
		case "RNTO":
			ConnexionHandler.cmd.setCmd("RNTO");
			renameRemoteDirec(line,clientSocket);
			break;
		case "MKD":
			ConnexionHandler.cmd.setCmd("MKD");
			createRemoteDirec(line, clientSocket);
			break;
		case "RMD":
			ConnexionHandler.cmd.setCmd("RMD");
			deleteRemoteDirec(line, clientSocket);
			break;
		case "QUIT":
			try {
				reply("221 GoodBye");
				clientSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			break;
		}
	}

	/**
	 * <p>To switch to mode of the transfer's type.</p>
	 * @param line <i>the client message.</i>
	 */
	private void transferType(String line) {
		String[] message = line.split(" ");
		if (message[1].equals("I")) {
			type = "BINARY";
			reply("200 Switching to Binary mode.");
		} else if (message[1].equals("A")) {
			type = "ASCII";
			reply("200 Switching to ASCII mode.");
		} else
			reply("504 Type not changed");
	}

	private void system(String line) {
		reply("215 UNIX");
	}


	/**
	 * <p>Showing the current directory</p>
	 * @param clientSocket <b>the client socket</b>
	 */
	private void askForCurrentDirectory(Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
		if (user != null) {
			Path directory = user.getcWorkingDir();
			reply("257 \"" + directory + "\" is the current directory.");
			System.out.println("PWD command function excuted");
		} else {
			closeConnexion(clientSocket, "No logIn");
		}
	}

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

	/**
	 * <p>
	 * When the user send the command in the form of LST repName, the server receive
	 * a string and then this string is splitted into a list of string of two
	 * elements; the second element represent the name of a repository. with this
	 * second element, a Path object is created and this object is passed through
	 * the function who list the content of the directory.
	 * </p>
	 * 
	 * @param line         String
	 * @param clientSocket <i>the client socket</i>
	 */
	private void askForDirectoryContent(String line, Socket clientSocket) {
		Users user = this.isLogged(clientSocket);

		if (user != null) {
			System.out.println("list command function execute");
			List<String> cmdInfos = Arrays.asList(line.split(" "));
			reply("150 Here comes the directory listing.");

			if (cmdInfos.size() == 2) {
				String dirName = cmdInfos.get(1);
				Path path = Paths.get(dirName);

				displayDirContent(path);

			} else {
				if (cmdInfos.size() == 1) {
					displayDirContent(user.getcWorkingDir());
				}
			}

		} else {
			closeConnexion(clientSocket, "No logIn");
		}
		reply("226 Directory send OK");
		dataServer.close();
	}

	/**
	 * <p>
	 * This function allow to browse the directory passed in parameter as a object
	 * of Path.
	 * </p>
	 * 
	 * @param path <i>the directory</i>
	 */
	private void displayDirContent(Path path) {

		try {
			ProcessBuilder processBuilder = new ProcessBuilder();

			processBuilder.command("bash", "-c", "ls -la " + path.toString());

			Process process = processBuilder.start();
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			int exitVal = process.waitFor();
			
			String line;
			
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
				dataServer.dataWrite(line);
			}
			dataServer.dataWrite("\r\n");
			

		} catch (Exception e) {
			reply("550 fail to send");
			throw new RuntimeException(e);

		}
	}

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

	/**
	 * This function leave the currentworking directory to the immediate root.
	 * 
	 * @param clientSocket
	 */

	private void askToLeaveWorkDirectory(Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
		if (user != null) {
			String currentDir = (user.getcWorkingDir()).toAbsolutePath().toString();
			int remoteDirecIndex = currentDir.lastIndexOf("/");
			if (remoteDirecIndex == 0) {
				reply("550 Can not leave root directory !");
			} else {
				user.setcWorkingDir(user.getcWorkingDir().getParent());
				reply("250 Left remote directory successfully !");
			}
		} else {
			closeConnexion(clientSocket, "No logIn");
		}
	}

	/**
	 * <p>To download the text file</p>
	 * @param line <i>the name of the file.</i>
	 */
	private void downloadTextFile(String line) {
		Users user = this.isLogged(clientSocket);
    	String fileName = Arrays.asList(line.split(" ")).get(1);
    	Path filePath = user.getcWorkingDir().resolve(fileName);
    	try {
			FileReader reader = new FileReader(filePath.toString());
			BufferedReader in = new BufferedReader(reader);
	        String str;
	         while ((str = in.readLine()) != null) {
	        	dataServer.dataWrite(str); 
	         }
	         reader.close();
	         in.close();			
		} catch (IOException e) {
			throw new RuntimeException(e);
		}     	
    }
	/**
	 * <p>To upload the text file</p>
	 * @param line <i>the name of the file.</i>
	 */
	 private void upload(String line) {
         if(type.equals("ASCII")) {
             uploadText(line);
         } else if(type.equals("BINARY")) {
             uploadBinary(line);
             }
         else {
             reply("504 No Type");
         }
     }

	/**
	 * <p>To upload the text file</p>
	 * @param line <i>the name of the file.</i>
	 */
     private void uploadText(String line) {
         Users user = this.isLogged(clientSocket);
         String[] message = line.split(" ");
         if(user != null) {
             String currentDir = (user.getcWorkingDir()).toAbsolutePath().toString();
             try {
                 FileWriter file = new FileWriter(currentDir + "/" + message[1]);
                 String code = dataServer.dataRead();
                 while (code != null) {
                     file.write(code + "\n");
                     code = dataServer.dataRead();
                 }
                 file.close();
                 dataServer.close();
                 reply("226 ASCII upload complete.");
             } catch (IOException e) {
                 reply("553 Failed to upload the file");
             }
         } else {
             closeConnexion(clientSocket, "No logIn");
         }
     }

	/**
	 * <p>To upload the text file by binary mode.</p>
	 * @param line <i>the name of the file.</i>
	 */
     private void uploadBinary(String line) {
         Users user = this.isLogged(clientSocket);
         String[] message = line.split(" ");
         if(user != null) {
             String currentDir = (user.getcWorkingDir()).toAbsolutePath().toString();
             try {
                 File file = new File(currentDir + "/" + message[1]);
                 FileOutputStream fileOutput = new FileOutputStream(file);
                 byte[] buff = new byte[4096];
                 int code = dataServer.in.read(buff);
                 while (code != -1) {
                     fileOutput.write(buff);
                     code = dataServer.in.read(buff);
                 }
                 fileOutput.close();
                 dataServer.close();
                 reply("226 Binary upload complete.");
             } catch (IOException e) {
                 reply("553 Failed to upload the file");
             }
         } else {
             closeConnexion(clientSocket, "No logIn");
         }
     }

	/**
	 * <p>To get from the client the directory that he would like to rename.</p>
	 * @param line <i>the name of the file/directory.</i>
	 * @param clientSocket   <i>the client's socket</i>
	 */
	 private void direcToRename(String line, Socket clientSocket) {
        Users user = this.isLogged(clientSocket);
        if (user != null) {
            List<String> cmdInfos = Arrays.asList(line.split(" "));
            Path path_file_one = Paths.get(cmdInfos.get(1));
            oldFileName = new File(path_file_one.toAbsolutePath().toString());
        } else {
            closeConnexion(clientSocket, "No logIn");
        }
	}

	 /** This function renames a remote file or directory
	 * 
	 * @param line line containing the name of the file to be renamed and new file
	 * name
	 * 
	 * @param clientSocket socket
	 */
	private void renameRemoteDirec(String line, Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
	    if (user != null) {
	        List<String> cmdInfos = Arrays.asList(line.split(" "));
	        Path path_file_two = Paths.get(cmdInfos.get(1));
	        File newFileName = new File(path_file_two.getFileName().toString());
	        if (oldFileName.renameTo(newFileName)) {
	            reply("250 File rename successful");
	        } else {
	            reply("550 File rename unsuccessful");
	        }
	    } else {
	        closeConnexion(clientSocket, "No logIn");
	    }
	}

	/**
	 * This function creates a new remote directory
	 * 
	 * @param line         line contains the command sent by client which contains
	 *                     the name of new directory to be created
	 * @param clientSocket Socket
	 */
	private void createRemoteDirec(String line, Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
		if (user != null) {
			Path currentDir = user.getcWorkingDir();
			List<String> cmdInfos = Arrays.asList(line.split(" "));
			File newDirec = new File(currentDir.toAbsolutePath() + "/" + cmdInfos.get(1));
			if (newDirec.mkdir()) {
				reply("257 New directory created !");
			} else {
				reply("553 Could not create new directory since it already exist !");
			}
		} else {
			closeConnexion(clientSocket, "No logIn");
		}
	}

	/**
	 * This function deletes a remote directory
	 * 
	 * @param line         Line which contains the name of the directory to be
	 *                     deleted.
	 * @param clientSocket Sockets
	 */

	private void deleteRemoteDirec(String line, Socket clientSocket) {
		Users user = this.isLogged(clientSocket);
		if (user != null) {
			List<String> cmdInfos = Arrays.asList(line.split(" "));
			Path path = Paths.get(cmdInfos.get(1));
			if (Files.isDirectory(path)) {
				File deleteDirec = new File(path.getFileName().toString());
				if (deleteDirec.delete()) {
					reply("250 Directory deleted successfully !");
				} else {
					reply("550 Directory is not deleted !");
				}
			} else {
				reply("553 Directory does not exist !");
			}
		} else {
			closeConnexion(clientSocket, "No logIn");
		}
	}

	/**
	 * <p>
	 * This function check if the client is already logged. By searching the port
	 * linked to the socket is in the list of ports of the clients who are already
	 * logged, by using the List contains() method. This list is an arrayList of
	 * integer.
	 * </p>
	 * 
	 * @param clientSocket <i>the client socket</i>
	 * @return <i>true if the port is in the list, false otherwise.</i>
	 */
	private Users isLogged(Socket clientSocket) {
		int port = clientSocket.getPort();
		for (Users u : connectedUsers) {
			if (u.getPort() == port)
				return u;
		}
		return null;
	}

	/**
	 * <p>
	 * Close the connexion with the client
	 * </p>
	 * 
	 * @param clientSocket <i>the client socket</i>
	 * @param msg          <i>the message to the client</i>
	 */
	private void closeConnexion(Socket clientSocket, String msg) {
		try {
			reply(msg);
			in.close();
			out.close();
			clientSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * <p>
	 * When the user send the command under the form of CON username password, the
	 * server receive a string and then this string is splitted as a list of 3
	 * elements; a new user is created with the second and the third element of the
	 * list. the function try to find this user in the list of users saved in the
	 * server through the function <i>foundUser(user)</i>.
	 * </p>
	 * 
	 * @param line         String
	 * @param clientSocket <i>the client socket</i>
	 */
	private void askForConnexion(String line, Socket clientSocket) {
		List<String> cmdInfos = Arrays.asList(line.split(" "));
		if (cmdInfos.size() == 3) {
			Users user = foundUser(new Users(cmdInfos.get(1), cmdInfos.get(2), clientSocket.getPort()));
			if (user == null) {
				closeConnexion(clientSocket, "BADU or BADP");
			} else {
				reply("Connexion granted");
				user.setPort(clientSocket.getPort());
				connectedUsers.add(user);
			}
		} else {
			reply("No user provide, please provide the username and the password");
		}
	}

	/**
	 * <p>
	 * This function apply a loop over the list of users saved in the server, and
	 * then compare each of the to the user passed in parameter.
	 * </p>
	 * 
	 * @param user <i>the User who send the command</i>
	 * @return <i>true if the user is exist in the list, false otherwise</i>
	 */
	private Users foundUser(Users user) {
		for (Users u : users) {
			if (user.equals(u)) {
				return u;
			}
		}
		return null;
	}
}
