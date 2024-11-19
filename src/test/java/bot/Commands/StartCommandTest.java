package bot.Commands;

import bot.DatabaseHandler;
import bot.User;
import bot.Keyboards;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class StartCommandTest {

    private DatabaseHandler databaseHandler;
    private StartCommand startCommand;
    private long userId = 12345;
    private String userLink = "user_link_example";

    @BeforeEach
    public void setUp() {
        databaseHandler = mock(DatabaseHandler.class);
        startCommand = new StartCommand(databaseHandler);
        startCommand.setUserData(userId, userLink);
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Команда для начала работы";
        String actualDescription = startCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды");
    }

    @Test
    public void testGetContent() {
        String expectedContent = "Привет! Твой профиль создан. Ты можешь начать выкладывать объявления!";
        String actualContent = startCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при проверке вызова команды");

        verify(databaseHandler, times(1)).addUser(any(User.class));
        verify(databaseHandler).addUser(argThat(user -> user.getUserId() == userId && user.getUserLink().equals(userLink)));
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/start";
        String actualCommand = startCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }

    @Test
    public void testGetKeyboard() {
        InlineKeyboardMarkup expectedKeyboard = Keyboards.getMainKeyboard();
        InlineKeyboardMarkup actualKeyboard = startCommand.getKeyboard();
        assertEquals(expectedKeyboard, actualKeyboard, "Ошибка при проверке клавиатуры команды");
    }
}