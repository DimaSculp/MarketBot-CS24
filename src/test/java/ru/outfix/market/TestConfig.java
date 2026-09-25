package ru.outfix.market;

import ru.outfix.market.config.BotConfig;

public final class TestConfig {

    public static final long MODERATION_CHANNEL = -1001L;
    public static final long MARKET_CHANNEL = -1002L;
    public static final long BROADCAST_CHANNEL = -1003L;
    public static final String SUPPORT = "@test_support";

    private TestConfig() {
    }

    public static BotConfig config() {
        return new BotConfig("token", "Market_OutFix_Bot", MODERATION_CHANNEL, MARKET_CHANNEL, "OutFix_Market",
                BROADCAST_CHANNEL, SUPPORT, "jdbc:postgresql://localhost/test", "user", "password", "", 0);
    }
}
