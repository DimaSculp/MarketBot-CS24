package ru.outfix.market.ad;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.outfix.market.TestAds;
import ru.outfix.market.TestConfig;
import ru.outfix.market.db.AdDraftRepository;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.geo.GeoPoint;
import ru.outfix.market.moderation.ModerationService;
import ru.outfix.market.telegram.Messenger;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.outfix.market.TelegramMocks.*;

class AdCreationFlowTest {

    private static final long CHAT = 100L;

    private Messenger messenger;
    private UserRepository users;
    private AdDraftRepository drafts;
    private ModerationService moderation;
    private ScheduledExecutorService scheduler;
    private AdCreationFlow flow;

    @BeforeEach
    void setUp() {
        messenger = mock(Messenger.class);
        users = mock(UserRepository.class);
        drafts = mock(AdDraftRepository.class);
        moderation = mock(ModerationService.class);
        scheduler = mock(ScheduledExecutorService.class);
        flow = new AdCreationFlow(messenger, users, drafts, moderation, scheduler, TestConfig.SUPPORT);
    }

    private void atStep(AdDraft.Step step) {
        when(drafts.find(CHAT)).thenReturn(Optional.of(TestAds.draftAt(CHAT, step)));
    }

    @Test
    void startRegistersUserAndCreatesDraft() {
        flow.start(CHAT, "test_user");

        verify(users).upsert(CHAT, "test_user");
        verify(drafts).start(CHAT);
        verify(messenger).send(eq(CHAT), eq("Пожалуйста, отправьте название объявления (до 45 символов)."), any());
    }

    @Test
    void validTitleIsSaved() {
        atStep(AdDraft.Step.TITLE);
        flow.handleMessage(message(CHAT, "Кроссовки"));

        verify(drafts).setTitle(CHAT, "Кроссовки");
        verify(messenger).send(CHAT, "Название успешно установлено.");
    }

    @Test
    void tooLongTitleIsRejected() {
        atStep(AdDraft.Step.TITLE);
        flow.handleMessage(message(CHAT, "x".repeat(46)));

        verify(drafts, never()).setTitle(anyLong(), any());
        verify(messenger).send(CHAT, "Ошибка: название превышает 45 символов.");
    }

    @Test
    void photoInsteadOfTitleAsksForText() {
        atStep(AdDraft.Step.TITLE);
        flow.handleMessage(photoMessage(CHAT, "p1"));

        verify(messenger).send(eq(CHAT), eq("Отправьте название объявления текстом."), any());
    }

    @Test
    void priceIsParsedAndValidated() {
        atStep(AdDraft.Step.PRICE);
        flow.handleMessage(message(CHAT, "abc"));
        flow.handleMessage(message(CHAT, "-5"));
        flow.handleMessage(message(CHAT, " 0 "));

        verify(messenger).send(CHAT, "Ошибка: пожалуйста, укажите корректную цену (целое число).");
        verify(messenger).send(CHAT, "Ошибка: цена не может быть отрицательной.");
        verify(drafts, times(1)).setPrice(CHAT, 0);
    }

    @Test
    void locationStepAcceptsOnlyLocation() {
        atStep(AdDraft.Step.LOCATION);
        flow.handleMessage(message(CHAT, "не геолокация"));
        flow.handleMessage(locationMessage(CHAT, 56.8f, 60.6f));

        verify(messenger).send(eq(CHAT), eq("Отправьте геолокацию с помощью встроенной функции телеграм"), any());
        verify(drafts).setLocation(CHAT, new GeoPoint(56.8f, 60.6f));
    }

    @Test
    void declinedLocation() {
        atStep(AdDraft.Step.LOCATION);
        flow.onLocationAnswer(CHAT, false);

        verify(drafts).setLocation(CHAT, null);
    }

    @Test
    void locationAnswerOutsideLocationStepIsIgnored() {
        atStep(AdDraft.Step.PHOTOS);
        flow.onLocationAnswer(CHAT, false);

        verify(drafts, never()).setLocation(anyLong(), any());
    }

    @Test
    void photoSummaryIsScheduledOncePerAlbum() {
        atStep(AdDraft.Step.PHOTOS);
        when(drafts.addPhoto(eq(CHAT), anyString())).thenReturn(true);

        flow.handleMessage(photoMessage(CHAT, "photo1"));
        flow.handleMessage(photoMessage(CHAT, "photo2"));

        verify(drafts).addPhoto(CHAT, "photo1");
        verify(drafts).addPhoto(CHAT, "photo2");
        verify(scheduler, times(1)).schedule(any(Runnable.class), eq(3000L), eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void photoSummaryShowsCountAndFinishButton() {
        atStep(AdDraft.Step.PHOTOS);
        flow.handleMessage(photoMessage(CHAT, "photo1"));
        ArgumentCaptor<Runnable> summary = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).schedule(summary.capture(), anyLong(), any());

        when(drafts.find(CHAT)).thenReturn(Optional.of(TestAds.readyDraft(CHAT, List.of("photo1", "photo2"))));
        summary.getValue().run();

        verify(messenger).send(eq(CHAT), eq("Вы добавили 2 фотографий"), any());
    }

    @Test
    void finishSubmitsAndDeletesDraft() {
        AdDraft draft = TestAds.readyDraft(CHAT, List.of("p1"));
        when(drafts.find(CHAT)).thenReturn(Optional.of(draft));
        when(moderation.submit(draft)).thenReturn(true);

        flow.finish(CHAT);

        verify(drafts).delete(CHAT);
        verify(messenger).send(CHAT, "Ваше объявление отправлено на модерацию! \n\n по техническим вопросам обращайтесь @test_support");
    }

    @Test
    void failedSubmissionKeepsDraft() {
        AdDraft draft = TestAds.readyDraft(CHAT, List.of("p1"));
        when(drafts.find(CHAT)).thenReturn(Optional.of(draft));
        when(moderation.submit(draft)).thenReturn(false);

        flow.finish(CHAT);

        verify(drafts, never()).delete(anyLong());
        verify(messenger).send(eq(CHAT), startsWith("Не удалось отправить объявление на модерацию"), any());
    }

    @Test
    void finishWithoutPhotosIsRefused() {
        when(drafts.find(CHAT)).thenReturn(Optional.of(TestAds.readyDraft(CHAT, List.of())));

        flow.finish(CHAT);

        verifyNoInteractions(moderation);
        verify(messenger).send(eq(CHAT), eq("Черновик объявления не найден. Создайте объявление заново."), any());
    }

    @Test
    void cancelDeletesDraft() {
        when(drafts.delete(CHAT)).thenReturn(true);
        flow.cancel(CHAT);

        verify(drafts).delete(CHAT);
        verify(messenger).send(eq(CHAT), eq("Вы вышли из режима создания объявления."), any());
    }
}
