package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

/** Текстовая команда бота вида {@code /name}. */
public interface BotCommand {

    /** Имя команды вместе со слэшем, например {@code /start}. */
    String name();

    /** Описание для списка команд. */
    String description();

    /** Текст ответа на команду. */
    String reply(CommandContext context);

    InlineKeyboardMarkup keyboard();
}
