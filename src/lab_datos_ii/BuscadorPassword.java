package lab_datos_ii;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger; 
import java.security.MessageDigest; 
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BuscadorPassword {
    
    private ArrayList<String> passwords;
    private String[] users;
    private String[] user_passwords;
    
    public BuscadorPassword (boolean readFile) {
        passwords = new ArrayList<>();
            
        if (readFile) {

            File file = new File("src/BaseDeDatos/rockyou.txt");

            BufferedReader br;

            try {
                br = new BufferedReader(new FileReader(file));

                // Declaring a string variable
                String st;
                while ((st = br.readLine()) != null)
                    this.passwords.add(st);

            } catch (FileNotFoundException ex) {
                Logger.getLogger(BuscadorPassword.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(BuscadorPassword.class.getName()).log(Level.SEVERE, null, ex);
            }

            this.users = new String[] {"user1", "user2", "user3", "user4", "user5"};
            this.user_passwords = new String[this.users.length];
            for (int i = 0; i < this.users.length; i++) {
                    int pwd_ind = Math.abs(new Random().nextInt() % this.passwords.size());
                    byte[] password = this.passwords.get(pwd_ind).getBytes();

                    byte[] pepper = this.generatePepper();
                    byte[] salt = this.generateSalt();
                    String cod = this.H(password, pepper, salt);

                    this.user_passwords[i] = cod;
            }
        }
        
    }
    
    public boolean usuarioValido (String user) {
        boolean encontrado = false;
        for (String usuario : this.users)
            if (usuario.equals(user))
                encontrado = true;
        
        return encontrado;
    }
    
    public ArrayList<String> getPasswords () {
        return this.passwords;
    }
    
    public String H (byte[] password, byte[] pepper, byte[] salt) {
        try { 
            // getInstance() method is called with algorithm SHA-512 
            MessageDigest md = MessageDigest.getInstance("SHA-512"); 
  
            byte[] input = this.orOperation(password, pepper, salt);
            
            byte[] messageDigest = md.digest(input); 
  
            // Convert byte array into signum representation 
            BigInteger no = new BigInteger(1, messageDigest); 
  
            // Convert message digest into hex value 
            String hashtext = no.toString(16); 
  
            // Add preceding 0s to make it 32 bit 
            while (hashtext.length() < 32) { 
                hashtext = "0" + hashtext; 
            } 
  
            // return the HashText 
            return hashtext; 
        } 
  
        // For specifying wrong message digest algorithms 
        catch (NoSuchAlgorithmException e) { 
            return null;
        } 
    }
    
    private byte[] orOperation (byte[] password, byte[] pepper, byte[] salt) {
        byte[] result;
        if (salt.length > password.length) {
            result = new byte[salt.length];
            for (int i = 0; i < salt.length; i++) {
                if (i == 0) {
                    result[0] = (byte) (pepper[0] | salt[0] | password[0]);
                } else if (i < password.length) {
                    result[i] = (byte) (salt[i] | password[i]);
                } else {
                    result[i] = salt[i];
                }
            }
        } else {
            result = new byte[password.length];
            for (int i = 0; i < password.length; i++) {
                if (i == 0) {
                    result[0] = (byte) (pepper[0] | salt[0] | password[0]);
                } else if (i < salt.length) {
                    result[i] = (byte) (salt[i] | password[i]);
                } else {
                    result[i] = password[i];
                }
            }
        }
        
        return result;
    }
    
    private int randomByte () {
        return (int) ((Math.random() * (255)));
    }
    
    public byte[] passwordToBytes (String password) {
        return password.getBytes();
    }
    
    public byte[] generatePepper () {
        byte[] b = new byte[1];
        new Random().nextBytes(b);
        return "f".getBytes();
    }
    
    public byte[] generateSalt () {
        byte[] b = new byte[16];
        new Random().nextBytes(b);
        return "ffffffffffffffff".getBytes();
    }
    
    public String[] recuperar (String username, String password) {
        byte[] password_bytes = this.passwordToBytes(password);
        
        byte[] pepper = this.generatePepper();
        byte[] salt = this.generateSalt();
        
        String pwd = this.H(password_bytes, pepper, salt);
        
        StringBuilder sb = new StringBuilder();
        for (byte b : salt) {
            sb.append(String.format("%02X ", b));
        }
        
        return new String[] {username, sb.toString(), pwd};
    }
    
    public boolean validarResultado (String[] resultado) {
        
        try {
            FileWriter myWriter = new FileWriter("src/BaseDeDatos/database.txt");
            myWriter.append(resultado[0]+";"+resultado[1]+";"+resultado[2]+"\n");
            myWriter.close();
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        
        String usuario = resultado[0];
        String password = resultado[2];
        
        for (int i = 0; i < this.users.length; i++) {
            if (usuario.equals(users[i]) & password.equals(user_passwords[i])) {
                return true;
            }
        }
        
        return false;
    }
    
    
}
