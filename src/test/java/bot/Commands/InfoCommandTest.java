package bot.Commands

import bot.Commands.InfoCommand;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InfoCommandTest {

    private InfoCommand infoCommand;

    @BeforeEach
    public void setUp() {
        infoCommand = new InfoCommand();
    }

    @Test
    public void testGetDescription() {
        String expectedDescription = "Краткое описание бота.";
        String actualDescription = infoCommand.getDescription();
        assertEquals(expectedDescription, actualDescription, "Ошибка при проверке описания команды");
    }

    @Test
    public void testGetContent() {
        String expectedContent = "Этот бот создан для удобной публикации и отслеживания объявлений в канале Барахолка УрФУ.";
        String actualContent = infoCommand.getContent();
        assertEquals(expectedContent, actualContent, "Ошибка при проверке вызова команды");
    }

    @Test
    public void testGetCommand() {
        String expectedCommand = "/info";
        String actualCommand = infoCommand.getCommand();
        assertEquals(expectedCommand, actualCommand, "Ошибка при проверке названия команды");
    }

}
