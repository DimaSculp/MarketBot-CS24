package ru.outfix.market.telegram;

import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.outfix.market.TestConfig;
import ru.outfix.market.broadcast.BroadcastService;
import ru.outfix.market.moderation.ModerationService;
import ru.outfix.market.monitoring.BotMetrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class UpdateDispatcherTest {

    private MessageHandler messages;
    private CallbackQueryHandler callbacks;
    private ModerationService moderation;
    private BroadcastService broadcast;
    private BotMetrics metrics;
    private UpdateDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        messages = mock(MessageHandler.class);
        callbacks = mock(CallbackQueryHandler.class);
        moderation = mock(ModerationService.class);
        broadcast = mock(BroadcastService.class);
        metrics = BotMetrics.noop();
        dispatcher = new UpdateDispatcher(TestConfig.config(), messages, callbacks, moderation, broadcast, metrics);
    }

    @Test
    void routesChannelPostsByChannel() {
        Message moderationPost = message(TestConfig.MODERATION_CHANNEL, Chat.Type.channel);
        Message broadcastPost = message(TestConfig.BROADCAST_CHANNEL, Chat.Type.channel);
        Message marketPost = message(TestConfig.MARKET_CHANNEL, Chat.Type.channel);

        dispatcher.dispatch(channelPost(moderationPost));
        dispatcher.dispatch(channelPost(broadcastPost));
        dispatcher.dispatch(channelPost(marketPost));

        verify(moderation).handleReply(moderationPost);
        verify(broadcast).broadcast(broadcastPost);
        // Ответы в канале объявлений не считаются решением модератора
        verifyNoMoreInteractions(moderation, broadcast);
        verifyNoInteractions(messages);
    }

    @Test
    void moderationWorksInGroup() {
        // Ответ модератора в группе приходит как обычное сообщение, а не channel_post
        Message reply = message(TestConfig.MODERATION_CHANNEL, Chat.Type.group);

        dispatcher.dispatch(privateOrGroupMessage(reply));

        verify(moderation).handleReply(reply);
        verifyNoInteractions(messages);
    }

    @Test
    void groupMessagesAreNotTreatedAsPrivateOrBroadcast() {
        dispatcher.dispatch(privateOrGroupMessage(message(-42L, Chat.Type.supergroup)));
        // Сообщение участника группы с id канала рассылки не должно уходить всем пользователям
        dispatcher.dispatch(privateOrGroupMessage(message(TestConfig.BROADCAST_CHANNEL, Chat.Type.supergroup)));

        verifyNoInteractions(messages, moderation, broadcast);
    }

    @Test
    void privateMessagesGoToMessageHandler() {
        Message message = message(1L, Chat.Type.Private);
        dispatcher.dispatch(privateOrGroupMessage(message));

        verify(messages).handle(message);
    }

    @Test
    void processesUpdatesOfOneChatInOrder() {
        Message first = message(1L, Chat.Type.Private);
        Message second = message(1L, Chat.Type.Private);
        Update u1 = mock(Update.class);
        Update u2 = mock(Update.class);
        when(u1.message()).thenReturn(first);
        when(u2.message()).thenReturn(second);

        dispatcher.process(java.util.List.of(u1, u2));
        dispatcher.close();

        var order = inOrder(messages);
        order.verify(messages).handle(first);
        order.verify(messages).handle(second);
        assertEquals(2, metrics.registry().find("outfix.updates").tag("type", "message").tag("outcome", "ok").timer().count());
    }

    private static Update channelPost(Message post) {
        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(post);
        return update;
    }

    private static Update privateOrGroupMessage(Message message) {
        Update update = mock(Update.class);
        when(update.message()).thenReturn(message);
        return update;
    }

    private static Message message(long chatId, Chat.Type type) {
        Message message = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(chatId);
        when(chat.type()).thenReturn(type);
        when(message.chat()).thenReturn(chat);
        return message;
    }
}
