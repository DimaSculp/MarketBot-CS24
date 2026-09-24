package ru.outfix.market.telegram;

public final class UserLinks {

    private UserLinks() {
    }

    /**
     * Ссылка на профиль пользователя. Для пользователей без username используется {@code tg://user?id=...}:
     * ссылка вида {@code https://t.me/null} никуда не ведёт.
     */
    public static String of(long userId, String username) {
        return username != null && !username.isBlank()
                ? "https://t.me/" + username
                : "tg://user?id=" + userId;
    }
}
