package ru.outfix.market.ad;

import com.pengrad.telegrambot.request.EditMessageCaption;
import com.pengrad.telegrambot.response.BaseResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.outfix.market.TestAds;
import ru.outfix.market.TestConfig;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.telegram.Messenger;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalMatchers.and;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.outfix.market.TelegramMocks.message;

class AdRemovalFlowTest {

    private static final long CHAT = 100L;
    private static final Ad FIRST = TestAds.ad(7, CHAT, AdStatus.PUBLISHED, 40, List.of("p1", "p2", "p3"));
    private static final Ad SECOND = new Ad(9, CHAT, "Велосипед", "d", 0, null, null, AdStatus.PUBLISHED, 43, List.of("p4"));

    private Messenger messenger;
    private AdRepository ads;
    private AdRemovalFlow flow;

    @BeforeEach
    void setUp() {
        messenger = mock(Messenger.class);
        ads = mock(AdRepository.class);
        UserRepository users = mock(UserRepository.class);
        flow = new AdRemovalFlow(messenger, users, ads, new AdFormatter(TestConfig.config()), BotMetrics.noop(),
                TestConfig.MARKET_CHANNEL, TestConfig.SUPPORT);
        when(ads.findPublishedByUser(CHAT)).thenReturn(List.of(FIRST, SECOND));
        when(ads.findById(7)).thenReturn(Optional.of(FIRST));
        when(ads.findById(9)).thenReturn(Optional.of(SECOND));
        when(users.findById(CHAT)).thenReturn(Optional.of(new UserAccount(CHAT, "seller")));
    }

    @Test
    void asksForConfirmationWithAdIdInButtons() {
        flow.start(CHAT);
        flow.handleMessage(message(CHAT, "1"));

        verify(messenger).send(eq(CHAT), eq("Вы уверены, что хотите снять объявление под номером 1 с публикации?\n"
                + "Если товар был продан, вы можете добавить его стоимость (4500 руб.) к своему заработку."), any());
        assertFalse(flow.isActive(CHAT), "после выбора номера диалог завершён, дальше решают кнопки");
    }

    @Test
    void validatesNumber() {
        flow.start(CHAT);
        flow.handleMessage(message(CHAT, "три"));
        flow.handleMessage(message(CHAT, "5"));

        verify(messenger).send(CHAT, "Ошибка: пожалуйста, отправьте целое число.");
        verify(messenger).send(CHAT, "Ошибка: пожалуйста, введите число от 1 до 2");
        assertTrue(flow.isActive(CHAT));
    }

    @Test
    void soldAdIsClosedAndMarkedInChannel() {
        BaseResponse ok = mock(BaseResponse.class);
        when(ok.isOk()).thenReturn(true);
        when(messenger.execute(any(EditMessageCaption.class))).thenReturn(ok);
        when(ads.close(7, CHAT, AdStatus.SOLD)).thenReturn(true);

        flow.confirm(CHAT, 7, true);

        ArgumentCaptor<EditMessageCaption> edit = ArgumentCaptor.forClass(EditMessageCaption.class);
        verify(messenger).execute(edit.capture());
        assertEquals(40, edit.getValue().getParameters().get("message_id"));
        assertEquals(TestConfig.MARKET_CHANNEL, edit.getValue().getParameters().get("chat_id"));
        assertTrue(((String) edit.getValue().getParameters().get("caption")).contains("https://t.me/seller"));
        verify(messenger).send(CHAT, "✅ Объявление «Кроссовки» снято с публикации. Цена 4500 руб. добавлена к вашему заработку.");
    }

    @Test
    void unsoldAdIsClosedWithoutChannelEdit() {
        when(ads.close(9, CHAT, AdStatus.REMOVED)).thenReturn(true);

        flow.confirm(CHAT, 9, false);

        verify(messenger, never()).execute(any(EditMessageCaption.class));
        verify(messenger).send(CHAT, "✅ Объявление «Велосипед» снято с публикации.");
    }

    @Test
    void alreadyClosedAdIsReported() {
        when(ads.close(7, CHAT, AdStatus.SOLD)).thenReturn(false);

        flow.confirm(CHAT, 7, true);

        verify(messenger, never()).execute(any());
        verify(messenger).send(eq(CHAT), eq("Объявление уже снято с публикации или не найдено."), any());
    }

    @Test
    void foreignAdCannotBeClosed() {
        flow.confirm(999L, 7, true);

        verify(ads, never()).close(anyLong(), anyLong(), any());
    }

    @Test
    void reportsFailedChannelEdit() {
        when(ads.close(7, CHAT, AdStatus.SOLD)).thenReturn(true);

        flow.confirm(CHAT, 7, true);

        verify(messenger).send(eq(CHAT), and(startsWith("⚠️ Объявление помечено как проданное"),
                endsWith("Обратитесь к @test_support")));
    }

    @Test
    void noAdsEndsDialog() {
        when(ads.findPublishedByUser(CHAT)).thenReturn(List.of());
        flow.start(CHAT);
        flow.handleMessage(message(CHAT, "1"));

        assertFalse(flow.isActive(CHAT));
        verify(messenger).send(eq(CHAT), eq("У вас нет активных объявлений для удаления."), any());
    }
}
