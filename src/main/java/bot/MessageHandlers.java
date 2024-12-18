package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendLocation;
import com.pengrad.telegrambot.request.SendMessage;
import bot.Commands.BotCommands;

import java.util.Arrays;
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
        String text = "null";
        System.out.println(message.location());
        if(message.location() == null) {
             text = message.text();
        }
        long userId = message.chat().id();
        String userLink = "https://t.me/" + message.from().username();
        if (message.photo() == null && message.location() == null && message.text().startsWith("/start geo_")) {
            String[] parts = message.text().split("_");
            if (parts.length == 5) {
                float latitude = Float.parseFloat(parts[1] + "." + parts[2]);
                float longitude = Float.parseFloat(parts[3] + "." + parts[4]);
                SendLocation locationMessage = new SendLocation(chatId, latitude, longitude);
                bot.execute(locationMessage);
                return;
            }
        }
        if (callbackHandlers.hasActiveAd(userId)) {
            callbackHandlers.handleMessage(bot, message);
            return;
        } else if (callbackHandlers.hasActiveRemove(userId)) {
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


