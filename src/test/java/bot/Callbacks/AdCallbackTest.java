package bot.Callbacks;

import bot.DatabaseHandler;
import bot.User;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdCallbackTest {
    private TelegramBot mockBot;
    private DatabaseHandler mockDatabaseHandler;
    private AdCallback adCallback;
    private long chatId = 123456789;

    @BeforeEach
    void setUp() {
        mockBot = mock(TelegramBot.class);
        mockDatabaseHandler = mock(DatabaseHandler.class);
        adCallback = new AdCallback(mockBot, mockDatabaseHandler, 123456789L);
        when(mockDatabaseHandler.getUserById(anyLong())).thenReturn(new User(123456789L, "https://t.me/TestUser"));
    }

    @Test
    void setTitle_valid() {
        String result = adCallback.setTitle("Моё объявление");
        assertEquals("Название успешно установлено.", result);
        assertTrue(adCallback.isTitleSet());
    }

    @Test
    void setTitle_invalid() {
        String result = adCallback.setTitle("Очень длинное название, которое превышает 45 символов...");
        assertEquals("Ошибка: название превышает 45 символов.", result);
        assertFalse(adCallback.isTitleSet());
    }

    @Test
    void setDescription_valid() {
        String result = adCallback.setDescription("Описание объявления.");
        assertEquals("Описание успешно установлено.", result);
        assertTrue(adCallback.isDescriptionSet());
    }

    @Test
    void setDescription_invalid() {
        String longDescription = "a".repeat(701);
        String result = adCallback.setDescription(longDescription);
        assertEquals("Ошибка: описание превышает 700 символов.", result);
        assertFalse(adCallback.isDescriptionSet());
    }

    @Test
    void setPrice_valid() {
        String result = adCallback.setPrice(1500);
        assertEquals("Цена успешно установлена.", result);
        assertTrue(adCallback.isPriceSet());
    }

    @Test
    void setPrice_invalid() {
        String result = adCallback.setPrice(-500);
        assertEquals("Ошибка: цена не может быть отрицательной.", result);
        assertFalse(adCallback.isPriceSet());
    }

    @Test
    void addPhoto_valid() {
        String result = adCallback.addPhoto("photoFileId1");
        assertEquals("Фото успешно добавлено.", result);
        assertFalse(adCallback.isPhotosSet());
    }

    @Test
    void addPhoto_limitReached() {
        for (int i = 0; i < 10; i++) {
            adCallback.addPhoto("photoFileId" + i);
        }
        String result = adCallback.addPhoto("photoFileId11");
        assertEquals("Ошибка: достигнут лимит в 10 фотографий.", result);
        assertTrue(adCallback.isPhotosSet());
    }

    @Test
    void getContent_valid() {
        User mockUser = new User(123456789L, "https://t.me/TestUser");

        when(mockDatabaseHandler.getUserById(123456789L)).thenReturn(mockUser);

        adCallback.setTitle("Пример объявления");
        adCallback.setDescription("Пример описания");
        adCallback.setPrice(3000);

        String content = adCallback.getContent();
        assertTrue(content.contains("**Пример объявления**"), "Содержит заголовок");
        assertTrue(content.contains("Пример описания"), "Содержит описание");
        assertTrue(content.contains("3000 руб."), "Содержит цену");
        assertTrue(content.contains("[Перейти к продавцу](https://t.me/TestUser)"), "Содержит ссылку на продавца");
    }

    @Test
    void sendAd_valid() {
        adCallback.setTitle("Пример объявления");
        adCallback.setDescription("Описание объявления");
        adCallback.setPrice(2500);
        adCallback.addPhoto("photoFileId1");
        adCallback.addPhoto("photoFileId2");

        adCallback.sendAd();

        ArgumentCaptor<SendPhoto> sendPhotoCaptor = ArgumentCaptor.forClass(SendPhoto.class);
        verify(mockBot, times(6)).execute(sendPhotoCaptor.capture());
        List<SendPhoto> capturedPhotos = sendPhotoCaptor.getAllValues();

        assertEquals(6, capturedPhotos.size());
        assertEquals(chatId, capturedPhotos.get(0).getParameters().get("chat_id"));
        assertEquals("photoFileId1", capturedPhotos.get(0).getParameters().get("photo"));
        assertEquals(chatId, capturedPhotos.get(1).getParameters().get("chat_id"));
        assertEquals("photoFileId2", capturedPhotos.get(1).getParameters().get("photo"));

        ArgumentCaptor<SendMessage> sendMessageCaptor = ArgumentCaptor.forClass(SendMessage.class);
        verify(mockBot, times(6)).execute(sendMessageCaptor.capture());
        SendMessage capturedMessage = sendMessageCaptor.getValue();

        assertEquals(-1002351079725L, capturedMessage.getParameters().get("chat_id"));
        assertEquals(adCallback.getContent(), capturedMessage.getParameters().get("text"));
    }
}
