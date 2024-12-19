package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import com.pengrad.telegrambot.response.MessageIdResponse;
import com.pengrad.telegrambot.request.CopyMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ModerationHandlerTest {
    private TelegramBot botMock;
    private DatabaseHandler dbMock;
    private ModerationHandler handler;

    @BeforeEach
    void setup() {
        botMock = mock(TelegramBot.class);
        dbMock = mock(DatabaseHandler.class);
    }

    @Test
    void testParseFileId() {
        ModerationHandler handler = new ModerationHandler(botMock, dbMock); // Создаём экземпляр вашего обработчика (или используйте существующий)

        String textWithValidFileIds = "Некоторые данные ~[file1, file2, file3]";
        List<String> result = handler.parseFileId(textWithValidFileIds);
        assertEquals(3, result.size(), "Должно быть 3 идентификатора файлов");
        assertTrue(result.contains("file1"), "Список должен содержать 'file1'");
        assertTrue(result.contains("file2"), "Список должен содержать 'file2'");
        assertTrue(result.contains("file3"), "Список должен содержать 'file3'");

        String textWithoutTilde = "Некоторые данные без символа ~";
        result = handler.parseFileId(textWithoutTilde);
        assertTrue(result.isEmpty(), "Если нет символа ~, результат должен быть пустым");

        String textWithInvalidFormat = "Некоторые данные ~file1, file2, file3";
        result = handler.parseFileId(textWithInvalidFormat);
        assertTrue(result.isEmpty(), "Если формат невалиден, результат должен быть пустым");

        String textWithOneFile = "Некоторые данные ~[file1]";
        result = handler.parseFileId(textWithOneFile);
        assertEquals(1, result.size(), "Должен быть один идентификатор файла");
        assertTrue(result.contains("file1"), "Список должен содержать 'file1'");

        String textWithSpacesAroundIds = "Некоторые данные ~[ file1 , file2 , file3 ]";
        result = handler.parseFileId(textWithSpacesAroundIds);
        assertEquals(3, result.size(), "Должно быть 3 идентификатора файлов");
        assertTrue(result.contains("file1"), "Список должен содержать 'file1'");
        assertTrue(result.contains("file2"), "Список должен содержать 'file2'");
        assertTrue(result.contains("file3"), "Список должен содержать 'file3'");
    }

    @Test
    void testApprove() {
        TelegramBot botMock = mock(TelegramBot.class);
        DatabaseHandler dbMock = mock(DatabaseHandler.class);
        ModerationHandler handler = new ModerationHandler(botMock, dbMock);


        Message repliedMessage = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(12345L);
        when(repliedMessage.chat()).thenReturn(chat);
        when(repliedMessage.caption()).thenReturn("Продавец: https://t.me/test_user ~[file1, file2]");
        when(repliedMessage.chat().id()).thenReturn(12345L);
        when(repliedMessage.messageId()).thenReturn(10);

        Message channelPost = mock(Message.class);
        when(channelPost.replyToMessage()).thenReturn(repliedMessage);
        when(channelPost.text()).thenReturn("approved");

        MessageIdResponse mockResponse = mock(MessageIdResponse.class);
        when(botMock.execute(any(CopyMessage.class))).thenReturn(mockResponse);
        when(mockResponse.isOk()).thenReturn(true);
        when(mockResponse.messageId()).thenReturn(12345);

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(channelPost);

        when(dbMock.findUserIdByUserlink(anyString())).thenReturn(54321L);

        handler.handleUpdate(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock, times(3)).execute(captor.capture());
    }

    @Test
    void testReject() {
        TelegramBot botMock = mock(TelegramBot.class);
        DatabaseHandler dbMock = mock(DatabaseHandler.class);
        ModerationHandler handler = new ModerationHandler(botMock, dbMock);

        Message repliedMessage = mock(Message.class);
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(12345L);
        when(repliedMessage.chat()).thenReturn(chat);
        when(repliedMessage.caption()).thenReturn("Продавец: https://t.me/test_user ~[file1, file2]");
        when(repliedMessage.chat().id()).thenReturn(12345L);
        when(repliedMessage.messageId()).thenReturn(10);

        Message channelPost = mock(Message.class);
        when(channelPost.replyToMessage()).thenReturn(repliedMessage);
        when(channelPost.text()).thenReturn("rejected");

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(channelPost);

        when(dbMock.findUserIdByUserlink(anyString())).thenReturn(54321L);

        handler.handleUpdate(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock).execute(captor.capture());
    }

    @Test
    void testExtractUserLink() {
        ModerationHandler handler = new ModerationHandler(botMock, dbMock);

        String textWithLink = "Посетите наш канал на Telegram: https://t.me/test_channel";
        String result = handler.extractUserLink(textWithLink);
        assertEquals("https://t.me/test_channel", result, "Ссылка должна быть извлечена корректно");

        String textWithoutLink = "Здесь нет ссылки на Telegram";
        result = handler.extractUserLink(textWithoutLink);
        assertNull(result, "Ссылка не должна быть найдена, если её нет в тексте");

        String textWithMultipleLinks = "Ссылки: https://t.me/first_link и https://t.me/second_link";
        result = handler.extractUserLink(textWithMultipleLinks);
        assertEquals("https://t.me/first_link", result, "Должна быть извлечена первая ссылка");

        String textWithInvalidLink = "Это не ссылка https://example.com";
        result = handler.extractUserLink(textWithInvalidLink);
        assertNull(result, "Если ссылка не соответствует формату 'https://t.me/', она не должна быть извлечена");
    }
}