package bot.Commands;

import bot.DatabaseHandler;
import bot.Keyboards;
import bot.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class ProfileCommandTest {

    private DatabaseHandler databaseHandler;
    private ProfileCommand profileCommand;
    private long userId = 12345;
    private User mockUser;

    @BeforeEach
    public void setUp() {
        databaseHandler = mock(DatabaseHandler.class);
        profileCommand = new ProfileCommand(databaseHandler);
        profileCommand.setUserData(userId);
        mockUser = mock(User.class);
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Просмотр Вашего профиля";
        String actualDescription = profileCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды");
    }

    @Test
    public void testGetContent_UserExists() {
        when(databaseHandler.getUserById(userId)).thenReturn(mockUser);
        when(mockUser.getUserId()).thenReturn(userId);
        when(mockUser.getActiveAdsCount()).thenReturn(5);
        when(mockUser.getEarnedMoney()).thenReturn(1000);

        String expectedContent = "Ваш профиль:\n" +
                "ID: " + userId + "\n" +
                "Активные объявления: 5\n" +
                "Заработанные деньги: 1000 рублей";

        String actualContent = profileCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при получении контента профиля пользователя");
    }

    @Test
    public void testGetContent_UserNotFound() {
        when(databaseHandler.getUserById(userId)).thenReturn(null);

        String expectedContent = "Профиль не найден.";
        String actualContent = profileCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при получении контента: профиль не найден");
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/profile";
        String actualCommand = profileCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }

    @Test
    public void testGetKeyboard() {
        InlineKeyboardMarkup keyboard = profileCommand.getKeyboard();
        assertEquals(Keyboards.getToMenuKeyboard(), keyboard);
    }
}
