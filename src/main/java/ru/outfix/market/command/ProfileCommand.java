package ru.outfix.market.command;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import ru.outfix.market.db.UserRepository;
import ru.outfix.market.db.UserStats;
import ru.outfix.market.telegram.Keyboards;

public class ProfileCommand implements BotCommand {

    private final UserRepository users;

    public ProfileCommand(UserRepository users) {
        this.users = users;
    }

    @Override
    public String name() {
        return "/profile";
    }

    @Override
    public String description() {
        return "Просмотр Вашего профиля";
    }

    @Override
    public String reply(CommandContext context) {
        if (users.findById(context.userId()).isEmpty()) {
            return "Профиль не найден.";
        }
        UserStats stats = users.stats(context.userId());
        return "Ваш профиль:\n"
                + "ID: " + context.userId() + "\n"
                + "Активные объявления: " + stats.activeAds() + "\n"
                + "Заработано: " + stats.earnedMoney() + " руб.\n";
    }

    @Override
    public InlineKeyboardMarkup keyboard() {
        return Keyboards.menu();
    }
}
