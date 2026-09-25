package ru.outfix.market.ad;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageCaption;
import com.pengrad.telegrambot.request.EditMessageText;
import com.pengrad.telegrambot.response.BaseResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.db.AdRepository;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.monitoring.BotMetrics.AdEvent;
import ru.outfix.market.telegram.Keyboards;
import ru.outfix.market.telegram.Messenger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Снятие объявления с публикации: пользователь вводит номер из списка, затем отмечает «продано» или «не продано».
 * Кнопки содержат id объявления, поэтому изменение списка между вводом номера и нажатием ничего не ломает.
 * При продаже подпись поста в канале заменяется на SOLD, цена учитывается в заработке.
 */
public class AdRemovalFlow {

    private static final Logger log = LoggerFactory.getLogger(AdRemovalFlow.class);

    private final Messenger messenger;
    private final UserRepository users;
    private final AdRepository ads;
    private final AdFormatter formatter;
    private final BotMetrics metrics;
    private final long marketChannelId;
    private final String supportContact;
    /** Чаты, ожидающие ввода номера объявления. Короткий диалог, поэтому хранится в памяти. */
    final Set<Long> awaitingNumber = ConcurrentHashMap.newKeySet();

    public AdRemovalFlow(Messenger messenger, UserRepository users, AdRepository ads, AdFormatter formatter,
                         BotMetrics metrics, long marketChannelId, String supportContact) {
        this.messenger = messenger;
        this.users = users;
        this.ads = ads;
        this.formatter = formatter;
        this.metrics = metrics;
        this.marketChannelId = marketChannelId;
        this.supportContact = supportContact;
    }

    public boolean isActive(long chatId) {
        return awaitingNumber.contains(chatId);
    }

    public void start(long chatId) {
        awaitingNumber.add(chatId);
        messenger.send(chatId, "Напишите номер объявления, которое вы хотите снять с публикации");
    }

    public void cancel(long chatId) {
        awaitingNumber.remove(chatId);
    }

    public void handleMessage(Message message) {
        long chatId = message.chat().id();
        List<Ad> published = ads.findPublishedByUser(chatId);
        if (published.isEmpty()) {
            awaitingNumber.remove(chatId);
            messenger.send(chatId, "У вас нет активных объявлений для удаления.", Keyboards.menu());
            return;
        }
        int number;
        try {
            number = Integer.parseInt(message.text() == null ? "" : message.text().strip());
        } catch (NumberFormatException e) {
            messenger.send(chatId, "Ошибка: пожалуйста, отправьте целое число.");
            return;
        }
        if (number < 1 || number > published.size()) {
            messenger.send(chatId, "Ошибка: пожалуйста, введите число от 1 до " + published.size());
            return;
        }
        awaitingNumber.remove(chatId);
        Ad ad = published.get(number - 1);
        String text = "Вы уверены, что хотите снять объявление под номером " + number + " с публикации?\n"
                + (ad.price() > 0 ? "Если товар был продан, вы можете добавить его стоимость (" + ad.price()
                + " руб.) к своему заработку." : "");
        messenger.send(chatId, text, Keyboards.confirmRemoval(ad.id()));
    }

    /** Нажатие «Продано» / «Не продано» под объявлением {@code adId}. */
    public void confirm(long chatId, long adId, boolean sold) {
        Optional<Ad> found = ads.findById(adId).filter(ad -> ad.userId() == chatId);
        // close() атомарен: повторное нажатие или чужое объявление не пройдут условие status = PUBLISHED
        if (found.isEmpty() || !ads.close(adId, chatId, sold ? AdStatus.SOLD : AdStatus.REMOVED)) {
            messenger.send(chatId, "Объявление уже снято с публикации или не найдено.", Keyboards.menu());
            return;
        }
        Ad ad = found.get();
        metrics.adEvent(sold ? AdEvent.SOLD : AdEvent.REMOVED);
        log.info("Объявление {} снято с публикации ({}), цена {}", adId, sold ? "продано" : "не продано", ad.price());
        if (sold) {
            markSoldInChannel(chatId, ad);
        }

        String title = "«" + ad.title() + "»";
        if (sold && ad.price() > 0) {
            messenger.send(chatId, "✅ Объявление " + title + " снято с публикации. Цена " + ad.price()
                    + " руб. добавлена к вашему заработку.");
        } else {
            messenger.send(chatId, "✅ Объявление " + title + " снято с публикации.");
        }
        messenger.send(chatId, "Выберите действие:", Keyboards.menu());
    }

    private void markSoldInChannel(long chatId, Ad ad) {
        UserAccount seller = users.findById(chatId).orElse(new UserAccount(chatId, null));
        String soldText = formatter.soldCaption(seller);
        // channel_message_id — первое сообщение альбома, именно у него есть подпись
        BaseResponse response = ad.photoIds().isEmpty()
                ? messenger.execute(new EditMessageText(marketChannelId, ad.channelMessageId(), soldText)
                        .parseMode(ParseMode.HTML))
                : messenger.execute(new EditMessageCaption(marketChannelId, ad.channelMessageId())
                        .caption(soldText).parseMode(ParseMode.HTML));
        if (!Messenger.isOk(response)) {
            log.warn("Не удалось пометить пост {} как проданный", ad.channelMessageId());
            messenger.send(chatId, "⚠️ Объявление помечено как проданное, но не удалось изменить подпись в канале.\n"
                    + "Ошибка: " + (response == null ? "нет ответа" : response.description()) + "\n"
                    + "Обратитесь к " + supportContact);
        }
    }
}
