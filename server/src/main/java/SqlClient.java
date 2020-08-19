import java.sql.*;

public class SqlClient {
    private static Connection connection;
    private static Statement statement;

    synchronized static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:server\\src\\main\\java\\serverUtil\\network-storage.db");
            statement = connection.createStatement();
            resetIsLogin();
            System.out.println("Подключение к базе данных успешно");
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized static void disconnect() {
        try {
            System.out.println("Отключение от базы данных");
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static synchronized boolean authorise(String login, String password) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users_tbl WHERE user_name = ? AND password = ?"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static synchronized boolean isRegisteredUser(String login){
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM users_tbl WHERE user_name = ?"
            );
            statement.setString(1, login);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    static synchronized boolean registration(String login, String password){
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users_tbl ('user_name', 'password') VALUES (?, ?)"
            );
            statement.setString(1, login);
            statement.setString(2, password);
            statement.execute();
            System.out.println("Регистрация прошла успешно");
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    static synchronized void setIsLogin(String login, boolean isLogin) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users_tbl SET isLogin = ? WHERE user_name = ?"
            );
            statement.setInt(1, isLogin ? 1 : 0);
            statement.setString(2, login);
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static synchronized boolean isLogin(String login) {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM users_tbl WHERE user_name = ? AND isLogin = 1"
            );
            statement.setString(1, login);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void resetIsLogin() {
        try {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE users_tbl SET isLogin = 0"
            );
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
