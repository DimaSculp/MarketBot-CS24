package ru.outfix.market.telegram;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendLocation;
import ru.outfix.market.ad.AdCreationFlow;
import ru.outfix.market.ad.AdRemovalFlow;
import ru.outfix.market.command.BotCommand;
import ru.outfix.market.command.CommandContext;
import ru.outfix.market.command.CommandRegistry;
import ru.outfix.market.geo.GeoPoint;

import java.util.Optional;

/** Обработка входящих сообщений в личном чате с ботом. */
public class MessageHandler {

    private static final String START_WITH_PAYLOAD = "/start ";

    private final Messenger messenger;
    private final CommandRegistry commands;
    private final AdCreationFlow adCreation;
    private final AdRemovalFlow adRemoval;

    public MessageHandler(Messenger messenger, CommandRegistry commands, AdCreationFlow adCreation, AdRemovalFlow adRemoval) {
        this.messenger = messenger;
        this.commands = commands;
        this.adCreation = adCreation;
        this.adRemoval = adRemoval;
    }

    public void handle(Message message) {
        long chatId = message.chat().id();
        String text = message.text();

        // Переход по ссылке «Место» из объявления: /start geo_56_838011_60_597465
        if (text != null && text.startsWith(START_WITH_PAYLOAD)) {
            Optional<GeoPoint> point = GeoPoint.fromStartPayload(text.substring(START_WITH_PAYLOAD.length()).strip());
            if (point.isPresent()) {
                messenger.execute(new SendLocation(chatId, point.get().latitude(), point.get().longitude()));
                return;
            }
        }

        if (adCreation.isActive(chatId)) {
            adCreation.handleMessage(message);
            return;
        }
        if (adRemoval.isActive(chatId)) {
            // У диалога снятия нет кнопки отмены, поэтому любая команда из него выходит
            if (text == null || !text.startsWith("/")) {
                adRemoval.handleMessage(message);
                return;
            }
            adRemoval.cancel(chatId);
        }

        Optional<BotCommand> command = commands.find(text);
        if (command.isEmpty()) {
            messenger.send(chatId, "Неизвестная команда. Введите /help для списка команд.");
            return;
        }
        String username = message.from() != null ? message.from().username() : null;
        BotCommand cmd = command.get();
        messenger.send(chatId, cmd.reply(new CommandContext(chatId, username)), cmd.keyboard());
    }
}
