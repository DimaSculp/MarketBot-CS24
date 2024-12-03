package bot;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseHandler {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("C:/Users/asus/JavaProject/BotMarket_CS24")
            .load();
    private static final String URL = dotenv.get("URL_DB");
    private static final String USER = dotenv.get("USER_DB");
    private static final String PASSWORD = dotenv.get("PASSWORD_DB");

    private Connection connection;

    public Connection getConnection() {
        return connection;
    }

    public DatabaseHandler() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("База данных: " + URL + " успешно подключена.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addUser(User user) {
        String insertUserSQL = "INSERT INTO public.users (user_id, user_link, active_ads_count, earned_money, active_ads) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id) DO UPDATE SET user_link = EXCLUDED.user_link, " +
                "active_ads_count = EXCLUDED.active_ads_count, earned_money = EXCLUDED.earned_money, " +
                "active_ads = EXCLUDED.active_ads";
        try (PreparedStatement statement = connection.prepareStatement(insertUserSQL)) {
            statement.setLong(1, user.getUserId());
            statement.setString(2, user.getUserLink());
            statement.setInt(3, user.getActiveAdsCount());
            statement.setInt(4, user.getEarnedMoney());
            statement.setArray(5, connection.createArrayOf("text", user.getActiveAds()));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUserById(long userId) {
        String selectUserSQL = "SELECT * FROM public.users WHERE user_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(selectUserSQL)) {
            statement.setLong(1, userId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String[] activeAds = (String[]) resultSet.getArray("active_ads").getArray();
                return new User(
                        resultSet.getLong("user_id"),
                        resultSet.getString("user_link"),
                        activeAds,
                        resultSet.getInt("active_ads_count"),
                        resultSet.getInt("earned_money")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }



    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
