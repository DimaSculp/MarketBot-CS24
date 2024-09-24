package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MessageHandlers {

    public static void handleMessage(TelegramBot bot, @NotNull Message message) {
        String chatId = message.chat().id().toString();
        String text = message.text();

        if ("/start".equals(text)) {
            CompletableFuture.runAsync(() ->
                    bot.execute(new SendMessage(chatId, "Привет! Это стартовое сообщение."))
            );
        }

        if ("/authors".equals(text)) {
            CompletableFuture.runAsync(() ->
                    bot.execute(new SendMessage(chatId, "Бота создали Бабенко Андрей (EMP_3RR0R) и Кухтей Дмитрий (sculp2ra). Спасибо за интерес!"))
            );
        }

        if ("/info".equals(text)) {
            CompletableFuture.runAsync(() ->
                    bot.execute(new SendMessage(chatId, "Этот бот создан для удобной публикации и отслеживания объявлений в канале Барахолка УрФУ."))
            );
        }

        if ("/help".equals(text)) {
            CompletableFuture.runAsync(() -> {
                StringBuilder helpMessage = new StringBuilder("Доступные команды:\n");
                CommandMap.getCommandsMap().forEach((command, description) ->
                        helpMessage.append(command).append(": ").append(description).append("\n")
                );
                bot.execute(new SendMessage(chatId, helpMessage.toString()));
            });
        }
    }
}
