package bot.Commands;

import bot.DatabaseHandler;
import bot.Keyboards;
import bot.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class StartCommand implements BotCommands {
    private final DatabaseHandler databaseHandler;
    private long userId;
    private String userLink;

    public StartCommand(DatabaseHandler databaseHandler) {
        this.databaseHandler = databaseHandler;
    }

    @Override
    public String getDescription() {
        return "Команда для начала работы";
    }

    @Override
    public String getContent() {
        User existingUser = databaseHandler.getUserById(userId);
        if (existingUser != null) {
            return "Привет! Твой профиль уже существует. Ты можешь начать выкладывать объявления!";
        }
        User newUser = new User(userId, userLink);
        databaseHandler.addUser(newUser);
        return "Привет! Твой профиль создан. Ты можешь начать выкладывать объявления!";
    }

    @Override
    public String getCommand() {
        return "/start";
    }

    public InlineKeyboardMarkup getKeyboard() {
        return Keyboards.getStartKeyboard();
    }

    public void setUserData(long userId, String userLink) {
        this.userId = userId;
        this.userLink = userLink;
    }
}
