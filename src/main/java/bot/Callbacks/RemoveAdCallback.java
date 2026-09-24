package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import bot.Keyboards;

import java.util.List;

public class RemoveAdCallback implements BotCallbacks{
    private static final long TARGET_CHANNEL_ID = -1003223929393L;  // Исправил на правильный ID
    DatabaseHandler db;

    public RemoveAdCallback(DatabaseHandler db){
        this.db = db;
    }
    @Override
    public String getContent() {
        return null;
    }

    public void removeAd(long chatId, int number, TelegramBot bot){
        List<String> ads = db.getAdsByChatId(chatId);
        db.removeAdFromUser(chatId, number);
        db.removeAdFromUser(chatId, number);
    }

    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return Keyboards.getStartKeyboard();
    }
}