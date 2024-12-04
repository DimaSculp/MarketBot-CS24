package bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.*;
import com.pengrad.telegrambot.request.CopyMessage;
import com.pengrad.telegrambot.request.SendMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
        handler = new ModerationHandler(botMock, dbMock);
    }

    @Test
    void testApprove() {
        Chat chat = mock(Chat.class);
        when(chat.id()).thenReturn(12345L);

        Message replied = mock(Message.class);
        when(replied.caption()).thenReturn("Продавец: https://t.me/test_user");
        when(replied.chat()).thenReturn(chat);
        when(replied.messageId()).thenReturn(67890);

        Message post = mock(Message.class);
        when(post.replyToMessage()).thenReturn(replied);
        when(post.text()).thenReturn("approved");

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(post);

        handler.handleUpdate(update);

        ArgumentCaptor<CopyMessage> captor = ArgumentCaptor.forClass(CopyMessage.class);
        verify(botMock).execute(captor.capture());

        CopyMessage copy = captor.getValue();
        assertEquals(-1002397946078L, copy.getParameters().get("chat_id"));
        assertEquals(12345L, copy.getParameters().get("from_chat_id"));
        assertEquals(67890, copy.getParameters().get("message_id"));
    }

    @Test
    void testReject() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dbMock.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("user_id")).thenReturn(54321L);

        Message replied = mock(Message.class);
        when(replied.caption()).thenReturn("Продавец: https://t.me/test_user");

        Message post = mock(Message.class);
        when(post.replyToMessage()).thenReturn(replied);
        when(post.text()).thenReturn("Некорректное описание");

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(post);

        handler.handleUpdate(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock).execute(captor.capture());

        SendMessage msg = captor.getValue();
        assertEquals(54321L, msg.getParameters().get("chat_id"));
        assertTrue(msg.getParameters().get("text").toString().contains("Ваше объявление отклонено. Причина: Некорректное описание"));
    }

    @Test
    void testExtractLink() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dbMock.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("user_id")).thenReturn(54321L);

        Message replied = mock(Message.class);
        when(replied.caption()).thenReturn("Продавец: https://t.me/test_user");

        Message post = mock(Message.class);
        when(post.replyToMessage()).thenReturn(replied);
        when(post.text()).thenReturn("Некорректное описание");

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(post);

        handler.handleUpdate(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock).execute(captor.capture());

        SendMessage msg = captor.getValue();
        assertEquals(54321L, msg.getParameters().get("chat_id"));
        assertTrue(msg.getParameters().get("text").toString().contains("Ваше объявление отклонено"));
    }

    @Test
    void testFindUserId() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(dbMock.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getLong("user_id")).thenReturn(54321L);

        Message replied = mock(Message.class);
        when(replied.caption()).thenReturn("Продавец: https://t.me/test_user");

        Message post = mock(Message.class);
        when(post.replyToMessage()).thenReturn(replied);
        when(post.text()).thenReturn("Некорректное описание");

        Update update = mock(Update.class);
        when(update.channelPost()).thenReturn(post);

        handler.handleUpdate(update);

        ArgumentCaptor<SendMessage> captor = ArgumentCaptor.forClass(SendMessage.class);
        verify(botMock).execute(captor.capture());

        SendMessage msg = captor.getValue();
        assertEquals(54321L, msg.getParameters().get("chat_id"));
    }
}

