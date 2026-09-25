package ru.outfix.market.command;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Реестр команд. Порядок регистрации определяет порядок в {@code /help}. */
public class CommandRegistry {

    private final Map<String, BotCommand> commands = new LinkedHashMap<>();

    public CommandRegistry register(BotCommand command) {
        commands.put(command.name(), command);
        return this;
    }

    /**
     * Ищет команду по тексту сообщения. Аргументы и суффикс {@code @bot_username} игнорируются:
     * {@code "/help@Market_OutFix_Bot"} и {@code "/start payload"} тоже распознаются.
     */
    public Optional<BotCommand> find(String text) {
        if (text == null || !text.startsWith("/")) {
            return Optional.empty();
        }
        String name = text.strip().split("\\s+", 2)[0];
        int at = name.indexOf('@');
        if (at > 0) {
            name = name.substring(0, at);
        }
        return Optional.ofNullable(commands.get(name));
    }

    public Collection<BotCommand> all() {
        return Collections.unmodifiableCollection(commands.values());
    }
}
