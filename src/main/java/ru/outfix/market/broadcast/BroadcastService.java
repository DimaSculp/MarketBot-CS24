package ru.outfix.market.broadcast;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.ForwardMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.monitoring.BotMetrics;
import ru.outfix.market.telegram.Messenger;

import java.time.Duration;
import java.util.List;

/** Пересылает каждый пост из канала рассылки («Маркет Пуши») всем пользователям бота. */
public class BroadcastService {

    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    /** Telegram ограничивает рассылку ~30 сообщениями в секунду. */
    private static final Duration DELAY_BETWEEN_MESSAGES = Duration.ofMillis(40);

    private final Messenger messenger;
    private final UserRepository users;
    private final BotMetrics metrics;

    public BroadcastService(Messenger messenger, UserRepository users, BotMetrics metrics) {
        this.messenger = messenger;
        this.users = users;
        this.metrics = metrics;
    }

    public void broadcast(Message post) {
        List<Long> userIds = users.findAllIds();
        int delivered = 0;
        for (Long userId : userIds) {
            try {
                boolean ok = Messenger.isOk(messenger.execute(new ForwardMessage(userId, post.chat().id(), post.messageId())));
                metrics.broadcastMessage(ok);
                if (ok) {
                    delivered++;
                }
                Thread.sleep(DELAY_BETWEEN_MESSAGES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (RuntimeException e) {
                log.warn("Не удалось переслать рассылку пользователю {}", userId, e);
            }
        }
        log.info("Рассылка {} доставлена {} из {} пользователей", post.messageId(), delivered, userIds.size());
    }
}
