package bot;

import bot.Commands.*;


import java.util.HashMap;
import java.util.Map;

public class CommandInitializer {

    public static Map<String, BotCommands> initializeCommands() {
        Map<String, BotCommands> commandMap = new HashMap<>();
        commandMap.put("/help", new HelpCommand(commandMap));
        commandMap.put("/authors", new AuthorsCommand());
        commandMap.put("/info", new InfoCommand());
        commandMap.put("/start", new StartCommand());
        commandMap.put("/profile", new ProfileCommand());

        return commandMap;
    }
}
