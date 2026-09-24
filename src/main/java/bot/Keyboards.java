package bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class Keyboards {
    public static InlineKeyboardMarkup getStartKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("🆕Добавить объявление").callbackData("to_create")
        ).addRow(
                new InlineKeyboardButton("👤Мой профиль").callbackData("to_profile"),
                new InlineKeyboardButton("📜Список команд").callbackData("to_help")
        );
    }
    public static InlineKeyboardMarkup getToMenuKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("🆕Добавить объявление").callbackData("to_create")
        ).addRow(
                new InlineKeyboardButton("🗃️Мои объявления").callbackData("to_ads"),
                new InlineKeyboardButton("📜Список команд").callbackData("to_help")
        );
    }
    public static InlineKeyboardMarkup getToEditAddsKeyboard() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("❌Снять объявление").callbackData("remove_ads")
        ).addRow(
                new InlineKeyboardButton("👤Мой профиль").callbackData("to_profile"),
                new InlineKeyboardButton("🆕Добавить объявление").callbackData("to_create")
        );
    }
    public static InlineKeyboardMarkup stopCreatingAdd() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Не создавать объявление").callbackData("stop_creating"));
    }

    public static InlineKeyboardMarkup yesOrNoGeo() {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("да✅").callbackData("yes_geo"),
                new InlineKeyboardButton("нет❌").callbackData("no_geo")
        );
    }

    public static InlineKeyboardMarkup removeAdChoice(int adNumber) {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton("Продано! ✅").callbackData("sold_ad_" + adNumber),
                new InlineKeyboardButton("Не продано ❌").callbackData("unsold_ad_" + adNumber)
        );
    }
}