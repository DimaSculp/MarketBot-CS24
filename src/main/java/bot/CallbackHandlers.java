package bot;

import bot.Commands.*;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.Map;

public class CallbackHandlers {
    private static DatabaseHandler databaseHandler;

    public CallbackHandlers(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    public void handleCallback(TelegramBot bot, CallbackQuery callbackQuery, Map<String, BotCommands> commandMap) {
        String callbackData = callbackQuery.data();
        long chatId = callbackQuery.message().chat().id();

        switch (callbackData) {
            case "my_profile":
                bot.execute(new SendMessage(chatId, commandMap.get("/profile").getContent()).replyMarkup(commandMap.get("/profile").getKeyboard()));
                bot.execute(new AnswerCallbackQuery(callbackQuery.id()));
                break;
            default:
                break;
        }
    }
}
