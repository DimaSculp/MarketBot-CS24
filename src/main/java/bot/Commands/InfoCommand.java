package bot.Commands;

import bot.Keyboards;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class InfoCommand implements BotCommands{

    @Override
    public String getDescription() {
        return "Краткое описание бота.";
    }

    @Override
    public String getContent() {
        return "Этот бот создан для удобной публикации и отслеживания объявлений в канале Барахолка УрФУ.";
    }
    @Override
    public String getCommand() {
        return "/info";
    }

    public InlineKeyboardMarkup getKeyboard(){
        return Keyboards.getToMenuKeyboard();
    }
}
