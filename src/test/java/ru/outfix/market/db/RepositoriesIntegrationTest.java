package ru.outfix.market.db;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import ru.outfix.market.TestAds;
import ru.outfix.market.ad.Ad;
import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.ad.AdStatus;
import ru.outfix.market.geo.GeoPoint;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Репозитории и миграции на настоящем PostgreSQL. Без Docker тесты пропускаются
 * (например, при сборке образа на self-hosted раннере; в CI для pull request'ов они выполняются).
 */
@Testcontainers(disabledWithoutDocker = true)
class RepositoriesIntegrationTest {

    private static final long USER = 100L;
    private static final long OTHER_USER = 200L;
    private static final long MODERATION_CHAT = -1001L;

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");

    private static Database database;
    private static UserRepository users;
    private static AdDraftRepository drafts;
    private static AdRepository ads;

    @BeforeAll
    static void migrate() {
        database = new Database(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword(),
                new SimpleMeterRegistry());
        database.migrate();
        users = new UserRepository(database.dataSource());
        drafts = new AdDraftRepository(database.dataSource());
        ads = new AdRepository(database.dataSource());
    }

    @AfterAll
    static void close() {
        database.close();
    }

    @BeforeEach
    void clean() throws SQLException {
        try (Connection conn = database.dataSource().getConnection()) {
            conn.createStatement().execute("TRUNCATE users, ad_drafts, ads, ad_photos, ad_moderation_messages CASCADE");
        }
        users.upsert(USER, "seller");
        users.upsert(OTHER_USER, null);
    }

    @Test
    void upsertReportsCreationAndUpdatesUsername() {
        assertTrue(users.upsert(300L, "first"));
        assertFalse(users.upsert(300L, "renamed"));
        assertEquals(new UserAccount(300L, "renamed"), users.findById(300L).orElseThrow());
        assertEquals(List.of(USER, OTHER_USER, 300L), users.findAllIds());
        assertEquals(3, users.count());
    }

    @Test
    void draftGoesThroughAllSteps() {
        drafts.start(USER);
        assertTrue(drafts.exists(USER));
        assertEquals(AdDraft.Step.TITLE, drafts.find(USER).orElseThrow().step());

        drafts.setTitle(USER, "Кроссовки");
        drafts.setDescription(USER, "Почти новые");
        drafts.setPrice(USER, 0);
        assertFalse(drafts.addPhoto(USER, "early"), "фото принимаются только на шаге PHOTOS");
        drafts.setLocation(USER, new GeoPoint(56.5f, 60.25f));

        AdDraft draft = drafts.find(USER).orElseThrow();
        assertEquals(AdDraft.Step.PHOTOS, draft.step());
        assertEquals("Кроссовки", draft.title());
        assertEquals(0, draft.price());
        assertEquals(new GeoPoint(56.5f, 60.25f), draft.location());
    }

    @Test
    void draftAcceptsAtMostTenPhotos() {
        drafts.start(USER);
        drafts.setLocation(USER, null);
        for (int i = 0; i < 10; i++) {
            assertTrue(drafts.addPhoto(USER, "p" + i));
        }
        assertFalse(drafts.addPhoto(USER, "p10"));
        assertEquals(10, drafts.find(USER).orElseThrow().photoIds().size());
        assertNull(drafts.find(USER).orElseThrow().location());
    }

    @Test
    void restartingDraftResetsIt() {
        drafts.start(USER);
        drafts.setTitle(USER, "old");
        drafts.start(USER);

        AdDraft draft = drafts.find(USER).orElseThrow();
        assertEquals(AdDraft.Step.TITLE, draft.step());
        assertNull(draft.title());
        assertTrue(drafts.delete(USER));
        assertFalse(drafts.exists(USER));
    }

    @Test
    void adIsCreatedWithOrderedPhotos() {
        AdDraft draft = new AdDraft(USER, AdDraft.Step.PHOTOS, "t", "d", 10, new GeoPoint(1.5f, 2.5f),
                List.of("c", "a", "b"));

        Ad ad = ads.create(draft, "Адрес");

        assertEquals(AdStatus.PENDING, ad.status());
        assertEquals(List.of("c", "a", "b"), ad.photoIds());
        assertEquals("Адрес", ad.address());
        assertEquals(new GeoPoint(1.5f, 2.5f), ad.location());
        assertNull(ad.channelMessageId());
    }

    @Test
    void moderationLifecycle() {
        Ad ad = ads.create(TestAds.readyDraft(USER, List.of("p1", "p2")), null);
        ads.addModerationMessages(ad.id(), MODERATION_CHAT, List.of(10, 11));

        assertEquals(ad.id(), ads.findByModerationMessage(MODERATION_CHAT, 11).orElseThrow().id());
        assertTrue(ads.findByModerationMessage(MODERATION_CHAT, 12).isEmpty());
        assertTrue(ads.findByModerationMessage(-999L, 10).isEmpty());

        assertTrue(ads.markRejected(ad.id(), "плохие фото"));
        assertTrue(ads.markPublished(ad.id(), 77), "отклонённое можно одобрить позже");
        assertFalse(ads.markPublished(ad.id(), 78), "повторная публикация невозможна");
        assertFalse(ads.markRejected(ad.id(), "поздно"));

        Ad published = ads.findById(ad.id()).orElseThrow();
        assertEquals(AdStatus.PUBLISHED, published.status());
        assertEquals(77, published.channelMessageId());
    }

    @Test
    void closingAdsAndStats() {
        Ad sold = publish(ads.create(TestAds.readyDraft(USER, List.of("p")), null), 1);
        Ad removed = publish(ads.create(TestAds.readyDraft(USER, List.of("p")), null), 2);
        publish(ads.create(TestAds.readyDraft(USER, List.of("p")), null), 3);
        ads.create(TestAds.readyDraft(USER, List.of("p")), null); // остаётся PENDING

        assertEquals(3, ads.findPublishedByUser(USER).size());
        assertFalse(ads.close(sold.id(), OTHER_USER, AdStatus.SOLD), "чужое объявление снять нельзя");
        assertTrue(ads.close(sold.id(), USER, AdStatus.SOLD));
        assertFalse(ads.close(sold.id(), USER, AdStatus.SOLD), "повторное снятие невозможно");
        assertTrue(ads.close(removed.id(), USER, AdStatus.REMOVED));

        assertEquals(new UserStats(1, 4500), users.stats(USER));
        assertEquals(new UserStats(0, 0), users.stats(OTHER_USER));
        assertEquals(Map.of(AdStatus.PENDING, 1L, AdStatus.PUBLISHED, 1L, AdStatus.REJECTED, 0L,
                AdStatus.SOLD, 1L, AdStatus.REMOVED, 1L), ads.countByStatus());
    }

    @Test
    void closeRejectsNonClosingStatus() {
        assertThrows(IllegalArgumentException.class, () -> ads.close(1, USER, AdStatus.PUBLISHED));
    }

    @Test
    void schemaRejectsInvalidAd() {
        AdDraft tooLong = new AdDraft(USER, AdDraft.Step.PHOTOS, "x".repeat(46), "d", 1, null, List.of("p"));
        assertThrows(DataAccessException.class, () -> ads.create(tooLong, null));
    }

    @Test
    void databaseIsHealthy() {
        assertTrue(database.isHealthy());
    }

    private static Ad publish(Ad ad, int channelMessageId) {
        assertTrue(ads.markPublished(ad.id(), channelMessageId));
        return ads.findById(ad.id()).orElseThrow();
    }
}
