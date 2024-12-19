package bot;

import bot.Commands.BotCommands;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Location;
import com.pengrad.telegrambot.model.Update;
import io.github.cdimascio.dotenv.Dotenv;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OurBot {

    private static final Dotenv dotenv = Dotenv.configure()
            .directory("C:/Users/asus/JavaProject/BotMarket_CS24")
            .load();
    private static final String BOT_TOKEN = dotenv.get("BOT_TOKEN");
    private static final String BOT_NAME = dotenv.get("BOT_NAME");

    private static final ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        TelegramBot bot = new TelegramBot(BOT_TOKEN);
        System.out.println("Бот " + BOT_NAME + " запущен.");
        System.out.println("Асинхронно работают 10 потоков.");

        DatabaseHandler databaseHandler = new DatabaseHandler();
        Map<String, BotCommands> commandMap = CommandInitializer.initializeCommands(databaseHandler);
        CommandInitializer.databaseHandler = databaseHandler;
        CallbackHandlers callbackHandlers = new CallbackHandlers(databaseHandler);
        MessageHandlers messageHandlers = new MessageHandlers(databaseHandler, commandMap, callbackHandlers);
        ModerationHandler moderationHandler = new ModerationHandler(bot, databaseHandler);
        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                CompletableFuture.runAsync(() -> handleUpdate(bot, update, commandMap, messageHandlers, callbackHandlers, moderationHandler), executor);
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private static void handleUpdate(TelegramBot bot, Update update, Map<String, BotCommands> commandMap, MessageHandlers messageHandlers, CallbackHandlers callbackHandlers,  ModerationHandler moderationHandler) {
        if (update.callbackQuery() != null) {
            System.out.println("Пользователь из чата ID: " + update.callbackQuery().from().id() +
                    " нажал на кнопку.");
            callbackHandlers.handleCallback(bot, update.callbackQuery(), commandMap);
        } else if (update.channelPost() != null) {
            System.out.println("Обнаружено сообщение в канале ID: " + update.channelPost().chat().id());
            moderationHandler.handleUpdate(update);
        }else if ( update.message().location() != null || update.message() != null) {
            System.out.println("Получено сообщение от чата ID: " + update.message().chat().id() +
                    ". Текст сообщения: " + update.message());
            messageHandlers.handleMessage(bot, update.message());
        }
        }
    }

