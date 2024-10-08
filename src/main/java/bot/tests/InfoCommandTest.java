package bot/tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class InfoCommandTest {

    private InfoCommand infoCommand;

    @BeforeEach
    void setUp() {
        infoCommand = new InfoCommand();
    }

    @Test
    void testGetContent() {
        // проверка содержания команды
        String expectedContent = "Этот бот помогает...";
        assertEquals(expectedContent, infoCommand.getContent());
    }
}