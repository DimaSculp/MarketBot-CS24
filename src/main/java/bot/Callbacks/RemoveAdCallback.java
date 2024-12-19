package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import bot.Keyboards;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageCaption;

import java.util.List;

public class RemoveAdCallback implements BotCallbacks{
    private static final long TARGET_CHANNEL_ID = -1002397946078L;
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
        String adLink = ads.get(number - 1);
        String[] parts = adLink.split("/");
        int postMessageId = Integer.parseInt(parts[parts.length - 1]);
        StringBuilder content = new StringBuilder();
        String userLink = db.getUserLinkByChatId(chatId);
        content.append("<b><i>SOLD SOLD SOLD</i></b>\n\n").append("<a href=\"").append(userLink).
                append(" \" >ПРОДАВЕЦ</a>\n").append("<a href=\"https://t.me/KB2024CHANNEL\">МАРКЕТ</a>\n\n");
        EditMessageCaption editMessageText = new EditMessageCaption(TARGET_CHANNEL_ID, postMessageId);
        editMessageText.caption(content.toString());
        editMessageText.parseMode(ParseMode.HTML);
        bot.execute(editMessageText);
        db.removeAdFromUser(chatId, number);
    }

    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return Keyboards.getStartKeyboard();
    }
}
