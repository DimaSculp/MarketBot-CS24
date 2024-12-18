package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;



import java.util.ArrayList;
import java.util.List;

public class AdCallback implements BotCallbacks {
    private TelegramBot bot;
    private long chatId;
    private String title;
    private String description;
    private int price;
    private List<String> photos;
    private DatabaseHandler databaseHandler;
    private boolean isSendCheckDone = true;
    public AdCallback(TelegramBot bot, DatabaseHandler databaseHandler, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
        this.databaseHandler = databaseHandler;
        this.photos = new ArrayList<>();
    }
    public boolean isSendCheckDone() {
        return isSendCheckDone;
    }

    public void setSendCheckDone() {
        this.isSendCheckDone = false;
    }
    public String setTitle(String title) {
        if (title.length() <= 45) {
            this.title = title;
            return "Название успешно установлено.";
        } else {
            return "Ошибка: название превышает 45 символов.";
        }
    }

    public String setDescription(String description) {
        if (description.length() <= 700) {
            this.description = description;
            return "Описание успешно установлено.";
        } else {
            return "Ошибка: описание превышает 700 символов.";
        }
    }

    public String setPrice(int price) {
        if (price >= 0) {
            this.price = price;
            return "Цена успешно установлена.";
        } else {
            return "Ошибка: цена не может быть отрицательной.";
        }
    }

    public int countPhoto(){
        return photos.size();
    }

    public String addPhoto(String fileId) {
        if (photos.size() < 10) {
            photos.add(fileId);
            System.out.println("+ фотка :" + fileId);
            return "Фото успешно добавлено.";
        } else {
            return "Ошибка: достигнут лимит в 10 фотографий.";
        }
    }
    public boolean isTitleSet() {
        return title != null;
    }
    public boolean isDescriptionSet() {
        return description != null;
    }
    public boolean isPriceSet() {
        return price > 0;
    }
    public boolean isPhotosSet() {
            return photos.size() > 0 && photos.size() <= 10;
    }
    @Override
    public String getContent() {
        String userLink = databaseHandler.getUserById(chatId).getUserLink();
        StringBuilder content = new StringBuilder();
        content.append("<b>").append(title).append("</b>\n\n")
                .append("<i>").append(description).append("</i>").append("\n\n")
                .append("<b>Цена: </b>").append(price).append(" руб.\n\n")
                .append("<b>").append(userLink.replace("https://t.me/", "")).append("</b>\n\n")
                .append("<a href=\"").append(userLink).append(" \" >контакт продовца</a>\n")
                .append("<a href=\"https://t.me/SculpTestShopBot\">разместить объявление</a>")
                .append("~").append(photos);
        return content.toString();
    }
    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return null;
    }
    protected boolean isAdSent = false;
    public void sendAd() {
        if (isAdSent) {
            return;
        }
        isAdSent = true;
        System.out.println("Количество фотографий: " + photos.size());
        InputMediaPhoto[] media = new InputMediaPhoto[photos.size()];
        for (int i = 0; i < photos.size(); i++) {
            media[i] = new InputMediaPhoto(photos.get(i));
            if (i == 0) {
                media[i].caption(getContent());
            }
        }
        long channelId = -1002351079725L;
        try {
            bot.execute(new SendMediaGroup(channelId, media));
            System.out.println("Отправлена медиа группа в канал.");
        } catch (Exception e) {
            System.err.println("Ошибка при отправке медиа-группы в канал: " + e.getMessage());
        }
    }


}
