package bot.Callbacks;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public interface BotCallbacks {
    String getContent();
    InlineKeyboardMarkup getKeyboard();
}
