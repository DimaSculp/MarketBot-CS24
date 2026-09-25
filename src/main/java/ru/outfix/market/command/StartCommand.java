package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.telegram.Keyboards;

public class StartCommand implements BotCommand {

    private static final Logger log = LoggerFactory.getLogger(StartCommand.class);

    private final UserRepository users;

    public StartCommand(UserRepository users) {
        this.users = users;
    }

    @Override
    public String name() {
        return "/start";
    }

    @Override
    public String description() {
        return "Команда для начала работы";
    }

    @Override
    public String reply(CommandContext context) {
        boolean created = users.upsert(context.userId(), context.username());
        if (created) {
            log.info("Зарегистрирован новый пользователь");
        }
        return created
                ? "Привет! Твой профиль создан. Ты можешь начать выкладывать объявления!"
                : "Привет! Твой профиль уже существует. Ты можешь начать выкладывать объявления!";
    }

    @Override
    public InlineKeyboardMarkup keyboard() {
        return Keyboards.start();
    }
}
