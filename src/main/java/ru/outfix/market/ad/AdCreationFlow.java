package ru.outfix.market.ad;

import com.pengrad.telegrambot.model.Location;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.PhotoSize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.db.AdDraftRepository;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.geo.GeoPoint;
import ru.outfix.market.moderation.ModerationService;
import ru.outfix.market.telegram.Keyboards;
import ru.outfix.market.telegram.Messenger;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Пошаговое создание объявления: название → описание → цена → место встречи → фото → модерация.
 * Черновик хранится в БД ({@link AdDraftRepository}) и переживает перезапуск бота.
 */
public class AdCreationFlow {

    private static final Logger log = LoggerFactory.getLogger(AdCreationFlow.class);

    /** Сколько ждать остальные фото альбома, прежде чем показать итог. */
    static final Duration PHOTO_ALBUM_WAIT = Duration.ofSeconds(3);

    private final Messenger messenger;
    private final UserRepository users;
    private final AdDraftRepository drafts;
    private final ModerationService moderation;
    private final ScheduledExecutorService scheduler;
    private final String supportContact;
    /** Чаты, для которых итог по фото уже запланирован (показываем один раз на черновик). */
    private final Set<Long> photoSummaryScheduled = ConcurrentHashMap.newKeySet();

    public AdCreationFlow(Messenger messenger, UserRepository users, AdDraftRepository drafts,
                          ModerationService moderation, ScheduledExecutorService scheduler, String supportContact) {
        this.messenger = messenger;
        this.users = users;
        this.drafts = drafts;
        this.moderation = moderation;
        this.scheduler = scheduler;
        this.supportContact = supportContact;
    }

    public boolean isActive(long chatId) {
        return drafts.exists(chatId);
    }

    public void start(long chatId, String username) {
        // Регистрируем пользователя, если он пропустил /start: черновик ссылается на users
        users.upsert(chatId, username);
        drafts.start(chatId);
        photoSummaryScheduled.remove(chatId);
        log.info("Начато создание объявления");
        messenger.send(chatId, "Пожалуйста, отправьте название объявления (до " + AdDraft.MAX_TITLE_LENGTH + " символов).",
                Keyboards.cancelAdCreation());
    }

    public void cancel(long chatId) {
        photoSummaryScheduled.remove(chatId);
        if (drafts.delete(chatId)) {
            log.info("Создание объявления отменено");
        }
        messenger.send(chatId, "Вы вышли из режима создания объявления.", Keyboards.menu());
    }

    public void handleMessage(Message message) {
        long chatId = message.chat().id();
        Optional<AdDraft> found = drafts.find(chatId);
        if (found.isEmpty()) {
            return;
        }
        String text = message.text();
        switch (found.get().step()) {
            case TITLE -> {
                if (text == null) {
                    messenger.send(chatId, "Отправьте название объявления текстом.", Keyboards.cancelAdCreation());
                    return;
                }
                Optional<String> error = AdDraft.validateTitle(text);
                if (error.isPresent()) {
                    messenger.send(chatId, error.get());
                    return;
                }
                drafts.setTitle(chatId, text);
                messenger.send(chatId, "Название успешно установлено.");
                messenger.send(chatId, "Отправьте описание объявления (до " + AdDraft.MAX_DESCRIPTION_LENGTH + " символов).",
                        Keyboards.cancelAdCreation());
            }
            case DESCRIPTION -> {
                if (text == null) {
                    messenger.send(chatId, "Отправьте описание объявления текстом.", Keyboards.cancelAdCreation());
                    return;
                }
                Optional<String> error = AdDraft.validateDescription(text);
                if (error.isPresent()) {
                    messenger.send(chatId, error.get());
                    return;
                }
                drafts.setDescription(chatId, text);
                messenger.send(chatId, "Описание успешно установлено.");
                messenger.send(chatId, "Укажите цену в рублях.", Keyboards.cancelAdCreation());
            }
            case PRICE -> {
                int price;
                try {
                    price = Integer.parseInt(text == null ? "" : text.strip());
                } catch (NumberFormatException e) {
                    messenger.send(chatId, "Ошибка: пожалуйста, укажите корректную цену (целое число).");
                    return;
                }
                Optional<String> error = AdDraft.validatePrice(price);
                if (error.isPresent()) {
                    messenger.send(chatId, error.get());
                    return;
                }
                drafts.setPrice(chatId, price);
                messenger.send(chatId, "Цена успешно установлена.");
                messenger.send(chatId, "Хотите, чтобы в вашем объявлении было указано удобное для Вас место встречи?",
                        Keyboards.askLocation());
            }
            case LOCATION -> {
                Location location = message.location();
                if (location == null) {
                    messenger.send(chatId, "Отправьте геолокацию с помощью встроенной функции телеграм",
                            Keyboards.cancelAdCreation());
                    return;
                }
                drafts.setLocation(chatId, new GeoPoint(location.latitude(), location.longitude()));
                messenger.send(chatId, "Отлично! Геопозиция установлена.\nТеперь отправьте фотографии одним сообщением (до "
                        + AdDraft.MAX_PHOTOS + " штук).", Keyboards.cancelAdCreation());
            }
            case PHOTOS -> {
                PhotoSize[] sizes = message.photo();
                if (sizes == null || sizes.length == 0) {
                    return;
                }
                // Последний размер — самый большой
                if (!drafts.addPhoto(chatId, sizes[sizes.length - 1].fileId())) {
                    log.debug("Фото не добавлено: достигнут лимит {}", AdDraft.MAX_PHOTOS);
                }
                if (photoSummaryScheduled.add(chatId)) {
                    scheduler.schedule(() -> sendPhotoSummary(chatId), PHOTO_ALBUM_WAIT.toMillis(), TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    /** Ответ на вопрос «указать место встречи?». */
    public void onLocationAnswer(long chatId, boolean wantsLocation) {
        Optional<AdDraft> draft = drafts.find(chatId);
        if (draft.isEmpty() || draft.get().step() != AdDraft.Step.LOCATION) {
            return;
        }
        if (wantsLocation) {
            messenger.send(chatId, "Отправьте геопозицию в диалог", Keyboards.cancelAdCreation());
        } else {
            drafts.setLocation(chatId, null);
            messenger.send(chatId, "Хорошо, теперь отправьте фотографии одним сообщением (до " + AdDraft.MAX_PHOTOS + " штук).",
                    Keyboards.cancelAdCreation());
        }
    }

    public void finish(long chatId) {
        Optional<AdDraft> draft = drafts.find(chatId);
        if (draft.isEmpty() || draft.get().step() != AdDraft.Step.PHOTOS || !draft.get().hasPhotos()) {
            log.info("Попытка завершить несуществующий или незаполненный черновик");
            messenger.send(chatId, "Черновик объявления не найден. Создайте объявление заново.", Keyboards.menu());
            return;
        }
        if (moderation.submit(draft.get())) {
            drafts.delete(chatId);
            photoSummaryScheduled.remove(chatId);
            messenger.send(chatId, "Ваше объявление отправлено на модерацию! \n\n по техническим вопросам обращайтесь " + supportContact);
        } else {
            log.warn("Объявление не удалось отправить на модерацию, черновик сохранён для повтора");
            messenger.send(chatId, "Не удалось отправить объявление на модерацию. Попробуйте нажать «Завершить создание» ещё раз"
                    + " или обратитесь к " + supportContact, Keyboards.finishAdCreation());
        }
    }

    private void sendPhotoSummary(long chatId) {
        try {
            drafts.find(chatId)
                    .filter(d -> d.step() == AdDraft.Step.PHOTOS && d.hasPhotos())
                    .ifPresent(d -> messenger.send(chatId, "Вы добавили " + d.photoIds().size() + " фотографий",
                            Keyboards.finishAdCreation()));
        } catch (RuntimeException e) {
            log.error("Не удалось показать итог по фото", e);
        }
    }
}
