package ru.outfix.market.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.config.BotConfig;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** Пул соединений с PostgreSQL и применение миграций схемы. */
public final class Database implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static final int HEALTH_CHECK_TIMEOUT_SECONDS = 2;

    private final HikariDataSource dataSource;

    public Database(BotConfig config, MeterRegistry registry) {
        this(config.dbUrl(), config.dbUser(), config.dbPassword(), registry);
    }

    public Database(String url, String user, String password, MeterRegistry registry) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(url);
        hikari.setUsername(user);
        hikari.setPassword(password);
        hikari.setPoolName("outfix-db");
        hikari.setMaximumPoolSize(10);
        hikari.setMinimumIdle(2);
        hikari.setKeepaliveTime(300_000);
        // Короткий таймаут: при недоступной БД /health и обработчики быстро получают ошибку, а не висят 30 с
        hikari.setConnectionTimeout(5_000);
        hikari.setMetricsTrackerFactory(new MicrometerMetricsTrackerFactory(registry));
        this.dataSource = new HikariDataSource(hikari);
    }

    /** Применяет миграции из {@code db/migration}. */
    public void migrate() {
        var result = Flyway.configure()
                .dataSource(dataSource)
                .load()
                .migrate();
        log.info("Схема БД актуальна, применено миграций: {}", result.migrationsExecuted);
    }

    public boolean isHealthy() {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(HEALTH_CHECK_TIMEOUT_SECONDS);
        } catch (SQLException e) {
            return false;
        }
    }

    public DataSource dataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        dataSource.close();
    }
}
