package bot.tests

import bot.Commands.ProfileCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ProfileCommandTest {

    private DatabaseHandler databaseHandler;
    private ProfileCommand profileCommand;
    private User mockUser;
    private long userId = 12345;

    @BeforeEach
    public void setUp() {
        databaseHandler = mock(DatabaseHandler.class);
        profileCommand = new ProfileCommand(databaseHandler, userId);
        mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn(userId);
        when(mockUser.getActiveAdsCount()).thenReturn(5);
        when(mockUser.getEarnedMoney()).thenReturn(1000.0);
        when(databaseHandler.getUserById(userId)).thenReturn(mockUser);
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Просмотр Вашего профиля";
        String actualDescription = profileCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды"");
    }

    @Test
    public void testGetContent() {
        String expectedContent = "Ваш профиль:\n" +
                "ID: " + userId + "\n" +
                "Активные объявления: 5\n" +
                "Заработанные деньги: 1000.0 рублей";
        String actualContent = profileCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при проверке вызова команды");
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/profile";
        String actualCommand = profileCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }
}
