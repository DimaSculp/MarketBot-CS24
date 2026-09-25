package ru.outfix.market.monitoring;

import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.outfix.market.db.Database;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MonitoringServerTest {

    private final HttpClient http = HttpClient.newHttpClient();
    private Database database;
    private BotMetrics metrics;
    private MonitoringServer server;

    @BeforeEach
    void start() throws Exception {
        database = mock(Database.class);
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        metrics = new BotMetrics(registry);
        server = new MonitoringServer(0, registry, database);
        server.start();
    }

    @AfterEach
    void stop() {
        server.close();
    }

    @Test
    void metricsAreExportedInPrometheusFormat() throws Exception {
        metrics.adEvent(BotMetrics.AdEvent.SUBMITTED);
        metrics.telegramApiError("sendMessage");
        metrics.recordUpdate("message", false, System.nanoTime());

        HttpResponse<String> response = get("/metrics");

        assertEquals(200, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElseThrow().startsWith("text/plain"));
        assertTrue(response.body().contains("outfix_ads_total{event=\"submitted\"} 1.0"), response.body());
        assertTrue(response.body().contains("outfix_telegram_api_errors_total{method=\"sendMessage\"} 1.0"));
        assertTrue(response.body().contains("outfix_updates_seconds_count{outcome=\"ok\",type=\"message\"} 1"));
    }

    @Test
    void healthReflectsDatabase() throws Exception {
        when(database.isHealthy()).thenReturn(true);
        assertEquals(200, get("/health").statusCode());

        when(database.isHealthy()).thenReturn(false);
        HttpResponse<String> down = get("/health");
        assertEquals(503, down.statusCode());
        assertTrue(down.body().contains("DOWN"));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + server.port() + path)).build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
