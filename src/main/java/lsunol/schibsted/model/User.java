package lsunol.schibsted.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class User {

    private final static Logger log = Logger.getLogger(User.class.getName());

    private String username;
    private List<String> roles;
    private String password;

    public User(String username, String password, List<String> roles) {
        this.username = username;
        this.password = calculateMd5(password);
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public void setPassword(String newPassword) {
        this.password = calculateMd5(newPassword);
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isPasswordCorrect(String password) {
        return this.password.equals(calculateMd5(password));
    }

    /**
     * Generates the MD5 sum for the given <code>messageToHash</code>.
     * https://howtodoinjava.com/security/how-to-generate-secure-password-hash-md5-sha-pbkdf2-bcrypt-examples/
     *
     * @param messageToHash String to be hashed.
     * @return the MD5 sum for the given <code>messageToHash</code>.
     */
    private static String calculateMd5(String messageToHash) {
        String generatedPassword = null;
        try {
            // Create MessageDigest instance for MD5
            MessageDigest md = MessageDigest.getInstance("MD5");
            //Add password bytes to digest
            md.update(messageToHash.getBytes());
            //Get the hash's bytes
            byte[] bytes = md.digest();
            //This bytes[] has bytes in decimal format;
            //Convert it to hexadecimal format
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++) {
                sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            //Get complete hashed password in hex format
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "Could not find the MD5 algorithm. Exception: " + e.getMessage(), e);
        }
        return generatedPassword;
    }
}
