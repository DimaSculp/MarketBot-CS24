package bot.Callbacks;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import bot.DatabaseHandler;
import bot.Keyboards;

import java.util.List;

public class AddsListCallback implements BotCallbacks{
    List<String> adds;
    DatabaseHandler db;

    public AddsListCallback(DatabaseHandler db) {
        this.db = db;
        this.adds = null;
    }
    @Override
    public String getContent() {
        StringBuilder content = new StringBuilder();
        content.append("<a href=\"https://t.me/OutFix_Market\">МАРКЕТ</a>\n\n");
        content.append("<b>Ваши объявления:</b>\n\n");

        if (adds != null && !adds.isEmpty()) {
            for (int i = 0; i < adds.size(); i++) {
                String rawLink = adds.get(i);
                String cleanLink = extractCleanLink(rawLink);

                content.append("<a href=\"")
                        .append(cleanLink)
                        .append("\">")
                        .append(i + 1)
                        .append(" объявление</a>\n");
            }
        } else {
            content.append("У вас нет активных объявлений.");
        }
        return content.toString();
    }

    private String extractCleanLink(String raw) {
        if (raw == null) return "#";

        int tildeIndex = raw.indexOf("~");
        if (tildeIndex != -1 && tildeIndex + 1 < raw.length()) {
            return raw.substring(tildeIndex + 1);
        }

        return raw;
    }
    public void getAdds(long chatId){
        List<String> ads = db.getAdsByChatId(chatId);
        System.out.println(ads);
        this.adds = ads;
    }
    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return Keyboards.getToEditAddsKeyboard();
    }

}
