package bot;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.HashMap;
import java.util.Map;

public class MessageHandlers {
    
    private static final Map<String, String> commandsMap = new HashMap<>();

    static {
        commandsMap.put("/start", "Команда для начала работы.");
        commandsMap.put("/authors", "Список авторов бота.");
        commandsMap.put("/info", "Краткое описание бота.");
        commandsMap.put("/help", "Список команд.");
    }
    
    public static void handleMessage(TelegramBot bot, Message message) {
        String chatId = message.chat().id().toString();
        String text = message.text();

        if ("/start".equals(text)) {
            bot.execute(new SendMessage(chatId, "Привет! Это стартовое сообщение."));
            bot.
        }

        if ("/authors".equals(text)) {
            bot.execute(new SendMessage(chatId, "Бота создали Бабенко Андрей (EMP_3RR0R) и Кухтей Дмитрий (sculp2ra). Спасибо за интерес!"));
            bot.
        }

        if ("/info".equals(text)) {
            bot.execute(new SendMessage(chatId, "Этот бот создан для удобной публикации и отслеживания объявлений в канале Барахолка УрФУ."));
            bot.
        }

        if ("/help".equals(text)) {
            StringBuilder helpMessage = new StringBuilder("Доступные команды:\n");
            commandsMap.forEach((command, description) -> 
                helpMessage.append(command).append(": ").append(description).append("\n")
            );
            bot.execute(new SendMessage(chatId, helpMessage.toString()));
        }
    }
}
