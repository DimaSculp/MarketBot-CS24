package bot/tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import java.util.HashMap;
import java.util.Map;

class HelpCommandTest {

    private HelpCommand helpCommand;
    private Map<String, BotCommands> commandMap;

    @BeforeEach
    void setUp() {
        commandMap = new HashMap<>();
        helpCommand = new HelpCommand(commandMap);
    }

    @Test
    void testGetContent() {
        String expectedContent = "Доступные команды:\n/help - Список команд\n/authors - Авторы проекта\n/info - Краткое описание бота.\n/start - Команда для начала работы\n/profile - Просмотр Вашего профиля";
        assertEquals(expectedContent, helpCommand.getContent());
    }
}
