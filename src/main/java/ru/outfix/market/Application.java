package ru.outfix.market;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetMe;
import com.pengrad.telegrambot.request.GetUpdates;
import com.pengrad.telegrambot.response.GetMeResponse;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.ad.AdCreationFlow;
import ru.outfix.market.ad.AdFormatter;
import ru.outfix.market.ad.AdRemovalFlow;
import ru.outfix.market.broadcast.BroadcastService;
import ru.outfix.market.command.AuthorsCommand;
import ru.outfix.market.command.CommandRegistry;
import ru.outfix.market.command.HelpCommand;
import ru.outfix.market.command.InfoCommand;
import ru.outfix.market.command.ProfileCommand;
import ru.outfix.market.command.StartCommand;
import ru.outfix.market.config.BotConfig;
import ru.outfix.market.db.AdDraftRepository;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.Database;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.geo.YandexGeocoder;
import ru.outfix.market.moderation.ModerationService;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.monitoring.MonitoringServer;
import ru.outfix.market.telegram.CallbackQueryHandler;
import ru.outfix.market.telegram.MessageHandler;
import ru.outfix.market.telegram.Messenger;
import ru.outfix.market.telegram.UpdateDispatcher;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/** Точка входа: собирает зависимости и запускает long polling. */
public final class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    /** Длительность long polling; должна быть меньше таймаута чтения HTTP-клиента библиотеки (75 с). */
    private static final int LONG_POLLING_TIMEOUT_SECONDS = 50;
    /** Пауза перед повтором getUpdates после ошибки. */
    private static final long POLLING_RETRY_DELAY_MS = 5_000;

    private Application() {
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        BotConfig config = BotConfig.load();

        TelegramBot bot = new TelegramBot.Builder(config.botToken())
                .updateListenerSleep(POLLING_RETRY_DELAY_MS)
                .build();
        verifyToken(bot, config);

        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().commonTags("application", "outfix-market-bot");
        new ClassLoaderMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new JvmGcMetrics().bindTo(registry);
        new JvmThreadMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        new UptimeMetrics().bindTo(registry);
        BotMetrics metrics = new BotMetrics(registry);

        Database database = new Database(config, registry);
        database.migrate();
        UserRepository users = new UserRepository(database.dataSource());
        AdDraftRepository drafts = new AdDraftRepository(database.dataSource());
        AdRepository ads = new AdRepository(database.dataSource());
        metrics.bindDatabaseGauges(database, users, ads);

        MonitoringServer monitoring = new MonitoringServer(config.metricsPort(), registry, database);
        monitoring.start();

        Messenger messenger = new Messenger(bot, metrics);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name("scheduler").daemon().factory());

        AdFormatter formatter = new AdFormatter(config);
        ModerationService moderation = new ModerationService(messenger, ads, users, formatter,
                new YandexGeocoder(config.yandexGeocoderApiKey()), metrics, config);
        AdCreationFlow adCreation = new AdCreationFlow(messenger, users, drafts, moderation, scheduler,
                config.supportContact());
        AdRemovalFlow adRemoval = new AdRemovalFlow(messenger, users, ads, formatter, metrics,
                config.marketChannelId(), config.supportContact());

        CommandRegistry commands = new CommandRegistry();
        ProfileCommand profile = new ProfileCommand(users);
        HelpCommand help = new HelpCommand(commands);
        commands.register(new StartCommand(users))
                .register(profile)
                .register(help)
                .register(new InfoCommand(config.marketChannelUsername()))
                .register(new AuthorsCommand(config.supportContact()));

        UpdateDispatcher dispatcher = new UpdateDispatcher(config,
                new MessageHandler(messenger, commands, adCreation, adRemoval),
                new CallbackQueryHandler(messenger, ads, formatter, adCreation, adRemoval, profile, help),
                moderation,
                new BroadcastService(messenger, users, metrics),
                metrics);

        CountDownLatch stopped = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Остановка бота...");
            bot.removeGetUpdatesListener();
            dispatcher.close();
            scheduler.shutdownNow();
            bot.shutdown();
            monitoring.close();
            database.close();
            stopped.countDown();
        }, "shutdown"));

        GetUpdates polling = new GetUpdates()
                .timeout(LONG_POLLING_TIMEOUT_SECONDS)
                .allowedUpdates("message", "callback_query", "channel_post");
        bot.setUpdatesListener(dispatcher, e -> {
            metrics.pollingError();
            if (e.response() != null) {
                log.error("Ошибка getUpdates: {} {}", e.response().errorCode(), e.response().description());
            } else {
                log.error("Ошибка сети при получении апдейтов", e);
            }
        }, polling);
        log.info("Бот @{} запущен", config.botUsername());
        stopped.await();
    }

    /** Завершает процесс, если токен не принят: иначе бот бесконечно повторял бы getUpdates с ошибкой 401. */
    private static void verifyToken(TelegramBot bot, BotConfig config) {
        GetMeResponse me = bot.execute(new GetMe());
        if (!me.isOk()) {
            log.error("Telegram отклонил токен бота: {} {}", me.errorCode(), me.description());
            bot.shutdown();
            System.exit(1);
        }
        String actual = me.user().username();
        if (!config.botUsername().equalsIgnoreCase(actual)) {
            log.warn("BOT_USERNAME={} не совпадает с username бота @{}: ссылки в объявлениях будут вести не туда",
                    config.botUsername(), actual);
        }
    }
}
