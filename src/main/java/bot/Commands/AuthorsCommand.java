package bot.Commands;

import bot.Keyboards;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;

public class AuthorsCommand implements BotCommands{
        @Override
        public String getDescription() {
            return "Автор проекта";
        }

        @Override
        public String getContent() {
            return "бот написан фиксером @sculp2ra";
        }

        @Override
        public String getCommand() {
            return "/authors";
        }

        public InlineKeyboardMarkup getKeyboard(){
            return Keyboards.getToMenuKeyboard();
    }
}
