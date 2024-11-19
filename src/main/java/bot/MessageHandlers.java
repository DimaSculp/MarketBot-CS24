package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import bot.Commands.BotCommands;

import java.util.Map;

public class MessageHandlers {
    private final Map<String, BotCommands> commandMap;
    private final CallbackHandlers callbackHandlers;

    public MessageHandlers(DatabaseHandler databaseHandler, Map<String, BotCommands> commandMap, CallbackHandlers callbackHandlers) {
        this.commandMap = commandMap;
        this.callbackHandlers = callbackHandlers;
    }



    public void handleMessage(TelegramBot bot, Message message) {
        String chatId = message.chat().id().toString();
        String text = message.text();
        long userId = message.chat().id();
        String userLink = "https://t.me/" + message.from().username();
        if (callbackHandlers.hasActiveAd(userId)) {
            callbackHandlers.handleMessage(bot, message);
            return;
        }
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
