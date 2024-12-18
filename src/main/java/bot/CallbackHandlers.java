package bot;

import bot.Callbacks.AdCallback;
import bot.Callbacks.AddsListCallback;
import bot.Commands.BotCommands;
import bot.Commands.ProfileCommand;
import bot.Commands.HelpCommand;
import bot.Callbacks.RemoveAdCallback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.Keyboard;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;

import java.util.HashMap;
import java.util.Map;

public class CallbackHandlers {
    private static DatabaseHandler databaseHandler;
    protected Map<Long, AdCallback> adCallbacks = new HashMap<>();
    protected Map<Long, RemoveAdCallback> removeAd = new HashMap<>();
    public CallbackHandlers(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }
    public void handleCallback(TelegramBot bot, CallbackQuery callbackQuery, Map<String, BotCommands> commandMap) {
        String callbackData = callbackQuery.data();
        long chatId = callbackQuery.message().chat().id();
        AnswerCallbackQuery answer = new AnswerCallbackQuery(callbackQuery.id())
                .showAlert(false);
        switch (callbackData) {
            case "to_create":
                adCallbacks.put(chatId, new AdCallback(bot, databaseHandler, chatId));
                SendMessage sendMessage =new SendMessage(chatId, "Пожалуйста, отправьте название объявления (до 45 символов).");
                sendMessage.replyMarkup(Keyboards.stopCreatingAdd());
                bot.execute(sendMessage);
                bot.execute(answer);
                break;
            case "to_profile":
                ProfileCommand profileCommand = (ProfileCommand) commandMap.get("/profile");
                profileCommand.setUserData(chatId);
                String profileContent = profileCommand.getContent();
                SendMessage message = new SendMessage(chatId, profileContent);
                message.replyMarkup(profileCommand.getKeyboard());
                bot.execute(message);
                bot.execute(answer);
                break;
            case "to_help":
                HelpCommand helpCommand = (HelpCommand) commandMap.get("/help");
                String helpContent = helpCommand.getContent();
                SendMessage helpMessage = new SendMessage(chatId, helpContent);
                helpMessage.replyMarkup(helpCommand.getKeyboard());
                bot.execute(helpMessage);
                bot.execute(answer);
                break;
            case "end_create":
                System.out.println("here");
                completeAdCreation(bot, chatId);
                bot.execute(new SendMessage(chatId, "Ваше объявление отправлено на модерацию! \n\n по техническим вопросам обращайтесь @sculp2ra"));
                bot.execute(answer);
                break;
            case "to_ads":
                AddsListCallback userAdds = new AddsListCallback(databaseHandler);
                userAdds.getAdds(chatId);
                SendMessage userAddsMess = new SendMessage(chatId, userAdds.getContent());
                userAddsMess.replyMarkup(userAdds.getKeyboard());
                bot.execute(userAddsMess.parseMode(ParseMode.HTML));
                bot.execute(answer);
                break;
            case "remove_ads":
                removeAd.put(chatId, new RemoveAdCallback(databaseHandler));
                bot.execute(new SendMessage(chatId, "Напишите номер объяления которое вы хотите снять с публикации"));
                bot.execute(answer);
                break;
            case "stop_creating":
                adCallbacks.remove(chatId);
                SendMessage sendMessage2 = new SendMessage(chatId, "Вы вышли из режима создания объявления.");
                sendMessage2.replyMarkup(Keyboards.getToMenuKeyboard());
                bot.execute(sendMessage2);
                bot.execute(answer);
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
                if (result.contains("успешно")){
                    SendMessage sendMessage = new SendMessage(chatId, "Отправьте описание объявления (до 700 символов).");
                    sendMessage.replyMarkup(Keyboards.stopCreatingAdd());
                    bot.execute(sendMessage);
                }
            } else if (!adCallback.isDescriptionSet()) {
                String result = adCallback.setDescription(message.text());
                bot.execute(new SendMessage(chatId, result));
                if (result.contains("успешно")) {
                    SendMessage sendMessage = new SendMessage(chatId, "Укажите цену в рублях.");
                    sendMessage.replyMarkup(Keyboards.stopCreatingAdd());
                    bot.execute(sendMessage);
                }
            } else if (!adCallback.isPriceSet()) {
                try {
                    int price = Integer.parseInt(message.text());
                    String result = adCallback.setPrice(price);
                    bot.execute(new SendMessage(chatId, result));
                    if (result.contains("успешно")) {
                        SendMessage sendMessage = new SendMessage(chatId, "Теперь отправьте фотографии (до 10 штук).");
                        sendMessage.replyMarkup(Keyboards.stopCreatingAdd());
                        bot.execute(sendMessage);
                    }
                } catch (NumberFormatException e) {
                    bot.execute(new SendMessage(chatId, "Ошибка: пожалуйста, укажите корректную цену (целое число)."));
                }
            } else if (message.photo() != null) {
                    PhotoSize photo = message.photo()[message.photo().length - 1];
                    String fileId = photo.fileId();
                    adCallback.addPhoto(fileId);
                if (adCallback.isPhotosSet() && adCallback.isSendCheckDone()) {
                    adCallback.setSendCheckDone();
                    sendCheck(bot, chatId, adCallback);
                }
            }
        }
        else {
            String number = message.text();
            RemoveAdCallback removeAdCallback = removeAd.get(chatId);
            int size = (databaseHandler.getAdsByChatId(chatId)).size();
            if (size == 0) {
                SendMessage sendMessage = new SendMessage(chatId, "У вас нет активных объявлений для удаления.");
                sendMessage.replyMarkup(removeAdCallback.getKeyboard());
                bot.execute(sendMessage);
                removeAd.remove(chatId);
            } else {
                try {
                    int parsedNumber = Integer.parseInt(number);
                    if (parsedNumber >= 1 && parsedNumber <= size) {
                        SendMessage sendMessage = new SendMessage(chatId, "Объявление под номером " + parsedNumber + " снято с публикации.");
                        sendMessage.replyMarkup(removeAdCallback.getKeyboard());
                        bot.execute(sendMessage);
                        removeAdCallback.removeAd(chatId, parsedNumber, bot);
                        removeAd.remove(chatId);
                    } else {
                        bot.execute(new SendMessage(chatId, "Ошибка: пожалуйста, введите число от 1 до " + size));
                    }
                } catch (NumberFormatException e) {
                    bot.execute(new SendMessage(chatId, "Ошибка: пожалуйста, отправьте целое число."));
                }
            }
        }
    }
    private void sendCheck(TelegramBot bot,long chatId, AdCallback adCallback){
        //System.out.println("tyt" );
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        SendMessage message = new SendMessage(chatId, "Вы добавили " + adCallback.countPhoto() +  " фотографий");
        Keyboard keyB = new InlineKeyboardMarkup(
                new InlineKeyboardButton("Завершить создание✅").callbackData("end_create")
        ).addRow(new InlineKeyboardButton("Не создавть❌").callbackData("stop_creating"));
        message.replyMarkup(keyB);
        bot.execute(message);
    }
    public boolean hasActiveAd(long chatId) {
        return adCallbacks.containsKey(chatId);
    }
    public boolean hasActiveRemove(long chatId){
        return removeAd.containsKey(chatId);
    }
    protected void completeAdCreation(TelegramBot bot, long chatId) {
        AdCallback adCallback = adCallbacks.get(chatId);
        if (adCallback != null) {
            adCallback.sendAd();
            adCallbacks.remove(chatId);
        }
    }
}
