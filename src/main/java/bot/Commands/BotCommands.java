package bot.Commands;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public interface BotCommands {
    String getDescription();
    String getContent();
    String getCommand();
    InlineKeyboardMarkup getKeyboard();
}