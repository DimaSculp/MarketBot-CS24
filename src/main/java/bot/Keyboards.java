package bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class Keyboards {
    public static InlineKeyboardMarkup getStartKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Добавить объявление").callbackData("to_create") // Первая строка
        ).addRow(
                new InlineKeyboardButton("Мой профиль").callbackData("to_profile"),
                new InlineKeyboardButton("Список команд").callbackData("to_help") // Вторая строка
        );
    }

    public static InlineKeyboardMarkup getToMenuKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Добавить объявление").callbackData("to_create") // Первая строка
        ).addRow(
                new InlineKeyboardButton("Мои объявления").callbackData("to_ads"),
                new InlineKeyboardButton("Список команд").callbackData("to_help") // Вторая строка
        );
    }
}