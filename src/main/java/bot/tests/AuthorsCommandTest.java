package bot/tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorsCommandTest {

    private AuthorsCommand authorsCommand;

    @BeforeEach
    void setUp() {
        authorsCommand = new AuthorsCommand();
    }

    @Test
    void testGetContent() {
        // проверка содержания команды
        String expectedContent = "Авторы бота: Илья и Андрей";
        assertEquals(expectedContent, authorsCommand.getContent());
    }
}