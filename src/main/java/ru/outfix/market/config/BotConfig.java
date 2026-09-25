package ru.outfix.market.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.function.Function;

/**
 * Настройки бота. Значения берутся из переменных окружения,
 * а при локальном запуске без Docker — из файла {@code .env} в рабочей директории.
 */
public record BotConfig(
        String botToken,
        String botUsername,
        long moderationChannelId,
        long marketChannelId,
        String marketChannelUsername,
        long broadcastChannelId,
        String supportContact,
        String dbUrl,
        String dbUser,
        String dbPassword,
        String yandexGeocoderApiKey,
        int metricsPort
) {

    private static final String DEFAULT_METRICS_PORT = "8081";

    public static BotConfig load() {
        Dotenv env = Dotenv.configure().ignoreIfMissing().load();
        Function<String, String> required = key -> {
            String value = env.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Не задана переменная окружения " + key);
            }
            return value.trim();
        };

        return new BotConfig(
                required.apply("BOT_TOKEN"),
                stripAt(required.apply("BOT_USERNAME")),
                Long.parseLong(required.apply("MODERATION_CHANNEL_ID")),
                Long.parseLong(required.apply("MARKET_CHANNEL_ID")),
                stripAt(required.apply("MARKET_CHANNEL_USERNAME")),
                Long.parseLong(required.apply("BROADCAST_CHANNEL_ID")),
                "@" + stripAt(required.apply("SUPPORT_CONTACT")),
                required.apply("DB_URL"),
                required.apply("DB_USER"),
                required.apply("DB_PASSWORD"),
                env.get("YANDEX_GEOCODER_API_KEY", ""),
                Integer.parseInt(env.get("METRICS_PORT", DEFAULT_METRICS_PORT))
        );
    }

    /** Ссылка на бота, например {@code https://t.me/Market_OutFix_Bot}. */
    public String botLink() {
        return "https://t.me/" + botUsername;
    }

    /** Ссылка на публичный канал с объявлениями. */
    public String marketChannelLink() {
        return "https://t.me/" + marketChannelUsername;
    }

    private static String stripAt(String username) {
        return username.startsWith("@") ? username.substring(1) : username;
    }
}
