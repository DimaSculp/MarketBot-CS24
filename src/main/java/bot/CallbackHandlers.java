package bot;

import bot.Callbacks.AdCallback;
import bot.Commands.BotCommands;
import bot.Commands.ProfileCommand;
import bot.Commands.HelpCommand;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.HashMap;
import java.util.Map;

public class CallbackHandlers {
    private static DatabaseHandler databaseHandler;
    protected Map<Long, AdCallback> adCallbacks = new HashMap<>();

    public CallbackHandlers(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    public void handleCallback(TelegramBot bot, CallbackQuery callbackQuery, Map<String, BotCommands> commandMap) {
        String callbackData = callbackQuery.data();
        long chatId = callbackQuery.message().chat().id();

        switch (callbackData) {
            case "to_create":
                adCallbacks.put(chatId, new AdCallback(bot, databaseHandler, chatId));
                bot.execute(new SendMessage(chatId, "Пожалуйста, отправьте название объявления (до 45 символов)."));
                break;
            case "to_profile":
                ProfileCommand profileCommand = (ProfileCommand) commandMap.get("/profile");
                profileCommand.setUserData(chatId);
                String profileContent = profileCommand.getContent();
                SendMessage message = new SendMessage(chatId, profileContent);
                message.replyMarkup(profileCommand.getKeyboard());
                bot.execute(message);
                break;
            case "to_help":
                HelpCommand helpCommand = (HelpCommand) commandMap.get("/help");
                String helpContent = helpCommand.getContent();
                SendMessage helpMessage = new SendMessage(chatId, helpContent);
                bot.execute(helpMessage);
                break;
            case "end_create":
                System.out.println("here");
                completeAdCreation(bot, chatId);
                bot.execute(new SendMessage(chatId, "Ваше объявление отправлено на модерацию! \n\n по техническим вопросам обращайтесь @sculp2ra"));
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
            } else if (message.photo() != null) {
                    PhotoSize photo = message.photo()[message.photo().length - 1];
                    String fileId = photo.fileId();
                    adCallback.addPhoto(fileId);
                    if (isSent){
                    sendCheck(bot, chatId, adCallback);
                }
            }
        }
    }


    private boolean isSent = true;
    private void sendCheck(TelegramBot bot,long chatId, AdCallback adCallback){
        isSent = false;
        System.out.println("tyt" );
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SendMessage message = new SendMessage(chatId, "Вы добавили " + adCallback.countPhoto() +  " фотографий");
        Keyboard keyB = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Завершить создание").callbackData("end_create")
        );
        message.replyMarkup(keyB);
        bot.execute(message);
    }

    public boolean hasActiveAd(long chatId) {
        return adCallbacks.containsKey(chatId);
    }

    protected void completeAdCreation(TelegramBot bot, long chatId) {
        AdCallback adCallback = adCallbacks.get(chatId);
        if (adCallback != null) {
            adCallback.sendAd();
            adCallbacks.remove(chatId);
        }
    }
}
