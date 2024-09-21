package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;

public class CallbackHandlers {

    public static void handleCallback(TelegramBot bot, CallbackQuery callbackQuery) {
        String callbackData = callbackQuery.data();
        String chatId = callbackQuery.message().chat().id().toString();

        if ("BUTTON_1".equals(callbackData)) {
            bot.execute(new SendMessage(chatId, "Вы нажали кнопку 1!"));
            bot.execute(new AnswerCallbackQuery(callbackQuery.id()).text("Спасибо за нажатие!"));
        }
    }
}