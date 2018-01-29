package milo.te4.beans;

import milo.te4.utilities.BCrypt;
import milo.te4.utilities.ConnectionFactory;
import javax.ejb.Stateless;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Stateless
public class LoginBean {
    // metod för att skapa användare
    public int createUser(String body){
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(body));
            JsonObject user = jsonReader.readObject();
            jsonReader.close();

            String username = user.getString("username");
            String password = user.getString("password");
            int rights = 1;

            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = "SELECT username FROM users";
            ResultSet usernameData = stmt.executeQuery(sql);

            List<String> usernames = new ArrayList<>();
            while(usernameData.next()){
                usernames.add(usernameData.getString("username"));
            }

            //kolla om användarnamnet inte är unikt
            if(usernames.contains(username))
                return 400;
            //kolla om lösenordet är starkt nog (minst 8 tecken, en stor och en liten bokstav, en siffra och ett specialtecken)
            if(!Pattern.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[$@$!%*?&])[A-Za-z\\d$@$!%*?&]{8,}", password))
                return 400;

            String hash = BCrypt.hashpw(password, BCrypt.gensalt());

            sql = String.format("INSERT INTO users VALUES ('%s', '%s', %d)", username, hash, rights);
            stmt.executeUpdate(sql);
            connection.close();

            return 200;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 500;
    }
    // verifiera användare, körs vid alla POST, PUT och DELETE förfrågningar, även vid inloggning på sidan
    public boolean checkCredentials(String basic_auth){
        basic_auth = basic_auth.substring(basic_auth.indexOf(" ") + 1);
        byte[] decoded = Base64.getDecoder().decode(basic_auth);
        String credentials = new String(decoded);
        String username = credentials.substring(0, credentials.indexOf(":"));
        String password = credentials.substring(credentials.indexOf(":") + 1);

        try {
            Connection connection = ConnectionFactory.getConnection();
            Statement stmt = connection.createStatement();
            String sql = "SELECT * FROM users WHERE username = " + "'" + username + "'";
            ResultSet user = stmt.executeQuery(sql);

            if(user.next()){
                if(BCrypt.checkpw(password, user.getString("password"))){
                    return true;
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
