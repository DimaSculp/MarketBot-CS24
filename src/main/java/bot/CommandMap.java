package bot;
import java.util.HashMap;
import java.util.Map;

public class CommandMap {
    private static final Map<String,String> CommandsMap = new HashMap<>();
    static {
        CommandsMap.put("/start", "Команда для начала работы.");
        CommandsMap.put("/authors", "Список авторов бота.");
        CommandsMap.put("/info", "Краткое описание бота.");
        CommandsMap.put("/help", "Список команд.");
    }

    public static Map<String,String> getCommandsMap(){
        return CommandsMap;
    }
}
