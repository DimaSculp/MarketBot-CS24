package ru.outfix.market.moderation;

import com.pengrad.telegrambot.model.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.outfix.market.TestAds;
import ru.outfix.market.TestConfig;
import ru.outfix.market.ad.Ad;
import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.ad.AdFormatter;
import ru.outfix.market.ad.AdStatus;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.geo.GeoPoint;
import ru.outfix.market.geo.YandexGeocoder;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.telegram.Messenger;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.outfix.market.TestConfig.*;

class ModerationServiceTest {

    private static final long USER = 555L;
    private static final Ad PENDING = TestAds.ad(1, USER, AdStatus.PENDING, null, List.of("p1", "p2"));

    private Messenger messenger;
    private AdRepository ads;
    private YandexGeocoder geocoder;
    private BotMetrics metrics;
    private ModerationService service;

    @BeforeEach
    void setUp() {
        messenger = mock(Messenger.class);
        ads = mock(AdRepository.class);
        geocoder = mock(YandexGeocoder.class);
        UserRepository users = mock(UserRepository.class);
        when(users.findById(USER)).thenReturn(Optional.of(new UserAccount(USER, "seller")));
        metrics = BotMetrics.noop();
        service = new ModerationService(messenger, ads, users, new AdFormatter(TestConfig.config()), geocoder, metrics,
                TestConfig.config());
    }

    @Test
    void submitCreatesAdAndLinksModerationMessages() {
        AdDraft draft = TestAds.readyDraft(USER, List.of("p1", "p2"));
        when(ads.create(draft, null)).thenReturn(PENDING);
        when(messenger.postAd(eq(MODERATION_CHANNEL), contains("<b>Кроссовки</b>"), eq(List.of("p1", "p2"))))
                .thenReturn(List.of(10, 11));

        assertTrue(service.submit(draft));
        verify(ads).addModerationMessages(1, MODERATION_CHANNEL, List.of(10, 11));
        verifyNoInteractions(geocoder);
        assertEquals(1, counter("submitted"));
    }

    @Test
    void submitGeocodesLocation() {
        GeoPoint point = new GeoPoint(56.5f, 60.25f);
        AdDraft draft = new AdDraft(USER, AdDraft.Step.PHOTOS, "t", "d", 1, point, List.of("p1"));
        when(geocoder.findAddress(point)).thenReturn(Optional.of("Екатеринбург, улица Мира, 19"));
        when(ads.create(draft, "Екатеринбург, улица Мира, 19")).thenReturn(PENDING);
        when(messenger.postAd(anyLong(), anyString(), anyList())).thenReturn(List.of(10));

        assertTrue(service.submit(draft));
    }

    @Test
    void failedSubmitDeletesAd() {
        AdDraft draft = TestAds.readyDraft(USER, List.of("p1"));
        when(ads.create(draft, null)).thenReturn(PENDING);
        when(messenger.postAd(anyLong(), anyString(), anyList())).thenReturn(List.of());

        assertFalse(service.submit(draft));
        verify(ads).delete(1);
        assertEquals(1, counter("submit_failed"));
    }

    @Test
    void approvePublishesAndMarksAd() {
        when(ads.findByModerationMessage(MODERATION_CHANNEL, 11)).thenReturn(Optional.of(PENDING));
        when(messenger.postAd(eq(MARKET_CHANNEL), anyString(), eq(List.of("p1", "p2")))).thenReturn(List.of(77, 78));

        service.handleReply(reply(11, "Approved"));

        verify(ads).markPublished(1, 77);
        verify(messenger).sendHtml(eq(USER), contains("<a href=\"https://t.me/OutFix_Market/77\">ссылке</a>"));
        assertEquals(1, counter("approved"));
    }

    @Test
    void alreadyPublishedAdIsNotPublishedAgain() {
        Ad published = TestAds.ad(1, USER, AdStatus.PUBLISHED, 77, List.of("p1"));
        when(ads.findByModerationMessage(MODERATION_CHANNEL, 10)).thenReturn(Optional.of(published));

        service.handleReply(reply(10, "approved"));

        verify(messenger, never()).postAd(anyLong(), anyString(), anyList());
    }

    @Test
    void failedPublicationKeepsAdPending() {
        when(ads.findByModerationMessage(MODERATION_CHANNEL, 10)).thenReturn(Optional.of(PENDING));
        when(messenger.postAd(anyLong(), anyString(), anyList())).thenReturn(List.of());

        service.handleReply(reply(10, "approved"));

        verify(ads, never()).markPublished(anyLong(), anyInt());
        assertEquals(1, counter("publish_failed"));
    }

    @Test
    void rejectionStoresAndSendsEscapedReason() {
        when(ads.findByModerationMessage(MODERATION_CHANNEL, 10)).thenReturn(Optional.of(PENDING));

        service.handleReply(reply(10, "цена <1000> & нет фото"));

        verify(ads).markRejected(1, "цена <1000> & нет фото");
        verify(messenger).sendHtml(USER, "Ваше объявление отклонено.\nПричина: <b>цена &lt;1000&gt; &amp; нет фото</b>");
    }

    @Test
    void ignoresPostsThatAreNotRepliesToAds() {
        Message post = mock(Message.class);
        when(post.text()).thenReturn("approved");
        service.handleReply(post);

        when(ads.findByModerationMessage(MODERATION_CHANNEL, 99)).thenReturn(Optional.empty());
        service.handleReply(reply(99, "approved"));

        verifyNoInteractions(messenger);
    }

    private double counter(String event) {
        var counter = metrics.registry().find("outfix.ads").tag("event", event).counter();
        return counter == null ? 0 : counter.count();
    }

    private static Message reply(int repliedMessageId, String text) {
        Message replied = mock(Message.class);
        when(replied.messageId()).thenReturn(repliedMessageId);
        Message post = mock(Message.class);
        when(post.replyToMessage()).thenReturn(replied);
        when(post.text()).thenReturn(text);
        when(post.messageId()).thenReturn(500);
        return post;
    }
}
