package bot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseHandler {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory(".")
            .load();
    private static final String URL = dotenv.get("URL_DB");
    private static final String USER = dotenv.get("USER_DB");
    private static final String PASSWORD = dotenv.get("PASSWORD_DB");

    private static HikariDataSource dataSource;

    public DatabaseHandler() {
        if (dataSource == null) {
            initializeDataSource();
        }
    }

    private static synchronized void initializeDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(URL);
            config.setUsername(USER);
            config.setPassword(PASSWORD);

            // Настройки пула соединений для долгоработающего приложения
            config.setMaximumPoolSize(10); // Максимум 10 соединений
            config.setMinimumIdle(2); // Минимум 2 простаивающих соединения
            config.setConnectionTimeout(30000); // 30 секунд на получение соединения
            config.setIdleTimeout(600000); // 10 минут простоя перед закрытием
            config.setMaxLifetime(1800000); // 30 минут максимальное время жизни соединения
            config.setKeepaliveTime(300000); // 5 минут - проверка живости соединения
            config.setConnectionTestQuery("SELECT 1"); // Проверка соединения

            dataSource = new HikariDataSource(config);
            System.out.println("База данных: " + URL + " успешно подключена через HikariCP.");
            System.out.println("Connection pool создан. Макс. соединений: " + config.getMaximumPoolSize());
        }
    }

    // Метод для получения соединения из пула
    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void addUser(User user) {
        String insertUserSQL = "INSERT INTO public.users (user_id, user_link, active_ads_count, earned_money, active_ads) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT (user_id) DO UPDATE SET user_link = EXCLUDED.user_link, " +
                "active_ads_count = EXCLUDED.active_ads_count, earned_money = EXCLUDED.earned_money, " +
                "active_ads = EXCLUDED.active_ads";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(insertUserSQL)) {
            statement.setLong(1, user.getUserId());
            statement.setString(2, user.getUserLink());
            statement.setInt(3, user.getActiveAdsCount());
            statement.setInt(4, user.getEarnedMoney());
            statement.setArray(5, conn.createArrayOf("text", user.getActiveAds()));
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User getUserById(long userId) {
        String selectUserSQL = "SELECT * FROM public.users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(selectUserSQL)) {
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

    public long findUserIdByUserlink(String userlink) {
        String query = "SELECT user_id FROM public.users WHERE user_link = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setString(1, userlink);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getLong("user_id");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean addAdToUser(long userId, String adLink) {
        String updateAdSQL = "UPDATE public.users " +
                "SET active_ads = array_append(active_ads, ?), " +
                "    active_ads_count = active_ads_count + 1 " +
                "WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(updateAdSQL)) {
            statement.setString(1, adLink);
            statement.setLong(2, userId);
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<String> getAdsByChatId(long chatId) {
        List<String> ads = new ArrayList<>();
        String query = "SELECT active_ads FROM public.users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String[] activeAds = (String[]) resultSet.getArray("active_ads").getArray();
                for (String ad : activeAds) {
                    ads.add(ad);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ads;
    }

    public void removeAdFromUser(long userId, int number) {
        List<String> ads = getAdsByChatId(userId);
        if (number < 1 || number > ads.size()) {
            System.out.println("Ошибка: Неверный номер объявления.");
            return;
        }
        ads.remove(number - 1);
        String updateAdSQL = "UPDATE public.users " +
                "SET active_ads = ? , active_ads_count = active_ads_count - 1 " +
                "WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(updateAdSQL)) {
            statement.setArray(1, conn.createArrayOf("text", ads.toArray()));
            statement.setLong(2, userId);
            statement.executeUpdate();
            System.out.println("Объявление удалено успешно.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка при удалении объявления.");
        }
    }

    private String extractAdTitle(String adContent) {
        Pattern pattern = Pattern.compile("<b>(.*?)</b>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(adContent);

        if (matcher.find()) {
            String title = matcher.group(1).trim();
            return title.split("\n")[0].trim();
        }

        String[] lines = adContent.split("\n");
        return lines.length > 0 ? lines[0].trim() : "Объявление";
    }

    public String getFormattedAdsList(long chatId) {
        List<String> rawAds = getAdsByChatId(chatId);
        if (rawAds == null || rawAds.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("<b>Список активных объявлений:</b>\n\n");

        for (int i = 0; i < rawAds.size(); i++) {
            String rawAd = rawAds.get(i);
            System.out.println("DEBUG: Raw ad from DB: " + rawAd);

            int lastTildeIndex = rawAd.lastIndexOf('~');

            String adContent;
            String adLink;

            if (lastTildeIndex != -1) {
                adContent = rawAd.substring(0, lastTildeIndex);
                adLink = rawAd.substring(lastTildeIndex + 1).trim();
            } else {
                adContent = rawAd;
                adLink = "#";
            }

            System.out.println("DEBUG: Extracted Ad Link: " + adLink);

            String title = extractAdTitle(adContent);

            sb.append(i + 1).append(") ")
                    .append("<a href=\"").append(adLink).append("\">").append(title).append("</a>\n");
        }


        System.out.println("DEBUG: Final HTML output: " + sb.toString());

        return sb.toString();
    }

    public void removeAd(long userId, int number) {
        List<String> ads = getAdsByChatId(userId);

        if (number < 1 || number > ads.size()) {
            System.out.println("Ошибка: Неверный номер объявления для удаления.");
            return;
        }

        ads.remove(number - 1);
        String updateAdSQL = "UPDATE public.users " +
                "SET active_ads = ? , active_ads_count = active_ads_count - 1 " +
                "WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(updateAdSQL)) {
            statement.setArray(1, conn.createArrayOf("text", ads.toArray()));
            statement.setLong(2, userId);
            statement.executeUpdate();
            System.out.println("Объявление удалено успешно.");
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Ошибка при удалении объявления.");
        }
    }

    public String getUserLinkByChatId(long chatId) {
        String userLink = null;
        String query = "SELECT user_link FROM public.users WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            statement.setLong(1, chatId);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                userLink = resultSet.getString("user_link");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userLink;
    }

    public List<Long> getAllUserIds() {
        List<Long> userIds = new ArrayList<>();
        String query = "SELECT user_id FROM public.users";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                userIds.add(resultSet.getLong("user_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return userIds;
    }

    public void addEarnings(long userId, int amount) {
        String updateEarningsSQL = "UPDATE public.users " +
                "SET earned_money = earned_money + ? " +
                "WHERE user_id = ?";
        try (Connection conn = getConnection();
             PreparedStatement statement = conn.prepareStatement(updateEarningsSQL)) {
            statement.setInt(1, amount);
            statement.setLong(2, userId);
            statement.executeUpdate();
            System.out.println("Добавлено " + amount + " к заработку пользователя " + userId);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getAdPrice(long userId, int adNumber) {
        List<String> ads = getAdsByChatId(userId);
        if (adNumber > 0 && adNumber <= ads.size()) {
            String adString = ads.get(adNumber - 1);

            Pattern pattern = Pattern.compile("Цена:\\s*(\\d+)\\s*руб\\.", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(adString);

            if (matcher.find()) {
                try {
                    String priceStr = matcher.group(1);
                    return Integer.parseInt(priceStr);
                } catch (NumberFormatException e) {
                    System.out.println("Ошибка при парсинге цены из объявления: " + e.getMessage());
                }
            }
        }
        return 0;
    }

    public String getRawAdByNumber(long userId, int number) {
        List<String> ads = getAdsByChatId(userId);
        if (number < 1 || ads == null || number > ads.size()) {
            return null;
        }
        return ads.get(number - 1);
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("Connection pool закрыт.");
        }
    }
}