package sr1.utils;

import java.math.BigInteger;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Users {

    private String username;
    private String pwd;
    private int port;
    private Path cWorkingDir = Paths.get(System.getProperty("user.dir"));

    public Users(String username, String pwd, int port) {
        this.username = username;
        this.pwd = pwdEncrypt(pwd);
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public String getPwd() {
        return pwd;
    }

    public int getPort() {
        return port;
    }

    public void setcWorkingDir(Path cWorkingDir) {
        this.cWorkingDir = cWorkingDir;
    }

    public Path getcWorkingDir() {
        return cWorkingDir;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
     * <p>Encryption of the password the algorithm SHA-1</p>
     * @param pwd <i>the real user password</i>
     * @return <i>a string is returning different to the real password</i>
     */
    public static String pwdEncrypt(String pwd){
        try{
            MessageDigest msgd = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = msgd.digest(pwd.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            StringBuilder hashtext = new StringBuilder(no.toString(16));

            // Add preceding 0s to make it 32 bit
            while (hashtext.length() < 32) {
                hashtext.insert(0, "0");
            }
            // return the HashText
            return hashtext.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean equals(Object o){
        if(!(o instanceof Users)){
            return false;
        }
        Users user = (Users) o;
        return this.username.equals(user.username) && this.pwd.equals(user.pwd);
    }


}
