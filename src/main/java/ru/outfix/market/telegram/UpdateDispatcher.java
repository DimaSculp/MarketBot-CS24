package ru.outfix.market.telegram;

import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import ru.outfix.market.config.BotConfig;
import ru.outfix.market.broadcast.BroadcastService;
import ru.outfix.market.moderation.ModerationService;
import ru.outfix.market.monitoring.BotMetrics;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Принимает апдейты от long polling и раздаёт их обработчикам.
 * Апдейты из разных чатов обрабатываются параллельно на виртуальных потоках,
 * а из одного чата — строго по очереди, чтобы шаги диалога не перемешивались.
 */
public class UpdateDispatcher implements UpdatesListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(UpdateDispatcher.class);

    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(20);

    private final BotConfig config;
    private final MessageHandler messageHandler;
    private final CallbackQueryHandler callbackHandler;
    private final ModerationService moderation;
    private final BroadcastService broadcast;
    private final BotMetrics metrics;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ConcurrentHashMap<Long, CompletableFuture<Void>> chatQueues = new ConcurrentHashMap<>();

    public UpdateDispatcher(BotConfig config, MessageHandler messageHandler, CallbackQueryHandler callbackHandler,
                            ModerationService moderation, BroadcastService broadcast, BotMetrics metrics) {
        this.config = config;
        this.messageHandler = messageHandler;
        this.callbackHandler = callbackHandler;
        this.moderation = moderation;
        this.broadcast = broadcast;
        this.metrics = metrics;
    }

    @Override
    public int process(List<Update> updates) {
        for (Update update : updates) {
            Long chatId = chatId(update);
            if (chatId != null) {
                enqueue(chatId, () -> withLogContext(update, chatId, () -> dispatchMeasured(update)));
            }
        }
        return CONFIRMED_UPDATES_ALL;
    }

    /** Обрабатывает апдейт и записывает метрику {@code outfix_updates_seconds{type, outcome}}. */
    private void dispatchMeasured(Update update) {
        long start = System.nanoTime();
        boolean failed = true;
        try {
            dispatch(update);
            failed = false;
        } finally {
            metrics.recordUpdate(type(update), failed, start);
        }
    }

    void dispatch(Update update) {
        if (update.callbackQuery() != null) {
            log.debug("Нажата кнопка {}", update.callbackQuery().data());
            callbackHandler.handle(update.callbackQuery());
        } else if (update.channelPost() != null || update.message() != null) {
            boolean isChannelPost = update.channelPost() != null;
            Message message = isChannelPost ? update.channelPost() : update.message();
            long chatId = message.chat().id();
            if (chatId == config.moderationChannelId()) {
                // Модерация может идти в канале (channel_post) или в группе (message)
                moderation.handleReply(message);
            } else if (isChannelPost && chatId == config.broadcastChannelId()) {
                // Только посты канала: иначе в группе рассылалось бы любое сообщение участника
                broadcast.broadcast(message);
            } else if (!isChannelPost && message.chat().type() == Chat.Type.Private) {
                log.debug("Входящее сообщение");
                messageHandler.handle(message);
            } else {
                log.debug("Сообщение из чата {} ({}) пропущено", chatId, message.chat().type());
            }
        }
    }

    private void enqueue(long chatId, Runnable task) {
        chatQueues.compute(chatId, (id, tail) -> {
            CompletableFuture<Void> previous = tail != null ? tail : CompletableFuture.completedFuture(null);
            CompletableFuture<Void> next = previous.thenRunAsync(() -> runSafely(task), executor);
            next.whenComplete((ignored, error) -> chatQueues.remove(id, next));
            return next;
        });
    }

    /** Добавляет chatId и updateId в каждую строку лога, записанную во время обработки апдейта. */
    private static void withLogContext(Update update, long chatId, Runnable task) {
        try (var ignoredChat = MDC.putCloseable("chatId", Long.toString(chatId));
             var ignoredUpdate = MDC.putCloseable("updateId", String.valueOf(update.updateId()))) {
            task.run();
        }
    }

    private static void runSafely(Runnable task) {
        try {
            task.run();
        } catch (RuntimeException e) {
            log.error("Необработанная ошибка при обработке апдейта", e);
        }
    }

    private static String type(Update update) {
        if (update.callbackQuery() != null) {
            return "callback_query";
        }
        return update.channelPost() != null ? "channel_post" : "message";
    }

    private static Long chatId(Update update) {
        if (update.callbackQuery() != null) {
            return update.callbackQuery().from().id();
        }
        if (update.channelPost() != null) {
            return update.channelPost().chat().id();
        }
        if (update.message() != null) {
            return update.message().chat().id();
        }
        return null;
    }

    /**
     * Дожидается обработки уже принятых апдейтов и останавливает пул.
     * Перед вызовом нужно остановить long polling, чтобы не поступали новые апдейты.
     */
    @Override
    public void close() {
        long deadline = System.nanoTime() + SHUTDOWN_TIMEOUT.toNanos();
        // Следующая задача чата ставится в пул только после завершения предыдущей,
        // поэтому ждём очереди чатов целиком, а не только уже отправленные задачи
        while (!chatQueues.isEmpty()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                log.warn("Не дождались обработки {} чатов при остановке", chatQueues.size());
                break;
            }
            try {
                CompletableFuture.allOf(chatQueues.values().toArray(CompletableFuture[]::new))
                        .get(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (ExecutionException | TimeoutException ignored) {
                // ошибки задач уже залогированы в runSafely, таймаут проверяется в условии цикла
            }
        }
        executor.close();
    }
}
