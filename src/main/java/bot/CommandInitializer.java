package bot;

import bot.Commands.*;

import java.util.HashMap;
import java.util.Map;

public class CommandInitializer {
    public static DatabaseHandler databaseHandler;
    public static long UserID;
    public static String userLink;

    private static StartCommand startCommand;
    private static ProfileCommand profileCommand;

    public static Map<String, BotCommands> initializeCommands(DatabaseHandler dbHandler) {
        databaseHandler = dbHandler;
        startCommand = new StartCommand(databaseHandler);
        profileCommand = new ProfileCommand(databaseHandler);

        Map<String, BotCommands> commandMap = new HashMap<>();
        commandMap.put("/help", new HelpCommand(commandMap));
        commandMap.put("/authors", new AuthorsCommand());
        commandMap.put("/info", new InfoCommand());
        commandMap.put("/start", startCommand);
        commandMap.put("/profile", profileCommand);

        return commandMap;
    }

    public static void updateUserData() {
        startCommand.setUserData(UserID, userLink);
        profileCommand.setUserData(UserID);
    }
}
