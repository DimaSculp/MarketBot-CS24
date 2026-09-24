package ru.outfix.market.moderation;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ReplyParameters;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.ad.Ad;
import ru.outfix.market.ad.AdDraft;
import ru.outfix.market.ad.AdFormatter;
import ru.outfix.market.config.BotConfig;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.geo.GeoPoint;
import ru.outfix.market.geo.YandexGeocoder;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.monitoring.BotMetrics.AdEvent;
import ru.outfix.market.telegram.Messenger;
import ru.outfix.market.util.Html;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Модерация объявлений. Объявление публикуется в чат модерации (канал или группа);
 * модератор отвечает на любое фото поста:
 * {@code approved} (или {@code approve}) — публикация в канал объявлений, любой другой текст — отказ с этой причиной.
 * Сообщения чата модерации обрабатываются последовательно (см. UpdateDispatcher), поэтому одно объявление
 * не может быть опубликовано дважды.
 */
public class ModerationService {

    private static final Logger log = LoggerFactory.getLogger(ModerationService.class);
    private static final Set<String> APPROVE_WORDS = Set.of("approved", "approve");

    private final Messenger messenger;
    private final AdRepository ads;
    private final UserRepository users;
    private final AdFormatter formatter;
    private final YandexGeocoder geocoder;
    private final BotMetrics metrics;
    private final BotConfig config;

    public ModerationService(Messenger messenger, AdRepository ads, UserRepository users, AdFormatter formatter,
                             YandexGeocoder geocoder, BotMetrics metrics, BotConfig config) {
        this.messenger = messenger;
        this.ads = ads;
        this.users = users;
        this.formatter = formatter;
        this.geocoder = geocoder;
        this.metrics = metrics;
        this.config = config;
    }

    /** Создаёт объявление из черновика и отправляет его в канал модерации. */
    public boolean submit(AdDraft draft) {
        GeoPoint location = draft.location();
        String address = location == null ? null : geocoder.findAddress(location).orElse(null);
        Ad ad = ads.create(draft, address);
        UserAccount seller = users.findById(draft.userId()).orElse(new UserAccount(draft.userId(), null));

        List<Integer> messageIds = messenger.postAd(config.moderationChannelId(), formatter.format(ad, seller), ad.photoIds());
        if (messageIds.isEmpty()) {
            ads.delete(ad.id());
            metrics.adEvent(AdEvent.SUBMIT_FAILED);
            return false;
        }
        ads.addModerationMessages(ad.id(), config.moderationChannelId(), messageIds);
        metrics.adEvent(AdEvent.SUBMITTED);
        log.info("Объявление {} отправлено на модерацию, сообщения {}", ad.id(), messageIds);
        return true;
    }

    /** Обрабатывает сообщение в чате модерации. Учитываются только ответы на посты с объявлениями. */
    public void handleReply(Message post) {
        Message replied = post.replyToMessage();
        String decision = post.text();
        if (replied == null || decision == null) {
            return;
        }
        Optional<Ad> found = ads.findByModerationMessage(config.moderationChannelId(), replied.messageId());
        if (found.isEmpty()) {
            log.info("Ответ модератора на сообщение {}, не относящееся к объявлению", replied.messageId());
            return;
        }
        Ad ad = found.get();
        if (!ad.canBeModerated()) {
            log.info("Объявление {} уже в статусе {}, ответ модератора пропущен", ad.id(), ad.status());
            return;
        }
        if (APPROVE_WORDS.contains(decision.strip().toLowerCase())) {
            approve(ad, post);
        } else {
            reject(ad, decision.strip());
        }
    }

    private void approve(Ad ad, Message moderatorPost) {
        UserAccount seller = users.findById(ad.userId()).orElse(new UserAccount(ad.userId(), null));
        List<Integer> messageIds = messenger.postAd(config.marketChannelId(), formatter.format(ad, seller), ad.photoIds());
        if (messageIds.isEmpty()) {
            metrics.adEvent(AdEvent.PUBLISH_FAILED);
            messenger.execute(new SendMessage(config.moderationChannelId(),
                    "Не удалось опубликовать объявление в канале. Попробуйте ответить approved ещё раз.")
                    .replyParameters(new ReplyParameters(moderatorPost.messageId())));
            return;
        }
        ads.markPublished(ad.id(), messageIds.getFirst());
        metrics.adEvent(AdEvent.APPROVED);

        String link = config.marketChannelLink() + "/" + messageIds.getFirst();
        messenger.sendHtml(ad.userId(), "Ваше объявление опубликовано в канале t.me/" + config.marketChannelUsername() + "!\n"
                + "Теперь оно доступно по <a href=\"" + link + "\">ссылке</a>");
        log.info("Объявление {} опубликовано: {}", ad.id(), link);
    }

    private void reject(Ad ad, String reason) {
        ads.markRejected(ad.id(), reason);
        metrics.adEvent(AdEvent.REJECTED);
        messenger.sendHtml(ad.userId(), "Ваше объявление отклонено.\nПричина: <b>" + Html.escape(reason) + "</b>");
        log.info("Объявление {} отклонено: {}", ad.id(), reason);
    }
}
