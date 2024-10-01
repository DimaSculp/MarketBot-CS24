package bot.Commands;

import bot.DatabaseHandler;
import bot.Keyboards;
import bot.User;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class StartCommand implements BotCommands {
    public StartCommand(){

    }

    private DatabaseHandler databaseHandler;
    private long userId;
    private String userLink;

    public StartCommand(DatabaseHandler databaseHandler, long userId, String userLink) {
        this.databaseHandler = databaseHandler;
        this.userId = userId;
        this.userLink = userLink;
    }


    @Override
    public String getDescription() {
        return "Команда для начала работы";
    }

    @Override
    public String getContent() {
        User newUser = new User(userId, userLink);
        databaseHandler.addUser(newUser);
        return "Привет! Твой профиль создан. Ты можешь начать выкладывать объявления!";
    }

    @Override
    public String getCommand() {
        return "/start";
    }
    public InlineKeyboardMarkup getKeyboard(){
        return Keyboards.getMainKeyboard();
    }
}
