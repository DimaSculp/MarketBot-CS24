package bot.Commands;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HelpCommandTest {

    private HelpCommand helpCommand;
    private Map<String, BotCommands> commandMap;

    @BeforeEach
    public void setUp() {
        commandMap = new HashMap<>();

        BotCommands mockHelpCommand = mock(HelpCommand.class);
        when(mockHelpCommand.getDescription()).thenReturn("Список команд");
        commandMap.put("/help", mockHelpCommand);

        BotCommands mockAuthorsCommand = mock(AuthorsCommand.class);
        when(mockAuthorsCommand.getDescription()).thenReturn("Авторы проекта");
        commandMap.put("/authors", mockAuthorsCommand);

        BotCommands mockInfoCommand = mock(InfoCommand.class);
        when(mockInfoCommand.getDescription()).thenReturn("Краткое описание бота.");
        commandMap.put("/info", mockInfoCommand);

        BotCommands mockStartCommand = mock(StartCommand.class);
        when(mockStartCommand.getDescription()).thenReturn("Команда для начала работы");
        commandMap.put("/start", mockStartCommand);

        BotCommands mockProfileCommand = mock(ProfileCommand.class);
        when(mockProfileCommand.getDescription()).thenReturn("Просмотр Вашего профиля");
        commandMap.put("/profile", mockProfileCommand);

        helpCommand = new HelpCommand(commandMap);
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Список команд";
        String actualDescription = helpCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды");
    }

    @Test
    public void testGetContent() {
        StringBuilder expectedContent = new StringBuilder("Доступные команды:\n");
        expectedContent.append("/help - Список команд\n")
                .append("/authors - Авторы проекта\n")
                .append("/info - Краткое описание бота.\n")
                .append("/start - Команда для начала работы\n")
                .append("/profile - Просмотр Вашего профиля\n");

        String actualContent = helpCommand.getContent();
        assertEquals(expectedContent.toString(), actualContent, "Ошибка при проверке вызова команды");
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/help";
        String actualCommand = helpCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }
}
