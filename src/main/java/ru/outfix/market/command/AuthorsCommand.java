package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import ru.outfix.market.telegram.Keyboards;

public class AuthorsCommand implements BotCommand {

    private final String supportContact;

    public AuthorsCommand(String supportContact) {
        this.supportContact = supportContact;
    }

    @Override
    public String name() {
        return "/authors";
    }

    @Override
    public String description() {
        return "Автор проекта";
    }

    @Override
    public String reply(CommandContext context) {
        return "бот написан фиксером " + supportContact;
    }

    @Override
    public InlineKeyboardMarkup keyboard() {
        return Keyboards.menu();
    }
}
