package bot.Commands;

import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import bot.Keyboards;
import bot.DatabaseHandler;
import bot.User;

public class ProfileCommand implements BotCommands {
    private DatabaseHandler databaseHandler;
    private long userId;

    public ProfileCommand(DatabaseHandler databaseHandler, long userId) {
        this.databaseHandler = databaseHandler;
        this.userId = userId;
    }
    public ProfileCommand(){

    }

    @Override
    public String getDescription() {
        return "Просмотр Вашего профиля";
    }

    @Override
    public String getContent() {
        User user = databaseHandler.getUserById(userId);
        return "Ваш профиль:\n" +
                "ID: " + user.getUserId() + "\n" +
                "Активные объявления: " + user.getActiveAdsCount() + "\n" +
                "Заработанные деньги: " + user.getEarnedMoney() + " рублей";
    }

    @Override
    public String getCommand() {
        return "/profile";
    }
    public InlineKeyboardMarkup getKeyboard(){
        return Keyboards.getToMenuKeyboard();
    }
}
