package ru.outfix.market.telegram;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.BaseResponse;
import com.pengrad.telegrambot.response.MessagesResponse;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.monitoring.BotMetrics;

import java.util.Arrays;
import java.util.List;

/** Обёртка над {@link TelegramBot}: логирует неуспешные ответы API и содержит частые операции. */
public class Messenger {

    private static final Logger log = LoggerFactory.getLogger(Messenger.class);

    private final TelegramBot bot;
    private final BotMetrics metrics;

    public Messenger(TelegramBot bot, BotMetrics metrics) {
        this.bot = bot;
        this.metrics = metrics;
    }

    /**
     * Выполняет запрос. Ошибки API и сети не бросаются, а логируются;
     * результат нужно проверять через {@link #isOk(BaseResponse)} ({@code null} при ошибке сети).
     */
    public <T extends BaseRequest<T, R>, R extends BaseResponse> R execute(BaseRequest<T, R> request) {
        R response;
        try {
            response = bot.execute(request);
        } catch (RuntimeException e) {
            // Сетевые ошибки библиотека пробрасывает как RuntimeException
            log.error("Не удалось выполнить {}: ошибка сети", request.getMethod(), e);
            metrics.telegramApiError(request.getMethod());
            return null;
        }
        if (!isOk(response)) {
            metrics.telegramApiError(request.getMethod());
            log.warn("Telegram отклонил {}: {} {}", request.getMethod(),
                    response == null ? "-" : response.errorCode(),
                    response == null ? "нет ответа" : response.description());
        }
        return response;
    }

    public static boolean isOk(BaseResponse response) {
        return response != null && response.isOk();
    }

    public void send(long chatId, String text) {
        execute(new SendMessage(chatId, text));
    }

    public void send(long chatId, String text, InlineKeyboardMarkup keyboard) {
        execute(new SendMessage(chatId, text).replyMarkup(keyboard));
    }

    public void sendHtml(long chatId, String html) {
        execute(new SendMessage(chatId, html).parseMode(ParseMode.HTML));
    }

    public void sendHtml(long chatId, String html, InlineKeyboardMarkup keyboard) {
        execute(new SendMessage(chatId, html).parseMode(ParseMode.HTML).replyMarkup(keyboard));
    }

    /**
     * Публикует объявление: одно фото — через sendPhoto, несколько — альбомом (sendMediaGroup
     * принимает только от 2 до 10 элементов), без фото — обычным сообщением.
     *
     * @return идентификаторы отправленных сообщений (первое — с подписью) или пустой список при ошибке
     */
    public List<Integer> postAd(long chatId, String htmlCaption, List<String> photoIds) {
        if (photoIds.isEmpty()) {
            SendResponse response = execute(new SendMessage(chatId, htmlCaption).parseMode(ParseMode.HTML));
            return isOk(response) ? List.of(response.message().messageId()) : List.of();
        }
        if (photoIds.size() == 1) {
            SendResponse response = execute(new SendPhoto(chatId, photoIds.getFirst())
                    .caption(htmlCaption)
                    .parseMode(ParseMode.HTML));
            return isOk(response) ? List.of(response.message().messageId()) : List.of();
        }
        InputMediaPhoto[] media = new InputMediaPhoto[photoIds.size()];
        for (int i = 0; i < media.length; i++) {
            media[i] = new InputMediaPhoto(photoIds.get(i));
        }
        media[0].caption(htmlCaption).parseMode(ParseMode.HTML);
        MessagesResponse response = execute(new SendMediaGroup(chatId, media));
        if (!isOk(response) || response.messages() == null) {
            return List.of();
        }
        return Arrays.stream(response.messages()).map(Message::messageId).toList();
    }
}
