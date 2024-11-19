package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;

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

    public AdCallback(TelegramBot bot, DatabaseHandler databaseHandler, long chatId) {
        this.bot = bot;
        this.chatId = chatId;
        this.databaseHandler = databaseHandler;
        this.photos = new ArrayList<>();
    }

    // Установка названия
    public String setTitle(String title) {
        if (title.length() <= 45) {
            this.title = title;
            return "Название успешно установлено.";
        } else {
            return "Ошибка: название превышает 45 символов.";
        }
    }

    // Установка описания
    public String setDescription(String description) {
        if (description.length() <= 700) {
            this.description = description;
            return "Описание успешно установлено.";
        } else {
            return "Ошибка: описание превышает 700 символов.";
        }
    }

    // Установка цены
    public String setPrice(int price) {
        if (price >= 0) {
            this.price = price;
            return "Цена успешно установлена.";
        } else {
            return "Ошибка: цена не может быть отрицательной.";
        }
    }

    // Добавление фотографии
    public String addPhoto(String fileId) {
        if (photos.size() < 10) {
            photos.add(fileId);
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
        return photos.size() >= 10;
    }

    // Формирование содержимого объявления
    @Override
    public String getContent() {
        // Получаем ссылку на продавца из базы данных
        String userLink = databaseHandler.getUserById(chatId).getUserLink();
        StringBuilder content = new StringBuilder();

        content.append("**").append(title).append("**\n\n") // Название жирным шрифтом
                .append(description).append("\n\n") // Описание
                .append("Цена: ").append(price).append(" руб.\n\n") // Цена
                .append("Продавец: [Перейти к продавцу](").append(userLink).append(")\n\n") // Ссылка на продавца
                .append("[Разместить объявление](https://t.me/SculpTestShopBot)"); // Ссылка на бота

        return content.toString();
    }

    // Создание клавиатуры для объявления
    @Override
    public InlineKeyboardMarkup getKeyboard() {
        return new InlineKeyboardMarkup(); // Здесь можно добавить кнопки, если необходимо
    }

    // Отправка объявления пользователю и в канал
    public void sendAd() {
        // Отправка фотографий в виде отдельных сообщений
        for (String photoId : photos) {
            bot.execute(new SendPhoto(chatId, photoId));
        }

        // Отправка текстового сообщения пользователю
        bot.execute(new SendMessage(chatId, getContent()).replyMarkup(getKeyboard()));

        // Отправка сообщения в канал
        long channelId = -1002351079725L;
        for (String photoId : photos) {
            bot.execute(new SendPhoto(channelId, photoId));
        }
        bot.execute(new SendMessage(channelId, getContent()));
    }
}
