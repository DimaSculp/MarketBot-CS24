package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import ru.outfix.market.telegram.Keyboards;

public class InfoCommand implements BotCommand {

    private final String marketChannelUsername;

    public InfoCommand(String marketChannelUsername) {
        this.marketChannelUsername = marketChannelUsername;
    }

    @Override
    public String name() {
        return "/info";
    }

    @Override
    public String description() {
        return "Краткое описание бота.";
    }

    @Override
    public String reply(CommandContext context) {
        return "Этот бот создан для удобной публикации и отслеживания объявлений в барахолке Аутфикса @"
                + marketChannelUsername + ". Написан на Java 25 + java-telegram-bot-api";
    }

    @Override
    public InlineKeyboardMarkup keyboard() {
        return Keyboards.menu();
    }
}
