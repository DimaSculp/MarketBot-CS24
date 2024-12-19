package bot;

import bot.Callbacks.AdCallback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CallbackHandlersTest {
    private TelegramBot botMock;
    private CallbackHandlers callbackHandlers;

    @BeforeEach
    void setUp() {
        botMock = mock(TelegramBot.class);
        DatabaseHandler dbHandlerMock = mock(DatabaseHandler.class);
        callbackHandlers = new CallbackHandlers(dbHandlerMock);
    }

    @Test
    void testHandleCallback() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.data()).thenReturn("to_create");

        Message message = mock(Message.class);
        Chat chatMock = mock(Chat.class);
        when(message.chat()).thenReturn(chatMock);
        when(chatMock.id()).thenReturn(12345L);
        when(callbackQuery.message()).thenReturn(message);
        when(callbackQuery.id()).thenReturn("callbackId");

        callbackHandlers.handleCallback(botMock, callbackQuery, new HashMap<>());

        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        ArgumentCaptor<AnswerCallbackQuery> answerCaptor = ArgumentCaptor.forClass(AnswerCallbackQuery.class);

        verify(botMock, times(2)).execute(sendCaptor.capture());
        verify(botMock, times(2)).execute(answerCaptor.capture());

        assertEquals("callbackId", callbackQuery.id());
    }


    @Test
    void testHandleMessage() {
        AdCallback adCallbackMock = mock(AdCallback.class);
        when(adCallbackMock.isTitleSet()).thenReturn(false);
        when(adCallbackMock.isDescriptionSet()).thenReturn(false);
        when(adCallbackMock.isPriceSet()).thenReturn(false);
        when(adCallbackMock.isPhotosSet()).thenReturn(false);
        when(adCallbackMock.setTitle(anyString())).thenReturn("Название успешно установлено.");

        callbackHandlers.adCallbacks.put(12345L, adCallbackMock);

        Message message = mock(Message.class);
        Chat chatMock = mock(Chat.class);
        when(message.chat()).thenReturn(chatMock);
        when(chatMock.id()).thenReturn(12345L);
        when(message.text()).thenReturn("Test Title");

        callbackHandlers.handleMessage(botMock, message);

        verify(adCallbackMock).setTitle("Test Title");

        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock, times(2)).execute(sendCaptor.capture());
        SendMessage sendMessage = sendCaptor.getAllValues().get(0);
        assertEquals("Название успешно установлено.", sendMessage.getParameters().get("text"));
    }

    @Test
    void testHasActiveAd() {
        assertFalse(callbackHandlers.hasActiveAd(12345L));
        AdCallback adCallbackMock = mock(AdCallback.class);
        callbackHandlers.adCallbacks.put(12345L, adCallbackMock);
        assertTrue(callbackHandlers.hasActiveAd(12345L));
    }

    @Test
    void testCompleteAdCreation() {
        AdCallback adCallbackMock = mock(AdCallback.class);
        callbackHandlers.adCallbacks.put(12345L, adCallbackMock);

        callbackHandlers.completeAdCreation(botMock, 12345L);

        verify(adCallbackMock).sendAd();

        assertFalse(callbackHandlers.adCallbacks.containsKey(12345L));
    }
}
