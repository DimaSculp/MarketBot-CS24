package ru.outfix.market.telegram;

import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.outfix.market.ad.AdCreationFlow;
import ru.outfix.market.ad.AdRemovalFlow;
import ru.outfix.market.command.AuthorsCommand;
import ru.outfix.market.command.CommandRegistry;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static ru.outfix.market.TelegramMocks.message;

class MessageHandlerTest {

    private static final long CHAT = 123456789L;

    private Messenger messenger;
    private AdCreationFlow adCreation;
    private AdRemovalFlow adRemoval;
    private MessageHandler handler;

    @BeforeEach
    void setUp() {
        messenger = mock(Messenger.class);
        adCreation = mock(AdCreationFlow.class);
        adRemoval = mock(AdRemovalFlow.class);
        CommandRegistry commands = new CommandRegistry().register(new AuthorsCommand("@test_support"));
        handler = new MessageHandler(messenger, commands, adCreation, adRemoval);
    }

    @Test
    void repliesToCommand() {
        handler.handle(message(CHAT, "/authors"));
        verify(messenger).send(eq(CHAT), eq("бот написан фиксером @test_support"), any());
    }

    @Test
    void unknownCommand() {
        handler.handle(message(CHAT, "unknown_command"));
        verify(messenger).send(CHAT, "Неизвестная команда. Введите /help для списка команд.");
    }

    @Test
    void messageWithoutTextIsUnknownCommand() {
        // Раньше стикер или документ вне диалога приводил к NPE
        handler.handle(message(CHAT, null));
        verify(messenger).send(CHAT, "Неизвестная команда. Введите /help для списка команд.");
    }

    @Test
    void geoDeepLinkSendsLocation() {
        handler.handle(message(CHAT, "/start geo_56_838011_60_597465"));

        ArgumentCaptor<SendLocation> captor = ArgumentCaptor.forClass(SendLocation.class);
        verify(messenger).execute(captor.capture());
        assertEquals(56.838011f, (float) captor.getValue().getParameters().get("latitude"), 1e-6);
        verifyNoInteractions(adCreation, adRemoval);
    }

    @Test
    void activeAdCreationReceivesMessages() {
        when(adCreation.isActive(CHAT)).thenReturn(true);
        Message message = message(CHAT, "/authors");

        handler.handle(message);

        verify(adCreation).handleMessage(message);
        verify(messenger, never()).send(anyLong(), anyString(), any());
    }

    @Test
    void activeRemovalReceivesMessages() {
        when(adRemoval.isActive(CHAT)).thenReturn(true);
        Message message = message(CHAT, "2");

        handler.handle(message);

        verify(adRemoval).handleMessage(message);
    }

    @Test
    void commandCancelsRemovalDialog() {
        when(adRemoval.isActive(CHAT)).thenReturn(true);

        handler.handle(message(CHAT, "/authors"));

        verify(adRemoval).cancel(CHAT);
        verify(adRemoval, never()).handleMessage(any());
        verify(messenger).send(eq(CHAT), eq("бот написан фиксером @test_support"), any());
    }
}
