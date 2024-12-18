package bot.Callbacks;

import bot.DatabaseHandler;
import bot.User;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMediaGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdCallbackTest {
    private TelegramBot mockBot;
    private AdCallback adCallback;

    @BeforeEach
    void setUp() {
        mockBot = mock(TelegramBot.class);
        DatabaseHandler mockDatabaseHandler = mock(DatabaseHandler.class);
        adCallback = new AdCallback(mockBot, mockDatabaseHandler, 123456789L);
        User mockUser = mock(User.class);
        when(mockUser.getUserLink()).thenReturn("https://t.me/test_user");
        when(mockDatabaseHandler.getUserById(123456789L)).thenReturn(mockUser);
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
        assertTrue(adCallback.isPhotosSet());
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
    void testGetContent() {
        adCallback.setTitle("Моё объявление");
        adCallback.setDescription("Описание объявления.");
        adCallback.setPrice(1500);
        adCallback.addPhoto("photoFileId1");

        String expectedContent = "<b>Моё объявление</b>\n\n" +
                "<i>Описание объявления.</i>\n\n" +
                "<b>Цена: </b>1500 руб.\n\n" +
                "<b>test_user</b>\n\n" +
                "<a href=\"https://t.me/test_user \" >контакт продовца</a>\n" +
                "<a href=\"https://t.me/SculpTestShopBot\">разместить объявление</a>" +
                "~[photoFileId1]";

        String content = adCallback.getContent();
        assertEquals(expectedContent, content);
    }

    @Test
    void sendAd_test() {
        adCallback.setTitle("Моё объявление");
        adCallback.setDescription("Описание объявления.");
        adCallback.setPrice(1500);
        adCallback.addPhoto("photoFileId1");
        adCallback.addPhoto("photoFileId2");

        adCallback.sendAd();

        ArgumentCaptor<SendMediaGroup> captor = ArgumentCaptor.forClass(SendMediaGroup.class);
        verify(mockBot).execute(captor.capture());

        SendMediaGroup sendMediaGroup = captor.getValue();

        assertNotNull(sendMediaGroup);

        verify(mockBot).execute(any(SendMediaGroup.class));

        assertTrue(adCallback.isSendCheckDone());
    }
}