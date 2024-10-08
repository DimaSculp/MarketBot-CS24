package bot.tests

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuthorsCommandTest {

    private AuthorsCommand authorsCommand;

    @BeforeEach
    public void setUp() {
        authorsCommand = new AuthorsCommand();
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Авторы проекта";
        String actualDescription = authorsCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды"");
    }

    @Test
    public void testGetContent() {
        String expectedContent = "Бота создали Бабенко Андрей (@minofprop) и Кухтей Дмитрий (@sculp2ra). Спасибо за интерес!";
        String actualContent = authorsCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при проверке вызова команды");
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/authors";
        String actualCommand = authorsCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }
}