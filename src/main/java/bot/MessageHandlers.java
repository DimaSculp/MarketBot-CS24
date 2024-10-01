package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import bot.Commands.BotCommands;
import bot.Commands.ProfileCommand;
import bot.Commands.StartCommand;

import java.util.Map;

public class MessageHandlers {

    private static Map<String, BotCommands> commandMap;
    private static DatabaseHandler databaseHandler;

    public MessageHandlers(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    public static void handleMessage(TelegramBot bot, Message message, Map<String, BotCommands> commandMap) {
        String chatId = message.chat().id().toString();
        String text = message.text();
        long userId = message.chat().id();
        String userLink = "https://t.me/" + message.from().username();

        BotCommands command = commandMap.get(text);

        if ("/start".equals(text)) {
            command = new StartCommand(databaseHandler, userId, userLink);
        }

        if ("/profile".equals(text)) {
            command = new ProfileCommand(databaseHandler, userId);
        }

        if (command != null) {
            bot.execute(new SendMessage(chatId, command.getContent()).replyMarkup(command.getKeyboard()));
        } else {
            bot.execute(new SendMessage(chatId, "Неизвестная команда. Введите /help для списка команд."));
        }
    }
    public static Map<String, BotCommands> getCommandsMap() {
        return commandMap;
    }
}
