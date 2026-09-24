package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import ru.outfix.market.telegram.Keyboards;

public class HelpCommand implements BotCommand {

    private final CommandRegistry registry;

    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String name() {
        return "/help";
    }

    @Override
    public String description() {
        return "Список команд";
    }

    @Override
    public String reply(CommandContext context) {
        StringBuilder sb = new StringBuilder("Доступные команды:\n");
        for (BotCommand command : registry.all()) {
            sb.append(command.name()).append(" - ").append(command.description()).append("\n");
        }
        return sb.toString();
    }

    @Override
    public InlineKeyboardMarkup keyboard() {
        return Keyboards.menu();
    }
}
