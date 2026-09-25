package ru.outfix.market.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.outfix.market.db.UserAccount;
import ru.outfix.market.db.UserStats;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.telegram.Keyboards;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandsTest {

    private static final CommandContext CONTEXT = new CommandContext(12345L, "test_user");

    private UserRepository users;
    private CommandRegistry registry;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        registry = new CommandRegistry();
        registry.register(new StartCommand(users))
                .register(new ProfileCommand(users))
                .register(new HelpCommand(registry))
                .register(new InfoCommand("OutFix_Market"))
                .register(new AuthorsCommand("@test_support"));
    }

    @Test
    void findsCommandIgnoringArgumentsAndBotSuffix() {
        assertEquals("/help", registry.find("/help").orElseThrow().name());
        assertEquals("/help", registry.find("/help@Market_OutFix_Bot").orElseThrow().name());
        assertEquals("/start", registry.find("/start promo").orElseThrow().name());
        assertTrue(registry.find("help").isEmpty());
        assertTrue(registry.find("/unknown").isEmpty());
        assertTrue(registry.find(null).isEmpty());
    }

    @Test
    void helpListsCommandsInRegistrationOrder() {
        String expected = "Доступные команды:\n"
                + "/start - Команда для начала работы\n"
                + "/profile - Просмотр Вашего профиля\n"
                + "/help - Список команд\n"
                + "/info - Краткое описание бота.\n"
                + "/authors - Автор проекта\n";
        assertEquals(expected, registry.find("/help").orElseThrow().reply(CONTEXT));
    }

    @Test
    void startCreatesProfile() {
        when(users.upsert(12345L, "test_user")).thenReturn(true);
        BotCommand start = registry.find("/start").orElseThrow();

        assertEquals("Привет! Твой профиль создан. Ты можешь начать выкладывать объявления!", start.reply(CONTEXT));
        assertEquals(Keyboards.start(), start.keyboard());
    }

    @Test
    void startForExistingUser() {
        when(users.upsert(12345L, "test_user")).thenReturn(false);
        assertEquals("Привет! Твой профиль уже существует. Ты можешь начать выкладывать объявления!",
                registry.find("/start").orElseThrow().reply(CONTEXT));
    }

    @Test
    void profileShowsStatistics() {
        when(users.findById(12345L)).thenReturn(Optional.of(new UserAccount(12345L, "test_user")));
        when(users.stats(12345L)).thenReturn(new UserStats(5, 1000));
        assertEquals("Ваш профиль:\nID: 12345\nАктивные объявления: 5\nЗаработано: 1000 руб.\n",
                registry.find("/profile").orElseThrow().reply(CONTEXT));
    }

    @Test
    void profileNotFound() {
        when(users.findById(12345L)).thenReturn(Optional.empty());
        assertEquals("Профиль не найден.", registry.find("/profile").orElseThrow().reply(CONTEXT));
    }

    @Test
    void staticCommands() {
        assertEquals("бот написан фиксером @test_support", registry.find("/authors").orElseThrow().reply(CONTEXT));
        assertTrue(registry.find("/info").orElseThrow().reply(CONTEXT).contains("@OutFix_Market"));
    }
}
