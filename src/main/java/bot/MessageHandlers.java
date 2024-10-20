package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import bot.Commands.BotCommands;

import java.util.Map;

public class MessageHandlers {
    private final Map<String, BotCommands> commandMap;

    public MessageHandlers(DatabaseHandler databaseHandler, Map<String, BotCommands> commandMap) {
        this.commandMap = commandMap;
    }

    public void handleMessage(TelegramBot bot, Message message) {
        String chatId = message.chat().id().toString();
        String text = message.text();
        long userId = message.chat().id();
        String userLink = "https://t.me/" + message.from().username();
        CommandInitializer.UserID = userId;
        CommandInitializer.userLink = userLink;
        CommandInitializer.updateUserData();
        BotCommands command = commandMap.get(text);
        if (command != null) {
            bot.execute(new SendMessage(chatId, command.getContent()).replyMarkup(command.getKeyboard()));
        } else {
            bot.execute(new SendMessage(chatId, "Неизвестная команда. Введите /help для списка команд."));
        }
    }
}
