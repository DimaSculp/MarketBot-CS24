package ru.outfix.market.db;

import ru.outfix.market.telegram.UserLinks;

/** Пользователь бота. {@code id} совпадает с id его личного чата с ботом. */
public record UserAccount(long id, String username) {

    /** Ссылка на профиль: {@code https://t.me/<username>} или {@code tg://user?id=...}, если username нет. */
    public String link() {
        return UserLinks.of(id, username);
    }
}
