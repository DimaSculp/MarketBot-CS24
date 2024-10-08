package bot/tests

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileCommandTest {

    private ProfileCommand profileCommand;

    @BeforeEach
    void setUp() {
        profileCommand = new ProfileCommand();
    }

    @Test
    void testGetContent() {
        // проверка содержания команды
        String expectedContent = "Ваш профиль: user123";
        assertEquals(expectedContent, profileCommand.getContent());
    }
}