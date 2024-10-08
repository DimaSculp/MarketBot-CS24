package bot/tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StartCommandTest {

    private StartCommand startCommand;

    @BeforeEach
    void setUp() {
        startCommand = new StartCommand();
    }

    @Test
    void testGetContent() {
        // проверка содержания команды
        String expectedContent = "Добро пожаловать в бот!";
        assertEquals(expectedContent, startCommand.getContent());
    }
}