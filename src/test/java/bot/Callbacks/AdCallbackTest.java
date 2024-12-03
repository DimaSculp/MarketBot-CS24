package bot.Callbacks;

import bot.DatabaseHandler;
import com.pengrad.telegrambot.TelegramBot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdCallbackTest {
    private TelegramBot mockBot;
    private DatabaseHandler mockDatabaseHandler;
    private AdCallback adCallback;

    @BeforeEach
    void setUp() {
        mockBot = mock(TelegramBot.class);
        mockDatabaseHandler = mock(DatabaseHandler.class);
        adCallback = new AdCallback(mockBot, mockDatabaseHandler, 123456789L);
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
}