package bot;

import bot.Callbacks.AdCallback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CallbackHandlersTest {
    private TelegramBot botMock;
    private DatabaseHandler dbHandlerMock;
    private CallbackHandlers callbackHandlers;

    @BeforeEach
    void setUp() {
        botMock = mock(TelegramBot.class);
        dbHandlerMock = mock(DatabaseHandler.class);
        callbackHandlers = new CallbackHandlers(dbHandlerMock);
    }

    @Test
    void testHandleCallback() {
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.data()).thenReturn("add_ad");

        Message message = mock(Message.class);
        Chat chatMock = mock(Chat.class);
        when(message.chat()).thenReturn(chatMock);
        when(chatMock.id()).thenReturn(12345L);

        when(callbackQuery.message()).thenReturn(message);

        callbackHandlers.handleCallback(botMock, callbackQuery, new HashMap<>());

        ArgumentCaptor<SendMessage> sendCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock).execute(sendCaptor.capture());

        SendMessage sendMessage = sendCaptor.getValue();
        assertEquals(12345L, sendMessage.getParameters().get("chat_id"));
        assertEquals("Пожалуйста, отправьте название объявления (до 45 символов).", sendMessage.getParameters().get("text"));
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
