package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OurBot {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("C:/Users/asus/JavaProject/BotMarket_CS24")
            .load();
    private static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");
    private static final String BOT_NAME = dotenv.get("BOT_NAME");

    // Создаем фиксированный пул потоков для асинхронной обработки
    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        // Инициализация бота
        TelegramBot bot = new TelegramBot(BOT_TOKEN);

        // Логирование запуска
        System.out.println("Бот " + BOT_NAME + " запущен.");
        System.out.println("Асинхронно работают 10 потоков.");

        // Устанавливаем слушатель для обновлений
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                // Асинхронная обработка сообщений и callback-запросов
                CompletableFuture.runAsync(() -> {
                    if (update.message() != null) {
                        System.out.println("Получено сообщение от чата ID: " + update.message().chat().id() +
                                ". Текст сообщения: " + update.message().text());
                        MessageHandlers.handleMessage(bot, update.message());
                    } else if (update.callbackQuery() != null) {
                        System.out.println("Пользователь из чата ID: " + update.callbackQuery().from().id() +
                                " нажал на кнопку.");
                        CallbackHandlers.handleCallback(bot, update.callbackQuery());
                    }
                }, executor);
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}
