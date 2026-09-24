package ru.outfix.market.monitoring;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.db.Database;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP-эндпоинты для мониторинга:
 * {@code /metrics} — метрики в формате Prometheus, {@code /health} — 200, если бот может работать (БД доступна), иначе 503.
 */
public class MonitoringServer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MonitoringServer.class);
    private static final String PROMETHEUS_CONTENT_TYPE = "text/plain; version=0.0.4; charset=utf-8";

    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public MonitoringServer(int port, PrometheusMeterRegistry registry, Database database) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(executor);
        server.createContext("/metrics", exchange -> respond(exchange, 200, PROMETHEUS_CONTENT_TYPE, registry.scrape()));
        server.createContext("/health", exchange -> {
            boolean healthy = database.isHealthy();
            respond(exchange, healthy ? 200 : 503, "application/json",
                    healthy ? "{\"status\":\"UP\"}" : "{\"status\":\"DOWN\",\"database\":\"unavailable\"}");
        });
    }

    public int port() {
        return server.getAddress().getPort();
    }

    public void start() {
        server.start();
        log.info("Мониторинг доступен на порту {}: /metrics, /health", server.getAddress().getPort());
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(1);
        executor.close();
    }
}
