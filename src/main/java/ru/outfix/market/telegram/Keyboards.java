package ru.outfix.market.telegram;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import static ru.outfix.market.telegram.CallbackData.*;

/** Инлайн-клавиатуры бота. */
public final class Keyboards {

    private Keyboards() {
    }

    public static InlineKeyboardMarkup start() {
        return new InlineKeyboardMarkup(
                button("🆕Добавить объявление", CREATE_AD)
        ).addRow(
                button("👤Мой профиль", PROFILE),
                button("📜Список команд", HELP)
        );
    }

    public static InlineKeyboardMarkup menu() {
        return new InlineKeyboardMarkup(
                button("🆕Добавить объявление", CREATE_AD)
        ).addRow(
                button("🗃️Мои объявления", MY_ADS),
                button("📜Список команд", HELP)
        );
    }

    public static InlineKeyboardMarkup myAds() {
        return new InlineKeyboardMarkup(
                button("❌Снять объявление", REMOVE_AD)
        ).addRow(
                button("👤Мой профиль", PROFILE),
                button("🆕Добавить объявление", CREATE_AD)
        );
    }

    public static InlineKeyboardMarkup cancelAdCreation() {
        return new InlineKeyboardMarkup(button("Не создавать объявление", CANCEL_AD));
    }

    public static InlineKeyboardMarkup askLocation() {
        return new InlineKeyboardMarkup(
                button("да✅", LOCATION_YES),
                button("нет❌", LOCATION_NO)
        );
    }

    public static InlineKeyboardMarkup finishAdCreation() {
        return new InlineKeyboardMarkup(
                button("Завершить создание✅", FINISH_AD)
        ).addRow(button("Не создавать❌", CANCEL_AD));
    }

    public static InlineKeyboardMarkup confirmRemoval(long adId) {
        return new InlineKeyboardMarkup(
                button("Продано! ✅", SOLD_PREFIX + adId),
                button("Не продано ❌", UNSOLD_PREFIX + adId)
        );
    }

    private static InlineKeyboardButton button(String text, String callbackData) {
        return new InlineKeyboardButton(text).callbackData(callbackData);
    }
}
