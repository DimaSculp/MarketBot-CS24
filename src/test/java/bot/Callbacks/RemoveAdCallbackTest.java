package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.EditMessageCaption;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;

public class RemoveAdCallbackTest {

    private DatabaseHandler dbMock;
    private TelegramBot botMock;
    private RemoveAdCallback removeAdCallback;
    private Connection connectionMock;

    @BeforeEach
    public void setUp() {
        dbMock = mock(DatabaseHandler.class);
        botMock = mock(TelegramBot.class);
        connectionMock = mock(Connection.class);

        when(dbMock.getConnection()).thenReturn(connectionMock);

        removeAdCallback = new RemoveAdCallback(dbMock);
    }

    @Test
    public void testRemoveAd(){
        long chatId = 12345L;
        int adNumber = 1;
        String userLink = "https://t.me/someUser";
        String adLink = "https://t.me/KB2024CHANNEL/post/100";
        List<String> ads = Arrays.asList(adLink);

        when(dbMock.getUserLinkByChatId(chatId)).thenReturn(userLink);

        verify(dbMock, never()).removeAdFromUser(chatId, adNumber);

        ArgumentCaptor<EditMessageCaption> captor = ArgumentCaptor.forClass(EditMessageCaption.class);
        verify(botMock, never()).execute(captor.capture());
    }

    @Test
    public void testRemoveAd_invalidAdNumber(){
        long chatId = 12345L;
        int invalidAdNumber = 5;
        List<String> ads = Arrays.asList("https://t.me/KB2024CHANNEL/post/100");

        verify(dbMock, never()).removeAdFromUser(anyLong(), anyInt());

        verify(botMock, never()).execute(any(EditMessageCaption.class));
    }

    @Test
    public void testRemoveAd_emptyAdsList(){
        long chatId = 12345L;
        int adNumber = 1;
        List<String> ads = Arrays.asList();

        verify(dbMock, never()).removeAdFromUser(anyLong(), anyInt());

        verify(botMock, never()).execute(any(EditMessageCaption.class));
    }
}