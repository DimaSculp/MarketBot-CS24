package bot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;

public class MessageHandlers {

    public static void handleMessage(TelegramBot bot, Message message) {
        String chatId = message.chat().id().toString();
        String text = message.text();

        if ("/start".equals(text)) {
            bot.execute(new SendMessage(chatId, "Привет! Это стартовое сообщение."));
            bot.
        }
    }
}
