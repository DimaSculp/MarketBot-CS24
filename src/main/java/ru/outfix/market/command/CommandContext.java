package ru.outfix.market.command;

/** Пользователь, вызвавший команду. {@code username} может быть {@code null}. */
public record CommandContext(long userId, String username) {
}
