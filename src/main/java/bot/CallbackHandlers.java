package bot;

import bot.Callbacks.AdCallback;
import bot.Commands.BotCommands;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.HashMap;
import java.util.Map;

public class CallbackHandlers {
    private static DatabaseHandler databaseHandler;
    private Map<Long, AdCallback> adCallbacks = new HashMap<>();

    public CallbackHandlers(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    public void handleCallback(TelegramBot bot, CallbackQuery callbackQuery, Map<String, BotCommands> commandMap) {
        String callbackData = callbackQuery.data();
        long chatId = callbackQuery.message().chat().id();
        switch (callbackData) {
            case "add_ad":
                adCallbacks.put(chatId, new AdCallback(bot, databaseHandler, chatId));
                bot.execute(new SendMessage(chatId, "Пожалуйста, отправьте название объявления (до 45 символов)."));
                break;
            default:
                break;
        }
    }

    public void handleMessage(TelegramBot bot, Message message) {
        long chatId = message.chat().id();
        AdCallback adCallback = adCallbacks.get(chatId);
        if (adCallback != null) {
            if (!adCallback.isTitleSet()) {
                String result = adCallback.setTitle(message.text());
                bot.execute(new SendMessage(chatId, result));
                if (result.contains("успешно")) {
                    bot.execute(new SendMessage(chatId, "Отправьте описание объявления (до 700 символов)."));
                }
            } else if (!adCallback.isDescriptionSet()) {
                String result = adCallback.setDescription(message.text());
                bot.execute(new SendMessage(chatId, result));
                if (result.contains("успешно")) {
                    bot.execute(new SendMessage(chatId, "Укажите цену в рублях."));
                }
            } else if (!adCallback.isPriceSet()) {
                try {
                    int price = Integer.parseInt(message.text());
                    String result = adCallback.setPrice(price);
                    bot.execute(new SendMessage(chatId, result));
                    if (result.contains("успешно")) {
                        bot.execute(new SendMessage(chatId, "Теперь отправьте фотографии (до 10 штук)."));
                    }
                } catch (NumberFormatException e) {
                    bot.execute(new SendMessage(chatId, "Ошибка: пожалуйста, укажите корректную цену (целое число)."));
                }
            } else if (adCallback.isPhotosSet()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                PhotoSize[] photos = message.photo();
                if (photos != null && photos.length > 0) {
                    String fileId = photos[photos.length - 1].fileId();
                    System.out.println(fileId);
                    adCallback.addPhoto(fileId);
                    completeAdCreation(bot, chatId);
                } else {
                    bot.execute(new SendMessage(chatId, "Пожалуйста, отправьте фотографии для объявления."));
                }
            }
        }
    }

    public boolean hasActiveAd(long chatId) {
        return adCallbacks.containsKey(chatId);
    }

    private void completeAdCreation(TelegramBot bot, long chatId) {
        AdCallback adCallback = adCallbacks.get(chatId);
        if (adCallback != null) {
            adCallback.sendAd();
            adCallbacks.remove(chatId);
        }
    }
}
