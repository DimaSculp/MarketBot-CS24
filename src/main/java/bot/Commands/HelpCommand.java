package bot.Commands;

import bot.Keyboards;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

import java.util.Map;

public class HelpCommand implements BotCommands {
    private final Map<String, BotCommands> commandMap;
    public HelpCommand(Map<String, BotCommands> commandMap) {
        this.commandMap = commandMap;
    }
    @Override
    public String getDescription() {
        return "Список команд";
    }

    @Override
    public String getContent() {
        StringBuilder content = new StringBuilder("Доступные команды:\n");
        for (Map.Entry<String, BotCommands> entry : commandMap.entrySet()) {
            content.append(entry.getKey())
                    .append(" - ")
                    .append(entry.getValue().getDescription())
                    .append("\n");
        }
        return content.toString();
    }
    @Override
    public String getCommand() {
        return "/help";
    }

    public InlineKeyboardMarkup getKeyboard(){
        return Keyboards.getToMenuKeyboard();
    }
}
