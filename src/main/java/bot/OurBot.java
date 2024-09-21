package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import io.github.cdimascio.dotenv.Dotenv;

public class OurBot {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("C:/Users/asus/JavaProject/BotMarket_CS24")
            .load();
    private static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");

    public static void main(String[] args) {
        TelegramBot bot = new TelegramBot(BOT_TOKEN);
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.message() != null) {
                    MessageHandlers.handleMessage(bot, update.message());
                } else if (update.callbackQuery() != null) {
                    CallbackHandlers.handleCallback(bot, update.callbackQuery());
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
}