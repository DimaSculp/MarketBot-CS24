package bot.Commands;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import bot.Keyboards;
import bot.DatabaseHandler;
import bot.User;

public class ProfileCommand implements BotCommands {
    private DatabaseHandler databaseHandler;
    private long userId;

    public ProfileCommand(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    @Override
    public String getDescription() {
        return "Просмотр Вашего профиля";
    }

    @Override
    public String getContent() {
        System.out.println("Получение данных для userId: " + userId);

        User user = databaseHandler.getUserById(userId);
        if (user != null) {
            return "Ваш профиль:\n" +
                    "ID: " + user.getUserId() + "\n" +
                    "Активные объявления: " + user.getActiveAdsCount() + "\n" +
                    "Заработанные деньги: " + user.getEarnedMoney() + " рублей";
        } else {
            return "Профиль не найден.";
        }
    }

    @Override
    public String getCommand() {
        return "/profile";
    }

    public InlineKeyboardMarkup getKeyboard() {
        return Keyboards.getToMenuKeyboard();
    }

    public void setUserData(long userId) {
        this.userId = userId;
    }
}
