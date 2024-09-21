package bot;

import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class Keyboards {

    public static InlineKeyboardMarkup createInlineKeyboard() {
        InlineKeyboardButton button = new InlineKeyboardButton("Кнопка 1").callbackData("BUTTON_1");

        return new InlineKeyboardMarkup(button);
    }
}