package bot;

import bot.Commands.BotCommands;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MessageHandlersTest {

    private TelegramBot mockBot;
    private CallbackHandlers mockCallbackHandlers;
    private MessageHandlers messageHandlers;

    @BeforeEach
    void setUp() {
        mockBot = mock(TelegramBot.class);
        mockCallbackHandlers = mock(CallbackHandlers.class);

        DatabaseHandler mockDatabaseHandler = mock(DatabaseHandler.class);
        Map<String, BotCommands> commandMap = CommandInitializer.initializeCommands(mockDatabaseHandler);

        messageHandlers = new MessageHandlers(mockDatabaseHandler, commandMap, mockCallbackHandlers);
    }

    @Test
    void handleAuthorsCommand() {
        Message mockMessage = mock(Message.class);
        Chat mockChat = mock(Chat.class);
        User mockUser = mock(User.class);

        when(mockMessage.chat()).thenReturn(mockChat);
        when(mockMessage.text()).thenReturn("/authors");
        when(mockChat.id()).thenReturn(123456789L);
        when(mockMessage.from()).thenReturn(mockUser);
        when(mockUser.username()).thenReturn("testuser");

        when(mockCallbackHandlers.hasActiveAd(123456789L)).thenReturn(false);

        messageHandlers.handleMessage(mockBot, mockMessage);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(mockBot).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();

        assertEquals("123456789", sentMessage.getParameters().get("chat_id"));
        assertEquals(
                "Бота создали Бабенко Андрей (@minofprop) и Кухтей Дмитрий (@sculp2ra). Спасибо за интерес!",
                sentMessage.getParameters().get("text")
        );
    }

    @Test
    void handleUnknownCommand() {
        Message mockMessage = mock(Message.class);
        Chat mockChat = mock(Chat.class);
        User mockUser = mock(User.class);

        when(mockMessage.chat()).thenReturn(mockChat);
        when(mockMessage.text()).thenReturn("unknown_command");
        when(mockChat.id()).thenReturn(123456789L);
        when(mockMessage.from()).thenReturn(mockUser);
        when(mockUser.username()).thenReturn("testuser");

        when(mockCallbackHandlers.hasActiveAd(123456789L)).thenReturn(false);

        messageHandlers.handleMessage(mockBot, mockMessage);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(mockBot).execute(captor.capture());

        SendMessage sentMessage = captor.getValue();

        assertEquals("123456789", sentMessage.getParameters().get("chat_id"));
        assertEquals("Неизвестная команда. Введите /help для списка команд.", sentMessage.getParameters().get("text"));
    }
}