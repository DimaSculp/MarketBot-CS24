package bot.tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class HelpCommandTest {

    private HelpCommand helpCommand;
    private Map<String, BotCommands> commandMap;

    @BeforeEach
    public void setUp() {
        commandMap = new HashMap<>();
        commandMap.put("/help", mock(HelpCommand.class));
        commandMap.put("/authors", mock(AuthorsCommand.class));
        commandMap.put("/info", mock(InfoCommand.class));
        commandMap.put("/start", mock(StartCommand.class));
        commandMap.put("/profile", mock(ProfileCommand.class));
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