package bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class Keyboards {
    public static InlineKeyboardMarkup getMainKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Добавить объявление").callbackData("add_ad"),
                new InlineKeyboardButton("Мои объявления").callbackData("my_ads"),
                new InlineKeyboardButton("Мой профиль").callbackData("my_profile")
        );
    }
    public static InlineKeyboardMarkup getToMenuKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Главное меню").callbackData("go_to_menu")
        );
    }
}