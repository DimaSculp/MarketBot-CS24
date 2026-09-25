package ru.outfix.market.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import ru.outfix.market.ad.AdStatus;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.Database;
import ru.outfix.market.db.UserRepository;

import java.time.Duration;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Прикладные метрики бота. В Prometheus они видны с префиксом {@code outfix_}
 * (точки в именах Micrometer заменяются на подчёркивания).
 */
public class BotMetrics {

    /** События жизненного цикла объявления для счётчика {@code outfix_ads_total{event=...}}. */
    public enum AdEvent {
        SUBMITTED, SUBMIT_FAILED, APPROVED, PUBLISH_FAILED, REJECTED, SOLD, REMOVED;

        String tag() {
            return name().toLowerCase();
        }
    }

    /** Как часто обновлять значения, требующие запроса в БД. */
    private static final Duration DB_GAUGE_CACHE = Duration.ofSeconds(30);

    private final MeterRegistry registry;
    private final Counter pollingErrors;

    public BotMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.pollingErrors = Counter.builder("outfix.telegram.polling.errors")
                .description("Ошибки getUpdates (сеть или ответ Telegram)")
                .register(registry);
    }

    /** Метрики без экспорта — для тестов. */
    public static BotMetrics noop() {
        return new BotMetrics(new SimpleMeterRegistry());
    }

    public MeterRegistry registry() {
        return registry;
    }

    /** Замер обработки одного апдейта: {@code outfix_updates_seconds{type, outcome}}. */
    public void recordUpdate(String type, boolean failed, long startNanos) {
        Timer.builder("outfix.updates")
                .description("Обработка апдейтов Telegram")
                .tag("type", type)
                .tag("outcome", failed ? "error" : "ok")
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofNanos(System.nanoTime() - startNanos));
    }

    public void telegramApiError(String method) {
        Counter.builder("outfix.telegram.api.errors")
                .description("Неуспешные вызовы Telegram Bot API")
                .tag("method", method)
                .register(registry)
                .increment();
    }

    public void pollingError() {
        pollingErrors.increment();
    }

    public void adEvent(AdEvent event) {
        Counter.builder("outfix.ads")
                .description("События жизненного цикла объявлений")
                .tag("event", event.tag())
                .register(registry)
                .increment();
    }

    public void broadcastMessage(boolean delivered) {
        Counter.builder("outfix.broadcast.messages")
                .description("Сообщения рассылки")
                .tag("result", delivered ? "delivered" : "failed")
                .register(registry)
                .increment();
    }

    /** Показатели, которые берутся из БД: доступность, число пользователей и объявлений по статусам. */
    public void bindDatabaseGauges(Database database, UserRepository users, AdRepository ads) {
        Gauge.builder("outfix.database.up", database, db -> db.isHealthy() ? 1 : 0)
                .description("Доступность PostgreSQL (1 — доступна)")
                .register(registry);

        Supplier<Long> userCount = cached(users::count);
        Gauge.builder("outfix.users", () -> userCount.get())
                .description("Пользователи бота")
                .register(registry);

        Supplier<Map<AdStatus, Long>> adCounts = cached(ads::countByStatus);
        for (AdStatus status : AdStatus.values()) {
            Gauge.builder("outfix.ads.current", () -> {
                        Map<AdStatus, Long> counts = adCounts.get();
                        return counts == null ? Double.NaN : counts.getOrDefault(status, 0L);
                    })
                    .description("Объявления по статусам")
                    .tag("status", status.name().toLowerCase())
                    .register(registry);
        }
    }

    /** Кеширует значение на {@link #DB_GAUGE_CACHE}; при ошибке БД возвращает {@code null} (метрика станет NaN). */
    private static <T> Supplier<T> cached(Supplier<T> loader) {
        return new Supplier<>() {
            private T value;
            private long loadedAt;

            @Override
            public synchronized T get() {
                long now = System.nanoTime();
                if (value == null || now - loadedAt > DB_GAUGE_CACHE.toNanos()) {
                    try {
                        value = loader.get();
                        loadedAt = now;
                    } catch (RuntimeException e) {
                        return null;
                    }
                }
                return value;
            }
        };
    }
}
